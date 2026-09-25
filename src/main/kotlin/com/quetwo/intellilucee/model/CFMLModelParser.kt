package com.quetwo.intellilucee.model

import com.intellij.openapi.util.TextRange
import java.util.regex.Pattern

object CFMLModelParser
{

    private val KEYWORDS = setOf(
        "var", "function", "if", "else", "elseif", "while", "do", "for", "in", "switch", "case", "default",
        "break", "continue", "return", "try", "catch", "finally", "rethrow", "throw", "lock", "transaction",
        "thread", "param", "property", "import", "include", "component", "interface", "true", "false", "null",
        "and", "or", "not", "eq", "neq", "gt", "gte", "lt", "lte", "is", "equal", "contains", "does", "greater",
        "than", "less", "to", "mod", "xor", "eqv", "imp", "new", "required", "public", "private", "package",
        "remote", "static", "final", "abstract", "void", "any", "string", "numeric", "number", "boolean", "bool",
        "array", "struct", "query", "date", "cfset", "cfif", "cfelse", "cfelseif", "cffunction", "cfargument",
        "cfloop", "cfoutput", "cfquery", "cfparam", "cfreturn", "cfdump", "cfabort", "cfinclude", "cfinvoke"
    )

    private val CONTROL_KEYWORDS = setOf(
        "if", "while", "for", "switch", "catch", "lock", "transaction", "thread", "return"
    )

    fun parse(text: String): CFMLDocumentModel
    {
        val functionDecls = mutableListOf<CFMLFunctionDeclaration>()
        val functionCalls = mutableListOf<CFMLFunctionCall>()
        val varDecls = mutableListOf<CFMLVariableDeclaration>()
        val varUsages = mutableListOf<CFMLVariableUsage>()

        // 1. Identify masks for comments so we don't parse inside comments
        val commentRanges = findCommentRanges(text)

        // 2. Extract function declarations (Script & Tag)
        extractScriptFunctions(text, commentRanges, functionDecls, varDecls)
        extractTagFunctions(text, commentRanges, functionDecls, varDecls)

        // 3. Extract variable declarations (Script & Tag) outside/inside functions
        extractScriptVariableDeclarations(text, commentRanges, functionDecls, varDecls)
        extractTagVariableDeclarations(text, commentRanges, functionDecls, varDecls)

        // 4. Extract function calls
        extractFunctionCalls(text, commentRanges, functionDecls, functionCalls)

        // 5. Extract variable usages
        extractVariableUsages(text, commentRanges, functionDecls, varDecls, functionCalls, varUsages)

        return CFMLDocumentModel(functionDecls, functionCalls, varDecls, varUsages, commentRanges)
    }

    fun findInnermostEnclosingFunction(offset: Int, functions: List<CFMLFunctionDeclaration>): CFMLFunctionDeclaration?
    {
        if (functions.isEmpty()) return null
        var bestFunc: CFMLFunctionDeclaration? = null
        var minLen = Int.MAX_VALUE
        for (func in functions)
        {
            if (func.range.startOffset <= offset && offset <= func.range.endOffset)
            {
                if (func.bodyRange?.containsOffset(offset) == true || func.range.containsOffset(offset))
                {
                    val len = func.range.length
                    if (len < minLen)
                    {
                        minLen = len
                        bestFunc = func
                    }
                }
            }
        }
        return bestFunc
    }

    fun isInsideRanges(offset: Int, ranges: List<TextRange>): Boolean
    {
        if (ranges.isEmpty()) return false
        var low = 0
        var high = ranges.size - 1
        while (low <= high)
        {
            val mid = (low + high) ushr 1
            val range = ranges[mid]
            if (offset < range.startOffset)
            {
                high = mid - 1
            }
            else if (offset >= range.endOffset)
            {
                low = mid + 1
            }
            else
            {
                return true
            }
        }
        return false
    }

    fun getCommentEndIfInside(offset: Int, ranges: List<TextRange>): Int
    {
        if (ranges.isEmpty()) return -1
        var low = 0
        var high = ranges.size - 1
        while (low <= high)
        {
            val mid = (low + high) ushr 1
            val range = ranges[mid]
            if (offset < range.startOffset)
            {
                high = mid - 1
            }
            else if (offset >= range.endOffset)
            {
                low = mid + 1
            }
            else
            {
                return range.endOffset
            }
        }
        return -1
    }

    fun findCommentRanges(text: String): List<TextRange>
    {
        val ranges = mutableListOf<TextRange>()
        var i = 0
        val len = text.length
        while (i < len) {
            // CFML tag comments: <!--- ... --->
            if (text.startsWith("<!---", i))
            {
                val start = i
                var depth = 1
                i += 5
                while (i < len && depth > 0) {
                    if (text.startsWith("<!---", i))
                    {
                        depth++
                        i += 5
                    }
                    else if (text.startsWith("--->", i))
                    {
                        depth--
                        i += 4
                        if (depth == 0)
                        {
                            ranges.add(TextRange(start, i))
                            break
                        }
                    }
                    else
                    {
                        i++
                    }
                }
                continue
            }
            // HTML comments: <!-- ... -->
            if (text.startsWith("<!--", i))
            {
                val start = i
                val end = text.indexOf("-->", i + 4)
                if (end >= 0)
                {
                    ranges.add(TextRange(start, end + 3))
                    i = end + 3
                }
                else
                {
                    ranges.add(TextRange(start, len))
                    break
                }
                continue
            }
            // Script multi-line comment: /* ... */
            if (i + 1 < len && text[i] == '/' && text[i + 1] == '*')
            {
                val start = i
                val end = text.indexOf("*/", i + 2)
                if (end >= 0)
                {
                    ranges.add(TextRange(start, end + 2))
                    i = end + 2
                }
                else
                {
                    ranges.add(TextRange(start, len))
                    break
                }
                continue
            }
            // Script single-line comment: // ...
            if (i + 1 < len && text[i] == '/' && text[i + 1] == '/')
            {
                val start = i
                val end = text.indexOf('\n', i + 2)
                if (end >= 0)
                {
                    ranges.add(TextRange(start, end))
                    i = end
                }
                else
                {
                    ranges.add(TextRange(start, len))
                    break
                }
                continue
            }
            i++
        }
        return ranges
    }

