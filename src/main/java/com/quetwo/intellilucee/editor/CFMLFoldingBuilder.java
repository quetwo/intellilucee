package com.quetwo.intellilucee.editor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
            return FoldingDescriptor.EMPTY;
        }
        List<FoldingDescriptor> descriptors = new ArrayList<>();

        for (String keyword : FOLD_KEYWORDS)
        {
            int searchFrom = 0;
            while (searchFrom < text.length())
            {
                int keywordStart = indexOfKeyword(text, keyword, searchFrom);
                if (keywordStart < 0)
                {
                    break;
                }

                int openingBrace = text.indexOf('{', keywordStart + keyword.length());
                if (openingBrace < 0)
                {
                    break;
                }

                int closingBrace = findMatchingBrace(text, openingBrace);
                if (closingBrace > openingBrace)
                {
                    TextRange range = TextRange.create(keywordStart, closingBrace + 1);
                    if (isMultiline(text, range))
                    {
                        descriptors.add(new FoldingDescriptor(rootNode, range));
                    }
                    searchFrom = closingBrace + 1;
                }
                else
                {
                    searchFrom = openingBrace + 1;
                }
            }
        }

        return descriptors.toArray(new FoldingDescriptor[0]);
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
        for (int i = openingBrace; i < text.length(); i++)
        {
            char c = text.charAt(i);
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

}