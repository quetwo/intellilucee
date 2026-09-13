package com.quetwo.intellilucee.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.IncorrectOperationException

class CFMLPsiReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val targetElement: PsiElement? = null
) : PsiReferenceBase<PsiElement>(element, rangeInElement, false) {

    override fun resolve(): PsiElement? {
        if (targetElement != null) return targetElement
        val file = element.containingFile ?: return null
        return CFMLPsiUtil.resolveSymbolAt(file, element.textRange.startOffset + rangeInElement.startOffset)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        throw IncorrectOperationException("Rename is not supported")
    }
}
