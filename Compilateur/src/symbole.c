#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "symbole.h"

int error = 0;
extern int lineno;

int estPresent(char *name, Symbole *symbole) {
    if(symbole == NULL || name == NULL) {
        return 0;
    }
    Symbole *tmp = symbole;
    while(tmp != NULL) {
        if(tmp->name != NULL && strcmp(name, tmp->name) == 0)
            return 1;
        tmp = tmp->suiv;
    }
    return 0;
}

Symbole* alloueSymbole(char *nom, char *type) {
    // Allocation du nouveau symbole
    Symbole* Symb = (Symbole*)malloc(sizeof(Symbole));
    if (Symb == NULL) {
        fprintf(stderr, "Erreur d'allocation de mémoire pour le symbole\n");
        error = 2;
    }
    Symb->name = nom;
    Symb->type = type;
    Symb->adresse = 0;
    Symb->indexTab = 0;
    Symb->suiv = NULL;
    return Symb;
}

void ajoutSymbole(Symbole** tete, char *name, char *type, int indexTab) {
    if(!name || !type) {
        fprintf(stderr, "Erreur : nom ou type est NULL\n");
        error = 2;
        return;
    }
    Symbole *symbole = NULL;
    if(indexTab > 0) {
        if(strcmp(type, "int") == 0) 
            symbole = alloueSymbole(name, "tableauInt");
        if(strcmp(type, "char") == 0) 
            symbole = alloueSymbole(name, "tableauChar");
    }
    else {
        symbole = alloueSymbole(name, type);
    }
    symbole->indexTab = indexTab;
    // Si la liste est vide, le symbole devient la tête
    if (*tete == NULL) {
        *tete = symbole;
    } else {
        Symbole *tmp = *tete;
        while (tmp->suiv != NULL) {
            tmp = tmp->suiv;
        }
        tmp->suiv = symbole;
    }
    symbole->adresse = (*tete)->adresse + 8;
}

TableSymbole * creerTabSymbole(char* name, char *type, Symbole *params, int nb_param) {
    TableSymbole *t;
    t = (TableSymbole *)malloc(sizeof(TableSymbole));
    
    if (t == NULL) {
        fprintf(stderr, "Erreur d'allocation : tableau des symboles globaux\n");
        error = 2;
    }
    t->name = name;
    t->type = type;
    t->adresse_retour = 0;
    t->symbole = params;
    t->nb_param = nb_param;
    t->suiv = NULL;
    return t;
}

TableSymbole *ajoutTabSymb(TableSymbole *tete, char* name, char *type, Symbole *params, int nb_param) {
    TableSymbole *nv_table = creerTabSymbole(name, type, params, nb_param);
    if(tete == NULL) // Si la table est vide (c'est la première table qui est ajoutée)
        return nv_table;
    TableSymbole *tmp = tete;
    while(tmp->suiv != NULL) {
        tmp = tmp->suiv;
    }
    nv_table->adresse_retour = tmp->adresse_retour + 8;
    tmp->suiv = nv_table;
    return tete;
}

