package com.quetwo.intellilucee.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CFMLGlobalSettingsStateTest
{
    @Test
    fun testDefaultSettings()
    {
        val state = CFMLGlobalSettingsState()
        assertEquals("LATEST", state.lspReleaseVersion)
        assertEquals(true, state.autoCloseTags)
        assertEquals(true, state.syntaxAndErrorHighlighting)
    }

    @Test
    fun testSettingsStateMutation()
    {
        val state = CFMLGlobalSettingsState()
        state.lspReleaseVersion = "v0.2.8"
        assertEquals("v0.2.8", state.lspReleaseVersion)
        state.autoCloseTags = false
        assertEquals(false, state.autoCloseTags)
        state.syntaxAndErrorHighlighting = false
        assertEquals(false, state.syntaxAndErrorHighlighting)
    }
}
