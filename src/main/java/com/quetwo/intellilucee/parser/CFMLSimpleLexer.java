package com.quetwo.intellilucee.parser;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CFMLSimpleLexer extends LexerBase
{
    private CharSequence buffer = "";
    private int bufferStart = 0;
    private int bufferEnd = 0;
    private int tokenStart = 0;
    private int tokenEnd = 0;
    private boolean exhausted = true;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
    {
        this.buffer = buffer;
        bufferStart = startOffset;
        bufferEnd = endOffset;
        tokenStart = startOffset;
        tokenEnd = startOffset;
        exhausted = startOffset >= endOffset;
    }

    @Override
    public int getState()
    {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType()
    {
        return exhausted ? null : CFMLTokenTypes.TEXT;
    }

    @Override
    public int getTokenStart()
    {
        return tokenStart;
    }

    @Override
    public int getTokenEnd()
    {
        return tokenEnd;
    }

    @Override
    public void advance()
    {
        if (exhausted)
        {
            return;
        }

        tokenStart = bufferEnd;
        tokenEnd = bufferEnd;
        exhausted = true;
    }

    @Override
    public @NotNull CharSequence getBufferSequence()
    {
        return buffer;
    }

    @Override
    public int getBufferEnd()
    {
        return bufferEnd;
    }
}