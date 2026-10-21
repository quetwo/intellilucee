package com.quetwo.intellilucee.debugger

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import com.quetwo.intellilucee.file.CFMLFileType

class CFMLDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {

    override fun getFileType(): FileType = CFMLFileType.INSTANCE

    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile {
        return PsiFileFactory.getInstance(project).createFileFromText("evaluation.cfm", CFMLFileType.INSTANCE, text)
    }
}
