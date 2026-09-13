package com.quetwo.intellilucee.utils;

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


}
