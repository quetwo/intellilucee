package com.quetwo.intellilucee.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.editor.CFMLCreateDependencyGraphAction
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
class CFMLCreateDependencyGraphActionTest : BasePlatformTestCase()
{
    @Test
    fun testIsSupportedFile()
    {
        val cfmFile = myFixture.createFile("test.cfm", "<cfset x = 1>")
        val cfcFile = myFixture.createFile("Component.cfc", "component {}")
        val upperCfm = myFixture.createFile("TEST.CFM", "<cfset x = 1>")
        val upperCfc = myFixture.createFile("COMPONENT.CFC", "component {}")
        val txtFile = myFixture.createFile("test.txt", "hello")
        val cfsFile = myFixture.createFile("test.cfs", "// cfs")
        val cfmlFile = myFixture.createFile("test.cfml", "<cfset y = 2>")
        val dir = cfmFile.parent

        assertTrue("test.cfm should be supported", CFMLCreateDependencyGraphAction.isSupportedFile(cfmFile))
        assertTrue("Component.cfc should be supported", CFMLCreateDependencyGraphAction.isSupportedFile(cfcFile))
        assertTrue("TEST.CFM should be supported", CFMLCreateDependencyGraphAction.isSupportedFile(upperCfm))
        assertTrue("COMPONENT.CFC should be supported", CFMLCreateDependencyGraphAction.isSupportedFile(upperCfc))
        assertFalse("test.txt should not be supported", CFMLCreateDependencyGraphAction.isSupportedFile(txtFile))
        assertFalse("test.cfs should not be supported", CFMLCreateDependencyGraphAction.isSupportedFile(cfsFile))
        assertFalse("test.cfml should not be supported", CFMLCreateDependencyGraphAction.isSupportedFile(cfmlFile))
        assertFalse("Directory should not be supported", CFMLCreateDependencyGraphAction.isSupportedFile(dir))
        assertFalse("null should not be supported", CFMLCreateDependencyGraphAction.isSupportedFile(null))
    }

    @Test
    fun testActionUpdateVisibility()
    {
        val action = CFMLCreateDependencyGraphAction()

        val cfmFile = myFixture.createFile("page.cfm", "<cfoutput>Hello</cfoutput>")
        val cfcFile = myFixture.createFile("Service.cfc", "component {}")
        val txtFile = myFixture.createFile("notes.txt", "notes")

        val eventCfm = createEvent(cfmFile)
        action.update(eventCfm)
        assertTrue(eventCfm.presentation.isEnabledAndVisible)

        val eventCfc = createEvent(cfcFile)
        action.update(eventCfc)
        assertTrue(eventCfc.presentation.isEnabledAndVisible)

        val eventTxt = createEvent(txtFile)
        action.update(eventTxt)
        assertFalse(eventTxt.presentation.isEnabledAndVisible)

        val eventNull = createEvent(null)
        action.update(eventNull)
        assertFalse(eventNull.presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionExecutionCreatesCommandAndOpensBrowser()
    {
        val cfmFile = myFixture.createFile("index.cfm", "<cfset x = 1>")
        var executedCommandLine: GeneralCommandLine? = null
        var openedTitle: String? = null
        var openedHtml: String? = null

        val fakeExecutable = Files.createTempFile("fake_lsp", ".exe")
        fakeExecutable.toFile().deleteOnExit()

        val expectedHtml = "<html><body>Dependency Graph Content</body></html>"

        val action = CFMLCreateDependencyGraphAction(
            lspExecutableResolver = { fakeExecutable },
            commandExecutor = { cmd ->
                executedCommandLine = cmd
                val params = cmd.parametersList.parameters
                val outIndex = params.indexOf("--out")
                assertTrue("Command line should contain --out parameter", outIndex != -1 && outIndex + 1 < params.size)
                val outFilePath = Path.of(params[outIndex + 1])
                assertTrue("Output file must have .htm extension", outFilePath.fileName.toString().endsWith(".htm"))
                Files.writeString(outFilePath, expectedHtml)
                ProcessOutput(0)
            },
            browserOpener = { _, title, html ->
                openedTitle = title
                openedHtml = html
            }
        )

        val event = createEvent(cfmFile)
        action.actionPerformed(event)
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertNotNull("Command line must have been executed", executedCommandLine)
        val params = executedCommandLine!!.parametersList.parameters
        assertEquals("graph", params[0])
        assertEquals("--format", params[1])
        assertEquals("html", params[2])
        assertEquals("--out", params[3])
        assertTrue("Output file should end with .htm", params[4].endsWith(".htm"))
        assertEquals(cfmFile.path, params[5].replace('\\', '/'))

        assertEquals("Dependency Graph: index.cfm", openedTitle)
        assertEquals(expectedHtml, openedHtml)
    }

    @Test
    fun testActionExecutionFailureDoesNotOpenBrowser()
    {
        val cfcFile = myFixture.createFile("MyComponent.cfc", "component {}")
        var browserOpened = false
        var reportedErrorMessage: String? = null

        val fakeExecutable = Files.createTempFile("fake_lsp", ".exe")
        fakeExecutable.toFile().deleteOnExit()

        val action = CFMLCreateDependencyGraphAction(
            lspExecutableResolver = { fakeExecutable },
            commandExecutor = {
                ProcessOutput("", "Failed to generate graph", 1, false, false)
            },
            browserOpener = { _, _, _ ->
                browserOpened = true
            },
            errorReporter = { _, message ->
                reportedErrorMessage = message
            }
        )

        val event = createEvent(cfcFile)
        action.actionPerformed(event)
        com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        assertFalse("Browser should not be opened when command fails", browserOpened)
        assertNotNull("Error should be reported when command fails", reportedErrorMessage)
        assertTrue(reportedErrorMessage!!.contains("Failed to generate graph"))
    }

    @Test
    fun testActionRegisteredInPluginXml()
    {
        val action = ActionManager.getInstance().getAction("CFML.CreateDependencyGraph")
        assertNotNull("Action CFML.CreateDependencyGraph must be registered in plugin.xml", action)
        assertTrue(action is CFMLCreateDependencyGraphAction)
    }

    private fun createEvent(file: VirtualFile?): AnActionEvent
    {
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        return TestActionEvent.createTestEvent(dataContext)
    }
}
