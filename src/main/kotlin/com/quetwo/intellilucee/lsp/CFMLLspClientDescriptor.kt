package com.quetwo.intellilucee.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import org.eclipse.lsp4j.ClientCapabilities
import java.nio.file.Path

class CFMLLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "CFML")
{
    private val LOG: Logger? = Logger.getInstance(CFMLLspClientDescriptor::class.java)

    companion object
    {
        private val SUPPORTED_EXTENSIONS = setOf("cfm", "cfc", "cfs", "cfml")

        fun isSupportedExtension(extension: String?): Boolean
        {
            return extension?.lowercase() in SUPPORTED_EXTENSIONS
        }

        private fun resolveLspExecutablePath(): Path
        {
            val pluginPath = PluginManagerCore.getPlugin(PluginId.getId("com.quetwo.IntelliLucee"))?.pluginPath
                ?: error("Unable to resolve IntelliLucee plugin path")
            val lspEXE = pluginPath.resolve("lsp\\cfmleditor-lsp.exe")
            LOG.info("plugin path detected as: ${lspEXE.toString()}")
            return lspEXE
        }
    }

    override fun isSupportedFile(file: VirtualFile): Boolean
    {
        return isSupportedExtension(file.extension)
    }

    override fun createCommandLine(): GeneralCommandLine
    {
        return GeneralCommandLine(resolveLspExecutablePath().toString())
    }

    override val clientCapabilities: ClientCapabilities
        get() = super.clientCapabilities.apply{
            //textDocument.diagnostic = null
        }


}