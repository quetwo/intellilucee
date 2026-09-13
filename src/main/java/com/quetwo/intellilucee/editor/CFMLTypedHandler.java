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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CFMLTypedHandler extends TypedHandlerDelegate
{
    private static final Set<String> PARENTHESIZED_KEYWORDS = new HashSet<>(Arrays.asList(
        "if",
        "elseif",
        "for",
        "while",
        "switch",
        "catch",
        "cfif",
        "cfelseif",
        "cfloop",
        "cfswitch",
        "cfcatch"
    ));

    private static final Set<String> SINGLE_KEYWORDS = new HashSet<>(Arrays.asList(
        "else",
        "cfelse",
        "do",
        "try",
        "cftry",
        "finally",
        "cffinally"
    ));

    private static final Set<String> CASE_KEYWORDS = new HashSet<>(Arrays.asList(
        "case",
        "cfcase",
        "default",
        "cfdefaultcase"
    ));

    private static final Set<String> TAG_BLOCK_KEYWORDS = new HashSet<>(Arrays.asList(
        "component",
        "cfcomponent",
        "interface",
        "cfinterface",
        "lock",
        "cflock",
        "transaction",
        "cftransaction",
        "thread",
        "cfthread",
        "savecontent",
        "cfsavecontent",
        "loop",
        "cfloop",
        "http",
        "cfhttp",
        "query",
        "cfquery"
    ));

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
        if (c != '>' && c != '{')
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

        if (c == '>')
        {
            handleGreaterThanTyped(offset, document, chars);
        }
        else if (c == '{')
        {
            handleOpeningBraceTyped(offset, document, chars);
        }

        return Result.CONTINUE;
    }

    private static void handleGreaterThanTyped(int offset, @NotNull Document document, @NotNull CharSequence chars)
    {
        if (offset <= 0 || offset > chars.length() || chars.charAt(offset - 1) != '>')
        {
            return;
        }

        if (offset >= 2 && chars.charAt(offset - 2) == '/')
        {
            return;
        }

        String tagName = findOpeningCfmlTagName(chars, offset - 1);
        if (tagName != null && !tagName.isEmpty())
        {
            if (EXCLUDED_AUTO_CLOSE_TAGS.contains(tagName.toLowerCase(Locale.ROOT)))
            {
                return;
            }

            String closingTag = "</" + tagName + ">";
            if (offset + closingTag.length() > chars.length() ||
                !chars.subSequence(offset, offset + closingTag.length()).toString().equalsIgnoreCase(closingTag))
            {
                document.insertString(offset, closingTag);
            }
        }
    }

    private static void handleOpeningBraceTyped(int offset, @NotNull Document document, @NotNull CharSequence chars)
    {
        if (offset <= 0 || offset > chars.length() || chars.charAt(offset - 1) != '{')
        {
            return;
        }

        if (offset < chars.length() && chars.charAt(offset) == '}')
        {
            return;
        }

        int bracePos = offset - 1;
        if (shouldAutoCloseBrace(chars, bracePos))
        {
            document.insertString(offset, "}");
        }
    }

    private static boolean shouldAutoCloseBrace(@NotNull CharSequence chars, int bracePos)
    {
        if (isFunctionDeclarationBeforeBrace(chars, bracePos))
        {
            return true;
        }
        return isControlFlowOrBlockHeaderBeforeBrace(chars, bracePos);
    }

    private static boolean isControlFlowOrBlockHeaderBeforeBrace(@NotNull CharSequence chars, int bracePos)
    {
        int minPos = Math.max(0, bracePos - 5000);
        int i = minPos;
        int stmtStart = minPos;
        int parenDepth = 0;
        int bracketDepth = 0;

        while (i < bracePos)
        {
            char c = chars.charAt(i);
            if (c == '"' || c == '\'')
            {
                i = skipStringLiteral(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '/')
            {
                i = skipLineComment(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '*')
            {
                i = skipBlockComment(chars, i);
            }
            else if (c == '<' && i + 1 < chars.length() && (Character.isLetter(chars.charAt(i + 1)) || chars.charAt(i + 1) == '/'))
            {
                int gtPos = chars.toString().indexOf('>', i + 1);
                if (gtPos >= 0 && gtPos < bracePos)
                {
                    stmtStart = gtPos + 1;
                    i = gtPos + 1;
                }
                else
                {
                    i++;
                }
            }
            else if (c == '(')
            {
                parenDepth++;
                i++;
            }
            else if (c == ')')
            {
                if (parenDepth > 0)
                {
                    parenDepth--;
                }
                i++;
            }
            else if (c == '[')
            {
                bracketDepth++;
                i++;
            }
            else if (c == ']')
            {
                if (bracketDepth > 0)
                {
                    bracketDepth--;
                }
                i++;
            }
            else if (parenDepth == 0 && bracketDepth == 0 && (c == ';' || c == '{' || c == '}'))
            {
                stmtStart = i + 1;
                i++;
            }
            else
            {
                i++;
            }
        }

        if (parenDepth != 0 || bracketDepth != 0)
        {
            return false;
        }

        List<String> tokens = new ArrayList<>();
        int j = stmtStart;
        while (j < bracePos)
        {
            char c = chars.charAt(j);
            if (Character.isWhitespace(c))
            {
                j++;
            }
            else if (c == '"' || c == '\'')
            {
                int end = skipStringLiteral(chars, j);
                tokens.add(chars.subSequence(j, Math.min(end, bracePos)).toString());
                j = end;
            }
            else if (c == '/' && j + 1 < chars.length() && chars.charAt(j + 1) == '/')
            {
                j = skipLineComment(chars, j);
            }
            else if (c == '/' && j + 1 < chars.length() && chars.charAt(j + 1) == '*')
            {
                j = skipBlockComment(chars, j);
            }
            else if (Character.isLetterOrDigit(c) || c == '_' || c == '$')
            {
                int startWord = j;
                while (j < bracePos)
                {
                    char ch = chars.charAt(j);
                    if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$')
                    {
                        j++;
                    }
                    else
                    {
                        break;
                    }
                }
                tokens.add(chars.subSequence(startWord, j).toString());
            }
            else if (c == '=' && j + 1 < bracePos && chars.charAt(j + 1) == '>')
            {
                tokens.add("=>");
                j += 2;
            }
            else if (c == '-' && j + 1 < bracePos && chars.charAt(j + 1) == '>')
            {
                tokens.add("->");
                j += 2;
            }
            else if (c == '=' && j + 1 < bracePos && chars.charAt(j + 1) == '=')
            {
                tokens.add("==");
                j += 2;
            }
            else if (c == '!' && j + 1 < bracePos && chars.charAt(j + 1) == '=')
            {
                tokens.add("!=");
                j += 2;
            }
            else if (c == '<' && j + 1 < bracePos && chars.charAt(j + 1) == '=')
            {
                tokens.add("<=");
                j += 2;
            }
            else if (c == '>' && j + 1 < bracePos && chars.charAt(j + 1) == '=')
            {
                tokens.add(">=");
                j += 2;
            }
            else
            {
                tokens.add(String.valueOf(c));
                j++;
            }
        }

        if (tokens.isEmpty())
        {
            return false;
        }

        String lastToken = tokens.get(tokens.size() - 1);
        if (lastToken.equals("=") || lastToken.equals("+") || lastToken.equals("-") ||
            lastToken.equals("*") || lastToken.equals("/") || lastToken.equals(",") ||
            lastToken.equals("?") || lastToken.equals("||") || lastToken.equals("&&"))
        {
            return false;
        }

        if (lastToken.equals("=>") || lastToken.equals("->"))
        {
            return true;
        }

        if (tokens.size() >= 2 && (tokens.get(1).equals("=") || tokens.get(1).equals("+=") ||
            tokens.get(1).equals("-=") || tokens.get(1).equals("*=") || tokens.get(1).equals("/=")))
        {
            return false;
        }

        if (tokens.size() >= 4 &&
            tokens.get(0).equalsIgnoreCase("else") &&
            tokens.get(1).equalsIgnoreCase("if") &&
            lastToken.equals(")"))
        {
            return true;
        }

        String first = tokens.get(0).toLowerCase(Locale.ROOT);

        if (PARENTHESIZED_KEYWORDS.contains(first) && lastToken.equals(")"))
        {
            return true;
        }

        if (SINGLE_KEYWORDS.contains(first) && tokens.size() == 1)
        {
            return true;
        }

        if (CASE_KEYWORDS.contains(first))
        {
            return true;
        }

        if (TAG_BLOCK_KEYWORDS.contains(first))
        {
            return true;
        }

        return false;
    }

    private static boolean isFunctionDeclarationBeforeBrace(@NotNull CharSequence chars, int bracePos)
    {
        int minPos = Math.max(0, bracePos - 5000);
        int i = minPos;
        while (i < bracePos)
        {
            char c = chars.charAt(i);
            if (c == '"' || c == '\'')
            {
                i = skipStringLiteral(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '/')
            {
                i = skipLineComment(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '*')
            {
                i = skipBlockComment(chars, i);
            }
            else if (isFunctionKeywordAt(chars, i))
            {
                if (isFunctionDeclarationStartingAt(chars, i, bracePos))
                {
                    return true;
                }
                i += 8;
            }
            else
            {
                i++;
            }
        }
        return false;
    }

    private static boolean isFunctionKeywordAt(@NotNull CharSequence chars, int pos)
    {
        if (pos + 8 > chars.length())
        {
            return false;
        }
        String sub = chars.subSequence(pos, pos + 8).toString();
        if (!sub.equalsIgnoreCase("function"))
        {
            return false;
        }
        if (pos > 0)
        {
            char prev = chars.charAt(pos - 1);
            if (Character.isLetterOrDigit(prev) || prev == '_' || prev == '$' || prev == '.')
            {
                return false;
            }
        }
        if (pos + 8 < chars.length())
        {
            char next = chars.charAt(pos + 8);
            if (Character.isLetterOrDigit(next) || next == '_' || next == '$')
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isFunctionDeclarationStartingAt(@NotNull CharSequence chars, int funcPos, int targetBracePos)
    {
        int i = funcPos + 8;
        i = skipWhitespaceAndComments(chars, i, targetBracePos);
        if (i >= targetBracePos)
        {
            return false;
        }

        char firstChar = chars.charAt(i);
        if (firstChar != '(')
        {
            if (!Character.isJavaIdentifierStart(firstChar))
            {
                return false;
            }
            i++;
            while (i < targetBracePos)
            {
                char ch = chars.charAt(i);
                if (Character.isJavaIdentifierPart(ch) || ch == '-')
                {
                    i++;
                }
                else
                {
                    break;
                }
            }
            i = skipWhitespaceAndComments(chars, i, targetBracePos);
        }

        if (i >= targetBracePos || chars.charAt(i) != '(')
        {
            return false;
        }

        int parenDepth = 0;
        while (i < targetBracePos)
        {
            char c = chars.charAt(i);
            if (c == '"' || c == '\'')
            {
                i = skipStringLiteral(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '/')
            {
                i = skipLineComment(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '*')
            {
                i = skipBlockComment(chars, i);
            }
            else if (c == '(')
            {
                parenDepth++;
                i++;
            }
            else if (c == ')')
            {
                parenDepth--;
                i++;
                if (parenDepth == 0)
                {
                    break;
                }
            }
            else if (c == ';' || c == '{' || c == '}')
            {
                return false;
            }
            else
            {
                i++;
            }
        }

        if (parenDepth != 0)
        {
            return false;
        }

        while (i < targetBracePos)
        {
            char c = chars.charAt(i);
            if (c == '"' || c == '\'')
            {
                i = skipStringLiteral(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '/')
            {
                i = skipLineComment(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '*')
            {
                i = skipBlockComment(chars, i);
            }
            else if (c == ';' || c == '{' || c == '}')
            {
                return false;
            }
            else
            {
                i++;
            }
        }

        return i == targetBracePos;
    }

    private static int skipWhitespaceAndComments(@NotNull CharSequence chars, int start, int maxPos)
    {
        int i = start;
        while (i < maxPos)
        {
            char c = chars.charAt(i);
            if (Character.isWhitespace(c))
            {
                i++;
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '/')
            {
                i = skipLineComment(chars, i);
            }
            else if (c == '/' && i + 1 < chars.length() && chars.charAt(i + 1) == '*')
            {
                i = skipBlockComment(chars, i);
            }
            else
            {
                break;
            }
        }
        return i;
    }

    private static int skipStringLiteral(@NotNull CharSequence chars, int start)
    {
        char quote = chars.charAt(start);
        int i = start + 1;
        int len = chars.length();
        while (i < len)
        {
            char c = chars.charAt(i);
            if (c == '\\')
            {
                i += 2;
                continue;
            }
            if (c == quote)
            {
                if (i + 1 < len && chars.charAt(i + 1) == quote)
                {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return len;
    }

    private static int skipLineComment(@NotNull CharSequence chars, int start)
    {
        int i = start + 2;
        int len = chars.length();
        while (i < len && chars.charAt(i) != '\n' && chars.charAt(i) != '\r')
        {
            i++;
        }
        return i;
    }

    private static int skipBlockComment(@NotNull CharSequence chars, int start)
    {
        int i = start + 2;
        int len = chars.length();
        while (i + 1 < len)
        {
            if (chars.charAt(i) == '*' && chars.charAt(i + 1) == '/')
            {
                return i + 2;
            }
            i++;
        }
        return len;
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
