package com.quetwo.intellilucee.debugger

import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLLineBreakpointTypeTest : BasePlatformTestCase() {

    @Test
    fun testCanPutAtSupportedExtensions() {
        val type = CFMLLineBreakpointType()
        val cfmFile = myFixture.configureByText("test.cfm", "<cfset x = 1>").virtualFile
        val cfcFile = myFixture.configureByText("test.cfc", "component {}").virtualFile
        val cfsFile = myFixture.configureByText("test.cfs", "var x = 1;").virtualFile
        val cfmlFile = myFixture.configureByText("test.cfml", "<cfset x = 1>").virtualFile
        val txtFile = myFixture.configureByText("test.txt", "hello").virtualFile

        assertTrue(type.canPutAt(cfmFile, 0, project))
        assertTrue(type.canPutAt(cfcFile, 0, project))
        assertTrue(type.canPutAt(cfsFile, 0, project))
        assertTrue(type.canPutAt(cfmlFile, 0, project))
        assertFalse(type.canPutAt(txtFile, 0, project))
    }

    @Test
    fun testVariantAndBreakpointMatchWithoutError() {
        val psiFile = myFixture.configureByText("Test.cfm", "line1\nline2\nline3\n")
        val virtualFile = psiFile.virtualFile

        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java) as CFMLLineBreakpointType

        val bp = breakpointManager.addLineBreakpoint(breakpointType, virtualFile.url, 1, CFMLLineBreakpointProperties())
        assertNotNull(bp)

        val pos = XDebuggerUtil.getInstance().createPosition(virtualFile, 1)!!
        val utilImpl = Class.forName("com.intellij.xdebugger.impl.XDebuggerUtilImpl")
        val createDefaultVariantMethod = utilImpl.getDeclaredMethod("createDefaultBreakpointVariant", com.intellij.xdebugger.XSourcePosition::class.java, XLineBreakpointType::class.java)
        createDefaultVariantMethod.isAccessible = true
        val variant = createDefaultVariantMethod.invoke(null, pos, breakpointType) as XLineBreakpointType<*>.XLineBreakpointVariant

        assertTrue(breakpointType.variantAndBreakpointMatch(bp, variant as XLineBreakpointType<CFMLLineBreakpointProperties>.XLineBreakpointVariant))
    }

    @Test
    fun testToggleBreakpointDoesNotProduceMismatchError() {
        val psiFile = myFixture.configureByText("Test.cfm", "<cfset x = 1>")
        val registeredType = XDebuggerUtil.getInstance().findBreakpointType(CFMLLineBreakpointType::class.java) as CFMLLineBreakpointType

        val loggedErrors = mutableListOf<String>()
        LoggedErrorProcessor.executeWith<Throwable>(object : LoggedErrorProcessor() {
            override fun processError(category: String, message: String, details: Array<out String>, t: Throwable?): Set<Action> {
                loggedErrors.add("$category: $message")
                return emptySet()
            }
        }) {
            val utilInstance = XDebuggerUtil.getInstance()
            val m = utilInstance.javaClass.getMethod(
                "toggleAndReturnLineBreakpoint",
                com.intellij.openapi.project.Project::class.java,
                XLineBreakpointType::class.java,
                com.intellij.openapi.vfs.VirtualFile::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            val future = m.invoke(utilInstance, project, registeredType, psiFile.virtualFile, 0, false) as org.jetbrains.concurrency.Promise<*>
            future.blockingGet(5000)
        }

        assertTrue(
            "Expected no breakpoint variant mismatch error, but got: $loggedErrors",
            loggedErrors.none { it.contains("doesn't match source variant") }
        )
    }

    @Test
    fun testCreateBreakpointProperties() {
        val type = CFMLLineBreakpointType()
        val file = myFixture.configureByText("test.cfm", "<cfset x = 1>").virtualFile
        val properties = type.createBreakpointProperties(file, 0)
        assertNotNull(properties)
    }
}
