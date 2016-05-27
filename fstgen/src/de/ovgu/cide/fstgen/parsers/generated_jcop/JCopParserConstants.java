/* Generated By:JavaCC: Do not edit this line. JCopParserConstants.java */
package de.ovgu.cide.fstgen.parsers.generated_jcop;

public interface JCopParserConstants {

  int EOF = 0;
  int SINGLE_LINE_COMMENT = 9;
  int FORMAL_COMMENT = 10;
  int MULTI_LINE_COMMENT = 11;
  int ABSTRACT = 13;
  int AFTER = 14;
  int ASSERT = 15;
  int BEFORE = 16;
  int BOOLEAN = 17;
  int BREAK = 18;
  int BYTE = 19;
  int CALL = 20;
  int CASE = 21;
  int CATCH = 22;
  int CHAR = 23;
  int CLASS = 24;
  int CONST = 25;
  int CONTEXT = 26;
  int CONTINUE = 27;
  int _DEFAULT = 28;
  int DO = 29;
  int DOUBLE = 30;
  int ELSE = 31;
  int ENUM = 32;
  int EXTENDS = 33;
  int FALSE = 34;
  int FINAL = 35;
  int FINALLY = 36;
  int FLOAT = 37;
  int FOR = 38;
  int GOTO = 39;
  int IF = 40;
  int IMPLEMENTS = 41;
  int IMPORT = 42;
  int IN = 43;
  int INSTANCEOF = 44;
  int INT = 45;
  int INTERFACE = 46;
  int LAYER = 47;
  int LONG = 48;
  int NATIVE = 49;
  int NEW = 50;
  int NULL = 51;
  int ON = 52;
  int PACKAGE = 53;
  int PRIVATE = 54;
  int PROCEED = 55;
  int PROTECTED = 56;
  int PUBLIC = 57;
  int RETURN = 58;
  int SHORT = 59;
  int STATIC = 60;
  int STATICACTIVE = 61;
  int STRICTFP = 62;
  int SUBJECT = 63;
  int SUPER = 64;
  int SWITCH = 65;
  int SYNCHRONIZED = 66;
  int THIS = 67;
  int THROW = 68;
  int THROWS = 69;
  int TRANSIENT = 70;
  int TRUE = 71;
  int TRY = 72;
  int VOID = 73;
  int VOLATILE = 74;
  int WHEN = 75;
  int WHILE = 76;
  int WITH = 77;
  int WITHOUT = 78;
  int INTEGER_LITERAL = 79;
  int DECIMAL_LITERAL = 80;
  int HEX_LITERAL = 81;
  int OCTAL_LITERAL = 82;
  int FLOATING_POINT_LITERAL = 83;
  int EXPONENT = 84;
  int CHARACTER_LITERAL = 85;
  int STRING_LITERAL = 86;
  int LPAREN = 87;
  int RPAREN = 88;
  int LBRACE = 89;
  int RBRACE = 90;
  int LBRACKET = 91;
  int RBRACKET = 92;
  int SEMICOLON = 93;
  int COMMA = 94;
  int DOT = 95;
  int AT = 96;
  int IDENTIFIER = 97;
  int LETTER = 98;
  int DIGIT = 99;
  int ASSIGN = 100;
  int LT = 101;
  int BANG = 102;
  int TILDE = 103;
  int HOOK = 104;
  int COLON = 105;
  int EQ = 106;
  int LE = 107;
  int GE = 108;
  int NE = 109;
  int SC_OR = 110;
  int SC_AND = 111;
  int INCR = 112;
  int DECR = 113;
  int PLUS = 114;
  int MINUS = 115;
  int STAR = 116;
  int SLASH = 117;
  int BIT_AND = 118;
  int BIT_OR = 119;
  int XOR = 120;
  int REM = 121;
  int LSHIFT = 122;
  int PLUSASSIGN = 123;
  int MINUSASSIGN = 124;
  int STARASSIGN = 125;
  int SLASHASSIGN = 126;
  int ANDASSIGN = 127;
  int ORASSIGN = 128;
  int XORASSIGN = 129;
  int REMASSIGN = 130;
  int LSHIFTASSIGN = 131;
  int RSIGNEDSHIFTASSIGN = 132;
  int RUNSIGNEDSHIFTASSIGN = 133;
  int ELLIPSIS = 134;
  int GT = 135;

  int DEFAULT = 0;
  int IN_SINGLE_LINE_COMMENT = 1;
  int IN_FORMAL_COMMENT = 2;
  int IN_MULTI_LINE_COMMENT = 3;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "\"//\"",
    "<token of kind 7>",
    "\"/*\"",
    "<SINGLE_LINE_COMMENT>",
    "\"*/\"",
    "\"*/\"",
    "<token of kind 12>",
    "\"abstract\"",
    "\"after\"",
    "\"assert\"",
    "\"before\"",
    "\"boolean\"",
    "\"break\"",
    "\"byte\"",
    "\"call\"",
    "\"case\"",
    "\"catch\"",
    "\"char\"",
    "\"class\"",
    "\"const\"",
    "\"contextclass\"",
    "\"continue\"",
    "\"default\"",
    "\"do\"",
    "\"double\"",
    "\"else\"",
    "\"enum\"",
    "\"extends\"",
    "\"false\"",
    "\"final\"",
    "\"finally\"",
    "\"float\"",
    "\"for\"",
    "\"goto\"",
    "\"if\"",
    "\"implements\"",
    "\"import\"",
    "\"in\"",
    "\"instanceof\"",
    "\"int\"",
    "\"interface\"",
    "\"layer\"",
    "\"long\"",
    "\"native\"",
    "\"new\"",
    "\"null\"",
    "\"on\"",
    "\"package\"",
    "\"private\"",
    "\"proceed\"",
    "\"protected\"",
    "\"public\"",
    "\"return\"",
    "\"short\"",
    "\"static\"",
    "\"staticactive\"",
    "\"strictfp\"",
    "\"subject\"",
    "\"super\"",
    "\"switch\"",
    "\"synchronized\"",
    "\"this\"",
    "\"throw\"",
    "\"throws\"",
    "\"transient\"",
    "\"true\"",
    "\"try\"",
    "\"void\"",
    "\"volatile\"",
    "\"when\"",
    "\"while\"",
    "\"with\"",
    "\"without\"",
    "<INTEGER_LITERAL>",
    "<DECIMAL_LITERAL>",
    "<HEX_LITERAL>",
    "<OCTAL_LITERAL>",
    "<FLOATING_POINT_LITERAL>",
    "<EXPONENT>",
    "<CHARACTER_LITERAL>",
    "<STRING_LITERAL>",
    "\"(\"",
    "\")\"",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "\";\"",
    "\",\"",
    "\".\"",
    "\"@\"",
    "<IDENTIFIER>",
    "<LETTER>",
    "<DIGIT>",
    "\"=\"",
    "\"<\"",
    "\"!\"",
    "\"~\"",
    "\"?\"",
    "\":\"",
    "\"==\"",
    "\"<=\"",
    "\">=\"",
    "\"!=\"",
    "\"||\"",
    "\"&&\"",
    "\"++\"",
    "\"--\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "\"&\"",
    "\"|\"",
    "\"^\"",
    "\"%\"",
    "\"<<\"",
    "\"+=\"",
    "\"-=\"",
    "\"*=\"",
    "\"/=\"",
    "\"&=\"",
    "\"|=\"",
    "\"^=\"",
    "\"%=\"",
    "\"<<=\"",
    "\">>=\"",
    "\">>>=\"",
    "\"...\"",
    "\">\"",
    "\"context\"",
  };

}
