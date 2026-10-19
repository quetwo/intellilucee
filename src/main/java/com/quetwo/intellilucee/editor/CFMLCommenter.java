package com.quetwo.intellilucee.editor;

import com.intellij.codeInsight.generation.CommenterDataHolder;
import com.intellij.codeInsight.generation.SelfManagingCommenter;
import com.intellij.lang.Commenter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CFMLCommenter implements Commenter, SelfManagingCommenter<CFMLCommenter.CFMLCommenterData>
{

    private static final Pattern CFSCRIPT_TAG_PATTERN = Pattern.compile(
            "(?i)<cfscript\\b[^>]*>([\\s\\S]*?)(?:</cfscript>|$)"
    );

    private static final Pattern SCRIPT_COMPONENT_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:component|interface)\\b"
    );

    public static class CFMLCommenterData extends CommenterDataHolder
    {
        final boolean isScriptFile;
        final List<TextRange> scriptRanges;

        public CFMLCommenterData(boolean isScriptFile, List<TextRange> scriptRanges)
        {
            this.isScriptFile = isScriptFile;
            this.scriptRanges = scriptRanges;
        }

        public boolean isScriptAt(int offset)
        {
            if (isScriptFile)
            {
                return true;
            }
            for (TextRange range : scriptRanges)
            {
                if (offset >= range.getStartOffset() && offset <= range.getEndOffset())
                {
                    return true;
                }
            }
            return false;
        }

        public boolean isScriptRange(int startOffset, int endOffset)
        {
            if (isScriptFile)
            {
                return true;
            }
            if (startOffset > endOffset)
            {
                int tmp = startOffset;
                startOffset = endOffset;
                endOffset = tmp;
            }
            for (TextRange range : scriptRanges)
            {
                if (startOffset >= range.getStartOffset() && endOffset <= range.getEndOffset())
                {
                    return true;
                }
            }
            return false;
        }
    }

    // Standard Commenter interface methods

    @Override
    public @Nullable String getLineCommentPrefix()
    {
        return "//";
    }

    @Override
    public @Nullable String getBlockCommentPrefix()
    {
        return "<!---";
    }

    @Override
    public @Nullable String getBlockCommentSuffix()
    {
        return "--->";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix()
    {
        return "<!---";
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix()
    {
        return "--->";
    }

    // SelfManagingCommenter interface methods

    @Override
    public @Nullable CFMLCommenterData createLineCommentingState(int startLine, int endLine, @NotNull Document document, @NotNull PsiFile file)
    {
        return createDataHolder(document, file);
    }

    @Override
    public @Nullable CFMLCommenterData createBlockCommentingState(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull PsiFile file)
    {
        return createDataHolder(document, file);
    }

    private CFMLCommenterData createDataHolder(@NotNull Document document, @NotNull PsiFile file)
    {
        String fileName = file.getName().toLowerCase();
        String text = document.getText();

        if (fileName.endsWith(".cfs"))
        {
            return new CFMLCommenterData(true, List.of());
        }

        if (fileName.endsWith(".cfc") && SCRIPT_COMPONENT_PATTERN.matcher(text).find())
        {
            return new CFMLCommenterData(true, List.of());
        }

        // Find <cfscript> body ranges
        List<TextRange> ranges = new ArrayList<>();
        Matcher matcher = CFSCRIPT_TAG_PATTERN.matcher(text);
        while (matcher.find())
        {
            int start = matcher.start(1);
            int end = matcher.end(1);
            ranges.add(new TextRange(start, end));
        }

        return new CFMLCommenterData(false, ranges);
    }

    @Override
    public boolean isLineCommented(int line, int offset, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        String lineContent = document.getText(new TextRange(lineStart, lineEnd)).trim();

        if (lineContent.isEmpty())
        {
            return false;
        }

        boolean isScript = data.isScriptAt(lineStart);
        if (isScript)
        {
            return lineContent.startsWith("//") ||
                   (lineContent.startsWith("/*") && lineContent.endsWith("*/")) ||
                   (lineContent.startsWith("<!---") && lineContent.endsWith("--->"));
        }
        else
        {
            return lineContent.startsWith("<!---") && lineContent.endsWith("--->");
        }
    }

    @Override
    public @Nullable String getCommentPrefix(int line, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        int lineStart = document.getLineStartOffset(line);
        boolean isScript = data.isScriptAt(lineStart);
        return isScript ? "//" : "<!---";
    }

    @Override
    public void commentLine(int line, int offset, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        String text = document.getText();

        // Calculate leading whitespace
        int contentStart = lineStart;
        while (contentStart < lineEnd && Character.isWhitespace(text.charAt(contentStart)))
        {
            contentStart++;
        }

        boolean isScript = data.isScriptAt(contentStart < lineEnd ? contentStart : lineStart);
        if (isScript)
        {
            document.insertString(contentStart, "//");
        }
        else
        {
            // Outside <cfscript>: wrap line in <!--- --->
            document.insertString(lineEnd, " --->");
            document.insertString(contentStart, "<!--- ");
        }
    }

    @Override
    public void uncommentLine(int line, int offset, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);
        String text = document.getText();

        int contentStart = lineStart;
        while (contentStart < lineEnd && Character.isWhitespace(text.charAt(contentStart)))
        {
            contentStart++;
        }

        int contentEnd = lineEnd;
        while (contentEnd > contentStart && Character.isWhitespace(text.charAt(contentEnd - 1)))
        {
            contentEnd--;
        }

        String content = text.substring(contentStart, contentEnd);

        if (content.startsWith("//"))
        {
            int removeEnd = contentStart + 2;
            if (removeEnd < lineEnd && text.charAt(removeEnd) == ' ')
            {
                removeEnd++;
            }
            document.deleteString(contentStart, removeEnd);
        }
        else if (content.startsWith("<!---") && content.endsWith("--->"))
        {
            int suffixStart = contentEnd - 4;
            int prefixEnd = contentStart + 5;
            if (prefixEnd < suffixStart && text.charAt(prefixEnd) == ' ')
            {
                prefixEnd++;
            }
            if (suffixStart > prefixEnd && text.charAt(suffixStart - 1) == ' ')
            {
                suffixStart--;
            }
            document.deleteString(suffixStart, contentEnd);
            document.deleteString(contentStart, prefixEnd);
        }
        else if (content.startsWith("/*") && content.endsWith("*/"))
        {
            int suffixStart = contentEnd - 2;
            int prefixEnd = contentStart + 2;
            if (prefixEnd < suffixStart && text.charAt(prefixEnd) == ' ')
            {
                prefixEnd++;
            }
            if (suffixStart > prefixEnd && text.charAt(suffixStart - 1) == ' ')
            {
                suffixStart--;
            }
            document.deleteString(suffixStart, contentEnd);
            document.deleteString(contentStart, prefixEnd);
        }
    }

    @Override
    public @Nullable TextRange getBlockCommentRange(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        String text = document.getText();
        boolean isScript = data.isScriptRange(selectionStart, selectionEnd);

        if (isScript)
        {
            TextRange scriptRange = findEnclosingCommentRange(text, selectionStart, selectionEnd, "/*", "*/");
            if (scriptRange != null)
            {
                return scriptRange;
            }
        }
        return findEnclosingCommentRange(text, selectionStart, selectionEnd, "<!---", "--->");
    }

    private TextRange findEnclosingCommentRange(String text, int selStart, int selEnd, String prefix, String suffix)
    {
        int searchStart = Math.min(selStart, text.length());
        int prefixIndex = text.lastIndexOf(prefix, searchStart);
        while (prefixIndex >= 0)
        {
            int suffixIndex = text.indexOf(suffix, prefixIndex + prefix.length());
            if (suffixIndex >= 0 && suffixIndex + suffix.length() >= selEnd)
            {
                return new TextRange(prefixIndex, suffixIndex + suffix.length());
            }
            if (prefixIndex == 0) break;
            prefixIndex = text.lastIndexOf(prefix, prefixIndex - 1);
        }
        return null;
    }

    @Override
    public @Nullable String getBlockCommentPrefix(int selectionStart, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        return data.isScriptAt(selectionStart) ? "/*" : "<!---";
    }

    @Override
    public @Nullable String getBlockCommentSuffix(int selectionEnd, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        return data.isScriptAt(selectionEnd) ? "*/" : "--->";
    }

    @Override
    public void uncommentBlockComment(int startOffset, int endOffset, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        String text = document.getText();
        if (startOffset < 0 || endOffset > text.length() || startOffset >= endOffset)
        {
            return;
        }

        String commentText = text.substring(startOffset, endOffset);
        if (commentText.startsWith("<!---") && commentText.endsWith("--->"))
        {
            int prefixLen = 5;
            int suffixLen = 4;
            int contentStart = startOffset + prefixLen;
            int contentEnd = endOffset - suffixLen;

            if (contentStart < contentEnd && text.charAt(contentStart) == ' ')
            {
                contentStart++;
            }
            if (contentEnd > contentStart && text.charAt(contentEnd - 1) == ' ')
            {
                contentEnd--;
            }

            document.deleteString(contentEnd, endOffset);
            document.deleteString(startOffset, contentStart);
        }
        else if (commentText.startsWith("/*") && commentText.endsWith("*/"))
        {
            int prefixLen = 2;
            int suffixLen = 2;
            int contentStart = startOffset + prefixLen;
            int contentEnd = endOffset - suffixLen;

            if (contentStart < contentEnd && text.charAt(contentStart) == ' ')
            {
                contentStart++;
            }
            if (contentEnd > contentStart && text.charAt(contentEnd - 1) == ' ')
            {
                contentEnd--;
            }

            document.deleteString(contentEnd, endOffset);
            document.deleteString(startOffset, contentStart);
        }
    }

    @Override
    public @Nullable TextRange insertBlockComment(int startOffset, int endOffset, @NotNull Document document, @NotNull CFMLCommenterData data)
    {
        boolean isScript = data.isScriptRange(startOffset, endOffset);
        String prefix = isScript ? "/* " : "<!--- ";
        String suffix = isScript ? " */" : " --->";

        document.insertString(endOffset, suffix);
        document.insertString(startOffset, prefix);

        return new TextRange(startOffset, endOffset + prefix.length() + suffix.length());
    }
}
