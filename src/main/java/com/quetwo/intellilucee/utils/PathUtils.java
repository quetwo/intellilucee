package com.quetwo.intellilucee.utils;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public static Path findApplicationFile(@Nullable Path workspaceRoot)
    {
        return findClosestApplicationFile(workspaceRoot);
    }


}
