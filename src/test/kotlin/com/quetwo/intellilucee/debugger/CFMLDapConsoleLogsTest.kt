package com.quetwo.intellilucee.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapConsoleLogsTest : BasePlatformTestCase() {

    private fun sendDapFrame(output: java.io.OutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    @Test
    fun testProcessHandlerReceivesStdoutAndStderrFromDapOutputEvents() {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        val stdoutReceived = StringBuilder()
        val stderrReceived = StringBuilder()
        val stdoutLatch = CountDownLatch(1)
        val stderrLatch = CountDownLatch(1)

        val executor = Executors.newSingleThreadExecutor()
        val serverFuture = executor.submit {
            serverSocket.use { server ->
                val client = server.accept()
                val out = client.getOutputStream()

                // Send DAP output events
                sendDapFrame(out, """{"seq":1,"type":"event","event":"output","body":{"category":"stdout","output":"[INFO] Application initialized\n"}}""")
                sendDapFrame(out, """{"seq":2,"type":"event","event":"output","body":{"category":"stderr","output":"[ERROR] Database timeout\n"}}""")

                // Wait until client terminates
                client.getInputStream().read()
            }
        }

        val clientSocket = Socket("127.0.0.1", port)
        val dapClient = CFMLDapClient(clientSocket)
        val handler = CFMLDapProcessHandler(clientSocket, dapClient)

        handler.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {}
            override fun processTerminated(event: ProcessEvent) {}

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    stdoutReceived.append(event.text)
                    if (stdoutReceived.contains("[INFO] Application initialized")) {
                        stdoutLatch.countDown()
                    }
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    stderrReceived.append(event.text)
                    if (stderrReceived.contains("[ERROR] Database timeout")) {
                        stderrLatch.countDown()
                    }
                }
            }
        })

        handler.startNotify()

        assertTrue(stdoutLatch.await(5, TimeUnit.SECONDS))
        assertTrue(stderrLatch.await(5, TimeUnit.SECONDS))

        assertTrue(stdoutReceived.contains("[INFO] Application initialized"))
        assertTrue(stderrReceived.contains("[ERROR] Database timeout"))

        handler.destroyProcess()
        serverFuture.get(5, TimeUnit.SECONDS)
        executor.shutdown()
    }
}
