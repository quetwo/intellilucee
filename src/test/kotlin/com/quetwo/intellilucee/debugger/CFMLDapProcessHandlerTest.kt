package com.quetwo.intellilucee.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapProcessHandlerTest : BasePlatformTestCase()
{
    @Test
    fun testProcessHandlerReceivesOutputAndHandlesTermination()
    {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        val textReceived = StringBuilder()
        val textLatch = CountDownLatch(1)
        val terminatedLatch = CountDownLatch(1)

        val executor = Executors.newSingleThreadExecutor()
        val serverFuture = executor.submit {
            serverSocket.use { server ->
                val client = server.accept()
                val out = client.getOutputStream()
                out.write("Hello DAP\n".toByteArray(StandardCharsets.UTF_8))
                out.flush()

                // Wait until client terminates
                client.getInputStream().read()
            }
        }

        val clientSocket = Socket("127.0.0.1", port)
        val handler = CFMLDapProcessHandler(clientSocket)

        handler.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {}

            override fun processTerminated(event: ProcessEvent) {
                terminatedLatch.countDown()
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                textReceived.append(event.text)
                if (textReceived.contains("Hello DAP")) {
                    textLatch.countDown()
                }
            }
        })

        handler.startNotify()

        assertTrue(textLatch.await(5, TimeUnit.SECONDS))
        assertTrue(textReceived.contains("Hello DAP"))
        assertNotNull(handler.processInput)

        handler.destroyProcess()
        assertTrue(terminatedLatch.await(5, TimeUnit.SECONDS))
        assertTrue(handler.isProcessTerminating || handler.isProcessTerminated)
        assertTrue(clientSocket.isClosed)

        serverFuture.get(5, TimeUnit.SECONDS)
        executor.shutdown()
    }
}
