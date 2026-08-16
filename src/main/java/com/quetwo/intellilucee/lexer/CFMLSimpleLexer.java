package com.quetwo.intellilucee.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
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
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
    {
        this.buffer = buffer;
        bufferStart = startOffset;
        bufferEnd = endOffset;
        tokenStart = startOffset;
        tokenEnd = startOffset;
        tokenType = null;
        locateToken(startOffset);
    }

    @Override
    public int getState()
    {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType()
    {
        return tokenType;
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
        if (tokenType == null)
        {
            return;
        }

        locateToken(tokenEnd);
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

    private void locateToken(int offset)
    {
        if (offset >= bufferEnd)
        {
            tokenStart = bufferEnd;
            tokenEnd = bufferEnd;
            tokenType = null;
            return;
        }

        tokenStart = offset;
        char firstChar = buffer.charAt(offset);

        if (Character.isWhitespace(firstChar))
        {
            int current = offset + 1;
            while (current < bufferEnd && Character.isWhitespace(buffer.charAt(current)))
            {
                current++;
            }

            tokenEnd = current;
            tokenType = TokenType.WHITE_SPACE;
            return;
        }

        if (isCommentStart(offset))
        {
            int commentEnd = indexOf(offset + 5, "--->");
            tokenEnd = commentEnd < 0 ? bufferEnd : commentEnd + 4;
            tokenType = CFMLTokenTypes.COMMENT;
            return;
        }

        if (firstChar == '<')
        {
            int current = offset + 1;
            while (current < bufferEnd)
            {
                char currentChar = buffer.charAt(current);
                if (currentChar == '>')
                {
                    current++;
                    break;
                }
                current++;
            }

            tokenEnd = current;
            tokenType = CFMLTokenTypes.TAG;
            return;
        }

        int current = offset + 1;
        while (current < bufferEnd)
        {
            char currentChar = buffer.charAt(current);
            if (Character.isWhitespace(currentChar) || currentChar == '<')
            {
                break;
            }
            current++;
        }

        tokenEnd = current;
        tokenType = CFMLTokenTypes.TEXT;
    }

    private boolean isCommentStart(int offset)
    {
        return offset + 4 < bufferEnd
            && buffer.charAt(offset) == '<'
            && buffer.charAt(offset + 1) == '!'
            && buffer.charAt(offset + 2) == '-'
            && buffer.charAt(offset + 3) == '-'
            && buffer.charAt(offset + 4) == '-';
    }

    private int indexOf(int fromIndex, @NotNull String sequence)
    {
        int max = bufferEnd - sequence.length();
        for (int index = fromIndex; index <= max; index++)
        {
            boolean matches = true;
            for (int partIndex = 0; partIndex < sequence.length(); partIndex++)
            {
                if (buffer.charAt(index + partIndex) != sequence.charAt(partIndex))
                {
                    matches = false;
                    break;
                }
            }

            if (matches)
            {
                return index;
            }
        }

        return -1;
    }
}