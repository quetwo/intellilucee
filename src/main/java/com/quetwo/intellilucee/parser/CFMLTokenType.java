package com.quetwo.intellilucee.parser;

import com.intellij.psi.tree.IElementType;
import com.quetwo.intellilucee.CFMLLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CFMLTokenType extends IElementType
{

    public CFMLTokenType(@NotNull @NonNls String debugName)
    {
        super(debugName, CFMLLanguage.INSTANCE);
    }

}