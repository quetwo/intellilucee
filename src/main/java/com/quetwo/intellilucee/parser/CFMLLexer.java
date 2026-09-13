package com.quetwo.intellilucee.parser;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CFMLLexer extends LexerBase
{

    private CharSequence buffer;
    private int bufferEnd;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
    {
        this.buffer = buffer;
        this.bufferEnd = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.tokenType = null;
        advance();
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
        if (tokenEnd >= bufferEnd)
        {
            tokenStart = bufferEnd;
            tokenType = null;
            return;
        }

        tokenStart = tokenEnd;
        int i = tokenStart;
        char c = buffer.charAt(i);

        // 1. Whitespace
        if (Character.isWhitespace(c))
        {
            while (i < bufferEnd && Character.isWhitespace(buffer.charAt(i)))
            {
                i++;
            }
            tokenEnd = i;
            tokenType = TokenType.WHITE_SPACE;
            return;
        }

        // 2. CFML Tag comment: <!--- ... --->
        if (c == '<' && i + 4 < bufferEnd && buffer.charAt(i + 1) == '!' && buffer.charAt(i + 2) == '-' && buffer.charAt(i + 3) == '-' && buffer.charAt(i + 4) == '-')
        {
            int depth = 1;
            i += 5;
            while (i < bufferEnd && depth > 0)
            {
                if (i + 4 < bufferEnd && buffer.charAt(i) == '<' && buffer.charAt(i + 1) == '!' && buffer.charAt(i + 2) == '-' && buffer.charAt(i + 3) == '-' && buffer.charAt(i + 4) == '-')
                {
                    depth++;
                    i += 5;
                }
                else if (i + 3 < bufferEnd && buffer.charAt(i) == '-' && buffer.charAt(i + 1) == '-' && buffer.charAt(i + 2) == '-' && buffer.charAt(i + 3) == '>')
                {
                    depth--;
                    i += 4;
                }
                else
                {
                    i++;
                }
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.COMMENT;
            return;
        }

        // 3. HTML comment: <!-- ... -->
        if (c == '<' && i + 3 < bufferEnd && buffer.charAt(i + 1) == '!' && buffer.charAt(i + 2) == '-' && buffer.charAt(i + 3) == '-')
        {
            i += 4;
            while (i + 2 < bufferEnd)
            {
                if (buffer.charAt(i) == '-' && buffer.charAt(i + 1) == '-' && buffer.charAt(i + 2) == '>')
                {
                    i += 3;
                    break;
                }
                i++;
            }
            if (i + 2 >= bufferEnd && i < bufferEnd)
            {
                i = bufferEnd;
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.COMMENT;
            return;
        }

        // 4. Script block comment: /* ... */
        if (c == '/' && i + 1 < bufferEnd && buffer.charAt(i + 1) == '*')
        {
            i += 2;
            while (i + 1 < bufferEnd)
            {
                if (buffer.charAt(i) == '*' && buffer.charAt(i + 1) == '/')
                {
                    i += 2;
                    break;
                }
                i++;
            }
            if (i + 1 >= bufferEnd && i < bufferEnd)
            {
                i = bufferEnd;
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.COMMENT;
            return;
        }

        // 5. Script line comment: // ...
        if (c == '/' && i + 1 < bufferEnd && buffer.charAt(i + 1) == '/')
        {
            i += 2;
            while (i < bufferEnd && buffer.charAt(i) != '\n' && buffer.charAt(i) != '\r')
            {
                i++;
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.COMMENT;
            return;
        }

        // 6. Strings: "..." or '...'
        if (c == '"' || c == '\'')
        {
            char quote = c;
            i++;
            while (i < bufferEnd)
            {
                char ch = buffer.charAt(i);
                if (ch == '\\' && i + 1 < bufferEnd)
                {
                    i += 2;
                    continue;
                }
                if (ch == quote)
                {
                    if (i + 1 < bufferEnd && buffer.charAt(i + 1) == quote)
                    {
                        i += 2;
                        continue;
                    }
                    i++;
                    break;
                }
                i++;
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.STRING;
            return;
        }

        // 7. Identifiers
        if (Character.isJavaIdentifierStart(c) || c == '$')
        {
            while (i < bufferEnd && (Character.isJavaIdentifierPart(buffer.charAt(i)) || buffer.charAt(i) == '$'))
            {
                i++;
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.IDENTIFIER;
            return;
        }

        // 8. Numbers
        if (Character.isDigit(c))
        {
            while (i < bufferEnd && Character.isDigit(buffer.charAt(i)))
            {
                i++;
            }
            if (i < bufferEnd && buffer.charAt(i) == '.' && i + 1 < bufferEnd && Character.isDigit(buffer.charAt(i + 1)))
            {
                i++;
                while (i < bufferEnd && Character.isDigit(buffer.charAt(i)))
                {
                    i++;
                }
            }
            tokenEnd = i;
            tokenType = CFMLTokenTypes.TEXT;
            return;
        }

        // 9. Single symbol / punctuation character / other
        tokenEnd = tokenStart + 1;
        tokenType = CFMLTokenTypes.TEXT;
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