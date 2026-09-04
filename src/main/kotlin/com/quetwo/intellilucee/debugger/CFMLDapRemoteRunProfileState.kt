package com.quetwo.intellilucee.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

class CFMLDapRemoteRunProfileState(
    private val configuration: CFMLDapRemoteConfiguration
) : RunProfileState
{
    @Throws(ExecutionException::class)
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? =
        connectToDapServer(configuration.host, configuration.port, configuration.dapSecret)

    @Throws(ExecutionException::class)
    private fun connectToDapServer(host: String, port: Int, secret: String): ExecutionResult?
    {
        try
        {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)

                if (secret.isNotBlank())
                {
                    val initializePayload = """
                        {"seq":1,"type":"request","command":"initialize","arguments":{"adapterID":"cfml","dapSecret":"${escapeJson(secret)}"}}
                    """.trimIndent()
                    val bytes = initializePayload.toByteArray(StandardCharsets.UTF_8)
                    val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
                    socket.getOutputStream().use { output ->
                        output.write(header)
                        output.write(bytes)
                        output.flush()
                    }
                }
            }
        }
        catch (exception: Exception)
        {
            throw ExecutionException("Failed to connect to DAP server at $host:$port", exception)
        }

        return null
    }

    private fun escapeJson(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    companion object
    {
        private const val CONNECT_TIMEOUT_MS = 5000
    }
}