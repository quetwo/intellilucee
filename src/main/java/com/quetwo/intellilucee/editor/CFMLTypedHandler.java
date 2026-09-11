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
import com.quetwo.intellilucee.settings.CFMLGlobalSettings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CFMLTypedHandler extends TypedHandlerDelegate
{
    private static final Set<String> EXCLUDED_AUTO_CLOSE_TAGS = new HashSet<>(Arrays.asList(
        "cfset",
        "cfdump",
        "cfabort",
        "cflog",
        "cfargument",
        "cfbreak",
        "cfcontent",
        "cferror",
        "cfexecute",
        "cffile"
    ));

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

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file)
    {
        if (c != '>')
        {
            return Result.CONTINUE;
        }

        if (!isCfmlFile(file))
        {
            return Result.CONTINUE;
        }

        CFMLGlobalSettings settings = CFMLGlobalSettings.getInstance();
        if (settings == null || settings.getState() == null || !settings.getState().getAutoCloseTags())
        {
            return Result.CONTINUE;
        }

        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        CharSequence chars = document.getCharsSequence();

        if (offset <= 0 || offset > chars.length() || chars.charAt(offset - 1) != '>')
        {
            return Result.CONTINUE;
        }

        if (offset >= 2 && chars.charAt(offset - 2) == '/')
        {
            return Result.CONTINUE;
        }

        String tagName = findOpeningCfmlTagName(chars, offset - 1);
        if (tagName != null && !tagName.isEmpty())
        {
            if (EXCLUDED_AUTO_CLOSE_TAGS.contains(tagName.toLowerCase(Locale.ROOT)))
            {
                return Result.CONTINUE;
            }

            String closingTag = "</" + tagName + ">";
            if (offset + closingTag.length() > chars.length() ||
                !chars.subSequence(offset, offset + closingTag.length()).toString().equalsIgnoreCase(closingTag))
            {
                document.insertString(offset, closingTag);
            }
        }

        return Result.CONTINUE;
    }

    private static @Nullable String findOpeningCfmlTagName(@NotNull CharSequence chars, int gtPos)
    {
        int minPos = Math.max(0, gtPos - 5000);
        for (int i = gtPos - 1; i >= minPos; i--)
        {
            if (chars.charAt(i) == '<')
            {
                if (i + 1 >= gtPos)
                {
                    continue;
                }
                char nextChar = chars.charAt(i + 1);
                if (nextChar == '/' || nextChar == '!' || nextChar == '?')
                {
                    continue;
                }

                int tagStart = i + 1;
                int tagEnd = tagStart;
                while (tagEnd < gtPos)
                {
                    char ch = chars.charAt(tagEnd);
                    if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == ':')
                    {
                        tagEnd++;
                    }
                    else
                    {
                        break;
                    }
                }

                if (tagEnd == tagStart)
                {
                    continue;
                }

                String name = chars.subSequence(tagStart, tagEnd).toString();
                if (!name.toLowerCase().startsWith("cf"))
                {
                    continue;
                }

                boolean inSingleQuote = false;
                boolean inDoubleQuote = false;
                boolean valid = true;

                for (int j = tagEnd; j < gtPos; j++)
                {
                    char ch = chars.charAt(j);
                    if (inSingleQuote)
                    {
                        if (ch == '\'')
                        {
                            inSingleQuote = false;
                        }
                    }
                    else if (inDoubleQuote)
                    {
                        if (ch == '"')
                        {
                            inDoubleQuote = false;
                        }
                    }
                    else
                    {
                        if (ch == '\'')
                        {
                            inSingleQuote = true;
                        }
                        else if (ch == '"')
                        {
                            inDoubleQuote = true;
                        }
                        else if (ch == '<' || ch == '>')
                        {
                            valid = false;
                            break;
                        }
                    }
                }

                if (valid && !inSingleQuote && !inDoubleQuote)
                {
                    return name;
                }
            }
        }
        return null;
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
        if (CFMLFileUtil.isCFMLFIle(vFile))
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
