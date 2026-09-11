package com.quetwo.intellilucee.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CFMLGlobalSettings", storages = [Storage("intellilucee.xml")])
class CFMLGlobalSettings : PersistentStateComponent<CFMLGlobalSettingsState>
{
    private var state = CFMLGlobalSettingsState()

    override fun getState(): CFMLGlobalSettingsState
    {
        return state
    }

    override fun loadState(state: CFMLGlobalSettingsState)
    {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object
    {
        @JvmStatic
        fun getInstance(): CFMLGlobalSettings
        {
            return ApplicationManager.getApplication().getService(CFMLGlobalSettings::class.java)
        }
    }
}
