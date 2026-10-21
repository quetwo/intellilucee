package com.quetwo.intellilucee.debugger

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class CFMLDapBreakpointManagementTest : BasePlatformTestCase() {

    private fun sendDapFrame(output: PipedOutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "Content-Length: ${bytes.size}\r\n\r\n".toByteArray(StandardCharsets.US_ASCII)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    @Test
    fun testBreakpointHandlerSendsSetBreakpointsOnRegistration() {
        val psiFile = myFixture.configureByText("TestBp.cfm", "<cfset a = 1>\n<cfset b = 2>\n")
        val virtualFile = psiFile.virtualFile

        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java)

        val bp = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 0, CFMLLineBreakpointProperties())
        assertNotNull(bp)

        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val transforms = listOf(PathTransform(virtualFile.parent.path, "/var/www"))
        val handler = CFMLDapBreakpointHandler(client, transforms, project)

        // Read the sent request from serverIn
        val latch = CountDownLatch(1)
        var receivedPayload = ""
        Thread {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(serverIn, StandardCharsets.UTF_8))
            val line = reader.readLine()
            if (line != null && line.startsWith("Content-Length:")) {
                val len = line.substringAfter("Content-Length:").trim().toInt()
                reader.readLine() // empty line
                val chars = CharArray(len)
                var read = 0
                while (read < len) {
                    val r = reader.read(chars, read, len - read)
                    if (r < 0) break
                    read += r
                }
                receivedPayload = String(chars, 0, read)
                latch.countDown()
            }
        }.start()

        handler.registerBreakpoint(bp)

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(receivedPayload.contains("\"command\":\"setBreakpoints\""))
        assertTrue(receivedPayload.contains("\"lines\":[1]"))
        assertTrue(receivedPayload.contains("/var/www/TestBp.cfm"))

        client.close()
    }

    @Test
    fun testSendAllBreakpoints() {
        val psiFile1 = myFixture.configureByText("File1.cfm", "<cfset a = 1>\n<cfset b = 2>\n")
        val psiFile2 = myFixture.configureByText("File2.cfm", "<cfset c = 3>\n")

        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java)

        val bp1 = breakpointManager.addLineBreakpoint(breakpointType, psiFile1.virtualFile.url, 1, CFMLLineBreakpointProperties())
        val bp2 = breakpointManager.addLineBreakpoint(breakpointType, psiFile2.virtualFile.url, 0, CFMLLineBreakpointProperties())
        assertNotNull(bp1)
        assertNotNull(bp2)

        val serverOut = PipedOutputStream()
        val clientIn = PipedInputStream(serverOut)

        val clientOut = PipedOutputStream()
        val serverIn = PipedInputStream(clientOut)

        val client = CFMLDapClient(customInputStream = clientIn, customOutputStream = clientOut)
        client.startListening()

        val transforms = listOf(PathTransform(psiFile1.virtualFile.parent.path, "/remote/app"))
        val handler = CFMLDapBreakpointHandler(client, transforms, project)

        val sendFuture = handler.sendAllBreakpoints()

        // Respond to all requests from serverOut
        Thread {
            try {
                var count = 0
                while (count < 2) {
                    sendDapFrame(serverOut, """{"seq":${count + 1},"type":"response","request_seq":${count + 1},"command":"setBreakpoints","success":true,"body":{"breakpoints":[{"verified":true}]}}""")
                    count++
                }
            } catch (_: Exception) {}
        }.start()

        sendFuture.get(5, TimeUnit.SECONDS)

        client.close()
    }
}
