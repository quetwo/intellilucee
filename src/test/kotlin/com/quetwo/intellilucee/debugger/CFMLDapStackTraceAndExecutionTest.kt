package com.quetwo.intellilucee.debugger

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapStackTraceAndExecutionTest : BasePlatformTestCase() {

    private fun sendDapFrame(output: PipedOutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    @Test
    fun testExecutionStackComputesFramesWithSourceMapping() {
        val psiFile = myFixture.configureByText("Application.cfc", "component {\n  function onApplicationStart() {\n    return true;\n  }\n}")
        val virtualFile = psiFile.virtualFile

        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val transforms = listOf(PathTransform(virtualFile.parent.path, "/var/www"))
        val thread = DapThread(1L, "Main Thread")
        val execStack = CFMLExecutionStack(client, thread, transforms)

        val framesLatch = CountDownLatch(1)
        val framesList = mutableListOf<XStackFrame>()

        val container = object : XExecutionStack.XStackFrameContainer {
            override fun addStackFrames(stackFrames: MutableList<out XStackFrame>, last: Boolean) {
                framesList.addAll(stackFrames)
                framesLatch.countDown()
            }
            override fun errorOccurred(errorMessage: String) {}
            override fun isObsolete(): Boolean = false
        }

        execStack.computeStackFrames(0, container)

        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"stackTrace","success":true,"body":{"stackFrames":[{"id":201,"name":"onApplicationStart()","line":2,"column":1,"source":{"path":"/var/www/Application.cfc"}}]}}""")

        assertTrue(framesLatch.await(5, TimeUnit.SECONDS))
        assertEquals(1, framesList.size)

        val topFrame = framesList[0] as CFMLStackFrame
        assertEquals("onApplicationStart()", topFrame.frame.name)
        assertEquals(2, topFrame.frame.line)

        val sourcePos = topFrame.sourcePosition
        assertNotNull(sourcePos)
        assertEquals(1, sourcePos!!.line) // 0-based in IntelliJ
        assertEquals(virtualFile.path, sourcePos.file.path)

        client.close()
    }

    @Test
    fun testSuspendContextComputesMultipleExecutionStacks() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val stoppedEvent = DapStoppedEventBody(reason = "breakpoint", threadId = 1L, allThreadsStopped = false)
        val suspendContext = CFMLSuspendContext(client, stoppedEvent, emptyList())

        assertNotNull(suspendContext.activeExecutionStack)

        val stacksLatch = CountDownLatch(1)
        val collectedStacks = mutableListOf<XExecutionStack>()

        val container = object : XSuspendContext.XExecutionStackContainer {
            override fun addExecutionStack(executionStacks: MutableList<out XExecutionStack>, last: Boolean) {
                collectedStacks.addAll(executionStacks)
                stacksLatch.countDown()
            }
            override fun errorOccurred(errorMessage: String) {}
        }

        suspendContext.computeExecutionStacks(container)

        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"threads","success":true,"body":{"threads":[{"id":1,"name":"HTTP Worker 1"},{"id":2,"name":"HTTP Worker 2"}]}}""")

        assertTrue(stacksLatch.await(5, TimeUnit.SECONDS))
        assertEquals(2, collectedStacks.size)

        client.close()
    }

    @Test
    fun testDebugProcessSteppingAndControl() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        // Stepping requests verification via DAP client
        val nextFuture = client.next(1L)
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"next","success":true}""")
        assertTrue(nextFuture.get(5, TimeUnit.SECONDS).success)

        val stepInFuture = client.stepIn(1L)
        sendDapFrame(serverOut, """{"seq":2,"type":"response","request_seq":2,"command":"stepIn","success":true}""")
        assertTrue(stepInFuture.get(5, TimeUnit.SECONDS).success)

        val stepOutFuture = client.stepOut(1L)
        sendDapFrame(serverOut, """{"seq":3,"type":"response","request_seq":3,"command":"stepOut","success":true}""")
        assertTrue(stepOutFuture.get(5, TimeUnit.SECONDS).success)

        val contFuture = client.continueThread(1L)
        sendDapFrame(serverOut, """{"seq":4,"type":"response","request_seq":4,"command":"continue","success":true}""")
        assertTrue(contFuture.get(5, TimeUnit.SECONDS).success)

        val pauseFuture = client.pause(1L)
        sendDapFrame(serverOut, """{"seq":5,"type":"response","request_seq":5,"command":"pause","success":true}""")
        assertTrue(pauseFuture.get(5, TimeUnit.SECONDS).success)

        client.close()
    }
}
