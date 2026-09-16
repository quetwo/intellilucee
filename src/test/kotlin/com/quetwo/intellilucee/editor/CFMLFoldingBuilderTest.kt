package com.quetwo.intellilucee.editor

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLFoldingBuilderTest : BasePlatformTestCase() {

    private val builder = CFMLFoldingBuilder()

    private fun getFoldRegions(text: String, fileName: String = "test.cfm"): Array<FoldingDescriptor> {
        val psiFile = myFixture.configureByText(fileName, text)
        val doc = myFixture.editor.document
        return builder.buildFoldRegions(psiFile, doc, false)
    }

    @Test
    fun testCfqueryFolding() {
        val text = """
            <cfquery name="getUsers" datasource="myDSN">
                SELECT id, username, email
                FROM users
                WHERE active = 1
            </cfquery>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertEquals(1, regions.size)
        val foldedText = text.substring(regions[0].range.startOffset, regions[0].range.endOffset)
        assertEquals(text, foldedText)
    }

    @Test
    fun testCfloopFolding() {
        val text = """
            <cfloop index="i" from="1" to="10">
                <cfoutput>#i#</cfoutput>
            </cfloop>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertEquals(1, regions.size)
        val foldedText = text.substring(regions[0].range.startOffset, regions[0].range.endOffset)
        assertEquals(text, foldedText)
    }

    @Test
    fun testCftryFolding() {
        val text = """
            <cftry>
                <cfset doSomething()>
                <cfcatch type="any">
                    <cfdump var="#cfcatch#">
                </cfcatch>
            </cftry>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertTrue(regions.isNotEmpty())
        val tryRegion = regions.find {
            text.substring(it.range.startOffset, it.range.endOffset).startsWith("<cftry>")
        }
        assertNotNull(tryRegion)
        assertEquals(text, text.substring(tryRegion!!.range.startOffset, tryRegion.range.endOffset))
    }

    @Test
    fun testCfifAndCfelseifFolding() {
        val text = """
            <cfif condition1>
                <cfset a = 1>
            <cfelseif condition2>
                <cfset a = 2>
            <cfelse>
                <cfset a = 3>
            </cfif>
        """.trimIndent()
        val regions = getFoldRegions(text)
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        
        // Should contain cfif folding
        assertTrue("Should fold full cfif", foldedTexts.any { it.startsWith("<cfif condition1>") && it.endsWith("</cfif>") })
        // Should contain cfelseif folding
        assertTrue("Should fold cfelseif", foldedTexts.any { it.startsWith("<cfelseif condition2>") && it.contains("<cfset a = 2>") })
    }

    @Test
    fun testCfswitchAndCfcaseFolding() {
        val text = """
            <cfswitch expression="#fruit#">
                <cfcase value="apple">
                    <cfoutput>Apple pie</cfoutput>
                </cfcase>
                <cfcase value="banana">
                    <cfoutput>Banana bread</cfoutput>
                </cfcase>
                <cfdefaultcase>
                    <cfoutput>Fruit salad</cfoutput>
                </cfdefaultcase>
            </cfswitch>
        """.trimIndent()
        val regions = getFoldRegions(text)
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        
        // Should fold cfswitch
        assertTrue("Should fold cfswitch", foldedTexts.any { it.startsWith("<cfswitch") && it.endsWith("</cfswitch>") })
        // Should fold cfcase apple
        assertTrue("Should fold apple cfcase", foldedTexts.any { it.startsWith("<cfcase value=\"apple\">") && it.endsWith("</cfcase>") })
        // Should fold cfcase banana
        assertTrue("Should fold banana cfcase", foldedTexts.any { it.startsWith("<cfcase value=\"banana\">") && it.endsWith("</cfcase>") })
    }

    @Test
    fun testCaseInsensitiveTagFolding() {
        val text = """
            <CFQUERY NAME="q" DATASOURCE="ds">
                SELECT * FROM tbl
            </CFQUERY>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertEquals(1, regions.size)
        val foldedText = text.substring(regions[0].range.startOffset, regions[0].range.endOffset)
        assertEquals(text, foldedText)
    }

    @Test
    fun testNestedTagsFolding() {
        val text = """
            <cfif isTrue>
                <cfloop query="myQuery">
                    <cftry>
                        <cfquery name="innerQuery">
                            SELECT 1
                        </cfquery>
                        <cfcatch>
                        </cfcatch>
                    </cftry>
                </cfloop>
            </cfif>
        """.trimIndent()
        val regions = getFoldRegions(text)
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        assertEquals(4, regions.size)
        assertTrue(foldedTexts.any { it.startsWith("<cfif isTrue>") })
        assertTrue(foldedTexts.any { it.startsWith("<cfloop query=\"myQuery\">") })
        assertTrue(foldedTexts.any { it.startsWith("<cftry>") })
        assertTrue(foldedTexts.any { it.startsWith("<cfquery name=\"innerQuery\">") })
    }

    @Test
    fun testScriptFoldingPreserved() {
        val text = """
            function calculateTotal(items) {
                var total = 0;
                for (var item in items) {
                    total += item.price;
                }
                return total;
            }
        """.trimIndent()
        val regions = getFoldRegions(text, "test.cfs")
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        assertTrue("Should fold function", foldedTexts.any { it.startsWith("function") })
    }

    @Test
    fun testTagsInsideCommentsNotFolded() {
        val text = """
            <!---
            <cfif commentedTest>
                <cfquery name="q">
                    SELECT 1
                </cfquery>
            </cfif>
            --->
            <cfquery name="activeQuery">
                SELECT 2
            </cfquery>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertEquals(1, regions.size)
        val foldedText = text.substring(regions[0].range.startOffset, regions[0].range.endOffset)
        assertTrue(foldedText.startsWith("<cfquery name=\"activeQuery\">"))
    }

    @Test
    fun testSingleLineTagsDoNotFold() {
        val text = "<cfquery name=\"q\">SELECT 1</cfquery>"
        val regions = getFoldRegions(text)
        assertEquals(0, regions.size)
    }

    @Test
    fun testMultipleCfelseifBranches() {
        val text = """
            <cfif score GT 90>
                <cfset grade = "A">
            <cfelseif score GT 80>
                <cfset grade = "B">
            <cfelseif score GT 70>
                <cfset grade = "C">
            <cfelse>
                <cfset grade = "F">
            </cfif>
        """.trimIndent()
        val regions = getFoldRegions(text)
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        assertEquals(3, regions.size) // cfif, cfelseif #1, cfelseif #2
        assertTrue("Contains cfif", foldedTexts.any { it.startsWith("<cfif score GT 90>") && it.endsWith("</cfif>") })
        assertTrue("Contains first cfelseif", foldedTexts.any { it.startsWith("<cfelseif score GT 80>") && it.contains("grade = \"B\"") })
        assertTrue("Contains second cfelseif", foldedTexts.any { it.startsWith("<cfelseif score GT 70>") && it.contains("grade = \"C\"") })
    }

    @Test
    fun testUnclosedCfcaseFolding() {
        val text = """
            <cfswitch expression="#status#">
                <cfcase value="active">
                    <cfset desc = "Active user">
                <cfcase value="inactive">
                    <cfset desc = "Inactive user">
                <cfdefaultcase>
                    <cfset desc = "Unknown">
            </cfswitch>
        """.trimIndent()
        val regions = getFoldRegions(text)
        val foldedTexts = regions.map { text.substring(it.range.startOffset, it.range.endOffset) }
        assertTrue("Contains cfswitch", foldedTexts.any { it.startsWith("<cfswitch") && it.endsWith("</cfswitch>") })
        assertTrue("Contains active case", foldedTexts.any { it.startsWith("<cfcase value=\"active\">") && it.contains("Active user") })
        assertTrue("Contains inactive case", foldedTexts.any { it.startsWith("<cfcase value=\"inactive\">") && it.contains("Inactive user") })
    }

    @Test
    fun testTagAttributesWithQuotesContainingAngleBrackets() {
        val text = """
            <cfquery name="q" title="a > b">
                SELECT *
                FROM users
            </cfquery>
        """.trimIndent()
        val regions = getFoldRegions(text)
        assertEquals(1, regions.size)
        val foldedText = text.substring(regions[0].range.startOffset, regions[0].range.endOffset)
        assertEquals(text, foldedText)
    }
}
