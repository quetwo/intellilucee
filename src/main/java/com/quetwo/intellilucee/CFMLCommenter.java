package com.quetwo.intellilucee;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

//TODO: Implement proper commenter

public class CFMLCommenter implements Commenter
{
    @Override
    public @Nullable String getLineCommentPrefix() {
        return "";
    }

    @Override
    public @Unmodifiable @NotNull List<String> getLineCommentPrefixes() {
        return Commenter.super.getLineCommentPrefixes();
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return "//";
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return "/*";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return "*/";
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return "";
    }

    @Override
    public boolean blockCommentRequiresFullLineSelection() {
        return Commenter.super.blockCommentRequiresFullLineSelection();
    }
}
