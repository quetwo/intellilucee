package com.quetwo.intellilucee.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.quetwo.intellilucee.psi.CFMLPsiFile;
import org.jetbrains.annotations.NotNull;

public class CFMLParserDefinition implements ParserDefinition
{

    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);

    @Override
    public @NotNull Lexer createLexer(Project project)
    {
        return new CFMLLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project)
    {
        return new CFMLParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType()
    {
        return CFMLElementTypes.FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens()
    {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements()
    {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node)
    {
        return new com.intellij.extapi.psi.ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider)
    {
        return new CFMLPsiFile(viewProvider);
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens()
    {
        return WHITE_SPACES;
    }

}