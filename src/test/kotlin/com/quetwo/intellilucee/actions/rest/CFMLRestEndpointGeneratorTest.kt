package com.quetwo.intellilucee.actions.rest

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.editor.rest.CFMLRestEndpointGenerator
import com.quetwo.intellilucee.editor.rest.RestArgument
import com.quetwo.intellilucee.editor.rest.RestEndpointDescriptor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLRestEndpointGeneratorTest : BasePlatformTestCase() {

    @Test
    fun testIsRestComponent_ScriptTrue() {
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("component rest=\"true\" restPath=\"/api\" {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("component rest='true' {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("component rest=true {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("component rest=\"yes\" {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("component rest {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("/* comment */\ncomponent rest=\"true\" {\n}"))
    }

    @Test
    fun testIsRestComponent_DocComment() {
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("/**\n * @rest true\n */\ncomponent {\n}"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("/**\n * @rest \"true\"\n */\ncomponent {\n}"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("/**\n * @rest false\n */\ncomponent {\n}"))
    }

    @Test
    fun testIsRestComponent_ScriptFalseOrMissing() {
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("component rest=\"false\" {\n}"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("component rest=false {\n}"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("component rest=\"no\" {\n}"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("component {\n    var rest = \"true\";\n}"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("component accessors=\"true\" {\n}"))
    }

    @Test
    fun testIsRestComponent_TagTrue() {
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest=\"true\" restPath=\"/api\">\n</cfcomponent>"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest='true'>\n</cfcomponent>"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest=true>\n</cfcomponent>"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest=\"yes\">\n</cfcomponent>"))
        assertTrue(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest>\n</cfcomponent>"))
    }

    @Test
    fun testIsRestComponent_TagFalseOrMissing() {
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest=\"false\">\n</cfcomponent>"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent rest=false>\n</cfcomponent>"))
        assertFalse(CFMLRestEndpointGenerator.isRestComponent("<cfcomponent displayname=\"Service\">\n</cfcomponent>"))
    }

    @Test
    fun testGenerateFunction_Full() {
        val descriptor = RestEndpointDescriptor(
            functionName = "getUser",
            httpMethod = "GET",
            restPath = "/{id}",
            produces = "application/json",
            consumes = "application/json",
            arguments = listOf(
                RestArgument("string", "id", "Path"),
                RestArgument("numeric", "format", "Query")
            )
        )

        val code = CFMLRestEndpointGenerator.generateFunction(descriptor)
        val expected = """
            remote function getUser(string id restArgSource="Path", numeric format restArgSource="Query") httpMethod="GET" restPath="/{id}" produces="application/json" consumes="application/json"
                {

                    return {};
                }
        """.trimIndent()
        assertEquals(expected, code)
    }

    @Test
    fun testGenerateFunction_OmitBlankOptionalFields() {
        val descriptor = RestEndpointDescriptor(
            functionName = "listAll",
            httpMethod = "GET",
            restPath = "",
            produces = "application/json",
            consumes = "",
            arguments = emptyList()
        )

        val code = CFMLRestEndpointGenerator.generateFunction(descriptor)
        val expected = """
            remote function listAll() httpMethod="GET" produces="application/json"
                {

                    return {};
                }
        """.trimIndent()
        assertEquals(expected, code)
    }

    @Test
    fun testGenerateFunction_NoProducesOrConsumes() {
        val descriptor = RestEndpointDescriptor(
            functionName = "deleteItem",
            httpMethod = "DELETE",
            restPath = "/{id}",
            produces = "",
            consumes = "",
            arguments = listOf(RestArgument("string", "id", "Path"))
        )

        val code = CFMLRestEndpointGenerator.generateFunction(descriptor)
        val expected = """
            remote function deleteItem(string id restArgSource="Path") httpMethod="DELETE" restPath="/{id}"
                {

                    return {};
                }
        """.trimIndent()
        assertEquals(expected, code)
    }

    @Test
    fun testInsertEndpoint_ScriptComponent() {
        val document = EditorFactory.getInstance().createDocument("component rest=\"true\" restpath=\"/users\" {\n}")
        val descriptor = RestEndpointDescriptor(
            functionName = "getUser",
            httpMethod = "GET",
            restPath = "/{id}",
            produces = "application/json",
            consumes = "",
            arguments = listOf(RestArgument("string", "id", "Path"))
        )

        WriteCommandAction.runWriteCommandAction(project) {
            CFMLRestEndpointGenerator.insertEndpoint(document, descriptor)
        }
        val expected = """
            component rest="true" restpath="/users" {

                remote function getUser(string id restArgSource="Path") httpMethod="GET" restPath="/{id}" produces="application/json"
                {

                    return {};
                }
            }
        """.trimIndent()
        assertEquals(expected, document.text)
    }

    @Test
    fun testInsertEndpoint_TagComponent() {
        val document = EditorFactory.getInstance().createDocument("<cfcomponent rest=\"true\">\n</cfcomponent>")
        val descriptor = RestEndpointDescriptor(
            functionName = "createUser",
            httpMethod = "POST",
            restPath = "/",
            produces = "application/json",
            consumes = "application/json",
            arguments = listOf(RestArgument("struct", "body", "Form"))
        )

        WriteCommandAction.runWriteCommandAction(project) {
            CFMLRestEndpointGenerator.insertEndpoint(document, descriptor)
        }
        val expected = """
            <cfcomponent rest="true">

                remote function createUser(struct body restArgSource="Form") httpMethod="POST" restPath="/" produces="application/json" consumes="application/json"
                {

                    return {};
                }
            </cfcomponent>
        """.trimIndent()
        assertEquals(expected, document.text)
    }
}
