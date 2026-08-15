package com.quetwo.intellilucee.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import org.eclipse.lsp4j.ClientCapabilities

class CFMLLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "CFML")
{
    override fun isSupportedFile(file: VirtualFile): Boolean
    {
        return (file.extension == "cfm") || (file.extension == "cfc") || (file.extension == "cfs") || (file.extension == "cfml")
    }

    override fun createCommandLine(): GeneralCommandLine
    {
        val gc:GeneralCommandLine = GeneralCommandLine("d:\\luceedev\\cfmleditor-lsp.exe");
        //gc.setWorkDirectory()
        return gc;
    }

    override val clientCapabilities: ClientCapabilities
        get() = super.clientCapabilities.apply{
            textDocument.diagnostic = null
        }
}