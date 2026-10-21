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
        val payload = CFMLDapRemoteRunProfileState.buildInitializePayload(1)
        assertEquals(
            """{"seq":1,"type":"request","command":"initialize","arguments":{"adapterID":"cfml"}}""",
            payload
        )
    }

    @Test
    fun testBuildAttachPayload() {
        val payload = CFMLDapRemoteRunProfileState.buildAttachPayload(2, "my-secret")
        assertEquals(
            """{"seq":2,"type":"request","command":"attach","arguments":{"request":"attach","secret":"my-secret"}}""",
            payload
        )
    }

    @Test
    fun testBuildAttachPayloadWithPathTransforms() {
        val transforms = listOf(
            PathTransform("/path/to/app", "/var/www")
        )
        val payload = CFMLDapRemoteRunProfileState.buildAttachPayload(2, "my-secret", transforms)
        assertEquals(
            """{"seq":2,"type":"request","command":"attach","arguments":{"request":"attach","secret":"my-secret","pathTransforms":[{"idePrefix":"/path/to/app","serverPrefix":"/var/www"}]}}""",
            payload
        )
    }

    @Test
    fun testBuildAttachPayloadWithMultiplePathTransforms() {
        val transforms = listOf(
            PathTransform("/path/to/app", "/var/www"),
            PathTransform("/path/to/lib", "/var/lib")
        )
        val payload = CFMLDapRemoteRunProfileState.buildAttachPayload(2, "my-secret", transforms)
        assertEquals(
            """{"seq":2,"type":"request","command":"attach","arguments":{"request":"attach","secret":"my-secret","pathTransforms":[{"idePrefix":"/path/to/app","serverPrefix":"/var/www"},{"idePrefix":"/path/to/lib","serverPrefix":"/var/lib"}]}}""",
            payload
        )
    }

    @Test
    fun testConfigurationSerializationWithTransforms() {
        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")
        configuration.host = "192.168.1.10"
        configuration.port = 12345
        configuration.dapSecret = "customSecret"
        configuration.idePrefix = "/local/path"
        configuration.serverPrefix = "/remote/path"

        val element = org.jdom.Element("configuration")
        configuration.writeExternal(element)

        val newConfiguration = CFMLDapRemoteConfiguration(project, factory, "Test DAP 2")
        newConfiguration.readExternal(element)

        assertEquals("192.168.1.10", newConfiguration.host)
        assertEquals(12345, newConfiguration.port)
        assertEquals("customSecret", newConfiguration.dapSecret)
        assertEquals("/local/path", newConfiguration.idePrefix)
        assertEquals("/remote/path", newConfiguration.serverPrefix)
        assertEquals(listOf(PathTransform("/local/path", "/remote/path")), newConfiguration.getEffectivePathTransforms())
    }

    @Test
    fun testSettingsEditorApplyAndReset() {
        val configType = CFMLDapRemoteConfigurationType()
        val factory = configType.configurationFactories[0]
        val configuration = CFMLDapRemoteConfiguration(project, factory, "Test DAP")
        configuration.host = "10.0.0.1"
        configuration.port = 8888
        configuration.dapSecret = "sec"
        configuration.pathTransforms = listOf(
            PathTransform("/ide/path1", "/srv/path1"),
            PathTransform("/ide/path2", "/srv/path2")
        )

        val editor = CFMLDapRemoteSettingsEditor(project)
        val panel = editor.component // ensure UI component initialized
        editor.resetFrom(configuration)

        val newConfig = CFMLDapRemoteConfiguration(project, factory, "New DAP")
        editor.applyTo(newConfig)

        assertEquals("10.0.0.1", newConfig.host)
        assertEquals(8888, newConfig.port)
        assertEquals("sec", newConfig.dapSecret)
        assertEquals("/ide/path1", newConfig.idePrefix)
        assertEquals("/srv/path1", newConfig.serverPrefix)
        assertEquals(
            listOf(
                PathTransform("/ide/path1", "/srv/path1"),
                PathTransform("/ide/path2", "/srv/path2")
            ),
            newConfig.getEffectivePathTransforms()
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
