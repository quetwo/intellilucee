package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLGotoDeclarationHandler : GotoDeclarationHandler
{

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor): Array<PsiElement>?
    {
        val file = sourceElement?.containingFile ?: return null
        if (!CFMLPsiUtil.isCFMLFile(file)) return null

        val target = CFMLPsiUtil.resolveSymbolAt(file, offset) ?: return null
        return arrayOf(target)
    }
}
