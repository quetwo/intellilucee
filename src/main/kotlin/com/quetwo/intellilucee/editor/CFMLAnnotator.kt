package com.quetwo.intellilucee.editor

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.parser.CFMLTokenTypes
import com.quetwo.intellilucee.psi.CFMLPsiFile
import com.quetwo.intellilucee.settings.CFMLGlobalSettings
import java.util.regex.Pattern

class CFMLAnnotator : Annotator
{

    companion object
    {
        private val VOID_TAGS = setOf(
            "cfset", "cfelse", "cfelseif", "cfparam", "cfinclude", "cfabort", "cfreturn", "cfbreak", "cfcontinue",
            "cfargument", "cfproperty", "cfdump", "cfheader", "cflocation", "cfthrow", "cfrethrow",
            "cfdirectory", "cffile", "cfmailparam", "cfhttpparam", "cfprocparam", "cfprocresult", "cfqueryparam",
            "cfpop", "cffeed", "cfftp", "cfimage", "cfcontent", "cfcookie", "cfsetting",
            "cfassociate", "cfregistry", "cfschedule", "cfcollection", "cfindex", "cfsearch",
            "cfchartdata", "cfapplication"
        )

        private val EXPRESSION_TAGS = setOf(
            "cfset", "cfif", "cfelseif", "cfreturn", "cfbreak", "cfcontinue", "cfabort", "cfthrow", "cfrethrow"
        )

        /**
         * Tags that evaluate/process hash expressions (#...#) within their body content.
         */
        private val VARIABLE_PROCESSING_BODY_TAGS = setOf(
            "cfoutput", "cfquery", "cfmail", "cfsavecontent", "cfxml", "cfdocument",
            "cfdocumentsection", "cfdocumentitem", "cfpdf", "cfreport", "cfchart",
            "cfchartseries", "cfchartdata", "cftable", "cfgrid", "cftree", "cfpop",
            "cfldap", "cfhttp", "cfzip", "cffile", "cfdirectory", "cfregistry", "cflogin",
            "cfstoredproc", "cfqueryparam", "cftransaction", "cflock", "cfthread", "cfscript"
        )

        private val TAG_PATTERN = Pattern.compile(
            """(?:</\s*([a-zA-Z0-9_:\-]+)\s*>)|(?:<([a-zA-Z0-9_:\-]+)((?:[^'">]|"[^"]*"|'[^']*')*?)(\/?>))"""
        )

        private val ATTRIBUTE_PATTERN = Pattern.compile(
            """\b([a-zA-Z0-9_:\-]+)\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]+)"""
        )
    }

    private data class TagInfo(
        val name: String,
        val rawName: String,
        val range: TextRange,
        val isCustomTag: Boolean = false
    )

    override fun annotate(element: PsiElement, holder: AnnotationHolder)
    {
        val globalSettings = ApplicationManager.getApplication()?.getService(CFMLGlobalSettings::class.java)
        if (globalSettings != null && !globalSettings.state.syntaxAndErrorHighlighting)
        {
            return
        }

        if (element is CFMLPsiFile)
        {
            annotateFile(element, holder)
            return
        }

        val elementType = element.node?.elementType
        if (elementType == TokenType.BAD_CHARACTER || elementType == CFMLTokenTypes.BAD_CHARACTER)
        {
            holder.newAnnotation(HighlightSeverity.ERROR, "Unexpected character: '${element.text}'")
                .range(element.textRange)
                .create()
        }
    }

    private fun annotateFile(file: CFMLPsiFile, holder: AnnotationHolder)
    {
        val text = file.text ?: return
        val commentRanges = CFMLModelParser.findCommentRanges(text)
        val stringRanges = CFMLModelParser.findStringRanges(text, commentRanges)

        // 1. Semantic Syntax Highlighting & Function Error Validation via ModelParser
        val model = CFMLModelParser.parse(text)

        for (func in model.functions)
        {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(func.nameRange)
                .textAttributes(CFMLSyntaxHighlighter.FUNCTION_DECLARATION)
                .create()

            val seenParams = mutableSetOf<String>()
            for (param in func.parameters)
            {
                val lowerParam = param.name.lowercase()
                if (!seenParams.add(lowerParam))
                {
                    holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate argument '${param.name}' in function '${func.name}'")
                        .range(param.nameRange)
                        .create()
                }
            }
        }

        for (call in model.functionCalls)
        {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(call.range)
                .textAttributes(CFMLSyntaxHighlighter.FUNCTION_CALL)
                .create()
        }

        // 2. Hash interpolation validation inside variable-processing contexts
        validateHashExpressions(text, commentRanges, stringRanges, holder)

        // 3. Tag Matching and Tag Structural Error Validation
        validateTags(text, commentRanges, stringRanges, holder)
    }

