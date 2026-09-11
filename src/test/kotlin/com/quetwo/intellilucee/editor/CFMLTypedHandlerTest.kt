package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
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
}
