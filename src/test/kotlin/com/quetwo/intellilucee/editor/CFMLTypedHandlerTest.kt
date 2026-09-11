package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.settings.CFMLGlobalSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLTypedHandlerTest : BasePlatformTestCase() {

    @Test
    fun testSurroundSelectionWithHashInCfm() {
        myFixture.configureByText("test.cfm", "<cfset <selection>foo</selection> = 1>")
        myFixture.type('#')
        myFixture.checkResult("<cfset #<selection>foo</selection># = 1>")
    }

    @Test
    fun testSurroundSelectionWithHashInCfc() {
        myFixture.configureByText("test.cfc", "component { property <selection>myVar</selection>; }")
        myFixture.type('#')
        myFixture.checkResult("component { property #<selection>myVar</selection>#; }")
    }

    @Test
    fun testSurroundSelectionWithHashInCfs() {
        myFixture.configureByText("test.cfs", "writeOutput(<selection>message</selection>);")
        myFixture.type('#')
        myFixture.checkResult("writeOutput(#<selection>message</selection>#);")
    }

    @Test
    fun testSurroundSelectionWithHashInCfml() {
        myFixture.configureByText("test.cfml", "<cfoutput><selection>data</selection></cfoutput>")
        myFixture.type('#')
        myFixture.checkResult("<cfoutput>#<selection>data</selection>#</cfoutput>")
    }

    @Test
    fun testSurroundMultipleSelectionsWithHash() {
        myFixture.configureByText("test.cfm", "<cfset first = second>")
        val editor = myFixture.editor
        val doc = editor.document
        val firstStart = doc.text.indexOf("first")
        val firstEnd = firstStart + "first".length
        val secondStart = doc.text.indexOf("second")
        val secondEnd = secondStart + "second".length

        val primaryCaret = editor.caretModel.primaryCaret
        primaryCaret.moveToOffset(firstEnd)
        primaryCaret.setSelection(firstStart, firstEnd)

        val secondCaret = editor.caretModel.addCaret(editor.offsetToVisualPosition(secondEnd))
        secondCaret?.setSelection(secondStart, secondEnd)

        myFixture.type('#')
        assertEquals("<cfset #first# = #second#>", doc.text)
    }

    @Test
    fun testNoSelectionTypeHash() {
        myFixture.configureByText("test.cfm", "<cfset foo<caret> = 1>")
        myFixture.type('#')
        myFixture.checkResult("<cfset foo#<caret> = 1>")
    }

    @Test
    fun testSurroundSelectionDisabledWhenSettingOff() {
        val settings = CodeInsightSettings.getInstance()
        val original = settings.SURROUND_SELECTION_ON_QUOTE_TYPED
        try {
            settings.SURROUND_SELECTION_ON_QUOTE_TYPED = false
            myFixture.configureByText("test.cfm", "<cfset <selection>foo</selection> = 1>")
            myFixture.type('#')
            myFixture.checkResult("<cfset #<caret> = 1>")
        } finally {
            settings.SURROUND_SELECTION_ON_QUOTE_TYPED = original
        }
    }

    @Test
    fun testAutoCloseCfmlTagSimple() {
        myFixture.configureByText("test.cfm", "<cfoutput<caret>")
        myFixture.type('>')
        myFixture.checkResult("<cfoutput><caret></cfoutput>")
    }

    @Test
    fun testAutoCloseCfmlTagWithAttributes() {
        myFixture.configureByText("test.cfm", "<cfquery name=\"myQuery\" datasource=\"db\"<caret>")
        myFixture.type('>')
        myFixture.checkResult("<cfquery name=\"myQuery\" datasource=\"db\"><caret></cfquery>")
    }

    @Test
    fun testAutoCloseCfmlTagPreservesCase() {
        myFixture.configureByText("test.cfm", "<CFIF true<caret>")
        myFixture.type('>')
        myFixture.checkResult("<CFIF true><caret></CFIF>")
    }

    @Test
    fun testAutoCloseCfmlTagSelfClosingNotClosed() {
        myFixture.configureByText("test.cfm", "<cfset x = 1 /<caret>")
        myFixture.type('>')
        myFixture.checkResult("<cfset x = 1 /><caret>")
    }

    @Test
    fun testAutoCloseCfmlTagDisabled() {
        val globalSettings = CFMLGlobalSettings.getInstance()
        val original = globalSettings.state.autoCloseTags
        try {
            globalSettings.state.autoCloseTags = false
            myFixture.configureByText("test.cfm", "<cfoutput<caret>")
            myFixture.type('>')
            myFixture.checkResult("<cfoutput><caret>")
        } finally {
            globalSettings.state.autoCloseTags = original
        }
    }

    @Test
    fun testNonCfTagNotAutoClosed() {
        myFixture.configureByText("test.cfm", "<div class=\"test\"<caret>")
        myFixture.type('>')
        myFixture.checkResult("<div class=\"test\"><caret>")
    }

    @Test
    fun testClosingTagNotDoubleClosed() {
        myFixture.configureByText("test.cfm", "</cfoutput<caret>")
        myFixture.type('>')
        myFixture.checkResult("</cfoutput><caret>")
    }

    @Test
    fun testAutoCloseInCfcFile() {
        myFixture.configureByText("test.cfc", "<cfcomponent<caret>")
        myFixture.type('>')
        myFixture.checkResult("<cfcomponent><caret></cfcomponent>")
    }

    @Test
    fun testAutoCloseWithNestedQuotes() {
        myFixture.configureByText("test.cfm", "<cfquery name=\"q\" str=\"<hello> world\"<caret>")
        myFixture.type('>')
        myFixture.checkResult("<cfquery name=\"q\" str=\"<hello> world\"><caret></cfquery>")
    }

    @Test
    fun testAutoCloseExcludedTagsNotClosed() {
        val excludedTags = listOf(
            "cfset",
            "cfdump",
            "cfabort",
            "cflog",
            "cfargument",
            "cfbreak",
            "cfcontent",
            "cferror",
            "cfexecute",
            "cffile"
        )
        for (tag in excludedTags) {
            myFixture.configureByText("test_${tag}.cfm", "<$tag var=\"x\"<caret>")
            myFixture.type('>')
            myFixture.checkResult("<$tag var=\"x\"><caret>")

            myFixture.configureByText("test_${tag}_upper.cfm", "<${tag.uppercase()}<caret>")
            myFixture.type('>')
            myFixture.checkResult("<${tag.uppercase()}><caret>")
        }
    }

    @Test
    fun testAutoCloseDoesNotDuplicateExistingClosingTag() {
        myFixture.configureByText("test.cfm", "<cfoutput<caret></cfoutput>")
        myFixture.type('>')
        myFixture.checkResult("<cfoutput><caret></cfoutput>")
    }
}
