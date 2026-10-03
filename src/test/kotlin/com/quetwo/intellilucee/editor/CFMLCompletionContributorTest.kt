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

    @Test
    fun testAssignmentPopulatesDocumentFunctionsAndVariablesInScript()
    {
        myFixture.configureByText(
            "test.cfs",
            """
            function calculateTotal(numeric rate, numeric amount) {
                return rate * amount;
            }
            function getStoreName() {
                return "MyStore";
            }
            var taxRate = 0.05;
            var finalTotal = <caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("calculateTotal"))
        assertTrue(lookupStrings.contains("getStoreName"))
        assertTrue(lookupStrings.contains("taxRate"))
        assertTrue(lookupStrings.contains("finalTotal"))
    }

    @Test
    fun testAssignmentPopulatesDocumentFunctionsAndVariablesInTag()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cffunction name="formatPrice">
                <cfargument name="val" type="numeric">
                <cfreturn "$#val#">
            </cffunction>
            <cfset basePrice = 100>
            <cfset displayPrice = <caret>>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("formatPrice"))
        assertTrue(lookupStrings.contains("basePrice"))
        assertTrue(lookupStrings.contains("displayPrice"))
    }

    @Test
    fun testAssignmentInFunctionScopesVariablesAndParameters()
    {
        myFixture.configureByText(
            "UserService.cfc",
            """
            component {
                property string appName;

                function processUser(numeric userId, string userName) {
                    var localStatus = "active";
                    var result = <caret>
                }

                function otherFunction(string otherParam) {
                    var otherLocal = 123;
                }
            }
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        // Should contain component functions
        assertTrue(lookupStrings!!.contains("processUser"))
        assertTrue(lookupStrings.contains("otherFunction"))
        // Should contain component property
        assertTrue(lookupStrings.contains("appName"))
        // Should contain current function's parameters and local variables
        assertTrue(lookupStrings.contains("userId"))
        assertTrue(lookupStrings.contains("userName"))
        assertTrue(lookupStrings.contains("localStatus"))
        assertTrue(lookupStrings.contains("result"))
        // Should NOT contain other function's parameters and local variables
        assertFalse(lookupStrings.contains("otherParam"))
        assertFalse(lookupStrings.contains("otherLocal"))
    }

    @Test
    fun testAssignmentSelectFunctionWithParametersInsertsSignature()
    {
        myFixture.configureByText(
            "test.cfs",
            """
            function calculateTotal(numeric rate, numeric amount) {
                return rate * amount;
            }
            var result = <caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val calcElement = elements!!.first { it.lookupString == "calculateTotal" }
        myFixture.lookup.currentItem = calcElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            function calculateTotal(numeric rate, numeric amount) {
                return rate * amount;
            }
            var result = calculateTotal(<caret>)
            """.trimIndent()
        )
    }

    @Test
    fun testAssignmentSelectFunctionWithoutParametersInsertsSignature()
    {
        myFixture.configureByText(
            "test.cfs",
            """
            function getVersion() {
                return "1.0.0";
            }
            var v = <caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val getVerElement = elements!!.first { it.lookupString == "getVersion" }
        myFixture.lookup.currentItem = getVerElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            function getVersion() {
                return "1.0.0";
            }
            var v = getVersion()<caret>
            """.trimIndent()
        )
    }

    @Test
    fun testAssignmentSelectVariableDoesNotInsertParentheses()
    {
        myFixture.configureByText(
            "test.cfs",
            """
            var greeting = "Hello";
            var message = <caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val greetingElement = elements!!.first { it.lookupString == "greeting" }
        myFixture.lookup.currentItem = greetingElement
        myFixture.type('\n')

        myFixture.checkResult(
            """
            var greeting = "Hello";
            var message = greeting<caret>
            """.trimIndent()
        )
    }

    @Test
    fun testAssignmentInCfparamDefaultPopulatesLookup()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cfset defaultTimeout = 30>
            <cfparam name="requestTimeout" default="<caret>">
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("defaultTimeout"))
    }

    @Test
    fun testCompletionCfobjectTagComponentAttribute()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")
        myFixture.addFileToProject("services/AuthService.cfc", "component {}")

        myFixture.configureByText(
            "test.cfm",
            """
            <cfset myVar = 100>
            <cfobject component="<caret>" name="userObj">
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
        assertTrue(lookupStrings.contains("services.AuthService"))
        assertTrue(lookupStrings.contains("Application"))
        // Should only contain components, not unrelated variables
        assertFalse(lookupStrings.contains("myVar"))
    }

    @Test
    fun testCompletionCfobjectTagWithTypeAndComponent()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/Product.cfc", "component {}")

        myFixture.configureByText(
            "test.cfm",
            """
            <cfobject type="component" component="<caret>">
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.Product"))
    }

    @Test
    fun testCompletionCfobjectTagWithSingleQuotes()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/Order.cfc", "component {}")

        myFixture.configureByText(
            "test.cfm",
            """
            <cfobject component='<caret>'>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.Order"))
    }

    @Test
    fun testCompletionCreateComponentPositional()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")
        myFixture.addFileToProject("services/AuthService.cfc", "component {}")

        myFixture.configureByText(
            "test.cfs",
            """
            var otherVar = "hello";
            var comp = createComponent("<caret>");
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
        assertTrue(lookupStrings.contains("services.AuthService"))
        assertTrue(lookupStrings.contains("Application"))
        assertFalse(lookupStrings.contains("otherVar"))
    }

    @Test
    fun testCompletionCreateComponentNamed()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")

        myFixture.configureByText(
            "test.cfs",
            """
            var comp = createComponent(component="<caret>");
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
    }

    @Test
    fun testCompletionCreateComponentTypedPositional()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")

        myFixture.configureByText(
            "test.cfs",
            """
            var comp = createComponent("component", "<caret>");
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
    }

    @Test
    fun testCompletionCreateObjectComponent()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")

        myFixture.configureByText(
            "test.cfs",
            """
            var obj = createObject("component", "<caret>");
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
    }

    @Test
    fun testCompletionScriptCfobject()
    {
        myFixture.addFileToProject("Application.cfc", "component {}")
        myFixture.addFileToProject("models/User.cfc", "component {}")

        myFixture.configureByText(
            "test.cfs",
            """
            cfobject component="<caret>" name="userObj";
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements?.map { it.lookupString }
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("models.User"))
    }

    @Test
    fun testCompletionCfqueryColumnsAndProperties()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <CFQUERY name="userSelect">
            SELECT userID, userHome, userData FROM table1
            </CFQUERY>
            <cfset userSelect.<caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull("Lookup strings should not be null", lookupStrings)
        assertTrue(lookupStrings!!.contains("userID"))
        assertTrue(lookupStrings.contains("userHome"))
        assertTrue(lookupStrings.contains("userData"))
        assertTrue(lookupStrings.contains("currentRow"))
        assertTrue(lookupStrings.contains("recordCount"))
    }

    @Test
    fun testCompletionCfqueryNameAsAvailableVariable()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <CFQUERY name="userSelect">
            SELECT userID, userHome, userData FROM table1
            </CFQUERY>
            <cfset myVar = <caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull("Lookup strings should not be null", lookupStrings)
        assertTrue(lookupStrings!!.contains("userSelect"))
    }

    @Test
    fun testCompletionCfqueryAliasesAndComplexSelect()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cfquery name="orderQuery" datasource="ds">
            <!--- Query comment --->
            SELECT DISTINCT TOP 100
                o.order_id AS orderID,
                COUNT(o.item_id) totalItems,
                [custName],
                `table2`.status,
                CONCAT(first_name, ' ', last_name) AS fullName
            FROM orders o
            JOIN table2 ON o.id = table2.order_id
            WHERE o.active = <cfqueryparam value="1" cfsqltype="cf_sql_integer">
            ORDER BY o.order_id DESC
            </cfquery>
            <cfset orderQuery.<caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("orderID"))
        assertTrue(lookupStrings.contains("totalItems"))
        assertTrue(lookupStrings.contains("custName"))
        assertTrue(lookupStrings.contains("status"))
        assertTrue(lookupStrings.contains("fullName"))
        assertTrue(lookupStrings.contains("currentRow"))
        assertTrue(lookupStrings.contains("recordCount"))
    }

    @Test
    fun testCompletionCfquerySelectStarIncludesCurrentRowAndRecordCount()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cfquery name="allUsers">
            SELECT * FROM users
            </cfquery>
            <cfset allUsers.<caret>>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("currentRow"))
        assertTrue(lookupStrings.contains("recordCount"))
    }

    @Test
    fun testCompletionCfqueryInsideFunction()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cffunction name="fetchData">
                <cfquery name="localQuery">
                SELECT empId, deptCode FROM employees
                </cfquery>
                <cfset localQuery.<caret>>
            </cffunction>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("empId"))
        assertTrue(lookupStrings.contains("deptCode"))
        assertTrue(lookupStrings.contains("currentRow"))
        assertTrue(lookupStrings.contains("recordCount"))
    }

    @Test
    fun testCompletionCfqueryInOutputHashTag()
    {
        myFixture.configureByText(
            "test.cfm",
            """
            <cfquery name="userSelect">
            SELECT userID, userHome, userData FROM table1
            </cfquery>
            <cfoutput>#userSelect.<caret>#</cfoutput>
            """.trimIndent()
        )

        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
        assertNotNull(lookupStrings)
        assertTrue(lookupStrings!!.contains("userID"))
        assertTrue(lookupStrings.contains("userHome"))
        assertTrue(lookupStrings.contains("userData"))
        assertTrue(lookupStrings.contains("currentRow"))
        assertTrue(lookupStrings.contains("recordCount"))
    }

    @Test
    fun testCompletionCfquerySqlColumnExtractorDirect()
    {
        val sql1 = """
            SELECT userID, userHome, userData FROM table1
        """.trimIndent()
        assertEquals(listOf("userID", "userHome", "userData"), com.quetwo.intellilucee.model.CFMLModelParser.extractSqlColumns(sql1))

        val sql2 = """
            SELECT (SELECT max(salary) FROM emp) AS maxSalary, e.id AS empId, e.name, [dept_name] AS deptName
            FROM employees e
            WHERE e.active = 1
        """.trimIndent()
        assertEquals(listOf("maxSalary", "empId", "name", "deptName"), com.quetwo.intellilucee.model.CFMLModelParser.extractSqlColumns(sql2))
    }

    @Test
    fun testLocalVariablesShownAtTopOfCompletionList()
    {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                variables.globalVar = "global";
                
                function myFunc(arg1) {
                    var localVar = "local";
                    var anotherLocal = "local2";
                    var result = <caret>
                }
                
                function otherFunc() {
                }
            }
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements!!.map { it.lookupString }
        
        // Local variables should appear before functions and global variables
        val localVarIndex = lookupStrings.indexOf("localVar")
        val anotherLocalIndex = lookupStrings.indexOf("anotherLocal")
        val arg1Index = lookupStrings.indexOf("arg1")
        val globalVarIndex = lookupStrings.indexOf("globalVar")
        val otherFuncIndex = lookupStrings.indexOf("otherFunc")
        val myFuncIndex = lookupStrings.indexOf("myFunc")

        assertTrue("localVar should be present", localVarIndex >= 0)
        assertTrue("anotherLocal should be present", anotherLocalIndex >= 0)
        assertTrue("arg1 should be present", arg1Index >= 0)
        assertTrue("globalVar should be present", globalVarIndex >= 0)
        assertTrue("otherFunc should be present", otherFuncIndex >= 0)
        assertTrue("myFunc should be present", myFuncIndex >= 0)

        // Local vars (including function arguments / var declarations) should appear before functions and global variables
        assertTrue("localVar ($localVarIndex) should be before otherFunc ($otherFuncIndex)", localVarIndex < otherFuncIndex)
        assertTrue("anotherLocal ($anotherLocalIndex) should be before otherFunc ($otherFuncIndex)", anotherLocalIndex < otherFuncIndex)
        assertTrue("arg1 ($arg1Index) should be before otherFunc ($otherFuncIndex)", arg1Index < otherFuncIndex)
        assertTrue("localVar ($localVarIndex) should be before globalVar ($globalVarIndex)", localVarIndex < globalVarIndex)
        assertTrue("anotherLocal ($anotherLocalIndex) should be before globalVar ($globalVarIndex)", anotherLocalIndex < globalVarIndex)
    }

    @Test
    fun testPageDeclaredVariablesShownInAvailableListAndBeforeFunctions()
    {
        myFixture.configureByText(
            "page.cfm",
            """
            <cfset pageVariable = "hello">
            <cfset anotherPageVar = 123>
            <cffunction name="helperFunction">
                <cfreturn true>
            </cffunction>
            
            <cfset <caret>>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements!!.map { it.lookupString }

        val pageVarIndex = lookupStrings.indexOf("pageVariable")
        val anotherVarIndex = lookupStrings.indexOf("anotherPageVar")
        val helperFuncIndex = lookupStrings.indexOf("helperFunction")

        assertTrue("pageVariable should be present in available list", pageVarIndex >= 0)
        assertTrue("anotherPageVar should be present in available list", anotherVarIndex >= 0)
        assertTrue("helperFunction should be present in available list", helperFuncIndex >= 0)

        // Page variables should be shown before functions
        assertTrue("pageVariable ($pageVarIndex) should be before helperFunction ($helperFuncIndex)", pageVarIndex < helperFuncIndex)
        assertTrue("anotherPageVar ($anotherVarIndex) should be before helperFunction ($helperFuncIndex)", anotherVarIndex < helperFuncIndex)
    }

    @Test
    fun testPageDeclaredVariablesInScriptFile()
    {
        myFixture.configureByText(
            "page.cfs",
            """
            firstVar = "value";
            secondVar = 42;
            
            function pageFunc() {
            }
            
            <caret>
            """.trimIndent()
        )

        val elements = myFixture.completeBasic()
        assertNotNull(elements)
        val lookupStrings = elements!!.map { it.lookupString }

        val firstVarIndex = lookupStrings.indexOf("firstVar")
        val secondVarIndex = lookupStrings.indexOf("secondVar")
        val funcIndex = lookupStrings.indexOf("pageFunc")

        assertTrue("firstVar should be present", firstVarIndex >= 0)
        assertTrue("secondVar should be present", secondVarIndex >= 0)
        assertTrue("pageFunc should be present", funcIndex >= 0)

        assertTrue("firstVar ($firstVarIndex) should be before pageFunc ($funcIndex)", firstVarIndex < funcIndex)
        assertTrue("secondVar ($secondVarIndex) should be before pageFunc ($funcIndex)", secondVarIndex < funcIndex)
    }
}
