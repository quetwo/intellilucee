package com.quetwo.intellilucee.debugger

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

object PathMapper {

    fun toServerPath(idePath: String, transforms: List<PathTransform>): String {
        if (transforms.isEmpty() || idePath.isBlank()) return idePath

        val normalizedIdePath = normalizeSeparators(idePath)
        for (transform in transforms) {
            if (transform.idePrefix.isBlank()) continue
            val normalizedPrefix = normalizeSeparators(transform.idePrefix).trimEnd('/')
            if (normalizedIdePath.startsWith(normalizedPrefix, ignoreCase = isWindowsPath(idePath) || isWindowsPath(transform.idePrefix))) {
                val remainder = normalizedIdePath.substring(normalizedPrefix.length).trimStart('/')
                val serverPrefix = transform.serverPrefix.trimEnd('/', '\\')
                val separator = if (serverPrefix.contains('\\') && !serverPrefix.contains('/')) "\\" else "/"
                val formattedRemainder = if (separator == "\\") remainder.replace('/', '\\') else remainder.replace('\\', '/')
                return if (formattedRemainder.isEmpty()) {
                    serverPrefix
                } else if (serverPrefix.isEmpty()) {
                    formattedRemainder
                } else {
                    "$serverPrefix$separator$formattedRemainder"
                }
            }
        }
        return idePath
    }

    fun toIdePath(serverPath: String, transforms: List<PathTransform>): String {
        if (transforms.isEmpty() || serverPath.isBlank()) return serverPath

        val normalizedServerPath = normalizeSeparators(serverPath)
        for (transform in transforms) {
            if (transform.serverPrefix.isBlank()) continue
            val normalizedPrefix = normalizeSeparators(transform.serverPrefix).trimEnd('/')
            if (normalizedServerPath.startsWith(normalizedPrefix, ignoreCase = isWindowsPath(serverPath) || isWindowsPath(transform.serverPrefix))) {
                val remainder = normalizedServerPath.substring(normalizedPrefix.length).trimStart('/')
                val idePrefix = transform.idePrefix.trimEnd('/', '\\')
                val separator = if (idePrefix.contains('\\')) "\\" else "/"
                val formattedRemainder = if (separator == "\\") remainder.replace('/', '\\') else remainder.replace('\\', '/')
                val result = if (formattedRemainder.isEmpty()) {
                    idePrefix
                } else if (idePrefix.isEmpty()) {
                    formattedRemainder
                } else {
                    "$idePrefix$separator$formattedRemainder"
                }
                return result
            }
        }
        return serverPath
    }

    fun findVirtualFile(idePath: String): VirtualFile? {
        if (idePath.isBlank()) return null
        val normalized = normalizeSeparators(idePath)
        val vfm = VirtualFileManager.getInstance()
        if (normalized.contains("://")) {
            return vfm.findFileByUrl(normalized) ?: vfm.refreshAndFindFileByUrl(normalized)
        }
        val fileUrl = VfsUtilCore.pathToUrl(normalized)
        val fromUrl = vfm.findFileByUrl(fileUrl) ?: vfm.refreshAndFindFileByUrl(fileUrl)
        if (fromUrl != null) return fromUrl

        val tempUrl = "temp://$normalized"
        val fromTemp = vfm.findFileByUrl(tempUrl) ?: vfm.refreshAndFindFileByUrl(tempUrl)
        if (fromTemp != null) return fromTemp

        val ioFile = File(idePath)
        val lfs = LocalFileSystem.getInstance()
        if (ioFile.exists()) {
            return lfs.findFileByIoFile(ioFile) ?: lfs.refreshAndFindFileByIoFile(ioFile)
        }
        return lfs.findFileByPath(normalized) ?: lfs.refreshAndFindFileByPath(normalized)
    }

    private fun normalizeSeparators(path: String): String =
        path.replace('\\', '/')

    private fun isWindowsPath(path: String): Boolean =
        (path.length >= 2 && path[1] == ':' && path[0].isLetter()) || path.startsWith("\\\\")
}
