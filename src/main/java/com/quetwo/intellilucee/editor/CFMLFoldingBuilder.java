package com.quetwo.intellilucee.editor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class CFMLFoldingBuilder extends FoldingBuilderEx
{

    private static final String[] FOLD_KEYWORDS = {"if", "case", "switch", "function"};

    @Override
    public @NotNull FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick)
    {
        String text = root.getText();
        ASTNode rootNode = root.getNode();
        if (rootNode == null)
        {
            return FoldingDescriptor.EMPTY_ARRAY;
        }
        List<FoldingDescriptor> descriptors = new ArrayList<>();

        buildScriptFoldRegions(text, rootNode, descriptors);
        buildTagFoldRegions(text, rootNode, descriptors);

        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void buildScriptFoldRegions(String text, ASTNode rootNode, List<FoldingDescriptor> descriptors)
    {
        for (String keyword : FOLD_KEYWORDS)
        {
            int searchFrom = 0;
            int textLen = text.length();
            while (searchFrom < textLen)
            {
                int keywordStart = indexOfKeyword(text, keyword, searchFrom);
                if (keywordStart < 0)
                {
                    break;
                }

                int maxLookahead = Math.min(textLen, keywordStart + 2000);
                int openingBrace = -1;
                for (int k = keywordStart + keyword.length(); k < maxLookahead; k++)
                {
                    char c = text.charAt(k);
                    if (c == ';')
                    {
                        break;
                    }
                    if (c == '{')
                    {
                        openingBrace = k;
                        break;
                    }
                }

                if (openingBrace < 0)
                {
                    searchFrom = keywordStart + keyword.length();
                    continue;
                }

                int closingBrace = findMatchingBrace(text, openingBrace);
                if (closingBrace > openingBrace)
                {
                    addFoldRegion(descriptors, rootNode, text, keywordStart, closingBrace + 1);
                    searchFrom = closingBrace + 1;
                }
                else
                {
                    searchFrom = openingBrace + 1;
                }
            }
        }
    }

    private void buildTagFoldRegions(String text, ASTNode rootNode, List<FoldingDescriptor> descriptors)
    {
        List<TagInfo> tags = scanTags(text);
        Deque<TagFrame> stack = new ArrayDeque<>();

        for (TagInfo tag : tags)
        {
            String name = tag.name;

            if (tag.isClosing)
            {
                if ("cfelseif".equals(name))
                {
                    TagFrame cfifFrame = findEnclosingFrame(stack, "cfif");
                    if (cfifFrame != null && cfifFrame.activeElseIfStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, cfifFrame.activeElseIfStart, tag.endOffset);
                        cfifFrame.activeElseIfStart = -1;
                    }
                }
                else if ("cfcase".equals(name))
                {
                    TagFrame caseFrame = findAndPopFrame(stack, "cfcase");
                    if (caseFrame != null)
                    {
                        addFoldRegion(descriptors, rootNode, text, caseFrame.startOffset, tag.endOffset);
                    }
                    TagFrame switchFrame = findEnclosingFrame(stack, "cfswitch");
                    if (switchFrame != null)
                    {
                        switchFrame.activeCaseStart = -1;
                    }
                }
                else if ("cfswitch".equals(name))
                {
                    TagFrame switchFrame = findEnclosingFrame(stack, "cfswitch");
                    if (switchFrame != null && switchFrame.activeCaseStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, switchFrame.activeCaseStart, tag.startOffset);
                        switchFrame.activeCaseStart = -1;
                    }
                    TagFrame popped = findAndPopFrame(stack, "cfswitch");
                    if (popped != null)
                    {
                        addFoldRegion(descriptors, rootNode, text, popped.startOffset, tag.endOffset);
                    }
                }
                else if ("cfif".equals(name))
                {
                    TagFrame cfifFrame = findEnclosingFrame(stack, "cfif");
                    if (cfifFrame != null && cfifFrame.activeElseIfStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, cfifFrame.activeElseIfStart, tag.startOffset);
                        cfifFrame.activeElseIfStart = -1;
                    }
                    TagFrame popped = findAndPopFrame(stack, "cfif");
                    if (popped != null)
                    {
                        addFoldRegion(descriptors, rootNode, text, popped.startOffset, tag.endOffset);
                    }
                }
                else if ("cftry".equals(name) || "cfloop".equals(name) || "cfquery".equals(name))
                {
                    TagFrame popped = findAndPopFrame(stack, name);
                    if (popped != null)
                    {
                        addFoldRegion(descriptors, rootNode, text, popped.startOffset, tag.endOffset);
                    }
                }
            }
            else
            {
                if (tag.isSelfClosing)
                {
                    continue;
                }

                if ("cfif".equals(name))
                {
                    stack.push(new TagFrame("cfif", tag.startOffset));
                }
                else if ("cfelseif".equals(name))
                {
                    TagFrame cfifFrame = findEnclosingFrame(stack, "cfif");
                    if (cfifFrame != null)
                    {
                        if (cfifFrame.activeElseIfStart != -1)
                        {
                            addFoldRegion(descriptors, rootNode, text, cfifFrame.activeElseIfStart, tag.startOffset);
                        }
                        cfifFrame.activeElseIfStart = tag.startOffset;
                    }
                }
                else if ("cfelse".equals(name))
                {
                    TagFrame cfifFrame = findEnclosingFrame(stack, "cfif");
                    if (cfifFrame != null && cfifFrame.activeElseIfStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, cfifFrame.activeElseIfStart, tag.startOffset);
                        cfifFrame.activeElseIfStart = -1;
                    }
                }
                else if ("cfswitch".equals(name))
                {
                    stack.push(new TagFrame("cfswitch", tag.startOffset));
                }
                else if ("cfcase".equals(name))
                {
                    TagFrame switchFrame = findEnclosingFrame(stack, "cfswitch");
                    if (switchFrame != null && switchFrame.activeCaseStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, switchFrame.activeCaseStart, tag.startOffset);
                    }
                    if (switchFrame != null)
                    {
                        switchFrame.activeCaseStart = tag.startOffset;
                    }
                    stack.push(new TagFrame("cfcase", tag.startOffset));
                }
                else if ("cfdefaultcase".equals(name))
                {
                    TagFrame switchFrame = findEnclosingFrame(stack, "cfswitch");
                    if (switchFrame != null && switchFrame.activeCaseStart != -1)
                    {
                        addFoldRegion(descriptors, rootNode, text, switchFrame.activeCaseStart, tag.startOffset);
                        switchFrame.activeCaseStart = -1;
                    }
                }
                else if ("cftry".equals(name) || "cfloop".equals(name) || "cfquery".equals(name))
                {
                    stack.push(new TagFrame(name, tag.startOffset));
                }
            }
        }
    }

    private TagFrame findEnclosingFrame(Deque<TagFrame> stack, String tagName)
    {
        for (TagFrame frame : stack)
        {
            if (frame.name.equals(tagName))
            {
                return frame;
            }
        }
        return null;
    }

    private TagFrame findAndPopFrame(Deque<TagFrame> stack, String tagName)
    {
        while (!stack.isEmpty())
        {
            TagFrame frame = stack.pop();
            if (frame.name.equals(tagName))
            {
                return frame;
            }
        }
        return null;
    }

    private void addFoldRegion(List<FoldingDescriptor> descriptors, ASTNode rootNode, String text, int startOffset, int endOffset)
    {
        if (startOffset >= 0 && endOffset <= text.length() && endOffset > startOffset)
        {
            TextRange range = TextRange.create(startOffset, endOffset);
            if (isMultiline(text, range))
            {
                descriptors.add(new FoldingDescriptor(rootNode, range, null));
            }
        }
    }

    private List<TagInfo> scanTags(String text)
    {
        List<TagInfo> tags = new ArrayList<>();
        int i = 0;
        int len = text.length();

        while (i < len)
        {
            // CFML comment: <!--- ... --->
            if (i + 4 < len && text.startsWith("<!---", i))
            {
                int depth = 1;
                i += 5;
                while (i < len && depth > 0)
                {
                    if (i + 4 < len && text.startsWith("<!---", i))
                    {
                        depth++;
                        i += 5;
                    }
                    else if (i + 3 < len && text.startsWith("--->", i))
                    {
                        depth--;
                        i += 4;
                    }
                    else
                    {
                        i++;
                    }
                }
                continue;
            }

            // HTML comment: <!-- ... -->
            if (i + 3 < len && text.startsWith("<!--", i))
            {
                int end = text.indexOf("-->", i + 4);
                if (end >= 0)
                {
                    i = end + 3;
                }
                else
                {
                    i = len;
                }
                continue;
            }

            // C-style block comment: /* ... */
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '*')
            {
                int end = text.indexOf("*/", i + 2);
                if (end >= 0)
                {
                    i = end + 2;
                }
                else
                {
                    i = len;
                }
                continue;
            }

            // C-style line comment: // ...
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '/')
            {
                int end = text.indexOf('\n', i + 2);
                if (end >= 0)
                {
                    i = end + 1;
                }
                else
                {
                    i = len;
                }
                continue;
            }

            if (text.charAt(i) == '<')
            {
                int tagStart = i;
                int cursor = i + 1;
                boolean isClosing = false;
                if (cursor < len && text.charAt(cursor) == '/')
                {
                    isClosing = true;
                    cursor++;
                }

                while (cursor < len && Character.isWhitespace(text.charAt(cursor)))
                {
                    cursor++;
                }

                int nameStart = cursor;
                while (cursor < len && isTagIdentifierChar(text.charAt(cursor)))
                {
                    cursor++;
                }

                if (cursor > nameStart)
                {
                    boolean isCfTag = (cursor - nameStart >= 2) &&
                            (text.charAt(nameStart) == 'c' || text.charAt(nameStart) == 'C') &&
                            (text.charAt(nameStart + 1) == 'f' || text.charAt(nameStart + 1) == 'F');

                    String tagName = isCfTag ? text.substring(nameStart, cursor).toLowerCase(Locale.ROOT) : null;
                    boolean isSelfClosing = false;

                    while (cursor < len)
                    {
                        char c = text.charAt(cursor);
                        if (c == '"' || c == '\'')
                        {
                            char quote = c;
                            cursor++;
                            while (cursor < len)
                            {
                                if (text.charAt(cursor) == quote)
                                {
                                    if (cursor + 1 < len && text.charAt(cursor + 1) == quote)
                                    {
                                        cursor += 2;
                                    }
                                    else
                                    {
                                        cursor++;
                                        break;
                                    }
                                }
                                else
                                {
                                    cursor++;
                                }
                            }
                        }
                        else if (c == '>')
                        {
                            if (cursor > tagStart && text.charAt(cursor - 1) == '/')
                            {
                                isSelfClosing = true;
                            }
                            cursor++;
                            break;
                        }
                        else
                        {
                            cursor++;
                        }
                    }

                    if (isCfTag && tagName != null)
                    {
                        tags.add(new TagInfo(tagName, tagStart, cursor, isClosing, isSelfClosing));
                    }
                    i = cursor;
                    continue;
                }
            }

            i++;
        }

        return tags;
    }

    private static boolean isTagIdentifierChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':';
    }

    private boolean isMultiline(String text, TextRange range)
    {
        for (int i = range.getStartOffset(); i < range.getEndOffset(); i++)
        {
            if (text.charAt(i) == '\n')
            {
                return true;
            }
        }
        return false;
    }

    private int indexOfKeyword(String text, String keyword, int fromIndex)
    {
        int keywordStart = text.indexOf(keyword, fromIndex);
        while (keywordStart >= 0)
        {
            if (isWordBoundary(text, keywordStart - 1)
                    && isWordBoundary(text, keywordStart + keyword.length()))
            {
                return keywordStart;
            }
            keywordStart = text.indexOf(keyword, keywordStart + 1);
        }
        return -1;
    }

    private boolean isWordBoundary(String text, int index)
    {
        if (index < 0 || index >= text.length())
        {
            return true;
        }
        char c = text.charAt(index);
        return !Character.isLetterOrDigit(c) && c != '_';
    }

    private int findMatchingBrace(String text, int openingBrace)
    {
        int depth = 0;
        int len = text.length();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = openingBrace; i < len; i++)
        {
            char c = text.charAt(i);
            if (inSingle)
            {
                if (c == '\'') inSingle = false;
                continue;
            }
            if (inDouble)
            {
                if (c == '"') inDouble = false;
                continue;
            }
            if (c == '\'')
            {
                inSingle = true;
                continue;
            }
            if (c == '"')
            {
                inDouble = true;
                continue;
            }
            if (c == '{')
            {
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node)
    {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node)
    {
        return false;
    }

    private static class TagInfo
    {
        final String name;
        final int startOffset;
        final int endOffset;
        final boolean isClosing;
        final boolean isSelfClosing;

        TagInfo(String name, int startOffset, int endOffset, boolean isClosing, boolean isSelfClosing)
        {
            this.name = name;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.isClosing = isClosing;
            this.isSelfClosing = isSelfClosing;
        }
    }

    private static class TagFrame
    {
        final String name;
        final int startOffset;
        int activeElseIfStart = -1;
        int activeCaseStart = -1;

        TagFrame(String name, int startOffset)
        {
            this.name = name;
            this.startOffset = startOffset;
        }
    }

}