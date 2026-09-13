package com.quetwo.intellilucee.editor

import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayPayload
import com.intellij.codeInsight.hints.declarative.InlayPosition
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLComponentDotPathInlayHintsProviderTest : BasePlatformTestCase()
{

    private class TestPresentationTreeBuilder : PresentationTreeBuilder
    {
        val texts = mutableListOf<String>()

        override fun text(text: String, actionData: com.intellij.codeInsight.hints.declarative.InlayActionData?)
        {
            texts.add(text)
        }

        override fun list(builder: PresentationTreeBuilder.() -> Unit)
        {
            builder(this)
        }

        override fun clickHandlerScope(
            actionData: com.intellij.codeInsight.hints.declarative.InlayActionData,
            builder: PresentationTreeBuilder.() -> Unit
        )
        {
            builder(this)
        }

        override fun collapsibleList(
            state: com.intellij.codeInsight.hints.declarative.CollapseState,
            expandedState: com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder.() -> Unit,
            collapsedState: com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder.() -> Unit
        )
        {
        }
    }

    @Test
    fun testComponentDotPathInlayHint_ScriptComponent()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        val psiFile = myFixture.addFileToProject(
            "models/services/UserService.cfc",
            """
            component {
                function init() {}
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)
        val file = myFixture.file

        val provider = CFMLComponentDotPathInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNotNull("Collector should not be null for CFC file", collector)
        assertTrue(collector is SharedBypassCollector)

        val presentations = mutableListOf<Pair<InlayPosition, String>>()
        val mockSink = object : InlayTreeSink
        {
            override fun addPresentation(
                position: InlayPosition,
                payloads: List<InlayPayload>?,
                tooltip: String?,
                hintFormat: HintFormat,
                builder: PresentationTreeBuilder.() -> Unit
            )
            {
                val mockBuilder = TestPresentationTreeBuilder()
                builder(mockBuilder)
                presentations.add(Pair(position, mockBuilder.texts.joinToString("")))
            }

            override fun whenOptionEnabled(optionId: String, block: () -> Unit)
            {
                block()
            }
        }

        (collector as SharedBypassCollector).collectFromElement(file, mockSink)
        assertEquals(1, presentations.size)

        val (pos, text) = presentations[0]
        assertTrue("Position should be InlineInlayPosition", pos is InlineInlayPosition)
        assertEquals("Path: (models.services.UserService)", text)
        val expectedOffset = file.text.indexOf("{") - 1 // before curly brace and space
        assertEquals(expectedOffset, (pos as InlineInlayPosition).offset)
    }

    @Test
    fun testComponentDotPathInlayHint_TagComponent()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        val psiFile = myFixture.addFileToProject(
            "models/OrderService.cfc",
            """
            <cfcomponent displayname="OrderService">
                <cffunction name="init">
                </cffunction>
            </cfcomponent>
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)
        val file = myFixture.file

        val provider = CFMLComponentDotPathInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNotNull("Collector should not be null for CFC file", collector)

        val presentations = mutableListOf<Pair<InlayPosition, String>>()
        val mockSink = object : InlayTreeSink
        {
            override fun addPresentation(
                position: InlayPosition,
                payloads: List<InlayPayload>?,
                tooltip: String?,
                hintFormat: HintFormat,
                builder: PresentationTreeBuilder.() -> Unit
            )
            {
                val mockBuilder = TestPresentationTreeBuilder()
                builder(mockBuilder)
                presentations.add(Pair(position, mockBuilder.texts.joinToString("")))
            }

            override fun whenOptionEnabled(optionId: String, block: () -> Unit)
            {
                block()
            }
        }

        (collector as SharedBypassCollector).collectFromElement(file, mockSink)
        assertEquals(1, presentations.size)
        assertEquals("Path: (models.OrderService)", presentations[0].second)
        val expectedOffset = file.text.indexOf(">")
        assertEquals(expectedOffset, (presentations[0].first as InlineInlayPosition).offset)
    }

    @Test
    fun testComponentDotPathInlayHint_NoComponentKeyword()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        val psiFile = myFixture.addFileToProject(
            "Utility.cfc",
            """
            function helper() {
                return true;
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)
        val file = myFixture.file

        val provider = CFMLComponentDotPathInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNotNull(collector)

        val presentations = mutableListOf<Pair<InlayPosition, String>>()
        val mockSink = object : InlayTreeSink
        {
            override fun addPresentation(
                position: InlayPosition,
                payloads: List<InlayPayload>?,
                tooltip: String?,
                hintFormat: HintFormat,
                builder: PresentationTreeBuilder.() -> Unit
            )
            {
                val mockBuilder = TestPresentationTreeBuilder()
                builder(mockBuilder)
                presentations.add(Pair(position, mockBuilder.texts.joinToString("")))
            }

            override fun whenOptionEnabled(optionId: String, block: () -> Unit)
            {
                block()
            }
        }

        (collector as SharedBypassCollector).collectFromElement(file, mockSink)
        assertEquals(1, presentations.size)
        assertEquals("Path: (Utility)", presentations[0].second)
        val expectedOffset = file.text.indexOfAny(charArrayOf('\n', '\r'))
        assertEquals(expectedOffset, (presentations[0].first as InlineInlayPosition).offset)
    }

    @Test
    fun testComponentDotPathInlayHint_NonCfcFile()
    {
        val file = myFixture.configureByText("test.cfm", "<h1>Hello</h1>")
        val provider = CFMLComponentDotPathInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNull("Collector should be null for non-CFC file", collector)
    }

    @Test
    fun testFindComponentOffset_ScriptWithComments()
    {
        val code = """
            // component inside comment
            /* <cfcomponent> inside block comment */
            component accessors="true" {
            }
        """.trimIndent()

        val offset = CFMLComponentDotPathInlayHintsProvider().findComponentOffset(code)
        val expectedOffset = code.indexOf("{") - 1
        assertEquals(expectedOffset, offset)
    }

    @Test
    fun testFindComponentOffset_TagWithComments()
    {
        val code = """
            <!--- <cfcomponent> inside cfml comment --->
            <cfcomponent displayname="test">
            </cfcomponent>
        """.trimIndent()

        val offset = CFMLComponentDotPathInlayHintsProvider().findComponentOffset(code)
        val expectedOffset = code.indexOf("<cfcomponent displayname") + "<cfcomponent displayname=\"test\"".length
        assertEquals(expectedOffset, offset)
    }

    @Test
    fun testFindComponentOffset_AbstractOrFinalComponent()
    {
        val provider = CFMLComponentDotPathInlayHintsProvider()
        val abstractCode = "abstract component extends=\"Base\" {}"
        val abstractOffset = provider.findComponentOffset(abstractCode)
        assertEquals(abstractCode.indexOf("{") - 1, abstractOffset)

        val finalCode = "final component {}"
        val finalOffset = provider.findComponentOffset(finalCode)
        assertEquals(finalCode.indexOf("{") - 1, finalOffset)
    }

    @Test
    fun testComponentDotPathInlayHint_NoApplicationFile_EmitsNoHint()
    {
        val file = myFixture.configureByText("Standalone.cfc", "component {}")
        val provider = CFMLComponentDotPathInlayHintsProvider()
        val collector = provider.createCollector(file, myFixture.editor)
        assertNotNull(collector)

        val presentations = mutableListOf<Pair<InlayPosition, String>>()
        val mockSink = object : InlayTreeSink
        {
            override fun addPresentation(
                position: InlayPosition,
                payloads: List<InlayPayload>?,
                tooltip: String?,
                hintFormat: HintFormat,
                builder: PresentationTreeBuilder.() -> Unit
            )
            {
                val mockBuilder = TestPresentationTreeBuilder()
                builder(mockBuilder)
                presentations.add(Pair(position, mockBuilder.texts.joinToString("")))
            }

            override fun whenOptionEnabled(optionId: String, block: () -> Unit)
            {
                block()
            }
        }

        (collector as SharedBypassCollector).collectFromElement(file, mockSink)
        assertEquals("Should NOT emit hints when no Application file is resolved", 0, presentations.size)
    }
}
