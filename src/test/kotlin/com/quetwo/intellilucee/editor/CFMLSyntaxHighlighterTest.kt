package com.quetwo.intellilucee.editor

import com.quetwo.intellilucee.parser.CFMLTokenTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLSyntaxHighlighterTest {

    private val highlighter = CFMLSyntaxHighlighter()

    @Test
    fun testKeywordsHighlighting() {
        for (kw in CFMLTokenTypes.KEYWORDS.types) {
            val highlights = highlighter.getTokenHighlights(kw)
            assertEquals(1, highlights.size)
            assertEquals(CFMLSyntaxHighlighter.KEYWORD, highlights[0])
        }
    }

    @Test
    fun testTypesHighlighting() {
        for (type in CFMLTokenTypes.TYPES.types) {
            val highlights = highlighter.getTokenHighlights(type)
            assertEquals(1, highlights.size)
            assertEquals(CFMLSyntaxHighlighter.TYPE, highlights[0])
        }
    }

    @Test
    fun testScopesHighlighting() {
        for (scope in CFMLTokenTypes.SCOPES.types) {
            val highlights = highlighter.getTokenHighlights(scope)
            assertEquals(1, highlights.size)
            assertEquals(CFMLSyntaxHighlighter.SCOPE, highlights[0])
        }
    }

    @Test
    fun testOperatorsHighlighting() {
        for (op in CFMLTokenTypes.OPERATORS.types) {
            val highlights = highlighter.getTokenHighlights(op)
            assertEquals(1, highlights.size)
            assertEquals(CFMLSyntaxHighlighter.OPERATOR, highlights[0])
        }
    }

    @Test
    fun testCommentsHighlighting() {
        val lineHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.LINE_COMMENT)
        assertEquals(CFMLSyntaxHighlighter.LINE_COMMENT, lineHighlights[0])

        val blockHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.BLOCK_COMMENT)
        assertEquals(CFMLSyntaxHighlighter.BLOCK_COMMENT, blockHighlights[0])

        val tagHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.TAG_COMMENT)
        assertEquals(CFMLSyntaxHighlighter.TAG_COMMENT, tagHighlights[0])
    }

    @Test
    fun testStringsAndHashHighlighting() {
        val strHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.DOUBLE_QUOTED_STRING)
        assertEquals(CFMLSyntaxHighlighter.STRING, strHighlights[0])

        val hashHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.HASH)
        assertEquals(CFMLSyntaxHighlighter.HASH, hashHighlights[0])

        val escapedHashHighlights = highlighter.getTokenHighlights(CFMLTokenTypes.ESCAPED_HASH)
        assertEquals(CFMLSyntaxHighlighter.HASH, escapedHashHighlights[0])
    }

    @Test
    fun testTagsHighlighting() {
        val tagOpen = highlighter.getTokenHighlights(CFMLTokenTypes.TAG_OPEN_START)
        assertEquals(CFMLSyntaxHighlighter.TAG, tagOpen[0])

        val tagName = highlighter.getTokenHighlights(CFMLTokenTypes.TAG_NAME)
        assertEquals(CFMLSyntaxHighlighter.TAG_NAME, tagName[0])

        val attrName = highlighter.getTokenHighlights(CFMLTokenTypes.ATTRIBUTE_NAME)
        assertEquals(CFMLSyntaxHighlighter.ATTRIBUTE_NAME, attrName[0])
    }

    @Test
    fun testColorSettingsPage() {
        val page = CFMLColorSettingsPage()
        assertEquals("CFML", page.displayName)
        assertNotNull(page.icon)
        assertNotNull(page.highlighter)
        assertTrue(page.demoText.isNotEmpty())
        assertTrue(page.attributeDescriptors.isNotEmpty())
        val descriptorNames = page.attributeDescriptors.map { it.displayName }
        assertTrue(descriptorNames.contains("Function declaration"))
        assertTrue(descriptorNames.contains("Function call"))
    }

    @Test
    fun testSyntaxHighlighterFactoryReturnsHighlighter() {
        val factory = CFMLSyntaxHighlighterFactory()
        val highlighter = factory.getSyntaxHighlighter(null, null)
        assertNotNull(highlighter)
        assertTrue(highlighter is CFMLSyntaxHighlighter)
    }
}
