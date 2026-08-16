package com.quetwo.intellilucee.syntax;

import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import org.jetbrains.annotations.NotNull;

public final class CFMLSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory
{
    @Override
    protected @NotNull SyntaxHighlighter createHighlighter()
    {
        return new CFMLSyntaxHighlighter();
    }
}