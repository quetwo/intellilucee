package com.quetwo.intellilucee.editor

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.quetwo.intellilucee.model.CFMLFunctionDeclaration
import com.quetwo.intellilucee.model.CFMLVariableDeclaration
import com.quetwo.intellilucee.psi.*

class CFMLFindUsagesHandlerFactory : FindUsagesHandlerFactory() {

    override fun canFindUsages(element: PsiElement): Boolean {
        if (element is CFMLNamedElement) return true
        val file = element.containingFile ?: return false
        if (!CFMLPsiUtil.isCFMLFile(file)) return false
        return CFMLPsiUtil.resolveSymbolAt(file, element.textRange.startOffset) != null
    }

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        val targetElement = if (element is CFMLNamedElement) {
            element
        } else {
            val file = element.containingFile ?: return null
            CFMLPsiUtil.resolveSymbolAt(file, element.textRange.startOffset) as? CFMLNamedElement ?: return null
        }
        return CFMLFindUsagesHandler(targetElement)
    }
}

class CFMLFindUsagesHandler(private val namedElement: CFMLNamedElement) : FindUsagesHandler(namedElement) {

    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Boolean {
        val file = namedElement.containingFile ?: return true
        val model = CFMLPsiUtil.getModel(file)

        when (val symbol = namedElement.symbol) {
            is CFMLFunctionDeclaration -> {
                val calls = model.findFunctionCalls(symbol.name)
                for (call in calls) {
                    val usageInfo = UsageInfo(file, call.range.startOffset, call.range.endOffset)
                    if (!processor.process(usageInfo)) {
                        return false
                    }
                }
            }
            is CFMLVariableDeclaration -> {
                val usages = model.findVariableUsages(symbol)
                for (u in usages) {
                    val usageInfo = UsageInfo(file, u.range.startOffset, u.range.endOffset)
                    if (!processor.process(usageInfo)) {
                        return false
                    }
                }
            }
            else -> {}
        }
        return true
    }

    override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {
        val file = namedElement.containingFile ?: return emptyList()
        val model = CFMLPsiUtil.getModel(file)
        val refs = mutableListOf<PsiReference>()

        when (val symbol = namedElement.symbol) {
            is CFMLFunctionDeclaration -> {
                val calls = model.findFunctionCalls(symbol.name)
                for (call in calls) {
                    refs.add(CFMLPsiReference(file, call.range, namedElement))
                }
            }
            is CFMLVariableDeclaration -> {
                val usages = model.findVariableUsages(symbol)
                for (u in usages) {
                    refs.add(CFMLPsiReference(file, u.range, namedElement))
                }
            }
            else -> {}
        }
        return refs
    }
}
