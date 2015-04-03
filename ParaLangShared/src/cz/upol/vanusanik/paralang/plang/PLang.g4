grammar PLang;

compilationUnit
    :   importDeclaration* moduleDeclaration EOF
    ;

importDeclaration
	:   'using' singleQualifiedName ';'
    |   'import' qualifiedName ';'
    ; 
    
moduleDeclaration  
	: 'module' Identifier '{' moduleDeclarations* '};' 
	; 
	
moduleDeclarations 
	: functionDeclaration
	| fieldDeclaration
	| classDeclaration
	;

classDeclaration
    :   'class' Identifier 
        ('>' type)?
        classBody
    ;

classBody
    :   '{' classBodyDeclaration* '}'
    ;

classBodyDeclaration
    :   ';'
    |   memberDeclaration
    ;


memberDeclaration
    :   functionDeclaration
    |   fieldDeclaration
    ;

functionDeclaration
    :   'restricted'? Identifier formalParameters
        (   functionBody
        |   ';'
        )
    ;

fieldDeclaration
    :   'var' variableDeclarators ';'
    ;

variableDeclarators
    :   variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    :   variableDeclaratorId ('=' variableInitializer)?
    ;

variableDeclaratorId
    :   Identifier
    ;

variableInitializer
    :   expression
    ;

type
    :   Identifier ('.' Identifier)?
    ;

qualifiedNameList
    :   qualifiedName (',' qualifiedName)*
    ;

formalParameters
    :   '(' formalParameterList? ')'
    ;

formalParameterList
    :   formalParameter (',' formalParameter)* (',' lastFormalParameter)?
    |   lastFormalParameter
    ;

formalParameter
    :   variableDeclaratorId
    ;

lastFormalParameter
    :   variableDeclaratorId '...'
    ;

functionBody
    :   block
    ;

constructorBody
    :   block
    ;

qualifiedName
    :   Identifier ('.' Identifier)*
    ;
    
singleQualifiedName
	: (Identifier '.')? Identifier
	;

literal
    :   IntegerLiteral
    |   FloatingPointLiteral
    |   CharacterLiteral
    |   StringLiteral
    |   BooleanLiteral
    |   'NoValue'
    ;

block
    :   '{' blockStatement* '}'
    ;

blockStatement
    :   localVariableDeclarationStatement
    |   statement
    ;

localVariableDeclarationStatement
    :   'var' localVariableDeclaration ';'
    ;

localVariableDeclaration
    :   variableDeclarators
    ;

statement
    :   block
    |   'if' parExpression statement ('else' statement)?
    |   'throw' expression ';'
    |   'try' block (catchClause+ finallyBlock? | finallyBlock)
    |   'for' '(' forControl ')' statement
    |   'while' parExpression statement
    |   'do' statement 'while' parExpression ';'
	|   'parallel' block
    |   'return' expression? ';'
    |   'break' ';'
    |   'continue' ';'
    |   ';'
    |   statementExpression ';'
    ;
    
catchClause
    :   'catch' '(' type Identifier ')' block
    ;
    
finallyBlock
	:	'finally' block
	;

forControl
    :   enhancedForControl
    |   forInit? ';' expression? ';' forUpdate?
    ;

forInit
    :   localVariableDeclaration
    |   expressionList
    ;

enhancedForControl
    :   variableDeclaratorId ':' expression
    ;

forUpdate
    :   expressionList
    ;

parExpression
    :   '(' expression ')'
    ;

expressionList
    :   expression (',' expression)*
    ;

statementExpression
    :   expression
    ;

constantExpression
    :   expression
    ;

