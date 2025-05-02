%{
    #include <stdio.h>
    #include <stdlib.h>
    #include <string.h>
    #include <unistd.h>
    #include "traduction.h"
    #include "parser.tab.h"
    void yyerror(const char *s);
    int yylex();
    extern int lineno;
    extern int error;
    extern int columnno;
    int tree = 0;
    int tableSymboles = 0;
    char* fileNameAsm = "_anonymous.asm";
    extern FILE *yyin;
%}

%union {
  Node *node;
  char ident[64];
  char nombre[64];
  char type[5];
  char *order;
  char operation;
  char element[2];
}

%type <node> Prog 
%type <node> DeclVars 
%type <node> Declarateurs 
%type <node> TAB 
%type <node> DeclFonct 
%type <node> EnTeteFonct 
%type <node> DeclFoncts 
%type <node> Parametres 
%type <node> ParamTAB 
%type <node> ListTypVar 
%type <node> Corps 
%type <node> SuiteInstr 
%type <node> Instr 
%type <node> Exp 
%type <node> TB 
%type <node> FB 
%type <node> M 
%type <node> E 
%type <node> T 
%type <node> F 
%type <node> LValue 
%type <node> Arguments 
%type <node> ListExp
%type <node> TABAffect

%token <element> CHARACTER 
%token <nombre> NUM
%token <ident> IDENT
%token <type> TYPE
%token <order> ORDER
%token EQ
%token NEQ
%token <operation> DIVSTAR
%token OR AND
%token <operation> ADDSUB
%token RETURN
%token VOID
%token IF
%token ELSE
%token WHILE



%%
Prog:  DeclVars DeclFoncts {
        $$ = makeNode(prog); 
        addChild($$, $1); 
        addChild($$, $2);
        Tables *t = alloueTable();
        t = parcours($$, t);
        /*****Pas ou plusieurs fonction main*****/
        TableSymbole *tmp = t->fonction;
        int presenceMain = 0;
        while(tmp) {
            if(strcmp(tmp->name, "main") == 0) {
                presenceMain++;
            }
            tmp=tmp->suiv;
        }
        if(presenceMain != 1) {
            fprintf(stderr, "\033[01;31mError : Il n'y a pas de fonction main\033[01;0m\n");
            error = 2;
            return 2;
        }
        
        /**********/
        
        FILE* fichier = NULL;
        fichier = fopen(fileNameAsm, "w");
        if(fichier == NULL)
            return 1;
        
        if(tableSymboles) {
            printf("Table des Symboles des fonctions\n");
            afficheTable(t->fonction);
            printf("Table des Symboles des variables\n");
            afficheSymbole(t->globale);
        }
        
        if(tree){
            printTree($$);
        }
        traductionASM($$, fichier, t);
        fclose(fichier);
        exit(error);
    }
    ;
DeclVars:
       DeclVars TYPE Declarateurs ';' {
            $$ = $1;
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($2) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $2);
            addChild($$, nType);
            addChild($$, $3);
        }
    | { $$ = makeNode(declvars);  }
    ;
Declarateurs:
       Declarateurs ',' IDENT {
            $$ = $1; 
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($3) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $3);
            addChild($$, nID); 
        }
    |  IDENT {
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($1) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $1);
            $$ = nID; 
        }
    |  Declarateurs ',' TAB {
            $$ = $1 ;
            addSibling($$, $3); 
        }
    |  TAB {
            $$ = $1;
        }
    ;
TAB : IDENT '[' NUM ']' {
            $$ = makeNode(tab); 
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($1) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $1);
            addChild($$, nID);
            Node *nNum = makeNode(num);
            nNum->attribute = (char*)malloc(strlen($3) + 1);
            if(nNum->attribute == NULL) {
                return 2;
            }
            strcpy(nNum->attribute, $3);
            addChild($$, nNum);
        }
    ;
DeclFoncts:
       DeclFoncts DeclFonct  {
            $$ = $1;
            addChild($$, $2); 
        }
    |  DeclFonct {
           $$ = makeNode(declfoncts); 
            addChild($$, $1);  
        }
    ;
DeclFonct:
       EnTeteFonct Corps {
            $$ = makeNode(declfonct); 
            addChild($$, $1); 
            addChild($$, $2); 
        }
    ;
EnTeteFonct:
       TYPE IDENT '(' Parametres ')' {
            $$ = makeNode(entetefonct); 
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($1) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $1);
            addChild($$, nType);
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($2) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $2);
            addChild($$, nID);
            addChild($$, $4); 
        }
    |  VOID IDENT '(' Parametres ')' {
            $$ = makeNode(entetefonct); 
            addChild($$, makeNode(_void_)); 
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($2) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $2);
            addChild($$, nID);
            addChild($$, $4); 
        }
    ;
