package com.quetwo.intellilucee.parser

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLParserTest : BasePlatformTestCase() {

    @Test
    fun testParseScriptComponent() {
        val file = myFixture.configureByText(
            "MyComponent.cfc",
            """
            component extends="BaseComponent" implements="MyInterface" {
                property name="id" type="numeric";
                property name="name" type="string";

                public void function init(numeric id, string name = "default") {
                    this.id = arguments.id;
                    this.name = arguments.name;
                }

                public numeric function calculate(numeric a, numeric b) {
                    var result = a + b * 2;
                    if (result > 100) {
                        return result;
                    } else if (result == 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
            """.trimIndent()
        )

        assertNotNull(file)
        val text = file.text
        assertTrue(text.contains("BaseComponent"))
        assertNotNull(file.node)
        assertTrue(file.node.getChildren(null).isNotEmpty())
    }

    @Test
    fun testParseTagComponent() {
        val file = myFixture.configureByText(
            "TagComponent.cfc",
            """
            <cfcomponent displayname="TagComponent">
                <cfproperty name="title" type="string">
                <cffunction name="getTitle" access="public" returntype="string">
                    <cfargument name="prefix" type="string" required="false" default="">
                    <cfset var fullTitle = arguments.prefix & variables.title>
                    <cfreturn fullTitle>
                </cffunction>
            </cfcomponent>
            """.trimIndent()
        )

        assertNotNull(file)
        assertNotNull(file.node)
        assertTrue(file.node.getChildren(null).isNotEmpty())
    }

    @Test
    fun testParseScriptPage() {
        val file = myFixture.configureByText(
            "test.cfs",
            """
            import my.package.Service;
            include "helpers.cfm";

            var data = [1, 2, 3];
            var map = { "a": 1, "b": 2 };
            var emptyMap = [:];

            try {
                for (var item in data) {
                    writeOutput(item);
                }
            } catch (any e) {
                writeOutput("Error: " & e.message);
            } finally {
                writeOutput("Done");
            }
            """.trimIndent()
        )

        assertNotNull(file)
        assertNotNull(file.node)
    }

    @Test
    fun testParseTagPageWithEmbeddedHtmlAndExpressions() {
        val file = myFixture.configureByText(
            "index.cfm",
            """
            <!--- Header comment --->
            <cfoutput>
                <div class="container">
                    <h1>Welcome, #session.username#!</h1>
                    <cfif session.isLoggedIn>
                        <p>User ID: #session.userId#</p>
                    <cfelse>
                        <p>Please log in.</p>
                    </cfif>
                </div>
            </cfoutput>
            """.trimIndent()
        )

        assertNotNull(file)
        assertNotNull(file.node)
    }
}
