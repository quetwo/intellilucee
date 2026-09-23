package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.quetwo.intellilucee.model.CFMLFunctionDeclaration
import com.quetwo.intellilucee.model.CFMLModelParser
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
                if (model.functions.isEmpty()) return
                val fileText = file.text
                val commentRanges = CFMLModelParser.findCommentRanges(fileText)
                for (func in model.functions)
                {
                    if (func.name.isBlank()) continue
                    val count = model.getFunctionUsageCount(func)
                    val text = if (count == 1) "1 use" else "$count uses"
                    val offset = findFunctionInlayOffset(fileText, func, commentRanges)
                    sink.addPresentation(
                        InlineInlayPosition(offset, true),
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

    companion object
    {
        /**
         * Finds the offset where the function usages inlay hint should be placed:
         * - On the same line as the function declaration.
         * - If the curly brace '{' is on the same line, before '{', but after the list of arguments.
         * - If the curly brace '{' is on a subsequent line, after the list of arguments on the declaration line.
         */
        @JvmStatic
        @JvmOverloads
        fun findFunctionInlayOffset(
            text: String,
            func: CFMLFunctionDeclaration,
            commentRanges: List<com.intellij.openapi.util.TextRange> = CFMLModelParser.findCommentRanges(text)
        ): Int
        {
            val nameEnd = func.nameRange.endOffset.coerceAtMost(text.length)
            val declStart = func.range.startOffset.coerceAtMost(text.length)

            // 1. Check if it's a tag-based function (<cffunction ... >)
            var isTagFunc = false
            if (declStart < nameEnd && text.regionMatches(declStart, "<cffunction", 0, 11, ignoreCase = true))
            {
                isTagFunc = true
            }
            else
            {
                var k = nameEnd - 1
                while (k >= 0 && (text[k] == ' ' || text[k] == '\t' || text[k] == '\r' || text[k] == '\n'))
                {
                    k--
                }
                if (k >= 0 && text[k] == '>')
                {
                    isTagFunc = true
                }
            }

            if (isTagFunc)
            {
                // Find the closing '>' of the <cffunction> opening tag
                var i = nameEnd
                val len = text.length
                while (i < len)
                {
                    val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
                    if (commentEnd > i)
                    {
                        i = commentEnd
                        continue
                    }
                    val c = text[i]
                    if (c == '>')
                    {
                        return i
                    }
                    i++
                }
            }

            // 2. Script function: find opening '(' and matching closing ')' for arguments list
            var openParen = -1
            var i = nameEnd
            val len = text.length
            while (i < len)
            {
                val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
                if (commentEnd > i)
                {
                    i = commentEnd
                    continue
                }
                val c = text[i]
                if (c == '(')
                {
                    openParen = i
                    break
                }
                if (c == '{' || c == ';')
                {
                    break
                }
                i++
            }

            var afterArgs = nameEnd
            if (openParen >= 0)
            {
                var parenDepth = 0
                var j = openParen
                while (j < len)
                {
                    val commentEnd = CFMLModelParser.getCommentEndIfInside(j, commentRanges)
                    if (commentEnd > j)
                    {
                        j = commentEnd
                        continue
                    }
                    val c = text[j]
                    if (c == '(')
                    {
                        parenDepth++
                    }
                    else if (c == ')')
                    {
                        parenDepth--
                        if (parenDepth == 0)
                        {
                            afterArgs = j + 1
                            break
                        }
                    }
                    j++
                }
            }

            // 3. Scan the remainder of the line after arguments to see if '{' is on the same line
            var lineEnd = text.length
            var braceOnSameLine = -1
            var k = afterArgs
            while (k < len)
            {
                val commentEnd = CFMLModelParser.getCommentEndIfInside(k, commentRanges)
                if (commentEnd > k)
                {
                    k = commentEnd
                    continue
                }
                val c = text[k]
                if (c == '\n' || c == '\r')
                {
                    lineEnd = k
                    break
                }
                if (c == '{')
                {
                    braceOnSameLine = k
                    break
                }
                if (c == ';')
                {
                    lineEnd = k
                    break
                }
                k++
            }

            if (braceOnSameLine >= 0)
            {
                // Curly brace is on the same line: place before '{', but after arguments
                var beforeBrace = braceOnSameLine
                while (beforeBrace > afterArgs && (text[beforeBrace - 1] == ' ' || text[beforeBrace - 1] == '\t'))
                {
                    beforeBrace--
                }
                return beforeBrace
            }

            // Curly brace is on a new line or absent: place at end of declaration line, after arguments
            var offset = lineEnd
            while (offset > afterArgs && (text[offset - 1] == ' ' || text[offset - 1] == '\t'))
            {
                offset--
            }
            return offset
        }
    }
}
