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

    @Test
    fun testFindClosestApplicationFileUp_SameDirectory()
    {
        val root = tempFolder.newFolder("up_same").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        val targetFile = Files.createFile(root.resolve("index.cfm"))

        val foundFromTarget = PathUtils.findClosestApplicationFileFromFile(targetFile)
        assertNotNull(foundFromTarget)
        assertEquals(appCfc.toRealPath(), foundFromTarget!!.toRealPath())

        val foundFromDir = PathUtils.findClosestApplicationFileFromFile(root)
        assertNotNull(foundFromDir)
        assertEquals(appCfc.toRealPath(), foundFromDir!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_TraverseParent()
    {
        val root = tempFolder.newFolder("up_parent").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        val subDir = Files.createDirectories(root.resolve("sub").resolve("deep"))
        val targetFile = Files.createFile(subDir.resolve("detail.cfm"))

        val found = PathUtils.findClosestApplicationFileFromFile(targetFile)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())

        val foundFromDir = PathUtils.findClosestApplicationFileFromFile(subDir)
        assertNotNull(foundFromDir)
        assertEquals(appCfc.toRealPath(), foundFromDir!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_MultipleLevels_PicksClosestAncestor()
    {
        val root = tempFolder.newFolder("up_multi").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        val midDir = Files.createDirectories(root.resolve("module"))
        val midApp = Files.createFile(midDir.resolve("Application.cfm"))

        val deepDir = Files.createDirectories(midDir.resolve("views"))
        val targetFile = Files.createFile(deepDir.resolve("view.cfm"))

        val found = PathUtils.findClosestApplicationFileFromFile(targetFile)
        assertNotNull(found)
        assertEquals(midApp.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_SameLevel_PrefersCfcOverCfm()
    {
        val root = tempFolder.newFolder("up_precedence").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        Files.createFile(root.resolve("Application.cfm"))
        val subDir = Files.createDirectories(root.resolve("sub"))
        val targetFile = Files.createFile(subDir.resolve("index.cfm"))

        val found = PathUtils.findClosestApplicationFileFromFile(targetFile)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_CaseInsensitive()
    {
        val root = tempFolder.newFolder("up_case").toPath()
        val appCfc = Files.createFile(root.resolve("application.cfc"))
        val subDir = Files.createDirectories(root.resolve("sub"))
        val targetFile = Files.createFile(subDir.resolve("index.cfm"))

        val found = PathUtils.findClosestApplicationFileFromFile(targetFile)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_DirectApplicationFile()
    {
        val root = tempFolder.newFolder("up_direct").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))

        val found = PathUtils.findClosestApplicationFileFromFile(appCfc)
        assertNotNull(found)
        assertEquals(appCfc.toRealPath(), found!!.toRealPath())
    }

    @Test
    fun testFindClosestApplicationFileUp_FileAndStringOverloads()
    {
        val root = tempFolder.newFolder("up_overloads").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        val subDir = Files.createDirectories(root.resolve("sub"))
        val target = Files.createFile(subDir.resolve("index.cfm"))

        val foundFile = PathUtils.findClosestApplicationFileFromFile(target.toFile())
        assertNotNull(foundFile)
        assertEquals(appCfc.toRealPath(), foundFile!!.toRealPath())

        val foundString = PathUtils.findClosestApplicationFileFromFile(target.toString())
        assertNotNull(foundString)
        assertEquals(appCfc.toRealPath(), foundString!!.toRealPath())

    }

    @Test
    fun testFindClosestApplicationFileUp_NotFound()
    {
        val root = tempFolder.newFolder("up_none").toPath()
        val subDir = Files.createDirectories(root.resolve("sub"))
        val target = Files.createFile(subDir.resolve("index.cfm"))

        val found = PathUtils.findClosestApplicationFileFromFile(target)
        assertNull(found)
    }

    @Test
    fun testFindClosestApplicationFileUp_NullOrNonExistent()
    {
        assertNull(PathUtils.findClosestApplicationFileFromFile(null as java.nio.file.Path?))
        assertNull(PathUtils.findClosestApplicationFileFromFile(null as java.io.File?))
        assertNull(PathUtils.findClosestApplicationFileFromFile(null as String?))
        assertNull(PathUtils.findClosestApplicationFileFromFile(tempFolder.root.toPath().resolve("non_existent")))
    }
}
