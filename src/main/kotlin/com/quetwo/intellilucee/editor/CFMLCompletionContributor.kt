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

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet)
    {
        val file = parameters.originalFile
        if (!CFMLPsiUtil.isCFMLFile(file))
        {
            return
        }

        val chars = parameters.editor.document.charsSequence
        val posOffset = parameters.position.textRange.startOffset
        val varInfo = findPrecedingVariable(chars, posOffset)
            ?: findPrecedingVariable(chars, parameters.offset)
            ?: return

        val dotNotation = resolveComponentDotNotation(file, varInfo.varName, posOffset)
            ?: resolveComponentDotNotation(parameters.position.containingFile, varInfo.varName, posOffset)
            ?: return

        val cfcModel = resolveCfcModel(file, dotNotation) ?: return

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
    }
}
