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
import java.nio.file.InvalidPathException
import java.nio.file.Path

class RustCFMLRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, factory, name)
{
    var executablePath: String = ""
    var applicationDirectory: String = project.basePath.orEmpty()
    var port: Int = DEFAULT_PORT

    override fun getConfigurationEditor(): SettingsEditor<out RunConfigurationBase<*>> = RustCFMLSettingsEditor(project)

    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration()
    {
        if (executablePath.isBlank())
        {
            throw RuntimeConfigurationError("RUSTCFML executable is required")
        }

        val executable = try
        {
            Path.of(executablePath)
        }
        catch (exception: InvalidPathException)
        {
            throw RuntimeConfigurationError("RUSTCFML executable path is invalid")
        }
        if (!executable.toFile().isFile || !executable.toFile().canExecute())
        {
            throw RuntimeConfigurationError("Select an existing executable RUSTCFML file")
        }

        if (applicationDirectory.isBlank())
        {
            throw RuntimeConfigurationError("Application directory is required")
        }

        val projectRoot = project.basePath?.let { Path.of(it) }
            ?: throw RuntimeConfigurationError("The project must have a local root directory")
        val applicationPath = try
        {
            Path.of(applicationDirectory)
        }
        catch (exception: InvalidPathException)
        {
            throw RuntimeConfigurationError("Application directory path is invalid")
        }

        if (!applicationPath.toFile().isDirectory || !isWithinProject(projectRoot, applicationPath))
        {
            throw RuntimeConfigurationError("Application directory must be inside the project")
        }

        if (port !in 1..65535)
        {
            throw RuntimeConfigurationError("Port must be between 1 and 65535")
        }
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
        RustCFMLRunProfileState(environment, this)

    override fun readExternal(element: Element)
    {
        super.readExternal(element)
        executablePath = JDOMExternalizerUtil.readField(element, EXECUTABLE_PATH_FIELD, executablePath)
        applicationDirectory = JDOMExternalizerUtil.readField(element, APPLICATION_DIRECTORY_FIELD, applicationDirectory)
        port = JDOMExternalizerUtil.readField(element, PORT_FIELD, port.toString()).toIntOrNull() ?: port
    }

    override fun writeExternal(element: Element)
    {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, EXECUTABLE_PATH_FIELD, executablePath)
        JDOMExternalizerUtil.writeField(element, APPLICATION_DIRECTORY_FIELD, applicationDirectory)
        JDOMExternalizerUtil.writeField(element, PORT_FIELD, port.toString())
    }

    companion object
    {
        const val DEFAULT_PORT = 8080
        private const val EXECUTABLE_PATH_FIELD = "executablePath"
        private const val APPLICATION_DIRECTORY_FIELD = "applicationDirectory"
        private const val PORT_FIELD = "port"

        internal fun isWithinProject(projectRoot: Path, applicationDirectory: Path): Boolean = try
        {
            applicationDirectory.toRealPath().startsWith(projectRoot.toRealPath())
        }
        catch (exception: Exception)
        {
            false
        }
    }
}