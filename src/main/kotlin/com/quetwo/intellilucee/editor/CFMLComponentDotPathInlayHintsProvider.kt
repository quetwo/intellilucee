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

        if ((file.name.lowercase() == "application.cfc") || (file.name.lowercase() == "application.cfm"))
        {
            return null;
        }

        return object : SharedBypassCollector
        {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink)
            {
                if (element != file) return
                val virtualFile = file.virtualFile ?: file.originalFile.virtualFile ?: file.viewProvider.virtualFile
                val dotPath = PathUtils.PathToDotNotation(virtualFile) ?: return
                if (dotPath.isBlank()) return

                val model = CFMLPsiUtil.getModel(file)
                val commentRanges = model.commentRanges.ifEmpty { CFMLModelParser.findCommentRanges(file.text) }
                val offset = findComponentOffset(file.text, commentRanges)
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

    @JvmOverloads
    fun findComponentOffset(
        text: String,
        commentRanges: List<com.intellij.openapi.util.TextRange> = CFMLModelParser.findCommentRanges(text)
    ): Int
    {

        // 1. Tag component: <cfcomponent ... >
        val tagMatcher = TAG_COMPONENT_PATTERN.matcher(text)
        while (tagMatcher.find())
        {
            val start = tagMatcher.start()
            if (!CFMLModelParser.isInsideRanges(start, commentRanges))
            {
                // Find the closing '>' of the <cfcomponent ...> opening tag or end of line
                var i = tagMatcher.end()
                while (i < text.length)
                {
                    val c = text[i]
                    if (c == '>')
                    {
                        return i // right before '>' or end of tag attributes
                    }
                    if (c == '\n' || c == '\r')
                    {
                        return i
                    }
                    i++
                }
                return text.length
            }
        }

        // 2. Script component: component ... {
        val scriptMatcher = SCRIPT_COMPONENT_PATTERN.matcher(text)
        while (scriptMatcher.find())
        {
            val nameStart = scriptMatcher.start(1)
            if (!CFMLModelParser.isInsideRanges(nameStart, commentRanges))
            {
                var i = scriptMatcher.end(1)
                while (i < text.length)
                {
                    val c = text[i]
                    if (c == '{')
                    {
                        // Right before the opening curly brace (trim whitespace before '{' if desired, but before '{' is exact)
                        var beforeBrace = i
                        while (beforeBrace > scriptMatcher.end(1) && text[beforeBrace - 1].isWhitespace() && text[beforeBrace - 1] != '\n' && text[beforeBrace - 1] != '\r')
                        {
                            beforeBrace--
                        }
                        return beforeBrace
                    }
                    if (c == '\n' || c == '\r')
                    {
                        return i
                    }
                    i++
                }
                return text.length
            }
        }

        // If no component header found, place at end of first line or 0
        val lineEnd = text.indexOfAny(charArrayOf('\n', '\r'))
        if (lineEnd >= 0)
        {
            return lineEnd
        }
        return text.length
    }
}
