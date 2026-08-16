package com.quetwo.intellilucee.parser;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.tree.IFileElementType;
import com.quetwo.intellilucee.CFMLLanguage;
import com.quetwo.intellilucee.file.CFMLFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class CFMLFile extends PsiFileBase
{

    public CFMLFile (@NotNull FileViewProvider viewProvider)
    {
        super( viewProvider, CFMLLanguage.INSTANCE);
    }

    @Override
    public @NotNull IFileElementType getFileElementType()
    {
        return CFMLFileElementType.INSTANCE;
    }

    @Override
    public @NotNull FileType getFileType() {
        return CFMLFileType.INSTANCE;
    }

    @Override
    public @NotNull String toString()
    {
        return "CFML File";
    }

    @Override
    public Icon getIcon(int flags)
    {
        return CFMLFileType.INSTANCE.getIcon();
    }
}
