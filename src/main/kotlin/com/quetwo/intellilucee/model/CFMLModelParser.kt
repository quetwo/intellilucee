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

    private fun isInsideRanges(offset: Int, ranges: List<TextRange>): Boolean
    {
        for (range in ranges)
        {
            if (range.containsOffset(offset)) return true
        }
        return false
    }

    private fun findCommentRanges(text: String): List<TextRange>
    {
        val ranges = mutableListOf<TextRange>()
        var i = 0
        val len = text.length
        while (i < len) {
            // CFML tag comments: <!--- ... --->
            if (i + 4 < len && text.startsWith("<!---", i))
            {
                val start = i
                var depth = 1
                i += 5
                while (i < len && depth > 0) {
                    if (i + 4 < len && text.startsWith("<!---", i))
                    {
                        depth++
                        i += 5
                    }
                    else if (i + 4 < len && text.startsWith("--->", i))
                    {
                        depth--
                        i += 5
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
            if (i + 3 < len && text.startsWith("<!--", i))
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
        // Match: [modifiers] function [name] ( [params] )
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
            val openBrace = text.indexOf('{', matcher.end())
            if (openBrace >= 0 && !isInsideRanges(openBrace, commentRanges))
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
            val funcDecl = CFMLFunctionDeclaration(name, nameRange, declRange, bodyRange)
            functions.add(funcDecl)

            // Parse parameters
            if (paramsText != null && paramsText.isNotBlank())
            {
                val paramPattern = Pattern.compile("""(?i)(?:(?:required|\b(?:void|any|string|numeric|number|boolean|bool|array|struct|query|date|[A-Za-z0-9_$.]+)\b)\s+)*([A-Za-z0-9_]+)(?:\s*=[^,]*)?""")
                val pm = paramPattern.matcher(paramsText)
                while (pm.find()) {
                    val pName = pm.group(1)
                    if (pName.isNotEmpty() && !KEYWORDS.contains(pName.lowercase()))
                    {
                        val pStart = paramsStart + pm.start(1)
                        val pEnd = paramsStart + pm.end(1)
                        val pNameRange = TextRange(pStart, pEnd)
                        val pFullRange = TextRange(paramsStart + pm.start(), paramsStart + pm.end())
                        val paramDecl = CFMLVariableDeclaration(pName, pNameRange, pFullRange, isLocal = true, enclosingFunction = funcDecl)
                        varDecls.add(paramDecl)
                    }
                }
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
            if (nameMatcher.find()) {
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
                val funcDecl = CFMLFunctionDeclaration(name, nameRange, TextRange(start, tagEnd), bodyRange)
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
                                enclosingFunction = funcDecl)
                            varDecls.add(paramDecl)
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
        // 1. var a = 1, b = 2; or var a;
        val varPattern = Pattern.compile("""(?i)(?:^|[\s;{}])var\s+([^;{}]+)[;{}]""")
        val matcher = varPattern.matcher(text)
        while (matcher.find())
        {
            val varList = matcher.group(1)
            val listStart = matcher.start(1)
            if (isInsideRanges(listStart, commentRanges)) continue
            val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(listStart) == true }

            val itemPattern = Pattern.compile("""(?i)([A-Za-z0-9_$.]+)(?:\s*=[^,]*)?""")
            val itemMatcher = itemPattern.matcher(varList)
            while (itemMatcher.find())
            {
                val varName = itemMatcher.group(1)
                val clean = CFMLDocumentModel.cleanVariableName(varName)
                if (clean.isNotEmpty() && !KEYWORDS.contains(clean.lowercase()))
                {
                    val nameStart = listStart + itemMatcher.start(1) + (varName.length - clean.length)
                    val nameEnd = listStart + itemMatcher.end(1)
                    val fullStart = listStart + itemMatcher.start()
                    val fullEnd = listStart + itemMatcher.end()
                    val decl = CFMLVariableDeclaration(
                        clean,
                        TextRange(nameStart, nameEnd),
                        TextRange(fullStart, fullEnd),
                        isLocal = enclosingFunc != null,
                        enclosingFunction = enclosingFunc                    )
                    varDecls.add(decl)
                }
            }
        }

        // 2. Typed declarations: string x = ...; numeric count = ...;
        val typedPattern = Pattern.compile("""(?i)(?:^|[\s;{}])(string|numeric|number|boolean|bool|array|struct|query|date)\s+([A-Za-z0-9_]+)\s*(?:=|;|,|\))""")
        val typedMatcher = typedPattern.matcher(text)
        while (typedMatcher.find())
        {
            val name = typedMatcher.group(2)
            val nameStart = typedMatcher.start(2)
            val nameEnd = typedMatcher.end(2)
            if (isInsideRanges(nameStart, commentRanges)) continue
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(typedMatcher.start(), typedMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc)
                // avoid duplicate if already recorded
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 3. For loop variables: for (var i in items) or for (var i = 1; ...) or for (i in items)
        val forPattern = Pattern.compile("""(?i)\bfor\s*\(\s*(?:var\s+)?([A-Za-z0-9_]+)\s*(?:in|=)""")
        val forMatcher = forPattern.matcher(text)
        while (forMatcher.find())
        {
            val name = forMatcher.group(1)
            val nameStart = forMatcher.start(1)
            val nameEnd = forMatcher.end(1)
            if (isInsideRanges(nameStart, commentRanges)) continue
            if (!KEYWORDS.contains(name.lowercase()))
            {
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(forMatcher.start(), forMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc)
                if (varDecls.none { it.nameRange == decl.nameRange })
                {
                    varDecls.add(decl)
                }
            }
        }

        // 4. local.x = ... or variables.x = ... (explicit declaration/assignment)
        val scopedPattern = Pattern.compile("""(?i)(?:^|[\s;{}])((?:local|variables)\.([A-Za-z0-9_]+))\s*=""")
        val scopedMatcher = scopedPattern.matcher(text)
        while (scopedMatcher.find())
        {
            val fullVar = scopedMatcher.group(1)
            val name = scopedMatcher.group(2)
            val nameStart = scopedMatcher.start(2)
            val nameEnd = scopedMatcher.end(2)
            if (isInsideRanges(nameStart, commentRanges)) continue
            val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
            val isLocal = fullVar.lowercase().startsWith("local.") || enclosingFunc != null
            val decl = CFMLVariableDeclaration(
                name,
                TextRange(nameStart, nameEnd),
                TextRange(scopedMatcher.start(1), scopedMatcher.end(1)),
                isLocal = isLocal,
                enclosingFunction = enclosingFunc)
            // Add if not already declared in this scope
            if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
            {
                varDecls.add(decl)
            }
        }
    }

    private fun extractTagVariableDeclarations(
        text: String,
        commentRanges: List<TextRange>,
        functions: List<CFMLFunctionDeclaration>,
        varDecls: MutableList<CFMLVariableDeclaration>)
    {
        // 1. <cfset var x = ...> or <cfset x = ...> or <cfset local.x = ...> or <cfset variables.x = ...>
        val cfsetPattern = Pattern.compile("""(?i)<cfset\s+(?:var\s+)?((?:(?:local|variables)\.)?([A-Za-z0-9_]+))\s*=""", Pattern.DOTALL)
        val cfsetMatcher = cfsetPattern.matcher(text)
        while (cfsetMatcher.find())
        {
            val start = cfsetMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val fullVar = cfsetMatcher.group(1)
            val name = cfsetMatcher.group(2)
            val nameStart = cfsetMatcher.start(2)
            val nameEnd = cfsetMatcher.end(2)
            val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
            val isLocal = fullVar.lowercase().startsWith("local.") || cfsetMatcher.group().lowercase().contains("var ") || (enclosingFunc != null)
            val decl = CFMLVariableDeclaration(
                name,
                TextRange(nameStart, nameEnd),
                TextRange(cfsetMatcher.start(), cfsetMatcher.end()),
                isLocal = isLocal,
                enclosingFunction = enclosingFunc)
            if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
            {
                varDecls.add(decl)
            }
        }

        // 2. <cfparam name="x" ...>
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
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
                val isLocal = fullVar.lowercase().startsWith("local.") || enclosingFunc != null
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
            val nameM = Pattern.compile("""(?i)\b(?:index|item|query)\s*=\s*["']?([A-Za-z0-9_]+)["']?""").matcher(attrs)
            if (nameM.find())
            {
                val name = nameM.group(1)
                val nameStart = cfloopMatcher.start(1) + nameM.start(1)
                val nameEnd = cfloopMatcher.start(1) + nameM.end(1)
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(cfloopMatcher.start(), cfloopMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc
                )
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc }) {
                    varDecls.add(decl)
                }
            }
        }

        // 4. <cfquery name="qResult" ...> or <cfhttp result="res" ...> or <cfdirectory name="dir" ...>
        val tagDeclPattern = Pattern.compile("""(?i)<(?:cfquery|cfdirectory|cfprocresult)\b([^>]*)>""")
        val tagDeclMatcher = tagDeclPattern.matcher(text)
        while (tagDeclMatcher.find())
        {
            val start = tagDeclMatcher.start()
            if (isInsideRanges(start, commentRanges)) continue
            val attrs = tagDeclMatcher.group(1)
            val nameM = Pattern.compile("""(?i)\bname\s*=\s*["']?([A-Za-z0-9_]+)["']?""").matcher(attrs)
            if (nameM.find())
            {
                val name = nameM.group(1)
                val nameStart = tagDeclMatcher.start(1) + nameM.start(1)
                val nameEnd = tagDeclMatcher.start(1) + nameM.end(1)
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
                val decl = CFMLVariableDeclaration(
                    name,
                    TextRange(nameStart, nameEnd),
                    TextRange(tagDeclMatcher.start(), tagDeclMatcher.end()),
                    isLocal = enclosingFunc != null,
                    enclosingFunction = enclosingFunc
                )
                if (varDecls.none { it.name.equals(name, ignoreCase = true) && it.enclosingFunction == enclosingFunc })
                {
                    varDecls.add(decl)
                }
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

            val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
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
                val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
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
            if (isTagAttributeName(text, nameStart, nameEnd))
            {
                continue
            }

            val enclosingFunc = functions.firstOrNull { it.bodyRange?.containsOffset(nameStart) == true }
            varUsages.add(CFMLVariableUsage(bareName, TextRange(nameStart, nameEnd), enclosingFunc))
        }
    }

    private fun isTagAttributeName(text: String, start: Int, end: Int): Boolean {
        // Check if preceding is inside a tag and following is '='
        var nextIdx = end
        while (nextIdx < text.length && text[nextIdx].isWhitespace())
        {
            nextIdx++
        }
        if (nextIdx < text.length && text[nextIdx] == '=')
        {
            // Check if we are inside a tag `<tag ... attr=`
            var prevTagOpen = text.lastIndexOf('<', start)
            var prevTagClose = text.lastIndexOf('>', start)
            if (prevTagOpen > prevTagClose)
            {
                // We are inside `<...>`
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
