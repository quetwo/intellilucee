package com.quetwo.intellilucee.debugger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapClientTest {

    private fun sendDapFrame(output: PipedOutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    @Test
    fun testInitializeAndAttach() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        // Test initialize
        val initFuture = client.initialize("cfml")
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"initialize","success":true,"body":{"supportsConfigurationDoneRequest":true}}""")

        val initResponse = initFuture.get(5, TimeUnit.SECONDS)
        assertNotNull(initResponse)
        assertTrue(initResponse.success)
        assertEquals("initialize", initResponse.command)

        // Test attach
        val attachFuture = client.attach("my-secret", listOf(PathTransform("/local", "/remote")))
        sendDapFrame(serverOut, """{"seq":2,"type":"response","request_seq":2,"command":"attach","success":true}""")

        val attachResponse = attachFuture.get(5, TimeUnit.SECONDS)
        assertNotNull(attachResponse)
        assertTrue(attachResponse.success)

        client.close()
    }

    @Test
    fun testSetBreakpoints() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val bpFuture = client.setBreakpoints("/var/www/index.cfm", listOf(5, 10))
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"setBreakpoints","success":true,"body":{"breakpoints":[{"id":1,"verified":true,"line":5},{"id":2,"verified":true,"line":10}]}}""")

        val breakpoints = bpFuture.get(5, TimeUnit.SECONDS)
        assertEquals(2, breakpoints.size)
        assertEquals(5, breakpoints[0].line)
        assertTrue(breakpoints[0].verified)
        assertEquals(10, breakpoints[1].line)
        assertTrue(breakpoints[1].verified)

        client.close()
    }

    @Test
    fun testThreadsAndStackTrace() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        // Threads
        val threadsFuture = client.threads()
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"threads","success":true,"body":{"threads":[{"id":1,"name":"Main Thread"},{"id":2,"name":"Worker Thread"}]}}""")

        val threads = threadsFuture.get(5, TimeUnit.SECONDS)
        assertEquals(2, threads.size)
        assertEquals(1L, threads[0].id)
        assertEquals("Main Thread", threads[0].name)

        // StackTrace
        val stackFuture = client.stackTrace(1L, 0, 10)
        sendDapFrame(serverOut, """{"seq":2,"type":"response","request_seq":2,"command":"stackTrace","success":true,"body":{"stackFrames":[{"id":100,"name":"getUser()","line":15,"column":1,"source":{"path":"/var/www/User.cfc","name":"User.cfc"}}]}}""")

        val frames = stackFuture.get(5, TimeUnit.SECONDS)
        assertEquals(1, frames.size)
        assertEquals(100L, frames[0].id)
        assertEquals("getUser()", frames[0].name)
        assertEquals(15, frames[0].line)
        assertEquals("/var/www/User.cfc", frames[0].source?.path)

        client.close()
    }

    @Test
    fun testScopesAndVariables() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        // Scopes
        val scopesFuture = client.scopes(100L)
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"scopes","success":true,"body":{"scopes":[{"name":"Local","variablesReference":1001,"expensive":false},{"name":"Variables","variablesReference":1002,"expensive":false}]}}""")

        val scopes = scopesFuture.get(5, TimeUnit.SECONDS)
        assertEquals(2, scopes.size)
        assertEquals("Local", scopes[0].name)
        assertEquals(1001L, scopes[0].variablesReference)

        // Variables
        val varsFuture = client.variables(1001L)
        sendDapFrame(serverOut, """{"seq":2,"type":"response","request_seq":2,"command":"variables","success":true,"body":{"variables":[{"name":"userId","value":"42","type":"numeric","variablesReference":0},{"name":"userData","value":"Struct (2 keys)","type":"struct","variablesReference":2001}]}}""")

        val vars = varsFuture.get(5, TimeUnit.SECONDS)
        assertEquals(2, vars.size)
        assertEquals("userId", vars[0].name)
        assertEquals("42", vars[0].value)
        assertEquals(0L, vars[0].variablesReference)
        assertEquals("userData", vars[1].name)
        assertEquals(2001L, vars[1].variablesReference)

        client.close()
    }

    @Test
    fun testEvaluateAndSetVariable() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val evalFuture = client.evaluate("userId + 1", 100L, "watch")
        sendDapFrame(serverOut, """{"seq":1,"type":"response","request_seq":1,"command":"evaluate","success":true,"body":{"result":"43","type":"numeric","variablesReference":0}}""")

        val evalResult = evalFuture.get(5, TimeUnit.SECONDS)
        assertEquals("43", evalResult.result)
        assertEquals("numeric", evalResult.type)

        val setVarFuture = client.setVariable(1001L, "userId", "99")
        sendDapFrame(serverOut, """{"seq":2,"type":"response","request_seq":2,"command":"setVariable","success":true,"body":{"name":"userId","value":"99","type":"numeric"}}""")

        val setVarResult = setVarFuture.get(5, TimeUnit.SECONDS)
        assertEquals("userId", setVarResult.name)
        assertEquals("99", setVarResult.value)

        client.close()
    }

    @Test
    fun testSteppingAndControlCommands() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

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

    @Test
    fun testEventsDispatching() {
        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)

        val stoppedLatch = CountDownLatch(1)
        var receivedReason = ""
        var receivedThreadId = 0L
        client.onStopped = { stopped ->
            receivedReason = stopped.reason
            receivedThreadId = stopped.threadId ?: 0L
            stoppedLatch.countDown()
        }

        val outputLatch = CountDownLatch(1)
        var receivedOutput = ""
        var receivedCategory = ""
        client.onOutput = { output ->
            receivedOutput = output.output
            receivedCategory = output.category ?: ""
            outputLatch.countDown()
        }

        client.startListening()

        sendDapFrame(serverOut, """{"seq":1,"type":"event","event":"stopped","body":{"reason":"breakpoint","threadId":1,"allThreadsStopped":true}}""")
        assertTrue(stoppedLatch.await(5, TimeUnit.SECONDS))
        assertEquals("breakpoint", receivedReason)
        assertEquals(1L, receivedThreadId)

        sendDapFrame(serverOut, """{"seq":2,"type":"event","event":"output","body":{"category":"stdout","output":"Hello from server\n"}}""")
        assertTrue(outputLatch.await(5, TimeUnit.SECONDS))
        assertEquals("Hello from server\n", receivedOutput)
        assertEquals("stdout", receivedCategory)

        client.close()
    }
}
