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
                tokenEnd = i + 1;
                tokenType = CFMLTokenTypes.TAG_OPEN_START;
                state = STATE_AFTER_TAG_START;
                return;
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

        // 11. Multi-character operators
        if (i + 2 < bufferEnd)
        {
            String three = buffer.subSequence(i, i + 3).toString();
            if ("===".equals(three)) { tokenEnd = i + 3; tokenType = CFMLTokenTypes.EXACT_EQ; return; }
            if ("!==".equals(three)) { tokenEnd = i + 3; tokenType = CFMLTokenTypes.EXACT_NOT_EQ; return; }
            if ("<=>".equals(three)) { tokenEnd = i + 3; tokenType = CFMLTokenTypes.SPACESHIP; return; }
            if (">>>".equals(three)) { tokenEnd = i + 3; tokenType = CFMLTokenTypes.SHIFT_RIGHT_UNSIGNED; return; }
        }

        if (i + 1 < bufferEnd)
        {
            String two = buffer.subSequence(i, i + 2).toString();
            switch (two)
            {
                case "==": tokenEnd = i + 2; tokenType = CFMLTokenTypes.EQ_EQ; return;
                case "!=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.NOT_EQ; return;
                case "<=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.LESS_EQ; return;
                case ">=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.GREATER_EQ; return;
                case "&&": tokenEnd = i + 2; tokenType = CFMLTokenTypes.AND_AND; return;
                case "||": tokenEnd = i + 2; tokenType = CFMLTokenTypes.OR_OR; return;
                case "++": tokenEnd = i + 2; tokenType = CFMLTokenTypes.PLUS_PLUS; return;
                case "--": tokenEnd = i + 2; tokenType = CFMLTokenTypes.MINUS_MINUS; return;
                case "+=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.PLUS_ASSIGN; return;
                case "-=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.MINUS_ASSIGN; return;
                case "*=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.MUL_ASSIGN; return;
                case "/=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.DIV_ASSIGN; return;
                case "%=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.MOD_ASSIGN; return;
                case "^=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.POW_ASSIGN; return;
                case "&=": tokenEnd = i + 2; tokenType = CFMLTokenTypes.CONCAT_ASSIGN; return;
                case "=>": tokenEnd = i + 2; tokenType = CFMLTokenTypes.ARROW; return;
                case "?:": tokenEnd = i + 2; tokenType = CFMLTokenTypes.ELVIS; return;
                case "?.": tokenEnd = i + 2; tokenType = CFMLTokenTypes.SAFE_NAV; return;
                case "<<": tokenEnd = i + 2; tokenType = CFMLTokenTypes.SHIFT_LEFT; return;
                case ">>": tokenEnd = i + 2; tokenType = CFMLTokenTypes.SHIFT_RIGHT; return;
            }
        }

        // 12. Single character delimiters / operators
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
            case '?': tokenEnd = i + 1; tokenType = CFMLTokenTypes.QUESTION; return;
            case '=': tokenEnd = i + 1; tokenType = CFMLTokenTypes.ASSIGN; return;
            case '+': tokenEnd = i + 1; tokenType = CFMLTokenTypes.PLUS; return;
            case '-': tokenEnd = i + 1; tokenType = CFMLTokenTypes.MINUS; return;
            case '*': tokenEnd = i + 1; tokenType = CFMLTokenTypes.MULTIPLY; return;
            case '/': tokenEnd = i + 1; tokenType = CFMLTokenTypes.DIVIDE; return;
            case '%': tokenEnd = i + 1; tokenType = CFMLTokenTypes.MODULO; return;
            case '^': tokenEnd = i + 1; tokenType = CFMLTokenTypes.POWER; return;
            case '!': tokenEnd = i + 1; tokenType = CFMLTokenTypes.EXCL; return;
            case '&': tokenEnd = i + 1; tokenType = CFMLTokenTypes.BIT_AND; return;
            case '|': tokenEnd = i + 1; tokenType = CFMLTokenTypes.BIT_OR; return;
            case '~': tokenEnd = i + 1; tokenType = CFMLTokenTypes.BIT_XOR; return;
            case '<': tokenEnd = i + 1; tokenType = CFMLTokenTypes.LESS; return;
            case '>': tokenEnd = i + 1; tokenType = CFMLTokenTypes.GREATER; return;
        }

        // 13. Identifiers, Keywords, Types, Scopes, Tag Names, Attribute Names
        if (Character.isJavaIdentifierStart(c) || c == '$')
        {
            while (i < bufferEnd && (Character.isJavaIdentifierPart(buffer.charAt(i)) || buffer.charAt(i) == '$' || buffer.charAt(i) == '-'))
            {
                i++;
            }
            tokenEnd = i;
            String text = buffer.subSequence(tokenStart, tokenEnd).toString();
            String lower = text.toLowerCase(Locale.ROOT);

            if (state == STATE_AFTER_TAG_START)
            {
                tokenType = CFMLTokenTypes.TAG_NAME;
                state = STATE_INSIDE_TAG;
                return;
            }

            if (state == STATE_INSIDE_TAG)
            {
                // In tag attribute context, check if it's followed by '=' or a standard keyword
                tokenType = CFMLTokenTypes.ATTRIBUTE_NAME;
                return;
            }

            IElementType keywordType = KEYWORDS.get(lower);
            if (keywordType != null)
            {
                tokenType = keywordType;
                return;
            }

            IElementType wordOpType = WORD_OPERATORS.get(lower);
            if (wordOpType != null)
            {
                tokenType = wordOpType;
                return;
            }

            IElementType scopeType = SCOPES.get(lower);
            if (scopeType != null)
            {
                tokenType = scopeType;
                return;
            }

            IElementType typeType = TYPES.get(lower);
            if (typeType != null)
            {
                tokenType = typeType;
                return;
            }

            tokenType = CFMLTokenTypes.IDENTIFIER;
            return;
        }

        // 14. Fallback character
        tokenEnd = tokenStart + 1;
        tokenType = CFMLTokenTypes.TEXT;
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