package com.quetwo.intellilucee.refactoring

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler
import com.quetwo.intellilucee.psi.CFMLNamedElement
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLRenameHandler : RenameHandler {

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val element = getTargetElement(dataContext)
        return element is CFMLNamedElement
    }

    override fun isRenaming(dataContext: DataContext): Boolean {
        return isAvailableOnDataContext(dataContext)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        val element = getTargetElement(dataContext) ?: return
        PsiElementRenameHandler.invoke(element, project, null, editor)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        val element = elements.firstOrNull() ?: getTargetElement(dataContext) ?: return
        PsiElementRenameHandler.invoke(element, project, null, null)
    }

    private fun getTargetElement(dataContext: DataContext): PsiElement? {
        val psiElement = com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT.getData(dataContext)
        if (psiElement is CFMLNamedElement) {
            return psiElement
        }

        val editor = com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR.getData(dataContext) ?: return null
        val file = com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
        if (!CFMLPsiUtil.isCFMLFile(file)) return null

        val offset = editor.caretModel.offset
        return CFMLPsiUtil.findDeclarationElementAt(file, offset)
            ?: CFMLPsiUtil.resolveSymbolAt(file, offset)
    }
}
