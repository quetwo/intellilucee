package com.quetwo.intellilucee.parser;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CFMLLexer extends LexerBase
{

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;
    private boolean tokenConsumed;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
    {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenConsumed = startOffset >= endOffset;
    }

    @Override
    public int getState()
    {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType()
    {
        return tokenConsumed ? null : CFMLTokenTypes.TEXT;
    }

    @Override
    public int getTokenStart()
    {
        return tokenConsumed ? endOffset : startOffset;
    }

    @Override
    public int getTokenEnd()
    {
        return endOffset;
    }

    @Override
    public void advance()
    {
        tokenConsumed = true;
    }

    @Override
    public @NotNull CharSequence getBufferSequence()
    {
        return buffer;
    }

    @Override
    public int getBufferEnd()
    {
        return endOffset;
    }

}