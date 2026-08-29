package com.quetwo.intellilucee.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.module.Module
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CFMLFormatterModuleSettings", storages = [Storage(StoragePathMacros.MODULE_FILE)])
class CFMLFormatterModuleSettings : PersistentStateComponent<CFMLFormatterModuleSettingsState>
{
    private var state = CFMLFormatterModuleSettingsState()

    override fun getState(): CFMLFormatterModuleSettingsState
    {
        return state
    }

    override fun loadState(state: CFMLFormatterModuleSettingsState)
    {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object
    {
        fun getInstance(module: Module): CFMLFormatterModuleSettings
        {
            return module.getService(CFMLFormatterModuleSettings::class.java)
        }
    }
}
