package com.quetwo.intellilucee.debugger

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

@RunWith(JUnit4::class)
class CFMLDapRemoteRunProfileStateTest : BasePlatformTestCase() {

    @Test
    fun testBuildInitializePayload() {
        val payload = CFMLDapRemoteRunProfileState.buildInitializePayload(1, "secret123")
        assertEquals(
            """{"seq":1,"type":"request","command":"initialize","arguments":{"adapterID":"cfml","dapSecret":"secret123"}}""",
            payload
        )
    }

    @Test
    fun testBuildSetBreakpointsPayload() {
        val payload = CFMLDapRemoteRunProfileState.buildSetBreakpointsPayload(2, "C:/app/index.cfm", listOf(5, 10))
        assertEquals(
            """{"seq":2,"type":"request","command":"setBreakpoints","arguments":{"source":{"path":"C:/app/index.cfm"},"lines":[5,10],"breakpoints":[{"line":5},{"line":10}]}}""",
            payload
        )
    }

    @Test
    fun testSendDapMessage() {
        val output = ByteArrayOutputStream()
        val payload = """{"seq":1,"type":"request","command":"initialize"}"""
        CFMLDapRemoteRunProfileState.sendDapMessage(output, payload)

        val expectedHeader = "Content-Length: ${payload.toByteArray(StandardCharsets.UTF_8).size}\r\n\r\n"
        val expected = expectedHeader + payload
        assertEquals(expected, output.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun testCollectLineBreakpointsWithEnabledAndDisabled() {
        val psiFile = myFixture.configureByText("Test.cfm", "line1\nline2\nline3\nline4\nline5\n")
        val virtualFile = psiFile.virtualFile

        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java)

        // Add breakpoint on line index 1 (line 2 in DAP, enabled)
        val bp1 = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 1, CFMLLineBreakpointProperties())
        assertNotNull(bp1)
        // Add breakpoint on line index 3 (line 4 in DAP, disabled)
        val bp2 = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 3, CFMLLineBreakpointProperties())
        assertNotNull(bp2)
        bp2.isEnabled = false

        val breakpoints = CFMLDapRemoteRunProfileState.collectLineBreakpoints(project)
        val expectedPath = bp1.presentableFilePath
        assertTrue(breakpoints.containsKey(expectedPath))
        assertEquals(listOf(2), breakpoints[expectedPath])
    }
}
