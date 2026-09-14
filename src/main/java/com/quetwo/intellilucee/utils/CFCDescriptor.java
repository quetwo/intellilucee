package com.quetwo.intellilucee.utils;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class CFCDescriptor
{
    private File cfcPath;
    private String dotNotation;

    public CFCDescriptor()
    {
    }

    public CFCDescriptor(@Nullable File cfcPath, @Nullable String dotNotation)
    {
        this.cfcPath = cfcPath;
        this.dotNotation = dotNotation;
    }

    public CFCDescriptor(@Nullable Path cfcPath, @Nullable String dotNotation)
    {
        this.cfcPath = cfcPath != null ? cfcPath.toFile() : null;
        this.dotNotation = dotNotation;
    }

    public CFCDescriptor(@Nullable String cfcPath, @Nullable String dotNotation)
    {
        this.cfcPath = cfcPath != null ? new File(cfcPath) : null;
        this.dotNotation = dotNotation;
    }

    @Nullable
    public File getCfcPath()
    {
        return cfcPath;
    }

    public void setCfcPath(@Nullable File cfcPath)
    {
        this.cfcPath = cfcPath;
    }

    @Nullable
    public File getPath()
    {
        return cfcPath;
    }

    public void setPath(@Nullable File path)
    {
        this.cfcPath = path;
    }

    @Nullable
    public File getFile()
    {
        return cfcPath;
    }

    public void setFile(@Nullable File file)
    {
        this.cfcPath = file;
    }

    @Nullable
    public File getCfcFile()
    {
        return cfcPath;
    }

    public void setCfcFile(@Nullable File cfcFile)
    {
        this.cfcPath = cfcFile;
    }

    @Nullable
    public String getDotNotation()
    {
        return dotNotation;
    }

    public void setDotNotation(@Nullable String dotNotation)
    {
        this.dotNotation = dotNotation;
    }

    @Nullable
    public String getCfcDotNotation()
    {
        return dotNotation;
    }

    public void setCfcDotNotation(@Nullable String cfcDotNotation)
    {
        this.dotNotation = cfcDotNotation;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || !(o instanceof CFCDescriptor)) return false;
        CFCDescriptor that = (CFCDescriptor) o;
        return Objects.equals(cfcPath, that.cfcPath) &&
               Objects.equals(dotNotation, that.dotNotation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(cfcPath, dotNotation);
    }

    @Override
    public String toString()
    {
        return "CFCDescriptor{" +
                "cfcPath=" + cfcPath +
                ", dotNotation='" + dotNotation + '\'' +
                '}';
    }
}
