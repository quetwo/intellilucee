package com.quetwo.intellilucee.debugger

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.openapi.project.Project
import com.quetwo.intellilucee.CFMLIcon

class RustCFMLConfigurationType : ConfigurationTypeBase(
    "RustCFMLConfigurationType",
    "RustCFML",
    "Run a RustCFML application",
    CFMLIcon.RUSTCFML
)
{
    init
    {
        addFactory(RustCFMLConfigurationFactory(this))
    }

    private class RustCFMLConfigurationFactory(type: RustCFMLConfigurationType) : ConfigurationFactory(type)
    {
        override fun getId(): String = "RustCFMLConfigurationFactory"

        override fun createTemplateConfiguration(project: Project) =
            RustCFMLRunConfiguration(project, this, "RUSTCFML")
    }
}