Parametres:
       VOID {
            $$ = makeNode(_void_); 
        }
    |  ListTypVar {
            $$ = $1; 
        }
    ;
ListTypVar:
       ListTypVar ',' TYPE IDENT {
            $$ = $1; 
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($3) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $3);
            addChild($$, nType);
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($4) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $4);
            addChild($$, nID);
        }
    |  TYPE IDENT {
            $$ = makeNode(listtypvar); 
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($1) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $1);
            addChild($$, nType); 
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($2) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $2);
            addChild($$, nID);
        }
    |  ListTypVar ',' TYPE ParamTAB {
            $$ = $1; 
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($3) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $3);
            addChild($$, nType);
            addChild($$, $4); 
        }
    |  TYPE ParamTAB {
            $$ = makeNode(listtypvar); 
            Node *nType = makeNode(type);
            nType->attribute = (char*)malloc(strlen($1) + 1);
            if(nType->attribute == NULL) {
                return 2;
            }
            strcpy(nType->attribute, $1);
            addChild($$, nType);
            addChild($$, $2); 
        }
    ;
ParamTAB: IDENT '[' ']' {
            $$ = makeNode(paramtab); 
            Node *nID = makeNode(ident);
            nID->attribute = (char*)malloc(strlen($1) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $1);
            addChild($$, nID);
        }
    ;
Corps: '{' DeclVars SuiteInstr '}' {
            $$ = makeNode(corps); 
            addChild($$, $2); 
            addChild($$, $3); 
        }
    ;
SuiteInstr:
        SuiteInstr Instr {
            $$ = $1; 
            addChild($$, $2); 
        }
    | {$$ = makeNode(suiteinstr); }
    ;
Instr: 
       LValue '=' Exp ';' {
            $$ = makeNode(affectation); 
            addChild($$, $1); 
            addChild($$, $3); 
        }
    |  LValue '[' Exp ']' '=' Exp ';' {
            $$ = makeNode(AffectationTab);
            addChild($$, $1); 
            addChild($$, $3); 
            addChild($$, $6); 
        }
    |  IF '(' Exp ')' Instr {
            $$ = makeNode(_if_); 
            addChild($$, $3); 
            addChild($$, $5); 
        }
    |  IF '(' Exp ')' Instr ELSE Instr {
            $$ = makeNode(_if_); 
            addChild($$, $3); 
            addChild($$, $5);
            addChild($$, $7); 
        }
    |  WHILE '(' Exp ')' Instr {
            $$ = makeNode(_while_); 
            addChild($$, $3); 
            addChild($$, $5);
        }
    |  IDENT '(' Arguments  ')' ';' {
            Node *nID = makeNode(fonction);
            nID->attribute = (char*)malloc(strlen($1) + 1);
            if(nID->attribute == NULL) {
                return 2;
            }
            strcpy(nID->attribute, $1);
            $$ = nID; 
            addChild($$, $3);
        }
    |  RETURN Exp ';' {
            $$ = makeNode(_return_); 
            addChild($$, $2);
        }
    |  RETURN ';' {
            $$ = makeNode(_return_); 
        }
    |  '{' SuiteInstr '}' {
            $$ = $2; 
        }
    |  ';' { }
    ;
