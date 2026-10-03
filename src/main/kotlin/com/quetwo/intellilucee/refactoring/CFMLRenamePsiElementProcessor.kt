package com.quetwo.intellilucee.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.quetwo.intellilucee.editor.CFMLFindUsagesHandlerFactory
import com.quetwo.intellilucee.psi.CFMLNamedElement

class CFMLRenamePsiElementProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        return element is CFMLNamedElement
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        if (element is CFMLNamedElement) {
            val handler = CFMLFindUsagesHandlerFactory().createFindUsagesHandler(element, false)
            if (handler != null) {
                return handler.findReferencesToHighlight(element, searchScope)
            }
        }
        return super.findReferences(element, searchScope, searchInCommentsAndStrings)
    }
}
