package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.quetwo.intellilucee.psi.CFMLPsiUtil

class CFMLFunctionUsageInlayHintsProvider : InlayHintsProvider
{

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector?
    {
        if (!CFMLPsiUtil.isCFMLFile(file)) return null

        return object : SharedBypassCollector
        {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink)
            {
                if (element != file) return
                val model = CFMLPsiUtil.getModel(file)
                for (func in model.functions)
                {
                    val count = model.getFunctionUsageCount(func)
                    val text = if (count == 1) "1 use" else "$count uses"
                    sink.addPresentation(
                        InlineInlayPosition(func.nameRange.endOffset, true),
                        null,
                        null,
                        HintFormat.default
                    ) {
                        text(" ($text)")
                    }
                }
            }
        }
    }
}
