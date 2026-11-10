package com.quetwo.intellilucee.docs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CFMLDocumentationToolWindowFactoryTest {
    @Test
    fun factoryDoesNotDeclareDeprecatedOrExperimentalMethods() {
        val declaredMethods = CFMLDocumentationToolWindowFactory::class.java.declaredMethods.map { it.name }
        val unsupportedMethods = setOf("isApplicable", "isDoNotActivateOnStart", "getAnchor", "getIcon", "manage")

        assertTrue(declaredMethods.contains("createToolWindowContent"))
        assertFalse(declaredMethods.any(unsupportedMethods::contains))
    }

    @Test
    fun factoryRemainsRegisteredWithItsExistingId() {
        assertEquals("CFML Documentation", CFMLDocumentationToolWindowFactory.TOOL_WINDOW_ID)
        val pluginXml = javaClass.getResourceAsStream("/META-INF/plugin.xml")
            ?.bufferedReader()
            ?.use { it.readText() }

        assertNotNull(pluginXml)
        assertTrue(pluginXml!!.contains("id=\"CFML Documentation\""))
        assertTrue(
            pluginXml.contains(
                "factoryClass=\"com.quetwo.intellilucee.docs.CFMLDocumentationToolWindowFactory\""
            )
        )
    }
}