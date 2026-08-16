package com.quetwo.intellilucee.lsp

import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.eclipse.lsp4j.ClientCapabilities
import java.nio.file.Path

class CFMLLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "CFML")
{
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
            return pluginPath.resolve("/lsp/cfmleditor-lsp.exe")
        }
    }

    override fun isSupportedFile(file: VirtualFile): Boolean
    {
        return isSupportedExtension(file.extension)
    }

    override fun createCommandLine(): GeneralCommandLine
    {
        //return GeneralCommandLine(resolveLspExecutablePath().toString())
        return GeneralCommandLine("d:\\luceedev\\cfmleditor-lsp.exe")
    }

    override val clientCapabilities: ClientCapabilities
        get() = super.clientCapabilities.apply{
            //textDocument.diagnostic = null
        }


}