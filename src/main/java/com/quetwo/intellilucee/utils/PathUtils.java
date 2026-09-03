package com.quetwo.intellilucee.utils;

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

}