TableSymbole *construitListeSymbole(Node* node, Tables *table) {
    char *name =  node->firstChild->firstChild->nextSibling->attribute;
    char *type_symbole;
    if(node->firstChild->firstChild->label == _void_) // Cas où le type de retour est void
        type_symbole = "void";
    else
        type_symbole =  node->firstChild->firstChild->attribute;

    if(estPresent(name, table->globale)) {
        fprintf(stderr, "\033[01;31mLine %d, %d : error : la fonction %s et la variable globale %s ont le meme nom\033[0m\n", node->lineno, node->columnno, name, name);
        error = 2;
    }
    
    // Vérifie si le type de la fonction main est bien de type int 
    if(strcmp(name, "main") == 0 && strcmp("int", type_symbole) != 0) {
        fprintf(stderr, "\033[01;31mLine %d, %d : error : in function main -> expected 'int' but have '%s'\033[0m\n", node->lineno, node->columnno, type_symbole);
        error = 2;
    }
    if(strcmp(name, "getint") == 0 || strcmp(name, "putint") == 0 || strcmp(name, "putchar") == 0 || strcmp(name, "getchar") == 0) {
        fprintf(stderr, "\033[01;31mLine %d, %d : error : Redefinition of %s\033[0m\n", node->lineno, node->columnno, name);
        error = 2;
    }
    Symbole *vars = NULL;
    int nb_param = 0;
    // Liste des parametres
    if (node->firstChild->firstChild->nextSibling->nextSibling != NULL) {
        Node* param = node->firstChild->firstChild->nextSibling->nextSibling;
        // Si la fonction a des paramètres
        if (param->label != _void_) {
            param = param->firstChild;
            char* type_param = NULL;
            while (param != NULL) {
                // si le paramètre est un tableau
                if (param->label == paramtab && param->firstChild->attribute != NULL) {
                    if (strcmp(name, param->firstChild->attribute) == 0) { // Si le paramètre a le même nom que la fonction
                        fprintf(stderr, "\033[01;31mLine %d, %d : error : Redefinition of %s\033[0m\n", param->lineno, param->columnno, name);
                        error = 2;
                    } else {
                        ajoutSymbole(&vars, param->firstChild->attribute, type_param, 1);
                        nb_param++;
                    }
                } else if (param->label == type) {
                    type_param = param->attribute;
                } else if (param->label == ident) {
                    if (strcmp(name, param->attribute) == 0) { // Si le paramètre a le même nom que la fonction
                        fprintf(stderr, "\033[01;31mLine %d, %d : error : Redefinition of %s\033[0m\n", param->lineno, param->columnno, name);
                        error = 2;
                    } else {
                        ajoutSymbole(&vars, param->attribute, type_param, 0);
                        nb_param++;
                    }
                }
                param = param->nextSibling;
            }
        }
    }
    /* Variable locales */
    vars = rempliTableSymbole(node->firstChild->nextSibling, vars);
    return ajoutTabSymb(table->fonction, name, type_symbole, vars, nb_param);;
}

int calculExpression(Node* node) {
    if(node == NULL) {
        return 0;
    }
    int res = 0;
    Node *child = node;
    switch(child->label) {
        case character:
            return child->attribute[0];
            break;
        case or:
            return calculExpression(child->firstChild) || calculExpression(child->firstChild->nextSibling);
            break;
        case and:
            return calculExpression(child->firstChild) && calculExpression(child->firstChild->nextSibling);
            break;
        case eq:
            return calculExpression(child->firstChild) == calculExpression(child->firstChild->nextSibling);
            break;
        case different:
            return calculExpression(child->firstChild) != calculExpression(child->firstChild->nextSibling);
            break;
        case num:
            return atoi(child->attribute);
            break;
        case addsub:
            if(strcmp(child->attribute, "+") == 0)
                return calculExpression(child->firstChild);
            else
                return -calculExpression(child->firstChild);
            break;
        case addition:
            return calculExpression(child->firstChild) + calculExpression(child->firstChild->nextSibling);
            break;
        case soustraction:
            return calculExpression(child->firstChild) - calculExpression(child->firstChild->nextSibling);
            break;
        case multiplication:
            return calculExpression(child->firstChild) * calculExpression(child->firstChild->nextSibling);
            break;
        case division:
            return calculExpression(child->firstChild) / calculExpression(child->firstChild->nextSibling);
            break;
        case modulo:
            return calculExpression(child->firstChild) % calculExpression(child->firstChild->nextSibling);
            break;
        default:
            break;
    }
    return res;
}

