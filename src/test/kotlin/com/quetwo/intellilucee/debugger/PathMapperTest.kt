package com.quetwo.intellilucee.debugger

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PathMapperTest : BasePlatformTestCase() {

    @Test
    fun testToServerPathWithSingleTransform() {
        val transforms = listOf(PathTransform("/path/to/app", "/var/www"))
        val serverPath = PathMapper.toServerPath("/path/to/app/models/User.cfc", transforms)
        assertEquals("/var/www/models/User.cfc", serverPath)
    }

    @Test
    fun testToServerPathWindowsToUnix() {
        val transforms = listOf(PathTransform("C:\\Projects\\app", "/var/www"))
        val serverPath = PathMapper.toServerPath("C:\\Projects\\app\\views\\main.cfm", transforms)
        assertEquals("/var/www/views/main.cfm", serverPath)
    }

    @Test
    fun testToIdePathWithSingleTransform() {
        val transforms = listOf(PathTransform("/path/to/app", "/var/www"))
        val idePath = PathMapper.toIdePath("/var/www/models/User.cfc", transforms)
        assertEquals("/path/to/app/models/User.cfc", idePath)
    }

    @Test
    fun testToIdePathUnixToWindows() {
        val transforms = listOf(PathTransform("C:\\Projects\\app", "/var/www"))
        val idePath = PathMapper.toIdePath("/var/www/views/main.cfm", transforms)
        assertEquals("C:\\Projects\\app\\views\\main.cfm", idePath)
    }

    @Test
    fun testUnmatchedPathReturnsOriginal() {
        val transforms = listOf(PathTransform("/path/to/app", "/var/www"))
        val serverPath = PathMapper.toServerPath("/other/path/file.cfm", transforms)
        assertEquals("/other/path/file.cfm", serverPath)

        val idePath = PathMapper.toIdePath("/other/server/path/file.cfm", transforms)
        assertEquals("/other/server/path/file.cfm", idePath)
    }

    @Test
    fun testFindVirtualFile() {
        val psiFile = myFixture.configureByText("TestFile.cfm", "<cfset x = 1>")
        val virtualFile = psiFile.virtualFile
        val found = PathMapper.findVirtualFile(virtualFile.path)
        assertNotNull(found)
        assertEquals(virtualFile.path, found!!.path)
    }
}
