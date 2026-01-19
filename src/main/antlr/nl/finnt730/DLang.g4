grammar DLang;

@header {
package nl.finnt730;
}

/*
 * Parser Rules
 */
compileUnit : statement* EOF;

statement
    : printStatement
    | variableStatement
    | expressionStatement
    | ifStatement
    ;

printStatement
    : PRINT LPAREN expression RPAREN SEMI
    ;

variableStatement
    : VAR ID ASSIGN expression SEMI
    ;

expressionStatement
    : expression SEMI
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
    : INT
    | ID
    | '(' expression ')'
    ;

ifStatement
    : IF LPAREN condition RPAREN LBRACE (thenBlock+=statement)* RBRACE (ELSE LBRACE (elseBlock+=statement)* RBRACE)?
    ;

condition
    : expression operator=(GRT|LST) expression
    ;

/*
 * Lexer Rules
 */

LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
ASSIGN : '=';
SEMI : ';' ;

GRT : '>' ;
LST : '<' ;

PRINT : 'print';
VAR : 'var' ;
IF : 'if' ;
ELSE : 'else' ;

ID : [a-zA-Z_][a-zA-Z0-9_]*;
INT : [0-9]+;
WS : [ \t\r\n]+ -> skip;