Symbole* rempliTableSymbole(Node* node, Symbole *variables) {
    if(node == NULL) {
        fprintf(stderr, "null") ;
        return variables;
    }
    char* type_var;
    for (Node *child2 = node->firstChild->firstChild; child2 != NULL; child2 = child2->nextSibling) {
        if(child2->label == type)
            type_var = child2->attribute;
        if(child2->label == ident) {
            if(strcmp(child2->attribute, "getint")  == 0 || strcmp(child2->attribute, "putint")  == 0 ||
               strcmp(child2->attribute, "getchar") == 0 || strcmp(child2->attribute, "putchar") == 0
            ) {
                fprintf(stderr, "\033[01;31mLine %d, %d : Error : redeclaration of '%s'\033[0m\n", child2->lineno, child2->columnno, child2->attribute);
                error=2;
            }
            if(estPresent(child2->attribute, variables)) {
                fprintf(stderr, "\033[01;31mLine %d, %d : Error : Redeclaration of %s\033[0m\n", child2->lineno, child2->columnno, child2->attribute);
                error = 2;
            }
            else {
                ajoutSymbole(&variables, child2->attribute, type_var, 0);
                if(child2->firstChild != NULL) {
                    for(Node* child3 = child2->firstChild; child3!= NULL; child3 = child3->nextSibling){
                        if(estPresent(child3->attribute, variables)) {
                            fprintf(stderr, "\033[01;31mLine %d, %d : error : Redeclaration of %s\033[0m\n", child3->lineno, child3->columnno, child3->attribute);
                            error = 2;
                        }
                        else
                            ajoutSymbole(&variables, child3->attribute, type_var, 0);
                    }   
                }
            }
            
        }
        // Si c'est un tableau
        if(child2->label == tab) {
            if(strcmp(child2->firstChild->attribute, "getint")  == 0 || strcmp(child2->firstChild->attribute, "putint")  == 0 ||
               strcmp(child2->firstChild->attribute, "getchar") == 0 || strcmp(child2->firstChild->attribute, "putchar") == 0
            ) {
                fprintf(stderr, "\033[01;31mLine %d, %d : Error : redeclaration of '%s'\033[0m\n", child2->lineno, child2->columnno, child2->firstChild->attribute);
                error=2;
            }
            if(estPresent(child2->firstChild->attribute, variables)) {
                fprintf(stderr, "\033[01;31mLine %d, %d error : Redeclaration of %s\033[0m\n", child2->firstChild->lineno, child2->firstChild->columnno, child2->firstChild->attribute);
                error = 2;
            }
            else {
                if(calculExpression(child2->firstChild->nextSibling) <= 0) {
                    fprintf(stderr, "\033[01;31mLine %d, %d error : The size of the array must be positive\033[0m\n", child2->firstChild->nextSibling->lineno, child2->firstChild->nextSibling->columnno);
                    error = 2;
                }
                else{
                    ajoutSymbole(&variables, child2->firstChild->attribute, type_var, calculExpression(child2->firstChild->nextSibling));
                    Node *child3 = child2->firstChild->nextSibling;
                    while(child3->nextSibling != NULL) {
                        if(estPresent(child3->nextSibling->attribute, variables)) {
                            fprintf(stderr, "\033[01;31mLine %d, %d error : Redeclaration of %s\033[0m\n", child3->nextSibling->lineno, child3->nextSibling->columnno, child3->nextSibling->attribute);
                            error = 2;
                        }
                        else
                            ajoutSymbole(&variables, child3->nextSibling->attribute, type_var, 0);
                        child3 = child3->nextSibling;
                    }
                }
            }
        }
    }
    return variables;
}

void afficheSymbole(Symbole *symbole) {
    Symbole *tmp = symbole;
    while(tmp) {
        if(tmp->indexTab != 0)
            printf("Nom : %s, Type : %s, Taille : %d (Tableau)\n", tmp->name, tmp->type, tmp->indexTab);
        else
            printf("Nom : %s, Type : %s\n", tmp->name, tmp->type);
        tmp = tmp->suiv;
    }
}

void afficheTable(TableSymbole *table) {
    if(table == NULL) {
        printf("La table est vide\n");
        return ;
    }
    TableSymbole *tmp = table;
    while(tmp != NULL) {
        printf("Nom : %s, Type : %s, Nombre paramètres: %d\n", tmp->name, tmp->type, tmp->nb_param);
        printf("Paramètres + variable : \n");
        afficheSymbole(tmp->symbole);
        printf("\n");
        tmp = tmp->suiv;
    }
}

// recupere et retourne l'adresse de la variable dans la table des symboles
int recupAdresse(char *nameVar, Tables *t, char* nameFonction) {
    if (!t) {fprintf(stderr, "La table de symbole est vide\n");return -1;}
    TableSymbole *fonctionTmp = t->fonction;
    // On le recherche d'abord dans les variables des fonctions
    while (fonctionTmp != NULL) {
        if(fonctionTmp->name != NULL && nameFonction != NULL && strcmp(fonctionTmp->name, nameFonction) == 0) {
            Symbole *symboleCourant = fonctionTmp->symbole;
            while (symboleCourant != NULL) {
                if (symboleCourant->name != NULL && nameVar != NULL && strcmp(symboleCourant->name, nameVar) == 0) {
                    return symboleCourant->adresse;
                }
                symboleCourant = symboleCourant->suiv;
            }
        }
        fonctionTmp = fonctionTmp->suiv;
    }
    // Ensuite dans les variables globales
    Symbole *tmp = t->globale;
    while (tmp != NULL) {
        if (tmp->name != NULL && nameVar != NULL && strcmp(tmp->name, nameVar) == 0) {
            return tmp->adresse;
        }
        tmp = tmp->suiv;
    }
    // La variable n'est pas trouvée
    printf("La variable %s n'a pas été trouvée dans la table des symboles\n", nameVar);
    return -1;
}

