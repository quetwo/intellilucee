package com.quetwo.intellilucee.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PluginPathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.quetwo.intellilucee.settings.CFMLGlobalSettings
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.PROJECT)
class CFMLMcpProjectService(private val project: Project) : Disposable
{
    private val generation = AtomicLong()

    fun refresh()
    {
        val requestGeneration = generation.incrementAndGet()
        val workspace = project.basePath?.let {
            try
            {
                Path.of(it).toAbsolutePath().normalize()
            }
            catch (_: Exception)
            {
                null
            }
        } ?: return
        val settings = CFMLGlobalSettings.getInstance().state
        val enabled = settings.exposeProjectViaMcp
        val releaseVersion = settings.lspReleaseVersion.trim().ifEmpty { CFMLLspReleaseProvider.LATEST }

        ApplicationManager.getApplication().executeOnPooledThread {
            try
            {
                if (!isCurrent(requestGeneration, workspace, enabled, releaseVersion))
                {
                    return@executeOnPooledThread
                }

                val executable = if (enabled) CFMLLspClientDescriptor.resolveLspExecutablePath(project) else null
                if (!isCurrent(requestGeneration, workspace, enabled, releaseVersion))
                {
                    return@executeOnPooledThread
                }

                updateConfiguration(workspace, executable, requestGeneration, enabled, releaseVersion)
            }
            catch (exception: Exception)
            {
                LOG.warn("Failed to update project MCP configuration for ${project.name}", exception)
            }
        }
    }

    private fun isCurrent(requestGeneration: Long, workspace: Path, enabled: Boolean, releaseVersion: String): Boolean
    {
        if (project.isDisposed || generation.get() != requestGeneration)
        {
            return false
        }

        val currentWorkspace = project.basePath?.let {
            try
            {
                Path.of(it).toAbsolutePath().normalize()
            }
            catch (_: Exception)
            {
                null
            }
        }
        val settings = CFMLGlobalSettings.getInstance().state
        return currentWorkspace == workspace &&
            settings.exposeProjectViaMcp == enabled &&
            settings.lspReleaseVersion.trim().ifEmpty { CFMLLspReleaseProvider.LATEST } == releaseVersion
    }

    private fun updateConfiguration(
        workspace: Path,
        executable: Path?,
        requestGeneration: Long,
        enabled: Boolean,
        releaseVersion: String
    )
    {
        val lspDirectory = PluginPathManager.getPluginHome("IntelliLucee").toPath().resolve("lsp").toAbsolutePath().normalize()
        val configFile = workspace.resolve(".junie").resolve("mcp").resolve("mcp.json")
        synchronized(fileLocks.computeIfAbsent(configFile) { Any() })
        {
            if (!isCurrent(requestGeneration, workspace, enabled, releaseVersion))
            {
                return
            }

            val existingJson = if (Files.isRegularFile(configFile)) Files.readString(configFile) else null
            val updatedJson = if (executable != null)
            {
                CFMLMcpConfiguration.upsert(existingJson, executable, workspace, lspDirectory)
            }
            else
            {
                CFMLMcpConfiguration.remove(existingJson, workspace, lspDirectory)
            }
            if (updatedJson != null && isCurrent(requestGeneration, workspace, enabled, releaseVersion))
            {
                writeAtomically(configFile, updatedJson)
            }
        }
    }

    private fun writeAtomically(configFile: Path, content: String)
    {
        val parent = configFile.parent
        Files.createDirectories(parent)
        val temporaryFile = Files.createTempFile(parent, "mcp.json-", ".tmp")
        try
        {
            Files.writeString(temporaryFile, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            try
            {
                Files.move(temporaryFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }
            catch (_: AtomicMoveNotSupportedException)
            {
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        finally
        {
            Files.deleteIfExists(temporaryFile)
        }
    }

    override fun dispose()
    {
        generation.incrementAndGet()
    }

    companion object
    {
        private val LOG = Logger.getInstance(CFMLMcpProjectService::class.java)
        private val fileLocks = ConcurrentHashMap<Path, Any>()

        fun refreshOpenProjects()
        {
            ProjectManager.getInstance().openProjects
                .filterNot { it.isDisposed }
                .forEach { it.getService(CFMLMcpProjectService::class.java).refresh() }
        }
    }
}

class CFMLMcpProjectActivity : com.intellij.openapi.startup.ProjectActivity
{
    override suspend fun execute(project: Project)
    {
        project.getService(CFMLMcpProjectService::class.java).refresh()
    }
}