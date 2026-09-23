package com.quetwo.intellilucee.model

import com.intellij.openapi.util.TextRange

enum class CFMLAccessType
{
    PUBLIC,
    PRIVATE,
    PACKAGE,
    REMOTE
}

sealed interface CFMLSymbol
{
    val name: String
    val range: TextRange
}

data class CFMLFunctionDeclaration(
    override val name: String,
    val nameRange: TextRange,
    override val range: TextRange,
    val bodyRange: TextRange?,
    val parameters: List<CFMLVariableDeclaration> = emptyList(),
    val access: CFMLAccessType = CFMLAccessType.PUBLIC
) : CFMLSymbol

data class CFMLFunctionCall(
    override val name: String,
    override val range: TextRange,
    val enclosingFunction: CFMLFunctionDeclaration?
) : CFMLSymbol

data class CFMLVariableDeclaration(
    override val name: String,
    val nameRange: TextRange,
    override val range: TextRange,
    val isLocal: Boolean,
    val enclosingFunction: CFMLFunctionDeclaration?
) : CFMLSymbol

data class CFMLVariableUsage(
    override val name: String,
    val nameRange: TextRange,
    override val range: TextRange,
    val fullName: String? = null,
    val enclosingFunction: CFMLFunctionDeclaration?
) : CFMLSymbol
{
    constructor(name: String, range: TextRange, enclosingFunction: CFMLFunctionDeclaration?) :
            this(name, range, range, name, enclosingFunction)
}

class CFMLDocumentModel(
    val functions: List<CFMLFunctionDeclaration>,
    val functionCalls: List<CFMLFunctionCall>,
    val variableDeclarations: List<CFMLVariableDeclaration>,
    val variableUsages: List<CFMLVariableUsage>)
{
    private val functionUsageCounts: Map<String, Int> = buildMap {
        for (call in functionCalls)
        {
            val key = call.name.lowercase()
            put(key, (get(key) ?: 0) + 1)
        }
    }

    private val functionDeclByName: Map<String, CFMLFunctionDeclaration> = buildMap {
        for (func in functions)
        {
            val key = func.name.lowercase()
            if (!containsKey(key))
            {
                put(key, func)
            }
        }
    }

    private val functionCallsByName: Map<String, List<CFMLFunctionCall>> = functionCalls.groupBy { it.name.lowercase() }

    fun findFunctionDeclaration(name: String): CFMLFunctionDeclaration?
    {
        return functionDeclByName[name.lowercase()]
    }

    fun findFunctionCalls(name: String): List<CFMLFunctionCall>
    {
        return functionCallsByName[name.lowercase()] ?: emptyList()
    }

    fun getFunctionUsageCount(functionDecl: CFMLFunctionDeclaration): Int
    {
        return functionUsageCounts[functionDecl.name.lowercase()] ?: 0
    }

    fun findEnclosingFunctionHierarchy(offset: Int): List<CFMLFunctionDeclaration>
    {
        return functions
            .filter { func ->
                func.bodyRange?.containsOffset(offset) == true || func.range.containsOffset(offset)
            }
            .sortedBy { it.range.length }
    }

    fun findEnclosingFunction(offset: Int): CFMLFunctionDeclaration?
    {
        return findEnclosingFunctionHierarchy(offset).firstOrNull()
    }

    fun findVariableDeclaration(name: String, offset: Int): CFMLVariableDeclaration?
    {
        val bareName = cleanVariableName(name)
        val lowerName = name.lowercase()
        val isExplicitArguments = lowerName.startsWith("arguments.")
        val isExplicitLocal = lowerName.startsWith("local.")
        val isExplicitVariables = lowerName.startsWith("variables.")

        val enclosingFunctions = findEnclosingFunctionHierarchy(offset)

        if (enclosingFunctions.isNotEmpty())
        {
            for (enclosingFunc in enclosingFunctions)
            {
                if (isExplicitArguments)
                {
                    val paramDecl = variableDeclarations.firstOrNull {
                        it.enclosingFunction == enclosingFunc &&
                                it.isLocal &&
                                (enclosingFunc.parameters.any { p -> p.name.equals(bareName, ignoreCase = true) } ||
                                 cleanVariableName(it.name).equals(bareName, ignoreCase = true))
                    }
                    if (paramDecl != null) return paramDecl
                }
                else if (isExplicitLocal)
                {
                    val localDecl = variableDeclarations.firstOrNull {
                        it.enclosingFunction == enclosingFunc &&
                                it.isLocal &&
                                cleanVariableName(it.name).equals(bareName, ignoreCase = true)
                    }
                    if (localDecl != null) return localDecl
                }
                else if (isExplicitVariables)
                {
                    val fileDecl = variableDeclarations.firstOrNull {
                        it.enclosingFunction == null && cleanVariableName(it.name).equals(bareName, ignoreCase = true)
                    }
                    if (fileDecl != null) return fileDecl
                }
                else
                {
                    val localDecl = variableDeclarations.firstOrNull {
                        it.enclosingFunction == enclosingFunc &&
                                cleanVariableName(it.name).equals(bareName, ignoreCase = true)
                    }
                    if (localDecl != null) return localDecl
                }
            }
        }

        if (!isExplicitLocal && !isExplicitArguments)
        {
            return variableDeclarations.firstOrNull {
                it.enclosingFunction == null && cleanVariableName(it.name).equals(bareName, ignoreCase = true)
            }
        }

        return null
    }

    fun findVariableUsages(decl: CFMLVariableDeclaration): List<CFMLVariableUsage>
    {
        val bareName = cleanVariableName(decl.name)
        return if (decl.enclosingFunction != null && decl.isLocal)
        {
            variableUsages.filter {
                it.enclosingFunction == decl.enclosingFunction && cleanVariableName(it.name).equals(
                    bareName,
                    ignoreCase = true
                )
            }
        }
        else
        {
            variableUsages.filter {
                cleanVariableName(it.name).equals(bareName, ignoreCase = true)
            }
        }
    }

    fun findSymbolAt(offset: Int): CFMLSymbol?
    {
        // Look in function declarations
        for (func in functions)
        {
            if (func.nameRange.containsOffset(offset)) return func
        }
        // Look in variable declarations
        for (v in variableDeclarations)
        {
            if (v.nameRange.containsOffset(offset) || v.range.containsOffset(offset)) return v
        }
        // Look in function calls
        for (call in functionCalls)
        {
            if (call.range.containsOffset(offset)) return call
        }
        // Look in variable usages
        for (v in variableUsages)
        {
            if (v.nameRange.containsOffset(offset) || v.range.containsOffset(offset)) return v
        }
        return null
    }

    companion object
    {
        fun cleanVariableName(rawName: String): String
        {
            val lower = rawName.lowercase()
            return when
            {
                lower.startsWith("local.") -> rawName.substring(6)
                lower.startsWith("variables.") -> rawName.substring(10)
                lower.startsWith("arguments.") -> rawName.substring(10)
                lower.startsWith("session.") -> rawName.substring(8)
                lower.startsWith("application.") -> rawName.substring(12)
                lower.startsWith("request.") -> rawName.substring(8)
                lower.startsWith("this.") -> rawName.substring(5)
                else -> rawName
            }
        }
    }
}
