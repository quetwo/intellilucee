package com.quetwo.intellilucee.parser;

import com.intellij.psi.tree.IElementType;
import com.quetwo.intellilucee.CFMLLanguage;

public final class CFMLTokenTypes
{
    public static final IElementType TEXT = new IElementType("TEXT", CFMLLanguage.INSTANCE);

    private CFMLTokenTypes()
    {
    }
}