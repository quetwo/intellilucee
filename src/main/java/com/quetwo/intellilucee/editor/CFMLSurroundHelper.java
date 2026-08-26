package com.quetwo.intellilucee.editor;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.quetwo.intellilucee.editor.surround.SurroundCfoutput;
import com.quetwo.intellilucee.editor.surround.SurroundCfoutputHash;
import com.quetwo.intellilucee.editor.surround.SurroundVar;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class CFMLSurroundHelper implements SurroundDescriptor
{

    private static final Surrounder[] surrounders = new Surrounder[]
        {
            new SurroundVar(),
            new SurroundCfoutput(),
            new SurroundCfoutputHash()
        };

    @Override
    public PsiElement @NotNull [] getElementsToSurround(PsiFile file, int startOffset, int endOffset)
    {
        if (!isCfmlFile(file))
        {
            return PsiElement.EMPTY_ARRAY;
        }

        if (endOffset <= startOffset)
        {
            return PsiElement.EMPTY_ARRAY;
        }

        return new PsiElement[] { file };
    }

    private static boolean isCfmlFile(@NotNull PsiFile file)
    {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".cfm") || name.endsWith(".cfml");
    }

    @Override
    public Surrounder @NotNull [] getSurrounders()
    {
        return surrounders;
    }

    @Override
    public boolean isExclusive()
    {
        return false;
    }

}