package com.quetwo.intellilucee.debugger

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets

class CFMLDapProcessHandler(
    val socket: Socket
) : ProcessHandler()
{
    companion object
    {
        private val LOG: Logger = Logger.getInstance(CFMLDapProcessHandler::class.java)
    }
    override fun startNotify()
    {
        super.startNotify()
        ApplicationManager.getApplication().executeOnPooledThread {
            listenToSocket()
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
