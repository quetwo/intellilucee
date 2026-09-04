package com.quetwo.intellilucee.debugger

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.openapi.project.Project
import com.quetwo.intellilucee.CFMLIcon

class CFMLDapRemoteConfigurationType : ConfigurationTypeBase(
    "CFMLDapRemoteConfigurationType",
    "Lucee Debugger Extension",
    "Connect to the Lucee Debugger Extension",
    CFMLIcon.FILE
)
{
    init
    {
        addFactory(CFMLDapRemoteConfigurationFactory(this))
    }

    private class CFMLDapRemoteConfigurationFactory(type: CFMLDapRemoteConfigurationType) : ConfigurationFactory(type)
    {
        override fun getId(): String = "CFMLDapRemoteConfigurationFactory"

        override fun createTemplateConfiguration(project: Project) =
            CFMLDapRemoteConfiguration(project, this, "CFML DAP Remote")
    }
}
