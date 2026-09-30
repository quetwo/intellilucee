package com.quetwo.intellilucee.parser;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CFMLLexer extends LexerBase
{

    public static final int STATE_DEFAULT = 0;
    public static final int STATE_AFTER_TAG_START = 1;
    public static final int STATE_INSIDE_TAG = 2;

    private static final Map<String, IElementType> KEYWORDS = new HashMap<>();
    private static final Map<String, IElementType> TYPES = new HashMap<>();
    private static final Map<String, IElementType> SCOPES = new HashMap<>();
    private static final Map<String, IElementType> WORD_OPERATORS = new HashMap<>();

    static
    {
        // Keywords
        KEYWORDS.put("var", CFMLTokenTypes.VAR_KEYWORD);
        KEYWORDS.put("local", CFMLTokenTypes.LOCAL_KEYWORD);
        KEYWORDS.put("function", CFMLTokenTypes.FUNCTION_KEYWORD);
        KEYWORDS.put("component", CFMLTokenTypes.COMPONENT_KEYWORD);
        KEYWORDS.put("interface", CFMLTokenTypes.INTERFACE_KEYWORD);
        KEYWORDS.put("extends", CFMLTokenTypes.EXTENDS_KEYWORD);
        KEYWORDS.put("implements", CFMLTokenTypes.IMPLEMENTS_KEYWORD);
        KEYWORDS.put("import", CFMLTokenTypes.IMPORT_KEYWORD);
        KEYWORDS.put("include", CFMLTokenTypes.INCLUDE_KEYWORD);
        KEYWORDS.put("property", CFMLTokenTypes.PROPERTY_KEYWORD);
        KEYWORDS.put("param", CFMLTokenTypes.PARAM_KEYWORD);
        KEYWORDS.put("new", CFMLTokenTypes.NEW_KEYWORD);
        KEYWORDS.put("if", CFMLTokenTypes.IF_KEYWORD);
        KEYWORDS.put("else", CFMLTokenTypes.ELSE_KEYWORD);
        KEYWORDS.put("elseif", CFMLTokenTypes.ELSEIF_KEYWORD);
        KEYWORDS.put("while", CFMLTokenTypes.WHILE_KEYWORD);
        KEYWORDS.put("do", CFMLTokenTypes.DO_KEYWORD);
        KEYWORDS.put("for", CFMLTokenTypes.FOR_KEYWORD);
        KEYWORDS.put("in", CFMLTokenTypes.IN_KEYWORD);
        KEYWORDS.put("switch", CFMLTokenTypes.SWITCH_KEYWORD);
        KEYWORDS.put("case", CFMLTokenTypes.CASE_KEYWORD);
        KEYWORDS.put("default", CFMLTokenTypes.DEFAULT_KEYWORD);
        KEYWORDS.put("break", CFMLTokenTypes.BREAK_KEYWORD);
        KEYWORDS.put("continue", CFMLTokenTypes.CONTINUE_KEYWORD);
        KEYWORDS.put("return", CFMLTokenTypes.RETURN_KEYWORD);
        KEYWORDS.put("try", CFMLTokenTypes.TRY_KEYWORD);
        KEYWORDS.put("catch", CFMLTokenTypes.CATCH_KEYWORD);
        KEYWORDS.put("finally", CFMLTokenTypes.FINALLY_KEYWORD);
        KEYWORDS.put("throw", CFMLTokenTypes.THROW_KEYWORD);
        KEYWORDS.put("rethrow", CFMLTokenTypes.RETHROW_KEYWORD);
        KEYWORDS.put("abort", CFMLTokenTypes.ABORT_KEYWORD);
        KEYWORDS.put("lock", CFMLTokenTypes.LOCK_KEYWORD);
        KEYWORDS.put("transaction", CFMLTokenTypes.TRANSACTION_KEYWORD);
        KEYWORDS.put("thread", CFMLTokenTypes.THREAD_KEYWORD);
        KEYWORDS.put("public", CFMLTokenTypes.PUBLIC_KEYWORD);
        KEYWORDS.put("private", CFMLTokenTypes.PRIVATE_KEYWORD);
        KEYWORDS.put("package", CFMLTokenTypes.PACKAGE_KEYWORD);
        KEYWORDS.put("remote", CFMLTokenTypes.REMOTE_KEYWORD);
        KEYWORDS.put("static", CFMLTokenTypes.STATIC_KEYWORD);
        KEYWORDS.put("final", CFMLTokenTypes.FINAL_KEYWORD);
        KEYWORDS.put("abstract", CFMLTokenTypes.ABSTRACT_KEYWORD);
        KEYWORDS.put("required", CFMLTokenTypes.REQUIRED_KEYWORD);
        KEYWORDS.put("true", CFMLTokenTypes.TRUE_KEYWORD);
        KEYWORDS.put("false", CFMLTokenTypes.FALSE_KEYWORD);
        KEYWORDS.put("null", CFMLTokenTypes.NULL_KEYWORD);
        KEYWORDS.put("yes", CFMLTokenTypes.YES_KEYWORD);
        KEYWORDS.put("no", CFMLTokenTypes.NO_KEYWORD);

        // Types
        TYPES.put("any", CFMLTokenTypes.TYPE_ANY);
        TYPES.put("array", CFMLTokenTypes.TYPE_ARRAY);
        TYPES.put("binary", CFMLTokenTypes.TYPE_BINARY);
        TYPES.put("boolean", CFMLTokenTypes.TYPE_BOOLEAN);
        TYPES.put("bool", CFMLTokenTypes.TYPE_BOOLEAN);
        TYPES.put("date", CFMLTokenTypes.TYPE_DATE);
        TYPES.put("datetime", CFMLTokenTypes.TYPE_DATE);
        TYPES.put("guid", CFMLTokenTypes.TYPE_GUID);
        TYPES.put("numeric", CFMLTokenTypes.TYPE_NUMERIC);
        TYPES.put("number", CFMLTokenTypes.TYPE_NUMERIC);
        TYPES.put("query", CFMLTokenTypes.TYPE_QUERY);
        TYPES.put("string", CFMLTokenTypes.TYPE_STRING);
        TYPES.put("struct", CFMLTokenTypes.TYPE_STRUCT);
        TYPES.put("uuid", CFMLTokenTypes.TYPE_UUID);
        TYPES.put("void", CFMLTokenTypes.TYPE_VOID);
        TYPES.put("xml", CFMLTokenTypes.TYPE_XML);
        TYPES.put("variablename", CFMLTokenTypes.TYPE_VARIABLENAME);

        // Scopes
        SCOPES.put("application", CFMLTokenTypes.SCOPE_APPLICATION);
        SCOPES.put("arguments", CFMLTokenTypes.SCOPE_ARGUMENTS);
        SCOPES.put("attributes", CFMLTokenTypes.SCOPE_ATTRIBUTES);
        SCOPES.put("caller", CFMLTokenTypes.SCOPE_CALLER);
        SCOPES.put("cgi", CFMLTokenTypes.SCOPE_CGI);
        SCOPES.put("client", CFMLTokenTypes.SCOPE_CLIENT);
        SCOPES.put("cookie", CFMLTokenTypes.SCOPE_COOKIE);
        SCOPES.put("flash", CFMLTokenTypes.SCOPE_FLASH);
        SCOPES.put("form", CFMLTokenTypes.SCOPE_FORM);
        SCOPES.put("request", CFMLTokenTypes.SCOPE_REQUEST);
        SCOPES.put("server", CFMLTokenTypes.SCOPE_SERVER);
        SCOPES.put("session", CFMLTokenTypes.SCOPE_SESSION);
        SCOPES.put("this", CFMLTokenTypes.SCOPE_THIS);
        SCOPES.put("thistag", CFMLTokenTypes.SCOPE_THISTAG);
        SCOPES.put("url", CFMLTokenTypes.SCOPE_URL);
        SCOPES.put("variables", CFMLTokenTypes.SCOPE_VARIABLES);
        SCOPES.put("self", CFMLTokenTypes.SCOPE_SELF);
        SCOPES.put("super", CFMLTokenTypes.SCOPE_SUPER);

        // Word Operators
        WORD_OPERATORS.put("and", CFMLTokenTypes.OP_AND);
        WORD_OPERATORS.put("or", CFMLTokenTypes.OP_OR);
        WORD_OPERATORS.put("not", CFMLTokenTypes.OP_NOT);
        WORD_OPERATORS.put("eq", CFMLTokenTypes.OP_EQ);
        WORD_OPERATORS.put("neq", CFMLTokenTypes.OP_NEQ);
        WORD_OPERATORS.put("lt", CFMLTokenTypes.OP_LT);
        WORD_OPERATORS.put("lte", CFMLTokenTypes.OP_LTE);
        WORD_OPERATORS.put("gt", CFMLTokenTypes.OP_GT);
        WORD_OPERATORS.put("gte", CFMLTokenTypes.OP_GTE);
        WORD_OPERATORS.put("is", CFMLTokenTypes.OP_IS);
        WORD_OPERATORS.put("equal", CFMLTokenTypes.OP_EQUAL);
        WORD_OPERATORS.put("contains", CFMLTokenTypes.OP_CONTAINS);
        WORD_OPERATORS.put("xor", CFMLTokenTypes.OP_XOR);
        WORD_OPERATORS.put("eqv", CFMLTokenTypes.OP_EQV);
        WORD_OPERATORS.put("imp", CFMLTokenTypes.OP_IMP);
        WORD_OPERATORS.put("mod", CFMLTokenTypes.OP_MOD);
        WORD_OPERATORS.put("to", CFMLTokenTypes.OP_TO);
    }

    private CharSequence buffer;
    private int bufferEnd;
    private int tokenStart;
    private int tokenEnd;
    private int state = STATE_DEFAULT;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState)
    {
        this.buffer = buffer;
        this.bufferEnd = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.state = initialState;
        this.tokenType = null;
        advance();
    }

    @Override
    public int getState()
    {
        return state;
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
        if (c <= ' ' && (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') || Character.isWhitespace(c))
        {
            i++;
            while (i < bufferEnd)
            {
                char ch = buffer.charAt(i);
                if ((ch <= ' ' && (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f')) || Character.isWhitespace(ch))
                {
                    i++;
                }
                else
                {
                    break;
                }
            }
            tokenEnd = i;
            tokenType = TokenType.WHITE_SPACE;
            return;
        }

        // 2. CFML Tag comment: <!--- ... ---> (supports nesting)
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
            tokenType = CFMLTokenTypes.TAG_COMMENT;
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
            tokenType = CFMLTokenTypes.LINE_COMMENT;
            return;
        }

        // 4. Script block comment: /* ... */
        if (c == '/' && i + 1 < bufferEnd && buffer.charAt(i + 1) == '*')
        {
            boolean isDoc = (i + 2 < bufferEnd && buffer.charAt(i + 2) == '*');
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
            tokenType = isDoc ? CFMLTokenTypes.DOC_COMMENT : CFMLTokenTypes.BLOCK_COMMENT;
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
            tokenType = CFMLTokenTypes.LINE_COMMENT;
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
            tokenType = (quote == '"') ? CFMLTokenTypes.DOUBLE_QUOTED_STRING : CFMLTokenTypes.SINGLE_QUOTED_STRING;
            if (state == STATE_AFTER_TAG_START)
            {
                state = STATE_INSIDE_TAG;
            }
            return;
        }

        // 7. Hash symbols: ## (escaped) or # (expression delimiter)
        if (c == '#')
        {
            if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '#')
            {
                tokenEnd = i + 2;
                tokenType = CFMLTokenTypes.ESCAPED_HASH;
            }
            else
            {
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.HASH;
            }
            return;
        }

        // 8. Tag starts: </ or <
        if (c == '<')
        {
            if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '/')
            {
                if (i + 2 < bufferEnd && (Character.isLetter(buffer.charAt(i + 2)) || buffer.charAt(i + 2) == '_' || buffer.charAt(i + 2) == ':'))
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.TAG_CLOSE_START;
                    state = STATE_AFTER_TAG_START;
                    return;
                }
            }
            else if (i + 1 < bufferEnd && (Character.isLetter(buffer.charAt(i + 1)) || buffer.charAt(i + 1) == '_' || buffer.charAt(i + 1) == ':'))
            {
                // If directly preceded by an identifier, number, or closing bracket without whitespace, '<' is a less-than operator (e.g. i<len, 5<x, (a)<b)
                boolean isDirectlyPrecededByIdentifier = (tokenStart > 0) &&
                        (Character.isJavaIdentifierPart(buffer.charAt(tokenStart - 1)) ||
                         buffer.charAt(tokenStart - 1) == ')' ||
                         buffer.charAt(tokenStart - 1) == ']');
                if (!isDirectlyPrecededByIdentifier)
                {
                    tokenEnd = i + 1;
                    tokenType = CFMLTokenTypes.TAG_OPEN_START;
                    state = STATE_AFTER_TAG_START;
                    return;
                }
            }
        }

        // 9. Tag ends: /> or >
        if (state != STATE_DEFAULT)
        {
            if (c == '/' && i + 1 < bufferEnd && buffer.charAt(i + 1) == '>')
            {
                tokenEnd = i + 2;
                tokenType = CFMLTokenTypes.TAG_EMPTY_END;
                state = STATE_DEFAULT;
                return;
            }
            if (c == '>')
            {
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.TAG_END;
                state = STATE_DEFAULT;
                return;
            }
        }

        // 10. Numbers: Hex (0x...) or decimal / float
        if (Character.isDigit(c) || (c == '.' && i + 1 < bufferEnd && Character.isDigit(buffer.charAt(i + 1))))
        {
            if (c == '0' && i + 1 < bufferEnd && (buffer.charAt(i + 1) == 'x' || buffer.charAt(i + 1) == 'X'))
            {
                i += 2;
                while (i < bufferEnd && isHexDigit(buffer.charAt(i)))
                {
                    i++;
                }
                tokenEnd = i;
                tokenType = CFMLTokenTypes.HEX_LITERAL;
                if (state == STATE_AFTER_TAG_START) state = STATE_INSIDE_TAG;
                return;
            }

            boolean isFloat = (c == '.');
            while (i < bufferEnd && Character.isDigit(buffer.charAt(i)))
            {
                i++;
            }
            if (!isFloat && i < bufferEnd && buffer.charAt(i) == '.' && i + 1 < bufferEnd && Character.isDigit(buffer.charAt(i + 1)))
            {
                isFloat = true;
                i++;
                while (i < bufferEnd && Character.isDigit(buffer.charAt(i)))
                {
                    i++;
                }
            }
            if (i < bufferEnd && (buffer.charAt(i) == 'e' || buffer.charAt(i) == 'E'))
            {
                isFloat = true;
                int expIdx = i + 1;
                if (expIdx < bufferEnd && (buffer.charAt(expIdx) == '+' || buffer.charAt(expIdx) == '-'))
                {
                    expIdx++;
                }
                if (expIdx < bufferEnd && Character.isDigit(buffer.charAt(expIdx)))
                {
                    i = expIdx + 1;
                    while (i < bufferEnd && Character.isDigit(buffer.charAt(i)))
                    {
                        i++;
                    }
                }
            }
            tokenEnd = i;
            tokenType = isFloat ? CFMLTokenTypes.FLOAT_LITERAL : CFMLTokenTypes.INTEGER_LITERAL;
            if (state == STATE_AFTER_TAG_START) state = STATE_INSIDE_TAG;
            return;
        }

        // 11. Identifiers, Keywords, Types, Scopes, Tag Names, Attribute Names
        if (Character.isJavaIdentifierStart(c) || c == '$')
        {
            i++;
            while (i < bufferEnd)
            {
                char ch = buffer.charAt(i);
                if (Character.isJavaIdentifierPart(ch) || ch == '$' || (state != STATE_DEFAULT && (ch == '-' || ch == ':')))
                {
                    i++;
                }
                else
                {
                    break;
                }
            }
            tokenEnd = i;

            if (state == STATE_AFTER_TAG_START)
            {
                tokenType = CFMLTokenTypes.TAG_NAME;
                state = STATE_INSIDE_TAG;
                return;
            }

            if (state == STATE_INSIDE_TAG)
            {
                tokenType = CFMLTokenTypes.ATTRIBUTE_NAME;
                return;
            }

            tokenType = lookupKeywordOrIdentifier(buffer, tokenStart, tokenEnd);
            return;
        }

        // 12. Delimiters and operators (zero heap allocations)
        switch (c)
        {
            case '(': tokenEnd = i + 1; tokenType = CFMLTokenTypes.LPAREN; return;
            case ')': tokenEnd = i + 1; tokenType = CFMLTokenTypes.RPAREN; return;
            case '{': tokenEnd = i + 1; tokenType = CFMLTokenTypes.LBRACE; return;
            case '}': tokenEnd = i + 1; tokenType = CFMLTokenTypes.RBRACE; return;
            case '[': tokenEnd = i + 1; tokenType = CFMLTokenTypes.LBRACKET; return;
            case ']': tokenEnd = i + 1; tokenType = CFMLTokenTypes.RBRACKET; return;
            case ';': tokenEnd = i + 1; tokenType = CFMLTokenTypes.SEMICOLON; return;
            case ',': tokenEnd = i + 1; tokenType = CFMLTokenTypes.COMMA; return;
            case '.': tokenEnd = i + 1; tokenType = CFMLTokenTypes.DOT; return;
            case ':': tokenEnd = i + 1; tokenType = CFMLTokenTypes.COLON; return;
            case '~': tokenEnd = i + 1; tokenType = CFMLTokenTypes.BIT_XOR; return;

            case '=':
                if (i + 2 < bufferEnd && buffer.charAt(i + 1) == '=' && buffer.charAt(i + 2) == '=')
                {
                    tokenEnd = i + 3;
                    tokenType = CFMLTokenTypes.EXACT_EQ;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.EQ_EQ;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '>')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.ARROW;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.ASSIGN;
                return;

            case '!':
                if (i + 2 < bufferEnd && buffer.charAt(i + 1) == '=' && buffer.charAt(i + 2) == '=')
                {
                    tokenEnd = i + 3;
                    tokenType = CFMLTokenTypes.EXACT_NOT_EQ;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.NOT_EQ;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.EXCL;
                return;

            case '<':
                if (i + 2 < bufferEnd && buffer.charAt(i + 1) == '=' && buffer.charAt(i + 2) == '>')
                {
                    tokenEnd = i + 3;
                    tokenType = CFMLTokenTypes.SPACESHIP;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.LESS_EQ;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '<')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.SHIFT_LEFT;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.LESS;
                return;

            case '>':
                if (i + 2 < bufferEnd && buffer.charAt(i + 1) == '>' && buffer.charAt(i + 2) == '>')
                {
                    tokenEnd = i + 3;
                    tokenType = CFMLTokenTypes.SHIFT_RIGHT_UNSIGNED;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '>')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.SHIFT_RIGHT;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.GREATER_EQ;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.GREATER;
                return;

            case '+':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '+')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.PLUS_PLUS;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.PLUS_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.PLUS;
                return;

            case '-':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '-')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.MINUS_MINUS;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.MINUS_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.MINUS;
                return;

            case '*':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.MUL_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.MULTIPLY;
                return;

            case '/':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.DIV_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.DIVIDE;
                return;

            case '%':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.MOD_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.MODULO;
                return;

            case '^':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.POW_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.POWER;
                return;

            case '&':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '&')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.AND_AND;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '=')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.CONCAT_ASSIGN;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.BIT_AND;
                return;

            case '|':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '|')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.OR_OR;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.BIT_OR;
                return;

            case '?':
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == ':')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.ELVIS;
                    return;
                }
                if (i + 1 < bufferEnd && buffer.charAt(i + 1) == '.')
                {
                    tokenEnd = i + 2;
                    tokenType = CFMLTokenTypes.SAFE_NAV;
                    return;
                }
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.QUESTION;
                return;
        }

        // 13. Fallback character / text
        tokenEnd = tokenStart + 1;
        tokenType = CFMLTokenTypes.TEXT;
    }

    private static IElementType lookupKeywordOrIdentifier(CharSequence buf, int start, int end)
    {
        int len = end - start;
        if (len < 2 || len > 12)
        {
            return CFMLTokenTypes.IDENTIFIER;
        }

        char firstChar = buf.charAt(start);
        char c0 = (firstChar >= 'A' && firstChar <= 'Z') ? (char)(firstChar + 32) : firstChar;

        switch (len)
        {
            case 2:
                switch (c0)
                {
                    case 'i':
                        if (matches(buf, start, "if")) return CFMLTokenTypes.IF_KEYWORD;
                        if (matches(buf, start, "in")) return CFMLTokenTypes.IN_KEYWORD;
                        if (matches(buf, start, "is")) return CFMLTokenTypes.OP_IS;
                        break;
                    case 'd':
                        if (matches(buf, start, "do")) return CFMLTokenTypes.DO_KEYWORD;
                        break;
                    case 'n':
                        if (matches(buf, start, "no")) return CFMLTokenTypes.NO_KEYWORD;
                        break;
                    case 'o':
                        if (matches(buf, start, "or")) return CFMLTokenTypes.OP_OR;
                        break;
                    case 'e':
                        if (matches(buf, start, "eq")) return CFMLTokenTypes.OP_EQ;
                        break;
                    case 'g':
                        if (matches(buf, start, "gt")) return CFMLTokenTypes.OP_GT;
                        break;
                    case 'l':
                        if (matches(buf, start, "lt")) return CFMLTokenTypes.OP_LT;
                        break;
                    case 't':
                        if (matches(buf, start, "to")) return CFMLTokenTypes.OP_TO;
                        break;
                }
                break;

            case 3:
                switch (c0)
                {
                    case 'v':
                        if (matches(buf, start, "var")) return CFMLTokenTypes.VAR_KEYWORD;
                        break;
                    case 'n':
                        if (matches(buf, start, "new")) return CFMLTokenTypes.NEW_KEYWORD;
                        if (matches(buf, start, "not")) return CFMLTokenTypes.OP_NOT;
                        if (matches(buf, start, "neq")) return CFMLTokenTypes.OP_NEQ;
                        break;
                    case 'f':
                        if (matches(buf, start, "for")) return CFMLTokenTypes.FOR_KEYWORD;
                        break;
                    case 't':
                        if (matches(buf, start, "try")) return CFMLTokenTypes.TRY_KEYWORD;
                        break;
                    case 'y':
                        if (matches(buf, start, "yes")) return CFMLTokenTypes.YES_KEYWORD;
                        break;
                    case 'a':
                        if (matches(buf, start, "and")) return CFMLTokenTypes.OP_AND;
                        if (matches(buf, start, "any")) return CFMLTokenTypes.TYPE_ANY;
                        break;
                    case 'x':
                        if (matches(buf, start, "xml")) return CFMLTokenTypes.TYPE_XML;
                        if (matches(buf, start, "xor")) return CFMLTokenTypes.OP_XOR;
                        break;
                    case 'c':
                        if (matches(buf, start, "cgi")) return CFMLTokenTypes.SCOPE_CGI;
                        break;
                    case 'u':
                        if (matches(buf, start, "url")) return CFMLTokenTypes.SCOPE_URL;
                        break;
                    case 'l':
                        if (matches(buf, start, "lte")) return CFMLTokenTypes.OP_LTE;
                        break;
                    case 'g':
                        if (matches(buf, start, "gte")) return CFMLTokenTypes.OP_GTE;
                        break;
                    case 'm':
                        if (matches(buf, start, "mod")) return CFMLTokenTypes.OP_MOD;
                        break;
                    case 'e':
                        if (matches(buf, start, "eqv")) return CFMLTokenTypes.OP_EQV;
                        break;
                    case 'i':
                        if (matches(buf, start, "imp")) return CFMLTokenTypes.OP_IMP;
                        break;
                }
                break;

            case 4:
                switch (c0)
                {
                    case 'e':
                        if (matches(buf, start, "else")) return CFMLTokenTypes.ELSE_KEYWORD;
                        break;
                    case 'c':
                        if (matches(buf, start, "case")) return CFMLTokenTypes.CASE_KEYWORD;
                        break;
                    case 'l':
                        if (matches(buf, start, "lock")) return CFMLTokenTypes.LOCK_KEYWORD;
                        if (matches(buf, start, "less")) return CFMLTokenTypes.OP_LESS_THAN;
                        break;
                    case 't':
                        if (matches(buf, start, "true")) return CFMLTokenTypes.TRUE_KEYWORD;
                        if (matches(buf, start, "this")) return CFMLTokenTypes.SCOPE_THIS;
                        if (matches(buf, start, "than")) return CFMLTokenTypes.OP_GREATER_THAN;
                        break;
                    case 'n':
                        if (matches(buf, start, "null")) return CFMLTokenTypes.NULL_KEYWORD;
                        break;
                    case 'd':
                        if (matches(buf, start, "date")) return CFMLTokenTypes.TYPE_DATE;
                        if (matches(buf, start, "does")) return CFMLTokenTypes.OP_DOES_NOT_CONTAIN;
                        break;
                    case 'g':
                        if (matches(buf, start, "guid")) return CFMLTokenTypes.TYPE_GUID;
                        break;
                    case 'v':
                        if (matches(buf, start, "void")) return CFMLTokenTypes.TYPE_VOID;
                        break;
                    case 'u':
                        if (matches(buf, start, "uuid")) return CFMLTokenTypes.TYPE_UUID;
                        break;
                    case 'b':
                        if (matches(buf, start, "bool")) return CFMLTokenTypes.TYPE_BOOLEAN;
                        break;
                    case 'f':
                        if (matches(buf, start, "form")) return CFMLTokenTypes.SCOPE_FORM;
                        break;
                    case 's':
                        if (matches(buf, start, "self")) return CFMLTokenTypes.SCOPE_SELF;
                        break;
                }
                break;

            case 5:
                switch (c0)
                {
                    case 'l':
                        if (matches(buf, start, "local")) return CFMLTokenTypes.LOCAL_KEYWORD;
                        break;
                    case 'p':
                        if (matches(buf, start, "param")) return CFMLTokenTypes.PARAM_KEYWORD;
                        break;
                    case 'w':
                        if (matches(buf, start, "while")) return CFMLTokenTypes.WHILE_KEYWORD;
                        break;
                    case 'b':
                        if (matches(buf, start, "break")) return CFMLTokenTypes.BREAK_KEYWORD;
                        break;
                    case 't':
                        if (matches(buf, start, "throw")) return CFMLTokenTypes.THROW_KEYWORD;
                        break;
                    case 'a':
                        if (matches(buf, start, "abort")) return CFMLTokenTypes.ABORT_KEYWORD;
                        if (matches(buf, start, "array")) return CFMLTokenTypes.TYPE_ARRAY;
                        break;
                    case 'f':
                        if (matches(buf, start, "final")) return CFMLTokenTypes.FINAL_KEYWORD;
                        if (matches(buf, start, "false")) return CFMLTokenTypes.FALSE_KEYWORD;
                        if (matches(buf, start, "flash")) return CFMLTokenTypes.SCOPE_FLASH;
                        break;
                    case 'c':
                        if (matches(buf, start, "catch")) return CFMLTokenTypes.CATCH_KEYWORD;
                        break;
                    case 'q':
                        if (matches(buf, start, "query")) return CFMLTokenTypes.TYPE_QUERY;
                        break;
                    case 's':
                        if (matches(buf, start, "super")) return CFMLTokenTypes.SCOPE_SUPER;
                        break;
                    case 'e':
                        if (matches(buf, start, "equal")) return CFMLTokenTypes.OP_EQUAL;
                        break;
                }
                break;

            case 6:
                switch (c0)
                {
                    case 'e':
                        if (matches(buf, start, "elseif")) return CFMLTokenTypes.ELSEIF_KEYWORD;
                        break;
                    case 's':
                        if (matches(buf, start, "switch")) return CFMLTokenTypes.SWITCH_KEYWORD;
                        if (matches(buf, start, "static")) return CFMLTokenTypes.STATIC_KEYWORD;
                        if (matches(buf, start, "string")) return CFMLTokenTypes.TYPE_STRING;
                        if (matches(buf, start, "struct")) return CFMLTokenTypes.TYPE_STRUCT;
                        if (matches(buf, start, "server")) return CFMLTokenTypes.SCOPE_SERVER;
                        break;
                    case 'r':
                        if (matches(buf, start, "return")) return CFMLTokenTypes.RETURN_KEYWORD;
                        if (matches(buf, start, "remote")) return CFMLTokenTypes.REMOTE_KEYWORD;
                        break;
                    case 't':
                        if (matches(buf, start, "thread")) return CFMLTokenTypes.THREAD_KEYWORD;
                        break;
                    case 'p':
                        if (matches(buf, start, "public")) return CFMLTokenTypes.PUBLIC_KEYWORD;
                        break;
                    case 'i':
                        if (matches(buf, start, "import")) return CFMLTokenTypes.IMPORT_KEYWORD;
                        break;
                    case 'n':
                        if (matches(buf, start, "number")) return CFMLTokenTypes.TYPE_NUMERIC;
                        break;
                    case 'b':
                        if (matches(buf, start, "binary")) return CFMLTokenTypes.TYPE_BINARY;
                        break;
                    case 'c':
                        if (matches(buf, start, "client")) return CFMLTokenTypes.SCOPE_CLIENT;
                        if (matches(buf, start, "cookie")) return CFMLTokenTypes.SCOPE_COOKIE;
                        if (matches(buf, start, "caller")) return CFMLTokenTypes.SCOPE_CALLER;
                        break;
                }
                break;

            case 7:
                switch (c0)
                {
                    case 'd':
                        if (matches(buf, start, "default")) return CFMLTokenTypes.DEFAULT_KEYWORD;
                        break;
                    case 'f':
                        if (matches(buf, start, "finally")) return CFMLTokenTypes.FINALLY_KEYWORD;
                        break;
                    case 'r':
                        if (matches(buf, start, "rethrow")) return CFMLTokenTypes.RETHROW_KEYWORD;
                        if (matches(buf, start, "request")) return CFMLTokenTypes.SCOPE_REQUEST;
                        break;
                    case 'p':
                        if (matches(buf, start, "package")) return CFMLTokenTypes.PACKAGE_KEYWORD;
                        if (matches(buf, start, "private")) return CFMLTokenTypes.PRIVATE_KEYWORD;
                        break;
                    case 'e':
                        if (matches(buf, start, "extends")) return CFMLTokenTypes.EXTENDS_KEYWORD;
                        break;
                    case 'i':
                        if (matches(buf, start, "include")) return CFMLTokenTypes.INCLUDE_KEYWORD;
                        break;
                    case 'n':
                        if (matches(buf, start, "numeric")) return CFMLTokenTypes.TYPE_NUMERIC;
                        break;
                    case 'b':
                        if (matches(buf, start, "boolean")) return CFMLTokenTypes.TYPE_BOOLEAN;
                        break;
                    case 's':
                        if (matches(buf, start, "session")) return CFMLTokenTypes.SCOPE_SESSION;
                        break;
                    case 't':
                        if (matches(buf, start, "thistag")) return CFMLTokenTypes.SCOPE_THISTAG;
                        break;
                    case 'g':
                        if (matches(buf, start, "greater")) return CFMLTokenTypes.OP_GREATER_THAN;
                        break;
                }
                break;

            case 8:
                switch (c0)
                {
                    case 'c':
                        if (matches(buf, start, "continue")) return CFMLTokenTypes.CONTINUE_KEYWORD;
                        if (matches(buf, start, "contains")) return CFMLTokenTypes.OP_CONTAINS;
                        break;
                    case 'a':
                        if (matches(buf, start, "abstract")) return CFMLTokenTypes.ABSTRACT_KEYWORD;
                        break;
                    case 'r':
                        if (matches(buf, start, "required")) return CFMLTokenTypes.REQUIRED_KEYWORD;
                        break;
                    case 'f':
                        if (matches(buf, start, "function")) return CFMLTokenTypes.FUNCTION_KEYWORD;
                        break;
                    case 'p':
                        if (matches(buf, start, "property")) return CFMLTokenTypes.PROPERTY_KEYWORD;
                        break;
                    case 'd':
                        if (matches(buf, start, "datetime")) return CFMLTokenTypes.TYPE_DATE;
                        break;
                }
                break;

            case 9:
                switch (c0)
                {
                    case 'c':
                        if (matches(buf, start, "component")) return CFMLTokenTypes.COMPONENT_KEYWORD;
                        break;
                    case 'i':
                        if (matches(buf, start, "interface")) return CFMLTokenTypes.INTERFACE_KEYWORD;
                        break;
                    case 'a':
                        if (matches(buf, start, "arguments")) return CFMLTokenTypes.SCOPE_ARGUMENTS;
                        break;
                    case 'v':
                        if (matches(buf, start, "variables")) return CFMLTokenTypes.SCOPE_VARIABLES;
                        break;
                }
                break;

            case 10:
                switch (c0)
                {
                    case 'i':
                        if (matches(buf, start, "implements")) return CFMLTokenTypes.IMPLEMENTS_KEYWORD;
                        break;
                    case 'a':
                        if (matches(buf, start, "attributes")) return CFMLTokenTypes.SCOPE_ATTRIBUTES;
                        break;
                }
                break;

            case 11:
                switch (c0)
                {
                    case 't':
                        if (matches(buf, start, "transaction")) return CFMLTokenTypes.TRANSACTION_KEYWORD;
                        break;
                    case 'a':
                        if (matches(buf, start, "application")) return CFMLTokenTypes.SCOPE_APPLICATION;
                        break;
                }
                break;

            case 12:
                if (matches(buf, start, "variablename")) return CFMLTokenTypes.TYPE_VARIABLENAME;
                break;
        }

        return CFMLTokenTypes.IDENTIFIER;
    }

    private static boolean matches(CharSequence buf, int start, String target)
    {
        int len = target.length();
        for (int k = 0; k < len; k++)
        {
            char c1 = buf.charAt(start + k);
            char c2 = target.charAt(k);
            if (c1 != c2 && ((c1 >= 'A' && c1 <= 'Z') ? (char)(c1 + 32) : c1) != c2)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char c)
    {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
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