// recupere et retourne le type de la variable dans la table des symboles
// retourne NULL si la variable n'est pas trouvée
// res possible -> int, char, tableau
char *recupType(char *nameVar, Tables *t, char* nameFonction) {
    if (!t) return NULL;
    TableSymbole *tmp = t->fonction;
    // recherche dans les variables locales
    while (tmp != NULL) {
        if((tmp->name != NULL && nameFonction != NULL && strcmp(tmp->name, nameFonction) == 0) || nameFonction == NULL) {
            Symbole *symboleCourant = tmp->symbole;
            while (symboleCourant != NULL) {
                if (symboleCourant->name != NULL && nameVar != NULL && strcmp(symboleCourant->name, nameVar) == 0) {
                    return symboleCourant->type;
                }
                symboleCourant = symboleCourant->suiv;
            }
        }
        tmp = tmp->suiv;
    }
    // Ensuite dans les variables globales
    Symbole *tmp2 = t->globale;
    while (tmp2 != NULL) {
        if (tmp2->name != NULL && nameVar != NULL && strcmp(tmp2->name, nameVar) == 0) {
            return tmp2->type;
        }
        tmp2 = tmp2->suiv;
    }
    // La variable n'est pas trouvée
    printf("\033[01;31mError :La variable %s n'a pas été trouvée dans la table des symboles\033[0m\n", nameVar);
    error = 2;
    return "void";
}

Tables *alloueTable() {
    Tables *t = (Tables *) malloc(sizeof(Tables));
    if(t != NULL) {
        t->fonction = NULL;
        t->fonctCourante = NULL;
        t->globale = NULL;
    }
    return t;
}

char *recupTypeFonctionTable(char *name, Tables *t) {
    if (!t || !t->fonction || !name) {
        fprintf(stderr, "\033[01;31mError in function : 'recupTypeFonctionTable' -> 't' is NULL\033[0m\n");
        error = 2;
        return NULL;
    }
    TableSymbole *fonctionTmp = t->fonction;
    while (fonctionTmp != NULL) {
        if (name != NULL && fonctionTmp->name != NULL && strcmp(fonctionTmp->name, name) == 0) {
            return fonctionTmp->type;
        }
        fonctionTmp = fonctionTmp->suiv;
    }
    return NULL;
}

