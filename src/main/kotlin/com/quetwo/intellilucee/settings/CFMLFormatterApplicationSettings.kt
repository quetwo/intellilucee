package com.quetwo.intellilucee.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CFMLFormatterApplicationSettings", storages = [Storage("intellilucee.xml")])
class CFMLFormatterApplicationSettings : PersistentStateComponent<CFMLFormatterSettingsState>
{
    private var state = CFMLFormatterSettingsState()

    override fun getState(): CFMLFormatterSettingsState
    {
        return state
    }

    override fun loadState(state: CFMLFormatterSettingsState)
    {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object
    {
        fun getInstance(): CFMLFormatterApplicationSettings
        {
            return ApplicationManager.getApplication().getService(CFMLFormatterApplicationSettings::class.java)
        }
    }
}
