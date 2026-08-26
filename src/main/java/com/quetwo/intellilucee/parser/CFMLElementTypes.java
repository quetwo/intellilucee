package com.quetwo.intellilucee.parser;

import com.intellij.psi.tree.IFileElementType;

public final class CFMLElementTypes
{

    public static final IFileElementType FILE = new IFileElementType("CFML_FILE", com.quetwo.intellilucee.CFMLLanguage.INSTANCE);

    private CFMLElementTypes()
    {
    }

}