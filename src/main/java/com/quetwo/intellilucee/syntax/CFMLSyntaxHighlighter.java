package com.quetwo.intellilucee.syntax;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.quetwo.intellilucee.lexer.CFMLSimpleLexer;
import com.quetwo.intellilucee.lexer.CFMLTokenTypes;
import org.jetbrains.annotations.NotNull;

public final class CFMLSyntaxHighlighter extends SyntaxHighlighterBase
{
    private static final TextAttributesKey TAG =
        TextAttributesKey.createTextAttributesKey("CFML_TAG", DefaultLanguageHighlighterColors.MARKUP_TAG);

    private static final TextAttributesKey COMMENT =
        TextAttributesKey.createTextAttributesKey("CFML_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);

    private static final TextAttributesKey BAD_CHARACTER =
        TextAttributesKey.createTextAttributesKey("CFML_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] TAG_KEYS = new TextAttributesKey[]{TAG};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @Override
    public @NotNull Lexer getHighlightingLexer()
    {
        return new CFMLSimpleLexer();
    }

    @Override
    public @NotNull TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType)
    {
        if (tokenType == CFMLTokenTypes.TAG)
        {
            return TAG_KEYS;
        }

        if (tokenType == CFMLTokenTypes.COMMENT)
        {
            return COMMENT_KEYS;
        }

        if (tokenType == TokenType.BAD_CHARACTER)
        {
            return BAD_CHAR_KEYS;
        }

        return EMPTY_KEYS;
    }
}