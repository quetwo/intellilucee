package com.quetwo.intellilucee.file;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.quetwo.intellilucee.CFMLLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class CFMLFileType extends LanguageFileType
{
    public final static CFMLFileType INSTANCE = new CFMLFileType();

    private CFMLFileType()
    {
        super(CFMLLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName()
    {
        return "CFML";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription()
    {
        return "CFML source file";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension()
    {
        return "cfml";
    }

    @Override
    public @Nullable Icon getIcon()
    {
        //TODO:Implement ICON
        return null;
    }
}
