package com.quetwo.intellilucee.editor

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.quetwo.intellilucee.lsp.CFMLLspClientDescriptor
import java.nio.file.Files
import java.nio.file.Path

class CFMLCreateDependencyGraphAction @JvmOverloads constructor(
    private val lspExecutableResolver: (Project) -> Path = { CFMLLspClientDescriptor.resolveLspExecutablePath(it) },
    private val commandExecutor: (GeneralCommandLine) -> ProcessOutput = { ExecUtil.execAndGetOutput(it) },
    private val browserOpener: (Project, String, String) -> Unit = { project, title, html ->
        HTMLEditorProvider.openEditor(project, title, html)
    },
    private val errorReporter: (Project, String) -> Unit = { project, message ->
        Messages.showErrorDialog(project, message, "Lucee CFML Error")
    }
) : AnAction()
{
    private val LOG: Logger = Logger.getInstance(CFMLCreateDependencyGraphAction::class.java)

    companion object
    {
        private val SUPPORTED_EXTENSIONS = setOf("cfm", "cfc")

        fun isSupportedFile(file: VirtualFile?): Boolean
        {
            if (file == null || file.isDirectory)
            {
                return false
            }
            val extension = file.extension?.lowercase() ?: return false
            return extension in SUPPORTED_EXTENSIONS
        }
    }

    override fun actionPerformed(event: AnActionEvent)
    {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (!isSupportedFile(file))
        {
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Creating Dependency Graph", true) {
            override fun run(indicator: ProgressIndicator)
            {
                indicator.isIndeterminate = true
                indicator.text = "Generating dependency graph for ${file.name}..."

                try
                {
                    val executablePath = lspExecutableResolver(project)
                    val tempFile = Files.createTempFile("cfml_dependency_graph_", ".htm")
                    tempFile.toFile().deleteOnExit()

                    val targetFilePath = try
                    {
                        file.toNioPath().toAbsolutePath().toString()
                    }
                    catch (_: Exception)
                    {
                        file.path
                    }

                    val commandLine = GeneralCommandLine(executablePath.toString())
                        .withParameters("graph", "--format", "html", "--out", tempFile.toAbsolutePath().toString(), targetFilePath)

                    project.basePath?.let {
                        commandLine.withWorkDirectory(it)
                    }

                    val output = commandExecutor(commandLine)
                    if (output.exitCode != 0)
                    {
                        val errorMsg = output.stderr.ifBlank { output.stdout }.ifBlank { "Exit code ${output.exitCode}" }
                        LOG.warn("Failed to generate dependency graph: exit code ${output.exitCode}, error: $errorMsg")
                        notifyError(project, "Failed to create dependency graph: $errorMsg")
                        return
                    }

                    if (!Files.exists(tempFile) || Files.size(tempFile) == 0L)
                    {
                        LOG.warn("Failed to generate dependency graph: output file was not created or is empty.")
                        notifyError(project, "Failed to create dependency graph: output file was not created or is empty.")
                        return
                    }

                    val htmlContent = Files.readString(tempFile)
                    try
                    {
                        Files.deleteIfExists(tempFile)
                    }
                    catch (_: Exception)
                    {
                    }

                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed)
                        {
                            browserOpener(project, "Dependency Graph: ${file.name}", htmlContent)
                        }
                    }
                }
                catch (exception: Exception)
                {
                    LOG.warn("Error creating dependency graph for ${file.name}", exception)
                    notifyError(project, "Error creating dependency graph: ${exception.message ?: "unknown error"}")
                }
            }
        })
    }

    private fun notifyError(project: Project, message: String)
    {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDispatchThread)
        {
            app.invokeLater {
                if (!project.isDisposed)
                {
                    errorReporter(project, message)
                }
            }
        }
        else if (!project.isDisposed)
        {
            errorReporter(project, message)
        }
    }

    override fun update(event: AnActionEvent)
    {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null && isSupportedFile(file)
        event.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread
    {
        return ActionUpdateThread.BGT
    }
}
