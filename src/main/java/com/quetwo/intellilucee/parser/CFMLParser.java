package com.quetwo.intellilucee.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class CFMLParser implements PsiParser
{

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder)
    {
        PsiBuilder.Marker rootMarker = builder.mark();
        while (!builder.eof())
        {
            parseTopLevelItem(builder);
        }
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    private void parseTopLevelItem(PsiBuilder builder)
    {
        IElementType token = builder.getTokenType();
        if (token == null)
        {
            return;
        }

        if (token == CFMLTokenTypes.TAG_OPEN_START || token == CFMLTokenTypes.TAG_CLOSE_START)
        {
            parseTag(builder);
        }
        else if (token == CFMLTokenTypes.COMPONENT_KEYWORD)
        {
            parseComponentDeclaration(builder);
        }
        else if (token == CFMLTokenTypes.INTERFACE_KEYWORD)
        {
            parseInterfaceDeclaration(builder);
        }
        else if (isFunctionStart(builder))
        {
            parseFunctionDeclaration(builder);
        }
        else
        {
            parseStatement(builder);
        }
    }

    private boolean isFunctionStart(PsiBuilder builder)
    {
        IElementType t = builder.getTokenType();
        return t == CFMLTokenTypes.FUNCTION_KEYWORD || isModifier(t) || isType(t);
    }

    private boolean isModifier(IElementType t)
    {
        return t == CFMLTokenTypes.PUBLIC_KEYWORD ||
               t == CFMLTokenTypes.PRIVATE_KEYWORD ||
               t == CFMLTokenTypes.REMOTE_KEYWORD ||
               t == CFMLTokenTypes.PACKAGE_KEYWORD ||
               t == CFMLTokenTypes.STATIC_KEYWORD ||
               t == CFMLTokenTypes.FINAL_KEYWORD ||
               t == CFMLTokenTypes.ABSTRACT_KEYWORD ||
               t == CFMLTokenTypes.DEFAULT_KEYWORD;
    }

    private boolean isType(IElementType t)
    {
        return CFMLTokenTypes.TYPES.contains(t);
    }

    private void parseTag(PsiBuilder builder)
    {
        PsiBuilder.Marker tagMarker = builder.mark();
        builder.advanceLexer(); // < or </

        if (builder.getTokenType() == CFMLTokenTypes.TAG_NAME)
        {
            PsiBuilder.Marker nameMarker = builder.mark();
            builder.advanceLexer();
            nameMarker.done(CFMLElementTypes.TAG_NAME_ELEMENT);
        }

        while (!builder.eof() &&
               builder.getTokenType() != CFMLTokenTypes.TAG_END &&
               builder.getTokenType() != CFMLTokenTypes.TAG_EMPTY_END &&
               builder.getTokenType() != CFMLTokenTypes.TAG_OPEN_START &&
               builder.getTokenType() != CFMLTokenTypes.TAG_CLOSE_START)
        {
            int startOffset = builder.getCurrentOffset();
            if (builder.getTokenType() == CFMLTokenTypes.ATTRIBUTE_NAME || builder.getTokenType() == CFMLTokenTypes.IDENTIFIER)
            {
                PsiBuilder.Marker attrMarker = builder.mark();
                builder.advanceLexer();
                if (builder.getTokenType() == CFMLTokenTypes.ASSIGN)
                {
                    builder.advanceLexer();
                    if (builder.getTokenType() == CFMLTokenTypes.DOUBLE_QUOTED_STRING ||
                        builder.getTokenType() == CFMLTokenTypes.SINGLE_QUOTED_STRING ||
                        builder.getTokenType() == CFMLTokenTypes.ATTRIBUTE_VALUE ||
                        builder.getTokenType() == CFMLTokenTypes.IDENTIFIER ||
                        builder.getTokenType() == CFMLTokenTypes.INTEGER_LITERAL ||
                        builder.getTokenType() == CFMLTokenTypes.FLOAT_LITERAL)
                    {
                        PsiBuilder.Marker valMarker = builder.mark();
                        builder.advanceLexer();
                        valMarker.done(CFMLElementTypes.TAG_ATTRIBUTE_VALUE);
                    }
                }
                attrMarker.done(CFMLElementTypes.TAG_ATTRIBUTE);
            }
            else
            {
                builder.advanceLexer();
            }

            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.TAG_END || builder.getTokenType() == CFMLTokenTypes.TAG_EMPTY_END)
        {
            builder.advanceLexer();
        }

        tagMarker.done(CFMLElementTypes.TAG);
    }

    private void parseComponentDeclaration(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // component

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.LBRACE && builder.getTokenType() != CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
        {
            parseBlock(builder);
        }
        else if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.COMPONENT_DECLARATION);
    }

    private void parseInterfaceDeclaration(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // interface

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.LBRACE && builder.getTokenType() != CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
        {
            parseBlock(builder);
        }
        else if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.INTERFACE_DECLARATION);
    }

    private void parseFunctionDeclaration(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();

        while (!builder.eof() && (isModifier(builder.getTokenType()) || isType(builder.getTokenType())))
        {
            builder.advanceLexer();
        }

        if (builder.getTokenType() == CFMLTokenTypes.FUNCTION_KEYWORD)
        {
            builder.advanceLexer();
        }

        if (builder.getTokenType() == CFMLTokenTypes.IDENTIFIER || isType(builder.getTokenType()))
        {
            builder.advanceLexer();
        }

        if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
        {
            parseParameterList(builder);
        }

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.LBRACE && builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.TAG_OPEN_START)
        {
            int offset = builder.getCurrentOffset();
            builder.advanceLexer();
            if (builder.getCurrentOffset() == offset) break;
        }

        if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
        {
            parseBlock(builder);
        }
        else if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.FUNCTION_DECLARATION);
    }

    private void parseParameterList(PsiBuilder builder)
    {
        PsiBuilder.Marker listMarker = builder.mark();
        builder.advanceLexer(); // (

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RPAREN)
        {
            int startOffset = builder.getCurrentOffset();
            PsiBuilder.Marker paramMarker = builder.mark();

            if (builder.getTokenType() == CFMLTokenTypes.REQUIRED_KEYWORD)
            {
                builder.advanceLexer();
            }

            if (isType(builder.getTokenType()) || builder.getTokenType() == CFMLTokenTypes.IDENTIFIER)
            {
                builder.advanceLexer();
            }

            if (builder.getTokenType() == CFMLTokenTypes.IDENTIFIER)
            {
                builder.advanceLexer();
            }

            if (builder.getTokenType() == CFMLTokenTypes.ASSIGN)
            {
                builder.advanceLexer();
                parseExpression(builder, 0);
            }

            paramMarker.done(CFMLElementTypes.PARAMETER);

            if (builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            else if (builder.getTokenType() != CFMLTokenTypes.RPAREN)
            {
                if (builder.getCurrentOffset() == startOffset)
                {
                    builder.advanceLexer();
                }
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RPAREN)
        {
            builder.advanceLexer();
        }

        listMarker.done(CFMLElementTypes.PARAMETER_LIST);
    }

    private void parseBlock(PsiBuilder builder)
    {
        PsiBuilder.Marker blockMarker = builder.mark();
        builder.advanceLexer(); // {

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RBRACE)
        {
            int startOffset = builder.getCurrentOffset();
            parseStatement(builder);
            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RBRACE)
        {
            builder.advanceLexer();
        }

        blockMarker.done(CFMLElementTypes.BLOCK_STATEMENT);
    }

    private void parseStatement(PsiBuilder builder)
    {
        IElementType token = builder.getTokenType();
        if (token == null)
        {
            return;
        }

        if (token == CFMLTokenTypes.TAG_OPEN_START || token == CFMLTokenTypes.TAG_CLOSE_START)
        {
            parseTag(builder);
            return;
        }

        if (token == CFMLTokenTypes.LBRACE)
        {
            parseBlock(builder);
            return;
        }

        if (token == CFMLTokenTypes.VAR_KEYWORD || token == CFMLTokenTypes.LOCAL_KEYWORD)
        {
            parseVarStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.PROPERTY_KEYWORD)
        {
            parsePropertyStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.IF_KEYWORD)
        {
            parseIfStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.WHILE_KEYWORD)
        {
            parseWhileStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.DO_KEYWORD)
        {
            parseDoWhileStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.FOR_KEYWORD)
        {
            parseForStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.SWITCH_KEYWORD)
        {
            parseSwitchStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.TRY_KEYWORD)
        {
            parseTryStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.RETURN_KEYWORD)
        {
            parseReturnStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.THROW_KEYWORD)
        {
            parseThrowStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.RETHROW_KEYWORD)
        {
            parseRethrowStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.IMPORT_KEYWORD)
        {
            parseImportStatement(builder);
            return;
        }

        if (token == CFMLTokenTypes.INCLUDE_KEYWORD)
        {
            parseIncludeStatement(builder);
            return;
        }

        if (isFunctionStart(builder))
        {
            parseFunctionDeclaration(builder);
            return;
        }

        if (token == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
            return;
        }

        parseExpressionStatement(builder);
    }

    private void parseVarStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // var or local

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.RBRACE)
        {
            int startOffset = builder.getCurrentOffset();
            if (isType(builder.getTokenType()))
            {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == CFMLTokenTypes.IDENTIFIER)
            {
                builder.advanceLexer();
            }
            if (builder.getTokenType() == CFMLTokenTypes.ASSIGN)
            {
                builder.advanceLexer();
                parseExpression(builder, 0);
            }
            if (builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            else
            {
                if (builder.getCurrentOffset() == startOffset)
                {
                    builder.advanceLexer();
                }
                break;
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.VAR_DECLARATION_STATEMENT);
    }

    private void parsePropertyStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // property

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.RBRACE)
        {
            int startOffset = builder.getCurrentOffset();
            builder.advanceLexer();
            if (builder.getCurrentOffset() == startOffset) break;
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.PROPERTY_STATEMENT);
    }

    private void parseIfStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // if

        if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
        {
            parseParenthesized(builder);
        }

        parseStatement(builder);

        while (builder.getTokenType() == CFMLTokenTypes.ELSEIF_KEYWORD)
        {
            builder.advanceLexer();
            if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
            {
                parseParenthesized(builder);
            }
            parseStatement(builder);
        }

        if (builder.getTokenType() == CFMLTokenTypes.ELSE_KEYWORD)
        {
            builder.advanceLexer();
            parseStatement(builder);
        }

        marker.done(CFMLElementTypes.IF_STATEMENT);
    }

    private void parseWhileStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // while

        if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
        {
            parseParenthesized(builder);
        }

        parseStatement(builder);
        marker.done(CFMLElementTypes.WHILE_STATEMENT);
    }

    private void parseDoWhileStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // do

        parseStatement(builder);

        if (builder.getTokenType() == CFMLTokenTypes.WHILE_KEYWORD)
        {
            builder.advanceLexer();
            if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
            {
                parseParenthesized(builder);
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.DO_WHILE_STATEMENT);
    }

    private void parseForStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // for

        if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
        {
            parseParenthesized(builder);
        }

        parseStatement(builder);
        marker.done(CFMLElementTypes.FOR_STATEMENT);
    }

    private void parseSwitchStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // switch

        if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
        {
            parseParenthesized(builder);
        }

        if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
        {
            builder.advanceLexer();
            while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RBRACE)
            {
                int startOffset = builder.getCurrentOffset();
                if (builder.getTokenType() == CFMLTokenTypes.CASE_KEYWORD)
                {
                    PsiBuilder.Marker caseMarker = builder.mark();
                    builder.advanceLexer();
                    parseExpression(builder, 0);
                    if (builder.getTokenType() == CFMLTokenTypes.COLON)
                    {
                        builder.advanceLexer();
                    }
                    caseMarker.done(CFMLElementTypes.CASE_CLAUSE);
                }
                else if (builder.getTokenType() == CFMLTokenTypes.DEFAULT_KEYWORD)
                {
                    PsiBuilder.Marker defMarker = builder.mark();
                    builder.advanceLexer();
                    if (builder.getTokenType() == CFMLTokenTypes.COLON)
                    {
                        builder.advanceLexer();
                    }
                    defMarker.done(CFMLElementTypes.DEFAULT_CLAUSE);
                }
                else
                {
                    parseStatement(builder);
                }

                if (builder.getCurrentOffset() == startOffset)
                {
                    builder.advanceLexer();
                }
            }

            if (builder.getTokenType() == CFMLTokenTypes.RBRACE)
            {
                builder.advanceLexer();
            }
        }

        marker.done(CFMLElementTypes.SWITCH_STATEMENT);
    }

    private void parseTryStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // try

        if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
        {
            parseBlock(builder);
        }

        while (builder.getTokenType() == CFMLTokenTypes.CATCH_KEYWORD)
        {
            PsiBuilder.Marker catchMarker = builder.mark();
            builder.advanceLexer();
            if (builder.getTokenType() == CFMLTokenTypes.LPAREN)
            {
                parseParenthesized(builder);
            }
            if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
            {
                parseBlock(builder);
            }
            catchMarker.done(CFMLElementTypes.CATCH_CLAUSE);
        }

        if (builder.getTokenType() == CFMLTokenTypes.FINALLY_KEYWORD)
        {
            PsiBuilder.Marker finallyMarker = builder.mark();
            builder.advanceLexer();
            if (builder.getTokenType() == CFMLTokenTypes.LBRACE)
            {
                parseBlock(builder);
            }
            finallyMarker.done(CFMLElementTypes.FINALLY_CLAUSE);
        }

        marker.done(CFMLElementTypes.TRY_STATEMENT);
    }

    private void parseReturnStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // return

        if (builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.RBRACE && !builder.eof())
        {
            parseExpression(builder, 0);
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.RETURN_STATEMENT);
    }

    private void parseThrowStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // throw

        if (builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.RBRACE && !builder.eof())
        {
            parseExpression(builder, 0);
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.THROW_STATEMENT);
    }

    private void parseRethrowStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // rethrow

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.RETHROW_STATEMENT);
    }

    private void parseImportStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // import

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.SEMICOLON && builder.getTokenType() != CFMLTokenTypes.RBRACE)
        {
            int offset = builder.getCurrentOffset();
            builder.advanceLexer();
            if (builder.getCurrentOffset() == offset) break;
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.IMPORT_STATEMENT);
    }

    private void parseIncludeStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // include

        if (builder.getTokenType() != CFMLTokenTypes.SEMICOLON && !builder.eof())
        {
            parseExpression(builder, 0);
        }

        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }

        marker.done(CFMLElementTypes.INCLUDE_STATEMENT);
    }

    private void parseExpressionStatement(PsiBuilder builder)
    {
        PsiBuilder.Marker marker = builder.mark();
        boolean parsed = parseExpression(builder, 0);
        if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON)
        {
            builder.advanceLexer();
        }
        else if (!parsed && !builder.eof())
        {
            builder.advanceLexer();
        }
        marker.done(CFMLElementTypes.EXPRESSION_STATEMENT);
    }

    private void parseParenthesized(PsiBuilder builder)
    {
        builder.advanceLexer(); // (

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RPAREN)
        {
            int startOffset = builder.getCurrentOffset();
            parseExpression(builder, 0);
            if (builder.getTokenType() == CFMLTokenTypes.SEMICOLON || builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RPAREN)
        {
            builder.advanceLexer();
        }
    }

    private boolean parseExpression(PsiBuilder builder, int minPrecedence)
    {
        IElementType token = builder.getTokenType();
        if (token == null)
        {
            return false;
        }

        PsiBuilder.Marker marker = builder.mark();
        if (!parsePrimary(builder, marker))
        {
            marker.drop();
            return false;
        }

        marker = marker.precede();

        while (!builder.eof())
        {
            IElementType op = builder.getTokenType();
            if (op == null) break;

            int prec = getInfixPrecedence(op);
            if (prec < minPrecedence || prec == 0)
            {
                break;
            }

            if (op == CFMLTokenTypes.ASSIGN || isAssignmentOp(op))
            {
                builder.advanceLexer();
                parseExpression(builder, prec);
                marker.done(CFMLElementTypes.ASSIGNMENT_EXPRESSION);
                marker = marker.precede();
            }
            else if (op == CFMLTokenTypes.QUESTION || op == CFMLTokenTypes.ELVIS)
            {
                builder.advanceLexer();
                parseExpression(builder, 0);
                if (builder.getTokenType() == CFMLTokenTypes.COLON)
                {
                    builder.advanceLexer();
                    parseExpression(builder, 0);
                }
                marker.done(CFMLElementTypes.TERNARY_EXPRESSION);
                marker = marker.precede();
            }
            else if (op == CFMLTokenTypes.LPAREN)
            {
                parseArgumentList(builder);
                marker.done(CFMLElementTypes.CALL_EXPRESSION);
                marker = marker.precede();
            }
            else if (op == CFMLTokenTypes.LBRACKET)
            {
                parseIndexAccess(builder);
                marker.done(CFMLElementTypes.INDEX_EXPRESSION);
                marker = marker.precede();
            }
            else if (op == CFMLTokenTypes.DOT || op == CFMLTokenTypes.SAFE_NAV)
            {
                builder.advanceLexer();
                if (builder.getTokenType() == CFMLTokenTypes.IDENTIFIER || CFMLTokenTypes.KEYWORDS.contains(builder.getTokenType()) || CFMLTokenTypes.SCOPES.contains(builder.getTokenType()))
                {
                    builder.advanceLexer();
                }
                marker.done(CFMLElementTypes.REFERENCE_EXPRESSION);
                marker = marker.precede();
            }
            else if (op == CFMLTokenTypes.PLUS_PLUS || op == CFMLTokenTypes.MINUS_MINUS)
            {
                builder.advanceLexer();
                marker.done(CFMLElementTypes.POSTFIX_EXPRESSION);
                marker = marker.precede();
            }
            else
            {
                builder.advanceLexer();
                parseExpression(builder, prec + 1);
                marker.done(CFMLElementTypes.BINARY_EXPRESSION);
                marker = marker.precede();
            }
        }

        marker.drop();
        return true;
    }

    private boolean parsePrimary(PsiBuilder builder, PsiBuilder.Marker marker)
    {
        IElementType token = builder.getTokenType();
        if (token == null)
        {
            return false;
        }

        if (token == CFMLTokenTypes.EXCL || token == CFMLTokenTypes.OP_NOT || token == CFMLTokenTypes.PLUS || token == CFMLTokenTypes.MINUS || token == CFMLTokenTypes.PLUS_PLUS || token == CFMLTokenTypes.MINUS_MINUS || token == CFMLTokenTypes.BIT_NOT)
        {
            builder.advanceLexer();
            parseExpression(builder, 10);
            marker.done(CFMLElementTypes.UNARY_EXPRESSION);
            return true;
        }

        if (token == CFMLTokenTypes.NEW_KEYWORD)
        {
            builder.advanceLexer();
            parseExpression(builder, 9);
            marker.done(CFMLElementTypes.CALL_EXPRESSION);
            return true;
        }

        if (token == CFMLTokenTypes.LPAREN)
        {
            parseParenthesized(builder);
            marker.done(CFMLElementTypes.PARENTHESIZED_EXPRESSION);
            return true;
        }

        if (token == CFMLTokenTypes.LBRACKET)
        {
            parseArrayLiteral(builder);
            marker.done(CFMLElementTypes.ARRAY_LITERAL);
            return true;
        }

        if (token == CFMLTokenTypes.LBRACE)
        {
            parseStructLiteral(builder);
            marker.done(CFMLElementTypes.STRUCT_LITERAL);
            return true;
        }

        if (token == CFMLTokenTypes.HASH)
        {
            parseHashExpression(builder);
            marker.done(CFMLElementTypes.HASH_EXPRESSION);
            return true;
        }

        if (token == CFMLTokenTypes.DOUBLE_QUOTED_STRING || token == CFMLTokenTypes.SINGLE_QUOTED_STRING ||
            token == CFMLTokenTypes.INTEGER_LITERAL || token == CFMLTokenTypes.FLOAT_LITERAL || token == CFMLTokenTypes.HEX_LITERAL ||
            token == CFMLTokenTypes.TRUE_KEYWORD || token == CFMLTokenTypes.FALSE_KEYWORD || token == CFMLTokenTypes.NULL_KEYWORD ||
            token == CFMLTokenTypes.YES_KEYWORD || token == CFMLTokenTypes.NO_KEYWORD)
        {
            builder.advanceLexer();
            marker.done(CFMLElementTypes.LITERAL_EXPRESSION);
            return true;
        }

        if (token == CFMLTokenTypes.IDENTIFIER || CFMLTokenTypes.SCOPES.contains(token) || CFMLTokenTypes.KEYWORDS.contains(token))
        {
            builder.advanceLexer();
            marker.done(CFMLElementTypes.REFERENCE_EXPRESSION);
            return true;
        }

        return false;
    }

    private void parseHashExpression(PsiBuilder builder)
    {
        builder.advanceLexer(); // #

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.HASH)
        {
            int startOffset = builder.getCurrentOffset();
            parseExpression(builder, 0);
            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.HASH)
        {
            builder.advanceLexer();
        }
    }

    private void parseArgumentList(PsiBuilder builder)
    {
        PsiBuilder.Marker argListMarker = builder.mark();
        builder.advanceLexer(); // (

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RPAREN)
        {
            int startOffset = builder.getCurrentOffset();
            PsiBuilder.Marker argMarker = builder.mark();
            parseExpression(builder, 0);
            argMarker.done(CFMLElementTypes.ARGUMENT);

            if (builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            else if (builder.getTokenType() != CFMLTokenTypes.RPAREN)
            {
                if (builder.getCurrentOffset() == startOffset)
                {
                    builder.advanceLexer();
                }
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RPAREN)
        {
            builder.advanceLexer();
        }

        argListMarker.done(CFMLElementTypes.ARGUMENT_LIST);
    }

    private void parseIndexAccess(PsiBuilder builder)
    {
        builder.advanceLexer(); // [
        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RBRACKET)
        {
            int startOffset = builder.getCurrentOffset();
            parseExpression(builder, 0);
            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }
        if (builder.getTokenType() == CFMLTokenTypes.RBRACKET)
        {
            builder.advanceLexer();
        }
    }

    private void parseArrayLiteral(PsiBuilder builder)
    {
        builder.advanceLexer(); // [

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RBRACKET)
        {
            int startOffset = builder.getCurrentOffset();
            parseExpression(builder, 0);
            if (builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            if (builder.getCurrentOffset() == startOffset)
            {
                builder.advanceLexer();
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RBRACKET)
        {
            builder.advanceLexer();
        }
    }

    private void parseStructLiteral(PsiBuilder builder)
    {
        builder.advanceLexer(); // {

        while (!builder.eof() && builder.getTokenType() != CFMLTokenTypes.RBRACE)
        {
            int startOffset = builder.getCurrentOffset();
            PsiBuilder.Marker entryMarker = builder.mark();

            if (builder.getTokenType() == CFMLTokenTypes.IDENTIFIER ||
                builder.getTokenType() == CFMLTokenTypes.DOUBLE_QUOTED_STRING ||
                builder.getTokenType() == CFMLTokenTypes.SINGLE_QUOTED_STRING ||
                CFMLTokenTypes.KEYWORDS.contains(builder.getTokenType()))
            {
                builder.advanceLexer();
            }

            if (builder.getTokenType() == CFMLTokenTypes.COLON || builder.getTokenType() == CFMLTokenTypes.ASSIGN)
            {
                builder.advanceLexer();
                parseExpression(builder, 0);
            }

            entryMarker.done(CFMLElementTypes.STRUCT_ENTRY);

            if (builder.getTokenType() == CFMLTokenTypes.COMMA)
            {
                builder.advanceLexer();
            }
            else if (builder.getTokenType() != CFMLTokenTypes.RBRACE)
            {
                if (builder.getCurrentOffset() == startOffset)
                {
                    builder.advanceLexer();
                }
            }
        }

        if (builder.getTokenType() == CFMLTokenTypes.RBRACE)
        {
            builder.advanceLexer();
        }
    }

    private int getInfixPrecedence(IElementType op)
    {
        if (op == CFMLTokenTypes.ASSIGN || isAssignmentOp(op)) return 1;
        if (op == CFMLTokenTypes.QUESTION || op == CFMLTokenTypes.ELVIS) return 2;
        if (op == CFMLTokenTypes.OR_OR || op == CFMLTokenTypes.OP_OR || op == CFMLTokenTypes.OP_XOR || op == CFMLTokenTypes.OP_EQV || op == CFMLTokenTypes.OP_IMP) return 3;
        if (op == CFMLTokenTypes.AND_AND || op == CFMLTokenTypes.OP_AND) return 4;
        if (op == CFMLTokenTypes.EQ_EQ || op == CFMLTokenTypes.NOT_EQ || op == CFMLTokenTypes.EXACT_EQ || op == CFMLTokenTypes.EXACT_NOT_EQ ||
            op == CFMLTokenTypes.OP_EQ || op == CFMLTokenTypes.OP_NEQ || op == CFMLTokenTypes.OP_IS || op == CFMLTokenTypes.OP_EQUAL) return 5;
        if (op == CFMLTokenTypes.LESS || op == CFMLTokenTypes.LESS_EQ || op == CFMLTokenTypes.GREATER || op == CFMLTokenTypes.GREATER_EQ ||
            op == CFMLTokenTypes.OP_LT || op == CFMLTokenTypes.OP_LTE || op == CFMLTokenTypes.OP_GT || op == CFMLTokenTypes.OP_GTE ||
            op == CFMLTokenTypes.OP_CONTAINS || op == CFMLTokenTypes.SPACESHIP) return 6;
        if (op == CFMLTokenTypes.PLUS || op == CFMLTokenTypes.MINUS || op == CFMLTokenTypes.BIT_AND) return 7;
        if (op == CFMLTokenTypes.MULTIPLY || op == CFMLTokenTypes.DIVIDE || op == CFMLTokenTypes.MODULO || op == CFMLTokenTypes.OP_MOD || op == CFMLTokenTypes.POWER) return 8;
        if (op == CFMLTokenTypes.LPAREN || op == CFMLTokenTypes.LBRACKET || op == CFMLTokenTypes.DOT || op == CFMLTokenTypes.SAFE_NAV ||
            op == CFMLTokenTypes.PLUS_PLUS || op == CFMLTokenTypes.MINUS_MINUS) return 9;
        return 0;
    }

    private boolean isAssignmentOp(IElementType op)
    {
        return op == CFMLTokenTypes.PLUS_ASSIGN ||
               op == CFMLTokenTypes.MINUS_ASSIGN ||
               op == CFMLTokenTypes.MUL_ASSIGN ||
               op == CFMLTokenTypes.DIV_ASSIGN ||
               op == CFMLTokenTypes.MOD_ASSIGN ||
               op == CFMLTokenTypes.POW_ASSIGN ||
               op == CFMLTokenTypes.CONCAT_ASSIGN;
    }

}