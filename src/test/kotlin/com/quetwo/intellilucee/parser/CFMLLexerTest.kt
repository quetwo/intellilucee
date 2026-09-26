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
}
