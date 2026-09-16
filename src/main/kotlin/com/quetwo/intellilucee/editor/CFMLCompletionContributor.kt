package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.model.CFMLDocumentModel
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import com.quetwo.intellilucee.utils.PathUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class CFMLCompletionContributor : CompletionContributor()
{

    data class PrecedingVarInfo(val varName: String, val dotOffset: Int)
    data class ComponentCompletionInfo(val prefix: String)

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet)
    {
        val file = parameters.originalFile
        if (!CFMLPsiUtil.isCFMLFile(file))
        {
            return
        }

        val chars = parameters.editor.document.charsSequence
        val posOffset = parameters.position.textRange.startOffset

        val componentInfo = findComponentCompletion(chars, posOffset)
            ?: findComponentCompletion(chars, parameters.offset)

        if (componentInfo != null)
        {
            fillComponentVariants(file, componentInfo, result)
            return
        }

        val varInfo = findPrecedingVariable(chars, posOffset)
            ?: findPrecedingVariable(chars, parameters.offset)

        if (varInfo != null)
        {
            val dotNotation = resolveComponentDotNotation(file, varInfo.varName, posOffset)
                ?: resolveComponentDotNotation(parameters.position.containingFile, varInfo.varName, posOffset)

            if (dotNotation != null)
            {
                val cfcModel = resolveCfcModel(file, dotNotation)
                if (cfcModel != null)
                {
                    for (func in cfcModel.functions)
                    {
                        val paramStr = func.parameters.joinToString(", ") { it.name }
                        val tailText = "($paramStr)"
                        val element = LookupElementBuilder.create(func.name)
                            .withIcon(AllIcons.Nodes.Method)
                            .withTailText(tailText, true)
                            .withTypeText("method", true)
                            .withInsertHandler(ParenthesesInsertHandler.getInstance(func.parameters.isNotEmpty()))
                        result.addElement(PrioritizedLookupElement.withPriority(element, 1000.0))
                    }

                    val addedVars = mutableSetOf<String>()
                    val componentVars = cfcModel.variableDeclarations.filter { !it.isLocal || it.enclosingFunction == null }
                    for (varDecl in componentVars)
                    {
                        if (addedVars.add(varDecl.name.lowercase()))
                        {
                            val element = LookupElementBuilder.create(varDecl.name)
                                .withIcon(AllIcons.Nodes.Variable)
                                .withTypeText("variable", true)
                            result.addElement(PrioritizedLookupElement.withPriority(element, 999.0))
                        }
                    }
                    return
                }
            }
            return
        }

        if (isAssigningVariable(chars, posOffset) || isAssigningVariable(chars, parameters.offset))
        {
            fillAssignmentVariants(parameters, chars, posOffset, result)
        }
    }

    private fun fillComponentVariants(
        file: PsiFile,
        componentInfo: ComponentCompletionInfo,
        result: CompletionResultSet
    )
    {
        val project = file.project
        var componentList = PathUtils.ListAllComponents(project)
        if (componentList.isEmpty())
        {
            val vFile = file.virtualFile ?: file.originalFile.virtualFile ?: file.viewProvider.virtualFile
            if (vFile != null)
            {
                var root = if (vFile.isDirectory) vFile else vFile.parent
                while (root?.parent != null)
                {
                    root = root.parent
                }
                if (root != null)
                {
                    componentList = PathUtils.ListAllComponents(root)
                }
            }
        }

        val addedNames = mutableSetOf<String>()
        val resultSet = if (componentInfo.prefix.isNotEmpty())
        {
            result.withPrefixMatcher(componentInfo.prefix)
        }
        else
        {
            result
        }

        for (descriptor in componentList)
        {
            val lookupText = descriptor.dotNotation?.takeIf { it.isNotBlank() }
                ?: descriptor.cfcPath?.nameWithoutExtension?.takeIf { it.isNotBlank() }
                ?: descriptor.file?.nameWithoutExtension?.takeIf { it.isNotBlank() }
                ?: continue

            if (addedNames.add(lookupText))
            {
                val element = LookupElementBuilder.create(lookupText)
                    .withIcon(CFMLIcon.FILE_CFC)
                    .withTypeText("component", true)
                resultSet.addElement(PrioritizedLookupElement.withPriority(element, 1000.0))
            }
        }
        result.stopHere()
    }

    private fun fillAssignmentVariants(
        parameters: CompletionParameters,
        chars: CharSequence,
        posOffset: Int,
        result: CompletionResultSet
    )
    {
        val model = CFMLModelParser.parse(chars.toString())
        val currentFunc = model.findEnclosingFunction(posOffset)
            ?: model.findEnclosingFunction(parameters.offset)

        // 1. Add functions from current document
        val addedFunctions = mutableSetOf<String>()
        for (func in model.functions)
        {
            if (addedFunctions.add(func.name.lowercase()))
            {
                val paramStr = func.parameters.joinToString(", ") { it.name }
                val tailText = "($paramStr)"
                val element = LookupElementBuilder.create(func.name)
                    .withIcon(AllIcons.Nodes.Function)
                    .withTailText(tailText, true)
                    .withTypeText("function", true)
                    .withInsertHandler(ParenthesesInsertHandler.getInstance(func.parameters.isNotEmpty()))
                result.addElement(PrioritizedLookupElement.withPriority(element, 1000.0))
            }
        }

        // 2. Add in-scope variables from current document
        val addedVars = mutableSetOf<String>()
        val inScopeVars = model.variableDeclarations.filter { decl ->
            if (decl.enclosingFunction == null || !decl.isLocal)
            {
                true
            }
            else
            {
                currentFunc != null && decl.enclosingFunction == currentFunc
            }
        }

        for (varDecl in inScopeVars)
        {
            val cleanName = CFMLDocumentModel.cleanVariableName(varDecl.name)
            if (cleanName.isNotEmpty() && addedVars.add(cleanName.lowercase()))
            {
                val element = LookupElementBuilder.create(cleanName)
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText("variable", true)
                result.addElement(PrioritizedLookupElement.withPriority(element, 999.0))
            }
        }
    }

    companion object
    {

        fun findPrecedingVariable(chars: CharSequence, offset: Int): PrecedingVarInfo?
        {
            if (offset <= 0 || offset > chars.length) return null

            var i = offset - 1

            // Skip any typed prefix characters (e.g. user.get)
            while (i >= 0 && (chars[i].isLetterOrDigit() || chars[i] == '_' || chars[i] == '$'))
            {
                i--
            }

            // Skip trailing whitespace before dot if any
            while (i >= 0 && (chars[i] == ' ' || chars[i] == '\t'))
            {
                i--
            }

            // Must be at a dot
            if (i < 0 || chars[i] != '.')
            {
                return null
            }
            val dotOffset = i
            i--

            // Skip whitespace before dot
            while (i >= 0 && (chars[i] == ' ' || chars[i] == '\t'))
            {
                i--
            }

            val varNameEnd = i + 1
            while (i >= 0 && (chars[i].isLetterOrDigit() || chars[i] == '_' || chars[i] == '$'))
            {
                i--
            }
            val varNameStart = i + 1
            if (varNameStart >= varNameEnd)
            {
                return null
            }

            var rawVarName = chars.subSequence(varNameStart, varNameEnd).toString()

            // Check if preceded by scope like local. or variables. or this.
            if (i >= 0 && chars[i] == '.')
            {
                val scopeDot = i
                var s = scopeDot - 1
                while (s >= 0 && (chars[s].isLetterOrDigit() || chars[s] == '_' || chars[s] == '$'))
                {
                    s--
                }
                val scopeStart = s + 1
                if (scopeStart < scopeDot)
                {
                    val scopeName = chars.subSequence(scopeStart, scopeDot).toString().lowercase()
                    if (scopeName in setOf("local", "variables", "this", "request", "session", "application", "arguments"))
                    {
                        rawVarName = "$scopeName.$rawVarName"
                    }
                }
            }

            val cleanName = CFMLDocumentModel.cleanVariableName(rawVarName)
            if (cleanName.isEmpty())
            {
                return null
            }

            return PrecedingVarInfo(cleanName, dotOffset)
        }

        fun resolveComponentDotNotation(file: PsiFile, varName: String, offset: Int): String?
        {
            val text = file.text
            val model = CFMLModelParser.parse(text)
            val enclosingFunc = model.findEnclosingFunction(offset)

            if (enclosingFunc?.bodyRange != null)
            {
                val funcText = text.substring(enclosingFunc.bodyRange.startOffset, enclosingFunc.bodyRange.endOffset)
                val dotNotation = extractCreateObjectDotNotation(funcText, varName)
                if (dotNotation != null)
                {
                    return dotNotation
                }
            }

            return extractCreateObjectDotNotation(text, varName)
        }

        fun extractCreateObjectDotNotation(text: String, varName: String): String?
        {
            val commentRanges = CFMLModelParser.findCommentRanges(text)
            val createObjectPattern = Pattern.compile("""(?i)\bcreateObject\s*\(([^)]+)\)""")
            val matcher = createObjectPattern.matcher(text)

            while (matcher.find())
            {
                val callStart = matcher.start()
                if (CFMLModelParser.isInsideRanges(callStart, commentRanges))
                {
                    continue
                }

                val argsText = matcher.group(1)
                val dotNotation = parseCreateObjectArguments(argsText) ?: continue

                // Verify this createObject call is assigned to varName
                val beforeCall = text.substring(0, callStart)
                if (isAssignedToVariable(beforeCall, varName, commentRanges))
                {
                    return dotNotation
                }
            }

            return null
        }

        private fun isAssignedToVariable(beforeCall: String, varName: String, commentRanges: List<TextRange>): Boolean
        {
            var i = beforeCall.length - 1

            // Skip whitespace, quotes, # characters, and comments
            while (i >= 0)
            {
                val c = beforeCall[i]
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '#' || c == '"' || c == '\'')
                {
                    i--
                    continue
                }
                break
            }

            if (i < 0) return false

            // Look back for assignment '=' or 'default='
            val lookbackStart = Math.max(0, i - 300)
            val chunk = beforeCall.substring(lookbackStart, i + 1)

            val tagParamPattern = Pattern.compile(
                """(?i)<cfparam\b[^>]*\bname\s*=\s*["']?(?:(?:local|variables|this)\.)?\Q$varName\E["']?[^>]*\bdefault\s*=\s*$"""
            )
            val scriptParamPattern = Pattern.compile(
                """(?i)\bparam\b[^;]*\bname\s*=\s*["']?(?:(?:local|variables|this)\.)?\Q$varName\E["']?[^;]*\bdefault\s*=\s*$"""
            )
            val assignPattern = Pattern.compile(
                """(?i)(?:^|[\s;{}<(])(?:<cfset\s+(?:var\s+)?|var\s+|param\s+|(?:string|numeric|boolean|any|component|[A-Za-z0-9_$.]+)\s+)?(?:(?:local|variables|this)\.)?\Q$varName\E\s*=\s*$"""
            )

            return tagParamPattern.matcher(chunk).find() ||
                   scriptParamPattern.matcher(chunk).find() ||
                   assignPattern.matcher(chunk).find()
        }

        fun parseCreateObjectArguments(argsText: String): String?
        {
            val args = splitArguments(argsText)
            if (args.isEmpty()) return null

            // 1. Positional argument check: createObject("component", "path.to.CFC")
            val firstArg = unquote(args[0]).trim()
            if (firstArg.equals("component", ignoreCase = true) && args.size >= 2)
            {
                val secondArg = unquote(args[1]).trim()
                if (secondArg.isNotEmpty())
                {
                    return secondArg
                }
            }

            // 2. 3-argument positional check: createObject(..., "component", "path.to.CFC")
            if (args.size >= 3)
            {
                val secondArg = unquote(args[1]).trim()
                if (secondArg.equals("component", ignoreCase = true))
                {
                    val thirdArg = unquote(args[2]).trim()
                    if (thirdArg.isNotEmpty())
                    {
                        return thirdArg
                    }
                }
            }

            // 3. Named arguments check: createObject(type="component", component="path.to.CFC")
            var hasTypeComponent = false
            var componentPath: String? = null

            for (arg in args)
            {
                val eqIdx = arg.indexOf('=')
                if (eqIdx > 0)
                {
                    val paramName = arg.substring(0, eqIdx).trim().lowercase()
                    val paramVal = unquote(arg.substring(eqIdx + 1).trim()).trim()
                    if (paramName == "type" && paramVal.equals("component", ignoreCase = true))
                    {
                        hasTypeComponent = true
                    }
                    if (paramName in setOf("component", "class", "classname"))
                    {
                        componentPath = paramVal
                    }
                }
            }

            if (hasTypeComponent && !componentPath.isNullOrEmpty())
            {
                return componentPath
            }

            return null
        }

        private fun splitArguments(argsText: String): List<String>
        {
            val result = mutableListOf<String>()
            var inSingle = false
            var inDouble = false
            var parenDepth = 0
            var start = 0

            for (i in 0 until argsText.length)
            {
                val c = argsText[i]
                if (inSingle)
                {
                    if (c == '\'') inSingle = false
                    continue
                }
                if (inDouble)
                {
                    if (c == '"') inDouble = false
                    continue
                }
                when (c)
                {
                    '\'' -> inSingle = true
                    '"' -> inDouble = true
                    '(' -> parenDepth++
                    ')' -> if (parenDepth > 0) parenDepth--
                    ',' ->
                    {
                        if (parenDepth == 0)
                        {
                            result.add(argsText.substring(start, i).trim())
                            start = i + 1
                        }
                    }
                }
            }

            if (start < argsText.length)
            {
                result.add(argsText.substring(start).trim())
            }

            return result
        }

        private fun unquote(s: String): String
        {
            val trimmed = s.trim()
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))
            )
            {
                if (trimmed.length >= 2)
                {
                    return trimmed.substring(1, trimmed.length - 1)
                }
            }
            return trimmed
        }

        fun resolveCfcModel(file: PsiFile, dotNotation: String): CFMLDocumentModel?
        {
            val vFile = file.virtualFile ?: file.originalFile.virtualFile ?: file.viewProvider.virtualFile
            val project = file.project

            // 1. Try PathUtils.DotNotationToFile
            val cfcFile = PathUtils.DotNotationToFile(vFile, dotNotation)
            if (cfcFile != null && cfcFile.exists())
            {
                val model = getCfcModel(project, cfcFile)
                if (model != null) return model
            }

            // 2. Try PathUtils.DotNotationToVirtualFile
            val cfcVFile = PathUtils.DotNotationToVirtualFile(vFile, dotNotation)
            if (cfcVFile != null)
            {
                val psi = PsiManager.getInstance(project).findFile(cfcVFile)
                if (psi != null)
                {
                    return CFMLPsiUtil.getModel(psi)
                }
            }

            // 3. Fallbacks using project base path
            val basePath = project.basePath
            if (basePath != null)
            {
                val pathFile = PathUtils.DotNotationToFile(Paths.get(basePath), dotNotation)
                if (pathFile != null && pathFile.exists())
                {
                    val model = getCfcModel(project, pathFile)
                    if (model != null) return model
                }
                val relPath = dotNotation.removeSuffix(".cfc").replace('.', File.separatorChar) + ".cfc"
                val direct = Paths.get(basePath, relPath).toFile()
                if (direct.exists())
                {
                    val model = getCfcModel(project, direct)
                    if (model != null) return model
                }
            }

            val relSlash = dotNotation.removeSuffix(".cfc").replace('.', '/') + ".cfc"
            var current = vFile.parent
            while (current != null)
            {
                val target = current.findFileByRelativePath(relSlash)
                if (target != null)
                {
                    val psi = PsiManager.getInstance(project).findFile(target)
                    if (psi != null)
                    {
                        return CFMLPsiUtil.getModel(psi)
                    }
                }
                current = current.parent
            }

            return null
        }

        fun getCfcModel(project: Project, cfcFile: File): CFMLDocumentModel?
        {
            try
            {
                val lfs = LocalFileSystem.getInstance()
                val vFile = lfs.findFileByIoFile(cfcFile) ?: lfs.refreshAndFindFileByIoFile(cfcFile)
                if (vFile != null)
                {
                    val psiFile = PsiManager.getInstance(project).findFile(vFile)
                    if (psiFile != null)
                    {
                        return CFMLPsiUtil.getModel(psiFile)
                    }
                }
                val text = Files.readString(cfcFile.toPath())
                return CFMLModelParser.parse(text)
            }
            catch (e: Exception)
            {
                return null
            }
        }

        fun isAssigningVariable(chars: CharSequence, offset: Int): Boolean
        {
            if (offset <= 0 || offset > chars.length) return false

            // 1. Check tag syntax: <cfset ...> or <cfparam ...>
            val tagAssign = checkTagAssignment(chars, offset)
            if (tagAssign != null)
            {
                return tagAssign
            }

            // 2. Check script syntax
            return checkScriptAssignment(chars, offset)
        }

        private fun checkTagAssignment(chars: CharSequence, offset: Int): Boolean?
        {
            var i = offset - 1
            var insideTag = false
            var tagStart = -1

            while (i >= 0)
            {
                val c = chars[i]
                if (c == '>')
                {
                    return null
                }
                if (c == '<')
                {
                    tagStart = i
                    insideTag = true
                    break
                }
                i--
            }

            if (!insideTag || tagStart < 0)
            {
                return null
            }

            val tagChunk = chars.subSequence(tagStart, offset).toString()
            val lower = tagChunk.lowercase()

            if (lower.startsWith("<cfset"))
            {
                val eqIndex = tagChunk.indexOf('=')
                return eqIndex in 6 until tagChunk.length
            }

            if (lower.startsWith("<cfparam"))
            {
                val defaultPattern = Pattern.compile("""(?i)\bdefault\s*=\s*["']?""")
                return defaultPattern.matcher(tagChunk).find()
            }

            return null
        }

        private fun checkScriptAssignment(chars: CharSequence, offset: Int): Boolean
        {
            var i = offset - 1
            var parenDepth = 0
            var bracketDepth = 0
            var stmtStart = 0

            while (i >= 0)
            {
                val c = chars[i]
                if (c == ')')
                {
                    parenDepth++
                }
                else if (c == '(')
                {
                    if (parenDepth > 0) parenDepth--
                }
                else if (c == ']')
                {
                    bracketDepth++
                }
                else if (c == '[')
                {
                    if (bracketDepth > 0) bracketDepth--
                }
                else if (parenDepth == 0 && bracketDepth == 0)
                {
                    if (c == ';' || c == '{' || c == '}')
                    {
                        stmtStart = i + 1
                        break
                    }
                    if (c == '\n' || c == '\r')
                    {
                        var prev = i - 1
                        while (prev >= 0 && (chars[prev] == ' ' || chars[prev] == '\t' || chars[prev] == '\r'))
                        {
                            prev--
                        }
                        if (prev >= 0)
                        {
                            val prevChar = chars[prev]
                            if (prevChar == '=' || prevChar == '+' || prevChar == '-' || prevChar == '*' ||
                                prevChar == '/' || prevChar == ',' || prevChar == '&' || prevChar == '|' ||
                                prevChar == '?' || prevChar == ':' || prevChar == '(' || prevChar == '[')
                            {
                                i = prev
                                continue
                            }
                        }
                        stmtStart = i + 1
                        break
                    }
                }
                i--
            }

            if (stmtStart >= offset) return false

            var stmtText = chars.subSequence(stmtStart, offset).toString().trim()
            if (stmtText.isEmpty()) return false

            stmtText = stmtText.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            stmtText = stmtText.replace(Regex("""//.*"""), "")
            stmtText = stmtText.trim()

            val forMatch = Regex("""(?i)^\bfor\s*\(\s*(.*)$""").find(stmtText)
            if (forMatch != null)
            {
                stmtText = forMatch.groupValues[1].trim()
            }

            val wordEnd = stmtText.indexOfFirst { !it.isLetterOrDigit() && it != '_' }
            if (wordEnd > 0)
            {
                val keyword = stmtText.substring(0, wordEnd).lowercase()
                if (keyword in setOf("if", "while", "switch", "catch", "return", "throw", "rethrow", "function", "interface", "component", "import", "include"))
                {
                    return false
                }
            }

            val eqIdx = findAssignmentEqualsIndex(stmtText)
            if (eqIdx < 0) return false

            var lhs = stmtText.substring(0, eqIdx).trim()
            if (lhs.contains(','))
            {
                lhs = lhs.substringAfterLast(',').trim()
            }

            return isValidAssignmentLhs(lhs)
        }

        private fun findAssignmentEqualsIndex(text: String): Int
        {
            var inSingle = false
            var inDouble = false
            var parenDepth = 0
            var bracketDepth = 0

            for (i in text.indices)
            {
                val c = text[i]
                if (inSingle)
                {
                    if (c == '\'') inSingle = false
                    continue
                }
                if (inDouble)
                {
                    if (c == '"') inDouble = false
                    continue
                }
                if (c == '\'')
                {
                    inSingle = true
                    continue
                }
                if (c == '"')
                {
                    inDouble = true
                    continue
                }
                if (c == '(')
                {
                    parenDepth++
                    continue
                }
                if (c == ')')
                {
                    if (parenDepth > 0) parenDepth--
                    continue
                }
                if (c == '[')
                {
                    bracketDepth++
                    continue
                }
                if (c == ']')
                {
                    if (bracketDepth > 0) bracketDepth--
                    continue
                }

                if (c == '=' && parenDepth == 0 && bracketDepth == 0)
                {
                    val prev = if (i > 0) text[i - 1] else ' '
                    val next = if (i + 1 < text.length) text[i + 1] else ' '
                    if (prev != '=' && prev != '!' && prev != '<' && prev != '>' && prev != '+' && prev != '-' && prev != '*' && prev != '/' && prev != '&' &&
                        next != '=' && next != '>')
                    {
                        return i
                    }
                    if ((prev == '+' || prev == '-' || prev == '*' || prev == '/' || prev == '&') && next != '=')
                    {
                        return i
                    }
                }
            }
            return -1
        }

        private fun isValidAssignmentLhs(lhs: String): Boolean
        {
            if (lhs.isEmpty()) return false

            if (lhs.lowercase().startsWith("param"))
            {
                return true
            }

            val varPattern = Pattern.compile(
                """(?i)^(?:var\s+)?(?:(?:string|numeric|number|boolean|bool|array|struct|query|date|any|[A-Za-z0-9_$.]+)\s+)?(?:(?:local|variables|this|request|session|application|arguments)\.)?[A-Za-z0-9_]+$"""
            )
            return varPattern.matcher(lhs).matches()
        }

        fun findComponentCompletion(chars: CharSequence, offset: Int): ComponentCompletionInfo?
        {
            return findComponentTagCompletion(chars, offset)
                ?: findFunctionCallComponentCompletion(chars, offset)
                ?: findScriptCfobjectCompletion(chars, offset)
        }

        fun findComponentTagCompletion(chars: CharSequence, offset: Int): ComponentCompletionInfo?
        {
            if (offset < 0 || offset > chars.length) return null

            var i = offset - 1
            while (i >= 0)
            {
                val c = chars[i]
                if (c == '>')
                {
                    return null
                }
                if (c == '<')
                {
                    break
                }
                i--
            }

            if (i < 0 || chars[i] != '<')
            {
                return null
            }

            val tagStart = i
            val tagChunk = chars.subSequence(tagStart, offset).toString()

            if (!Regex("""(?i)^<\s*cfobject\b""").containsMatchIn(tagChunk))
            {
                return null
            }

            val attrMatch = Regex("""(?i)\bcomponent\s*=\s*(["']?)([^"'>]*)$""").find(tagChunk)
                ?: return null

            return ComponentCompletionInfo(attrMatch.groupValues[2].trim())
        }

        fun findFunctionCallComponentCompletion(chars: CharSequence, offset: Int): ComponentCompletionInfo?
        {
            if (offset <= 0 || offset > chars.length) return null

            var i = offset - 1
            var parenDepth = 0
            var inSingle = false
            var inDouble = false
            var openParenIndex = -1

            while (i >= 0)
            {
                val c = chars[i]
                if (c == '\'' && !inDouble)
                {
                    inSingle = !inSingle
                }
                else if (c == '"' && !inSingle)
                {
                    inDouble = !inDouble
                }
                else if (!inSingle && !inDouble)
                {
                    if (c == ')')
                    {
                        parenDepth++
                    }
                    else if (c == '(')
                    {
                        if (parenDepth > 0)
                        {
                            parenDepth--
                        }
                        else
                        {
                            openParenIndex = i
                            break
                        }
                    }
                    else if (c == ';' || c == '{' || c == '}')
                    {
                        break
                    }
                }
                i--
            }

            if (openParenIndex < 0) return null

            var fnEnd = openParenIndex - 1
            while (fnEnd >= 0 && (chars[fnEnd] == ' ' || chars[fnEnd] == '\t' || chars[fnEnd] == '\n' || chars[fnEnd] == '\r'))
            {
                fnEnd--
            }
            if (fnEnd < 0) return null

            var fnStart = fnEnd
            while (fnStart >= 0 && (chars[fnStart].isLetterOrDigit() || chars[fnStart] == '_' || chars[fnStart] == '$'))
            {
                fnStart--
            }
            fnStart++

            if (fnStart > fnEnd) return null

            val functionName = chars.subSequence(fnStart, fnEnd + 1).toString()
            val isCreateComponent = functionName.equals("createComponent", ignoreCase = true)
            val isCreateObject = functionName.equals("createObject", ignoreCase = true)
            val isCfobjectFn = functionName.equals("cfobject", ignoreCase = true)

            if (!isCreateComponent && !isCreateObject && !isCfobjectFn)
            {
                return null
            }

            val argsInside = chars.subSequence(openParenIndex + 1, offset).toString()
            val argsList = splitArguments(argsInside)
            val currentArg = if (argsList.isNotEmpty()) argsList.last() else ""

            if (isCreateComponent)
            {
                val namedMatch = Regex("""(?i)^\s*component\s*=\s*(["']?)(.*)$""").find(currentArg)
                if (namedMatch != null)
                {
                    return ComponentCompletionInfo(cleanPrefix(namedMatch.groupValues[2]))
                }

                if (Regex("""(?i)^\s*[a-zA-Z0-9_]+\s*=""").containsMatchIn(currentArg))
                {
                    return null
                }

                if (argsList.size <= 1)
                {
                    return ComponentCompletionInfo(cleanPrefix(currentArg))
                }
                if (argsList.size == 2)
                {
                    val firstArg = unquote(argsList[0]).trim()
                    if (firstArg.equals("component", ignoreCase = true))
                    {
                        return ComponentCompletionInfo(cleanPrefix(currentArg))
                    }
                }
            }
            else if (isCreateObject)
            {
                val namedMatch = Regex("""(?i)^\s*component\s*=\s*(["']?)(.*)$""").find(currentArg)
                if (namedMatch != null)
                {
                    return ComponentCompletionInfo(cleanPrefix(namedMatch.groupValues[2]))
                }

                if (argsList.size == 2)
                {
                    val firstArg = unquote(argsList[0]).trim()
                    if (firstArg.equals("component", ignoreCase = true))
                    {
                        if (!Regex("""(?i)^\s*[a-zA-Z0-9_]+\s*=""").containsMatchIn(currentArg))
                        {
                            return ComponentCompletionInfo(cleanPrefix(currentArg))
                        }
                    }
                }
                else if (argsList.size == 3)
                {
                    val secondArg = unquote(argsList[1]).trim()
                    if (secondArg.equals("component", ignoreCase = true))
                    {
                        if (!Regex("""(?i)^\s*[a-zA-Z0-9_]+\s*=""").containsMatchIn(currentArg))
                        {
                            return ComponentCompletionInfo(cleanPrefix(currentArg))
                        }
                    }
                }
            }
            else if (isCfobjectFn)
            {
                val namedMatch = Regex("""(?i)^\s*component\s*=\s*(["']?)(.*)$""").find(currentArg)
                if (namedMatch != null)
                {
                    return ComponentCompletionInfo(cleanPrefix(namedMatch.groupValues[2]))
                }
            }

            return null
        }

        fun findScriptCfobjectCompletion(chars: CharSequence, offset: Int): ComponentCompletionInfo?
        {
            if (offset <= 0 || offset > chars.length) return null

            var i = offset - 1
            while (i >= 0)
            {
                val c = chars[i]
                if (c == ';' || c == '{' || c == '}')
                {
                    break
                }
                i--
            }
            val stmtStart = i + 1
            val stmtChunk = chars.subSequence(stmtStart, offset).toString().trimStart()

            if (!stmtChunk.matches(Regex("""(?i)^cfobject\b.*""")))
            {
                return null
            }

            val attrMatch = Regex("""(?i)\bcomponent\s*=\s*(["']?)([^"';)]*)$""").find(stmtChunk)
                ?: return null

            return ComponentCompletionInfo(cleanPrefix(attrMatch.groupValues[2]))
        }

        private fun cleanPrefix(raw: String): String
        {
            return raw.trim().trimStart('"', '\'').trim()
        }
    }
}
