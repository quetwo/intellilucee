package com.quetwo.intellilucee.lexer;

import com.intellij.psi.tree.IElementType;
import com.quetwo.intellilucee.CFMLLanguage;

public class CFMLTokenType extends IElementType
{
    public CFMLTokenType(String debugName)
    {
        super (debugName, CFMLLanguage.INSTANCE);
    }
}
