package com.quetwo.intellilucee.lsp

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Path

internal object CFMLMcpConfiguration
{
    const val SERVER_NAME = "intellilucee-cfml"

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun upsert(existingJson: String?, executable: Path, workspace: Path, lspDirectory: Path): String?
    {
        val root = parseRoot(existingJson) ?: return null
        val servers = getOrCreateServers(root) ?: return null
        val existingServer = servers.get(SERVER_NAME)
        if (existingServer != null && !isManagedServer(existingServer, workspace, lspDirectory))
        {
            return null
        }

        val server = existingServer?.asJsonObject ?: JsonObject()
        val arguments = JsonArray().apply { add("mcp") }
        if (server.get("command")?.asString == executable.toString() &&
            server.get("args") == arguments &&
            server.get("cwd")?.asString == workspace.toString())
        {
            return null
        }

        server.addProperty("command", executable.toString())
        server.add("args", arguments)
        server.addProperty("cwd", workspace.toString())
        servers.add(SERVER_NAME, server)
        return serialize(root)
    }

    fun remove(existingJson: String?, workspace: Path, lspDirectory: Path): String?
    {
        val root = parseRoot(existingJson) ?: return null
        val servers = root.get("mcpServers")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val existingServer = servers.get(SERVER_NAME) ?: return null
        if (!isManagedServer(existingServer, workspace, lspDirectory))
        {
            return null
        }

        servers.remove(SERVER_NAME)
        return serialize(root)
    }

    private fun parseRoot(existingJson: String?): JsonObject?
    {
        if (existingJson == null)
        {
            return JsonObject()
        }

        return try
        {
            JsonParser.parseString(existingJson).takeIf { it.isJsonObject }?.asJsonObject
        }
        catch (_: Exception)
        {
            null
        }
    }

    private fun getOrCreateServers(root: JsonObject): JsonObject?
    {
        val existing = root.get("mcpServers")
        if (existing != null)
        {
            return existing.takeIf { it.isJsonObject }?.asJsonObject
        }

        return JsonObject().also { root.add("mcpServers", it) }
    }

    private fun isManagedServer(serverElement: com.google.gson.JsonElement, workspace: Path, lspDirectory: Path): Boolean
    {
        if (!serverElement.isJsonObject)
        {
            return false
        }

        val server = serverElement.asJsonObject
        val command = server.get("command")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: return false
        val arguments = server.get("args")?.takeIf { it.isJsonArray }?.asJsonArray ?: return false
        val workingDirectory = server.get("cwd")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString ?: return false
        val executable = try
        {
            Path.of(command).toAbsolutePath().normalize()
        }
        catch (_: Exception)
        {
            return false
        }
        val normalizedLspDirectory = lspDirectory.toAbsolutePath().normalize()

        return executable.startsWith(normalizedLspDirectory) &&
            arguments.size() == 1 && arguments[0].isJsonPrimitive && arguments[0].asString == "mcp" &&
            workingDirectory == workspace.toString()
    }

    private fun serialize(root: JsonObject): String = gson.toJson(root) + System.lineSeparator()
}