package com.quetwo.intellilucee.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.quetwo.intellilucee.CFMLLanguage;
import com.quetwo.intellilucee.file.CFMLFileType;
import org.jetbrains.annotations.NotNull;

public class CFMLPsiFile extends PsiFileBase
{

    public CFMLPsiFile(@NotNull com.intellij.psi.FileViewProvider viewProvider)
    {
        super(viewProvider, CFMLLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType()
    {
        return CFMLFileType.INSTANCE;
    }

}