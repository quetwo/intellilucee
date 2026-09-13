package com.quetwo.intellilucee.parser;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public final class CFMLTokenTypes
{

    public static final IElementType TEXT = new CFMLTokenType("CFML_TEXT");
    public static final IElementType IDENTIFIER = new CFMLTokenType("CFML_IDENTIFIER");
    public static final IElementType COMMENT = new CFMLTokenType("CFML_COMMENT");
    public static final IElementType STRING = new CFMLTokenType("CFML_STRING");
    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    private CFMLTokenTypes()
    {
    }

}