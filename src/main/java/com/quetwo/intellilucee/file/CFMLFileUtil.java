package com.quetwo.intellilucee.file;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CFMLFileUtil
{

    private  CFMLFileUtil()
    {

    }

    public static boolean isCFMLFIle(@Nullable VirtualFile file )
    {
        return file != null && isCFMLxtension( file.getExtension() );
    }

    public static boolean isCFMLxtension(@Nullable String extension )
    {
        if ( extension == null || extension.isBlank() ) {
            return false;
        }

        String normalized = extension.toLowerCase( Locale.ROOT );
        return normalized.equals( "cfml" ) || normalized.equals( "cfm" ) || normalized.equals( "cfc" ) || normalized.equals( "cfs" );
    }

}