expression
    :   primary
    |   expression '.' Identifier
    |   expression '->' Identifier '(' expressionList? ')'
    |   Identifier '->' Identifier '(' expressionList? ')'
    |   expression methodCall
    |   'new' constructorCall
    |   'new' Identifier '.' constructorCall
    |   expression ('++' | '--')
    |   ('+'|'-'|'++'|'--') expression
    |   ('~'|'!') expression
    |   expression ('*'|'/'|'%') expression
    |   expression ('+'|'-') expression
    |   expression ('<<' | '>>>' | '>>') expression
    |   expression ('<=' | '>=' | '>' | '<') expression
    |   expression 'instanceof' type
    |   expression ('==' | '!=') expression
    |   expression '&' expression
    |   expression '^' expression
    |   expression '|' expression
    |   expression '&&' expression
    |   expression '||' expression
    |   expression '?' expression ':' expression
    |   <assoc=right> extended
        (   '='
        |   '+='
        |   '-='
        |   '*='
        |   '/='
        |   '&='
        |   '|='
        |   '^='
        |   '>>='
        |   '>>>='
        |   '<<='
        |   '%='
        )
        expression
    ;
    
methodCall
	: '(' expressionList? ')'
	;
    
constructorCall 
	:	Identifier '(' expressionList? ')'
	;
    
extended 
	: identified 
	| constExpr
	;
	
identified
	: primary '.' Identifier
	;

primary
    :   '(' expression ')'
    |   constExpr
    |   literal
    ;
    
constExpr
	:   'inst'
    |   'parent'
    |   id
    ;
    
id
	: Identifier
	;

superSuffix
    :   arguments
    |   '.' Identifier arguments?
    ;

explicitGenericInvocationSuffix
    :   'super' superSuffix
    |   Identifier arguments
    ;

arguments
    :   '(' expressionList? ')'
    ;

BREAK         : 'break';
CLASS         : 'class';
PARENTCLASS   : 'parent class';
CONTINUE      : 'continue';
DO            : 'do';
ELSE          : 'else';
VAR       	  : 'var';
RESTRICTED    : 'restricted';
PARALLEL      : 'parallel';
FOR           : 'for';
IF            : 'if';
IMPORT        : 'import';
JAVAIMPORT    : 'java import';
INSTANCEOF    : 'instanceof';
NEW           : 'new';
RETURN        : 'return';
SUPER         : 'super';
INST          : 'inst';
VOLATILE      : 'volatile';
WHILE         : 'while';

IntegerLiteral
    :   DecimalIntegerLiteral
    |   HexIntegerLiteral
    |   OctalIntegerLiteral
    |   BinaryIntegerLiteral
    ;

fragment
DecimalIntegerLiteral
    :   DecimalNumeral IntegerTypeSuffix?
    ;

fragment
HexIntegerLiteral
    :   HexNumeral IntegerTypeSuffix?
    ;

fragment
OctalIntegerLiteral
    :   OctalNumeral IntegerTypeSuffix?
    ;

fragment
BinaryIntegerLiteral
    :   BinaryNumeral IntegerTypeSuffix?
    ;

fragment
IntegerTypeSuffix
    :   [lL]
    ;

fragment
DecimalNumeral
    :   '0'
    |   NonZeroDigit (Digits? | Underscores Digits)
    ;

fragment
Digits
    :   Digit (DigitOrUnderscore* Digit)?
    ;

fragment
Digit
    :   '0'
    |   NonZeroDigit
    ;

fragment
NonZeroDigit
    :   [1-9]
    ;

fragment
DigitOrUnderscore
    :   Digit
    |   '_'
    ;

fragment
Underscores
    :   '_'+
    ;

fragment
HexNumeral
    :   '0' [xX] HexDigits
    ;

fragment
HexDigits
    :   HexDigit (HexDigitOrUnderscore* HexDigit)?
    ;

fragment
HexDigit
    :   [0-9a-fA-F]
    ;

fragment
HexDigitOrUnderscore
    :   HexDigit
    |   '_'
    ;

fragment
OctalNumeral
    :   '0' Underscores? OctalDigits
    ;

fragment
OctalDigits
    :   OctalDigit (OctalDigitOrUnderscore* OctalDigit)?
    ;

fragment
OctalDigit
    :   [0-7]
    ;

fragment
OctalDigitOrUnderscore
    :   OctalDigit
    |   '_'
    ;

fragment
BinaryNumeral
    :   '0' [bB] BinaryDigits
    ;

fragment
BinaryDigits
    :   BinaryDigit (BinaryDigitOrUnderscore* BinaryDigit)?
    ;

fragment
BinaryDigit
    :   [01]
    ;

