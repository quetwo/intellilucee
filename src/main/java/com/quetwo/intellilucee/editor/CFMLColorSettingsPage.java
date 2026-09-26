package com.quetwo.intellilucee.editor;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.quetwo.intellilucee.CFMLIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

public class CFMLColorSettingsPage implements ColorSettingsPage
{

    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor("Keyword", CFMLSyntaxHighlighter.KEYWORD),
        new AttributesDescriptor("Tag", CFMLSyntaxHighlighter.TAG),
        new AttributesDescriptor("Tag name", CFMLSyntaxHighlighter.TAG_NAME),
        new AttributesDescriptor("Tag attribute", CFMLSyntaxHighlighter.ATTRIBUTE_NAME),
        new AttributesDescriptor("String", CFMLSyntaxHighlighter.STRING),
        new AttributesDescriptor("Number", CFMLSyntaxHighlighter.NUMBER),
        new AttributesDescriptor("Line comment", CFMLSyntaxHighlighter.LINE_COMMENT),
        new AttributesDescriptor("Block comment", CFMLSyntaxHighlighter.BLOCK_COMMENT),
        new AttributesDescriptor("Doc comment", CFMLSyntaxHighlighter.DOC_COMMENT),
        new AttributesDescriptor("Tag comment", CFMLSyntaxHighlighter.TAG_COMMENT),
        new AttributesDescriptor("Operator", CFMLSyntaxHighlighter.OPERATOR),
        new AttributesDescriptor("Brackets", CFMLSyntaxHighlighter.BRACKETS),
        new AttributesDescriptor("Braces", CFMLSyntaxHighlighter.BRACES),
        new AttributesDescriptor("Parentheses", CFMLSyntaxHighlighter.PARENTHESES),
        new AttributesDescriptor("Comma", CFMLSyntaxHighlighter.COMMA),
        new AttributesDescriptor("Semicolon", CFMLSyntaxHighlighter.SEMICOLON),
        new AttributesDescriptor("Dot", CFMLSyntaxHighlighter.DOT),
        new AttributesDescriptor("Type", CFMLSyntaxHighlighter.TYPE),
        new AttributesDescriptor("Scope", CFMLSyntaxHighlighter.SCOPE),
        new AttributesDescriptor("Hash interpolation", CFMLSyntaxHighlighter.HASH),
        new AttributesDescriptor("Identifier", CFMLSyntaxHighlighter.IDENTIFIER),
        new AttributesDescriptor("Function declaration", CFMLSyntaxHighlighter.FUNCTION_DECLARATION),
        new AttributesDescriptor("Function call", CFMLSyntaxHighlighter.FUNCTION_CALL),
        new AttributesDescriptor("Bad character", CFMLSyntaxHighlighter.BAD_CHARACTER)
    };

    @Override
    public @Nullable Icon getIcon()
    {
        return CFMLIcon.FILE;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter()
    {
        return new CFMLSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText()
    {
        return "<!--- CFML Tag Comment --->\n" +
               "<cfcomponent displayname=\"UserService\">\n" +
               "    <cffunction name=\"getUser\" access=\"public\" returntype=\"struct\">\n" +
               "        <cfargument name=\"userId\" type=\"numeric\" required=\"true\">\n" +
               "        <cfset var user = { id: arguments.userId, name: \"John Doe\" }>\n" +
               "        <cfoutput>Fetching user: #user.name#</cfoutput>\n" +
               "        <cfreturn user>\n" +
               "    </cffunction>\n" +
               "</cfcomponent>\n\n" +
               "// Script syntax\n" +
               "component {\n" +
               "    /* Block comment */\n" +
               "    public string function greet(required string name) {\n" +
               "        var message = \"Hello, \" & arguments.name & \"!\";\n" +
               "        if (session.isLoggedIn eq true) {\n" +
               "            writeOutput(message);\n" +
               "        }\n" +
               "        return message;\n" +
               "    }\n" +
               "}";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap()
    {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors()
    {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors()
    {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName()
    {
        return "CFML";
    }

}
