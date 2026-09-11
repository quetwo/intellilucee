package com.quetwo.intellilucee.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.quetwo.intellilucee.CFMLLanguage;
import com.quetwo.intellilucee.file.CFMLFileType;
import com.quetwo.intellilucee.file.CFMLFileUtil;
import org.jetbrains.annotations.NotNull;

public class CFMLTypedHandler extends TypedHandlerDelegate
{

    @Override
    public @NotNull Result beforeSelectionRemoved(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file)
    {
        if (c != '#')
        {
            return Result.CONTINUE;
        }

        if (!CodeInsightSettings.getInstance().SURROUND_SELECTION_ON_QUOTE_TYPED)
        {
            return Result.CONTINUE;
        }

        if (!isCfmlFile(file))
        {
            return Result.CONTINUE;
        }

        Caret caret = editor.getCaretModel().getCurrentCaret();
        if (caret == null)
        {
            caret = editor.getCaretModel().getPrimaryCaret();
        }

        if (caret == null || !caret.hasSelection())
        {
            return Result.CONTINUE;
        }

        int start = caret.getSelectionStart();
        int end = caret.getSelectionEnd();
        if (end <= start)
        {
            return Result.CONTINUE;
        }

        Document document = editor.getDocument();
        document.insertString(end, "#");
        document.insertString(start, "#");
        caret.setSelection(start + 1, end + 1);

        return Result.STOP;
    }

    private static boolean isCfmlFile(@NotNull PsiFile file)
    {
        if (file.getLanguage().isKindOf(CFMLLanguage.INSTANCE))
        {
            return true;
        }
        if (file.getFileType() instanceof CFMLFileType)
        {
            return true;
        }
        VirtualFile vFile = file.getVirtualFile();
        if (vFile != null && CFMLFileUtil.isCFMLFIle(vFile))
        {
            return true;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0)
        {
            return CFMLFileUtil.isCFMLxtension(name.substring(dot + 1));
        }
        return false;
    }

}
