package com.quetwo.intellilucee.actions.rest

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.editor.CFMLAddRestEndpointAction
import com.quetwo.intellilucee.editor.rest.CFMLAddRestEndpointDialog
import com.quetwo.intellilucee.editor.rest.CFMLRestEndpointGenerator
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLAddRestEndpointActionTest : BasePlatformTestCase() {

    @Test
    fun testActionEnabledOnRestCfc() {
        val psiFile = myFixture.addFileToProject(
            "services/UserService.cfc",
            """
            component rest="true" restPath="/users" {
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val action = CFMLAddRestEndpointAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .add(CommonDataKeys.VIRTUAL_FILE, psiFile.virtualFile)
                .build()
        )

        action.update(event)
        assertTrue(event.presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionDisabledOnNonRestCfc() {
        val psiFile = myFixture.addFileToProject(
            "services/PlainService.cfc",
            """
            component {
                function init() {}
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val action = CFMLAddRestEndpointAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .add(CommonDataKeys.VIRTUAL_FILE, psiFile.virtualFile)
                .build()
        )

        action.update(event)
        assertFalse(event.presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionDisabledOnCfmFile() {
        val psiFile = myFixture.addFileToProject(
            "views/index.cfm",
            """
            <cfcomponent rest="true">
            </cfcomponent>
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val action = CFMLAddRestEndpointAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .add(CommonDataKeys.VIRTUAL_FILE, psiFile.virtualFile)
                .build()
        )

        action.update(event)
        assertFalse(event.presentation.isEnabledAndVisible)
    }

    @Test
    fun testDialogDefaultsAndDescriptor() {
        val dialog = CFMLAddRestEndpointDialog(project)
        try {
            dialog.setFunctionName("getUsers")
            dialog.setHttpMethod("GET")
            dialog.setRestPath("/all")
            dialog.setProduces("application/json")
            dialog.setConsumes("")
            dialog.addArgument("numeric", "page", "Query")

            val descriptor = dialog.getEndpointDescriptor()
            assertEquals("getUsers", descriptor.functionName)
            assertEquals("GET", descriptor.httpMethod)
            assertEquals("/all", descriptor.restPath)
            assertEquals("application/json", descriptor.produces)
            assertEquals("", descriptor.consumes)
            assertEquals(1, descriptor.arguments.size)
            assertEquals("numeric", descriptor.arguments[0].argumentType)
            assertEquals("page", descriptor.arguments[0].argumentName)
            assertEquals("Query", descriptor.arguments[0].argumentSource)
        } finally {
            dialog.close(0)
        }
    }

    @Test
    fun testDialogValidation() {
        val dialog = CFMLAddRestEndpointDialog(project)
        try {
            dialog.setFunctionName("")
            val validation = dialog.doValidate()
            assertNotNull("Validation should fail when function name is empty", validation)
            assertTrue(validation!!.message.contains("Function Name"))
        } finally {
            dialog.close(0)
        }
    }

    @Test
    fun testDialogDefaultValues() {
        val dialog = CFMLAddRestEndpointDialog(project)
        try {
            val descriptor = dialog.getEndpointDescriptor()
            assertEquals("", descriptor.functionName)
            assertEquals("GET", descriptor.httpMethod)
            assertEquals("/", descriptor.restPath)
            assertEquals("application/json", descriptor.produces)
            assertEquals("", descriptor.consumes)
            assertTrue(descriptor.arguments.isEmpty())
        } finally {
            dialog.close(0)
        }
    }

    @Test
    fun testDialogMultipleArgumentsAndSources() {
        val dialog = CFMLAddRestEndpointDialog(project)
        try {
            dialog.setFunctionName("updateProfile")
            dialog.setHttpMethod("UPDATE")
            dialog.setRestPath("/profile/{userId}")
            dialog.setProduces("application/json")
            dialog.setConsumes("application/json")

            dialog.addArgument("string", "userId", "Path")
            dialog.addArgument("string", "authToken", "Header")
            dialog.addArgument("string", "sessionId", "Cookie")
            dialog.addArgument("struct", "formData", "Form")
            dialog.addArgument("string", "version", "Matrix")
            dialog.addArgument("numeric", "debug", "Query")

            val descriptor = dialog.getEndpointDescriptor()
            assertEquals("UPDATE", descriptor.httpMethod)
            assertEquals(6, descriptor.arguments.size)
            assertEquals("userId", descriptor.arguments[0].argumentName)
            assertEquals("Path", descriptor.arguments[0].argumentSource)
            assertEquals("authToken", descriptor.arguments[1].argumentName)
            assertEquals("Header", descriptor.arguments[1].argumentSource)
            assertEquals("sessionId", descriptor.arguments[2].argumentName)
            assertEquals("Cookie", descriptor.arguments[2].argumentSource)
            assertEquals("formData", descriptor.arguments[3].argumentName)
            assertEquals("Form", descriptor.arguments[3].argumentSource)
            assertEquals("version", descriptor.arguments[4].argumentName)
            assertEquals("Matrix", descriptor.arguments[4].argumentSource)
            assertEquals("debug", descriptor.arguments[5].argumentName)
            assertEquals("Query", descriptor.arguments[5].argumentSource)

            val generatedCode = CFMLRestEndpointGenerator.generateFunction(descriptor)
            assertTrue(generatedCode.contains("string userId restArgSource=\"Path\""))
            assertTrue(generatedCode.contains("string authToken restArgSource=\"Header\""))
            assertTrue(generatedCode.contains("string sessionId restArgSource=\"Cookie\""))
            assertTrue(generatedCode.contains("struct formData restArgSource=\"Form\""))
            assertTrue(generatedCode.contains("string version restArgSource=\"Matrix\""))
            assertTrue(generatedCode.contains("numeric debug restArgSource=\"Query\""))
            assertTrue(generatedCode.contains("httpMethod=\"UPDATE\""))
            assertTrue(generatedCode.contains("restPath=\"/profile/{userId}\""))
            assertTrue(generatedCode.contains("produces=\"application/json\""))
            assertTrue(generatedCode.contains("consumes=\"application/json\""))
        } finally {
            dialog.close(0)
        }
    }
}
