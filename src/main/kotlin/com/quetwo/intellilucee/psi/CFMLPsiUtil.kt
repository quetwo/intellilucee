package com.quetwo.intellilucee.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.quetwo.intellilucee.CFMLLanguage
import com.quetwo.intellilucee.model.CFMLDocumentModel
import com.quetwo.intellilucee.model.CFMLFunctionDeclaration
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.model.CFMLVariableDeclaration

object CFMLPsiUtil {

    fun isCFMLFile(file: PsiFile?): Boolean {
        if (file == null) return false
        if (file.language == CFMLLanguage.INSTANCE) return true
        val ext = file.virtualFile?.extension?.lowercase() ?: file.name.substringAfterLast('.', "").lowercase()
        return ext in setOf("cfm", "cfc", "cfs", "cfml")
    }

    fun getModel(file: PsiFile): CFMLDocumentModel {
        return CachedValuesManager.getCachedValue(file) {
            val model = CFMLModelParser.parse(file.text)
            CachedValueProvider.Result.create(model, file)
        }
    }

    fun getFunctionElement(file: PsiFile, decl: CFMLFunctionDeclaration): CFMLFunctionElement {
        return CFMLFunctionElement(file, decl)
    }

    fun getVariableElement(file: PsiFile, decl: CFMLVariableDeclaration): CFMLVariableElement {
        return CFMLVariableElement(file, decl)
    }

    fun findDeclarationElementAt(file: PsiFile, offset: Int): PsiElement? {
        val model = getModel(file)
        val symbol = model.findSymbolAt(offset) ?: return null
        return when (symbol) {
            is CFMLFunctionDeclaration -> getFunctionElement(file, symbol)
            is CFMLVariableDeclaration -> getVariableElement(file, symbol)
            else -> null
        }
    }

    fun resolveSymbolAt(file: PsiFile, offset: Int): PsiElement? {
        val model = getModel(file)
        val symbol = model.findSymbolAt(offset) ?: return null
        return when (symbol) {
            is com.quetwo.intellilucee.model.CFMLFunctionCall -> {
                val decl = model.findFunctionDeclaration(symbol.name)
                decl?.let { getFunctionElement(file, it) }
            }
            is com.quetwo.intellilucee.model.CFMLVariableUsage -> {
                val decl = model.findVariableDeclaration(symbol.fullName ?: symbol.name, offset)
                decl?.let { getVariableElement(file, it) }
            }
            is CFMLFunctionDeclaration -> getFunctionElement(file, symbol)
            is CFMLVariableDeclaration -> getVariableElement(file, symbol)
        }
    }
}
