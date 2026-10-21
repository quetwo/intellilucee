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
    var idePrefix: String = ""
    var serverPrefix: String = ""
    var pathTransforms: List<PathTransform> = emptyList()

    fun getEffectivePathTransforms(): List<PathTransform>
    {
        if (pathTransforms.isNotEmpty())
        {
            return pathTransforms
        }
        if (idePrefix.isNotBlank() || serverPrefix.isNotBlank())
        {
            return listOf(PathTransform(idePrefix, serverPrefix))
        }
        return emptyList()
    }

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
        idePrefix = JDOMExternalizerUtil.readField(element, "idePrefix", idePrefix)
        serverPrefix = JDOMExternalizerUtil.readField(element, "serverPrefix", serverPrefix)

        val transformsElement = element.getChild("pathTransforms")
        if (transformsElement != null)
        {
            val list = mutableListOf<PathTransform>()
            for (child in transformsElement.getChildren("transform"))
            {
                val ide = child.getAttributeValue("idePrefix") ?: ""
                val server = child.getAttributeValue("serverPrefix") ?: ""
                list.add(PathTransform(ide, server))
            }
            pathTransforms = list
        }
    }

    override fun writeExternal(element: Element)
    {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, "host", host)
        JDOMExternalizerUtil.writeField(element, "port", port.toString())
        JDOMExternalizerUtil.writeField(element, "dapSecret", dapSecret)
        JDOMExternalizerUtil.writeField(element, "idePrefix", idePrefix)
        JDOMExternalizerUtil.writeField(element, "serverPrefix", serverPrefix)

        val effectiveTransforms = getEffectivePathTransforms()
        if (effectiveTransforms.isNotEmpty())
        {
            val transformsElement = Element("pathTransforms")
            for (transform in effectiveTransforms)
            {
                val child = Element("transform")
                child.setAttribute("idePrefix", transform.idePrefix)
                child.setAttribute("serverPrefix", transform.serverPrefix)
                transformsElement.addContent(child)
            }
            element.addContent(transformsElement)
        }
    }
}