fragment
BinaryDigitOrUnderscore
    :   BinaryDigit
    |   '_'
    ;

FloatingPointLiteral
    :   DecimalFloatingPointLiteral
    |   HexadecimalFloatingPointLiteral
    ;

fragment
DecimalFloatingPointLiteral
    :   Digits '.' Digits? ExponentPart? FloatTypeSuffix?
    |   '.' Digits ExponentPart? FloatTypeSuffix?
    |   Digits ExponentPart FloatTypeSuffix?
    |   Digits FloatTypeSuffix
    ;

fragment
ExponentPart
    :   ExponentIndicator SignedInteger
    ;

fragment
ExponentIndicator
    :   [eE]
    ;

fragment
SignedInteger
    :   Sign? Digits
    ;

fragment
Sign
    :   [+-]
    ;

fragment
FloatTypeSuffix
    :   [fFdD]
    ;

fragment
HexadecimalFloatingPointLiteral
    :   HexSignificand BinaryExponent FloatTypeSuffix?
    ;

fragment
HexSignificand
    :   HexNumeral '.'?
    |   '0' [xX] HexDigits? '.' HexDigits
    ;

fragment
BinaryExponent
    :   BinaryExponentIndicator SignedInteger
    ;

fragment
BinaryExponentIndicator
    :   [pP]
    ;

BooleanLiteral
    :   'true'
    |   'false'
    ;

CharacterLiteral
    :   '\'' SingleCharacter '\''
    |   '\'' EscapeSequence '\''
    ;

fragment
SingleCharacter
    :   ~['\\]
    ;

StringLiteral
    :   '"' StringCharacters? '"'
    ;

fragment
StringCharacters
    :   StringCharacter+
    ;

fragment
StringCharacter
    :   ~["\\]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
    |   OctalEscape
    |   UnicodeEscape
    ;

fragment
OctalEscape
    :   '\\' OctalDigit
    |   '\\' OctalDigit OctalDigit
    |   '\\' ZeroToThree OctalDigit OctalDigit
    ;

fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
ZeroToThree
    :   [0-3]
    ;

NullLiteral
    :   'NoValue'
    ;

LPAREN          : '(';
RPAREN          : ')';
LBRACE          : '{';
RBRACE          : '}';
LBRACK          : '[';
RBRACK          : ']';
SEMI            : ';';
COMMA           : ',';
DOT             : '.';


ASSIGN          : '=';
GT              : '>';
LT              : '<';
BANG            : '!';
TILDE           : '~';
QUESTION        : '?';
COLON           : ':';
EQUAL           : '==';
LE              : '<=';
GE              : '>=';
NOTEQUAL        : '!=';
AND             : '&&';
OR              : '||';
INC             : '++';
DEC             : '--';
ADD             : '+';
SUB             : '-';
MUL             : '*';
DIV             : '/';
BITAND          : '&';
BITOR           : '|';
CARET           : '^';
MOD             : '%';

ADD_ASSIGN      : '+=';
SUB_ASSIGN      : '-=';
MUL_ASSIGN      : '*=';
DIV_ASSIGN      : '/=';
AND_ASSIGN      : '&=';
OR_ASSIGN       : '|=';
XOR_ASSIGN      : '^=';
MOD_ASSIGN      : '%=';
LSHIFT_ASSIGN   : '<<=';
RSHIFT_ASSIGN   : '>>=';
URSHIFT_ASSIGN  : '>>>=';


Identifier
    :   JavaLetter JavaLetterOrDigit*
    ;

fragment
JavaLetter
    :   [a-zA-Z$_] // these are the "java letters" below 0xFF
    |   // covers all characters above 0xFF which are not a surrogate
        ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierStart(_input.LA(-1))}?
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;

fragment
JavaLetterOrDigit
    :   [a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
    |   // covers all characters above 0xFF which are not a surrogate
        ~[\u0000-\u00FF\uD800-\uDBFF]
        {Character.isJavaIdentifierPart(_input.LA(-1))}?
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;


AT : '@';
ELLIPSIS : '...';


WS  :  [ \t\r\n\u000C]+ -> skip
    ;

COMMENT
    :   '/*' .*? '*/' -> skip
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> skip
    ;