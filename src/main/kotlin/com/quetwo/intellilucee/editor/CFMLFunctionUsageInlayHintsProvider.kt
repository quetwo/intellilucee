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
                val commentRanges = model.commentRanges.ifEmpty { CFMLModelParser.findCommentRanges(fileText) }
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
         * - Always after the text on the first line of the function declaration.
         * - If the curly brace '{' is on the first line (outside strings and comments), before '{'.
         * - If the curly brace '{' is on a subsequent line (or absent), at the end of the text on the first line.
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

            // Find the end of the first line of the declaration (relative to name / decl)
            val len = text.length
            var firstLineEnd = len
            var lineScan = nameEnd
            while (lineScan < len)
            {
                val c = text[lineScan]
                if (c == '\n' || c == '\r')
                {
                    firstLineEnd = lineScan
                    break
                }
                lineScan++
            }

            if (isTagFunc)
            {
                // Find closing '>' on the first line or overall opening tag
                var inSingle = false
                var inDouble = false
                val tagStart = if (declStart < text.length && text[declStart] == '<') declStart else text.lastIndexOf("<cffunction", nameEnd, ignoreCase = true).coerceAtLeast(0)
                var i = tagStart
                while (i < firstLineEnd)
                {
                    val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
                    if (commentEnd > i)
                    {
                        i = commentEnd.coerceAtMost(firstLineEnd)
                        continue
                    }
                    val c = text[i]
                    if (inSingle)
                    {
                        if (c == '\'') inSingle = false
                        i++
                        continue
                    }
                    if (inDouble)
                    {
                        if (c == '"') inDouble = false
                        i++
                        continue
                    }
                    if (c == '\'')
                    {
                        inSingle = true
                        i++
                        continue
                    }
                    if (c == '"')
                    {
                        inDouble = true
                        i++
                        continue
                    }
                    if (c == '>')
                    {
                        return i
                    }
                    i++
                }

                // If '>' not on first line, place at the end of the text on the first line
                var offset = firstLineEnd
                while (offset > nameEnd && (text[offset - 1] == ' ' || text[offset - 1] == '\t'))
                {
                    offset--
                }
                return offset
            }

            // 2. Script function: Scan the first line of the declaration
            var inSingle = false
            var inDouble = false
            var parenDepth = 0
            var braceOnFirstLine = -1
            var commentStartOnFirstLine = -1
            var i = nameEnd

            while (i < firstLineEnd)
            {
                val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
                if (commentEnd > i)
                {
                    if (commentStartOnFirstLine < 0)
                    {
                        commentStartOnFirstLine = i
                    }
                    i = commentEnd.coerceAtMost(firstLineEnd)
                    continue
                }

                // Check for line comment // or /* starting on this line
                if (!inSingle && !inDouble && i + 1 < firstLineEnd && text[i] == '/' && (text[i + 1] == '/' || text[i + 1] == '*'))
                {
                    if (commentStartOnFirstLine < 0)
                    {
                        commentStartOnFirstLine = i
                    }
                    break
                }

                val c = text[i]
                if (inSingle)
                {
                    if (c == '\'') inSingle = false
                    i++
                    continue
                }
                if (inDouble)
                {
                    if (c == '"') inDouble = false
                    i++
                    continue
                }
                if (c == '\'')
                {
                    inSingle = true
                    i++
                    continue
                }
                if (c == '"')
                {
                    inDouble = true
                    i++
                    continue
                }

                if (c == '(')
                {
                    parenDepth++
                }
                else if (c == ')')
                {
                    if (parenDepth > 0) parenDepth--
                }
                else if (c == '{' && parenDepth == 0)
                {
                    braceOnFirstLine = i
                    break
                }
                else if (c == ';' && parenDepth == 0)
                {
                    firstLineEnd = i
                    break
                }
                i++
            }

            if (braceOnFirstLine >= 0)
            {
                // Curly brace '{' is on the first line: place before '{'
                var beforeBrace = braceOnFirstLine
                while (beforeBrace > nameEnd && (text[beforeBrace - 1] == ' ' || text[beforeBrace - 1] == '\t'))
                {
                    beforeBrace--
                }
                return beforeBrace
            }

            // Curly brace is on a subsequent line, absent, or multiline:
            // Place after the text on the first line of the declaration
            val endLimit = if (commentStartOnFirstLine in nameEnd until firstLineEnd) commentStartOnFirstLine else firstLineEnd
            var offset = endLimit
            while (offset > nameEnd && (text[offset - 1] == ' ' || text[offset - 1] == '\t'))
            {
                offset--
            }
            return offset
        }
    }
}
