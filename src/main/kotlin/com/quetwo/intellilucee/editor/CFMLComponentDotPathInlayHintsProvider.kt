package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import com.quetwo.intellilucee.utils.PathUtils
import java.util.regex.Pattern

private val TAG_COMPONENT_PATTERN = Pattern.compile("""(?i)<cfcomponent\b""")
private val SCRIPT_COMPONENT_PATTERN = Pattern.compile("""(?i)(?:^|[\s;{}])(?:(?:abstract|final)\s+)?\b(component)\b""")

class CFMLComponentDotPathInlayHintsProvider : InlayHintsProvider
{

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector?
    {
        if (!CFMLPsiUtil.isCFCFile(file)) return null

        return object : SharedBypassCollector
        {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink)
            {
                if (element != file) return
                val virtualFile = file.virtualFile ?: file.originalFile.virtualFile ?: file.viewProvider.virtualFile
                val dotPath = PathUtils.PathToDotNotation(virtualFile) ?: return
                if (dotPath.isBlank()) return

                val offset = findComponentOffset(file.text)
                sink.addPresentation(
                    InlineInlayPosition(offset, true),
                    null,
                    null,
                    HintFormat.default)
                {
                    text("Path: ($dotPath)")
                }
            }
        }
    }

    fun findComponentOffset(text: String): Int
    {
        val commentRanges = CFMLModelParser.findCommentRanges(text)

        // 1. Tag component: <cfcomponent
        val tagMatcher = TAG_COMPONENT_PATTERN.matcher(text)
        while (tagMatcher.find())
        {
            val start = tagMatcher.start()
            if (!CFMLModelParser.isInsideRanges(start, commentRanges))
            {
                return tagMatcher.end()
            }
        }

        // 2. Script component: component
        val scriptMatcher = SCRIPT_COMPONENT_PATTERN.matcher(text)
        while (scriptMatcher.find())
        {
            val nameStart = scriptMatcher.start(1)
            if (!CFMLModelParser.isInsideRanges(nameStart, commentRanges))
            {
                return scriptMatcher.end(1)
            }
        }

        return 0
    }
}
