package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLFunctionUsageLineMarkerProvider : LineMarkerProvider
{

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>?
    {
        val file = element.containingFile ?: return null
        if (!CFMLPsiUtil.isCFMLFile(file)) return null

        // Only create line marker for the root/first element or file element that covers the functions
        // To avoid duplicate markers for nested AST nodes
        if (element.parent != file && element != file) return null

        return null
    }

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    )
    {
        if (elements.isEmpty()) return
        val file = elements.first().containingFile ?: return
        if (!CFMLPsiUtil.isCFMLFile(file)) return

        val model = CFMLPsiUtil.getModel(file)
        for (func in model.functions)
        {
            val usageCount = model.getFunctionUsageCount(func)
            val tooltip = if (usageCount == 1) "1 use" else "$usageCount uses"
            val targetElement = file.findElementAt(func.nameRange.startOffset) ?: file

            val navHandler = GutterIconNavigationHandler<PsiElement>
            { e, elt ->
                val funcElement = CFMLPsiUtil.getFunctionElement(file, func)
                val findUsagesHandler = CFMLFindUsagesHandlerFactory().createFindUsagesHandler(funcElement, false)
                if (findUsagesHandler != null)
                {
                    val findManager = com.intellij.find.FindManager.getInstance(file.project)
                    findManager.findUsages(funcElement)
                }
            }

            val marker = LineMarkerInfo(
                targetElement,
                func.nameRange,
                CFMLIcon.FILE,
                { tooltip },
                navHandler,
                GutterIconRenderer.Alignment.LEFT,
                { tooltip }
            )
            result.add(marker)
        }
    }
}