    private val SCRIPT_FUNC_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s;{}])((?:(?:public|private|package|remote|static|final|abstract|default|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*)function\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)"""
    )
    private val ANON_FUNC_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s;{}(=,:])(?:(?:public|private|package|remote|static|final|abstract|default|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*function\s*\(([^)]*)\)"""
    )
    private val ARROW_FUNC_PATTERN = Pattern.compile(
        """(?i)(?:^|[\s;{}(=,:])(?:\(([^)]*)\)|([A-Za-z0-9_]+))\s*=>"""
    )
    private val PARAM_EXTRACT_PATTERN = Pattern.compile(
        """(?i)(?:(?:required|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*([A-Za-z0-9_]+)(?:\s*=[^,]*)?"""
    )
    private val ACCESS_ATTR_PATTERN = Pattern.compile("""(?i)\baccess\s*=\s*["']?([A-Za-z0-9_]+)["']?""")
    private val CFFUNCTION_PATTERN = Pattern.compile("""(?i)<cffunction\b([^>]*)>""", Pattern.DOTALL)
    private val CFFUNCTION_CLOSE_PATTERN = Pattern.compile("""(?i)</cffunction>""")
    private val CFARGUMENT_PATTERN = Pattern.compile("""(?i)<cfargument\b([^>]*)>""")
    private val NAME_ATTR_PATTERN = Pattern.compile("""(?i)\bname\s*=\s*["']?([A-Za-z0-9_]+)["']?""")
    private val PROP_PATTERN = Pattern.compile("""(?i)\bproperty\s+([^;]+);""")
    private val VAR_STMT_PATTERN = Pattern.compile("""(?i)\bvar\s+""")
    private val TYPED_DECL_PATTERN = Pattern.compile("""(?i)(?:^|[\s;{}])(string|numeric|number|boolean|bool|array|struct|query|date|any)\s+([A-Za-z0-9_]+)\s*(?:=|;|,|\))""")
    private val FOR_VAR_PATTERN = Pattern.compile("""(?i)\bfor\s*\(\s*(?:var\s+)?([A-Za-z0-9_]+)\s*(?:in|=|;)""")
    private val CATCH_PATTERN = Pattern.compile("""(?i)\bcatch\s*\(\s*(?:(?:any|[A-Za-z0-9_$.]+)\s+)?([A-Za-z0-9_]+)\s*\)""")
    private val PARAM_PATTERN = Pattern.compile("""(?i)\bparam\s+(?:name\s*=\s*["']?((?:local\.)?([A-Za-z0-9_]+))["']?|(?:(?:string|numeric|number|boolean|bool|array|struct|query|date|any)\s+)?([A-Za-z0-9_]+)\s*=)""")
    private val ASSIGN_PATTERN = Pattern.compile("""(?i)(?:^|[\s;{}])((?:(?:local|variables|this)\.)?([A-Za-z0-9_]+))\s*(?:=|\+=|-=|\*=|/=|&=)(?!=)""")
    private val VAR_ITEM_PATTERN = Pattern.compile("""(?i)((?:(?:local|variables|this)\.)?([A-Za-z0-9_]+))\s*$""")
    private val CFPROP_PATTERN = Pattern.compile("""(?i)<cfproperty\b([^>]*)>""")
    private val CFSET_PATTERN = Pattern.compile("""(?i)<cfset\s+(?:var\s+)?((?:(?:local|variables|this)\.)?([A-Za-z0-9_]+))\s*(?:=|\+=|-=|\*=|/=|&=)""", Pattern.DOTALL)
    private val CFPARAM_PATTERN = Pattern.compile("""(?i)<cfparam\b([^>]*)>""")
    private val CFPARAM_NAME_ATTR_PATTERN = Pattern.compile("""(?i)\bname\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""")
    private val CFLOOP_PATTERN = Pattern.compile("""(?i)<cfloop\b([^>]*)>""")
    private val CFLOOP_ATTR_PATTERN = Pattern.compile("""(?i)\b(?:index|item|query)\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""")
    private val CFQUERY_PATTERN = Pattern.compile("""(?i)<cfquery\b([^>]*)>""")
    private val CFCATCH_PATTERN = Pattern.compile("""(?i)<cfcatch\b([^>]*)>""")
    private val CALL_PATTERN = Pattern.compile("""(?i)(?:^|[^A-Za-z0-9_$.])([A-Za-z0-9_]+)\s*\(""")
    private val CFINVOKE_PATTERN = Pattern.compile("""(?i)<cfinvoke\b([^>]*)>""")
    private val METHOD_ATTR_PATTERN = Pattern.compile("""(?i)\bmethod\s*=\s*["']?([A-Za-z0-9_]+)["']?""")
    private val ID_PATTERN = Pattern.compile("""(?i)(?:(?:local|variables|arguments|session|application|request|this)\.)?([A-Za-z0-9_]+)""")

    private fun extractScriptFunctions(
        text: String,
        commentRanges: List<TextRange>,
        functions: MutableList<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        // 1. Named functions: [modifiers] function [name] ( [params] )
        val matcher = SCRIPT_FUNC_PATTERN.matcher(text)
        while (matcher.find())
        {
            val modifiersText = matcher.group(1) ?: ""
            val name = matcher.group(2)
            val nameStart = matcher.start(2)
            val nameEnd = matcher.end(2)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!matcher.find(commentEnd)) break
                continue
            }

            val paramsText = matcher.group(3)
            val paramsStart = matcher.start(3)

            // Find function body { ... }
            var bodyRange: TextRange? = null
            var funcEnd = matcher.end()
            val openBrace = findNextOpenBrace(text, matcher.end(), commentRanges)
            if (openBrace >= 0)
            {
                val closeBrace = findMatchingBrace(text, openBrace, commentRanges)
                if (closeBrace > openBrace)
                {
                    bodyRange = TextRange(openBrace, closeBrace + 1)
                    funcEnd = closeBrace + 1
                }
            }

            val afterParamsText = if (openBrace > matcher.end()) {
                text.substring(matcher.end(), openBrace)
            } else {
                ""
            }
            val access = parseAccessType(modifiersText, afterParamsText)

            val declRange = TextRange(matcher.start(), funcEnd)
            val nameRange = TextRange(nameStart, nameEnd)
            val paramDecls = mutableListOf<CFMLVariableDeclaration>()
            val funcDecl = CFMLFunctionDeclaration(name, nameRange, declRange, bodyRange, paramDecls, access)
            functions.add(funcDecl)

            // Parse parameters
            extractFunctionParameters(paramsText, paramsStart, funcDecl, paramDecls, varDecls)
        }

        // 2. Anonymous functions / Closures: function ( [params] ) { ... }
        val anonMatcher = ANON_FUNC_PATTERN.matcher(text)
        while (anonMatcher.find())
        {
            val start = anonMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!anonMatcher.find(commentEnd)) break
                continue
            }
            val paramsText = anonMatcher.group(1)
            val paramsStart = anonMatcher.start(1)

            // Check if already covered by named function
            if (functions.any { it.nameRange.startOffset >= start && it.nameRange.endOffset <= anonMatcher.end() }) continue

            var bodyRange: TextRange? = null
            var funcEnd = anonMatcher.end()
            val openBrace = findNextOpenBrace(text, anonMatcher.end(), commentRanges)
            if (openBrace >= 0)
            {
                val closeBrace = findMatchingBrace(text, openBrace, commentRanges)
                if (closeBrace > openBrace)
                {
                    bodyRange = TextRange(openBrace, closeBrace + 1)
                    funcEnd = closeBrace + 1
                }
            }

            val declRange = TextRange(start, funcEnd)
            val nameRange = TextRange(start, anonMatcher.end())
            val paramDecls = mutableListOf<CFMLVariableDeclaration>()
            val funcDecl = CFMLFunctionDeclaration("", nameRange, declRange, bodyRange, paramDecls)
            functions.add(funcDecl)

            extractFunctionParameters(paramsText, paramsStart, funcDecl, paramDecls, varDecls)
        }

        // 3. Arrow functions: ( [params] ) => { ... } or param => { ... }
        val arrowMatcher = ARROW_FUNC_PATTERN.matcher(text)
        while (arrowMatcher.find())
        {
            val start = arrowMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!arrowMatcher.find(commentEnd)) break
                continue
            }
            val paramsText = arrowMatcher.group(1) ?: arrowMatcher.group(2) ?: ""
            val paramsStart = if (arrowMatcher.group(1) != null) arrowMatcher.start(1) else arrowMatcher.start(2)

            var bodyRange: TextRange? = null
            var funcEnd = arrowMatcher.end()
            val openBrace = findNextOpenBrace(text, arrowMatcher.end(), commentRanges)
            if (openBrace >= 0)
            {
                val closeBrace = findMatchingBrace(text, openBrace, commentRanges)
                if (closeBrace > openBrace)
                {
                    bodyRange = TextRange(openBrace, closeBrace + 1)
                    funcEnd = closeBrace + 1
                }
            }

            val declRange = TextRange(start, funcEnd)
            val nameRange = TextRange(start, arrowMatcher.end())
            val paramDecls = mutableListOf<CFMLVariableDeclaration>()
            val funcDecl = CFMLFunctionDeclaration("", nameRange, declRange, bodyRange, paramDecls)
            functions.add(funcDecl)

            extractFunctionParameters(paramsText, paramsStart, funcDecl, paramDecls, varDecls)
        }
    }

    private fun findNextOpenBrace(text: String, fromIndex: Int, commentRanges: List<TextRange>): Int
    {
        var i = fromIndex
        val len = text.length
        while (i < len)
        {
            val commentEnd = getCommentEndIfInside(i, commentRanges)
            if (commentEnd > i)
            {
                i = commentEnd
                continue
            }
            val c = text[i]
            if (c == ';') return -1 // Semicolon before brace means no body (interface/abstract)
            if (c == '{') return i
            if (c == '}' || c == '<') return -1
            i++
        }
        return -1
    }

    private fun extractFunctionParameters(
        paramsText: String?,
        paramsStart: Int,
        funcDecl: CFMLFunctionDeclaration,
        paramDecls: MutableList<CFMLVariableDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        if (paramsText.isNullOrBlank()) return
        val pm = PARAM_EXTRACT_PATTERN.matcher(paramsText)
        while (pm.find())
        {
            val pName = pm.group(1)
            if (pName.isNotEmpty() && !KEYWORDS.contains(pName.lowercase()))
            {
                val pStart = paramsStart + pm.start(1)
                val pEnd = paramsStart + pm.end(1)
                val pNameRange = TextRange(pStart, pEnd)
                val pFullRange = TextRange(paramsStart + pm.start(), paramsStart + pm.end())
                val paramDecl = CFMLVariableDeclaration(pName, pNameRange, pFullRange, isLocal = true, enclosingFunction = funcDecl)
                varDecls.add(paramDecl)
                paramDecls.add(paramDecl)
            }
        }
    }

    fun parseAccessType(modifiers: String, extraAttrs: String = ""): CFMLAccessType
    {
        val combined = "$modifiers $extraAttrs"
        val attrMatcher = ACCESS_ATTR_PATTERN.matcher(combined)
        if (attrMatcher.find())
        {
            return when (attrMatcher.group(1).lowercase())
            {
                "private" -> CFMLAccessType.PRIVATE
                "package" -> CFMLAccessType.PACKAGE
                "remote" -> CFMLAccessType.REMOTE
                "public" -> CFMLAccessType.PUBLIC
                else -> CFMLAccessType.PUBLIC
            }
        }

        val tokens = modifiers.lowercase().split(Regex("[^a-zA-Z0-9_]+")).filter { it.isNotEmpty() }
        if (tokens.contains("private")) return CFMLAccessType.PRIVATE
        if (tokens.contains("package")) return CFMLAccessType.PACKAGE
        if (tokens.contains("remote")) return CFMLAccessType.REMOTE
        if (tokens.contains("public")) return CFMLAccessType.PUBLIC

        return CFMLAccessType.PUBLIC
    }

    private fun extractTagFunctions(
        text: String,
        commentRanges: List<TextRange>,
        functions: MutableList<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        val matcher = CFFUNCTION_PATTERN.matcher(text)
        val closeMatcher = CFFUNCTION_CLOSE_PATTERN.matcher(text)
        while (matcher.find())
        {
            val start = matcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = matcher.group(1)
            val nameMatcher = NAME_ATTR_PATTERN.matcher(attrs)
            if (nameMatcher.find())
            {
                val name = nameMatcher.group(1)
                val nameStart = matcher.start(1) + nameMatcher.start(1)
                val nameEnd = matcher.start(1) + nameMatcher.end(1)
                val nameRange = TextRange(nameStart, nameEnd)

                // Find closing </cffunction>
                var bodyRange: TextRange? = null
                var tagEnd = matcher.end()
                if (closeMatcher.find(matcher.end()))
                {
                    tagEnd = closeMatcher.end()
                    bodyRange = TextRange(matcher.end(), closeMatcher.start())
                }
                val access = parseAccessType("", attrs)
                val paramDecls = mutableListOf<CFMLVariableDeclaration>()
                val funcDecl = CFMLFunctionDeclaration(name, nameRange, TextRange(start, tagEnd), bodyRange, paramDecls, access)
                functions.add(funcDecl)

                // Check for <cfargument> inside this function
                if (bodyRange != null)
                {
                    val bodyText = text.substring(bodyRange.startOffset, bodyRange.endOffset)
                    val argMatcher = CFARGUMENT_PATTERN.matcher(bodyText)
                    while (argMatcher.find())
                    {
                        val argAttrs = argMatcher.group(1)
                        val argNameM = NAME_ATTR_PATTERN.matcher(argAttrs)
                        if (argNameM.find())
                        {
                            val argName = argNameM.group(1)
                            val argNameStart = bodyRange.startOffset + argMatcher.start(1) + argNameM.start(1)
                            val argNameEnd = bodyRange.startOffset + argMatcher.start(1) + argNameM.end(1)
                            val argFullStart = bodyRange.startOffset + argMatcher.start()
                            val argFullEnd = bodyRange.startOffset + argMatcher.end()
                            val paramDecl = CFMLVariableDeclaration(
                                argName,
                                TextRange(argNameStart, argNameEnd),
                                TextRange(argFullStart, argFullEnd),
                                isLocal = true,
                                enclosingFunction = funcDecl
                            )
                            varDecls.add(paramDecl)
                            paramDecls.add(paramDecl)
                        }
                    }
                }
            }
        }
    }

    private val TAG_DECL_PATTERN = Pattern.compile("""(?i)<(?:cfquery|cfdirectory|cfprocresult|cfhttp|cffile)\b([^>]*)>""")
    private val TAG_DECL_ATTR_PATTERN = Pattern.compile("""(?i)\b(?:name|result|variable)\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""")

    private fun extractScriptVariableDeclarations(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        val nameScopeSet = HashSet<String>()
        val nameRangeSet = HashSet<TextRange>()
        for (d in varDecls)
        {
            nameScopeSet.add("${d.name.lowercase()}#${d.enclosingFunction?.range?.startOffset ?: -1}")
            nameRangeSet.add(d.nameRange)
        }

        // 0. Property declarations: property name="foo" type="string"; or property string foo; or property foo;
        val propMatcher = PROP_PATTERN.matcher(text)
        while (propMatcher.find())
        {
            val propStart = propMatcher.start()
            val commentEnd = getCommentEndIfInside(propStart, commentRanges)
            if (commentEnd > propStart)
            {
                if (!propMatcher.find(commentEnd)) break
                continue
            }
            val propBody = propMatcher.group(1).trim()
            val enclosingFunc = findInnermostEnclosingFunction(propStart, functions)

            val nameAttrM = NAME_ATTR_PATTERN.matcher(propBody)
            if (nameAttrM.find())
            {
                val name = nameAttrM.group(1)
                val nameStart = propMatcher.start(1) + nameAttrM.start(1)
                val nameEnd = propMatcher.start(1) + nameAttrM.end(1)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(propMatcher.start(), propMatcher.end()),
                    isLocal = false,
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    nameRangeSet.add(decl.nameRange)
                    varDecls.add(decl)
                }
            }
            else
            {
                val beforeEq = propBody.substringBefore('=').trim()
                val eqTokens = beforeEq.split(Regex("""\s+""")).filter { it.isNotEmpty() }
                val name = eqTokens.lastOrNull()
                if (name != null && name.matches(Regex("""[A-Za-z0-9_]+""")) && !KEYWORDS.contains(name.lowercase()))
                {
                    val nameIdx = propBody.lastIndexOf(name)
                    val nameStart = propMatcher.start(1) + nameIdx
                    val nameEnd = nameStart + name.length
                    val decl = CFMLVariableDeclaration(
                        name,
                        TextRange(nameStart, nameEnd),
                        TextRange(propMatcher.start(), propMatcher.end()),
                        isLocal = false,
                        enclosingFunction = enclosingFunc
                    )
                    val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                    if (nameScopeSet.add(key))
                    {
                        nameRangeSet.add(decl.nameRange)
                        varDecls.add(decl)
                    }
                }
            }
        }

        // 1. var declarations: var a = 1, b = 2; or var string x = "hello"; or var x;
        val varMatcher = VAR_STMT_PATTERN.matcher(text)
        while (varMatcher.find())
        {
            val varStart = varMatcher.start()
            val commentEnd = getCommentEndIfInside(varStart, commentRanges)
            if (commentEnd > varStart)
            {
                if (!varMatcher.find(commentEnd)) break
                continue
            }
            val afterVar = varMatcher.end()
            val stmtEnd = findStatementEnd(text, afterVar, commentRanges)
            val stmtText = text.substring(afterVar, stmtEnd)
            val enclosingFunc = findInnermostEnclosingFunction(varStart, functions)

            val items = splitByTopLevelCommas(stmtText, afterVar, commentRanges)
            for (itemRange in items)
            {
                val itemText = text.substring(itemRange.startOffset, itemRange.endOffset).trim()
                if (itemText.isEmpty()) continue
                val decl = extractVarItem(text, itemRange.startOffset, itemRange.endOffset, enclosingFunc)
                if (decl != null && nameRangeSet.add(decl.nameRange))
                {
                    nameScopeSet.add("${decl.name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}")
                    varDecls.add(decl)
                }
            }
        }

        // 2. Typed declarations without 'var': string x = ...; numeric count = ...;
        val typedMatcher = TYPED_DECL_PATTERN.matcher(text)
        while (typedMatcher.find())
        {
            val name = typedMatcher.group(2)
            val nameStart = typedMatcher.start(2)
            val nameEnd = typedMatcher.end(2)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!typedMatcher.find(commentEnd)) break
                continue
            }
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(typedMatcher.start(), typedMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc
                )
                if (nameRangeSet.add(decl.nameRange))
                {
                    nameScopeSet.add("${decl.name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}")
                    varDecls.add(decl)
                }
            }
        }

        // 3. For loop variables: for (var i in items) or for (i in items) or for (var i = 1; ...)
        val forMatcher = FOR_VAR_PATTERN.matcher(text)
        while (forMatcher.find())
        {
            val name = forMatcher.group(1)
            val nameStart = forMatcher.start(1)
            val nameEnd = forMatcher.end(1)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!forMatcher.find(commentEnd)) break
                continue
            }
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(forMatcher.start(), forMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc
                )
                if (nameRangeSet.add(decl.nameRange))
                {
                    nameScopeSet.add("${decl.name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}")
                    varDecls.add(decl)
                }
            }
        }

        // 4. Catch block variable: catch (any e) or catch (CustomException e) or catch (e)
        val catchMatcher = CATCH_PATTERN.matcher(text)
        while (catchMatcher.find())
        {
            val name = catchMatcher.group(1)
            val nameStart = catchMatcher.start(1)
            val nameEnd = catchMatcher.end(1)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!catchMatcher.find(commentEnd)) break
                continue
            }
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(catchMatcher.start(), catchMatcher.end()),
                    isLocal = true,
                    enclosingFunction = enclosingFunc
                )
                if (nameRangeSet.add(decl.nameRange))
                {
                    nameScopeSet.add("${decl.name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}")
                    varDecls.add(decl)
                }
            }
        }

        // 5. Param declarations: param name="x" default="1"; or param string x = 1; or param x = 1;
        val paramMatcher = PARAM_PATTERN.matcher(text)
        while (paramMatcher.find())
        {
            val rawName = if (paramMatcher.group(1) != null) paramMatcher.group(1) else paramMatcher.group(3) ?: continue
            val name = CFMLDocumentModel.cleanVariableName(rawName)
            val nameStart = if (paramMatcher.group(1) != null) paramMatcher.start(1) + (rawName.length - name.length) else paramMatcher.start(3)
            val nameEnd = nameStart + name.length
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!paramMatcher.find(commentEnd)) break
                continue
            }
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(paramMatcher.start(), paramMatcher.end()),
                    isLocal = enclosingFunc != null || rawName.lowercase().startsWith("local."),
                    enclosingFunction = enclosingFunc
                )
                if (nameRangeSet.add(decl.nameRange))
                {
                    nameScopeSet.add("${decl.name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}")
                    varDecls.add(decl)
                }
            }
        }

        // 6. Plain and scoped assignments: x = ... or local.x = ... or variables.x = ... or this.x = ...
        val assignMatcher = ASSIGN_PATTERN.matcher(text)
        while (assignMatcher.find())
        {
            val fullVar = assignMatcher.group(1)
            val name = assignMatcher.group(2)
            val nameStart = assignMatcher.start(2)
            val nameEnd = assignMatcher.end(2)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!assignMatcher.find(commentEnd)) break
                continue
            }
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val isLocal = fullVar.lowercase().startsWith("local.") || (enclosingFunc != null && !fullVar.lowercase().startsWith("variables.") && !fullVar.lowercase().startsWith("this."))
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(assignMatcher.start(1), assignMatcher.end(1)),
                    isLocal = isLocal,
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    nameRangeSet.add(decl.nameRange)
                    varDecls.add(decl)
                }
            }
        }
    }

    private fun extractVarItem(
        text: String,
        start: Int,
        end: Int,
        enclosingFunc: CFMLFunctionDeclaration?): CFMLVariableDeclaration?
    {
        val item = text.substring(start, end)
        val eqIdx = item.indexOf('=')
        val lhs = if (eqIdx >= 0) item.substring(0, eqIdx) else item

        val m = VAR_ITEM_PATTERN.matcher(lhs)
        if (m.find())
        {
            val fullVar = m.group(1)
            val name = m.group(2)
            val clean = CFMLDocumentModel.cleanVariableName(name)
            if (clean.isNotEmpty() && !KEYWORDS.contains(clean.lowercase()))
            {
                val nameStart = start + m.start(2)
                val nameEnd = start + m.end(2)
                val fullStart = start + m.start(1)
                val fullEnd = start + m.end(1)
                return CFMLVariableDeclaration(
                    clean,
                    TextRange(nameStart, nameEnd),
                    TextRange(fullStart, fullEnd),
                    isLocal = (enclosingFunc != null && !fullVar.lowercase().startsWith("variables.") && !fullVar.lowercase().startsWith("this.")) || fullVar.lowercase().startsWith("local."),
                    enclosingFunction = enclosingFunc
                )
            }
        }
        return null
    }

    private fun findStatementEnd(text: String, start: Int, commentRanges: List<TextRange>): Int
    {
        var parenDepth = 0
        var braceDepth = 0
        var bracketDepth = 0
        var inSingleQuote = false
        var inDoubleQuote = false

        var i = start
        val len = text.length
        while (i < len)
        {
            val commentEnd = getCommentEndIfInside(i, commentRanges)
            if (commentEnd > i)
            {
                i = commentEnd
                continue
            }
            val c = text[i]

            if (inSingleQuote)
            {
                if (c == '\'') inSingleQuote = false
                i++
                continue
            }
            if (inDoubleQuote)
            {
                if (c == '"') inDoubleQuote = false
                i++
                continue
            }

            when (c)
            {
                '\'' -> inSingleQuote = true
                '"' -> inDoubleQuote = true
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                '{' -> braceDepth++
                '}' -> {
                    if (braceDepth > 0) braceDepth--
                    else return i
                }
                '[' -> bracketDepth++
                ']' -> if (bracketDepth > 0) bracketDepth--
                ';' -> if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) return i
                '\n' -> if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) return i
            }
            i++
        }
        return text.length
    }

    private fun splitByTopLevelCommas(stmtText: String, baseOffset: Int, commentRanges: List<TextRange>): List<TextRange>
    {
        val result = mutableListOf<TextRange>()
        var parenDepth = 0
        var braceDepth = 0
        var bracketDepth = 0
        var inSingleQuote = false
        var inDoubleQuote = false
        var itemStart = 0

        var i = 0
        val len = stmtText.length
        while (i < len)
        {
            val absOffset = baseOffset + i
            val commentEnd = getCommentEndIfInside(absOffset, commentRanges)
            if (commentEnd > absOffset)
            {
                i += (commentEnd - absOffset)
                continue
            }
            val c = stmtText[i]

            if (inSingleQuote)
            {
                if (c == '\'') inSingleQuote = false
                i++
                continue
            }
            if (inDoubleQuote)
            {
                if (c == '"') inDoubleQuote = false
                i++
                continue
            }

            when (c)
            {
                '\'' -> inSingleQuote = true
                '"' -> inDoubleQuote = true
                '(' -> parenDepth++
                ')' -> if (parenDepth > 0) parenDepth--
                '{' -> braceDepth++
                '}' -> if (braceDepth > 0) braceDepth--
                '[' -> bracketDepth++
                ']' -> if (bracketDepth > 0) bracketDepth--
                ',' -> {
                    if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0)
                    {
                        result.add(TextRange(baseOffset + itemStart, baseOffset + i))
                        itemStart = i + 1
                    }
                }
            }
            i++
        }
        if (itemStart < stmtText.length)
        {
            result.add(TextRange(baseOffset + itemStart, baseOffset + stmtText.length))
        }
        return result
    }

    private fun extractTagVariableDeclarations(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        val nameScopeSet = HashSet<String>()
        for (d in varDecls)
        {
            nameScopeSet.add("${d.name.lowercase()}#${d.enclosingFunction?.range?.startOffset ?: -1}")
        }

        // 0. <cfproperty name="myProp" type="string">
        val cfpropMatcher = CFPROP_PATTERN.matcher(text)
        while (cfpropMatcher.find())
        {
            val start = cfpropMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!cfpropMatcher.find(commentEnd)) break
                continue
            }
            val attrs = cfpropMatcher.group(1)
            val nameM = NAME_ATTR_PATTERN.matcher(attrs)
            if (nameM.find())
            {
                val name = nameM.group(1)
                val nameStart = cfpropMatcher.start(1) + nameM.start(1)
                val nameEnd = cfpropMatcher.start(1) + nameM.end(1)
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(cfpropMatcher.start(), cfpropMatcher.end()),
                    isLocal = false,
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    varDecls.add(decl)
                }
            }
        }

        // 1. <cfset var x = ...> or <cfset x = ...> or <cfset local.x = ...> or <cfset variables.x = ...> or <cfset this.x = ...>
        val cfsetMatcher = CFSET_PATTERN.matcher(text)
        while (cfsetMatcher.find())
        {
            val start = cfsetMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!cfsetMatcher.find(commentEnd)) break
                continue
            }
            val fullVar = cfsetMatcher.group(1)
            val name = cfsetMatcher.group(2)
            val nameStart = cfsetMatcher.start(2)
            val nameEnd = cfsetMatcher.end(2)
            val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
            val isLocal = fullVar.lowercase().startsWith("local.") || cfsetMatcher.group().lowercase().contains("var ") || (enclosingFunc != null && !fullVar.lowercase().startsWith("variables.") && !fullVar.lowercase().startsWith("this."))
            val decl = CFMLVariableDeclaration(
                name,
                TextRange(nameStart, nameEnd),
                TextRange(cfsetMatcher.start(1), cfsetMatcher.end(1)),
                isLocal = isLocal,
                enclosingFunction = enclosingFunc
            )
            val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
            if (nameScopeSet.add(key))
            {
                varDecls.add(decl)
            }
        }

        // 2. <cfparam name="myVar" default="...">
        val cfparamMatcher = CFPARAM_PATTERN.matcher(text)
        while (cfparamMatcher.find())
        {
            val start = cfparamMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!cfparamMatcher.find(commentEnd)) break
                continue
            }
            val attrs = cfparamMatcher.group(1)
            val nameM = CFPARAM_NAME_ATTR_PATTERN.matcher(attrs)
            if (nameM.find())
            {
                val fullVar = nameM.group(1)
                val name = nameM.group(2)
                val nameStart = cfparamMatcher.start(1) + nameM.start(2)
                val nameEnd = cfparamMatcher.start(1) + nameM.end(2)
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val isLocal = fullVar.lowercase().startsWith("local.") || (enclosingFunc != null)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(cfparamMatcher.start(), cfparamMatcher.end()),
                    isLocal = isLocal,
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    varDecls.add(decl)
                }
            }
        }

        // 3. <cfloop index="i" ...> or item="item" or query="q"
        val cfloopMatcher = CFLOOP_PATTERN.matcher(text)
        while (cfloopMatcher.find())
        {
            val start = cfloopMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!cfloopMatcher.find(commentEnd)) break
                continue
            }
            val attrs = cfloopMatcher.group(1)
            val nameM = CFLOOP_ATTR_PATTERN.matcher(attrs)
            while (nameM.find())
            {
                val fullVar = nameM.group(1)
                val name = nameM.group(2)
                val nameStart = cfloopMatcher.start(1) + nameM.start(2)
                val nameEnd = cfloopMatcher.start(1) + nameM.end(2)
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(cfloopMatcher.start(), cfloopMatcher.end()),
                    isLocal = enclosingFunc != null || fullVar.lowercase().startsWith("local."),
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    varDecls.add(decl)
                }
            }
        }

        // 4. <cfquery name="qResult" ...> or <cfhttp result="res" ...> or <cfdirectory name="dir" ...> or <cffile variable="content" ...>
        val tagDeclMatcher = TAG_DECL_PATTERN.matcher(text)
        while (tagDeclMatcher.find())
        {
            val start = tagDeclMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!tagDeclMatcher.find(commentEnd)) break
                continue
            }
            val attrs = tagDeclMatcher.group(1)
            val nameM = TAG_DECL_ATTR_PATTERN.matcher(attrs)
            if (nameM.find())
            {
                val fullVar = nameM.group(1)
                val name = nameM.group(2)
                val nameStart = tagDeclMatcher.start(1) + nameM.start(2)
                val nameEnd = tagDeclMatcher.start(1) + nameM.end(2)
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(tagDeclMatcher.start(), tagDeclMatcher.end()),
                    isLocal = enclosingFunc != null || fullVar.lowercase().startsWith("local."),
                    enclosingFunction = enclosingFunc
                )
                val key = "${name.lowercase()}#${enclosingFunc?.range?.startOffset ?: -1}"
                if (nameScopeSet.add(key))
                {
                    varDecls.add(decl)
                }
            }
        }

        // 5. <cfcatch type="...">
        val cfcatchMatcher = CFCATCH_PATTERN.matcher(text)
        while (cfcatchMatcher.find())
        {
            val start = cfcatchMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!cfcatchMatcher.find(commentEnd)) break
                continue
            }
            val enclosingFunc = findInnermostEnclosingFunction(start, functions)
            val decl = CFMLVariableDeclaration(
                "cfcatch",
                TextRange(start + 1, start + 8),
                TextRange(start, cfcatchMatcher.end()),
                isLocal = true,
                enclosingFunction = enclosingFunc
            )
            val key = "cfcatch#${enclosingFunc?.range?.startOffset ?: -1}"
            if (nameScopeSet.add(key))
            {
                varDecls.add(decl)
            }
        }
    }

    private fun extractFunctionCalls(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        functionCalls: MutableList<CFMLFunctionCall>)
    {
        val funcNameStarts = functions.map { it.nameRange.startOffset }.toHashSet()

        // 1. Script function calls: foo(...)
        val matcher = CALL_PATTERN.matcher(text)
        while (matcher.find())
        {
            val name = matcher.group(1)
            val nameStart = matcher.start(1)
            val nameEnd = matcher.end(1)
            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!matcher.find(commentEnd)) break
                continue
            }

            // Exclude control flow keywords like if (, while (, for (, switch (, catch (
            if (CONTROL_KEYWORDS.contains(name.lowercase())) continue

            // Exclude function declaration itself
            if (funcNameStarts.contains(nameStart)) continue

            val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
            functionCalls.add(CFMLFunctionCall(name, TextRange(nameStart, nameEnd), enclosingFunc))
        }

        // 2. <cfinvoke ... method="funcName" ...>
        val invokeMatcher = CFINVOKE_PATTERN.matcher(text)
        while (invokeMatcher.find())
        {
            val start = invokeMatcher.start()
            val commentEnd = getCommentEndIfInside(start, commentRanges)
            if (commentEnd > start)
            {
                if (!invokeMatcher.find(commentEnd)) break
                continue
            }
            val attrs = invokeMatcher.group(1)
            val methodM = METHOD_ATTR_PATTERN.matcher(attrs)
            if (methodM.find())
            {
                val name = methodM.group(1)
                val nameStart = invokeMatcher.start(1) + methodM.start(1)
                val nameEnd = invokeMatcher.start(1) + methodM.end(1)
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                functionCalls.add(CFMLFunctionCall(name, TextRange(nameStart, nameEnd), enclosingFunc))
            }
        }
    }

    private fun extractVariableUsages(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        varDecls: List<CFMLVariableDeclaration>,
        functionCalls: List<CFMLFunctionCall>,
        varUsages: MutableList<CFMLVariableUsage>)
    {
        val funcNameStarts = functions.map { it.nameRange.startOffset }.toHashSet()
        val funcCallStarts = functionCalls.map { it.range.startOffset }.toHashSet()
        val varDeclNameStarts = varDecls.map { it.nameRange.startOffset }.toHashSet()

        // Match identifier tokens: (optional scope.)varName
        val matcher = ID_PATTERN.matcher(text)
        while (matcher.find())
        {
            val fullToken = matcher.group()
            val bareName = matcher.group(1)
            val nameStart = matcher.start(1)
            val nameEnd = matcher.end(1)
            val fullStart = matcher.start()
            val fullEnd = matcher.end()

            val commentEnd = getCommentEndIfInside(nameStart, commentRanges)
            if (commentEnd > nameStart)
            {
                if (!matcher.find(commentEnd)) break
                continue
            }

            // Check if keyword
            if (KEYWORDS.contains(bareName.lowercase())) continue

            // Check if it's a function declaration identifier
            if (funcNameStarts.contains(nameStart)) continue

            // Check if it's a function call identifier
            if (funcCallStarts.contains(nameStart) || funcCallStarts.contains(fullStart)) continue

            // Check if it's a variable declaration identifier itself
            if (varDeclNameStarts.contains(nameStart) || varDeclNameStarts.contains(fullStart)) continue

            // Check if immediately followed by `(` (in case it was a function call not caught)
            var nextCharIdx = nameEnd
            while (nextCharIdx < text.length && text[nextCharIdx].isWhitespace())
            {
                nextCharIdx++
            }
            if (nextCharIdx < text.length && text[nextCharIdx] == '(')
            {
                continue
            }

            // Check if tag attribute name (e.g. ` name=` or ` type=` or ` datasource=`)
            if (isTagAttributeName(text, fullStart, fullEnd))
            {
                continue
            }

            val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
            varUsages.add(CFMLVariableUsage(bareName, TextRange(nameStart, nameEnd), TextRange(fullStart, fullEnd), fullToken, enclosingFunc))
        }
    }

    private fun isTagAttributeName(text: String, start: Int, end: Int): Boolean {
        var nextIdx = end
        while (nextIdx < text.length && text[nextIdx].isWhitespace())
        {
            nextIdx++
        }
        if (nextIdx < text.length && text[nextIdx] == '=')
        {
            var i = start - 1
            while (i >= 0)
            {
                val c = text[i]
                if (c == '>')
                {
                    return false
                }
                if (c == '<')
                {
                    return true
                }
                if (c == ';' || c == '{' || c == '}')
                {
                    return false
                }
                i--
            }
        }
        return false
    }

    private fun findMatchingBrace(text: String, openingBrace: Int, commentRanges: List<TextRange>): Int
    {
        var depth = 0
        var i = openingBrace
        val len = text.length
        var inSingle = false
        var inDouble = false

        while (i < len)
        {
            val commentEnd = getCommentEndIfInside(i, commentRanges)
            if (commentEnd > i)
            {
                i = commentEnd
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
            if (c == '{')
            {
                depth++
            }
            else if (c == '}')
            {
                depth--
                if (depth == 0)
                {
                    return i
                }
            }
            i++
        }
        return -1
    }
}
