package com.quetwo.intellilucee.structure

import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.psi.CFMLPsiFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLStructureViewTest : BasePlatformTestCase() {

    @Test
    fun testStructureViewRegistration() {
        val file = myFixture.configureByText("test.cfm", "<cfset x = 1>") as CFMLPsiFile
        val builder = LanguageStructureViewBuilder.getInstance().getStructureViewBuilder(file)
        assertNotNull("StructureViewBuilder should be registered for CFML", builder)
    }

    @Test
    fun testHtmlAndCFMLTagsHierarchy() {
        val code = """
            <!DOCTYPE html>
            <html>
                <head>
                    <title>My Page</title>
                </head>
                <body class="main-body">
                    <div id="content" class="container fluid">
                        <h1>Header</h1>
                        <cfquery name="getUsers" datasource="dsn">
                            SELECT * FROM users
                        </cfquery>
                        <cfloop query="getUsers">
                            <div class="user-row">
                                <cfoutput>#getUsers.name#</cfoutput>
                            </div>
                        </cfloop>
                        <input type="text" name="search" />
                    </div>
                </body>
            </html>
        """.trimIndent()

        val file = myFixture.configureByText("page.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        // Top level should have html
        assertTrue("Expected top-level HTML tags", children.isNotEmpty())
        val htmlElement = children.find { it.presentation.presentableText?.startsWith("html") == true }
        assertNotNull("html tag should be in root children", htmlElement)

        val htmlChildren = htmlElement!!.children
        val headElement = htmlChildren.find { it.presentation.presentableText?.startsWith("head") == true }
        val bodyElement = htmlChildren.find { it.presentation.presentableText?.startsWith("body") == true }
        assertNotNull("head tag should be under html", headElement)
        assertNotNull("body tag should be under html", bodyElement)
        assertEquals("body.main-body", bodyElement!!.presentation.presentableText)

        val bodyChildren = bodyElement.children
        val divContent = bodyChildren.find { it.presentation.presentableText?.startsWith("div#content") == true }
        assertNotNull("div#content should be under body", divContent)
        assertEquals("div#content.container", divContent!!.presentation.presentableText)

        val divChildren = divContent.children
        val h1Element = divChildren.find { it.presentation.presentableText == "h1" }
        val queryElement = divChildren.find { it.presentation.presentableText == "cfquery" }
        val loopElement = divChildren.find { it.presentation.presentableText == "cfloop" }
        val inputElement = divChildren.find { it.presentation.presentableText?.startsWith("input") == true }

        assertNotNull("h1 should be under div#content", h1Element)
        assertNotNull("cfquery should be under div#content", queryElement)
        assertEquals("getUsers", queryElement!!.presentation.locationString)

        assertNotNull("cfloop should be under div#content", loopElement)
        val loopChildren = loopElement!!.children
        val userRowDiv = loopChildren.find { it.presentation.presentableText?.startsWith("div.user-row") == true }
        assertNotNull("div.user-row should be under cfloop", userRowDiv)

        val userRowChildren = userRowDiv!!.children
        val outputElement = userRowChildren.find { it.presentation.presentableText == "cfoutput" }
        assertNotNull("cfoutput should be under div.user-row", outputElement)

        assertNotNull("input element should be under div#content", inputElement)
        assertEquals("name=\"search\"", inputElement!!.presentation.locationString)
    }

    @Test
    fun testCffunctionStructure() {
        val code = """
            <cfcomponent>
                <cffunction name="getUserById" access="public" returntype="struct">
                    <cfargument name="id" type="numeric" required="true">
                    <cfreturn { id: arguments.id, name: "Test" }>
                </cffunction>
                <cffunction name="privateHelper" access="private">
                    <cfset var x = 1>
                </cffunction>
            </cfcomponent>
        """.trimIndent()

        val file = myFixture.configureByText("User.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val compElement = children.find { it.presentation.presentableText == "cfcomponent" }
        assertNotNull("cfcomponent should be present", compElement)

        val compChildren = compElement!!.children
        val getFunc = compChildren.find { it.presentation.presentableText?.contains("getUserById") == true }
        val privateFunc = compChildren.find { it.presentation.presentableText?.contains("privateHelper") == true }

        assertNotNull("getUserById should be under cfcomponent", getFunc)
        assertNotNull("privateHelper should be under cfcomponent", privateFunc)
        assertEquals(": struct", getFunc!!.presentation.locationString)
    }

    @Test
    fun testScriptFunctionsInStructureView() {
        val code = """
            <cfscript>
                function calculateTotal(numeric price, numeric tax) {
                    return price + tax;
                }
            </cfscript>
        """.trimIndent()

        val file = myFixture.configureByText("calculator.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val cfscriptElement = children.find { it.presentation.presentableText == "cfscript" }
        assertNotNull("cfscript tag should be found", cfscriptElement)

        val scriptChildren = cfscriptElement!!.children
        val calcFunc = scriptChildren.find { it.presentation.presentableText?.startsWith("calculateTotal") == true }
        assertNotNull("calculateTotal function should be found under cfscript", calcFunc)
    }

    @Test
    fun testSelfClosingAndVoidTags() {
        val code = """
            <div>
                <img src="test.png" id="avatar" class="rounded" />
                <br>
                <hr>
                <p>Paragraph text</p>
            </div>
        """.trimIndent()

        val file = myFixture.configureByText("void.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        assertEquals(1, children.size)
        val div = children[0]
        assertEquals("div", div.presentation.presentableText)

        val divChildren = div.children
        val names = divChildren.map { it.presentation.presentableText }
        assertEquals("Expected [img#avatar.rounded, br, hr, p] but got $names", 4, divChildren.size)
        assertEquals("img#avatar.rounded", divChildren[0].presentation.presentableText)
        assertEquals("br", divChildren[1].presentation.presentableText)
        assertEquals("hr", divChildren[2].presentation.presentableText)
        assertEquals("p", divChildren[3].presentation.presentableText)
    }

    @Test
    fun testCustomTagsAndBooleanAttributes() {
        val code = """
            <form id="loginForm" method="post" novalidate>
                <input type="text" name="username" required disabled />
                <cf_customheader title="Login" />
                <button type="submit" class="btn btn-primary">Submit</button>
            </form>
        """.trimIndent()

        val file = myFixture.configureByText("form.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        assertEquals(1, children.size)
        val form = children[0]
        assertEquals("form#loginForm", form.presentation.presentableText)

        val formChildren = form.children
        assertEquals(3, formChildren.size)
        assertEquals("input", formChildren[0].presentation.presentableText)
        assertEquals("name=\"username\"", formChildren[0].presentation.locationString)
        assertEquals("cf_customheader", formChildren[1].presentation.presentableText)
        assertEquals("button.btn", formChildren[2].presentation.presentableText)
    }

    @Test
    fun testNavigation() {
        val code = """
            <html>
                <body id="main">
                    <cffunction name="testFn">
                    </cffunction>
                </body>
            </html>
        """.trimIndent()

        val file = myFixture.configureByText("nav.cfm", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val htmlTree = rootElement.children[0] as CFMLStructureViewElement
        val bodyTree = htmlTree.children[0] as CFMLStructureViewElement
        val fnTree = bodyTree.children[0] as CFMLStructureViewElement

        assertTrue(htmlTree.canNavigate())
        assertTrue(bodyTree.canNavigate())
        assertTrue(fnTree.canNavigate())
        assertTrue(htmlTree.canNavigateToSource())
    }

    @Test
    fun testCfcTagStructureWithVariablesAndFunctions() {
        val code = """
            <cfcomponent extends="BaseUser">
                <cfproperty name="id" type="numeric">
                <cfset this.publicName = "John">
                <cfset variables.privateSecret = "hidden">
                <cffunction name="getUser" access="public" returntype="struct">
                    <cfargument name="userId" type="numeric">
                    <cfset var localVal = 10>
                    <cfreturn { id: arguments.userId }>
                </cffunction>
                <cffunction name="checkAuth" access="private">
                </cffunction>
            </cfcomponent>
        """.trimIndent()

        val file = myFixture.configureByText("User.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val compElement = children.find { it.presentation.presentableText == "cfcomponent" }
        assertNotNull("cfcomponent should be present", compElement)
        assertEquals("extends BaseUser", compElement!!.presentation.locationString)

        val compChildren = compElement.children
        val names = compChildren.map { it.presentation.presentableText }

        assertTrue("id property should be listed", names.contains("id"))
        assertTrue("publicName variable should be listed", names.contains("publicName"))
        assertTrue("privateSecret variable should be listed", names.contains("privateSecret"))
        assertTrue("getUser function should be listed", names.any { it?.startsWith("getUser") == true })
        assertTrue("checkAuth function should be listed", names.any { it?.startsWith("checkAuth") == true })
        assertFalse("localVal should not be in component children", names.contains("localVal"))

        val idProp = compChildren.find { it.presentation.presentableText == "id" }
        assertEquals(": numeric", idProp!!.presentation.locationString)
        assertEquals(0, idProp.children.size)

        val pubVar = compChildren.find { it.presentation.presentableText == "publicName" }
        assertEquals("public", pubVar!!.presentation.locationString)
        assertEquals(0, pubVar.children.size)

        val privVar = compChildren.find { it.presentation.presentableText == "privateSecret" }
        assertEquals("private", privVar!!.presentation.locationString)
        assertEquals(0, privVar.children.size)

        val getUserFn = compChildren.find { it.presentation.presentableText?.startsWith("getUser") == true }
        assertEquals("getUser(userId)", getUserFn!!.presentation.presentableText)
        assertEquals(": struct", getUserFn.presentation.locationString)
        assertEquals(0, getUserFn.children.size)

        val checkAuthFn = compChildren.find { it.presentation.presentableText?.startsWith("checkAuth") == true }
        assertEquals("checkAuth()", checkAuthFn!!.presentation.presentableText)
        assertEquals("private", checkAuthFn.presentation.locationString)
        assertEquals(0, checkAuthFn.children.size)
    }

    @Test
    fun testCfcScriptStructureWithVariablesAndFunctions() {
        val code = """
            component extends="AppBase" {
                property string title;
                property name="count" type="numeric";
                this.role = "admin";
                variables.apiKey = "secret";

                public void function save(numeric id, string name) {
                    var innerX = 1;
                }

                private boolean function isAuthorized() {
                    return true;
                }
            }
        """.trimIndent()

        val file = myFixture.configureByText("Service.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val compElement = children.find { it.presentation.presentableText == "component" }
        assertNotNull("component should be present", compElement)
        assertEquals("extends AppBase", compElement!!.presentation.locationString)

        val compChildren = compElement.children
        val names = compChildren.map { it.presentation.presentableText }

        assertTrue("title property should be listed", names.contains("title"))
        assertTrue("count property should be listed", names.contains("count"))
        assertTrue("role variable should be listed", names.contains("role"))
        assertTrue("apiKey variable should be listed", names.contains("apiKey"))
        assertTrue("save function should be listed", names.any { it?.startsWith("save") == true })
        assertTrue("isAuthorized function should be listed", names.any { it?.startsWith("isAuthorized") == true })
        assertFalse("innerX should not be in component children", names.contains("innerX"))

        val titleProp = compChildren.find { it.presentation.presentableText == "title" }
        assertEquals(": string", titleProp!!.presentation.locationString)
        assertEquals(0, titleProp.children.size)

        val countProp = compChildren.find { it.presentation.presentableText == "count" }
        assertEquals(": numeric", countProp!!.presentation.locationString)
        assertEquals(0, countProp.children.size)

        val roleVar = compChildren.find { it.presentation.presentableText == "role" }
        assertEquals("public", roleVar!!.presentation.locationString)
        assertEquals(0, roleVar.children.size)

        val apiKeyVar = compChildren.find { it.presentation.presentableText == "apiKey" }
        assertEquals("private", apiKeyVar!!.presentation.locationString)
        assertEquals(0, apiKeyVar.children.size)

        val saveFn = compChildren.find { it.presentation.presentableText?.startsWith("save") == true }
        assertEquals("save(id, name)", saveFn!!.presentation.presentableText)
        assertEquals(": void", saveFn.presentation.locationString)
        assertEquals(0, saveFn.children.size)

        val authFn = compChildren.find { it.presentation.presentableText?.startsWith("isAuthorized") == true }
        assertEquals("isAuthorized()", authFn!!.presentation.presentableText)
        assertEquals(": boolean", authFn.presentation.locationString)
        assertEquals(0, authFn.children.size)
    }

    @Test
    fun testCfcScriptWithoutComponentWrapper() {
        val code = """
            property string name;
            this.status = "active";
            variables.logLevel = "DEBUG";

            public function run() {
                var localI = 0;
            }
        """.trimIndent()

        val file = myFixture.configureByText("Task.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val children = rootElement.children

        val names = children.map { it.presentation.presentableText }
        assertTrue("name property should be in root", names.contains("name"))
        assertTrue("status variable should be in root", names.contains("status"))
        assertTrue("logLevel variable should be in root", names.contains("logLevel"))
        assertTrue("run function should be in root", names.any { it?.startsWith("run") == true })
        assertFalse("localI should not be in root", names.contains("localI"))

        val runFn = children.find { it.presentation.presentableText?.startsWith("run") == true } as CFMLStructureViewElement
        assertEquals("run()", runFn.presentation.presentableText)
        assertEquals(0, runFn.children.size)

        // Verify navigation
        assertTrue(runFn.canNavigate())
        assertTrue(runFn.canNavigateToSource())
        val psiElem = runFn.value as CFMLStructurePsiElement
        assertEquals("run", file.text.substring(psiElem.textOffset, psiElem.textOffset + 3))
        assertNotNull(psiElem.navigationElement)
        runFn.navigate(true)
        assertEquals(psiElem.textOffset, myFixture.editor.caretModel.offset)
    }

    @Test
    fun testFunctionNavigationInCfc() {
        val code = """
            <cfcomponent>
                <cffunction name="myMethod" access="public">
                </cffunction>
            </cfcomponent>
        """.trimIndent()

        val file = myFixture.configureByText("Nav.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val compElement = rootElement.children[0] as CFMLStructureViewElement
        val fnElement = compElement.children.find { it.presentation.presentableText?.startsWith("myMethod") == true } as CFMLStructureViewElement

        assertTrue(fnElement.canNavigate())
        assertTrue(fnElement.canNavigateToSource())

        val psi = fnElement.value as CFMLStructurePsiElement
        assertEquals(file, psi.containingFile)
        assertEquals(file, psi.parent)
        assertNotNull(psi.navigationElement)
        val nameAtOffset = file.text.substring(psi.textOffset, psi.textOffset + "myMethod".length)
        assertEquals("myMethod", nameAtOffset)

        fnElement.navigate(true)
        assertEquals(psi.textOffset, myFixture.editor.caretModel.offset)
    }

    @Test
    fun testVariableAndPropertyNavigationInCfc() {
        val code = """
            component {
                property string username;
                this.isActive = true;

                public function doWork() {
                }
            }
        """.trimIndent()

        val file = myFixture.configureByText("User.cfc", code) as CFMLPsiFile
        val rootElement = CFMLStructureViewElement(file)
        val compElement = rootElement.children[0] as CFMLStructureViewElement
        val propElement = compElement.children.find { it.presentation.presentableText == "username" } as CFMLStructureViewElement
        val varElement = compElement.children.find { it.presentation.presentableText == "isActive" } as CFMLStructureViewElement
        val fnElement = compElement.children.find { it.presentation.presentableText?.startsWith("doWork") == true } as CFMLStructureViewElement

        propElement.navigate(true)
        val propPsi = propElement.value as CFMLStructurePsiElement
        assertEquals(propPsi.textOffset, myFixture.editor.caretModel.offset)
        assertEquals("username", file.text.substring(propPsi.textOffset, propPsi.textOffset + "username".length))

        varElement.navigate(true)
        val varPsi = varElement.value as CFMLStructurePsiElement
        assertEquals(varPsi.textOffset, myFixture.editor.caretModel.offset)
        assertEquals("isActive", file.text.substring(varPsi.textOffset, varPsi.textOffset + "isActive".length))

        fnElement.navigate(true)
        val fnPsi = fnElement.value as CFMLStructurePsiElement
        assertEquals(fnPsi.textOffset, myFixture.editor.caretModel.offset)
        assertEquals("doWork", file.text.substring(fnPsi.textOffset, fnPsi.textOffset + "doWork".length))
    }
}
