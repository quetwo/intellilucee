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
                val fileText = file.text
                for (func in model.functions)
                {
                    if (func.name.isBlank()) continue
                    val count = model.getFunctionUsageCount(func)
                    val text = if (count == 1) "1 use" else "$count uses"
                    val offset = findFunctionInlayOffset(fileText, func)
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
        fun findFunctionInlayOffset(text: String, func: CFMLFunctionDeclaration): Int
        {
            val commentRanges = CFMLModelParser.findCommentRanges(text)
            val nameEnd = func.nameRange.endOffset.coerceAtMost(text.length)
            val declStart = func.range.startOffset.coerceAtMost(text.length)

            // 1. Check if it's a tag-based function (<cffunction ... >)
            val prefix = if (declStart < nameEnd) text.substring(declStart, nameEnd) else ""
            if (prefix.contains("<cffunction", ignoreCase = true) || text.substring(0, nameEnd).trimEnd().endsWith(">"))
            {
                // Find the closing '>' of the <cffunction> opening tag
                for (i in nameEnd until text.length)
                {
                    if (CFMLModelParser.isInsideRanges(i, commentRanges)) continue
                    val c = text[i]
                    if (c == '>')
                    {
                        return i
                    }
                }
            }

            // 2. Script function: find opening '(' and matching closing ')' for arguments list
            var openParen = -1
            for (i in nameEnd until text.length)
            {
                if (CFMLModelParser.isInsideRanges(i, commentRanges)) continue
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
            }

            var afterArgs = nameEnd
            if (openParen >= 0)
            {
                var parenDepth = 0
                for (i in openParen until text.length)
                {
                    if (CFMLModelParser.isInsideRanges(i, commentRanges)) continue
                    val c = text[i]
                    if (c == '(')
                    {
                        parenDepth++
                    }
                    else if (c == ')')
                    {
                        parenDepth--
                        if (parenDepth == 0)
                        {
                            afterArgs = i + 1
                            break
                        }
                    }
                }
            }

            // 3. Scan the remainder of the line after arguments to see if '{' is on the same line
            var lineEnd = text.length
            var braceOnSameLine = -1
            for (i in afterArgs until text.length)
            {
                if (CFMLModelParser.isInsideRanges(i, commentRanges)) continue
                val c = text[i]
                if (c == '\n' || c == '\r')
                {
                    lineEnd = i
                    break
                }
                if (c == '{')
                {
                    braceOnSameLine = i
                    break
                }
                if (c == ';')
                {
                    lineEnd = i
                    break
                }
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
