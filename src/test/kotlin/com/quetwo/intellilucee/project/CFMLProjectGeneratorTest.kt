package com.quetwo.intellilucee.project

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CFMLProjectGeneratorTest : BasePlatformTestCase()
{
    private val generator = CFMLProjectGenerator()

    @Test
    fun testGenerateProjectWithoutDockerCreatesBaseGitignore()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_no_docker")
        val settings = CFMLProjectGenerator.Settings(
            "test-app",
            "7.0.x",
            false,
            false,
            "MySQL",
            false,
            "test-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val gitignoreFile = rootDir.findChild(".gitignore")
        assertNotNull("Expected .gitignore to be created", gitignoreFile)

        val content = String(gitignoreFile!!.contentsToByteArray())
        assertEquals(".idea/\n*.iml\n", content)
    }

    @Test
    fun testGenerateProjectWithDockerSupportIgnoresFrontendSecrets()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_docker")
        val settings = CFMLProjectGenerator.Settings(
            "test-app",
            "7.0.x",
            true,
            false,
            "MySQL",
            false,
            "test-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val gitignoreFile = rootDir.findChild(".gitignore")
        assertNotNull("Expected .gitignore to be created", gitignoreFile)

        val content = String(gitignoreFile!!.contentsToByteArray())
        assertEquals(".idea/\n*.iml\nfrontend/secrets.txt\n", content)
    }

    @Test
    fun testGenerateProjectWithDockerAndDatabaseSupportIgnoresFrontendAndDbSecrets()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_docker_db")
        val settings = CFMLProjectGenerator.Settings(
            "test-app",
            "7.0.x",
            true,
            true,
            "MySQL",
            false,
            "test-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val gitignoreFile = rootDir.findChild(".gitignore")
        assertNotNull("Expected .gitignore to be created", gitignoreFile)

        val content = String(gitignoreFile!!.contentsToByteArray())
        assertEquals(".idea/\n*.iml\nfrontend/secrets.txt\ndb/secrets.txt\n", content)
    }
}
