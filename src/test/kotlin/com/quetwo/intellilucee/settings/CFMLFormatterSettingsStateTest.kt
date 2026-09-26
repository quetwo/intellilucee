package com.quetwo.intellilucee.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CFMLFormatterSettingsStateTest
{
    @Test
    fun testDefaultFormatterSettings()
    {
        val state = CFMLFormatterSettingsState()
        assertEquals("same-line", state.braceStyle)
        assertEquals("pad", state.parenSpacing)
    }

    @Test
    fun testFormatterSettingsMutation()
    {
        val state = CFMLFormatterSettingsState()
        state.braceStyle = "next-line"
        state.parenSpacing = "tight"
        assertEquals("next-line", state.braceStyle)
        assertEquals("tight", state.parenSpacing)
    }

    @Test
    fun testDefaultModuleFormatterSettings()
    {
        val state = CFMLFormatterModuleSettingsState()
        assertEquals("same-line", state.braceStyle)
        assertEquals("pad", state.parenSpacing)
    }

    @Test
    fun testModuleFormatterSettingsMutation()
    {
        val state = CFMLFormatterModuleSettingsState()
        state.braceStyle = "next-line"
        state.parenSpacing = "tight"
        assertEquals("next-line", state.braceStyle)
        assertEquals("tight", state.parenSpacing)
    }
}
