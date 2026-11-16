package com.quetwo.intellilucee.editor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.quetwo.intellilucee.editor.rest.CFMLAddRestEndpointDialog
import com.quetwo.intellilucee.editor.rest.CFMLRestEndpointGenerator

class CFMLAddRestEndpointAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!"cfc".equals(file.extension, ignoreCase = true)) {
            return
        }

        val dialog = CFMLAddRestEndpointDialog(project)
        if (!dialog.showAndGet()) {
            return
        }

        val descriptor = dialog.getEndpointDescriptor()
        val document = editor.document
        WriteCommandAction.runWriteCommandAction(project, "Add REST Endpoint", null, {
            CFMLRestEndpointGenerator.insertEndpoint(document, descriptor)
        })
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)

        var enabled = false
        if (project != null && editor != null && file != null) {
            if ("cfc".equals(file.extension, ignoreCase = true)) {
                val text = editor.document.text
                enabled = CFMLRestEndpointGenerator.isRestComponent(text)
            }
        }
        event.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
