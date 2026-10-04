package com.quetwo.intellilucee.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

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

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element is CFMLNamedElement) {
            val resolved = resolve()
            if (resolved is CFMLNamedElement) {
                return resolved.isEquivalentTo(element)
            }
        }
        return super.isReferenceTo(element)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val file = element.containingFile ?: return element
        val startOffset = element.textRange.startOffset + rangeInElement.startOffset
        val endOffset = element.textRange.startOffset + rangeInElement.endOffset

        val document = file.viewProvider.document
            ?: PsiDocumentManager.getInstance(file.project).getDocument(file)
            ?: return element

        document.replaceString(startOffset, endOffset, newElementName)
        PsiDocumentManager.getInstance(file.project).commitDocument(document)

        return element
    }
}
