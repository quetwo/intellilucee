package com.quetwo.intellilucee.parser

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLLexerTest {

    private fun tokenize(code: String): List<Pair<String, IElementType?>> {
        val lexer = CFMLLexer()
        lexer.start(code, 0, code.length, CFMLLexer.STATE_DEFAULT)
        val tokens = mutableListOf<Pair<String, IElementType?>>()
        while (lexer.tokenType != null) {
            val text = code.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(Pair(text, lexer.tokenType))
            lexer.advance()
        }
        return tokens
    }

    @Test
    fun testKeywords() {
        val code = "component interface function public private remote var return if else while for in try catch"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.COMPONENT_KEYWORD, tokens[0].second)
        assertEquals(CFMLTokenTypes.INTERFACE_KEYWORD, tokens[1].second)
        assertEquals(CFMLTokenTypes.FUNCTION_KEYWORD, tokens[2].second)
        assertEquals(CFMLTokenTypes.PUBLIC_KEYWORD, tokens[3].second)
        assertEquals(CFMLTokenTypes.PRIVATE_KEYWORD, tokens[4].second)
        assertEquals(CFMLTokenTypes.REMOTE_KEYWORD, tokens[5].second)
        assertEquals(CFMLTokenTypes.VAR_KEYWORD, tokens[6].second)
        assertEquals(CFMLTokenTypes.RETURN_KEYWORD, tokens[7].second)
        assertEquals(CFMLTokenTypes.IF_KEYWORD, tokens[8].second)
        assertEquals(CFMLTokenTypes.ELSE_KEYWORD, tokens[9].second)
        assertEquals(CFMLTokenTypes.WHILE_KEYWORD, tokens[10].second)
        assertEquals(CFMLTokenTypes.FOR_KEYWORD, tokens[11].second)
        assertEquals(CFMLTokenTypes.IN_KEYWORD, tokens[12].second)
        assertEquals(CFMLTokenTypes.TRY_KEYWORD, tokens[13].second)
        assertEquals(CFMLTokenTypes.CATCH_KEYWORD, tokens[14].second)
    }

    @Test
    fun testCaseInsensitiveKeywords() {
        val code = "COMPONENT Function Public RETURN"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.COMPONENT_KEYWORD, tokens[0].second)
        assertEquals(CFMLTokenTypes.FUNCTION_KEYWORD, tokens[1].second)
        assertEquals(CFMLTokenTypes.PUBLIC_KEYWORD, tokens[2].second)
        assertEquals(CFMLTokenTypes.RETURN_KEYWORD, tokens[3].second)
    }

    @Test
    fun testTypes() {
        val code = "numeric string boolean array struct query void any date"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.TYPE_NUMERIC, tokens[0].second)
        assertEquals(CFMLTokenTypes.TYPE_STRING, tokens[1].second)
        assertEquals(CFMLTokenTypes.TYPE_BOOLEAN, tokens[2].second)
        assertEquals(CFMLTokenTypes.TYPE_ARRAY, tokens[3].second)
        assertEquals(CFMLTokenTypes.TYPE_STRUCT, tokens[4].second)
        assertEquals(CFMLTokenTypes.TYPE_QUERY, tokens[5].second)
        assertEquals(CFMLTokenTypes.TYPE_VOID, tokens[6].second)
        assertEquals(CFMLTokenTypes.TYPE_ANY, tokens[7].second)
        assertEquals(CFMLTokenTypes.TYPE_DATE, tokens[8].second)
    }

    @Test
    fun testScopes() {
        val code = "application session variables arguments this cgi request server"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.SCOPE_APPLICATION, tokens[0].second)
        assertEquals(CFMLTokenTypes.SCOPE_SESSION, tokens[1].second)
        assertEquals(CFMLTokenTypes.SCOPE_VARIABLES, tokens[2].second)
        assertEquals(CFMLTokenTypes.SCOPE_ARGUMENTS, tokens[3].second)
        assertEquals(CFMLTokenTypes.SCOPE_THIS, tokens[4].second)
        assertEquals(CFMLTokenTypes.SCOPE_CGI, tokens[5].second)
        assertEquals(CFMLTokenTypes.SCOPE_REQUEST, tokens[6].second)
        assertEquals(CFMLTokenTypes.SCOPE_SERVER, tokens[7].second)
    }

    @Test
    fun testWordOperators() {
        val code = "a eq b and c neq d or e gt f and g gte h and i lt j and k lte l and m is n and o contains p"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_EQ })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_NEQ })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_GT })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_GTE })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_LT })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_LTE })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_IS })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_CONTAINS })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_AND })
        assertTrue(tokens.any { it.second == CFMLTokenTypes.OP_OR })
    }

    @Test
    fun testSymbolOperators() {
        val code = "+ - * / % ++ -- += -= == != <= >= && || => ?: ?. === !== <=>"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.PLUS, tokens[0].second)
        assertEquals(CFMLTokenTypes.MINUS, tokens[1].second)
        assertEquals(CFMLTokenTypes.MULTIPLY, tokens[2].second)
        assertEquals(CFMLTokenTypes.DIVIDE, tokens[3].second)
        assertEquals(CFMLTokenTypes.MODULO, tokens[4].second)
        assertEquals(CFMLTokenTypes.PLUS_PLUS, tokens[5].second)
        assertEquals(CFMLTokenTypes.MINUS_MINUS, tokens[6].second)
        assertEquals(CFMLTokenTypes.PLUS_ASSIGN, tokens[7].second)
        assertEquals(CFMLTokenTypes.MINUS_ASSIGN, tokens[8].second)
        assertEquals(CFMLTokenTypes.EQ_EQ, tokens[9].second)
        assertEquals(CFMLTokenTypes.NOT_EQ, tokens[10].second)
        assertEquals(CFMLTokenTypes.LESS_EQ, tokens[11].second)
        assertEquals(CFMLTokenTypes.GREATER_EQ, tokens[12].second)
        assertEquals(CFMLTokenTypes.AND_AND, tokens[13].second)
        assertEquals(CFMLTokenTypes.OR_OR, tokens[14].second)
        assertEquals(CFMLTokenTypes.ARROW, tokens[15].second)
        assertEquals(CFMLTokenTypes.ELVIS, tokens[16].second)
        assertEquals(CFMLTokenTypes.SAFE_NAV, tokens[17].second)
        assertEquals(CFMLTokenTypes.EXACT_EQ, tokens[18].second)
        assertEquals(CFMLTokenTypes.EXACT_NOT_EQ, tokens[19].second)
        assertEquals(CFMLTokenTypes.SPACESHIP, tokens[20].second)
    }

    @Test
    fun testComments() {
        val code = """
            // line comment
            /* block comment */
            /** doc comment */
            <!-- html comment -->
            <!--- tag comment --->
            <!--- nested <!--- inner ---> tag comment --->
        """.trimIndent()
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.LINE_COMMENT, tokens[0].second)
        assertEquals(CFMLTokenTypes.BLOCK_COMMENT, tokens[1].second)
        assertEquals(CFMLTokenTypes.DOC_COMMENT, tokens[2].second)
        assertEquals(CFMLTokenTypes.LINE_COMMENT, tokens[3].second)
        assertEquals(CFMLTokenTypes.TAG_COMMENT, tokens[4].second)
        assertEquals(CFMLTokenTypes.TAG_COMMENT, tokens[5].second)
        assertEquals("<!--- nested <!--- inner ---> tag comment --->", tokens[5].first)
    }

    @Test
    fun testNumbers() {
        val code = "42 3.14 0xFF 1.5e10"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.INTEGER_LITERAL, tokens[0].second)
        assertEquals("42", tokens[0].first)
        assertEquals(CFMLTokenTypes.FLOAT_LITERAL, tokens[1].second)
        assertEquals("3.14", tokens[1].first)
        assertEquals(CFMLTokenTypes.HEX_LITERAL, tokens[2].second)
        assertEquals("0xFF", tokens[2].first)
        assertEquals(CFMLTokenTypes.FLOAT_LITERAL, tokens[3].second)
        assertEquals("1.5e10", tokens[3].first)
    }

    @Test
    fun testStringsAndHashInterpolation() {
        val code = "\"hello\" 'world' ## #expr#"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.DOUBLE_QUOTED_STRING, tokens[0].second)
        assertEquals(CFMLTokenTypes.SINGLE_QUOTED_STRING, tokens[1].second)
        assertEquals(CFMLTokenTypes.ESCAPED_HASH, tokens[2].second)
        assertEquals(CFMLTokenTypes.HASH, tokens[3].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, tokens[4].second)
        assertEquals("expr", tokens[4].first)
        assertEquals(CFMLTokenTypes.HASH, tokens[5].second)
    }

    @Test
    fun testStringsWithBackslashesAndEscapes() {
        val code = "findOneOf( '/\\', action ) gt 0"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.IDENTIFIER, tokens[0].second)
        assertEquals("findOneOf", tokens[0].first)
        assertEquals(CFMLTokenTypes.LPAREN, tokens[1].second)
        assertEquals(CFMLTokenTypes.SINGLE_QUOTED_STRING, tokens[2].second)
        assertEquals("'/\\'", tokens[2].first)
        assertEquals(CFMLTokenTypes.COMMA, tokens[3].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, tokens[4].second)
        assertEquals("action", tokens[4].first)
        assertEquals(CFMLTokenTypes.RPAREN, tokens[5].second)
        assertEquals(CFMLTokenTypes.OP_GT, tokens[6].second)
        assertEquals(CFMLTokenTypes.INTEGER_LITERAL, tokens[7].second)
        assertEquals("0", tokens[7].first)

        val doubleQuoteCode = "\"C:\\path\\to\\file\""
        val doubleQuoteTokens = tokenize(doubleQuoteCode).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.DOUBLE_QUOTED_STRING, doubleQuoteTokens[0].second)
        assertEquals("\"C:\\path\\to\\file\"", doubleQuoteTokens[0].first)

        val doubledQuotesCode = "'it''s' \"say \"\"hello\"\"\""
        val doubledQuotesTokens = tokenize(doubledQuotesCode).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.SINGLE_QUOTED_STRING, doubledQuotesTokens[0].second)
        assertEquals("'it''s'", doubledQuotesTokens[0].first)
        assertEquals(CFMLTokenTypes.DOUBLE_QUOTED_STRING, doubledQuotesTokens[1].second)
        assertEquals("\"say \"\"hello\"\"\"", doubledQuotesTokens[1].first)
    }

    @Test
    fun testTags() {
        val code = "<cfset x = 1><cffunction name=\"myFunc\" access=\"public\"></cffunction><div class=\"test\"></div>"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }

        // <cfset x = 1>
        assertEquals(CFMLTokenTypes.TAG_OPEN_START, tokens[0].second)
        assertEquals(CFMLTokenTypes.TAG_NAME, tokens[1].second)
        assertEquals("cfset", tokens[1].first)

        // </cffunction>
        val closeFuncStart = tokens.indexOfFirst { it.first == "</" }
        assertTrue(closeFuncStart >= 0)
        assertEquals(CFMLTokenTypes.TAG_CLOSE_START, tokens[closeFuncStart].second)
        assertEquals(CFMLTokenTypes.TAG_NAME, tokens[closeFuncStart + 1].second)
        assertEquals("cffunction", tokens[closeFuncStart + 1].first)
    }

    @Test
    fun testHyphensInTagsAndScript() {
        // Hyphens in tags/attributes vs hyphens in script
        val tagCode = "<custom-tag my-attribute=\"val\" />"
        val tagTokens = tokenize(tagCode).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.TAG_OPEN_START, tagTokens[0].second)
        assertEquals(CFMLTokenTypes.TAG_NAME, tagTokens[1].second)
        assertEquals("custom-tag", tagTokens[1].first)
        assertEquals(CFMLTokenTypes.ATTRIBUTE_NAME, tagTokens[2].second)
        assertEquals("my-attribute", tagTokens[2].first)

        val scriptCode = "a - b; x-y;"
        val scriptTokens = tokenize(scriptCode).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.IDENTIFIER, scriptTokens[0].second)
        assertEquals("a", scriptTokens[0].first)
        assertEquals(CFMLTokenTypes.MINUS, scriptTokens[1].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, scriptTokens[2].second)
        assertEquals("b", scriptTokens[2].first)
        assertEquals(CFMLTokenTypes.SEMICOLON, scriptTokens[3].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, scriptTokens[4].second)
        assertEquals("x", scriptTokens[4].first)
        assertEquals(CFMLTokenTypes.MINUS, scriptTokens[5].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, scriptTokens[6].second)
        assertEquals("y", scriptTokens[6].first)
    }

    @Test
    fun testDollarIdentifiers() {
        val code = "\$foo = 1; var \$bar = \$foo + 2;"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.IDENTIFIER, tokens[0].second)
        assertEquals("\$foo", tokens[0].first)
        assertEquals(CFMLTokenTypes.ASSIGN, tokens[1].second)
        assertEquals(CFMLTokenTypes.INTEGER_LITERAL, tokens[2].second)
        assertEquals(CFMLTokenTypes.SEMICOLON, tokens[3].second)
        assertEquals(CFMLTokenTypes.VAR_KEYWORD, tokens[4].second)
        assertEquals(CFMLTokenTypes.IDENTIFIER, tokens[5].second)
        assertEquals("\$bar", tokens[5].first)
    }

    @Test
    fun testAllRemainingKeywordsAndTypes() {
        val code = "variablename datetime uuid guid binary struct array transaction application implements attributes"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        assertEquals(CFMLTokenTypes.TYPE_VARIABLENAME, tokens[0].second)
        assertEquals(CFMLTokenTypes.TYPE_DATE, tokens[1].second)
        assertEquals(CFMLTokenTypes.TYPE_UUID, tokens[2].second)
        assertEquals(CFMLTokenTypes.TYPE_GUID, tokens[3].second)
        assertEquals(CFMLTokenTypes.TYPE_BINARY, tokens[4].second)
        assertEquals(CFMLTokenTypes.TYPE_STRUCT, tokens[5].second)
        assertEquals(CFMLTokenTypes.TYPE_ARRAY, tokens[6].second)
        assertEquals(CFMLTokenTypes.TRANSACTION_KEYWORD, tokens[7].second)
        assertEquals(CFMLTokenTypes.SCOPE_APPLICATION, tokens[8].second)
        assertEquals(CFMLTokenTypes.IMPLEMENTS_KEYWORD, tokens[9].second)
        assertEquals(CFMLTokenTypes.SCOPE_ATTRIBUTES, tokens[10].second)
    }

    @Test
    fun testLessThanInScriptExpressions() {
        val code = "for (var i = 0; i<len; i++) { if (x<y) { var b = (a)<c; } }"
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        val lessTokens = tokens.filter { it.second == CFMLTokenTypes.LESS }
        assertEquals(3, lessTokens.size)
        // Ensure no tag start is erroneously produced inside loop header
        val tagStartTokens = tokens.filter { it.second == CFMLTokenTypes.TAG_OPEN_START }
        assertEquals(0, tagStartTokens.size)
    }

    @Test
    fun testMasaCmsPatternLexing() {
        val masaCode = """
            <cfcomponent output="false" extends="mura.bean.beanORM">
                <cfproperty name="contentid" fieldtype="id">
                <cfproperty name="siteid" default="">
                <cffunction name="save" access="public" output="false" returntype="any">
                    <cfargument name="data" type="struct" required="false">
                    <cfset var result = "">
                    <cfset var local = {}>
                    <cfif isDefined("arguments.data") and isStruct(arguments.data)>
                        <cfset local.item = arguments.data>
                    </cfif>
                    <cfreturn result>
                </cffunction>
            </cfcomponent>
        """.trimIndent()
        val tokens = tokenize(masaCode).filter { it.second != TokenType.WHITE_SPACE }
        assertTrue(tokens.isNotEmpty())
        assertEquals(CFMLTokenTypes.TAG_OPEN_START, tokens[0].second)
        assertEquals(CFMLTokenTypes.TAG_NAME, tokens[1].second)
        assertEquals("cfcomponent", tokens[1].first)
    }

    @Test
    fun testSqlCommentWithApostropheInCfquery() {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                -- It's a comment with an apostrophe
                SELECT * FROM users WHERE status = 'active'
            </cfquery>
        """.trimIndent()
        val tokens = tokenize(code).filter { it.second != TokenType.WHITE_SPACE }
        val commentToken = tokens.find { it.second == CFMLTokenTypes.LINE_COMMENT }
        assertNotNull("Expected LINE_COMMENT token for SQL comment", commentToken)
        assertEquals("-- It's a comment with an apostrophe", commentToken!!.first)
        val selectToken = tokens.find { it.first.equals("SELECT", ignoreCase = true) }
        assertNotNull("SELECT should be tokenized outside any string literal", selectToken)
    }
}
