package com.quetwo.intellilucee.editor

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.settings.CFMLGlobalSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLAnnotatorTest : BasePlatformTestCase()
{

    override fun tearDown()
    {
        try
        {
            CFMLGlobalSettings.getInstance().state.syntaxAndErrorHighlighting = true
        }
        finally
        {
            super.tearDown()
        }
    }

    @Test
    fun testValidCFMLCodeProducesNoErrors()
    {
        val code = """
            <cfcomponent displayname="UserService">
                <cffunction name="getUser" access="public" returntype="struct">
                    <cfargument name="userId" type="numeric" required="true">
                    <cfargument name="includeDetails" type="boolean" default="false">
                    <cfset var user = { id: arguments.userId, name: "John Doe" }>
                    <cfif arguments.includeDetails>
                        <cfoutput>Details: #user.name#</cfoutput>
                    <cfelse>
                        <cfoutput>#user.id#</cfoutput>
                    </cfif>
                    <cfreturn user>
                </cffunction>
            </cfcomponent>
        """.trimIndent()

        myFixture.configureByText("valid.cfc", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors in valid CFML code, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testUnclosedTagError()
    {
        val code = """
            <cfif condition>
                <cfset x = 1>
        """.trimIndent()

        myFixture.configureByText("unclosed.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report unclosed tag error", errors.any { it.description != null && it.description.contains("Unclosed tag '<cfif>'") })
    }

    @Test
    fun testUnmatchedClosingTagError()
    {
        val code = """
            <cfset x = 1>
            </cfif>
        """.trimIndent()

        myFixture.configureByText("unmatched.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report unmatched closing tag error", errors.any { it.description != null && it.description.contains("Unmatched closing tag '</cfif>'") })
    }

    @Test
    fun testMismatchedClosingTagError()
    {
        val code = """
            <cfif condition>
                <cfset x = 1>
            </cffunction>
        """.trimIndent()

        myFixture.configureByText("mismatched.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report mismatched closing tag error", errors.any { it.description != null && it.description.contains("Mismatched closing tag '</cffunction>', expected '</cfif>'") })
    }

    @Test
    fun testContextRestrictedTagsErrors()
    {
        val code = """
            <cfelse>
            <cfcatch>
            <cfcase value="1">
            <cfargument name="arg1">
        """.trimIndent()

        myFixture.configureByText("bad_context.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report cfelse outside cfif error", errors.any { it.description != null && it.description.contains("<cfelse>' must be inside a '<cfif>' tag") })
        assertTrue("Should report cfcatch outside cftry error", errors.any { it.description != null && it.description.contains("<cfcatch>' must be inside a '<cftry>' tag") })
        assertTrue("Should report cfcase outside cfswitch error", errors.any { it.description != null && it.description.contains("<cfcase>' must be inside a '<cfswitch>' tag") })
        assertTrue("Should report cfargument outside cffunction error", errors.any { it.description != null && it.description.contains("<cfargument>' must be inside a '<cffunction>' tag") })
    }

    @Test
    fun testMissingRequiredAttributeError()
    {
        val code = """
            <cffunction access="public">
                <cfargument type="string">
            </cffunction>
        """.trimIndent()

        myFixture.configureByText("missing_attr.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report missing name attribute on cffunction", errors.any { it.description != null && it.description.contains("Tag '<cffunction>' requires a 'name' attribute") })
        assertTrue("Should report missing name attribute on cfargument", errors.any { it.description != null && it.description.contains("Tag '<cfargument>' requires a 'name' attribute") })
    }

    @Test
    fun testDuplicateFunctionArgumentError()
    {
        val code = """
            function calculate(a, b, a) {
                return a + b;
            }
        """.trimIndent()

        myFixture.configureByText("dup_arg.cfs", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report duplicate argument error", errors.any { it.description != null && it.description.contains("Duplicate argument 'a' in function 'calculate'") })
    }

    @Test
    fun testUnclosedHashExpressionError()
    {
        val code = """
            <cfset msg = "Hello #name and more">
        """.trimIndent()

        myFixture.configureByText("unclosed_hash.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report unclosed hash expression error", errors.any { it.description != null && it.description.contains("Unclosed hash expression") })
    }

    @Test
    fun testDisabledSyntaxAndErrorHighlightingSuppressesAnnotations()
    {
        CFMLGlobalSettings.getInstance().state.syntaxAndErrorHighlighting = false

        val code = """
            <cfif condition>
                <cfset msg = "Hello #name">
            </cffunction>
        """.trimIndent()

        myFixture.configureByText("disabled.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("No errors should be reported when syntax and error highlighting is disabled", errors.isEmpty())
    }
}
