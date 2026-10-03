package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.quetwo.intellilucee.psi.CFMLNamedElement
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLTargetElementEvaluator : TargetElementEvaluatorEx2() {

    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? {
        return ref.resolve()
    }

    override fun getNamedElement(element: PsiElement): PsiElement? {
        if (element is CFMLNamedElement) return element
        val file = element.containingFile ?: return null
        if (!CFMLPsiUtil.isCFMLFile(file)) return null
        val offset = element.textRange.startOffset
        return CFMLPsiUtil.findDeclarationElementAt(file, offset)
            ?: CFMLPsiUtil.resolveSymbolAt(file, offset)
    }
}
