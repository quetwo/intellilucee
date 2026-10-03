package com.quetwo.intellilucee.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.psi.CFMLPsiUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLRenameTest : BasePlatformTestCase() {

    @Test
    fun testRenameFunctionAtDeclaration() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function cal<caret>culateTotal(a, b) {
                    return a + b;
                }

                function run() {
                    var sum = calculateTotal(1, 2);
                    var total = calculateTotal(3, 4);
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("computeSum")

        myFixture.checkResult(
            """
            component {
                function computeSum(a, b) {
                    return a + b;
                }

                function run() {
                    var sum = computeSum(1, 2);
                    var total = computeSum(3, 4);
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameFunctionAtCallSite() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function calculateTotal(a, b) {
                    return a + b;
                }

                function run() {
                    var sum = cal<caret>culateTotal(1, 2);
                    var total = calculateTotal(3, 4);
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("computeSum")

        myFixture.checkResult(
            """
            component {
                function computeSum(a, b) {
                    return a + b;
                }

                function run() {
                    var sum = computeSum(1, 2);
                    var total = computeSum(3, 4);
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameLocalVariable() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function process() {
                    var user<caret>Count = 10;
                    userCount = userCount + 1;
                    return userCount;
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("totalUsers")

        myFixture.checkResult(
            """
            component {
                function process() {
                    var totalUsers = 10;
                    totalUsers = totalUsers + 1;
                    return totalUsers;
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameLocalVariableFromUsage() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function process() {
                    var userCount = 10;
                    user<caret>Count = userCount + 1;
                    return userCount;
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("totalUsers")

        myFixture.checkResult(
            """
            component {
                function process() {
                    var totalUsers = 10;
                    totalUsers = totalUsers + 1;
                    return totalUsers;
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameFunctionParameter() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function greet(string user<caret>Name) {
                    var msg = "Hello " & userName;
                    return userName;
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("personName")

        myFixture.checkResult(
            """
            component {
                function greet(string personName) {
                    var msg = "Hello " & personName;
                    return personName;
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameScriptPageVariable() {
        myFixture.configureByText(
            "test.cfs",
            """
            var api<caret>Key = "12345";
            writeOutput(apiKey);
            writeOutput("Key: " & apiKey);
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("secretToken")

        myFixture.checkResult(
            """
            var secretToken = "12345";
            writeOutput(secretToken);
            writeOutput("Key: " & secretToken);
            """.trimIndent()
        )
    }

    @Test
    fun testRenameCaseInsensitiveReferences() {
        myFixture.configureByText(
            "test.cfs",
            """
            function doWork() {
                return true;
            }

            do<caret>Work();
            DOWORK();
            DoWork();
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("performTask")

        myFixture.checkResult(
            """
            function performTask() {
                return true;
            }

            performTask();
            performTask();
            performTask();
            """.trimIndent()
        )
    }

    @Test
    fun testRenameScopedVariableUsages() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function run(string my<caret>Param) {
                    var myLocal = 1;
                    writeOutput(arguments.myParam);
                    writeOutput(myParam);
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("newParam")

        myFixture.checkResult(
            """
            component {
                function run(string newParam) {
                    var myLocal = 1;
                    writeOutput(arguments.newParam);
                    writeOutput(newParam);
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameLocalScopedVariable() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function calculate() {
                    var re<caret>sult = 42;
                    writeOutput(local.result);
                    writeOutput(result);
                    return result;
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("outputValue")

        myFixture.checkResult(
            """
            component {
                function calculate() {
                    var outputValue = 42;
                    writeOutput(local.outputValue);
                    writeOutput(outputValue);
                    return outputValue;
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameForInLoopVariable() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function iterate(items) {
                    for (it<caret>em in items) {
                        writeOutput(item);
                    }
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("element")

        myFixture.checkResult(
            """
            component {
                function iterate(items) {
                    for (element in items) {
                        writeOutput(element);
                    }
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testRenameCatchVariable() {
        myFixture.configureByText(
            "test.cfc",
            """
            component {
                function handle() {
                    try {
                        doSomething();
                    } catch (any e<caret>x) {
                        writeOutput(ex.message);
                    }
                }
            }
            """.trimIndent()
        )

        myFixture.renameElementAtCaret("err")

        myFixture.checkResult(
            """
            component {
                function handle() {
                    try {
                        doSomething();
                    } catch (any err) {
                        writeOutput(err.message);
                    }
                }
            }
            """.trimIndent()
        )
    }
}
