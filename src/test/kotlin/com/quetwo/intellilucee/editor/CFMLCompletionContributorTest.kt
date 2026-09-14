package com.quetwo.intellilucee.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLCompletionContributorTest : BasePlatformTestCase()
{

    @Test
    fun testCompletionScriptCreateObject()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/User.cfc",
            """
            component {
                property numeric id;
                property string name;
                variables.status = "active";
                
                public string function getName() {
                    return name;
                }
                
                public void function save(numeric flags) {
                }
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfm",
            """
            <cfscript>
                var user = createObject("component", "models.User");
                user.<caret>
            </cfscript>
            """.trimIndent()
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull("Lookup elements should not be null", lookupElements)

        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("getName"))
        assertTrue(lookupStrings.contains("save"))
        assertTrue(lookupStrings.contains("id"))
        assertTrue(lookupStrings.contains("name"))
        assertTrue(lookupStrings.contains("status"))

        // Ensure CFC methods and variables appear at the top of the list
        val topElements = lookupStrings.take(5)
        assertTrue(topElements.contains("getName"))
        assertTrue(topElements.contains("save"))
    }

    @Test
    fun testCompletionSingleQuotesAndChainedInit()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "services/AuthService.cfc",
            """
            component {
                property boolean isLoggedIn;
                
                function init(numeric timeout) {
                    return this;
                }
                
                function authenticate(string username, string password) {
                    return true;
                }
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfs",
            """
            var auth = createObject('component', 'services.AuthService').init(30);
            auth.<caret>
            """.trimIndent()
        )

        val lookupElements = myFixture.completeBasic()
        assertNotNull(lookupElements)

        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("init"))
        assertTrue(lookupStrings.contains("authenticate"))
        assertTrue(lookupStrings.contains("isLoggedIn"))
    }

    @Test
    fun testCompletionTagBasedCreateObject()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Order.cfc",
            """
            <cfcomponent>
                <cfproperty name="orderId" type="numeric">
                <cfproperty name="total" type="numeric">
                <cfset variables.isProcessed = false>
                
                <cffunction name="processOrder" access="public" returntype="boolean">
                    <cfargument name="discount" type="numeric">
                    <cfreturn true>
                </cffunction>
                
                <cffunction name="cancelOrder" access="public">
                </cffunction>
            </cfcomponent>
            """.trimIndent()
        )

        myFixture.configureByText(
            "orderView.cfm",
            """
            <cfset order = createObject("component", "models.Order")>
            <cfset order.<caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("processOrder"))
        assertTrue(lookupStrings.contains("cancelOrder"))
        assertTrue(lookupStrings.contains("orderId"))
        assertTrue(lookupStrings.contains("total"))
        assertTrue(lookupStrings.contains("isProcessed"))
    }

    @Test
    fun testCompletionScopedVariable()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Logger.cfc",
            """
            component {
                property string level;
                function logInfo(string msg) {}
                function logError(string msg) {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "loggerTest.cfc",
            """
            component {
                function run() {
                    local.logger = createObject("component", "models.Logger");
                    local.logger.<caret>
                }
            }
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("logInfo"))
        assertTrue(lookupStrings.contains("logError"))
        assertTrue(lookupStrings.contains("level"))
    }

    @Test
    fun testCompletionPrefixFiltering()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Customer.cfc",
            """
            component {
                property string customerName;
                property string customerEmail;
                
                function getCustomerName() {}
                function getCustomerEmail() {}
                function saveCustomer() {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfm",
            """
            <cfscript>
                var customer = createObject("component", "models.Customer");
                customer.get<caret>
            </cfscript>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("getCustomerName"))
        assertTrue(lookupStrings.contains("getCustomerEmail"))
        assertFalse(lookupStrings.contains("saveCustomer"))
    }

    @Test
    fun testCompletionNonComponentCreateObjectIgnored()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cfscript>
                var javaString = createObject("java", "java.lang.String");
                javaString.<caret>
            </cfscript>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        // Should not crash and should not have component lookup elements
        val strings = myFixture.lookupElementStrings ?: emptyList()
        assertFalse(strings.contains("java.lang.String"))
    }

    @Test
    fun testCompletionNamedParametersCreateObject()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Product.cfc",
            """
            component {
                property numeric price;
                function calculateTax() {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfm",
            """
            <cfscript>
                var prod = createObject(type="component", component="models.Product");
                prod.<caret>
            </cfscript>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("calculateTax"))
        assertTrue(lookupStrings.contains("price"))
    }

    @Test
    fun testCompletionMultipleVariablesDifferentComponents()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/User.cfc",
            """
            component {
                function getUserName() {}
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "models/Order.cfc",
            """
            component {
                function getOrderId() {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfm",
            """
            <cfscript>
                var user = createObject("component", "models.User");
                var order = createObject("component", "models.Order");
                user.<caret>
            </cfscript>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val userLookup = myFixture.lookupElementStrings
        assertNotNull(userLookup)
        assertTrue(userLookup!!.contains("getUserName"))
        assertFalse(userLookup.contains("getOrderId"))

        myFixture.configureByText(
            "test2.cfm",
            """
            <cfscript>
                var user = createObject("component", "models.User");
                var order = createObject("component", "models.Order");
                order.<caret>
            </cfscript>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val orderLookup = myFixture.lookupElementStrings
        assertNotNull(orderLookup)
        assertTrue(orderLookup!!.contains("getOrderId"))
        assertFalse(orderLookup.contains("getUserName"))
    }

    @Test
    fun testCompletionCfparamCreateObject()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Config.cfc",
            """
            component {
                property string apiKey;
                function loadConfig() {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfm",
            """
            <cfparam name="config" default="#createObject('component', 'models.Config')#">
            <cfset config.<caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("loadConfig"))
        assertTrue(lookupStrings.contains("apiKey"))
    }

    @Test
    fun testSelectMethodWithoutParametersInsertsSignature()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/User.cfc",
            """
            component {
                function getName() {
                    return "John";
                }
                function getAge() {
                    return 30;
                }
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfs",
            """
            var user = createObject("component", "models.User");
            user.<caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val getNameElement = elements!!.first { it.lookupString == "getName" }
        myFixture.lookup.currentItem = getNameElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            var user = createObject("component", "models.User");
            user.getName()<caret>
            """.trimIndent()
        )
    }

    @Test
    fun testSelectMethodWithParametersInsertsSignatureAndPlacesCaretInside()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/User.cfc",
            """
            component {
                function save(numeric id, string name) {
                }
                function saveDraft(string name) {
                }
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfs",
            """
            var user = createObject("component", "models.User");
            user.<caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val saveElement = elements!!.first { it.lookupString == "save" }
        myFixture.lookup.currentItem = saveElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            var user = createObject("component", "models.User");
            user.save(<caret>)
            """.trimIndent()
        )
    }

    @Test
    fun testSelectVariableDoesNotInsertParentheses()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/User.cfc",
            """
            component {
                property string email;
                property string extra;
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfs",
            """
            var user = createObject("component", "models.User");
            user.<caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val emailElement = elements!!.first { it.lookupString == "email" }
        myFixture.lookup.currentItem = emailElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            var user = createObject("component", "models.User");
            user.email<caret>
            """.trimIndent()
        )
    }

    @Test
    fun testAutoCompletesSingleMethodWithSignature()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject(
            "models/Single.cfc",
            """
            component {
                function execute(string command) {}
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "test.cfs",
            """
            var s = createObject("component", "models.Single");
            s.exec<caret>
            """.trimIndent()
        )

        myFixture.completeBasic()

        myFixture.checkResult(
            """
            var s = createObject("component", "models.Single");
            s.execute(<caret>)
            """.trimIndent()
        )
    }
}