char *calculTypeExpression(Node *node, Tables *t, char *nameFonction) {
    if (!node || !t) {
        return "void";
    }
    char * type1 = NULL;
    char * type2 = NULL;
    switch(node->label) {
        case tab:
            return recupType(node->firstChild->attribute, t, nameFonction);
            break;
        case tabAffect:
            // verifier que c'est vraiment un tableau
            if(estTableau(node->firstChild->attribute, t) <= 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas un tableau\033[0m\n", node->firstChild->lineno, node->firstChild->columnno, node->firstChild->attribute);
                error = 2;
            }
            // on verifie que le le type de l'index est bien un entier
            char *typeIndex = calculTypeExpression(node->firstChild->nextSibling, t, nameFonction);
            compareType("int", typeIndex);
            return "int";
            break;
        case fonction:
            if(strcmp(node->attribute, "getint") == 0) {
                verificationAppelFonction(node, t, nameFonction);
                return "int";
            }
            if(strcmp(node->attribute, "putint") == 0 || strcmp(node->attribute, "putchar") == 0) {
                verificationAppelFonction(node, t, nameFonction);
                return "void";
            }
            if(strcmp(node->attribute, "getchar") == 0) {
                verificationAppelFonction(node, t, nameFonction);
                return "char";
            }
            // verifier que c'est bien une fonction
            if(estFonction(node->attribute, t) != 1) {
                fprintf(stderr, "\033[01;31mLine %d, %d error : %s n'est pas une fonction\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
                return NULL;
            }
            // verifier le type de retour de la fonction
            char *typeFonction = recupTypeFonctionTable(node->attribute, t);
            if(strcmp(typeFonction, "void") == 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d error : La fonction %s ne retourne rien\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
            }
            return typeFonction;
            break;
        case ident:
            if(node->attribute != NULL) {
                char *typeVar = recupType(node->attribute, t, nameFonction);
                if(typeVar == NULL) {
                    fprintf(stderr, "\033[01;31mLine %d, %d error : %s n'est pas déclaré\033[0m\n", node->lineno, node->columnno, node->attribute);
                    error = 2;
                }
                return typeVar;
            }
            break;
        case num:
            return "int";
            break;
        case character:
            return "char";
            break;
        case addsub:;
            return calculTypeExpression(node->firstChild, t, nameFonction);
            break;
        case or:;
        case and:;
        case eq:;
        case neq:;
        case different:;
        case inferieur:;
        case inferieurEgal:;
        case superieur:;
        case superieurEgal:;
        case addition:;
        case soustraction:;
        case multiplication:;
        case division:;
        case modulo:;
            type1 = calculTypeExpression(node->firstChild, t, nameFonction);
            type2 = calculTypeExpression(node->firstChild->nextSibling, t, nameFonction);
            if(strcmp(type1, "tableauInt") == 0 || strcmp(type2, "tableauInt") == 0 || strcmp(type2, "tableauInt") == 0 || strcmp(type1, "tableauInt") == 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : Operation sur un tableau\033[0m\n", node->firstChild->lineno, node->firstChild->columnno);
                error = 2;
            }
            return type1;
            break;
        default:
            break;
    }
    return "void";
}

// verifie que tous les types de retour de la fonction sont corrects
int verifierTypeRetourFonction(Node *node, char *type, Tables *t, char *nameFonction) {
    // chercher tous les noeuds return
    int res = 0;
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
        if (child->label == _return_) {
            // calcul le type de retour de l'expression
            char *typeExp = calculTypeExpression(child->firstChild, t, nameFonction);
            // on compare avec le type de retour de la fonction
            compareType(type, typeExp);
            res = 1;
        }
        int tmp = verifierTypeRetourFonction(child, type, t, nameFonction);
        if(tmp > 0)
            res = tmp;
    }
    return res;
}

Tables *parcours(Node *node, Tables *t) {
    if(node->label == prog) { // debut de l'arbre
        t->globale = rempliTableSymbole(node, t->globale); // variable globales
    }
    TableSymbole *nouvelleFonction = NULL;
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
        switch(child->label) {
            case declfonct:
                nouvelleFonction = construitListeSymbole(child, t);
                if (!t->fonction) {
                    t->fonction = nouvelleFonction;
                    t->fonctCourante = t->fonction; // Si c'est la première fonction, pointe vers la tête
                } else {
                    while(nouvelleFonction->suiv) {
                        nouvelleFonction = nouvelleFonction->suiv;
                    }
                    t->fonctCourante = nouvelleFonction; // Ajoute à la fin de la liste
                }
                // verifier que le type de fonction correspond au type de retour
                // Recupere le type de retour de la fonction a partir de l'etiquette suiteinstr
                char *type = recupTypeFonctionTable(t->fonctCourante->name, t);
                int a = verifierTypeRetourFonction(child, type, t, t->fonctCourante->name);
                if(a == 0 && type != NULL && strcmp(type, "void") != 0) {
                    fprintf(stderr, "\033[01;31mLine %d, %d: error : La fonction %s n'a pas de return\033[0m\n", child->lineno,child->columnno, t->fonctCourante->name);
                    error = 2;
                }
                break;
            case suiteinstr:;
                parcoursInstr(child, t, t->fonctCourante->name);
                break;
            default:
                break;
        }
        t = parcours(child, t);
    }
    return t;
}

// et retourne l'index du tableau si cela en est un
int estTableau(char *name, Tables *t) {
    if (!t || !name) {
        fprintf(stderr, "\033[01;31mError in function : 'estTableau' -> 't' is NULL\033[0m\n");
        error = 2;
        return 0;
    }
    for(TableSymbole *fonctionTmp = t->fonction; fonctionTmp; fonctionTmp = fonctionTmp->suiv) {
        Symbole *symboleCourant = fonctionTmp->symbole;
        while (symboleCourant) {
            if (symboleCourant->name != NULL && name != NULL && strcmp(symboleCourant->name, name) == 0) {
                if (symboleCourant->indexTab > 0) {
                    return symboleCourant->indexTab;
                }
            }
            symboleCourant = symboleCourant->suiv;
        }
    }
    Symbole *tmp = t->globale;
    while (tmp) {
        if (tmp->name != NULL && name != NULL && strcmp(tmp->name, name) == 0) {
            if (tmp->indexTab > 0) {
                return tmp->indexTab;
            }
        }
        tmp = tmp->suiv;
    }
    
    return 0;
}

void parcoursIf(Node *node, Tables *t) {
    if(!node) {
        return;
    }
    switch(node->label) {
        case fonction:
            if(strcmp(node->attribute, "getint") == 0 || strcmp(node->attribute, "getchar") == 0){
                return;
            }
            if(strcmp(node->attribute, "putint") == 0 || strcmp(node->attribute, "putchar") == 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s ne peut pas être utilisé dans un 'if'\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
                return;
            }
            // verifier que c'est bien une fonction
            if(estFonction(node->attribute, t) != 1) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas une fonction\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
                return;
            }
            // verifier le type de retour de la fonction
            char *typeFonction = recupTypeFonctionTable(node->attribute, t);
            compareType("int", typeFonction);
            verificationAppelFonction(node, t, NULL);
            break;
        case inferieur:
        case inferieurEgal:
        case superieur:
        case superieurEgal:
        case and:
        case or:
        case eq:
        case neq:
        case addition:
        case soustraction:
        case multiplication:
        case division:
        case modulo:
        case different:
            parcoursIf(node->firstChild, t);
            parcoursIf(node->firstChild->nextSibling, t);
            break;
        case ident:
            if(estTableau(node->attribute, t) > 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : impossible to use type Tab in 'if'\033[0m\n", node->lineno, node->columnno);
                error = 2;
                return; 
            }
            // recupere le type de la variable
            char *typeVar = recupType(node->attribute, t, NULL);
            // Si on ne retrouve pas la variable dans la table des symboles
            if(typeVar == NULL) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas déclaré\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
                return;
            }
            // on verifie que le type de la variable est bien un entier
            if(strcmp(typeVar, "int") != 0 && strcmp(typeVar, "char") != 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s doit être un entier\033[0m\n", node->lineno, node->columnno, node->attribute);
                error = 2;
            }
            break;
        case tabAffect:
            // verifier que c'est vraiment un tableau
            if(estTableau(node->firstChild->attribute, t) <= 0) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas un tableau\033[0m\n", node->lineno, node->columnno, node->firstChild->attribute);
                error = 2;
            }
            break;
        default:
            break;
    }
}

/* Parcours le noeud suiteinstr */
int parcoursInstr(Node *node, Tables *t, char *nameFonction) {
    if (!t) {
        printf("Function 'parcoursInstr': Table is NULL\n");
        return 0;
    }
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
        switch (child->label) {
            case fonction:
                if(strcmp(child->attribute, "getint") == 0 || strcmp(child->attribute, "getchar") == 0 || strcmp(child->attribute, "putint") == 0 || strcmp(child->attribute, "putchar") == 0) {
                    verificationAppelFonction(child, t, nameFonction);
                    return 1;
                }
                // // verifier que c'est bien une fonction
                if(estFonction(child->attribute, t) != 1) {
                    fprintf(stderr, "\033[01;31mLine %d, %d error : %s n'est pas une fonction\033[0m\n", child->lineno, child->columnno, child->attribute);
                    error = 2;
                    return 0;
                }
                verificationAppelFonction(child, t, nameFonction);
                break;
            case AffectationTab:;
                int indexTab = estTableau(child->firstChild->attribute, t);
                // si c'est un tableau
                if(indexTab > 0) {
                    // verifier le type de l'index
                    char *typeIndex = calculTypeExpression(child->firstChild->nextSibling, t, nameFonction);
                    compareType("int", typeIndex);
                    // si le fils est un tableau
                    if(child->firstChild->nextSibling->nextSibling->label == tabAffect) {
                        char *typeTab = recupType(child->firstChild->attribute, t, nameFonction);
                        if(strcmp(typeTab, "tableauInt") != 0 && strcmp(typeTab, "tableauChar") != 0){
                            fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas un tableau\033[0m\n", child->firstChild->lineno, child->firstChild->columnno, child->firstChild->attribute);
                            error = 2;
                        }
                        // verifier que le type de la variable est le meme que celui de l'expression
                        char *typeExp = calculTypeExpression(child->firstChild->nextSibling->nextSibling, t, nameFonction);
                        compareType("int", typeExp);
                    } else {
                        if(child->firstChild->nextSibling->nextSibling->label == fonction) {
                            int i = 0;
                            TableSymbole *ftmp2 = t->fonction;
                            for(Node *param = child->firstChild->nextSibling->nextSibling->firstChild; param; param = param->nextSibling){
                                i++;
                                if(param->label == fonction) {
                                    int i2 = 0;
                                    if(!estFonction(param->attribute, t)) {
                                        fprintf(stderr, "\033[01;31mError : '%s' is not a function\033[0m\n", param->attribute);
                                        error = 2;
                                    } else {
                                        Node *param2 = param->firstChild;
                                        while(param2->nextSibling) {
                                            i2++;
                                        }
                                        TableSymbole *ftmp = t->fonction;
                                        while(ftmp) {
                                            if(strcmp(ftmp->name, param->attribute) == 0){
                                                if(i2 != ftmp->nb_param) {
                                                    fprintf(stderr, "\033[01;31mError : pas le bon nombre de paramètre\033[0m\n");
                                                    error = 2;
                                                    break;
                                                }   
                                            }
                                            ftmp = ftmp->suiv;
                                        }
                                        
                                        printf("C'est une fonction : %s\n", param->attribute);
                                    }
                                }
                            }
                            while(ftmp2) {
                                if(strcmp(ftmp2->name, child->firstChild->nextSibling->nextSibling->attribute) == 0){
                                    if(i != ftmp2->nb_param) {
                                        fprintf(stderr, "\033[01;31mError : pas le bon nombre de paramètre\033[0m\n");
                                        error = 2;
                                        break;
                                    }   
                                }
                                ftmp2 = ftmp2->suiv;
                            }
                        }
                    }
                    // 
                    // gerer nombre et type quand c'est une fonction
                    //
                    if(child->firstChild->nextSibling->label == fonction) {
                        // verifier que c'est vraiment une fonction
                        if(estFonction(child->firstChild->nextSibling->attribute, t) != 1) {
                            fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas une fonction\033[0m\n", child->firstChild->nextSibling->lineno, child->firstChild->nextSibling->columnno,child->firstChild->nextSibling->attribute);
                            error = 2;
                        }
                        verificationAppelFonction(child->firstChild->nextSibling, t, nameFonction);
                    }

                    else {
                        // verifier que le type de la variable est le meme que celui de l'expression
                        char *typeExp = calculTypeExpression(child->firstChild->nextSibling->nextSibling, t, nameFonction);
                        compareType("int", typeExp);
                    }
                }
                else {
                    fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas un tableau\033[0m\n", child->firstChild->lineno, child->firstChild->columnno, child->firstChild->attribute);
                    error = 2;
                }

                break;
            case affectation:
                if(!estFonction(child->attribute, t)) {
                    // verifier que le type de la variable est le meme que celui de l'expression
                    char *typeVar = recupType(child->firstChild->attribute, t, nameFonction);
                    if(child->firstChild->nextSibling->label == tabAffect) {
                        // si le fils droit est un tableau, verifier que c'est vraiment un tableau
                        if(estTableau(child->firstChild->nextSibling->firstChild->attribute, t) <= 0) {
                            fprintf(stderr, "\033[01;31mLine %d, %d : error : %s n'est pas un tableau\033[0m\n", child->firstChild->nextSibling->firstChild->lineno, child->firstChild->nextSibling->firstChild->columnno, child->firstChild->nextSibling->firstChild->attribute);
                            error = 2;
                        }
                    }                    

                    char *typeExp = calculTypeExpression(child->firstChild->nextSibling, t, nameFonction);
                    compareType(typeVar, typeExp);
                }
                // si on essaie d'affecter une valeur à une fonction
                if(child->firstChild->label == fonction) {
                    fprintf(stderr, "\033[01;31mLine %d, %d : error : %s est une fonction\033[0m\n", child->lineno, child->columnno, child->attribute);
                    error = 2;
                }
                // si le fils droit est une fonction, verifier nombre parametre et type
                if(estFonction(child->firstChild->nextSibling->attribute, t)) {
                    verificationAppelFonction(child->firstChild->nextSibling, t, nameFonction);
                }
                break;
            case _if_:;
                parcoursIf(child->firstChild, t);
                break;
            case _while_:;
                parcoursIf(child->firstChild, t);
                break;
            default:
                parcoursInstr(child, t, nameFonction);
                break;
        }
    }
    return 1;
}

// verification du nombre de parametre et de leur type d'un appel de fonction
void verificationAppelFonction(Node* node, Tables *t, char *nameFonction) {
    if(!node || node->attribute == NULL) {
        return ;
    }
    // compter le nombre de paramètres 
    Node *param = node->firstChild;
    int nb_param = 0;
    while(param != NULL && param->label != _void_) {
        nb_param++;
        param = param->nextSibling;
    }
    Node *child = node;
    // Vérifie si le nombre de paramètres est le bon
    if(strcmp(node->attribute, "getint") == 0 || strcmp(node->attribute, "getchar") == 0){
        if(nb_param != 0) {
            fprintf(stderr, "\033[01;31mLine %d, %d : error : %s function expected 0 parameters, have %d\033[0m\n", node->lineno, node->columnno, node->attribute, nb_param);
            error = 2;
        }
        return;
    }
    if(strcmp(node->attribute, "putint") == 0 || strcmp(node->attribute, "putchar") == 0) {
        if(nb_param != 1) {
            fprintf(stderr, "\033[01;31mLine %d, %d : error: %s function expected 1 parameters, have %d\033[0m\n", node->lineno, node->columnno, node->attribute, nb_param);
            error = 2;
        }
        // verifier que le type de l'expression est int
        char *typeExp = calculTypeExpression(node->firstChild, t, nameFonction);
        compareType("int", typeExp);
        return;
    }
    TableSymbole *fonctionTmp = t->fonction;
    for (; fonctionTmp; fonctionTmp = fonctionTmp->suiv) {
        if (child->attribute != NULL && strcmp(child->attribute, fonctionTmp->name) == 0) {
            if (fonctionTmp->nb_param != nb_param) {
                fprintf(stderr, "\033[01;31mLine %d, %d : error: %s function expected %d parameters, have %d\033[0m\n", child->lineno, child->columnno, fonctionTmp->name, fonctionTmp->nb_param, nb_param);
                error = 2;
            }
            else {
                // refaire un parcours pour verifier les types des parametres
                if(nb_param > 0) {
                    Node *param = child->firstChild;
                    Symbole *symboleCourant = fonctionTmp->symbole;
                    while(param != NULL) {
                        char *typeExp =  calculTypeExpression(param, t, nameFonction);
                        char *typeVar = symboleCourant->type;
                        if(param->label == tabAffect) {
                            if(strcmp(typeVar, "int") != 0) {
                                fprintf(stderr, "\033[01;31mLine %d, %d : error : %s doit être un tableau\033[0m\n", param->firstChild->lineno, param->firstChild->columnno, param->firstChild->attribute);
                                error = 2;
                            }
                        }
                        else {
                            compareType(typeVar, typeExp);
                        }
                        param = param->nextSibling;
                        symboleCourant = symboleCourant->suiv;
                    }
                }
            }
        }
    }
}

/* Compare deux types */
void compareType(char *t1, char *t2) {
    if(!t1 || !t2) {
        error = 2;
        return ;
    }
    if (strcmp(t1, "char") == 0 && strcmp(t2, "int") == 0) {
        fprintf(stderr, "\033[01;35mWarning : assignment from 'char' to 'int'\033[0m\n");
        return ;
    } 
    else if (strcmp(t2, "char") == 0 && strcmp(t1, "int") == 0) {
        return ;
    }
    if (strcmp(t1, t2) != 0) {
        fprintf(stderr, "\033[01;31mError : assignment from %s to %s\033[0m\n", t1, t2);
        error = 2;
    }
    return ;
}

// Fonction qui indique si le noeud est une fonction
int estFonction(char *name, Tables *t) {
    if(t == NULL || name == NULL) {
        return 0;
    }
    TableSymbole *tmp = t->fonction;
    while(tmp != NULL) {
        if(strcmp(name, tmp->name) == 0)
            return 1;
        tmp = tmp->suiv;
    }
    return 0;
}