package com.quetwo.intellilucee.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.model.CFMLModelParser
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLLargeFilePerformanceTest : BasePlatformTestCase() {

    private fun generateLargeCfmContent(numBlocks: Int = 500): String {
        val sb = StringBuilder()
        sb.append("<!--- Global Header Comment with information --->\n")
        sb.append("<cfset globalVar = \"init\">\n")
        sb.append("<cfparam name=\"pageTitle\" default=\"Performance Test\">\n\n")

        for (i in 1..numBlocks) {
            sb.append("<!--- Block $i comment with multiple lines\n")
            sb.append("      Line 2 of comment for block $i\n")
            sb.append("--->\n")
            sb.append("<cffunction name=\"func$i\" access=\"public\" returntype=\"string\">\n")
            sb.append("    <cfargument name=\"arg$i\" type=\"numeric\" required=\"true\">\n")
            sb.append("    <cfset var localVal$i = arg$i * 2>\n")
            sb.append("    <cfquery name=\"q$i\" datasource=\"db\">\n")
            sb.append("        SELECT * FROM table_$i WHERE col = #localVal$i#\n")
            sb.append("    </cfquery>\n")
            sb.append("    <cfif localVal$i > 10>\n")
            sb.append("        <cfreturn \"high\">\n")
            sb.append("    <cfelse>\n")
            sb.append("        <cfreturn \"low\">\n")
            sb.append("    </cfif>\n")
            sb.append("</cffunction>\n\n")

            sb.append("<cfoutput>\n")
            sb.append("    #func$i(arg$i=$i)#\n")
            sb.append("</cfoutput>\n\n")
        }

        return sb.toString()
    }

    @Test
    fun testLargeCfmParsingPerformance() {
        val largeContent = generateLargeCfmContent(300) // 300 functions, ~6000 lines
        val start = System.currentTimeMillis()
        val model = CFMLModelParser.parse(largeContent)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(300, model.functions.size)
        assertEquals(300, model.functionCalls.size)
        assertTrue("Parsing large CFM should complete quickly (was ${elapsed}ms)", elapsed < 3000)

        // Test O(1) usage counts
        val func1 = model.functions[0]
        assertEquals(1, model.getFunctionUsageCount(func1))
    }

    @Test
    fun testLargeCfmTypingPerformance() {
        val largeContent = generateLargeCfmContent(200)
        // Add a caret near the end
        val textWithCaret = largeContent + "\n<cfset newVar<caret> = 10>\n"
        myFixture.configureByText("largeTest.cfm", textWithCaret)

        val start = System.currentTimeMillis()
        // Type characters
        myFixture.type(' ')
        myFixture.type('+')
        myFixture.type(' ')
        myFixture.type('5')
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Typing in large CFM file should be fast (was ${elapsed}ms)", elapsed < 2000)
    }

    @Test
    fun testLargeCfmTagAutoClosingPerformance() {
        val largeContent = generateLargeCfmContent(200)
        val textWithCaret = largeContent + "\n<cfloop index=\"i\" from=\"1\" to=\"10\"<caret>\n"
        myFixture.configureByText("largeTagTest.cfm", textWithCaret)

        val start = System.currentTimeMillis()
        myFixture.type('>')
        val elapsed = System.currentTimeMillis() - start

        myFixture.checkResult(largeContent + "\n<cfloop index=\"i\" from=\"1\" to=\"10\"><caret></cfloop>\n")
        assertTrue("Auto-closing tag in large CFM should be fast (was ${elapsed}ms)", elapsed < 2000)
    }

    @Test
    fun testLargeCfmScriptBraceAutoClosingPerformance() {
        val largeContent = generateLargeCfmContent(200)
        val textWithCaret = largeContent + "\n<cfscript>\nfunction testScript(numeric a) <caret>\n</cfscript>\n"
        myFixture.configureByText("largeScriptTest.cfm", textWithCaret)

        val start = System.currentTimeMillis()
        myFixture.type('{')
        val elapsed = System.currentTimeMillis() - start

        myFixture.checkResult(largeContent + "\n<cfscript>\nfunction testScript(numeric a) {<caret>}\n</cfscript>\n")
        assertTrue("Auto-closing brace in large CFM should be fast (was ${elapsed}ms)", elapsed < 2000)
    }

    @Test
    fun testLargeCfmInlayHintsPerformance() {
        val largeContent = generateLargeCfmContent(200)
        val file = myFixture.configureByText("largeInlayTest.cfm", largeContent)
        val model = CFMLPsiUtil.getModel(file)

        val start = System.currentTimeMillis()
        val commentRanges = CFMLModelParser.findCommentRanges(largeContent)
        for (func in model.functions) {
            val offset = CFMLFunctionUsageInlayHintsProvider.findFunctionInlayOffset(largeContent, func, commentRanges)
            assertTrue(offset > 0)
        }
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Inlay hints offset calculations on large CFM should be fast (was ${elapsed}ms)", elapsed < 2000)
    }

    @Test
    fun testLargeCfmFoldingPerformance() {
        val largeContent = generateLargeCfmContent(200)
        val file = myFixture.configureByText("largeFoldTest.cfm", largeContent)

        val foldingBuilder = CFMLFoldingBuilder()
        val start = System.currentTimeMillis()
        val descriptors = foldingBuilder.buildFoldRegions(file, myFixture.editor.document, false)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Folding regions calculation on large CFM should be fast (was ${elapsed}ms)", elapsed < 2000)
        assertTrue(descriptors.isNotEmpty())
    }

    @Test
    fun testLargeCfmLineMarkerPerformance() {
        val largeContent = generateLargeCfmContent(200)
        val file = myFixture.configureByText("largeLineMarkerTest.cfm", largeContent)

        val provider = CFMLFunctionUsageLineMarkerProvider()
        val markers = mutableListOf<com.intellij.codeInsight.daemon.LineMarkerInfo<*>>()

        // Benchmark typing scenario: daemon pass with single element around cursor
        val elem = file.findElementAt(50)!!
        val start = System.currentTimeMillis()
        provider.collectSlowLineMarkers(listOf(elem), markers)
        val elapsed = System.currentTimeMillis() - start

        assertTrue("Single-element line marker pass on large CFM should be near-instant (was ${elapsed}ms)", elapsed < 500)
    }
}
