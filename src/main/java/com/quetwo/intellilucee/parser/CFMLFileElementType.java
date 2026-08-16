package com.quetwo.intellilucee.parser;

import com.intellij.psi.tree.IFileElementType;
import com.quetwo.intellilucee.CFMLLanguage;

public final class CFMLFileElementType extends IFileElementType
{
    public static final CFMLFileElementType INSTANCE = new CFMLFileElementType();

    private CFMLFileElementType()
    {
        super (CFMLLanguage.INSTANCE);
    }
}
