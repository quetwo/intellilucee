package com.quetwo.intellilucee.debugger

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets

class CFMLDapProcessHandler @JvmOverloads constructor(
    val socket: Socket,
    val dapClient: CFMLDapClient? = null
) : ProcessHandler()
{
    companion object
    {
        private val LOG: Logger = Logger.getInstance(CFMLDapProcessHandler::class.java)
    }

    init
    {
        dapClient?.let { client ->
            client.onOutput = { outputEvent ->
                val outputType = when (outputEvent.category?.lowercase()) {
                    "stderr" -> ProcessOutputTypes.STDERR
                    "system" -> ProcessOutputTypes.SYSTEM
                    else -> ProcessOutputTypes.STDOUT
                }
                notifyTextAvailable(outputEvent.output, outputType)
            }
            client.onRawOutput = { text ->
                notifyTextAvailable(text, ProcessOutputTypes.STDOUT)
            }
            client.onTerminated = {
                if (!isProcessTerminated && !isProcessTerminating)
                {
                    closeSocket()
                    notifyProcessTerminated(0)
                }
            }
        }
    }

    override fun startNotify()
    {
        if (isStartNotified) return
        super.startNotify()
        if (dapClient != null)
        {
            dapClient.startListening()
        }
        else
        {
            ApplicationManager.getApplication().executeOnPooledThread {
                listenToSocket()
            }
        }
    }

    private fun listenToSocket()
    {
        try
        {
            val input: InputStream = socket.getInputStream()
            val buffer = ByteArray(4096)
            while (!isProcessTerminating && !isProcessTerminated && !socket.isClosed)
            {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1)
                {
                    break
                }
                val text = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                LOG.debug("[LUCEE-DAP] <= $text")
                notifyTextAvailable(text, ProcessOutputTypes.STDOUT)
            }
        }
        catch (_: Exception)
        {
            // Socket closed or connection interrupted
        }
        finally
        {
            if (!isProcessTerminated)
            {
                closeSocket()
                notifyProcessTerminated(0)
            }
        }
    }

    private fun closeSocket()
    {
        try
        {
            dapClient?.close()
            if (!socket.isClosed)
            {
                socket.close()
            }
        }
        catch (_: Exception)
        {
        }
    }

    override fun destroyProcessImpl()
    {
        closeSocket()
        notifyProcessTerminated(0)
    }

    override fun detachProcessImpl()
    {
        closeSocket()
        notifyProcessDetached()
    }

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream? =
        if (!socket.isClosed) socket.getOutputStream() else null
}
