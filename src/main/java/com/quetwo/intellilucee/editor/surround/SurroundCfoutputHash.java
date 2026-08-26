package com.quetwo.intellilucee.editor.surround;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SurroundCfoutputHash implements Surrounder
{

    private static final String PREFIX = "<cfoutput>#";
    private static final String SUFFIX = "#</cfoutput>";

    @Override
    public @NlsActions.ActionText String getTemplateDescription()
    {
        return "<cfoutput>#variable#</cfoutput>";
    }

    @Override
    public boolean isApplicable(PsiElement @NotNull [] elements)
    {
        return elements.length > 0;
    }

    @Override
    public @Nullable TextRange surroundElements(@NotNull Project project, @NotNull Editor editor, PsiElement @NotNull [] elements)
    {
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        int selectionEnd = editor.getSelectionModel().getSelectionEnd();

        if (selectionEnd <= selectionStart)
        {
            return null;
        }

        Document document = editor.getDocument();
        document.insertString(selectionEnd, SUFFIX);
        document.insertString(selectionStart, PREFIX);

        return new TextRange(selectionStart + PREFIX.length(), selectionEnd + PREFIX.length());
    }

}