    private fun validateHashExpressions(
        text: String,
        commentRanges: List<TextRange>,
        stringRanges: List<TextRange>,
        holder: AnnotationHolder
    )
    {
        val variableRanges = findVariableProcessingRanges(text, commentRanges, stringRanges)

        for (range in variableRanges)
        {
            validateSegmentHashes(text, range, commentRanges, holder)
        }
    }

    private fun findVariableProcessingRanges(
        text: String,
        commentRanges: List<TextRange>,
        stringRanges: List<TextRange>
    ): List<TextRange>
    {
        val ranges = mutableListOf<TextRange>()
        val tagStack = ArrayDeque<Pair<String, Int>>() // Pair(tagName, bodyStartOffset)
        val matcher = TAG_PATTERN.matcher(text)

        var isScriptFile = !text.contains("<cf", ignoreCase = true) && !text.contains("<html", ignoreCase = true)

        if (isScriptFile)
        {
            // Entire file is script
            return listOf(TextRange(0, text.length))
        }

        while (matcher.find())
        {
            val start = matcher.start()
            val end = matcher.end()

            if (CFMLModelParser.isInsideRanges(start, commentRanges) || CFMLModelParser.isInsideRanges(start, stringRanges))
            {
                continue
            }

            val closingTagName = matcher.group(1)
            val openingTagName = matcher.group(2)

            if (closingTagName != null)
            {
                val tagName = closingTagName.lowercase()
                if (tagStack.isNotEmpty())
                {
                    val last = tagStack.last()
                    if (last.first.equals(tagName, ignoreCase = true))
                    {
                        tagStack.removeLast()
                        if (VARIABLE_PROCESSING_BODY_TAGS.contains(tagName))
                        {
                            if (start > last.second)
                            {
                                ranges.add(TextRange(last.second, start))
                            }
                        }
                    }
                    else
                    {
                        val matchIndex = tagStack.indexOfLast { it.first.equals(tagName, ignoreCase = true) }
                        if (matchIndex != -1)
                        {
                            val matched = tagStack[matchIndex]
                            while (tagStack.size > matchIndex)
                            {
                                tagStack.removeLast()
                            }
                            if (VARIABLE_PROCESSING_BODY_TAGS.contains(tagName))
                            {
                                if (start > matched.second)
                                {
                                    ranges.add(TextRange(matched.second, start))
                                }
                            }
                        }
                    }
                }
            }
            else if (openingTagName != null)
            {
                val tagName = openingTagName.lowercase()
                val closingSlash = matcher.group(4) ?: ""
                val isSelfClosing = closingSlash == "/>" || VOID_TAGS.contains(tagName)

                // Attributes of tags (e.g. <cfset x = "#val#">, <a href="#url#">) always process variables/hashes
                ranges.add(TextRange(start, end))

                if (!isSelfClosing)
                {
                    tagStack.addLast(Pair(tagName, end))
                }
            }
        }

        return ranges
    }

    private fun validateSegmentHashes(
        text: String,
        range: TextRange,
        commentRanges: List<TextRange>,
        holder: AnnotationHolder
    )
    {
        var i = range.startOffset
        val end = range.endOffset
        var hashStart = -1

        while (i < end)
        {
            val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
            if (commentEnd > i)
            {
                i = commentEnd
                continue
            }

            val c = text[i]
            if (c == '#')
            {
                if (i + 1 < end && text[i + 1] == '#')
                {
                    i += 2
                    continue
                }
                if (hashStart == -1)
                {
                    hashStart = i
                }
                else
                {
                    hashStart = -1
                }
            }
            i++
        }

        if (hashStart != -1)
        {
            val errorRange = TextRange(hashStart, end)
            holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed hash expression")
                .range(errorRange)
                .create()
        }
    }

