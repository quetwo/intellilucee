package com.quetwo.intellilucee.parser;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public final class CFMLTokenTypes
{

    // Basic & Legacy tokens
    public static final IElementType TEXT = new CFMLTokenType("CFML_TEXT");
    public static final IElementType IDENTIFIER = new CFMLTokenType("CFML_IDENTIFIER");
    public static final IElementType COMMENT = new CFMLTokenType("CFML_COMMENT");
    public static final IElementType STRING = new CFMLTokenType("CFML_STRING");
    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    // Comments
    public static final IElementType LINE_COMMENT = new CFMLTokenType("CFML_LINE_COMMENT");
    public static final IElementType BLOCK_COMMENT = new CFMLTokenType("CFML_BLOCK_COMMENT");
    public static final IElementType TAG_COMMENT = new CFMLTokenType("CFML_TAG_COMMENT");
    public static final IElementType DOC_COMMENT = new CFMLTokenType("CFML_DOC_COMMENT");

    // Strings & Interpolation
    public static final IElementType SINGLE_QUOTED_STRING = new CFMLTokenType("CFML_SINGLE_QUOTED_STRING");
    public static final IElementType DOUBLE_QUOTED_STRING = new CFMLTokenType("CFML_DOUBLE_QUOTED_STRING");
    public static final IElementType HASH = new CFMLTokenType("CFML_HASH");
    public static final IElementType ESCAPED_HASH = new CFMLTokenType("CFML_ESCAPED_HASH");

    // Numbers
    public static final IElementType INTEGER_LITERAL = new CFMLTokenType("CFML_INTEGER_LITERAL");
    public static final IElementType FLOAT_LITERAL = new CFMLTokenType("CFML_FLOAT_LITERAL");
    public static final IElementType HEX_LITERAL = new CFMLTokenType("CFML_HEX_LITERAL");

    // Literals / Constants
    public static final IElementType TRUE_KEYWORD = new CFMLTokenType("CFML_TRUE_KEYWORD");
    public static final IElementType FALSE_KEYWORD = new CFMLTokenType("CFML_FALSE_KEYWORD");
    public static final IElementType NULL_KEYWORD = new CFMLTokenType("CFML_NULL_KEYWORD");
    public static final IElementType YES_KEYWORD = new CFMLTokenType("CFML_YES_KEYWORD");
    public static final IElementType NO_KEYWORD = new CFMLTokenType("CFML_NO_KEYWORD");

    // Keywords
    public static final IElementType VAR_KEYWORD = new CFMLTokenType("CFML_VAR_KEYWORD");
    public static final IElementType LOCAL_KEYWORD = new CFMLTokenType("CFML_LOCAL_KEYWORD");
    public static final IElementType FUNCTION_KEYWORD = new CFMLTokenType("CFML_FUNCTION_KEYWORD");
    public static final IElementType COMPONENT_KEYWORD = new CFMLTokenType("CFML_COMPONENT_KEYWORD");
    public static final IElementType INTERFACE_KEYWORD = new CFMLTokenType("CFML_INTERFACE_KEYWORD");
    public static final IElementType EXTENDS_KEYWORD = new CFMLTokenType("CFML_EXTENDS_KEYWORD");
    public static final IElementType IMPLEMENTS_KEYWORD = new CFMLTokenType("CFML_IMPLEMENTS_KEYWORD");
    public static final IElementType IMPORT_KEYWORD = new CFMLTokenType("CFML_IMPORT_KEYWORD");
    public static final IElementType INCLUDE_KEYWORD = new CFMLTokenType("CFML_INCLUDE_KEYWORD");
    public static final IElementType PROPERTY_KEYWORD = new CFMLTokenType("CFML_PROPERTY_KEYWORD");
    public static final IElementType PARAM_KEYWORD = new CFMLTokenType("CFML_PARAM_KEYWORD");
    public static final IElementType NEW_KEYWORD = new CFMLTokenType("CFML_NEW_KEYWORD");
    public static final IElementType IF_KEYWORD = new CFMLTokenType("CFML_IF_KEYWORD");
    public static final IElementType ELSE_KEYWORD = new CFMLTokenType("CFML_ELSE_KEYWORD");
    public static final IElementType ELSEIF_KEYWORD = new CFMLTokenType("CFML_ELSEIF_KEYWORD");
    public static final IElementType WHILE_KEYWORD = new CFMLTokenType("CFML_WHILE_KEYWORD");
    public static final IElementType DO_KEYWORD = new CFMLTokenType("CFML_DO_KEYWORD");
    public static final IElementType FOR_KEYWORD = new CFMLTokenType("CFML_FOR_KEYWORD");
    public static final IElementType IN_KEYWORD = new CFMLTokenType("CFML_IN_KEYWORD");
    public static final IElementType SWITCH_KEYWORD = new CFMLTokenType("CFML_SWITCH_KEYWORD");
    public static final IElementType CASE_KEYWORD = new CFMLTokenType("CFML_CASE_KEYWORD");
    public static final IElementType DEFAULT_KEYWORD = new CFMLTokenType("CFML_DEFAULT_KEYWORD");
    public static final IElementType BREAK_KEYWORD = new CFMLTokenType("CFML_BREAK_KEYWORD");
    public static final IElementType CONTINUE_KEYWORD = new CFMLTokenType("CFML_CONTINUE_KEYWORD");
    public static final IElementType RETURN_KEYWORD = new CFMLTokenType("CFML_RETURN_KEYWORD");
    public static final IElementType TRY_KEYWORD = new CFMLTokenType("CFML_TRY_KEYWORD");
    public static final IElementType CATCH_KEYWORD = new CFMLTokenType("CFML_CATCH_KEYWORD");
    public static final IElementType FINALLY_KEYWORD = new CFMLTokenType("CFML_FINALLY_KEYWORD");
    public static final IElementType THROW_KEYWORD = new CFMLTokenType("CFML_THROW_KEYWORD");
    public static final IElementType RETHROW_KEYWORD = new CFMLTokenType("CFML_RETHROW_KEYWORD");
    public static final IElementType ABORT_KEYWORD = new CFMLTokenType("CFML_ABORT_KEYWORD");
    public static final IElementType LOCK_KEYWORD = new CFMLTokenType("CFML_LOCK_KEYWORD");
    public static final IElementType TRANSACTION_KEYWORD = new CFMLTokenType("CFML_TRANSACTION_KEYWORD");
    public static final IElementType THREAD_KEYWORD = new CFMLTokenType("CFML_THREAD_KEYWORD");
    public static final IElementType PUBLIC_KEYWORD = new CFMLTokenType("CFML_PUBLIC_KEYWORD");
    public static final IElementType PRIVATE_KEYWORD = new CFMLTokenType("CFML_PRIVATE_KEYWORD");
    public static final IElementType PACKAGE_KEYWORD = new CFMLTokenType("CFML_PACKAGE_KEYWORD");
    public static final IElementType REMOTE_KEYWORD = new CFMLTokenType("CFML_REMOTE_KEYWORD");
    public static final IElementType STATIC_KEYWORD = new CFMLTokenType("CFML_STATIC_KEYWORD");
    public static final IElementType FINAL_KEYWORD = new CFMLTokenType("CFML_FINAL_KEYWORD");
    public static final IElementType ABSTRACT_KEYWORD = new CFMLTokenType("CFML_ABSTRACT_KEYWORD");
    public static final IElementType REQUIRED_KEYWORD = new CFMLTokenType("CFML_REQUIRED_KEYWORD");

    // Built-in Types
    public static final IElementType TYPE_ANY = new CFMLTokenType("CFML_TYPE_ANY");
    public static final IElementType TYPE_ARRAY = new CFMLTokenType("CFML_TYPE_ARRAY");
    public static final IElementType TYPE_BINARY = new CFMLTokenType("CFML_TYPE_BINARY");
    public static final IElementType TYPE_BOOLEAN = new CFMLTokenType("CFML_TYPE_BOOLEAN");
    public static final IElementType TYPE_COMPONENT = new CFMLTokenType("CFML_TYPE_COMPONENT");
    public static final IElementType TYPE_DATE = new CFMLTokenType("CFML_TYPE_DATE");
    public static final IElementType TYPE_FUNCTION = new CFMLTokenType("CFML_TYPE_FUNCTION");
    public static final IElementType TYPE_GUID = new CFMLTokenType("CFML_TYPE_GUID");
    public static final IElementType TYPE_NUMERIC = new CFMLTokenType("CFML_TYPE_NUMERIC");
    public static final IElementType TYPE_QUERY = new CFMLTokenType("CFML_TYPE_QUERY");
    public static final IElementType TYPE_STRING = new CFMLTokenType("CFML_TYPE_STRING");
    public static final IElementType TYPE_STRUCT = new CFMLTokenType("CFML_TYPE_STRUCT");
    public static final IElementType TYPE_UUID = new CFMLTokenType("CFML_TYPE_UUID");
    public static final IElementType TYPE_VOID = new CFMLTokenType("CFML_TYPE_VOID");
    public static final IElementType TYPE_XML = new CFMLTokenType("CFML_TYPE_XML");
    public static final IElementType TYPE_VARIABLENAME = new CFMLTokenType("CFML_TYPE_VARIABLENAME");

    // Built-in Scopes
    public static final IElementType SCOPE_APPLICATION = new CFMLTokenType("CFML_SCOPE_APPLICATION");
    public static final IElementType SCOPE_ARGUMENTS = new CFMLTokenType("CFML_SCOPE_ARGUMENTS");
    public static final IElementType SCOPE_ATTRIBUTES = new CFMLTokenType("CFML_SCOPE_ATTRIBUTES");
    public static final IElementType SCOPE_CALLER = new CFMLTokenType("CFML_SCOPE_CALLER");
    public static final IElementType SCOPE_CGI = new CFMLTokenType("CFML_SCOPE_CGI");
    public static final IElementType SCOPE_CLIENT = new CFMLTokenType("CFML_SCOPE_CLIENT");
    public static final IElementType SCOPE_COOKIE = new CFMLTokenType("CFML_SCOPE_COOKIE");
    public static final IElementType SCOPE_FLASH = new CFMLTokenType("CFML_SCOPE_FLASH");
    public static final IElementType SCOPE_FORM = new CFMLTokenType("CFML_SCOPE_FORM");
    public static final IElementType SCOPE_LOCAL = new CFMLTokenType("CFML_SCOPE_LOCAL");
    public static final IElementType SCOPE_REQUEST = new CFMLTokenType("CFML_SCOPE_REQUEST");
    public static final IElementType SCOPE_SERVER = new CFMLTokenType("CFML_SCOPE_SERVER");
    public static final IElementType SCOPE_SESSION = new CFMLTokenType("CFML_SCOPE_SESSION");
    public static final IElementType SCOPE_THIS = new CFMLTokenType("CFML_SCOPE_THIS");
    public static final IElementType SCOPE_THISTAG = new CFMLTokenType("CFML_SCOPE_THISTAG");
    public static final IElementType SCOPE_URL = new CFMLTokenType("CFML_SCOPE_URL");
    public static final IElementType SCOPE_VARIABLES = new CFMLTokenType("CFML_SCOPE_VARIABLES");
    public static final IElementType SCOPE_SELF = new CFMLTokenType("CFML_SCOPE_SELF");
    public static final IElementType SCOPE_SUPER = new CFMLTokenType("CFML_SCOPE_SUPER");

    // Word Operators
    public static final IElementType OP_AND = new CFMLTokenType("CFML_OP_AND");
    public static final IElementType OP_OR = new CFMLTokenType("CFML_OP_OR");
    public static final IElementType OP_NOT = new CFMLTokenType("CFML_OP_NOT");
    public static final IElementType OP_EQ = new CFMLTokenType("CFML_OP_EQ");
    public static final IElementType OP_NEQ = new CFMLTokenType("CFML_OP_NEQ");
    public static final IElementType OP_LT = new CFMLTokenType("CFML_OP_LT");
    public static final IElementType OP_LTE = new CFMLTokenType("CFML_OP_LTE");
    public static final IElementType OP_GT = new CFMLTokenType("CFML_OP_GT");
    public static final IElementType OP_GTE = new CFMLTokenType("CFML_OP_GTE");
    public static final IElementType OP_IS = new CFMLTokenType("CFML_OP_IS");
    public static final IElementType OP_EQUAL = new CFMLTokenType("CFML_OP_EQUAL");
    public static final IElementType OP_CONTAINS = new CFMLTokenType("CFML_OP_CONTAINS");
    public static final IElementType OP_DOES_NOT_CONTAIN = new CFMLTokenType("CFML_OP_DOES_NOT_CONTAIN");
    public static final IElementType OP_GREATER_THAN = new CFMLTokenType("CFML_OP_GREATER_THAN");
    public static final IElementType OP_LESS_THAN = new CFMLTokenType("CFML_OP_LESS_THAN");
    public static final IElementType OP_GREATER_THAN_OR_EQUAL_TO = new CFMLTokenType("CFML_OP_GREATER_THAN_OR_EQUAL_TO");
    public static final IElementType OP_LESS_THAN_OR_EQUAL_TO = new CFMLTokenType("CFML_OP_LESS_THAN_OR_EQUAL_TO");
    public static final IElementType OP_XOR = new CFMLTokenType("CFML_OP_XOR");
    public static final IElementType OP_EQV = new CFMLTokenType("CFML_OP_EQV");
    public static final IElementType OP_IMP = new CFMLTokenType("CFML_OP_IMP");
    public static final IElementType OP_MOD = new CFMLTokenType("CFML_OP_MOD");
    public static final IElementType OP_TO = new CFMLTokenType("CFML_OP_TO");

    // Symbol Operators
    public static final IElementType PLUS = new CFMLTokenType("CFML_PLUS");
    public static final IElementType MINUS = new CFMLTokenType("CFML_MINUS");
    public static final IElementType MULTIPLY = new CFMLTokenType("CFML_MULTIPLY");
    public static final IElementType DIVIDE = new CFMLTokenType("CFML_DIVIDE");
    public static final IElementType MODULO = new CFMLTokenType("CFML_MODULO");
    public static final IElementType POWER = new CFMLTokenType("CFML_POWER");
    public static final IElementType PLUS_PLUS = new CFMLTokenType("CFML_PLUS_PLUS");
    public static final IElementType MINUS_MINUS = new CFMLTokenType("CFML_MINUS_MINUS");
    public static final IElementType PLUS_ASSIGN = new CFMLTokenType("CFML_PLUS_ASSIGN");
    public static final IElementType MINUS_ASSIGN = new CFMLTokenType("CFML_MINUS_ASSIGN");
    public static final IElementType MUL_ASSIGN = new CFMLTokenType("CFML_MUL_ASSIGN");
    public static final IElementType DIV_ASSIGN = new CFMLTokenType("CFML_DIV_ASSIGN");
    public static final IElementType MOD_ASSIGN = new CFMLTokenType("CFML_MOD_ASSIGN");
    public static final IElementType POW_ASSIGN = new CFMLTokenType("CFML_POW_ASSIGN");
    public static final IElementType CONCAT_ASSIGN = new CFMLTokenType("CFML_CONCAT_ASSIGN");
    public static final IElementType EQ_EQ = new CFMLTokenType("CFML_EQ_EQ");
    public static final IElementType NOT_EQ = new CFMLTokenType("CFML_NOT_EQ");
    public static final IElementType LESS = new CFMLTokenType("CFML_LESS");
    public static final IElementType LESS_EQ = new CFMLTokenType("CFML_LESS_EQ");
    public static final IElementType GREATER = new CFMLTokenType("CFML_GREATER");
    public static final IElementType GREATER_EQ = new CFMLTokenType("CFML_GREATER_EQ");
    public static final IElementType EXACT_EQ = new CFMLTokenType("CFML_EXACT_EQ");
    public static final IElementType EXACT_NOT_EQ = new CFMLTokenType("CFML_EXACT_NOT_EQ");
    public static final IElementType SPACESHIP = new CFMLTokenType("CFML_SPACESHIP");
    public static final IElementType AND_AND = new CFMLTokenType("CFML_AND_AND");
    public static final IElementType OR_OR = new CFMLTokenType("CFML_OR_OR");
    public static final IElementType EXCL = new CFMLTokenType("CFML_EXCL");
    public static final IElementType BIT_AND = new CFMLTokenType("CFML_BIT_AND");
    public static final IElementType BIT_OR = new CFMLTokenType("CFML_BIT_OR");
    public static final IElementType BIT_XOR = new CFMLTokenType("CFML_BIT_XOR");
    public static final IElementType BIT_NOT = new CFMLTokenType("CFML_BIT_NOT");
    public static final IElementType SHIFT_LEFT = new CFMLTokenType("CFML_SHIFT_LEFT");
    public static final IElementType SHIFT_RIGHT = new CFMLTokenType("CFML_SHIFT_RIGHT");
    public static final IElementType SHIFT_RIGHT_UNSIGNED = new CFMLTokenType("CFML_SHIFT_RIGHT_UNSIGNED");
    public static final IElementType QUESTION = new CFMLTokenType("CFML_QUESTION");
    public static final IElementType COLON = new CFMLTokenType("CFML_COLON");
    public static final IElementType ELVIS = new CFMLTokenType("CFML_ELVIS");
    public static final IElementType SAFE_NAV = new CFMLTokenType("CFML_SAFE_NAV");
    public static final IElementType ARROW = new CFMLTokenType("CFML_ARROW");
    public static final IElementType ASSIGN = new CFMLTokenType("CFML_ASSIGN");

    // Delimiters & Punctuation
    public static final IElementType LPAREN = new CFMLTokenType("CFML_LPAREN");
    public static final IElementType RPAREN = new CFMLTokenType("CFML_RPAREN");
    public static final IElementType LBRACE = new CFMLTokenType("CFML_LBRACE");
    public static final IElementType RBRACE = new CFMLTokenType("CFML_RBRACE");
    public static final IElementType LBRACKET = new CFMLTokenType("CFML_LBRACKET");
    public static final IElementType RBRACKET = new CFMLTokenType("CFML_RBRACKET");
    public static final IElementType SEMICOLON = new CFMLTokenType("CFML_SEMICOLON");
    public static final IElementType COMMA = new CFMLTokenType("CFML_COMMA");
    public static final IElementType DOT = new CFMLTokenType("CFML_DOT");

    // Tags & Markup
    public static final IElementType TAG_OPEN_START = new CFMLTokenType("CFML_TAG_OPEN_START");
    public static final IElementType TAG_CLOSE_START = new CFMLTokenType("CFML_TAG_CLOSE_START");
    public static final IElementType TAG_END = new CFMLTokenType("CFML_TAG_END");
    public static final IElementType TAG_EMPTY_END = new CFMLTokenType("CFML_TAG_EMPTY_END");
    public static final IElementType TAG_NAME = new CFMLTokenType("CFML_TAG_NAME");
    public static final IElementType ATTRIBUTE_NAME = new CFMLTokenType("CFML_ATTRIBUTE_NAME");
    public static final IElementType ATTRIBUTE_VALUE = new CFMLTokenType("CFML_ATTRIBUTE_VALUE");
    public static final IElementType TAG_TEXT = new CFMLTokenType("CFML_TAG_TEXT");

    // Token Sets
    public static final TokenSet COMMENTS = TokenSet.create(
        COMMENT,
        LINE_COMMENT,
        BLOCK_COMMENT,
        TAG_COMMENT,
        DOC_COMMENT
    );

    public static final TokenSet STRINGS = TokenSet.create(
        STRING,
        SINGLE_QUOTED_STRING,
        DOUBLE_QUOTED_STRING
    );

    public static final TokenSet NUMBERS = TokenSet.create(
        INTEGER_LITERAL,
        FLOAT_LITERAL,
        HEX_LITERAL
    );

    public static final TokenSet KEYWORDS = TokenSet.create(
        VAR_KEYWORD,
        LOCAL_KEYWORD,
        FUNCTION_KEYWORD,
        COMPONENT_KEYWORD,
        INTERFACE_KEYWORD,
        EXTENDS_KEYWORD,
        IMPLEMENTS_KEYWORD,
        IMPORT_KEYWORD,
        INCLUDE_KEYWORD,
        PROPERTY_KEYWORD,
        PARAM_KEYWORD,
        NEW_KEYWORD,
        IF_KEYWORD,
        ELSE_KEYWORD,
        ELSEIF_KEYWORD,
        WHILE_KEYWORD,
        DO_KEYWORD,
        FOR_KEYWORD,
        IN_KEYWORD,
        SWITCH_KEYWORD,
        CASE_KEYWORD,
        DEFAULT_KEYWORD,
        BREAK_KEYWORD,
        CONTINUE_KEYWORD,
        RETURN_KEYWORD,
        TRY_KEYWORD,
        CATCH_KEYWORD,
        FINALLY_KEYWORD,
        THROW_KEYWORD,
        RETHROW_KEYWORD,
        ABORT_KEYWORD,
        LOCK_KEYWORD,
        TRANSACTION_KEYWORD,
        THREAD_KEYWORD,
        PUBLIC_KEYWORD,
        PRIVATE_KEYWORD,
        PACKAGE_KEYWORD,
        REMOTE_KEYWORD,
        STATIC_KEYWORD,
        FINAL_KEYWORD,
        ABSTRACT_KEYWORD,
        REQUIRED_KEYWORD,
        TRUE_KEYWORD,
        FALSE_KEYWORD,
        NULL_KEYWORD,
        YES_KEYWORD,
        NO_KEYWORD
    );

    public static final TokenSet TYPES = TokenSet.create(
        TYPE_ANY,
        TYPE_ARRAY,
        TYPE_BINARY,
        TYPE_BOOLEAN,
        TYPE_COMPONENT,
        TYPE_DATE,
        TYPE_FUNCTION,
        TYPE_GUID,
        TYPE_NUMERIC,
        TYPE_QUERY,
        TYPE_STRING,
        TYPE_STRUCT,
        TYPE_UUID,
        TYPE_VOID,
        TYPE_XML,
        TYPE_VARIABLENAME
    );

    public static final TokenSet SCOPES = TokenSet.create(
        SCOPE_APPLICATION,
        SCOPE_ARGUMENTS,
        SCOPE_ATTRIBUTES,
        SCOPE_CALLER,
        SCOPE_CGI,
        SCOPE_CLIENT,
        SCOPE_COOKIE,
        SCOPE_FLASH,
        SCOPE_FORM,
        SCOPE_LOCAL,
        SCOPE_REQUEST,
        SCOPE_SERVER,
        SCOPE_SESSION,
        SCOPE_THIS,
        SCOPE_THISTAG,
        SCOPE_URL,
        SCOPE_VARIABLES,
        SCOPE_SELF,
        SCOPE_SUPER
    );

    public static final TokenSet OPERATORS = TokenSet.create(
        OP_AND,
        OP_OR,
        OP_NOT,
        OP_EQ,
        OP_NEQ,
        OP_LT,
        OP_LTE,
        OP_GT,
        OP_GTE,
        OP_IS,
        OP_EQUAL,
        OP_CONTAINS,
        OP_DOES_NOT_CONTAIN,
        OP_GREATER_THAN,
        OP_LESS_THAN,
        OP_GREATER_THAN_OR_EQUAL_TO,
        OP_LESS_THAN_OR_EQUAL_TO,
        OP_XOR,
        OP_EQV,
        OP_IMP,
        OP_MOD,
        OP_TO,
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        MODULO,
        POWER,
        PLUS_PLUS,
        MINUS_MINUS,
        PLUS_ASSIGN,
        MINUS_ASSIGN,
        MUL_ASSIGN,
        DIV_ASSIGN,
        MOD_ASSIGN,
        POW_ASSIGN,
        CONCAT_ASSIGN,
        EQ_EQ,
        NOT_EQ,
        LESS,
        LESS_EQ,
        GREATER,
        GREATER_EQ,
        EXACT_EQ,
        EXACT_NOT_EQ,
        SPACESHIP,
        AND_AND,
        OR_OR,
        EXCL,
        BIT_AND,
        BIT_OR,
        BIT_XOR,
        BIT_NOT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        SHIFT_RIGHT_UNSIGNED,
        QUESTION,
        COLON,
        ELVIS,
        SAFE_NAV,
        ARROW,
        ASSIGN
    );

    public static final TokenSet BRACKETS = TokenSet.create(LBRACKET, RBRACKET);
    public static final TokenSet BRACES = TokenSet.create(LBRACE, RBRACE);
    public static final TokenSet PARENTHESES = TokenSet.create(LPAREN, RPAREN);

    private CFMLTokenTypes()
    {
    }

}