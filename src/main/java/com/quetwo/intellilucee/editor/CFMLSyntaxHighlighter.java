package com.quetwo.intellilucee.editor;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.quetwo.intellilucee.parser.CFMLLexer;
import com.quetwo.intellilucee.parser.CFMLTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class CFMLSyntaxHighlighter extends SyntaxHighlighterBase
{

    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("CFML_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey TAG =
        createTextAttributesKey("CFML_TAG", DefaultLanguageHighlighterColors.MARKUP_TAG);
    public static final TextAttributesKey TAG_NAME =
        createTextAttributesKey("CFML_TAG_NAME", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ATTRIBUTE_NAME =
        createTextAttributesKey("CFML_ATTRIBUTE_NAME", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE);
    public static final TextAttributesKey ATTRIBUTE_VALUE =
        createTextAttributesKey("CFML_ATTRIBUTE_VALUE", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey STRING =
        createTextAttributesKey("CFML_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
        createTextAttributesKey("CFML_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey LINE_COMMENT =
        createTextAttributesKey("CFML_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT =
        createTextAttributesKey("CFML_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey DOC_COMMENT =
        createTextAttributesKey("CFML_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public static final TextAttributesKey TAG_COMMENT =
        createTextAttributesKey("CFML_TAG_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public static final TextAttributesKey OPERATOR =
        createTextAttributesKey("CFML_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACKETS =
        createTextAttributesKey("CFML_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey BRACES =
        createTextAttributesKey("CFML_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey PARENTHESES =
        createTextAttributesKey("CFML_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey COMMA =
        createTextAttributesKey("CFML_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey SEMICOLON =
        createTextAttributesKey("CFML_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey DOT =
        createTextAttributesKey("CFML_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey TYPE =
        createTextAttributesKey("CFML_TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
    public static final TextAttributesKey SCOPE =
        createTextAttributesKey("CFML_SCOPE", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey HASH =
        createTextAttributesKey("CFML_HASH", DefaultLanguageHighlighterColors.MARKUP_ENTITY);
    public static final TextAttributesKey IDENTIFIER =
        createTextAttributesKey("CFML_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey BAD_CHARACTER =
        createTextAttributesKey("CFML_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<>();

    static
    {
        // Comments
        ATTRIBUTES.put(CFMLTokenTypes.COMMENT, LINE_COMMENT);
        ATTRIBUTES.put(CFMLTokenTypes.LINE_COMMENT, LINE_COMMENT);
        ATTRIBUTES.put(CFMLTokenTypes.BLOCK_COMMENT, BLOCK_COMMENT);
        ATTRIBUTES.put(CFMLTokenTypes.DOC_COMMENT, DOC_COMMENT);
        ATTRIBUTES.put(CFMLTokenTypes.TAG_COMMENT, TAG_COMMENT);

        // Strings & Interpolation
        ATTRIBUTES.put(CFMLTokenTypes.STRING, STRING);
        ATTRIBUTES.put(CFMLTokenTypes.SINGLE_QUOTED_STRING, STRING);
        ATTRIBUTES.put(CFMLTokenTypes.DOUBLE_QUOTED_STRING, STRING);
        ATTRIBUTES.put(CFMLTokenTypes.HASH, HASH);
        ATTRIBUTES.put(CFMLTokenTypes.ESCAPED_HASH, HASH);

        // Numbers
        ATTRIBUTES.put(CFMLTokenTypes.INTEGER_LITERAL, NUMBER);
        ATTRIBUTES.put(CFMLTokenTypes.FLOAT_LITERAL, NUMBER);
        ATTRIBUTES.put(CFMLTokenTypes.HEX_LITERAL, NUMBER);

        // Delimiters & Punctuation
        ATTRIBUTES.put(CFMLTokenTypes.LPAREN, PARENTHESES);
        ATTRIBUTES.put(CFMLTokenTypes.RPAREN, PARENTHESES);
        ATTRIBUTES.put(CFMLTokenTypes.LBRACE, BRACES);
        ATTRIBUTES.put(CFMLTokenTypes.RBRACE, BRACES);
        ATTRIBUTES.put(CFMLTokenTypes.LBRACKET, BRACKETS);
        ATTRIBUTES.put(CFMLTokenTypes.RBRACKET, BRACKETS);
        ATTRIBUTES.put(CFMLTokenTypes.COMMA, COMMA);
        ATTRIBUTES.put(CFMLTokenTypes.SEMICOLON, SEMICOLON);
        ATTRIBUTES.put(CFMLTokenTypes.DOT, DOT);

        // Tags
        ATTRIBUTES.put(CFMLTokenTypes.TAG_OPEN_START, TAG);
        ATTRIBUTES.put(CFMLTokenTypes.TAG_CLOSE_START, TAG);
        ATTRIBUTES.put(CFMLTokenTypes.TAG_END, TAG);
        ATTRIBUTES.put(CFMLTokenTypes.TAG_EMPTY_END, TAG);
        ATTRIBUTES.put(CFMLTokenTypes.TAG_NAME, TAG_NAME);
        ATTRIBUTES.put(CFMLTokenTypes.ATTRIBUTE_NAME, ATTRIBUTE_NAME);
        ATTRIBUTES.put(CFMLTokenTypes.ATTRIBUTE_VALUE, ATTRIBUTE_VALUE);

        // Identifiers & Bad character
        ATTRIBUTES.put(CFMLTokenTypes.IDENTIFIER, IDENTIFIER);
        ATTRIBUTES.put(CFMLTokenTypes.BAD_CHARACTER, BAD_CHARACTER);

        // Keywords
        for (IElementType type : CFMLTokenTypes.KEYWORDS.getTypes())
        {
            ATTRIBUTES.put(type, KEYWORD);
        }

        // Types
        for (IElementType type : CFMLTokenTypes.TYPES.getTypes())
        {
            ATTRIBUTES.put(type, TYPE);
        }

        // Scopes
        for (IElementType type : CFMLTokenTypes.SCOPES.getTypes())
        {
            ATTRIBUTES.put(type, SCOPE);
        }

        // Operators
        for (IElementType type : CFMLTokenTypes.OPERATORS.getTypes())
        {
            ATTRIBUTES.put(type, OPERATOR);
        }
    }

    @Override
    public @NotNull Lexer getHighlightingLexer()
    {
        return new CFMLLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType)
    {
        TextAttributesKey key = ATTRIBUTES.get(tokenType);
        return key != null ? new TextAttributesKey[]{key} : TextAttributesKey.EMPTY_ARRAY;
    }

}
