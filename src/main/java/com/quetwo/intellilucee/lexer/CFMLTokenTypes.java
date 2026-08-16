package com.quetwo.intellilucee.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public final class CFMLTokenTypes
{
    public static final IElementType    TEXT                = new CFMLTokenType( "TEXT");
    public static final IElementType    COMMENT             = new CFMLTokenType( "COMMENT");
    public static final IElementType	KEYWORD				= new CFMLTokenType( "KEYWORD" );
    public static final IElementType	IDENTIFIER			= new CFMLTokenType( "IDENTIFIER" );
    public static final IElementType	NUMBER				= new CFMLTokenType( "NUMBER" );
    public static final IElementType	STRING				= new CFMLTokenType( "STRING" );
    public static final IElementType	HASH_SIGN			= new CFMLTokenType( "HASH_SIGN" );
    public static final IElementType	LINE_COMMENT		= new CFMLTokenType( "LINE_COMMENT" );
    public static final IElementType	BLOCK_COMMENT		= new CFMLTokenType( "BLOCK_COMMENT" );
    public static final IElementType	DOC_COMMENT			= new CFMLTokenType( "DOC_COMMENT" );
    public static final IElementType	OPERATOR			= new CFMLTokenType( "OPERATOR" );
    public static final IElementType	TAG					= new CFMLTokenType( "TAG" );
    public static final IElementType	BRACE				= new CFMLTokenType( "BRACE" );
    public static final IElementType	PAREN				= new CFMLTokenType( "PAREN" );
    public static final IElementType	BRACKET				= new CFMLTokenType( "BRACKET" );
    public static final IElementType	COMMA				= new CFMLTokenType( "COMMA" );
    public static final IElementType	DOT					= new CFMLTokenType( "DOT" );
    public static final IElementType	SEMICOLON			= new CFMLTokenType( "SEMICOLON" );
    public static final IElementType	ANNOTATION			= new CFMLTokenType( "ANNOTATION" );
    public static final IElementType	FUNCTION_NAME		= new CFMLTokenType( "FUNCTION_NAME" );
    public static final IElementType	CONSTANT			= new CFMLTokenType( "CONSTANT" );
    public static final IElementType	SCOPE_VARIABLE		= new CFMLTokenType( "SCOPE_VARIABLE" );
    public static final IElementType	STORAGE_TYPE		= new CFMLTokenType( "STORAGE_TYPE" );
    public static final IElementType	STORAGE_MODIFIER	= new CFMLTokenType( "STORAGE_MODIFIER" );
    public static final IElementType	BUILTIN_FUNCTION	= new CFMLTokenType( "BUILTIN_FUNCTION" );
    public static final IElementType	STRUCT_KEY			= new CFMLTokenType( "STRUCT_KEY" );
    public static final IElementType	FUNCTION_CALL		= new CFMLTokenType( "FUNCTION_CALL" );
    public static final IElementType	NAMED_ARGUMENT		= new CFMLTokenType( "NAMED_ARGUMENT" );
    public static final IElementType	BAD_CHARACTER		= TokenType.BAD_CHARACTER;

    private CFMLTokenTypes()
    {
    }
}