package com.quetwo.intellilucee.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class PathUtilsTest
{
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFindClosestApplicationFile_AtRoot_Cfc()
    {
        val root = tempFolder.newFolder("workspace_cfc").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_AtRoot_Cfm()
    {
        val root = tempFolder.newFolder("workspace_cfm").toPath()
        val appCfm = Files.createFile(root.resolve("Application.cfm"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfm.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_BothAtRoot_PrefersCfc()
    {
        val root = tempFolder.newFolder("workspace_both").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        Files.createFile(root.resolve("Application.cfm"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_CaseInsensitive()
    {
        val root = tempFolder.newFolder("workspace_case").toPath()
        val appCfc = Files.createFile(root.resolve("application.cfc"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_InSubdirectory()
    {
        val root = tempFolder.newFolder("workspace_sub").toPath()
        val sub = Files.createDirectories(root.resolve("src").resolve("app"))
        val appCfc = Files.createFile(sub.resolve("Application.cfc"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_PicksClosestDepth()
    {
        val root = tempFolder.newFolder("workspace_depth").toPath()
        val depth1 = Files.createDirectories(root.resolve("web"))
        val depth2 = Files.createDirectories(root.resolve("sub").resolve("deep"))

        val appCfmDepth1 = Files.createFile(depth1.resolve("Application.cfm"))
        Files.createFile(depth2.resolve("Application.cfc"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNotNull(found)
        assertEquals(appCfmDepth1.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFile_NotFound()
    {
        val root = tempFolder.newFolder("workspace_empty").toPath()
        Files.createDirectories(root.resolve("sub"))
        Files.createFile(root.resolve("sub").resolve("index.cfm"))

        val found = PathUtils.findClosestApplicationFile(root)
        assertNull(found)
    }

    @Test
    fun testFindClosestApplicationFile_NullOrNonExistent()
    {
        assertNull(PathUtils.findClosestApplicationFile(null as java.nio.file.Path?))
        assertNull(PathUtils.findClosestApplicationFile(null as String?))
        assertNull(PathUtils.findClosestApplicationFile(tempFolder.root.toPath().resolve("does_not_exist")))
    }

    @Test
    fun testFindClosestApplicationFile_DirectFile()
    {
        val root = tempFolder.newFolder("workspace_file").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))

        val found = PathUtils.findClosestApplicationFile(appCfc)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }
}
