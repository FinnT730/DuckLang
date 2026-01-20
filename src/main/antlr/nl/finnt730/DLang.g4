grammar DLang;

@header {
package nl.finnt730;
}

/*
 * Parser Rules
 */
compileUnit : (functionDeclaration | statement)* EOF;

functionDeclaration
    : FUNC ID LPAREN (parameters)? RPAREN block
    ;

parameters
    : ID (COMMA ID)*
    ;

block
    : LBRACE statement* RBRACE
    ;

statement
    : printStatement
    | variableStatement
    | assignmentStatement
    | expressionStatement
    | ifStatement
    | whileStatement
    | returnStatement
    ;

printStatement
    : PRINT LPAREN expression RPAREN SEMI
    ;

variableStatement
    : VAR ID ASSIGN expression SEMI
    ;

assignmentStatement
    : ID ASSIGN expression SEMI
    ;

expressionStatement
    : expression SEMI
    ;

ifStatement
    : IF LPAREN condition RPAREN block (ELSE block)?
    ;

whileStatement
    : WHILE LPAREN condition RPAREN block
    ;

returnStatement
    : RETURN expression? SEMI
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
    : INT                                #IntegerLiteral
    | STRING                             #StringLiteral
    | BOOL                               #BooleanLiteral
    | LPAREN expression RPAREN           #ParenthesizedExpression
    | LBRACK listContents? RBRACK        #ArrayLiteral
    | ID LPAREN listContents? RPAREN     #FunctionCall
    | ID LBRACK expression RBRACK        #ArrayAccess
    | ID                                 #Identifier
    ;

listContents
    : expression (COMMA expression)*
    ;

/*
 * Lexer Rules
 */

LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACK : '[' ;
RBRACK : ']' ;
COMMA : ',' ;

ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/';
ASSIGN : '=';
SEMI : ';' ;

GRT : '>' ;
LST : '<' ;
EQ : '==' ;
NEQ : '!=' ;

PRINT : 'print';
VAR : 'var' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FUNC : 'func' ;
RETURN : 'return' ;

BOOL : 'true' | 'false' ;
ID : [a-zA-Z_][a-zA-Z0-9_]*;
INT : [0-9]+;
STRING : '"' .*? '"' ;
WS : [ \t\r\n]+ -> skip;
