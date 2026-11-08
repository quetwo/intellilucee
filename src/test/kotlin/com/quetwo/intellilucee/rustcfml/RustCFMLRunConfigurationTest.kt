package com.quetwo.intellilucee.rustcfml

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.quetwo.intellilucee.debugger.RustCFMLConfigurationType
import com.quetwo.intellilucee.debugger.RustCFMLRunConfiguration
import com.quetwo.intellilucee.debugger.RustCFMLSettingsEditor
import org.jdom.Element
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4::class)
class RustCFMLRunConfigurationTest : BasePlatformTestCase()
{
    @Test
    fun testDefaultsAndEditorRoundTrip()
    {
        val configuration = newConfiguration()
        assertEquals("", configuration.executablePath)
        assertEquals(project.basePath, configuration.applicationDirectory)
        assertEquals(8080, configuration.port)

        configuration.executablePath = "C:/tools/rustcfml.exe"
        configuration.applicationDirectory = "C:/project/site"
        configuration.port = 9000
        val editor = TestSettingsEditor(project)
        editor.reset(configuration)
        editor.apply(configuration)

        assertEquals("C:/tools/rustcfml.exe", configuration.executablePath)
        assertEquals("C:/project/site", configuration.applicationDirectory)
        assertEquals(9000, configuration.port)
    }

    @Test
    fun testJdomPersistence()
    {
        val original = newConfiguration()
        original.executablePath = "C:/tools/rustcfml.exe"
        original.applicationDirectory = "C:/project/site"
        original.port = 12345
        val element = Element("configuration")
        original.writeExternal(element)

        val restored = newConfiguration()
        restored.readExternal(element)

        assertEquals(original.executablePath, restored.executablePath)
        assertEquals(original.applicationDirectory, restored.applicationDirectory)
        assertEquals(original.port, restored.port)
    }

    @Test
    fun testProjectRootAndDescendantDirectoriesAreValid()
    {
        val root = projectRoot()
        val descendant = Files.createTempDirectory(root, "rustcfml-app-")
        val configuration = configuredFor(descendant)

        configuration.applicationDirectory = root.toString()
        configuration.checkConfiguration()
        configuration.applicationDirectory = descendant.toString()
        configuration.checkConfiguration()
    }

    @Test
    fun testRejectsMissingExecutableAndApplicationDirectoryOutsideProject()
    {
        val configuration = newConfiguration()
        configuration.applicationDirectory = projectRoot().toString()
        expectInvalid(configuration)

        configuration.executablePath = executableFile().toString()
        configuration.applicationDirectory = projectRoot().parent.toString()
        expectInvalid(configuration)

        configuration.applicationDirectory = projectRoot().resolve("missing-rustcfml-app").toString()
        expectInvalid(configuration)
    }

    @Test
    fun testRejectsPathsEscapingThroughSymlink()
    {
        val root = projectRoot()
        val outside = Files.createDirectory(root.parent.resolve("${root.fileName}-outside"))
        val link = root.resolve("rustcfml-link")
        try
        {
            try
            {
                Files.createSymbolicLink(link, outside)
            }
            catch (exception: Exception)
            {
                org.junit.Assume.assumeNoException("Symbolic links are unavailable in this environment", exception)
            }
            assertFalse(RustCFMLRunConfiguration.isWithinProject(root, link))
        }
        finally
        {
            Files.deleteIfExists(link)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun testPortBoundaries()
    {
        val root = projectRoot()
        val configuration = configuredFor(root)
        configuration.port = 1
        configuration.checkConfiguration()
        configuration.port = 65535
        configuration.checkConfiguration()

        configuration.port = 0
        expectInvalid(configuration)
        configuration.port = 65536
        expectInvalid(configuration)
    }

    private fun newConfiguration(): RustCFMLRunConfiguration
    {
        val type = RustCFMLConfigurationType()
        return RustCFMLRunConfiguration(project, type.configurationFactories[0], "Test RUSTCFML")
    }

    private fun configuredFor(applicationDirectory: Path): RustCFMLRunConfiguration = newConfiguration().apply {
        executablePath = executableFile().toString()
        this.applicationDirectory = applicationDirectory.toString()
        port = RustCFMLRunConfiguration.DEFAULT_PORT
    }

    private fun executableFile(): Path = Files.createTempFile(projectRoot(), "rustcfml", ".exe").apply {
        toFile().setExecutable(true)
    }

    private fun projectRoot(): Path = Path.of(project.basePath!!).also(Files::createDirectories)

    private fun expectInvalid(configuration: RustCFMLRunConfiguration)
    {
        try
        {
            configuration.checkConfiguration()
            fail("Expected invalid RUSTCFML configuration")
        }
        catch (_: com.intellij.execution.configurations.RuntimeConfigurationError)
        {
        }
    }

    private class TestSettingsEditor(project: com.intellij.openapi.project.Project) : RustCFMLSettingsEditor(project)
    {
        fun reset(configuration: RustCFMLRunConfiguration) = resetEditorFrom(configuration)
        fun apply(configuration: RustCFMLRunConfiguration) = applyEditorTo(configuration)
    }
}