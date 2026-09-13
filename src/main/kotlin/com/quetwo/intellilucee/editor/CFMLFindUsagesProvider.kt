package com.quetwo.intellilucee.editor

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.quetwo.intellilucee.parser.CFMLLexer
import com.quetwo.intellilucee.parser.CFMLTokenTypes
import com.quetwo.intellilucee.psi.CFMLFunctionElement
import com.quetwo.intellilucee.psi.CFMLNamedElement
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import com.quetwo.intellilucee.psi.CFMLVariableElement

class CFMLFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            CFMLLexer(),
            TokenSet.create(CFMLTokenTypes.IDENTIFIER, CFMLTokenTypes.TEXT),
            TokenSet.create(CFMLTokenTypes.COMMENT),
            TokenSet.create(CFMLTokenTypes.STRING)
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean {
        if (psiElement is CFMLNamedElement) return true
        val file = psiElement.containingFile ?: return false
        if (!CFMLPsiUtil.isCFMLFile(file)) return false
        return CFMLPsiUtil.resolveSymbolAt(file, psiElement.textRange.startOffset) != null
    }

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is CFMLFunctionElement -> "function"
            is CFMLVariableElement -> "variable"
            else -> {
                val file = element.containingFile ?: return "element"
                val resolved = CFMLPsiUtil.resolveSymbolAt(file, element.textRange.startOffset)
                when (resolved) {
                    is CFMLFunctionElement -> "function"
                    is CFMLVariableElement -> "variable"
                    else -> "element"
                }
            }
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        if (element is CFMLNamedElement) return element.name
        val file = element.containingFile ?: return element.text
        val resolved = CFMLPsiUtil.resolveSymbolAt(file, element.textRange.startOffset)
        if (resolved is CFMLNamedElement) return resolved.name
        return element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return getDescriptiveName(element)
    }
}
