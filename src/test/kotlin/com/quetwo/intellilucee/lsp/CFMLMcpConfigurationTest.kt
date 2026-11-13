package com.quetwo.intellilucee.lsp

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CFMLMcpConfigurationTest
{
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun upsertAddsServerWithSingleArgumentAndWorkspaceDirectoryAndPreservesOtherServers()
    {
        val workspace = temporaryFolder.root.toPath().resolve("workspace")
        val lspDirectory = temporaryFolder.root.toPath().resolve("plugin/lsp")
        val executable = lspDirectory.resolve("v1/clif.exe")
        val existing = """
            {
              "otherSetting": true,
              "mcpServers": {
                "Other": { "command": "other-server", "args": [] }
              }
            }
        """.trimIndent()

        val updated = CFMLMcpConfiguration.upsert(existing, executable, workspace, lspDirectory)
        assertNotNull(updated)
        val root = JsonParser.parseString(updated).asJsonObject
        val server = root.getAsJsonObject("mcpServers").getAsJsonObject(CFMLMcpConfiguration.SERVER_NAME)

        assertEquals(true, root.get("otherSetting").asBoolean)
        assertTrue(root.getAsJsonObject("mcpServers").has("Other"))
        assertEquals(executable.toString(), server.get("command").asString)
        assertEquals(listOf("mcp"), server.getAsJsonArray("args").map { it.asString })
        assertEquals(workspace.toString(), server.get("cwd").asString)
    }

    @Test
    fun upsertDoesNotReplaceConflictingServer()
    {
        val workspace = temporaryFolder.root.toPath().resolve("workspace")
        val lspDirectory = temporaryFolder.root.toPath().resolve("plugin/lsp")
        val existing = """
            { "mcpServers": { "${CFMLMcpConfiguration.SERVER_NAME}": { "command": "user-command", "args": [] } } }
        """.trimIndent()

        assertNull(CFMLMcpConfiguration.upsert(existing, lspDirectory.resolve("v1/clif"), workspace, lspDirectory))
    }

    @Test
    fun upsertUpdatesExecutableForSelectedRelease()
    {
        val workspace = temporaryFolder.root.toPath().resolve("workspace")
        val lspDirectory = temporaryFolder.root.toPath().resolve("plugin/lsp")
        val oldExecutable = lspDirectory.resolve("v1/clif")
        val newExecutable = lspDirectory.resolve("v2/clif")
        val initial = CFMLMcpConfiguration.upsert(null, oldExecutable, workspace, lspDirectory)
        val updated = CFMLMcpConfiguration.upsert(initial, newExecutable, workspace, lspDirectory)

        assertNotNull(updated)
        val server = JsonParser.parseString(updated).asJsonObject
            .getAsJsonObject("mcpServers")
            .getAsJsonObject(CFMLMcpConfiguration.SERVER_NAME)
        assertEquals(newExecutable.toString(), server.get("command").asString)
    }

    @Test
    fun removeDeletesOnlyManagedServerAndPreservesOtherServers()
    {
        val workspace = temporaryFolder.root.toPath().resolve("workspace")
        val lspDirectory = temporaryFolder.root.toPath().resolve("plugin/lsp")
        val executable = lspDirectory.resolve("v1/clif")
        val generated = CFMLMcpConfiguration.upsert(null, executable, workspace, lspDirectory)
        val root = JsonParser.parseString(generated).asJsonObject
        root.getAsJsonObject("mcpServers").add("Other", JsonObject().apply { addProperty("command", "other-server") })

        val updated = CFMLMcpConfiguration.remove(root.toString(), workspace, lspDirectory)
        assertNotNull(updated)
        val servers = JsonParser.parseString(updated).asJsonObject.getAsJsonObject("mcpServers")
        assertFalse(servers.has(CFMLMcpConfiguration.SERVER_NAME))
        assertTrue(servers.has("Other"))
    }

    @Test
    fun invalidJsonIsLeftUntouched()
    {
        val workspace = temporaryFolder.root.toPath().resolve("workspace")
        val lspDirectory = temporaryFolder.root.toPath().resolve("plugin/lsp")

        assertNull(CFMLMcpConfiguration.upsert("not-json", lspDirectory.resolve("v1/clif"), workspace, lspDirectory))
        assertNull(CFMLMcpConfiguration.remove("not-json", workspace, lspDirectory))
    }
}