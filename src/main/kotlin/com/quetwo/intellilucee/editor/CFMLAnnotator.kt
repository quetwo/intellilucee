package com.quetwo.intellilucee.editor

import com.intellij.lang.Language
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
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
            "cfoutput", "cfmail", "cfsavecontent", "cfxml", "cfdocument",
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

        private val SQL_KEYWORDS = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "SET", "DELETE",
            "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON",
            "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET",
            "UNION", "ALL", "DISTINCT", "AS", "AND", "OR", "NOT", "IN", "IS",
            "NULL", "LIKE", "BETWEEN", "EXISTS", "CASE", "WHEN", "THEN", "ELSE",
            "END", "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "DATABASE",
            "SCHEMA", "VALUES", "EXEC", "EXECUTE", "DECLARE", "BEGIN", "COMMIT",
            "ROLLBACK", "TRANSACTION", "TRUNCATE", "GRANT", "REVOKE", "WITH", "TOP",
            "FETCH", "FIRST", "NEXT", "ROWS", "ONLY", "COUNT", "SUM", "AVG", "MIN", "MAX",
            "COALESCE", "CAST", "CONVERT"
        )
    }

    private data class TagInfo(
        val name: String,
        val rawName: String,
        val range: TextRange,
        val isCustomTag: Boolean = false
    )

    private data class QueryBlock(
        val bodyRange: TextRange
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

        // 4. Embedded SQL Highlighting & SQL Injection Error Validation for <cfquery>
        validateAndHighlightCfquery(file, text, commentRanges, stringRanges, holder)
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

    private fun validateAndHighlightCfquery(
        file: CFMLPsiFile,
        text: String,
        commentRanges: List<TextRange>,
        stringRanges: List<TextRange>,
        holder: AnnotationHolder
    )
    {
        val queryBlocks = findQueryBlocks(text, commentRanges, stringRanges)

        for (block in queryBlocks)
        {
            val bodyRange = block.bodyRange
            val bodyStart = bodyRange.startOffset
            val bodyEnd = bodyRange.endOffset
            if (bodyEnd <= bodyStart)
            {
                continue
            }

            val nestedTagMatcher = TAG_PATTERN.matcher(text)
            nestedTagMatcher.region(bodyStart, bodyEnd)

            val tagMarkupRanges = mutableListOf<TextRange>()
            val paramTagRanges = mutableListOf<TextRange>()
            val outputTagRanges = mutableListOf<TextRange>()

            while (nestedTagMatcher.find())
            {
                val tStart = nestedTagMatcher.start()
                val tEnd = nestedTagMatcher.end()
                if (CFMLModelParser.isInsideRanges(tStart, commentRanges))
                {
                    continue
                }

                val tagRange = TextRange(tStart, tEnd)
                tagMarkupRanges.add(tagRange)

                val closing = nestedTagMatcher.group(1)?.lowercase()
                val opening = nestedTagMatcher.group(2)?.lowercase()

                if (closing == "cfoutput" || opening == "cfoutput")
                {
                    outputTagRanges.add(tagRange)
                    holder.newAnnotation(HighlightSeverity.ERROR, "SQL Injection Possible")
                        .range(tagRange)
                        .create()
                }
                else if (opening == "cfqueryparam")
                {
                    paramTagRanges.add(tagRange)
                }
            }

            // Check for variable interpolation hashes (#...#) in the query body
            var i = bodyStart
            while (i < bodyEnd)
            {
                val commentEnd = CFMLModelParser.getCommentEndIfInside(i, commentRanges)
                if (commentEnd > i)
                {
                    i = commentEnd
                    continue
                }

                val paramRange = paramTagRanges.firstOrNull { it.startOffset <= i && i < it.endOffset }
                if (paramRange != null)
                {
                    i = maxOf(i + 1, paramRange.endOffset)
                    continue
                }

                val otherTagRange = tagMarkupRanges.firstOrNull { it.startOffset <= i && i < it.endOffset && !outputTagRanges.contains(it) }
                if (otherTagRange != null)
                {
                    i = maxOf(i + 1, otherTagRange.endOffset)
                    continue
                }

                val c = text[i]
                if (c == '#')
                {
                    if (i + 1 < bodyEnd && text[i + 1] == '#')
                    {
                        i += 2
                        continue
                    }

                    var hashEnd = -1
                    var j = i + 1
                    while (j < bodyEnd)
                    {
                        val cEnd = CFMLModelParser.getCommentEndIfInside(j, commentRanges)
                        if (cEnd > j)
                        {
                            j = cEnd
                            continue
                        }
                        if (text[j] == '#')
                        {
                            if (j + 1 < bodyEnd && text[j + 1] == '#')
                            {
                                j += 2
                                continue
                            }
                            hashEnd = j
                            break
                        }
                        j++
                    }

                    if (hashEnd != -1)
                    {
                        val hashExprRange = TextRange(i, hashEnd + 1)
                        holder.newAnnotation(HighlightSeverity.ERROR, "SQL Injection Possible")
                            .range(hashExprRange)
                            .create()
                        i = hashEnd + 1
                    }
                    else
                    {
                        val hashExprRange = TextRange(i, bodyEnd)
                        holder.newAnnotation(HighlightSeverity.ERROR, "SQL Injection Possible")
                            .range(hashExprRange)
                            .create()
                        break
                    }
                }
                else
                {
                    i++
                }
            }

            // Highlight SQL segments using embedded SQL lexer
            val nonSqlRanges = mutableListOf<TextRange>()
            for (cr in commentRanges)
            {
                val cText = text.substring(cr.startOffset, minOf(cr.endOffset, text.length))
                if (cText.startsWith("<!--"))
                {
                    val intersection = bodyRange.intersection(cr)
                    if (intersection != null && !intersection.isEmpty)
                    {
                        nonSqlRanges.add(intersection)
                    }
                }
            }
            for (tr in tagMarkupRanges)
            {
                val intersection = bodyRange.intersection(tr)
                if (intersection != null && !intersection.isEmpty)
                {
                    nonSqlRanges.add(intersection)
                }
            }

            val sortedNonSql = nonSqlRanges.sortedBy { it.startOffset }
            val mergedNonSql = mutableListOf<TextRange>()
            for (r in sortedNonSql)
            {
                if (mergedNonSql.isEmpty())
                {
                    mergedNonSql.add(r)
                }
                else
                {
                    val last = mergedNonSql.last()
                    if (r.startOffset <= last.endOffset)
                    {
                        mergedNonSql[mergedNonSql.size - 1] = TextRange(last.startOffset, maxOf(last.endOffset, r.endOffset))
                    }
                    else
                    {
                        mergedNonSql.add(r)
                    }
                }
            }

            var curr = bodyStart
            for (nonSql in mergedNonSql)
            {
                if (nonSql.startOffset > curr)
                {
                    val seg = text.substring(curr, nonSql.startOffset)
                    if (seg.isNotBlank())
                    {
                        highlightSqlSegment(file, seg, curr, holder)
                    }
                }
                curr = maxOf(curr, nonSql.endOffset)
            }
            if (curr < bodyEnd)
            {
                val seg = text.substring(curr, bodyEnd)
                if (seg.isNotBlank())
                {
                    highlightSqlSegment(file, seg, curr, holder)
                }
            }
        }
    }

    private fun findQueryBlocks(
        text: String,
        commentRanges: List<TextRange>,
        stringRanges: List<TextRange>
    ): List<QueryBlock>
    {
        val blocks = mutableListOf<QueryBlock>()
        val matcher = TAG_PATTERN.matcher(text)
        val queryStack = ArrayDeque<Int>()

        while (matcher.find())
        {
            val start = matcher.start()
            val end = matcher.end()

            if (CFMLModelParser.isInsideRanges(start, commentRanges) || CFMLModelParser.isInsideRanges(start, stringRanges))
            {
                continue
            }

            val closingTagName = matcher.group(1)?.lowercase()
            val openingTagName = matcher.group(2)?.lowercase()
            val closingSlash = matcher.group(4) ?: ""

            if (closingTagName == "cfquery")
            {
                if (queryStack.isNotEmpty())
                {
                    val bodyStart = queryStack.removeLast()
                    if (start > bodyStart)
                    {
                        blocks.add(QueryBlock(TextRange(bodyStart, start)))
                    }
                }
            }
            else if (openingTagName == "cfquery")
            {
                if (closingSlash != "/>")
                {
                    queryStack.addLast(end)
                }
            }
        }

        while (queryStack.isNotEmpty())
        {
            val bodyStart = queryStack.removeLast()
            if (text.length > bodyStart)
            {
                blocks.add(QueryBlock(TextRange(bodyStart, text.length)))
            }
        }

        return blocks
    }

    private fun highlightSqlSegment(
        file: CFMLPsiFile,
        segText: String,
        segStart: Int,
        holder: AnnotationHolder
    )
    {
        val project = file.project
        val virtualFile = file.virtualFile ?: file.originalFile.virtualFile
        val sqlHighlighter = getSqlHighlighter(project, virtualFile)

        if (sqlHighlighter != null)
        {
            try
            {
                val lexer = sqlHighlighter.highlightingLexer
                lexer.start(segText, 0, segText.length)
                while (lexer.tokenType != null)
                {
                    val tokenType = lexer.tokenType
                    val tokenStart = segStart + lexer.tokenStart
                    val tokenEnd = segStart + lexer.tokenEnd
                    val attributes = sqlHighlighter.getTokenHighlights(tokenType)
                    for (attr in attributes)
                    {
                        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                            .range(TextRange(tokenStart, tokenEnd))
                            .textAttributes(attr)
                            .create()
                    }
                    lexer.advance()
                }
                return
            }
            catch (_: Exception)
            {
            }
        }

        highlightSqlFallback(segText, segStart, holder)
    }

    private fun getSqlHighlighter(project: Project, virtualFile: VirtualFile?): SyntaxHighlighter?
    {
        val sqlLanguage = Language.findLanguageByID("GenericSQL")
            ?: Language.findLanguageByID("SQL")
            ?: Language.findLanguageByID("MySQL")
            ?: Language.findLanguageByID("PostgreSQL")
            ?: Language.findLanguageByID("Oracle")
            ?: Language.findLanguageByID("SQLite")
        return sqlLanguage?.let { SyntaxHighlighterFactory.getSyntaxHighlighter(it, project, virtualFile) }
    }

    private fun highlightSqlFallback(segText: String, segStart: Int, holder: AnnotationHolder)
    {
        var idx = 0
        val len = segText.length
        while (idx < len)
        {
            val ch = segText[idx]
            if (ch.isWhitespace())
            {
                idx++
                continue
            }
            if (ch == '-' && idx + 1 < len && segText[idx + 1] == '-')
            {
                val lineEnd = segText.indexOf('\n', idx).let { if (it == -1) len else it }
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(segStart + idx, segStart + lineEnd))
                    .textAttributes(CFMLSyntaxHighlighter.LINE_COMMENT)
                    .create()
                idx = lineEnd
                continue
            }
            if (ch == '/' && idx + 1 < len && segText[idx + 1] == '*')
            {
                val commentEnd = segText.indexOf("*/", idx + 2).let { if (it == -1) len else it + 2 }
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(segStart + idx, segStart + commentEnd))
                    .textAttributes(CFMLSyntaxHighlighter.BLOCK_COMMENT)
                    .create()
                idx = commentEnd
                continue
            }
            if (ch == '\'' || ch == '"')
            {
                val quote = ch
                var sEnd = idx + 1
                while (sEnd < len)
                {
                    if (segText[sEnd] == quote)
                    {
                        if (sEnd + 1 < len && segText[sEnd + 1] == quote)
                        {
                            sEnd += 2
                            continue
                        }
                        sEnd++
                        break
                    }
                    sEnd++
                }
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(segStart + idx, segStart + sEnd))
                    .textAttributes(CFMLSyntaxHighlighter.STRING)
                    .create()
                idx = sEnd
                continue
            }
            if (ch.isDigit())
            {
                var numEnd = idx + 1
                while (numEnd < len && (segText[numEnd].isDigit() || segText[numEnd] == '.'))
                {
                    numEnd++
                }
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(segStart + idx, segStart + numEnd))
                    .textAttributes(CFMLSyntaxHighlighter.NUMBER)
                    .create()
                idx = numEnd
                continue
            }
            if (ch.isLetter() || ch == '_')
            {
                var idEnd = idx + 1
                while (idEnd < len && (segText[idEnd].isLetterOrDigit() || segText[idEnd] == '_'))
                {
                    idEnd++
                }
                val word = segText.substring(idx, idEnd)
                if (SQL_KEYWORDS.contains(word.uppercase()))
                {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(segStart + idx, segStart + idEnd))
                        .textAttributes(CFMLSyntaxHighlighter.KEYWORD)
                        .create()
                }
                idx = idEnd
                continue
            }
            idx++
        }
    }
}
