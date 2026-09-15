package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.model.CFMLAccessType
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

        val elementSet = elements.toHashSet()
        val isWholeFile = file in elementSet
        val model = CFMLPsiUtil.getModel(file)

        for (func in model.functions)
        {
            if (func.name.isEmpty()) continue
            val targetElement = file.findElementAt(func.nameRange.startOffset) ?: if (isWholeFile) file else continue
            if (isWholeFile || targetElement in elementSet)
            {
                val usageCount = model.getFunctionUsageCount(func)
                val tooltip = if (usageCount == 1) "1 use" else "$usageCount uses"

                val navHandler = GutterIconNavigationHandler<PsiElement>
                { _, _ ->
                    val funcElement = CFMLPsiUtil.getFunctionElement(file, func)
                    val findUsagesHandler = CFMLFindUsagesHandlerFactory().createFindUsagesHandler(funcElement, false)
                    if (findUsagesHandler != null)
                    {
                        val findManager = com.intellij.find.FindManager.getInstance(file.project)
                        findManager.findUsages(funcElement)
                    }
                }

                val icon = when (func.access)
                {
                    CFMLAccessType.PUBLIC -> CFMLIcon.FUNCTION_PUBLIC
                    CFMLAccessType.PRIVATE, CFMLAccessType.PACKAGE -> CFMLIcon.FUNCTION_PRIVATE
                    CFMLAccessType.REMOTE -> CFMLIcon.FUNCTION_REMOTE
                }

                val marker = LineMarkerInfo(
                    targetElement,
                    targetElement.textRange,
                    icon,
                    { tooltip },
                    navHandler,
                    GutterIconRenderer.Alignment.LEFT,
                    { tooltip }
                )
                result.add(marker)
            }
        }
    }
}
