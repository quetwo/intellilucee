package com.quetwo.intellilucee.rustcfml

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
class RustCFMLRunProfileStateTest : BasePlatformTestCase()
{
    @Test
    fun testCommandLineArgumentsWorkingDirectoryConsoleAndBrowser()
    {
        val configuration = configuredApplication()
        val environment = environment(configuration)
        val browsedUrls = mutableListOf<String>()
        var processCreated = false
        val handler = TestProcessHandler()
        val state = RustCFMLRunProfileState(
            environment,
            configuration,
            processHandlerFactory = {
                processCreated = true
                handler
            },
            browserLauncher = { url ->
                assertTrue("Browser must open after process creation", processCreated)
                browsedUrls.add(url)
            }
        )

        val commandLine = state.createCommandLine()
        assertEquals(configuration.executablePath, commandLine.exePath)
        assertEquals(
            listOf("--serve", configuration.applicationDirectory, "--port", "18080"),
            commandLine.parametersList.parameters
        )
        assertEquals(configuration.applicationDirectory, commandLine.workDirectory?.path)

        val executionResult = state.execute(DefaultRunExecutor.getRunExecutorInstance(), testRunner())
        assertNotNull(executionResult)
        assertNotNull(executionResult.executionConsole)
        assertSame(handler, executionResult.processHandler)
        assertEquals(listOf("http://localhost:18080"), browsedUrls)

        state.createProcessHandlerForTest()
        assertEquals("Browser should open exactly once", 1, browsedUrls.size)
        handler.startNotify()
        handler.destroyProcess()
        assertTrue(handler.destroyCalled)
        assertTrue(handler.isProcessTerminating || handler.isProcessTerminated)
    }

    @Test
    fun testProcessCreationFailureDoesNotOpenBrowser()
    {
        val configuration = configuredApplication()
        val state = RustCFMLRunProfileState(
            environment(configuration),
            configuration,
            processHandlerFactory = { throw IllegalStateException("process creation failed") },
            browserLauncher = { fail("Browser must not open when process creation fails") }
        )

        val exception = try
        {
            state.createProcessHandlerForTest()
            fail("Expected process creation to fail")
            throw AssertionError("Expected process creation to fail")
        }
        catch (exception: ExecutionException)
        {
            exception
        }
        assertTrue(exception.message!!.contains("process creation failed"))
    }

    private fun configuredApplication(): RustCFMLRunConfiguration
    {
        val root = Path.of(project.basePath!!).also(Files::createDirectories)
        val fixtureDirectory = Files.createTempDirectory(root, "rustcfml-run-state-")
        val application = Files.createDirectory(fixtureDirectory.resolve("application with spaces"))
        val executable = Files.createTempFile(fixtureDirectory, "rust cfml", ".exe").apply { toFile().setExecutable(true) }
        val configurationType = RustCFMLConfigurationType()
        return RustCFMLRunConfiguration(project, configurationType.configurationFactories[0], "Test RustCFML").apply {
            executablePath = executable.toString()
            applicationDirectory = application.toString()
            port = 18080
        }
    }

    private fun environment(configuration: RustCFMLRunConfiguration): ExecutionEnvironment =
        ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configuration).build()

    private fun testRunner(): ProgramRunner<RunnerSettings> = object : ProgramRunner<RunnerSettings> {
        override fun getRunnerId(): String = "RustCFMLTestRunner"
        override fun canRun(executorId: String, profile: RunProfile): Boolean = true
        override fun execute(environment: ExecutionEnvironment) {}
    }

    private class TestProcessHandler : ProcessHandler()
    {
        private val input = ByteArrayOutputStream()
        var destroyCalled = false

        override fun destroyProcessImpl()
        {
            destroyCalled = true
            notifyProcessTerminated(0)
        }

        override fun detachProcessImpl()
        {
            notifyProcessDetached()
        }

        override fun detachIsDefault(): Boolean = false

        override fun getProcessInput(): OutputStream = input
    }
}