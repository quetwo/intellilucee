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
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.quetwo.intellilucee.lexer.CFMLSimpleLexer;
import com.quetwo.intellilucee.lexer.CFMLTokenTypes;
import org.jetbrains.annotations.NotNull;

public final class CFMLParserDefinition implements ParserDefinition
{
    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(CFMLTokenTypes.COMMENT);

    @Override
    public @NotNull Lexer createLexer(Project project)
    {
        return new CFMLSimpleLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project)
    {
        return new CFMLPsiParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType()
    {
        return CFMLFileElementType.INSTANCE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens()
    {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements()
    {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node)
    {
        return new LeafPsiElement(node.getElementType(), node.getText());
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider)
    {
        return new CFMLFile(viewProvider);
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens()
    {
        return WHITE_SPACES;
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right)
    {
        return SpaceRequirements.MAY;
    }
}