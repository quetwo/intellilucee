package com.quetwo.intellilucee.model

import com.intellij.openapi.util.TextRange

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
    val parameters: List<CFMLVariableDeclaration> = emptyList()
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
    override val range: TextRange,
    val enclosingFunction: CFMLFunctionDeclaration?
) : CFMLSymbol

class CFMLDocumentModel(
    val functions: List<CFMLFunctionDeclaration>,
    val functionCalls: List<CFMLFunctionCall>,
    val variableDeclarations: List<CFMLVariableDeclaration>,
    val variableUsages: List<CFMLVariableUsage>)
{
    fun findFunctionDeclaration(name: String): CFMLFunctionDeclaration?
    {
        return functions.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun findFunctionCalls(name: String): List<CFMLFunctionCall>
    {
        return functionCalls.filter { it.name.equals(name, ignoreCase = true) }
    }

    fun getFunctionUsageCount(functionDecl: CFMLFunctionDeclaration): Int
    {
        return functionCalls.count { it.name.equals(functionDecl.name, ignoreCase = true) }
    }

    fun findVariableDeclaration(name: String, offset: Int): CFMLVariableDeclaration?
    {
        val bareName = cleanVariableName(name)
        val enclosingFunc = findEnclosingFunction(offset)
        if (enclosingFunc != null)
        {
            // First search inside local variables / parameters of this function
            val localDecl = variableDeclarations.firstOrNull {
                it.enclosingFunction == enclosingFunc && cleanVariableName(it.name).equals(bareName, ignoreCase = true)
            }
            if (localDecl != null)
            {
                return localDecl
            }
        }
        // Then search in file-level variable declarations
        return variableDeclarations.firstOrNull {
            it.enclosingFunction == null && cleanVariableName(it.name).equals(bareName, ignoreCase = true)
        } ?: variableDeclarations.firstOrNull {
            cleanVariableName(it.name).equals(bareName, ignoreCase = true)
        }
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

    fun findEnclosingFunction(offset: Int): CFMLFunctionDeclaration?
    {
        return functions.firstOrNull { func ->
            func.bodyRange?.containsOffset(offset) == true || func.range.containsOffset(offset)
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
            if (v.nameRange.containsOffset(offset)) return v
        }
        // Look in function calls
        for (call in functionCalls)
        {
            if (call.range.containsOffset(offset)) return call
        }
        // Look in variable usages
        for (v in variableUsages)
        {
            if (v.range.containsOffset(offset)) return v
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
