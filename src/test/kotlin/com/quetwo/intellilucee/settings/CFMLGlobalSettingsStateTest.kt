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
    }

    @Test
    fun testSettingsStateMutation()
    {
        val state = CFMLGlobalSettingsState()
        state.lspReleaseVersion = "v0.2.8"
        assertEquals("v0.2.8", state.lspReleaseVersion)
        state.autoCloseTags = false
        assertEquals(false, state.autoCloseTags)
    }
}
