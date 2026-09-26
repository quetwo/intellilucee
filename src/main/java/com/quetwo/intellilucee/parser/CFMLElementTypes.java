package com.quetwo.intellilucee.parser;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.quetwo.intellilucee.CFMLLanguage;

public final class CFMLElementTypes
{

    public static final IFileElementType FILE = new IFileElementType("CFML_FILE", CFMLLanguage.INSTANCE);

    // Tag Elements
    public static final IElementType TAG = new CFMLElementType("CFML_TAG");
    public static final IElementType TAG_HEADER = new CFMLElementType("CFML_TAG_HEADER");
    public static final IElementType TAG_FOOTER = new CFMLElementType("CFML_TAG_FOOTER");
    public static final IElementType TAG_NAME_ELEMENT = new CFMLElementType("CFML_TAG_NAME_ELEMENT");
    public static final IElementType TAG_ATTRIBUTE = new CFMLElementType("CFML_TAG_ATTRIBUTE");
    public static final IElementType TAG_ATTRIBUTE_NAME = new CFMLElementType("CFML_TAG_ATTRIBUTE_NAME");
    public static final IElementType TAG_ATTRIBUTE_VALUE = new CFMLElementType("CFML_TAG_ATTRIBUTE_VALUE");
    public static final IElementType TAG_BODY = new CFMLElementType("CFML_TAG_BODY");
    public static final IElementType TAG_COMMENT_ELEMENT = new CFMLElementType("CFML_TAG_COMMENT_ELEMENT");
    public static final IElementType SCRIPT_BLOCK = new CFMLElementType("CFML_SCRIPT_BLOCK");

    // Script Declarations & Statements
    public static final IElementType COMPONENT_DECLARATION = new CFMLElementType("CFML_COMPONENT_DECLARATION");
    public static final IElementType INTERFACE_DECLARATION = new CFMLElementType("CFML_INTERFACE_DECLARATION");
    public static final IElementType FUNCTION_DECLARATION = new CFMLElementType("CFML_FUNCTION_DECLARATION");
    public static final IElementType PARAMETER_LIST = new CFMLElementType("CFML_PARAMETER_LIST");
    public static final IElementType PARAMETER = new CFMLElementType("CFML_PARAMETER");
    public static final IElementType BLOCK_STATEMENT = new CFMLElementType("CFML_BLOCK_STATEMENT");
    public static final IElementType VAR_DECLARATION_STATEMENT = new CFMLElementType("CFML_VAR_DECLARATION_STATEMENT");
    public static final IElementType PROPERTY_STATEMENT = new CFMLElementType("CFML_PROPERTY_STATEMENT");
    public static final IElementType IMPORT_STATEMENT = new CFMLElementType("CFML_IMPORT_STATEMENT");
    public static final IElementType INCLUDE_STATEMENT = new CFMLElementType("CFML_INCLUDE_STATEMENT");
    public static final IElementType IF_STATEMENT = new CFMLElementType("CFML_IF_STATEMENT");
    public static final IElementType WHILE_STATEMENT = new CFMLElementType("CFML_WHILE_STATEMENT");
    public static final IElementType DO_WHILE_STATEMENT = new CFMLElementType("CFML_DO_WHILE_STATEMENT");
    public static final IElementType FOR_STATEMENT = new CFMLElementType("CFML_FOR_STATEMENT");
    public static final IElementType SWITCH_STATEMENT = new CFMLElementType("CFML_SWITCH_STATEMENT");
    public static final IElementType CASE_CLAUSE = new CFMLElementType("CFML_CASE_CLAUSE");
    public static final IElementType DEFAULT_CLAUSE = new CFMLElementType("CFML_DEFAULT_CLAUSE");
    public static final IElementType TRY_STATEMENT = new CFMLElementType("CFML_TRY_STATEMENT");
    public static final IElementType CATCH_CLAUSE = new CFMLElementType("CFML_CATCH_CLAUSE");
    public static final IElementType FINALLY_CLAUSE = new CFMLElementType("CFML_FINALLY_CLAUSE");
    public static final IElementType RETURN_STATEMENT = new CFMLElementType("CFML_RETURN_STATEMENT");
    public static final IElementType THROW_STATEMENT = new CFMLElementType("CFML_THROW_STATEMENT");
    public static final IElementType RETHROW_STATEMENT = new CFMLElementType("CFML_RETHROW_STATEMENT");

    // Expressions
    public static final IElementType EXPRESSION_STATEMENT = new CFMLElementType("CFML_EXPRESSION_STATEMENT");
    public static final IElementType ASSIGNMENT_EXPRESSION = new CFMLElementType("CFML_ASSIGNMENT_EXPRESSION");
    public static final IElementType BINARY_EXPRESSION = new CFMLElementType("CFML_BINARY_EXPRESSION");
    public static final IElementType UNARY_EXPRESSION = new CFMLElementType("CFML_UNARY_EXPRESSION");
    public static final IElementType POSTFIX_EXPRESSION = new CFMLElementType("CFML_POSTFIX_EXPRESSION");
    public static final IElementType CALL_EXPRESSION = new CFMLElementType("CFML_CALL_EXPRESSION");
    public static final IElementType ARGUMENT_LIST = new CFMLElementType("CFML_ARGUMENT_LIST");
    public static final IElementType ARGUMENT = new CFMLElementType("CFML_ARGUMENT");
    public static final IElementType REFERENCE_EXPRESSION = new CFMLElementType("CFML_REFERENCE_EXPRESSION");
    public static final IElementType INDEX_EXPRESSION = new CFMLElementType("CFML_INDEX_EXPRESSION");
    public static final IElementType STRUCT_LITERAL = new CFMLElementType("CFML_STRUCT_LITERAL");
    public static final IElementType STRUCT_ENTRY = new CFMLElementType("CFML_STRUCT_ENTRY");
    public static final IElementType ARRAY_LITERAL = new CFMLElementType("CFML_ARRAY_LITERAL");
    public static final IElementType LITERAL_EXPRESSION = new CFMLElementType("CFML_LITERAL_EXPRESSION");
    public static final IElementType PARENTHESIZED_EXPRESSION = new CFMLElementType("CFML_PARENTHESIZED_EXPRESSION");
    public static final IElementType TERNARY_EXPRESSION = new CFMLElementType("CFML_TERNARY_EXPRESSION");
    public static final IElementType HASH_EXPRESSION = new CFMLElementType("CFML_HASH_EXPRESSION");

    private CFMLElementTypes()
    {
    }

}