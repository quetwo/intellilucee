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

    @Test
    fun testPathToDotNotation_RootCfc()
    {
        val root = tempFolder.newFolder("dot_root").toPath()
        Files.createFile(root.resolve("Application.cfc"))
        val serviceCfc = Files.createFile(root.resolve("UserService.cfc"))

        val dotNotation = PathUtils.PathToDotNotation(serviceCfc)
        assertEquals("UserService", dotNotation)
    }

    @Test
    fun testPathToDotNotation_NestedDirectories()
    {
        val root = tempFolder.newFolder("dot_nested").toPath()
        Files.createFile(root.resolve("Application.cfc"))
        val nestedDir = Files.createDirectories(root.resolve("models").resolve("services").resolve("auth"))
        val cfcFile = Files.createFile(nestedDir.resolve("AuthManager.cfc"))

        val dotNotation = PathUtils.PathToDotNotation(cfcFile)
        assertEquals("models.services.auth.AuthManager", dotNotation)
    }

    @Test
    fun testPathToDotNotation_FileAndStringOverloads()
    {
        val root = tempFolder.newFolder("dot_overloads").toPath()
        Files.createFile(root.resolve("Application.cfc"))
        val nestedDir = Files.createDirectories(root.resolve("services"))
        val cfcFile = Files.createFile(nestedDir.resolve("OrderService.cfc"))

        val fromFile = PathUtils.PathToDotNotation(cfcFile.toFile())
        assertEquals("services.OrderService", fromFile)

        val fromString = PathUtils.PathToDotNotation(cfcFile.toString())
        assertEquals("services.OrderService", fromString)
    }

    @Test
    fun testPathToDotNotation_NoApplicationFile()
    {
        val root = tempFolder.newFolder("dot_none").toPath()
        val cfcFile = Files.createFile(root.resolve("Test.cfc"))

        val dotNotation = PathUtils.PathToDotNotation(cfcFile)
        assertNull(dotNotation)
    }

    @Test
    fun testPathToDotNotation_NullAndNonExistent()
    {
        assertNull(PathUtils.PathToDotNotation(null as java.nio.file.Path?))
        assertNull(PathUtils.PathToDotNotation(null as java.io.File?))
        assertNull(PathUtils.PathToDotNotation(null as String?))
        assertNull(PathUtils.PathToDotNotation(tempFolder.root.toPath().resolve("non_existent.cfc")))
    }

    @Test
    fun testDotNotationToPath_RootComponent()
    {
        val root = tempFolder.newFolder("dot_to_path_root").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        val expected = root.resolve("UserService.cfc")
        val result = PathUtils.DotNotationToPath(root, "UserService")
        assertNotNull(result)
        assertEquals(expected.toAbsolutePath().normalize(), result!!.toAbsolutePath().normalize())
    }

    @Test
    fun testDotNotationToPath_NestedComponents()
    {
        val root = tempFolder.newFolder("dot_to_path_nested").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        val expected = root.resolve("models").resolve("services").resolve("auth").resolve("AuthManager.cfc")
        val result = PathUtils.DotNotationToPath(root, "models.services.auth.AuthManager")
        assertNotNull(result)
        assertEquals(expected.toAbsolutePath().normalize(), result!!.toAbsolutePath().normalize())
    }

    @Test
    fun testDotNotationToPath_WithCfcExtensionInNotation()
    {
        val root = tempFolder.newFolder("dot_to_path_ext").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        val expected = root.resolve("models").resolve("services").resolve("OrderService.cfc")
        val result = PathUtils.DotNotationToPath(root, "models.services.OrderService.cfc")
        assertNotNull(result)
        assertEquals(expected.toAbsolutePath().normalize(), result!!.toAbsolutePath().normalize())
    }

    @Test
    fun testDotNotationToFile_AndOverloads()
    {
        val root = tempFolder.newFolder("dot_to_file").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        val expectedFile = root.resolve("services").resolve("UserService.cfc").toFile()

        val fromPath = PathUtils.DotNotationToFile(root, "services.UserService")
        assertNotNull(fromPath)
        assertEquals(expectedFile.absolutePath, fromPath!!.absolutePath)

        val fromFile = PathUtils.DotNotationToFile(root.toFile(), "services.UserService")
        assertNotNull(fromFile)
        assertEquals(expectedFile.absolutePath, fromFile!!.absolutePath)

        val fromString = PathUtils.DotNotationToFile(root.toString(), "services.UserService")
        assertNotNull(fromString)
        assertEquals(expectedFile.absolutePath, fromString!!.absolutePath)

        val fromCamel = PathUtils.dotNotationToFile(root, "services.UserService")
        assertNotNull(fromCamel)
        assertEquals(expectedFile.absolutePath, fromCamel!!.absolutePath)
    }

    @Test
    fun testDotNotationToPath_NoApplicationFile()
    {
        val root = tempFolder.newFolder("dot_to_path_none").toPath()

        val result = PathUtils.DotNotationToPath(root, "services.UserService")
        assertNull(result)
    }

    @Test
    fun testDotNotationToPath_NullAndEmptyInputs()
    {
        val root = tempFolder.newFolder("dot_to_path_nulls").toPath()
        Files.createFile(root.resolve("Application.cfc"))

        assertNull(PathUtils.DotNotationToPath(null as java.nio.file.Path?, "UserService"))
        assertNull(PathUtils.DotNotationToPath(root, null))
        assertNull(PathUtils.DotNotationToPath(root, ""))
        assertNull(PathUtils.DotNotationToPath(root, "   "))
        assertNull(PathUtils.DotNotationToFile(null as java.nio.file.Path?, "UserService"))
        assertNull(PathUtils.DotNotationToFile(root, null))
    }

    @Test
    fun testDotNotation_RoundTrip()
    {
        val root = tempFolder.newFolder("dot_roundtrip").toPath()
        Files.createFile(root.resolve("Application.cfc"))
        val subDir = Files.createDirectories(root.resolve("models").resolve("services"))
        val serviceFile = Files.createFile(subDir.resolve("UserService.cfc"))

        val notation = PathUtils.PathToDotNotation(serviceFile)
        assertEquals("models.services.UserService", notation)

        val resolvedPath = PathUtils.DotNotationToPath(root, notation)
        assertNotNull(resolvedPath)
        assertEquals(serviceFile.toRealPath(), resolvedPath!!.toRealPath())
    }

    @Test
    fun testListAllComponents_WithApplicationCfc()
    {
        val root = tempFolder.newFolder("list_all_app").toPath()
        val appCfc = Files.createFile(root.resolve("Application.cfc"))
        val modelsDir = Files.createDirectories(root.resolve("models"))
        val userCfc = Files.createFile(modelsDir.resolve("User.cfc"))
        val servicesDir = Files.createDirectories(root.resolve("services").resolve("sub"))
        val authCfc = Files.createFile(servicesDir.resolve("AuthService.CFC"))
        // Create a non-CFC file to ensure it's filtered out
        Files.createFile(modelsDir.resolve("helper.cfm"))
        Files.createFile(root.resolve("readme.txt"))

        val components = PathUtils.ListAllComponents(root)
        assertEquals(3, components.size)

        val dotNotations = components.map { it.dotNotation }.toSet()
        val files = components.map { it.cfcPath?.canonicalPath }.toSet()

        assertEquals(setOf("Application", "models.User", "services.sub.AuthService"), dotNotations)
        assertEquals(
            setOf(
                appCfc.toFile().canonicalPath,
                userCfc.toFile().canonicalPath,
                authCfc.toFile().canonicalPath
            ),
            files
        )
    }

    @Test
    fun testListAllComponents_WithoutApplicationCfc()
    {
        val root = tempFolder.newFolder("list_all_no_app").toPath()
        val modelsDir = Files.createDirectories(root.resolve("models"))
        val userCfc = Files.createFile(modelsDir.resolve("User.cfc"))
        val itemCfc = Files.createFile(root.resolve("Item.cfc"))

        val components = PathUtils.ListAllComponents(root)
        assertEquals(2, components.size)

        val dotNotations = components.map { it.dotNotation }.toSet()
        val files = components.map { it.cfcPath?.canonicalPath }.toSet()

        assertEquals(setOf(null), dotNotations)
        assertEquals(
            setOf(
                userCfc.toFile().canonicalPath,
                itemCfc.toFile().canonicalPath
            ),
            files
        )
    }

    @Test
    fun testListAllComponents_Overloads()
    {
        val root = tempFolder.newFolder("list_all_overloads").toPath()
        Files.createFile(root.resolve("Application.cfc"))
        val userCfc = Files.createFile(root.resolve("User.cfc"))

        val fromPath = PathUtils.ListAllComponents(root)
        val fromFile = PathUtils.ListAllComponents(root.toFile())
        val fromString = PathUtils.ListAllComponents(root.toString())


        assertEquals(2, fromPath.size)
        assertEquals(2, fromFile.size)
        assertEquals(2, fromString.size)

        assertEquals(fromPath, fromFile)
        assertEquals(fromPath, fromString)
    }

    @Test
    fun testListAllComponents_EmptyOrNull()
    {
        val emptyRoot = tempFolder.newFolder("list_all_empty").toPath()
        assertEquals(0, PathUtils.ListAllComponents(emptyRoot).size)
        assertEquals(0, PathUtils.ListAllComponents(null as java.nio.file.Path?).size)
        assertEquals(0, PathUtils.ListAllComponents(null as java.io.File?).size)
        assertEquals(0, PathUtils.ListAllComponents(null as String?).size)
        assertEquals(0, PathUtils.ListAllComponents(emptyRoot.resolve("non_existent")).size)
    }

    @Test
    fun testCFCDescriptor_PropertiesAndMethods()
    {
        val file = java.io.File("models/User.cfc")
        val descriptor = CFCDescriptor(file, "models.User")

        assertEquals(file, descriptor.cfcPath)
        assertEquals(file, descriptor.path)
        assertEquals(file, descriptor.file)
        assertEquals(file, descriptor.cfcFile)
        assertEquals("models.User", descriptor.dotNotation)
        assertEquals("models.User", descriptor.cfcDotNotation)

        val newFile = java.io.File("services/Auth.cfc")
        descriptor.cfcPath = newFile
        descriptor.dotNotation = "services.Auth"
        assertEquals(newFile, descriptor.cfcPath)
        assertEquals("services.Auth", descriptor.dotNotation)

        val desc2 = CFCDescriptor(newFile, "services.Auth")
        assertEquals(descriptor, desc2)
        assertEquals(descriptor.hashCode(), desc2.hashCode())

        val innerDesc = PathUtils.CFCDescriptor(newFile, "services.Auth")
        assertEquals(descriptor, innerDesc)
    }
}
