package com.quetwo.intellilucee.debugger

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapRemoteExecutionTest : BasePlatformTestCase() {

    @Test
    fun testExecuteCommunicatesBreakpointsToDapServerAndKeepsConnectionAlive() {
        val psiFile = myFixture.configureByText("index.cfm", "<cfset x = 1>\n<cfset y = 2>\n<cfset z = 3>\n")
        val virtualFile = psiFile.virtualFile

        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java)

        // Add line breakpoint at line index 0 (line 1) and line index 2 (line 3)
        val bp1 = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 0, CFMLLineBreakpointProperties())
        val bp2 = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 2, CFMLLineBreakpointProperties())
        assertNotNull(bp1)
        assertNotNull(bp2)

        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort

        val executor = Executors.newSingleThreadExecutor()
        val receivedMessagesFuture = executor.submit(Callable {
            val messages = mutableListOf<String>()
            serverSocket.use { server ->
                server.soTimeout = 10000
                val client = server.accept()
                client.soTimeout = 5000
                val input = client.getInputStream()

                val reader = BufferedReader(InputStreamReader(input, StandardCharsets.US_ASCII))
                while (messages.size < 3) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("Content-Length:")) {
                        val length = line.substringAfter("Content-Length:").trim().toInt()
                        // skip empty line \r\n
                        reader.readLine()

                        val bodyChars = CharArray(length)
                        var read = 0
                        while (read < length) {
                            val count = reader.read(bodyChars, read, length - read)
                            if (count < 0) break
                            read += count
                        }
                        messages.add(String(bodyChars, 0, read))
                    }
                }
            }
            messages
        })

        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")
        configuration.host = "127.0.0.1"
        configuration.port = port
        configuration.dapSecret = "testSecret"
        configuration.idePrefix = "/path/to/app"
        configuration.serverPrefix = "/var/www"

        val state = CFMLDapRemoteRunProfileState(configuration)
        val dummyRunner = object : com.intellij.execution.runners.ProgramRunner<com.intellij.execution.configurations.RunnerSettings> {
            override fun getRunnerId(): String = "TestRunner"
            override fun canRun(executorId: String, profile: com.intellij.execution.configurations.RunProfile): Boolean = true
            override fun execute(environment: com.intellij.execution.runners.ExecutionEnvironment) {}
        }
        val executionResult = state.execute(DefaultDebugExecutor.getDebugExecutorInstance(), dummyRunner)
        assertNotNull(executionResult)

        val processHandler = executionResult?.processHandler as? CFMLDapProcessHandler
        assertNotNull(processHandler)
        assertTrue(processHandler!!.socket.isConnected)
        assertFalse(processHandler.socket.isClosed)

        val receivedMessages = receivedMessagesFuture.get(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(3, receivedMessages.size)
        // Message 1: initialize
        assertTrue(receivedMessages[0].contains("\"command\":\"initialize\""))
        assertTrue(receivedMessages[0].contains("\"adapterID\":\"cfml\""))

        // Message 2: attach
        assertTrue(receivedMessages[1].contains("\"command\":\"attach\""))
        assertTrue(receivedMessages[1].contains("\"request\":\"attach\""))
        assertTrue(receivedMessages[1].contains("\"secret\":\"testSecret\""))
        assertTrue(receivedMessages[1].contains("\"pathTransforms\":[{\"idePrefix\":\"/path/to/app\",\"serverPrefix\":\"/var/www\"}]"))

        // Message 3: setBreakpoints
        assertTrue(receivedMessages[2].contains("\"command\":\"setBreakpoints\""))
        assertTrue(receivedMessages[2].contains("\"lines\":[1,3]"))
        assertTrue(receivedMessages[2].contains("\"line\":1"))
        assertTrue(receivedMessages[2].contains("\"line\":3"))

        // Verify destroying process disconnects the socket
        processHandler.destroyProcess()
        assertTrue(processHandler.isProcessTerminating || processHandler.isProcessTerminated)
        assertTrue(processHandler.socket.isClosed)
    }
}
