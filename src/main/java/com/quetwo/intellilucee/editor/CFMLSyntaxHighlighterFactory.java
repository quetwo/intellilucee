package com.quetwo.intellilucee.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.quetwo.intellilucee.settings.CFMLGlobalSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CFMLSyntaxHighlighterFactory extends SyntaxHighlighterFactory
{

    @Override
    public @NotNull SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile)
    {
        CFMLGlobalSettings settings = ApplicationManager.getApplication() != null ? CFMLGlobalSettings.getInstance() : null;
        if (settings != null && settings.getState() != null && !settings.getState().getSyntaxAndErrorHighlighting())
        {
            return new PlainSyntaxHighlighter();
        }
        return new CFMLSyntaxHighlighter();
    }

}
