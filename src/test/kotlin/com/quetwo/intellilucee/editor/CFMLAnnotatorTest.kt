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
    fun testPoundSignOutsideCfoutputDoesNotProduceError()
    {
        val code = """
            <div>This is item #1 on the list, color is #FF0000, and channel is #general.</div>
            <p>Welcome to our page #</p>
        """.trimIndent()

        myFixture.configureByText("pound_plain_text.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for pound signs outside cfoutput, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testPoundSignInsideCfoutputReportsUnclosedHashError()
    {
        val code = """
            <cfoutput>
                <div>Hello #name and welcome!</div>
            </cfoutput>
        """.trimIndent()

        myFixture.configureByText("unclosed_hash_cfoutput.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report unclosed hash error inside cfoutput", errors.any { it.description != null && it.description.contains("Unclosed hash expression") })
    }

    @Test
    fun testPoundSignInsideCfoutputValidInterpolationProducesNoError()
    {
        val code = """
            <cfoutput>
                <div>Hello #name# and welcome!</div>
                <div>Escaped ## pound signs ##</div>
            </cfoutput>
        """.trimIndent()

        myFixture.configureByText("valid_cfoutput.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for valid hash expressions inside cfoutput, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCustomTagsWithoutClosingTagsDoNotProduceErrors()
    {
        val code = """
            <cfif isTrue>
                <cf_customheader title="Test Page">
                <div>Content here</div>
                <cf_customfooter>
            </cfif>
            <cf_standalone_tag attr1="val1" attr2="val2">
        """.trimIndent()

        myFixture.configureByText("custom_tags_unclosed.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for custom tags without closing tags, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCustomTagsWithClosingTagsProduceNoErrors()
    {
        val code = """
            <cf_my_wrapper param="1">
                <p>Hello world</p>
            </cf_my_wrapper>
        """.trimIndent()

        myFixture.configureByText("custom_tags_paired.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for properly closed custom tags, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testEmptyCfifBlockAndCommentsOnlyProducesNoErrors()
    {
        val code = """
            <cfif condition>
                <!--- just a comment --->
            </cfif>

            <cfif condition2>
            </cfif>

            <cfif condition3>
                <!--- First comment --->
                <!--- Second nested comment <!--- deep ---> --->
            <cfelseif condition4>
                <!--- comment inside cfelseif --->
            <cfelse>
                <!--- comment inside cfelse --->
            </cfif>

            <cfif isSomething>
                <!-- HTML comment inside cfif -->
            </cfif>

            <cfif checkSomething>
                // single line comment
                /* multi line
                   comment */
            </cfif>
        """.trimIndent()

        myFixture.configureByText("empty_cfif.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for cfif with no executable code, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testScriptIfWithEmptyBodyOrCommentsOnlyProducesNoErrors()
    {
        val code = """
            function testIf() {
                if (true) {
                    // comment only
                }

                if (false) {
                    /* block comment only */
                }

                if (condition) {
                } else if (other) {
                    // comment
                } else {
                    /* comment */
                }
            }
        """.trimIndent()

        myFixture.configureByText("empty_script_if.cfs", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for script if with no executable code, but got: $errors", errors.isEmpty())
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

    @Test
    fun testCfexecuteTagWithNoContentBetweenProducesNoErrors()
    {
        val code = """
            <cfexecute name = "#temp_folder#taskkill_cmd#unique#.bat"
              outputFile = "#temp_folder#output2.txt"
              timeout="2">
            </cfexecute>
        """.trimIndent()

        myFixture.configureByText("execute_empty.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for <cfexecute></cfexecute> with no body content, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCfexecuteTagWithBodyContentProducesNoErrors()
    {
        val code = """
            <cfexecute name="my_app.exe" timeout="10" variable="procOutput">
                arg1 arg2 arg3
            </cfexecute>
        """.trimIndent()

        myFixture.configureByText("execute_with_body.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for <cfexecute> with body, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCfexecuteSelfClosingTagProducesNoErrors()
    {
        val code = """
            <cfexecute name="ping" arguments="localhost" timeout="5" />
        """.trimIndent()

        myFixture.configureByText("execute_self_closing.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for self-closing <cfexecute />, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testUnclosedCfexecuteReportsError()
    {
        val code = """
            <cfexecute name="something.bat" timeout="5">
        """.trimIndent()

        myFixture.configureByText("execute_unclosed.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Should report unclosed cfexecute error", errors.any { it.description != null && it.description.contains("Unclosed tag '<cfexecute>'") })
    }

    @Test
    fun testCfqueryparamWithinCfqueryProducesNoErrors()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                SELECT * FROM users
                WHERE id = <cfqueryparam value="123" cfsqltype="cf_sql_integer">
                  AND name = <cfqueryparam value="#John#" cfsqltype="cf_sql_varchar" />
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("queryparam.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for <cfqueryparam> (unclosed or self-closing) in <cfquery>, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testSqlInjectionFromHashInCfqueryReportsError()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                SELECT * FROM users
                WHERE id = #userId#
                  AND name = '#userName#'
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("sqli_hash.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected SQL Injection errors for #...# in cfquery, got: $errors", errors.isNotEmpty())
        val injectionErrors = errors.filter { it.description != null && it.description.contains("SQL Injection Possible") }
        assertEquals(2, injectionErrors.size)
    }

    @Test
    fun testSqlInjectionFromCfoutputInCfqueryReportsError()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                <cfoutput>
                    SELECT * FROM users WHERE id = 1
                </cfoutput>
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("sqli_cfoutput.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected SQL Injection errors for <cfoutput> inside cfquery, got: $errors", errors.isNotEmpty())
        val injectionErrors = errors.filter { it.description != null && it.description.contains("SQL Injection Possible") }
        assertTrue("Should report SQL Injection Possible for cfoutput tags", injectionErrors.size >= 2)
    }

    @Test
    fun testCfqueryWithCfifAndCfqueryparamProducesNoErrors()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                SELECT id, name FROM users
                WHERE 1=1
                <cfif structKeyExists(arguments, "status")>
                    AND status = <cfqueryparam value="#arguments.status#" cfsqltype="cf_sql_varchar">
                </cfif>
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("cfif_queryparam.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for <cfif> with <cfqueryparam> in <cfquery>, got: $errors", errors.isEmpty())
    }

    @Test
    fun testCfqueryWithCfifAndHashInSqlReportsError()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                SELECT id, name FROM users
                WHERE 1=1
                <cfif structKeyExists(arguments, "status")>
                    AND status = '#arguments.status#'
                </cfif>
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("cfif_hash_sqli.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected SQL Injection error for #arguments.status# inside cfif in cfquery, got: $errors",
            errors.any { it.description != null && it.description.contains("SQL Injection Possible") })
    }

    @Test
    fun testCfqueryWithEscapedHashesProducesNoErrors()
    {
        val code = """
            <cfquery name="tempQuery" datasource="myDSN">
                SELECT * FROM ##temp_table
                WHERE tag = 'item ##1'
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("escaped_hash.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for escaped hashes in <cfquery>, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCfquerySqlSyntaxHighlightingApplied()
    {
        val code = """
            <cfquery name="q" datasource="dsn">
                SELECT id, name FROM users WHERE active = 1
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("sql_highlight.cfm", code)
        val highlights = myFixture.doHighlighting()
        val infoHighlights = highlights.filter { it.severity == HighlightSeverity.INFORMATION }
        // Verify information highlights exist for SQL keywords/tokens inside cfquery
        assertTrue("Expected syntax highlighting annotations in cfquery", infoHighlights.isNotEmpty())
    }

    @Test
    fun testCfqueryWithSqlCommentContainingApostropheProducesNoErrors()
    {
        val code = """
            <cfquery name="getUser" datasource="myDSN">
                -- It's a comment with an apostrophe
                SELECT id, name FROM users
                WHERE 1=1
                <cfif structKeyExists(arguments, "status")>
                    AND status = 'active'
                </cfif>
                /* Don't break on block comments either */
                AND type = 'admin'
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("sql_comment_apostrophe.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for SQL comments containing apostrophes in <cfquery>, but got: $errors", errors.isEmpty())
    }

    @Test
    fun testCfqueryWithIfWeveBeenNotifyingComment()
    {
        val code = """
            <cfquery name="qTest" datasource="myDSN">
                SELECT * FROM notifications WHERE count < 10
                -- If we've been notifying
                SELECT id FROM test
            </cfquery>
        """.trimIndent()

        myFixture.configureByText("sql_notifying.cfm", code)
        val highlights = myFixture.doHighlighting()
        val errors = highlights.filter { it.severity == HighlightSeverity.ERROR }
        assertTrue("Expected no errors for <cfquery> with '-- If we\\'ve been notifying', but got: $errors", errors.isEmpty())
    }
}
