package com.quetwo.intellilucee.parser;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public final class CFMLTokenTypes
{

    public static final IElementType TEXT = new CFMLTokenType("CFML_TEXT");
    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    private CFMLTokenTypes()
    {
    }

}