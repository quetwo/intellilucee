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

    @Test
    fun testGenerateProjectWithoutDockerCreatesReadme()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_readme_no_docker")
        val settings = CFMLProjectGenerator.Settings(
            "my-custom-app",
            "7.0.x",
            false,
            false,
            "MySQL",
            false,
            "my-custom-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val readmeFile = rootDir.findChild("README.md")
        assertNotNull("Expected README.md to be created", readmeFile)

        val content = String(readmeFile!!.contentsToByteArray())
        assertTrue(content.startsWith("# my-custom-app\n\n"))
        assertTrue(content.contains("frontend/"))
        assertTrue(content.contains("frontend/webroot/"))
        assertTrue(content.contains("Application.cfc"))
        assertTrue(content.contains("index.cfm"))
        assertFalse(content.contains("docker-compose.yml"))
        assertFalse(content.contains("Dockerfile"))
        assertFalse(content.contains("secrets.txt"))
        assertFalse(content.contains("db/"))
        assertFalse(content.contains("proxy/"))
        assertTrue(content.contains("Modify the `Application.cfc`"))
    }

    @Test
    fun testGenerateProjectWithDockerCreatesReadme()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_readme_docker")
        val settings = CFMLProjectGenerator.Settings(
            "my-docker-app",
            "7.0.x",
            true,
            false,
            "MySQL",
            false,
            "my-docker-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val readmeFile = rootDir.findChild("README.md")
        assertNotNull("Expected README.md to be created", readmeFile)

        val content = String(readmeFile!!.contentsToByteArray())
        assertTrue(content.startsWith("# my-docker-app\n\n"))
        assertTrue(content.contains("frontend/"))
        assertTrue(content.contains("frontend/webroot/"))
        assertTrue(content.contains("Application.cfc"))
        assertTrue(content.contains("index.cfm"))
        assertTrue(content.contains("docker-compose.yml"))
        assertTrue(content.contains("frontend/Dockerfile"))
        assertTrue(content.contains("frontend/secrets.txt"))
        assertFalse(content.contains("db/"))
        assertFalse(content.contains("proxy/"))
        assertTrue(content.contains("Modify the `Application.cfc`"))
        assertTrue(content.contains("Lucee administrator password"))
        assertTrue(content.contains("frontend/secrets.txt"))
    }

    @Test
    fun testGenerateProjectWithDockerAndDatabaseAndProxyCreatesReadme()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_readme_full")
        val settings = CFMLProjectGenerator.Settings(
            "my-full-app",
            "7.0.x",
            true,
            true,
            "Postgres",
            true,
            "my-full-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val readmeFile = rootDir.findChild("README.md")
        assertNotNull("Expected README.md to be created", readmeFile)

        val content = String(readmeFile!!.contentsToByteArray())
        assertTrue(content.startsWith("# my-full-app\n\n"))
        assertTrue(content.contains("frontend/"))
        assertTrue(content.contains("frontend/webroot/"))
        assertTrue(content.contains("Application.cfc"))
        assertTrue(content.contains("index.cfm"))
        assertTrue(content.contains("docker-compose.yml"))
        assertTrue(content.contains("frontend/Dockerfile"))
        assertTrue(content.contains("frontend/secrets.txt"))
        assertTrue(content.contains("db/"))
        assertTrue(content.contains("db/secrets.txt"))
        assertTrue(content.contains("db/sql/"))
        assertTrue(content.contains("proxy/"))
        assertTrue(content.contains("proxy/config.toml"))
        assertTrue(content.contains("Modify the `Application.cfc`"))
        assertTrue(content.contains("Lucee administrator password"))
        assertTrue(content.contains("frontend/secrets.txt"))
        assertTrue(content.contains("database passwords"))
        assertTrue(content.contains("db/secrets.txt"))
        assertTrue(content.contains("domain name"))
    }
}
