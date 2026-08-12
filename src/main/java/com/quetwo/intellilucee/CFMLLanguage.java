package com.quetwo.intellilucee;

import com.intellij.lang.Language;

public class CFMLLanguage extends Language
{
    public static final CFMLLanguage INSTANCE = new CFMLLanguage();

    private  CFMLLanguage()
    {
        super("CFML");
    }
}