Exp :  Exp OR TB {
            $$ = makeNode(or);
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  TB {
            $$ = $1; 
        }
    ;
TB  :  TB AND FB {
            $$ = makeNode(and); 
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  FB {
            $$ = $1; 
        }
    ;
FB  :  FB EQ M {
            $$ = makeNode(eq); 
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  FB NEQ M {
            $$ = makeNode(neq); 
            addChild($$, $1); 
            addChild($$, $3);
        }
    | M {
            $$ = $1; 
        }
    ;
M   :  M ORDER E {
            $$ = makeNode(atoi($2)); 
            if(strcmp($2, "<") == 0) {
                $$ = makeNode(inferieur); 
            }
            if(strcmp($2, ">") == 0) {
                $$ = makeNode(superieur); 
            }
            if(strcmp($2, "<=") == 0) {
                $$ = makeNode(inferieurEgal); 
            }
            if(strcmp($2, ">=") == 0) {
                $$ = makeNode(superieurEgal); 
            }
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  E {
            $$ = $1; 
        }
    ;
E   :  E ADDSUB T {
            if($2 == '+') {
                $$ = makeNode(addition); 
            }
            if($2 == '-') {
                $$ = makeNode(soustraction); 
            }
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  T {
            $$ = $1; 
        }
    ;    
T   :  T DIVSTAR F {
            if($2 == '*') {
                $$ = makeNode(multiplication); 
            }
            if($2 == '/') {
                $$ = makeNode(division); 
            }
            if($2 == '%') {
                $$ = makeNode(modulo); 
            }
            addChild($$, $1); 
            addChild($$, $3);
        }
    |  F {
            $$ = $1; 
        }
    ;
F   :  ADDSUB F {
            Node* n = makeNode(addsub);
            n->attribute = (char*)malloc(1);
            if(n->attribute == NULL) {
                return 2;
            }
            *(n->attribute) = $1;
            $$ = n; 
            addChild($$, $2); 
        }
    |  '!' F  {
            $$ = makeNode(different); 
            addChild($$, $2); 
        }
    |  '(' Exp ')' {
            $$ = $2; 
        }
    |  NUM {
            Node* n = makeNode(num);
            n->attribute = (char*)malloc(strlen($1) + 1);
            if(n->attribute == NULL) {
                return 2;
            }
            strcpy(n->attribute, $1);
            $$ = n; 
        }
    |  CHARACTER {
            Node* n = makeNode(character);
            n->attribute = (char*)malloc(strlen($1) + 1);
            if(n->attribute == NULL) {
                return 2;
            }
            strcpy(n->attribute, $1);
            $$ = n;
        }
    |  LValue {
            $$ = $1; 
        }
    |  IDENT '(' Arguments  ')'{
            Node* id = makeNode(fonction);
            id->attribute = (char*)malloc(strlen($1) + 1);
            if(id->attribute == NULL) {
                return 2;
            }
            strcpy(id->attribute, $1);
            $$ = id;
            addChild($$, $3); 
        }
    |  TABAffect {
            $$ = $1; 
        }
    ;
TABAffect :
    IDENT '[' Exp ']' {
            $$ = makeNode(tabAffect); 
            Node* n = makeNode(ident);
            n->attribute = (char*)malloc(strlen($1) + 1);
            if(n->attribute == NULL) {
                return 2;
            }
            strcpy(n->attribute, $1);
            addChild($$, n); 
            addChild($$, $3);
        }
    ;
LValue:
       IDENT {
            Node* n = makeNode(ident);
            n->attribute = (char*)malloc(strlen($1) + 1);
            if(n->attribute == NULL) {
                return 2;
            }
            strcpy(n->attribute, $1);
            $$ = n;
        }
    ;
Arguments:
        ListExp {
            $$ = $1; 
        }
    |  { $$ = makeNode(_void_); }
    ;
ListExp:
        ListExp ',' Exp {
            $$ = $1; 
            addSibling($$, $3); 
        }
    |   Exp {
            $$ = $1; 
        }
    ;
%%

void yyerror (char const *s) {
    fprintf (stderr, "%s  : at the position %d, %d\n", s, lineno, columnno);
}

void afficheDescription() {
    printf("Description de l'analyseur:\n");
    printf("Compilation: make\n");
    printf("Execution du programme: bin/./tpcas [OPTIONS] < FILE.tpc, remplacez FILE par le fichier à analyser\n");
    printf("Options: \n");
    printf("    -t, --tree: Affiche l’arbre abstrait sur la sortie standard\n");
    printf("    -h, --help: Affiche une description de l’interface utilisateur et termine l’exécution\n");
    printf("    -s, --symtabs: Affiche toutes les tables des symboles sur la sortie standard\n");
}

int main(int argc, char *argv[]) {
    char* fileName = NULL;
    
    for(int i = 1; i < argc; i++) {
        if(strcmp(argv[i], "--tree") == 0 || strcmp(argv[i], "-t") == 0) {
            tree = 1;
        } else if (strstr(argv[i], ".tpc") != NULL) {
            fileName = argv[i];
            
            fileNameAsm = strdup(argv[i]);
            fileNameAsm = strrchr(fileNameAsm, '/');
            if(fileNameAsm == NULL) {
                fileNameAsm = argv[i];
            } else {
                fileNameAsm++;
            }
            char* ext = strrchr(fileNameAsm, '.');
            if(ext != NULL) {
                *ext = '\0';
            }  
            fileNameAsm = strcat(fileNameAsm, ".asm");

        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            afficheDescription();
            return 0;
        } else if (strcmp(argv[i], "--symtabs") == 0 || strcmp(argv[i], "-s") == 0) {
            tableSymboles = 1;
        } else {
            printf("Illegal argument : %s\n", argv[i]);
            return 2;
        }
    }
    FILE* fichier = NULL;
    if (fileName != NULL) {
        fichier = fopen(fileName, "r");
        if(fichier == NULL) {
            printf("Unable to open file: %s\n", fileName);
            return 2;
        }
        yyin = fichier;
    }
    
    int result = yyparse();
    
    if(fichier != NULL)
        fclose(fichier);
    
    return result;
}