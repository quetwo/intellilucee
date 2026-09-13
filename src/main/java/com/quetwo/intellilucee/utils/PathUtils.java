package com.quetwo.intellilucee.utils;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathUtils
{
    static public String fixURIForLSP(String path)
    {
        Matcher driveLetter = Pattern.compile("file://(\\w):/").matcher(path);
        if (driveLetter.find())
        {
            // we are running on Windows and need to fix the drive letter for the LSP server
            return "file:///" + driveLetter.group(1).toLowerCase() + "%3A/" + path.substring(10);
        }
        else
        {
            return path;
        }
    }

    /**
     * Finds and returns the Path to the Application.cfc or Application.cfm file that is closest to the workspace root.
     * Searches level-by-level starting from the workspace root directory.
     * If both Application.cfc and Application.cfm exist at the same level, Application.cfc is prioritized.
     *
     * @param workspaceRoot the root path of the workspace
     * @return Path to the closest Application.cfc or Application.cfm file, or null if not found
     */
    @Nullable
    public static Path findClosestApplicationFile(@Nullable Path workspaceRoot)
    {
        if (workspaceRoot == null || !Files.exists(workspaceRoot))
        {
            return null;
        }

        Path startDir = Files.isDirectory(workspaceRoot) ? workspaceRoot : workspaceRoot.getParent();
        if (startDir == null || !Files.exists(startDir))
        {
            String fileName = workspaceRoot.getFileName() != null ? workspaceRoot.getFileName().toString() : "";
            if (fileName.equalsIgnoreCase("Application.cfc") || fileName.equalsIgnoreCase("Application.cfm"))
            {
                return workspaceRoot;
            }
            return null;
        }

        if (!Files.isDirectory(workspaceRoot))
        {
            String fileName = workspaceRoot.getFileName() != null ? workspaceRoot.getFileName().toString() : "";
            if (fileName.equalsIgnoreCase("Application.cfc") || fileName.equalsIgnoreCase("Application.cfm"))
            {
                return workspaceRoot;
            }
        }

        Queue<Path> queue = new ArrayDeque<>();
        Set<Path> visited = new HashSet<>();

        try
        {
            visited.add(startDir.toRealPath());
        }
        catch (IOException e)
        {
            visited.add(startDir.toAbsolutePath().normalize());
        }
        queue.add(startDir);

        while (!queue.isEmpty())
        {
            int levelSize = queue.size();
            Path bestCfcAtLevel = null;
            Path bestCfmAtLevel = null;
            List<Path> nextLevelDirs = new ArrayList<>();

            for (int i = 0; i < levelSize; i++)
            {
                Path currentDir = queue.poll();
                if (currentDir == null)
                {
                    continue;
                }

                List<Path> currentDirSubDirs = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir))
                {
                    for (Path entry : stream)
                    {
                        if (Files.isDirectory(entry))
                        {
                            try
                            {
                                Path realPath = entry.toRealPath();
                                if (visited.add(realPath))
                                {
                                    currentDirSubDirs.add(entry);
                                }
                            }
                            catch (IOException e)
                            {
                                if (visited.add(entry.toAbsolutePath().normalize()))
                                {
                                    currentDirSubDirs.add(entry);
                                }
                            }
                        }
                        else if (Files.isRegularFile(entry))
                        {
                            String name = entry.getFileName() != null ? entry.getFileName().toString() : "";
                            if (name.equalsIgnoreCase("Application.cfc"))
                            {
                                if (bestCfcAtLevel == null || entry.toString().compareToIgnoreCase(bestCfcAtLevel.toString()) < 0)
                                {
                                    bestCfcAtLevel = entry;
                                }
                            }
                            else if (name.equalsIgnoreCase("Application.cfm"))
                            {
                                if (bestCfmAtLevel == null || entry.toString().compareToIgnoreCase(bestCfmAtLevel.toString()) < 0)
                                {
                                    bestCfmAtLevel = entry;
                                }
                            }
                        }
                    }
                }
                catch (IOException | SecurityException ignored)
                {
                    // Skip inaccessible directories
                }

                currentDirSubDirs.sort(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER));
                nextLevelDirs.addAll(currentDirSubDirs);
            }

            if (bestCfcAtLevel != null)
            {
                return bestCfcAtLevel;
            }
            if (bestCfmAtLevel != null)
            {
                return bestCfmAtLevel;
            }

            queue.addAll(nextLevelDirs);
        }

        return null;
    }

    @Nullable
    public static Path findClosestApplicationFile(@Nullable String workspaceRoot)
    {
        return workspaceRoot != null ? findClosestApplicationFile(Paths.get(workspaceRoot)) : null;
    }

    @Nullable
    public static Path findClosestApplicationFile(@Nullable VirtualFile workspaceRoot)
    {
        if (workspaceRoot == null)
        {
            return null;
        }
        try
        {
            return findClosestApplicationFile(workspaceRoot.toNioPath());
        }
        catch (UnsupportedOperationException e)
        {
            String path = workspaceRoot.getPath();
            return findClosestApplicationFile(path);
        }
    }

    /**
     * Finds and returns the Path to the closest Application.cfc or Application.cfm file from a given Path, File, or VirtualFile,
     * traversing up the directory path towards the filesystem root.
     * In each directory checked, Application.cfc is prioritized over Application.cfm.
     *
     * @param path the starting Path
     * @return Path to the closest Application.cfc or Application.cfm file traversing upwards, or null if not found
     */
    @Nullable
    public static Path findClosestApplicationFileFromFile(@Nullable Path path)
    {
        if (path == null)
        {
            return null;
        }

        Path current = path.toAbsolutePath().normalize();
        if (!Files.exists(current))
        {
            return null;
        }

        Path currentDir;
        if (Files.isDirectory(current))
        {
            currentDir = current;
        }
        else
        {
            String fileName = current.getFileName() != null ? current.getFileName().toString() : "";
            if (fileName.equalsIgnoreCase("Application.cfc") || fileName.equalsIgnoreCase("Application.cfm"))
            {
                return current;
            }
            currentDir = current.getParent();
        }

        Set<Path> visited = new HashSet<>();

        while (currentDir != null && Files.exists(currentDir))
        {
            try
            {
                Path realDir = currentDir.toRealPath();
                if (!visited.add(realDir))
                {
                    break;
                }
            }
            catch (IOException e)
            {
                if (!visited.add(currentDir))
                {
                    break;
                }
            }

            Path foundCfc = null;
            Path foundCfm = null;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir))
            {
                for (Path entry : stream)
                {
                    if (Files.isRegularFile(entry))
                    {
                        String name = entry.getFileName() != null ? entry.getFileName().toString() : "";
                        if (name.equalsIgnoreCase("Application.cfc"))
                        {
                            foundCfc = entry;
                            break; // cfc has priority over cfm in the same directory
                        }
                        else if (name.equalsIgnoreCase("Application.cfm") && foundCfm == null)
                        {
                            foundCfm = entry;
                        }
                    }
                }
            }
            catch (IOException | SecurityException ignored)
            {
                // Skip inaccessible directories
            }

            if (foundCfc != null)
            {
                return foundCfc;
            }
            if (foundCfm != null)
            {
                return foundCfm;
            }

            currentDir = currentDir.getParent();
        }

        return null;
    }

    @Nullable
    public static Path findClosestApplicationFileFromFile(@Nullable File file)
    {
        return file != null ? findClosestApplicationFileFromFile(file.toPath()) : null;
    }

    @Nullable
    public static Path findClosestApplicationFileFromFile(@Nullable VirtualFile file)
    {
        if (file == null)
        {
            return null;
        }
        try
        {
            return findClosestApplicationFileFromFile(file.toNioPath());
        }
        catch (UnsupportedOperationException e)
        {
            String filePath = file.getPath();
            return findClosestApplicationFileFromFile(filePath);
        }
    }

    @Nullable
    public static Path findClosestApplicationFileFromFile(@Nullable String path)
    {
        return path != null ? findClosestApplicationFileFromFile(Paths.get(path)) : null;
    }

    /**
     * Converts a given File, VirtualFile, Path, or String file path into a ColdFusion Component (CFC) dot notation name.
     * Compares the target path against the closest Application.cfc/Application.cfm found using findClosestApplicationFileFromFile.
     * The relative path from the directory containing the Application file to the target file is converted into dot notation
     * by replacing directory separators with dots and removing the file extension.
     *
     * @param path the target Path
     * @return dot-notation CFC component name, or null if path or closest Application file cannot be resolved
     */
    @Nullable
    public static String PathToDotNotation(@Nullable Path path)
    {
        if (path == null)
        {
            return null;
        }

        Path appFile = findClosestApplicationFileFromFile(path);
        if (appFile == null)
        {
            return null;
        }

        Path baseDir = appFile.getParent();
        if (baseDir == null)
        {
            return null;
        }

        try
        {
            Path target = path.toAbsolutePath().normalize();
            Path base = baseDir.toAbsolutePath().normalize();

            Path relative = base.relativize(target);
            String relPathStr = relative.toString().replace('\\', '/');

            while (relPathStr.startsWith("/"))
            {
                relPathStr = relPathStr.substring(1);
            }

            int lastDot = relPathStr.lastIndexOf('.');
            int lastSlash = relPathStr.lastIndexOf('/');
            if (lastDot > lastSlash)
            {
                relPathStr = relPathStr.substring(0, lastDot);
            }

            if (relPathStr.isEmpty())
            {
                return "";
            }

            return relPathStr.replace('/', '.');
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Nullable
    public static String PathToDotNotation(@Nullable File file)
    {
        return file != null ? PathToDotNotation(file.toPath()) : null;
    }

    @Nullable
    public static String PathToDotNotation(@Nullable VirtualFile file)
    {
        if (file == null)
        {
            return null;
        }
        try
        {
            return PathToDotNotation(file.toNioPath());
        }
        catch (UnsupportedOperationException e)
        {
            String filePath = file.getPath();
            return PathToDotNotation(filePath);
        }
    }

    @Nullable
    public static String PathToDotNotation(@Nullable String path)
    {
        return path != null ? PathToDotNotation(Paths.get(path)) : null;
    }

    /**
     * Converts a ColdFusion Component (CFC) dot notation string into a Path relative to the closest Application file.
     * Finds the closest Application.cfc or Application.cfm to the searchRoot, and appends the dot notation segments
     * as directories, with the last item having the .cfc extension.
     *
     * @param searchRoot the root path to search from
     * @param dotNotation the component dot notation (e.g. "models.services.UserService")
     * @return Path to the component file, or null if searchRoot, dotNotation, or Application file cannot be resolved
     */
    @Nullable
    public static Path DotNotationToPath(@Nullable Path searchRoot, @Nullable String dotNotation)
    {
        if (searchRoot == null || dotNotation == null)
        {
            return null;
        }

        String notation = dotNotation.trim();
        if (notation.isEmpty())
        {
            return null;
        }

        Path appFile = findClosestApplicationFile(searchRoot);
        if (appFile == null)
        {
            return null;
        }

        Path baseDir = appFile.getParent();
        if (baseDir == null)
        {
            return null;
        }

        if (notation.toLowerCase().endsWith(".cfc"))
        {
            notation = notation.substring(0, notation.length() - 4);
        }

        String[] parts = notation.split("\\.");
        List<String> segments = new ArrayList<>();
        for (String part : parts)
        {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                segments.add(trimmed);
            }
        }

        if (segments.isEmpty())
        {
            return null;
        }

        Path result = baseDir;
        for (int i = 0; i < segments.size() - 1; i++)
        {
            result = result.resolve(segments.get(i));
        }

        String fileName = segments.get(segments.size() - 1);
        if (!fileName.toLowerCase().endsWith(".cfc"))
        {
            fileName += ".cfc";
        }
        result = result.resolve(fileName);

        return result.normalize();
    }

    @Nullable
    public static Path DotNotationToPath(@Nullable File searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToPath(searchRoot.toPath(), dotNotation) : null;
    }

    @Nullable
    public static Path DotNotationToPath(@Nullable VirtualFile searchRoot, @Nullable String dotNotation)
    {
        if (searchRoot == null)
        {
            return null;
        }
        try
        {
            return DotNotationToPath(searchRoot.toNioPath(), dotNotation);
        }
        catch (UnsupportedOperationException e)
        {
            return DotNotationToPath(Paths.get(searchRoot.getPath()), dotNotation);
        }
    }

    @Nullable
    public static Path DotNotationToPath(@Nullable String searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToPath(Paths.get(searchRoot), dotNotation) : null;
    }

    /**
     * Converts a ColdFusion Component (CFC) dot notation string into a File relative to the closest Application file.
     *
     * @param searchRoot the root path to search from
     * @param dotNotation the component dot notation (e.g. "models.services.UserService")
     * @return File of the component, or null if searchRoot, dotNotation, or Application file cannot be resolved
     */
    @Nullable
    public static File DotNotationToFile(@Nullable Path searchRoot, @Nullable String dotNotation)
    {
        Path path = DotNotationToPath(searchRoot, dotNotation);
        return path != null ? path.toFile() : null;
    }

    @Nullable
    public static File dotNotationToFile(@Nullable Path searchRoot, @Nullable String dotNotation)
    {
        return DotNotationToFile(searchRoot, dotNotation);
    }

    @Nullable
    public static File DotNotationToFile(@Nullable File searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToFile(searchRoot.toPath(), dotNotation) : null;
    }

    @Nullable
    public static File DotNotationToFile(@Nullable VirtualFile searchRoot, @Nullable String dotNotation)
    {
        Path path = DotNotationToPath(searchRoot, dotNotation);
        return path != null ? path.toFile() : null;
    }

    @Nullable
    public static File DotNotationToFile(@Nullable String searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToFile(Paths.get(searchRoot), dotNotation) : null;
    }

    /**
     * Converts a ColdFusion Component (CFC) dot notation string into a VirtualFile relative to the closest Application file.
     *
     * @param searchRoot the root path to search from
     * @param dotNotation the component dot notation (e.g. "models.services.UserService")
     * @return VirtualFile of the component, or null if searchRoot, dotNotation, or Application file cannot be resolved
     */
    @Nullable
    public static VirtualFile DotNotationToVirtualFile(@Nullable Path searchRoot, @Nullable String dotNotation)
    {
        Path path = DotNotationToPath(searchRoot, dotNotation);
        if (path == null)
        {
            return null;
        }
        try
        {
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            if (lfs != null)
            {
                VirtualFile vf = lfs.findFileByNioFile(path);
                if (vf == null)
                {
                    vf = lfs.refreshAndFindFileByNioFile(path);
                }
                return vf;
            }
        }
        catch (Throwable ignored)
        {
        }
        return null;
    }

    @Nullable
    public static VirtualFile DotNotationToVirtualFile(@Nullable File searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToVirtualFile(searchRoot.toPath(), dotNotation) : null;
    }

    @Nullable
    public static VirtualFile DotNotationToVirtualFile(@Nullable VirtualFile searchRoot, @Nullable String dotNotation)
    {
        Path path = DotNotationToPath(searchRoot, dotNotation);
        if (path == null)
        {
            return null;
        }
        try
        {
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            if (lfs != null)
            {
                VirtualFile vf = lfs.findFileByNioFile(path);
                if (vf == null)
                {
                    vf = lfs.refreshAndFindFileByNioFile(path);
                }
                return vf;
            }
        }
        catch (Throwable ignored)
        {
        }
        return null;
    }

    @Nullable
    public static VirtualFile DotNotationToVirtualFile(@Nullable String searchRoot, @Nullable String dotNotation)
    {
        return searchRoot != null ? DotNotationToVirtualFile(Paths.get(searchRoot), dotNotation) : null;
    }

}
