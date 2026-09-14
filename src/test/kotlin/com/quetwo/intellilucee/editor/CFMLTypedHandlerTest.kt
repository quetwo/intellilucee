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

    @Test
    fun testAutoCloseCurlyBraceInFunctionSimple() {
        myFixture.configureByText("test.cfs", "function test()<caret>")
        myFixture.type('{')
        myFixture.checkResult("function test(){<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionWithSpaces() {
        myFixture.configureByText("test.cfs", "function test() <caret>")
        myFixture.type('{')
        myFixture.checkResult("function test() {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionWithModifiersAndParams() {
        myFixture.configureByText("test.cfc", "public void function doWork(numeric a, string b = \"hello\") <caret>")
        myFixture.type('{')
        myFixture.checkResult("public void function doWork(numeric a, string b = \"hello\") {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInAnonymousFunction() {
        myFixture.configureByText("test.cfs", "var fn = function(x, y) <caret>")
        myFixture.type('{')
        myFixture.checkResult("var fn = function(x, y) {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionMultiline() {
        myFixture.configureByText("test.cfs", "function test()\n<caret>")
        myFixture.type('{')
        myFixture.checkResult("function test()\n{<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionWithAttributes() {
        myFixture.configureByText("test.cfc", "remote any function getData() output=\"false\" <caret>")
        myFixture.type('{')
        myFixture.checkResult("remote any function getData() output=\"false\" {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionDisabledWhenSettingOff() {
        val globalSettings = CFMLGlobalSettings.getInstance()
        val original = globalSettings.state.autoCloseTags
        try {
            globalSettings.state.autoCloseTags = false
            myFixture.configureByText("test.cfs", "function test() <caret>")
            myFixture.type('{')
            myFixture.checkResult("function test() {<caret>")
        } finally {
            globalSettings.state.autoCloseTags = original
        }
    }

    @Test
    fun testAutoCloseCurlyBraceInIfStatement() {
        myFixture.configureByText("test.cfs", "if (x == 1) <caret>")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1) {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInIfStatementMultiline() {
        myFixture.configureByText("test.cfs", "if (x == 1)\n<caret>")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1)\n{<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInElseStatement() {
        myFixture.configureByText("test.cfs", "if (x == 1) { doSomething(); } else <caret>")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1) { doSomething(); } else {<caret>}")

        myFixture.configureByText("test_multiline.cfs", "else\n<caret>")
        myFixture.type('{')
        myFixture.checkResult("else\n{<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInElseIfStatement() {
        myFixture.configureByText("test.cfs", "if (x == 1) { } else if (x == 2) <caret>")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1) { } else if (x == 2) {<caret>}")

        myFixture.configureByText("test_multiline.cfs", "else if (x == 2)\n<caret>")
        myFixture.type('{')
        myFixture.checkResult("else if (x == 2)\n{<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInElseifKeywordStatement() {
        myFixture.configureByText("test.cfs", "if (x == 1) { } elseif (x == 2) <caret>")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1) { } elseif (x == 2) {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInForLoop() {
        myFixture.configureByText("test.cfs", "for (var i = 1; i <= 10; i++) <caret>")
        myFixture.type('{')
        myFixture.checkResult("for (var i = 1; i <= 10; i++) {<caret>}")

        myFixture.configureByText("test_in.cfs", "for (var item in items) <caret>")
        myFixture.type('{')
        myFixture.checkResult("for (var item in items) {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInWhileAndDoLoops() {
        myFixture.configureByText("test.cfs", "while (condition) <caret>")
        myFixture.type('{')
        myFixture.checkResult("while (condition) {<caret>}")

        myFixture.configureByText("test_do.cfs", "do <caret>")
        myFixture.type('{')
        myFixture.checkResult("do {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInSwitchAndCase() {
        myFixture.configureByText("test.cfs", "switch (val) <caret>")
        myFixture.type('{')
        myFixture.checkResult("switch (val) {<caret>}")

        myFixture.configureByText("test_case_num.cfs", "case 1: <caret>")
        myFixture.type('{')
        myFixture.checkResult("case 1: {<caret>}")

        myFixture.configureByText("test_case_str.cfs", "case \"hello\": <caret>")
        myFixture.type('{')
        myFixture.checkResult("case \"hello\": {<caret>}")

        myFixture.configureByText("test_case_nobp.cfs", "case 1 <caret>")
        myFixture.type('{')
        myFixture.checkResult("case 1 {<caret>}")

        myFixture.configureByText("test_default.cfs", "default: <caret>")
        myFixture.type('{')
        myFixture.checkResult("default: {<caret>}")

        myFixture.configureByText("test_default_nobp.cfs", "default <caret>")
        myFixture.type('{')
        myFixture.checkResult("default {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInTryCatchFinally() {
        myFixture.configureByText("test.cfs", "try <caret>")
        myFixture.type('{')
        myFixture.checkResult("try {<caret>}")

        myFixture.configureByText("test_catch.cfs", "catch (any e) <caret>")
        myFixture.type('{')
        myFixture.checkResult("catch (any e) {<caret>}")

        myFixture.configureByText("test_finally.cfs", "finally <caret>")
        myFixture.type('{')
        myFixture.checkResult("finally {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInOtherTags() {
        myFixture.configureByText("test.cfc", "component <caret>")
        myFixture.type('{')
        myFixture.checkResult("component {<caret>}")

        myFixture.configureByText("test_comp_attrs.cfc", "component extends=\"Base\" <caret>")
        myFixture.type('{')
        myFixture.checkResult("component extends=\"Base\" {<caret>}")

        myFixture.configureByText("test_interface.cfc", "interface <caret>")
        myFixture.type('{')
        myFixture.checkResult("interface {<caret>}")

        myFixture.configureByText("test_lock.cfs", "lock timeout=\"10\" <caret>")
        myFixture.type('{')
        myFixture.checkResult("lock timeout=\"10\" {<caret>}")

        myFixture.configureByText("test_transaction.cfs", "transaction <caret>")
        myFixture.type('{')
        myFixture.checkResult("transaction {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInArrowFunction() {
        myFixture.configureByText("test.cfs", "(x, y) => <caret>")
        myFixture.type('{')
        myFixture.checkResult("(x, y) => {<caret>}")
    }

    @Test
    fun testNonBlockCurlyBraceNotAutoClosed() {
        myFixture.configureByText("test.cfs", "var s = <caret>")
        myFixture.type('{')
        myFixture.checkResult("var s = {<caret>")

        myFixture.configureByText("test2.cfs", "data = <caret>")
        myFixture.type('{')
        myFixture.checkResult("data = {<caret>")

        myFixture.configureByText("test3.cfs", "return <caret>")
        myFixture.type('{')
        myFixture.checkResult("return {<caret>")

        myFixture.configureByText("test4.cfs", "callFunc(<caret>")
        myFixture.type('{')
        myFixture.checkResult("callFunc({<caret>")

        myFixture.configureByText("test5.cfs", "var arr = [<caret>")
        myFixture.type('{')
        myFixture.checkResult("var arr = [{<caret>")
    }

    @Test
    fun testAutoCloseCurlyBraceDoesNotDuplicateExisting() {
        myFixture.configureByText("test.cfs", "function test() <caret>}")
        myFixture.type('{')
        myFixture.checkResult("function test() {<caret>}")

        myFixture.configureByText("test_if.cfs", "if (x == 1) <caret>}")
        myFixture.type('{')
        myFixture.checkResult("if (x == 1) {<caret>}")
    }

    @Test
    fun testAutoCloseCurlyBraceInFunctionInsideCfscriptTag() {
        myFixture.configureByText("test.cfm", "<cfscript>\nfunction test() <caret>\n</cfscript>")
        myFixture.type('{')
        myFixture.checkResult("<cfscript>\nfunction test() {<caret>}\n</cfscript>")

        myFixture.configureByText("test_if_tag.cfm", "<cfscript>\nif (isValid) <caret>\n</cfscript>")
        myFixture.type('{')
        myFixture.checkResult("<cfscript>\nif (isValid) {<caret>}\n</cfscript>")
    }

    @Test
    fun testCurlyBraceAfterCommentedFunctionNotAutoClosed() {
        myFixture.configureByText("test.cfs", "/* function foo() */ var s = <caret>")
        myFixture.type('{')
        myFixture.checkResult("/* function foo() */ var s = {<caret>")
    }

    @Test
    fun testTypePeriodInCfmlFile() {
        myFixture.configureByText("test.cfs", "var user = createObject('component', 'models.User');\nuser<caret>")
        myFixture.type('.')
        myFixture.checkResult("var user = createObject('component', 'models.User');\nuser.<caret>")
    }
}
