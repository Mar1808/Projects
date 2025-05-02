%{
#include "tree.h"
/*Projet -> Analyseur lexical*/
#include "parser.tab.h"
int lineno = 1;
int columnno = 1;
int standard = 1;
%}

%option nounput
%option noinput

%x COM1
%x COM2 

%%
\/\*                    { standard = 0;BEGIN COM1;               }
<COM1>\n                { lineno++;                 }
<COM1>.                 {                           }
<COM1>\*\/              { standard = 1;BEGIN INITIAL;            }
\/\/                    { standard = 0;BEGIN COM2;               }
<COM2>.                 {                           }
<COM2>\n                { standard = 1;lineno ++;  BEGIN INITIAL;}
            
int|char                { strcpy( yylval.type, yytext); columnno += strlen(yytext);return TYPE;              }
return                  { columnno += strlen(yytext);return RETURN;            }
void                    { columnno += strlen(yytext); return VOID;              }
if                      { columnno += strlen(yytext); return IF;                }
else                    { columnno += strlen(yytext); return ELSE;              }
while                   { columnno += strlen(yytext); return WHILE;             }
\'.\'                   { columnno += strlen(yytext); strcpy( yylval.element, yytext);return CHARACTER;         }
\'\\'\'                 { columnno += strlen(yytext); strcpy( yylval.element, yytext);return CHARACTER;         }
\'\\0\'                 { columnno += strlen(yytext); strcpy( yylval.element, yytext);return CHARACTER;         }
\'\\t\'                 { columnno += strlen(yytext); strcpy( yylval.element, yytext);return CHARACTER;         }
\'\\n\'                 { columnno += strlen(yytext); strcpy( yylval.element, yytext);return CHARACTER;         }
                    
=                       { columnno ++; return yytext[0];         }
[-+]                    { columnno ++; yylval.operation=yytext[0]; return ADDSUB;            }
!                       { columnno ++;return yytext[0];         }
[*|\/|\%]               { columnno ++; yylval.operation = yytext[0]; return DIVSTAR;           }
[0-9]+                  { columnno += strlen(yytext); strcpy( yylval.nombre, yytext); return NUM;               }
[a-zA-Z_][a-zA-Z0-9_]*  { columnno += strlen(yytext); strcpy( yylval.ident, yytext); return IDENT;             }         
        
\[                      { columnno ++; return yytext[0];         }
\]                      { columnno ++; return yytext[0];         }
                
"=="              { columnno += strlen(yytext); return EQ;                }
"!="              { columnno += strlen(yytext); return NEQ;                }
"<="                    { columnno += strlen(yytext);yylval.order = strdup(yytext);return ORDER;             }
">="                    { columnno += strlen(yytext);yylval.order = strdup(yytext);return ORDER;             }
"<"                     { columnno += strlen(yytext);yylval.order = strdup(yytext); return ORDER;             }
">"                     { columnno += strlen(yytext);yylval.order = strdup(yytext); return ORDER;             }
\|\|                    { columnno += strlen(yytext);return OR;                }
&&                      { columnno += strlen(yytext);return AND;               }
[;|,]                   { columnno += strlen(yytext);return yytext[0];         }
\(                      { columnno += strlen(yytext);return yytext[0];         }
\)                      { columnno += strlen(yytext);return yytext[0];         }
\{                      { columnno += strlen(yytext);return yytext[0];         }
\}                      { columnno += strlen(yytext);return yytext[0];         }
[ ]|\t                  { columnno += strlen(yytext);                          }
\n                      { columnno = 0; lineno++;                 }   

. ;                     {columnno += strlen(yytext); return yytext[0];          }    

%%                  