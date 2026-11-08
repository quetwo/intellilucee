package com.quetwo.intellilucee.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.BrowserUtil
import java.util.concurrent.atomic.AtomicBoolean

class RustCFMLRunProfileState(
    environment: ExecutionEnvironment,
    private val configuration: RustCFMLRunConfiguration,
    private val processHandlerFactory: (GeneralCommandLine) -> ProcessHandler = { OSProcessHandler(it) },
    private val browserLauncher: (String) -> Unit = { BrowserUtil.browse(it) }
) : CommandLineState(environment)
{
    private val browserOpened = AtomicBoolean(false)

    internal fun createCommandLine(): GeneralCommandLine = GeneralCommandLine(configuration.executablePath)
        .withParameters("--serve", configuration.applicationDirectory, "--port", configuration.port.toString())
        .withWorkDirectory(configuration.applicationDirectory)

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler
    {
        val processHandler = try
        {
            processHandlerFactory(createCommandLine())
        }
        catch (exception: Exception)
        {
            throw ExecutionException(
                "Failed to start RUSTCFML executable '${configuration.executablePath}': ${exception.message ?: "unknown error"}",
                exception
            )
        }

        if (browserOpened.compareAndSet(false, true))
        {
            try
            {
                browserLauncher("http://localhost:${configuration.port}")
            }
            catch (exception: Exception)
            {
                processHandler.notifyTextAvailable(
                    "Failed to open the default browser: ${exception.message ?: "unknown error"}\n",
                    ProcessOutputType.STDERR
                )
            }
        }
        return processHandler
    }

    internal fun createProcessHandlerForTest(): ProcessHandler = startProcess()
}