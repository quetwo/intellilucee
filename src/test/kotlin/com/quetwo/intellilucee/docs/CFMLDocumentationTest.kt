package com.quetwo.intellilucee.docs

import com.google.gson.JsonParser
import java.awt.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CFMLDocumentationTest {
    @Test
    fun packagedDatasetContainsCFDocsFunctionsAndTags() {
        val index = javaClass.classLoader.getResourceAsStream("cfdocs/index.txt")
            ?: error("CFDocs index resource is missing")
        val recordCount = index.bufferedReader().use { it.readLines().count(String::isNotBlank) }
        val entries = CFMLDocumentationRepository.loadEntries()

        assertTrue("Expected the full bundled CFDocs snapshot", recordCount >= 1_100)
        assertTrue("Expected over a thousand searchable functions and tags", entries.size >= 1_000)
        assertTrue(entries.all { it.type == "function" || it.type == "tag" })
        assertNotNull(entries.find { it.name.equals("arrayAppend", ignoreCase = true) })
        assertNotNull(entries.find { it.name.equals("cfquery", ignoreCase = true) })
        assertFalse(entries.any { it.type == "listing" || it.type == "component" })
    }

    @Test
    fun searchMatchesNamesDescriptionsAndTypeFilters() {
        val entries = listOf(
            record("queryExecute", "function", "Runs a SQL query"),
            record("cfquery", "tag", "Runs a database query")
        )

        assertEquals(1, CFMLDocumentationFilter.filter(entries, query = "queryexecute").size)
        assertEquals(1, CFMLDocumentationFilter.filter(entries, query = "database").size)
        assertEquals(
            listOf("cfquery"),
            CFMLDocumentationFilter.filter(entries, type = DocumentationTypeFilter.TAGS).map { it.name }
        )
    }

    @Test
    fun engineFilterRequiresExplicitEngineRecordAndKeepsUnknownMinimumVisible() {
        val explicitUnknown = record("explicit", "function", engines = """"lucee":{"minimum_version":""}""")
        val absent = record("absent", "function")

        assertEquals(
            listOf("explicit"),
            CFMLDocumentationFilter.filter(listOf(explicitUnknown, absent), engineId = "lucee", version = "99")
                .map { it.name }
        )
        assertTrue(CFMLDocumentationFilter.filter(listOf(explicitUnknown), engineId = "boxlang").isEmpty())
    }

    @Test
    fun versionFilteringUsesNumericMinimumAndExclusiveRemovalBoundaries() {
        val entry = record(
            "versioned",
            "function",
            engines = """"lucee":{"minimum_version":"5.9","removed":"6.0"}"""
        )

        assertTrue(CFMLDocumentationFilter.filter(listOf(entry), engineId = "lucee", version = "5.8").isEmpty())
        assertEquals(1, CFMLDocumentationFilter.filter(listOf(entry), engineId = "lucee", version = "5.9").size)
        assertEquals(1, CFMLDocumentationFilter.filter(listOf(entry), engineId = "lucee", version = "5.10").size)
        assertTrue(CFMLDocumentationFilter.filter(listOf(entry), engineId = "lucee", version = "6.0").isEmpty())
        assertTrue((CFMLDocumentationFilter.compareVersions("5.10", "5.9") ?: 0) > 0)
    }

    @Test
    fun invalidVersionIsReportedAndDoesNotMatch() {
        val entry = record("versioned", "function", engines = """"lucee":{"minimum_version":"5"}""")

        assertNotNull(CFMLDocumentationFilter.versionError("5.x"))
        assertNull(CFMLDocumentationFilter.versionError("5.10.2"))
        assertTrue(CFMLDocumentationFilter.filter(listOf(entry), engineId = "lucee", version = "5.x").isEmpty())
    }

    @Test
    fun detailHtmlEscapesDescriptionsAndExamples() {
        val entry = record(
            "unsafe",
            "function",
            description = "<script>alert('x')</script> & text",
            extra = """, "examples":[{"title":"Example", "code":"<img src=x>", "result":"<ok>"}]"""
        )

        val html = CFMLDocumentationHtmlRenderer.render(entry)

        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; text"))
        assertTrue(html.contains("&lt;img src=x&gt;"))
        assertTrue(html.contains("&lt;ok&gt;"))
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("https://cfdocs.org/unsafe"))
    }

    @Test
    fun detailHtmlUsesLightAndDarkThemeColors() {
        val entry = record(
            "themed",
            "function",
            engines = """"lucee":{"minimum_version":"5"}""",
            extra = """, "examples":[{"code":"writeOutput('hello')"}]"""
        )
        val darkForeground = Color(224, 224, 224)
        val darkBackground = Color(32, 32, 32)
        val darkHtml = CFMLDocumentationHtmlRenderer.render(entry, darkForeground, darkBackground)
        val lightHtml = CFMLDocumentationHtmlRenderer.render(entry, Color(32, 33, 36), Color(250, 250, 250))
        val emptyHtml = CFMLDocumentationHtmlRenderer.renderMessage("Nothing found", darkForeground, darkBackground)

        assertTrue(darkHtml.contains("color:#e0e0e0;background-color:#202020"))
        assertTrue(darkHtml.contains("a{color:#e0e0e0;"))
        assertTrue(darkHtml.contains("border:1px solid"))
        assertTrue(darkHtml.contains("pre{white-space:pre-wrap;background-color:#2f2f2f;"))
        assertTrue(lightHtml.contains("color:#202124;background-color:#fafafa"))
        assertTrue(lightHtml.contains("a{color:#202124;"))
        assertFalse(darkHtml.contains("#202124"))
        assertFalse(darkHtml.contains("#f3f3f3"))
        assertTrue(emptyHtml.contains("color:#e0e0e0;background-color:#202020"))
        assertTrue(emptyHtml.contains("Nothing found"))
    }

    @Test
    fun actualTagAndFunctionExposeAttributesAndExamples() {
        val entries = CFMLDocumentationRepository.loadEntries()
        val function = entries.first { it.name.equals("arrayAppend", ignoreCase = true) }
        val tag = entries.first { it.name.equals("cfquery", ignoreCase = true) }
        val functionHtml = CFMLDocumentationHtmlRenderer.render(function)
        val tagHtml = CFMLDocumentationHtmlRenderer.render(tag)

        assertTrue(function.parameters.any { it.name == "merge" })
        assertTrue(function.examples.isNotEmpty())
        assertTrue(functionHtml.contains("Parameters and attributes"))
        assertTrue(tagHtml.contains("datasource"))
        assertTrue(tagHtml.contains("queryExecute"))
    }

    private fun record(
        name: String,
        type: String,
        description: String = "",
        engines: String = "",
        extra: String = ""
    ): CFMLDocumentationEntry {
        val engineObject = engines.takeIf(String::isNotBlank)?.let { ",\"engines\":{$it}" }.orEmpty()
        val json = """{"name":"$name","type":"$type","description":"$description"$engineObject$extra}"""
        return CFMLDocumentationEntry.fromJson(JsonParser.parseString(json).asJsonObject)
            ?: error("Test record was not parsed")
    }
}