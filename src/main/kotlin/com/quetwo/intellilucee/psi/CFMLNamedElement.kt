package com.quetwo.intellilucee.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.IncorrectOperationException
import com.quetwo.intellilucee.CFMLIcon
import com.quetwo.intellilucee.model.CFMLFunctionDeclaration
import com.quetwo.intellilucee.model.CFMLSymbol
import com.quetwo.intellilucee.model.CFMLVariableDeclaration
import javax.swing.Icon

abstract class CFMLNamedElement(
    private val containingPsiFile: PsiFile,
    open val symbol: CFMLSymbol,
    private val nameRange: TextRange
) : FakePsiElement(), PsiNameIdentifierOwner {

    override fun getParent(): PsiElement = containingPsiFile

    override fun getContainingFile(): PsiFile = containingPsiFile

    override fun isValid(): Boolean = containingPsiFile.isValid

    override fun getTextRange(): TextRange = symbol.range

    override fun getTextOffset(): Int = nameRange.startOffset

    override fun getName(): String = symbol.name

    override fun getText(): String {
        val fileText = containingPsiFile.text
        return if (symbol.range.endOffset <= fileText.length && symbol.range.startOffset >= 0) {
            fileText.substring(symbol.range.startOffset, symbol.range.endOffset)
        } else {
            symbol.name
        }
    }

    override fun getNameIdentifier(): PsiElement = this

    override fun setName(name: String): PsiElement {
        throw IncorrectOperationException("Rename is not supported")
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true

    override fun navigate(requestFocus: Boolean) {
        val file = containingPsiFile.virtualFile ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(
            project,
            file,
            nameRange.startOffset
        ).navigate(requestFocus)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CFMLNamedElement) return false
        return containingPsiFile == other.containingPsiFile &&
                symbol.name.equals(other.symbol.name, ignoreCase = true) &&
                symbol.range == other.symbol.range
    }

    override fun hashCode(): Int {
        return containingPsiFile.hashCode() * 31 + symbol.name.lowercase().hashCode() * 31 + symbol.range.hashCode()
    }
}

class CFMLFunctionElement(
    file: PsiFile,
    val functionDecl: CFMLFunctionDeclaration
) : CFMLNamedElement(file, functionDecl, functionDecl.nameRange) {
    override fun getIcon(flags: Int): Icon = CFMLIcon.FILE_CFC

    override fun toString(): String = "CFMLFunctionElement(${functionDecl.name})"
}

class CFMLVariableElement(
    file: PsiFile,
    val variableDecl: CFMLVariableDeclaration
) : CFMLNamedElement(file, variableDecl, variableDecl.nameRange) {
    override fun getIcon(flags: Int): Icon = CFMLIcon.FILE_CFM

    override fun toString(): String = "CFMLVariableElement(${variableDecl.name})"
}
