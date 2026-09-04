package com.quetwo.intellilucee.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import org.jdom.Element

class CFMLDapRemoteConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, factory, name)
{
    var host: String = "127.0.0.1"
    var port: Int = 10000
    var dapSecret: String = ""

    override fun getConfigurationEditor(): SettingsEditor<out RunConfigurationBase<*>> = CFMLDapRemoteSettingsEditor()

    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration()
    {
        if (host.isBlank())
        {
            throw RuntimeConfigurationError("Host is required")
        }

        if (port !in 1..65535)
        {
            throw RuntimeConfigurationError("Port must be between 1 and 65535")
        }
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        CFMLDapRemoteRunProfileState(this)

    override fun readExternal(element: Element)
    {
        super.readExternal(element)
        host = JDOMExternalizerUtil.readField(element, "host", host)
        port = JDOMExternalizerUtil.readField(element, "port", port.toString()).toIntOrNull() ?: port
        dapSecret = JDOMExternalizerUtil.readField(element, "dapSecret", dapSecret)
    }

    override fun writeExternal(element: Element)
    {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "host", host)
        JDOMExternalizerUtil.writeField(element, "port", port.toString())
        JDOMExternalizerUtil.writeField(element, "dapSecret", dapSecret)
    }
}
