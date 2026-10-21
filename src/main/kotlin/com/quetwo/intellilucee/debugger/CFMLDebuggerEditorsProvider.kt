package com.quetwo.intellilucee.debugger

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.quetwo.intellilucee.file.CFMLFileType

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class CFMLDebuggerEditorsProvider : XDebuggerEditorsProvider() {

    override fun getFileType(): FileType = CFMLFileType.INSTANCE

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun createDocument(
        project: Project,
        text: String,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document {
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText("evaluation.cfm", CFMLFileType.INSTANCE, text)
        return PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: EditorFactory.getInstance().createDocument(text)
    }
}