    private fun validateTags(
        text: String,
        commentRanges: List<TextRange>,
        stringRanges: List<TextRange>,
        holder: AnnotationHolder
    )
    {
        val tagStack = ArrayDeque<TagInfo>()
        val matcher = TAG_PATTERN.matcher(text)

        while (matcher.find())
        {
            val start = matcher.start()
            val end = matcher.end()

            if (CFMLModelParser.isInsideRanges(start, commentRanges) || CFMLModelParser.isInsideRanges(start, stringRanges))
            {
                continue
            }

            val closingTagName = matcher.group(1)
            val openingTagName = matcher.group(2)
            val tagRange = TextRange(start, end)

            if (closingTagName != null)
            {
                val rawName = closingTagName
                val tagName = rawName.lowercase()
                if (!tagName.startsWith("cf") && !tagName.contains(":"))
                {
                    continue
                }

                val isCustomTag = tagName.startsWith("cf_")

                if (tagStack.isEmpty())
                {
                    if (!isCustomTag)
                    {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing tag '</$rawName>'")
                            .range(tagRange)
                            .create()
                    }
                }
                else if (tagStack.last().name.equals(tagName, ignoreCase = true))
                {
                    tagStack.removeLast()
                }
                else
                {
                    val matchIndex = tagStack.indexOfLast { it.name.equals(tagName, ignoreCase = true) }
                    if (matchIndex != -1)
                    {
                        while (tagStack.size > matchIndex + 1)
                        {
                            val unclosed = tagStack.removeLast()
                            if (!unclosed.isCustomTag)
                            {
                                holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed tag '<${unclosed.rawName}>'")
                                    .range(unclosed.range)
                                    .create()
                            }
                        }
                        tagStack.removeLast()
                    }
                    else
                    {
                        // Check if previous tags on stack are custom tags that weren't closed
                        // If we can pop preceding custom tags and find a match for this closing tag, pop them without error
                        var foundMatchingAncestor = false
                        // Check if all intervening tags between current and a matching ancestor are custom tags
                        val potentialIndex = tagStack.indexOfLast { it.name.equals(tagName, ignoreCase = true) }
                        if (potentialIndex != -1 && (potentialIndex + 1 until tagStack.size).all { tagStack[it].isCustomTag })
                        {
                            while (tagStack.size > potentialIndex)
                            {
                                tagStack.removeLast()
                            }
                            foundMatchingAncestor = true
                        }

                        if (!foundMatchingAncestor)
                        {
                            if (!isCustomTag)
                            {
                                val expected = tagStack.last().rawName
                                holder.newAnnotation(HighlightSeverity.ERROR, "Mismatched closing tag '</$rawName>', expected '</$expected>'")
                                    .range(tagRange)
                                    .create()
                            }
                        }
                    }
                }
            }
            else if (openingTagName != null)
            {
                val rawName = openingTagName
                val tagName = rawName.lowercase()
                if (!tagName.startsWith("cf") && !tagName.contains(":"))
                {
                    continue
                }

                val isCustomTag = tagName.startsWith("cf_")
                val attributes = matcher.group(3) ?: ""
                val closingSlash = matcher.group(4) ?: ""
                val isSelfClosing = closingSlash == "/>" || VOID_TAGS.contains(tagName)

                // Check context constraints
                when (tagName)
                {
                    "cfelse", "cfelseif" ->
                    {
                        if (tagStack.none { it.name == "cfif" })
                        {
                            holder.newAnnotation(HighlightSeverity.ERROR, "'<$rawName>' must be inside a '<cfif>' tag")
                                .range(tagRange)
                                .create()
                        }
                    }
                    "cfcatch" ->
                    {
                        if (tagStack.none { it.name == "cftry" })
                        {
                            holder.newAnnotation(HighlightSeverity.ERROR, "'<cfcatch>' must be inside a '<cftry>' tag")
                                .range(tagRange)
                                .create()
                        }
                    }
                    "cfcase", "cfdefaultcase" ->
                    {
                        if (tagStack.none { it.name == "cfswitch" })
                        {
                            holder.newAnnotation(HighlightSeverity.ERROR, "'<$rawName>' must be inside a '<cfswitch>' tag")
                                .range(tagRange)
                                .create()
                        }
                    }
                    "cfargument" ->
                    {
                        if (tagStack.none { it.name == "cffunction" })
                        {
                            holder.newAnnotation(HighlightSeverity.ERROR, "'<cfargument>' must be inside a '<cffunction>' tag")
                                .range(tagRange)
                                .create()
                        }
                    }
                }

                // Check required attributes
                if (tagName == "cffunction" || tagName == "cfargument" || tagName == "cfparam")
                {
                    val hasName = attributes.contains(Regex("""\bname\s*=""", RegexOption.IGNORE_CASE))
                    if (!hasName)
                    {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Tag '<$rawName>' requires a 'name' attribute")
                            .range(tagRange)
                            .create()
                    }
                }

                // Check duplicate attributes
                if (!EXPRESSION_TAGS.contains(tagName))
                {
                    val attrMatcher = ATTRIBUTE_PATTERN.matcher(attributes)
                    val seenAttrs = mutableSetOf<String>()
                    while (attrMatcher.find())
                    {
                        val attrName = attrMatcher.group(1).lowercase()
                        if (!seenAttrs.add(attrName))
                        {
                            val attrOffset = matcher.start(3) + attrMatcher.start()
                            val attrRange = TextRange(attrOffset, attrOffset + attrMatcher.group(1).length)
                            holder.newAnnotation(HighlightSeverity.WARNING, "Duplicate attribute '${attrMatcher.group(1)}'")
                                .range(attrRange)
                                .create()
                        }
                    }
                }

                if (!isSelfClosing)
                {
                    tagStack.addLast(TagInfo(tagName, rawName, tagRange, isCustomTag))
                }
            }
        }

        // Any remaining unclosed tags
        for (unclosed in tagStack)
        {
            if (!unclosed.isCustomTag)
            {
                holder.newAnnotation(HighlightSeverity.ERROR, "Unclosed tag '<${unclosed.rawName}>'")
                    .range(unclosed.range)
                    .create()
            }
        }
    }
}
