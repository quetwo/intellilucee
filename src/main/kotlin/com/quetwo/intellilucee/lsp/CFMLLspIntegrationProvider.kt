package com.quetwo.intellilucee.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspIntegrationProvider

internal class CFMLLspIntegrationProvider : LspIntegrationProvider
{
    override fun fileOpened(project: Project, file: VirtualFile, clientStarter: LspIntegrationProvider.LspClientStarter)
    {
        if (file.extension == "cfm")
        {
            clientStarter.ensureClientStarted(CFMLLspClientDescriptor(project))
        }
    }

}