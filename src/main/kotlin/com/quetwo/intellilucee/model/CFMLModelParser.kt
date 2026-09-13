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

        return CFMLDocumentModel(functionDecls, functionCalls, varDecls, varUsages)
    }

    fun findInnermostEnclosingFunction(offset: Int, functions: List<CFMLFunctionDeclaration>): CFMLFunctionDeclaration?
    {
        return functions
            .filter { func ->
                func.bodyRange?.containsOffset(offset) == true || func.range.containsOffset(offset)
            }
            .minByOrNull { it.range.length }
    }

    fun isInsideRanges(offset: Int, ranges: List<TextRange>): Boolean
    {
        for (range in ranges)
        {
            if (range.containsOffset(offset)) return true
        }
        return false
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

    private fun extractScriptFunctions(
        text: String,
        commentRanges: List<TextRange>,
        functions: MutableList<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        // 1. Named functions: [modifiers] function [name] ( [params] )
        val funcPattern = Pattern.compile(
            """(?i)(?:^|[\s;{}])(?:(?:public|private|package|remote|static|final|abstract|default|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*function\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)"""
        )
        val matcher = funcPattern.matcher(text)
        while (matcher.find())
        {
            val name = matcher.group(1)
            val nameStart = matcher.start(1)
            val nameEnd = matcher.end(1)
            if (isInsideRanges(nameStart, commentRanges)) continue

            val paramsText = matcher.group(2)
            val paramsStart = matcher.start(2)

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

            val declRange = TextRange(matcher.start(), funcEnd)
            val nameRange = TextRange(nameStart, nameEnd)
            val paramDecls = mutableListOf<CFMLVariableDeclaration>()
            val funcDecl = CFMLFunctionDeclaration(name, nameRange, declRange, bodyRange, paramDecls)
            functions.add(funcDecl)

            // Parse parameters
            extractFunctionParameters(paramsText, paramsStart, funcDecl, paramDecls, varDecls)
        }

        // 2. Anonymous functions / Closures: function ( [params] ) { ... }
        val anonPattern = Pattern.compile(
            """(?i)(?:^|[\s;{}(=,:])(?:(?:public|private|package|remote|static|final|abstract|default|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*function\s*\(([^)]*)\)"""
        )
        val anonMatcher = anonPattern.matcher(text)
        while (anonMatcher.find())
        {
            val start = anonMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
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
        val arrowPattern = Pattern.compile(
            """(?i)(?:^|[\s;{}(=,:])(?:\(([^)]*)\)|([A-Za-z0-9_]+))\s*=>"""
        )
        val arrowMatcher = arrowPattern.matcher(text)
        while (arrowMatcher.find())
        {
            val start = arrowMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
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
        for (i in fromIndex until text.length)
        {
            if (isInsideRanges(i, commentRanges)) continue
            val c = text[i]
            if (c == ';') return -1 // Semicolon before brace means no body (interface/abstract)
            if (c == '{') return i
            if (c == '}' || c == '<') return -1
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
        val paramPattern = Pattern.compile("""(?i)(?:(?:required|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*([A-Za-z0-9_]+)(?:\s*=[^,]*)?""")
        val pm = paramPattern.matcher(paramsText)
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

    private fun extractTagFunctions(
        text: String,
        commentRanges: List<TextRange>,
        functions: MutableList<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        val tagPattern = Pattern.compile("""(?i)<cffunction\b([^>]*)>""", Pattern.DOTALL)
        val matcher = tagPattern.matcher(text)
        while (matcher.find())
        {
            val start = matcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = matcher.group(1)
            val nameMatcher = Pattern.compile("""(?i)\bname\s*=\s*["']?([A-Za-z0-9_]+)["']?""").matcher(attrs)
            if (nameMatcher.find())
            {
                val name = nameMatcher.group(1)
                val nameStart = matcher.start(1) + nameMatcher.start(1)
                val nameEnd = matcher.start(1) + nameMatcher.end(1)
                val nameRange = TextRange(nameStart, nameEnd)

                // Find closing </cffunction>
                val closeTagPattern = Pattern.compile("""(?i)</cffunction>""")
                val closeMatcher = closeTagPattern.matcher(text)
                var bodyRange: TextRange? = null
                var tagEnd = matcher.end()
                if (closeMatcher.find(matcher.end()))
                {
                    tagEnd = closeMatcher.end()
                    bodyRange = TextRange(matcher.end(), closeMatcher.start())
                }
                val paramDecls = mutableListOf<CFMLVariableDeclaration>()
                val funcDecl = CFMLFunctionDeclaration(name, nameRange, TextRange(start, tagEnd), bodyRange, paramDecls)
                functions.add(funcDecl)

                // Check for <cfargument> inside this function
                if (bodyRange != null)
                {
                    val bodyText = text.substring(bodyRange.startOffset, bodyRange.endOffset)
                    val argPattern = Pattern.compile("""(?i)<cfargument\b([^>]*)>""")
                    val argMatcher = argPattern.matcher(bodyText)
                    while (argMatcher.find())
                    {
                        val argAttrs = argMatcher.group(1)
                        val argNameM = Pattern.compile("""(?i)\bname\s*=\s*["']?([A-Za-z0-9_]+)["']?""").matcher(argAttrs)
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

    private fun extractScriptVariableDeclarations(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        // 1. var declarations: var a = 1, b = 2; or var string x = "hello"; or var x;
        val varPattern = Pattern.compile("""(?i)\bvar\s+""")
        val varMatcher = varPattern.matcher(text)
        while (varMatcher.find())
        {
            val varStart = varMatcher.start()
            if (isInsideRanges(varStart, commentRanges)) continue
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
                if (decl != null && varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 2. Typed declarations without 'var': string x = ...; numeric count = ...;
        val typedPattern = Pattern.compile("""(?i)(?:^|[\s;{}])(string|numeric|number|boolean|bool|array|struct|query|date|any)\s+([A-Za-z0-9_]+)\s*(?:=|;|,|\))""")
        val typedMatcher = typedPattern.matcher(text)
        while (typedMatcher.find())
        {
            val name = typedMatcher.group(2)
            val nameStart = typedMatcher.start(2)
            val nameEnd = typedMatcher.end(2)
            if (isInsideRanges(nameStart, commentRanges)) continue
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
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 3. For loop variables: for (var i in items) or for (i in items) or for (var i = 1; ...)
        val forPattern = Pattern.compile("""(?i)\bfor\s*\(\s*(?:var\s+)?([A-Za-z0-9_]+)\s*(?:in|=|;)""")
        val forMatcher = forPattern.matcher(text)
        while (forMatcher.find())
        {
            val name = forMatcher.group(1)
            val nameStart = forMatcher.start(1)
            val nameEnd = forMatcher.end(1)
            if (isInsideRanges(nameStart, commentRanges)) continue
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
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 4. Catch block variable: catch (any e) or catch (CustomException e) or catch (e)
        val catchPattern = Pattern.compile("""(?i)\bcatch\s*\(\s*(?:(?:any|[A-Za-z0-9_$.]+)\s+)?([A-Za-z0-9_]+)\s*\)""")
        val catchMatcher = catchPattern.matcher(text)
        while (catchMatcher.find())
        {
            val name = catchMatcher.group(1)
            val nameStart = catchMatcher.start(1)
            val nameEnd = catchMatcher.end(1)
            if (isInsideRanges(nameStart, commentRanges)) continue
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
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 5. Param declarations: param name="x" default="1"; or param string x = 1; or param x = 1;
        val paramPattern = Pattern.compile("""(?i)\bparam\s+(?:name\s*=\s*["']?((?:local\.)?([A-Za-z0-9_]+))["']?|(?:(?:string|numeric|number|boolean|bool|array|struct|query|date|any)\s+)?([A-Za-z0-9_]+)\s*=)""")
        val paramMatcher = paramPattern.matcher(text)
        while (paramMatcher.find())
        {
            val rawName = if (paramMatcher.group(1) != null) paramMatcher.group(1) else paramMatcher.group(3) ?: continue
            val name = CFMLDocumentModel.cleanVariableName(rawName)
            val nameStart = if (paramMatcher.group(1) != null) paramMatcher.start(1) + (rawName.length - name.length) else paramMatcher.start(3)
            val nameEnd = nameStart + name.length
            if (isInsideRanges(nameStart, commentRanges)) continue
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
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 6. Plain and scoped assignments: x = ... or local.x = ... or variables.x = ...
        val assignPattern = Pattern.compile("""(?i)(?:^|[\s;{}])((?:(?:local|variables)\.)?([A-Za-z0-9_]+))\s*(?:=|\+=|-=|\*=|/=|&=)(?!=)""")
        val assignMatcher = assignPattern.matcher(text)
        while (assignMatcher.find())
        {
            val fullVar = assignMatcher.group(1)
            val name = assignMatcher.group(2)
            val nameStart = assignMatcher.start(2)
            val nameEnd = assignMatcher.end(2)
            if (isInsideRanges(nameStart, commentRanges)) continue
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
                val isLocal = fullVar.lowercase().startsWith("local.") || enclosingFunc != null
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(assignMatcher.start(1), assignMatcher.end(1)),
                    isLocal = isLocal,
                    enclosingFunction = enclosingFunc
                )
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
                {
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

        val pattern = Pattern.compile("""(?i)((?:(?:local|variables)\.)?([A-Za-z0-9_]+))\s*$""")
        val m = pattern.matcher(lhs)
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
                    isLocal = enclosingFunc != null || fullVar.lowercase().startsWith("local."),
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

        for (i in start until text.length)
        {
            if (isInsideRanges(i, commentRanges)) continue
            val c = text[i]

            if (inSingleQuote)
            {
                if (c == '\'') inSingleQuote = false
                continue
            }
            if (inDoubleQuote)
            {
                if (c == '"') inDoubleQuote = false
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

        for (i in 0 until stmtText.length)
        {
            val absOffset = baseOffset + i
            if (isInsideRanges(absOffset, commentRanges)) continue
            val c = stmtText[i]

            if (inSingleQuote)
            {
                if (c == '\'') inSingleQuote = false
                continue
            }
            if (inDoubleQuote)
            {
                if (c == '"') inDoubleQuote = false
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
        // 1. <cfset var x = ...> or <cfset x = ...> or <cfset local.x = ...> or <cfset variables.x = ...>
        val cfsetPattern = Pattern.compile("""(?i)<cfset\s+(?:var\s+)?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))\s*(?:=|\+=|-=|\*=|/=|&=)""", Pattern.DOTALL)
        val cfsetMatcher = cfsetPattern.matcher(text)
        while (cfsetMatcher.find())
        {
            val start = cfsetMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val fullVar = cfsetMatcher.group(1)
            val name = cfsetMatcher.group(2)
            val nameStart = cfsetMatcher.start(2)
            val nameEnd = cfsetMatcher.end(2)
            val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
            val isLocal = fullVar.lowercase().startsWith("local.") || cfsetMatcher.group().lowercase().contains("var ") || (enclosingFunc != null)
            val decl = CFMLVariableDeclaration(
                name,
                TextRange(nameStart, nameEnd),
                TextRange(cfsetMatcher.start(1), cfsetMatcher.end(1)),
                isLocal = isLocal,
                enclosingFunction = enclosingFunc
            )
            if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
            {
                varDecls.add(decl)
            }
        }

        // 2. <cfparam name="myVar" default="...">
        val cfparamPattern = Pattern.compile("""(?i)<cfparam\b([^>]*)>""")
        val cfparamMatcher = cfparamPattern.matcher(text)
        while (cfparamMatcher.find())
        {
            val start = cfparamMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = cfparamMatcher.group(1)
            val nameM = Pattern.compile("""(?i)\bname\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""").matcher(attrs)
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
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 3. <cfloop index="i" ...> or item="item" or query="q"
        val cfloopPattern = Pattern.compile("""(?i)<cfloop\b([^>]*)>""")
        val cfloopMatcher = cfloopPattern.matcher(text)
        while (cfloopMatcher.find())
        {
            val start = cfloopMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = cfloopMatcher.group(1)
            val nameM = Pattern.compile("""(?i)\b(?:index|item|query)\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""").matcher(attrs)
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
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc }) {
                    varDecls.add(decl)
                }
            }
        }

        // 4. <cfquery name="qResult" ...> or <cfhttp result="res" ...> or <cfdirectory name="dir" ...> or <cffile variable="content" ...>
        val tagDeclPattern = Pattern.compile("""(?i)<(?:cfquery|cfdirectory|cfprocresult|cfhttp|cffile)\b([^>]*)>""")
        val tagDeclMatcher = tagDeclPattern.matcher(text)
        while (tagDeclMatcher.find())
        {
            val start = tagDeclMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = tagDeclMatcher.group(1)
            val nameM = Pattern.compile("""(?i)\b(?:name|result|variable)\s*=\s*["']?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))["']?""").matcher(attrs)
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
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 5. <cfcatch type="...">
        val cfcatchPattern = Pattern.compile("""(?i)<cfcatch\b([^>]*)>""")
        val cfcatchMatcher = cfcatchPattern.matcher(text)
        while (cfcatchMatcher.find())
        {
            val start = cfcatchMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val enclosingFunc = findInnermostEnclosingFunction(start, functions)
            val decl = CFMLVariableDeclaration(
                "cfcatch",
                TextRange(start + 1, start + 8),
                TextRange(start, cfcatchMatcher.end()),
                isLocal = true,
                enclosingFunction = enclosingFunc
            )
            if (varDecls.none { it.name.equals("cfcatch", ignoreCase = true) && it.enclosingFunction == enclosingFunc })
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
        // 1. Script function calls: foo(...)
        val callPattern = Pattern.compile("""(?i)(?:^|[^A-Za-z0-9_$.])([A-Za-z0-9_]+)\s*\(""")
        val matcher = callPattern.matcher(text)
        while (matcher.find())
        {
            val name = matcher.group(1)
            val nameStart = matcher.start(1)
            val nameEnd = matcher.end(1)
            if (isInsideRanges(nameStart, commentRanges)) continue

            // Exclude control flow keywords like if (, while (, for (, switch (, catch (
            if (CONTROL_KEYWORDS.contains(name.lowercase())) continue

            // Exclude function declaration itself
            if (functions.any { it.nameRange.containsOffset(nameStart) }) continue

            val enclosingFunc = findInnermostEnclosingFunction(nameStart, functions)
            functionCalls.add(CFMLFunctionCall(name, TextRange(nameStart, nameEnd), enclosingFunc))
        }

        // 2. <cfinvoke ... method="funcName" ...>
        val invokePattern = Pattern.compile("""(?i)<cfinvoke\b([^>]*)>""")
        val invokeMatcher = invokePattern.matcher(text)
        while (invokeMatcher.find())
        {
            val start = invokeMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = invokeMatcher.group(1)
            val methodM = Pattern.compile("""(?i)\bmethod\s*=\s*["']?([A-Za-z0-9_]+)["']?""").matcher(attrs)
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
        // Match identifier tokens: (optional scope.)varName
        val idPattern = Pattern.compile("""(?i)(?:(?:local|variables|arguments|session|application|request|this)\.)?([A-Za-z0-9_]+)""")
        val matcher = idPattern.matcher(text)
        while (matcher.find())
        {
            val fullToken = matcher.group()
            val bareName = matcher.group(1)
            val nameStart = matcher.start(1)
            val nameEnd = matcher.end(1)
            val fullStart = matcher.start()
            val fullEnd = matcher.end()

            if (isInsideRanges(nameStart, commentRanges)) continue

            // Check if keyword
            if (KEYWORDS.contains(bareName.lowercase())) continue

            // Check if it's a function declaration identifier
            if (functions.any { it.nameRange.containsOffset(nameStart) }) continue

            // Check if it's a function call identifier
            if (functionCalls.any { it.range.containsOffset(nameStart) }) continue

            // Check if it's a variable declaration identifier itself
            if (varDecls.any { it.nameRange.containsOffset(nameStart) }) continue

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
            val prevTagOpen = text.lastIndexOf('<', start)
            val prevTagClose = text.lastIndexOf('>', start)
            if (prevTagOpen > prevTagClose)
            {
                return true
            }
        }
        return false
    }

    private fun findMatchingBrace(text: String, openingBrace: Int, commentRanges: List<TextRange>): Int
    {
        var depth = 0
        for (i in openingBrace until text.length) {
            if (isInsideRanges(i, commentRanges)) continue
            val c = text[i]
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
        }
        return -1
    }
}
