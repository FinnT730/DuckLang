grammar DLang;

@header {
package nl.finnt730;
}

/*
 * Parser Rules
 */
compileUnit : classDeclaration* EOF;

classDeclaration
    : ID DCOLON CLASS LPAREN RPAREN LBRACE memberDeclaration* RBRACE
    ;

memberDeclaration
    : (STATIC)? ID DCOLON FUNC LPAREN parameters? RPAREN block
    ;

parameters
    : parameter (COMMA parameter)*
    ;

parameter
    : ID COLON type
    ;

type
    : ID
    | LBRACK RBRACK ID
    ;

block
    : LBRACE statement* RBRACE
    ;

statement
    : variableDeclaration
    | assignmentStatement
    | expressionStatement
    | ifStatement
    | whileStatement
    | foreachStatement
    | returnStatement
    ;

variableDeclaration
    : (type | VAR) ID ASSIGN expression
    ;

assignmentStatement
    : ID ASSIGN expression
    ;

expressionStatement
    : expression
    ;

ifStatement
    : IF LPAREN condition RPAREN block (ELSE block)?
    ;

whileStatement
    : WHILE LPAREN condition RPAREN block
    ;

foreachStatement
    : FOREACH LPAREN type ID COLON expression RPAREN block
    ;

returnStatement
    : RETURN expression?
    ;

condition
    : expression (operator=(GRT|LST|EQ|NEQ) expression)?
    ;

expression
    : additive
    ;

additive
    : multiplicative ((ADD|SUB) multiplicative)*
    ;

multiplicative
    : primary ((MUL|DIV) primary)*
    ;

primary
    : (
        INT
        | STRING
        | CHAR
        | BOOL
        | LPAREN expression RPAREN
        | LBRACK listContents? RBRACK
        | ID LPAREN listContents? RPAREN
        | ID DCOLON ID LPAREN listContents? RPAREN
        | STATIC DCOLON ID LPAREN listContents? RPAREN
        | ID LBRACK expression RBRACK
        | ID
      )
      (DOT ID LPAREN listContents? RPAREN)*
    ;

listContents
    : expression (COMMA expression)*
    ;

/*
 * Lexer Rules
 */

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACK : '[' ;
RBRACK : ']' ;
COMMA : ',' ;
COLON : ':' ;
DCOLON : '::' ;
DOT : '.' ;

ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
ASSIGN : '=';

GRT : '>' ;
LST : '<' ;
EQ : '==' ;
NEQ : '!=' ;

PRINT : 'print';
VAR : 'var' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FOREACH : 'foreach' ;
FUNC : 'func' ;
RETURN : 'return' ;
CLASS : 'class' ;
STATIC : 'static' ;

BOOL : 'true' | 'false' ;
ID : [a-zA-Z_][a-zA-Z0-9_]*;
INT : [0-9]+;
STRING : '"' .*? '"' ;
CHAR : '\'' . '\'' ;
WS : [ \t\r\n]+ -> skip;
