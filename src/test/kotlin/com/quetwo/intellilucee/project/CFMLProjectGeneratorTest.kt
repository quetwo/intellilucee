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

        val webrootDir = rootDir.findChild("frontend")?.findChild("webroot")
        assertNotNull("Expected frontend/webroot directory to exist", webrootDir)
        val indexFile = webrootDir!!.findChild("index.cfm")
        assertNotNull("Expected index.cfm to be created", indexFile)
        val indexContent = String(indexFile!!.contentsToByteArray())
        assertTrue(indexContent.contains("<title>my-custom-app</title>"))
        assertTrue(indexContent.contains("<cfoutput>"))
        assertTrue(indexContent.contains("#server.coldfusion.productname#"))
        assertTrue(indexContent.contains("#server.lucee.version#"))
        assertTrue(indexContent.contains("</cfoutput>"))

        val appFile = webrootDir.findChild("Application.cfc")
        assertNotNull("Expected Application.cfc to be created", appFile)
        val appContent = String(appFile!!.contentsToByteArray())
        assertTrue(appContent.contains("component {"))
        assertTrue(appContent.contains("this.name = \"my-custom-app\";"))
        assertTrue(appContent.contains("this.sessionManagement = true;"))
        assertTrue(appContent.contains("this.sessionTimeout = createTimeSpan(0, 0, 30, 0);"))
        assertTrue(appContent.contains("this.applicationTimeout = createTimeSpan(0, 12, 0, 0);"))
        assertFalse(appContent.contains("this.datasource"))

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

        val webrootDir = rootDir.findChild("frontend")?.findChild("webroot")
        assertNotNull("Expected frontend/webroot directory to exist", webrootDir)
        val appFile = webrootDir!!.findChild("Application.cfc")
        assertNotNull("Expected Application.cfc to be created", appFile)
        val appContent = String(appFile!!.contentsToByteArray())
        assertTrue(appContent.contains("component {"))
        assertTrue(appContent.contains("this.name = \"my-full-app\";"))
        assertTrue(appContent.contains("this.sessionManagement = true;"))
        assertTrue(appContent.contains("this.sessionTimeout = createTimeSpan(0, 0, 30, 0);"))
        assertTrue(appContent.contains("this.applicationTimeout = createTimeSpan(0, 12, 0, 0);"))
        assertTrue(appContent.contains("this.datasource = {class: 'org.postgresql.Driver'"))
        assertTrue(appContent.contains("bundleName: 'org.postgresql.jdbc'"))
        assertTrue(appContent.contains("connectionString: 'jdbc:postgresql://db:5432/' & server.system.environment.POSTGRES_DB"))
        assertTrue(appContent.contains("username: server.system.environment.POSTGRES_USER"))
        assertTrue(appContent.contains("password: server.system.environment.POSTGRES_PASSWORD"))

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

    @Test
    fun testGenerateProjectWithPostgresDatabaseSecrets()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_postgres_db")
        val settings = CFMLProjectGenerator.Settings(
            "pg-app",
            "7.0.x",
            true,
            true,
            "Postgres",
            false,
            "pg-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val dbSecretsFile = rootDir.findChild("db")?.findChild("secrets.txt")
        assertNotNull("Expected db/secrets.txt to be created", dbSecretsFile)

        val content = String(dbSecretsFile!!.contentsToByteArray())
        assertTrue(content.contains("POSTGRES_PASSWORD="))
        assertTrue(content.contains("POSTGRES_DB=db_pg-app"))
        assertTrue(content.contains("POSTGRES_USER=pg-app_user"))
        assertTrue(content.contains("DB_HOST=db"))
    }

    @Test
    fun testGenerateProjectWithMssqlDatabaseSecrets()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_mssql_db")
        val settings = CFMLProjectGenerator.Settings(
            "mssql-app",
            "7.0.x",
            true,
            true,
            "MSSQL",
            false,
            "mssql-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val dbSecretsFile = rootDir.findChild("db")?.findChild("secrets.txt")
        assertNotNull("Expected db/secrets.txt to be created", dbSecretsFile)

        val content = String(dbSecretsFile!!.contentsToByteArray())
        assertTrue(content.contains("ACCEPT_EULA=Y"))
        assertTrue(content.contains("MSSQL_SA_PASSWORD="))
        assertTrue(content.contains("DB_HOST=db"))

        val webrootDir = rootDir.findChild("frontend")?.findChild("webroot")
        assertNotNull("Expected frontend/webroot directory to exist", webrootDir)
        val appFile = webrootDir!!.findChild("Application.cfc")
        assertNotNull("Expected Application.cfc to be created", appFile)
        val appContent = String(appFile!!.contentsToByteArray())
        assertTrue(appContent.contains("component {"))
        assertTrue(appContent.contains("this.name = \"mssql-app\";"))
        assertTrue(appContent.contains("this.datasource = {class: 'com.microsoft.sqlserver.jdbc.SQLServerDriver'"))
        assertTrue(appContent.contains("bundleName: 'com.microsoft.sqlserver.mssql-jdbc'"))
        assertTrue(appContent.contains("connectionString: 'jdbc:sqlserver://db:1433;trustServerCertificate=true'"))
        assertTrue(appContent.contains("username: 'sa'"))
        assertTrue(appContent.contains("password: server.system.environment.MSSQL_SA_PASSWORD"))
    }

    @Test
    fun testGenerateProjectDockerComposeWithoutProxy()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_docker_no_proxy")
        val settings = CFMLProjectGenerator.Settings(
            "no-proxy-app",
            "7.0.x",
            true,
            false,
            "MySQL",
            false,
            "no-proxy-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val dockerComposeFile = rootDir.findChild("docker-compose.yml")
        assertNotNull("Expected docker-compose.yml to be created", dockerComposeFile)

        val content = String(dockerComposeFile!!.contentsToByteArray())
        assertFalse(content.contains("dockerfile:"))
        assertTrue(content.contains("context: frontend/"))
        assertTrue(content.contains("volumes:\n      - ./frontend/webroot:/var/www"))
        assertTrue(content.contains("\"8888:8888\""))
        assertFalse(content.contains("traefik.enable=true"))
    }

    @Test
    fun testGenerateProjectDockerComposeWithProxy()
    {
        val rootDir = myFixture.tempDirFixture.findOrCreateDir("proj_docker_with_proxy")
        val settings = CFMLProjectGenerator.Settings(
            "proxy-app",
            "7.0.x",
            true,
            false,
            "MySQL",
            true,
            "proxy-app.local"
        )

        generator.generateProject(project, rootDir, settings, null)

        val dockerComposeFile = rootDir.findChild("docker-compose.yml")
        assertNotNull("Expected docker-compose.yml to be created", dockerComposeFile)

        val content = String(dockerComposeFile!!.contentsToByteArray())
        assertFalse(content.contains("dockerfile:"))
        assertTrue(content.contains("context: frontend/"))
        assertTrue(content.contains("volumes:\n      - ./frontend/webroot:/var/www"))
        assertTrue(content.contains("labels:"))
        assertTrue(content.contains("\"traefik.enable=true\""))
        assertTrue(content.contains("\"traefik.http.routers.frontend.entrypoints=web\""))
        assertTrue(content.contains("\"traefik.http.routers.frontends.entrypoints=websecure\""))
        assertTrue(content.contains("\"traefik.http.routers.frontends.tls=true\""))
        assertTrue(content.contains("\"traefik.http.routers.frontends.tls.certresolver=myresolver\""))
        assertTrue(content.contains("\"traefik.http.services.web.loadbalancer.server.port=8888\""))
        assertTrue(content.contains("\"traefik.http.routers.frontends.tls.domains[0].main=proxy-app.local\""))
    }
}
