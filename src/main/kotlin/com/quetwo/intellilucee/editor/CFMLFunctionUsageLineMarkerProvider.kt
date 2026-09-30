package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.model.CFMLAccessType
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLFunctionUsageLineMarkerProvider : LineMarkerProvider
{

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>?
    {
        val file = element.containingFile ?: return null
        if (!CFMLPsiUtil.isCFMLFile(file)) return null

        // Line marker must be anchored to a leaf PSI element
        if (element.firstChild != null) return null

        val model = CFMLPsiUtil.getModel(file)
        if (model.functions.isEmpty()) return null

        val elemRange = element.textRange

        // Find the function declaration whose name identifier matches this leaf element
        val func = model.functions.firstOrNull { f ->
            f.name.isNotEmpty() &&
            f.nameRange.startOffset >= elemRange.startOffset &&
            f.nameRange.startOffset <= elemRange.endOffset &&
            file.findElementAt(f.nameRange.startOffset) == element
        } ?: return null

        // Only show at most one function accessor indicator per document line
        val document = file.viewProvider.document
        if (document != null && func.nameRange.startOffset <= document.textLength)
        {
            val currentLine = document.getLineNumber(func.nameRange.startOffset)
            val firstFuncOnLine = model.functions.firstOrNull { f ->
                f.name.isNotEmpty() &&
                f.nameRange.startOffset <= document.textLength &&
                document.getLineNumber(f.nameRange.startOffset) == currentLine
            }
            if (firstFuncOnLine != null && firstFuncOnLine != func)
            {
                return null
            }
        }

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

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltip },
            navHandler,
            GutterIconRenderer.Alignment.LEFT,
            { tooltip }
        )
    }

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    )
    {
        // Line markers for functions are provided via getLineMarkerInfo (fast pass).
        // Leaving collectSlowLineMarkers empty prevents duplicate line markers in the gutter.
    }
}
