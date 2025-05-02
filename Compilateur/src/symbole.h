#ifndef SYMBOLE
#define SYMBOLE

#include "tree.h"

// variable / parametres
typedef struct symbole {
    char *name;                     // nom de la variable
    int adresse;                    // adresse de la variable
    char *type;                     // type de la variable
    int indexTab;                   // index du tableau
    struct symbole *suiv;
} Symbole;

// fonction
typedef struct tableSymbole { 
    char *name;                     // nom de la fonction
    char *type;                     // type de la fonction
    int adresse_retour;             // adresse de retour
    Symbole *symbole;               // tables des variables locales + params
    int nb_param;                   // nombre param
    struct tableSymbole *suiv;      // pointeur vers la tete de liste de symbole
} TableSymbole;

typedef struct tables {
    TableSymbole *fonction;         // tables des variables locales
    TableSymbole *fonctCourante;    // Garde en mémoire l'adresse de la dernière fonction insérer
    Symbole *globale;               // tables des variables locales
} Tables;

/**
 * Vérifie si un symbole avec le nom donné est présent dans la liste de symboles.
 *
 * @param name Le nom du symbole à rechercher.
 * @param symbole La liste de symboles dans laquelle rechercher.
 * @return 1 si le symbole est présent, 0 sinon.
 */
int estPresent(char *name, Symbole *symbole);

/**
 * Alloue un nouveau symbole avec le nom et le type donnés.
 *
 * @param nom Le nom du symbole.
 * @param type Le type du symbole.
 * @return Un pointeur vers le nouveau symbole alloué.
 */
Symbole* alloueSymbole(char *nom, char *type);

/**
 * Ajoute un symbole avec le nom et le type donnés à la liste de symboles.
 *
 * @param tete La tête de la liste de symboles.
 * @param name Le nom du symbole à ajouter.
 * @param type Le type du symbole à ajouter.
 * @param indexTab L'index du tableau.
 */
void ajoutSymbole(Symbole** tete, char *name, char *type, int indexTab);

/**
 * Crée une nouvelle instance de la structure TableSymbole avec les paramètres spécifiés.
 *
 * @param name Le nom de la table des symboles.
 * @param type Le type de la table des symboles.
 * @param params Les paramètres de la table des symboles.
 * @param nb_param Le nombre de paramètres de la table des symboles.
 * @return Un pointeur vers la nouvelle instance de TableSymbole.
 */
TableSymbole * creerTabSymbole(char* name, char *type, Symbole *params, int nb_param);

/**
 * Ajoute un symbole à la table des symboles.
 *
 * @param tete La tête de la table des symboles.
 * @param name Le nom du symbole à ajouter.
 * @param type Le type du symbole à ajouter.
 * @param params Les paramètres du symbole à ajouter.
 * @param nb_param Le nombre de paramètres du symbole à ajouter.
 * @return Un pointeur vers la table des symboles mise à jour.
 */
TableSymbole *ajoutTabSymb(TableSymbole *tete, char* name, char *type, Symbole *params, int nb_param);

/**
 * @brief Parcours l'arbre de syntaxe abstraite et construit une table de symboles.
 * 
 * @param node Le nœud racine de l'arbre de syntaxe abstraite.
 * @param table La table de symboles à construire.
 * @return Un pointeur vers la table de symboles construite.
 */
TableSymbole *construitListeSymbole(Node* node, Tables *table);

/**
 * @brief Parcours l'arbre syntaxique pour récupérer les symboles de variables.
 * 
 * Cette fonction parcourt l'arbre syntaxique représenté par le nœud donné en paramètre
 * et récupère les symboles de variables rencontrés. Elle renvoie un pointeur vers la
 * structure Symbole contenant les variables trouvées.
 * 
 * @param node Le nœud de l'arbre syntaxique à parcourir.
 * @param variables Un pointeur vers la structure Symbole contenant les variables trouvées.
 * 
 * @return Un pointeur vers la structure Symbole contenant les variables trouvées.
 */
Symbole* rempliTableSymbole(Node* node, Symbole *variables);

/**
 * Affiche les informations d'un symbole.
 *
 * @param symbole Le symbole à afficher.
 */
void afficheSymbole(Symbole *symbole);

/**
 * Affiche la table des symboles.
 *
 * Cette fonction affiche le contenu de la table des symboles passée en paramètre.
 *
 * @param table Un pointeur vers la table des symboles à afficher.
 */
void afficheTable(TableSymbole *table);

/**
 * Récupère l'adresse d'une variable dans les tables de symboles.
 * 
 * @param nameVar Le nom de la variable.
 * @param t Les tables de symboles.
 * @param nameFonction Le nom de la fonction courante.
 * @return L'adresse de la variable si elle est trouvée, sinon -1.
 */
int recupAdresse(char *nameVar, Tables *t, char* nameFonction);

/**
 * @brief Alloue et retourne une nouvelle table.
 * 
 * @return Un pointeur vers la nouvelle table allouée.
 */
Tables *alloueTable();

/**
 * Effectue un parcours de l'arbre syntaxique à partir du nœud spécifié.
 * 
 * @param node Le nœud à partir duquel effectuer le parcours.
 * @param t    Les tables de symboles utilisées lors du parcours.
 * @return     Les tables de symboles mises à jour après le parcours.
 */
Tables *parcours(Node *node, Tables *t);

/**
 * Effectue un parcours de l'instruction représentée par le nœud donné.
 * 
 * @param node Le nœud représentant l'instruction à parcourir.
 * @param t    Les tables de symboles utilisées pour la vérification des types.
 * @param nameFonction Le nom de la fonction courante.
 * @return     La valeur de retour de l'instruction.
 */
int parcoursInstr(Node *node, Tables *t, char *nameFonction);

/**
 * Compare les types de deux chaînes de caractères.
 *
 * @param t1 La première chaîne de caractères représentant un type.
 * @param t2 La deuxième chaîne de caractères représentant un type.
 */
void compareType(char *t1, char *t2);

/**
 * Vérifie si le nom spécifié est une fonction dans la table des symboles.
 *
 * @param name Le nom à vérifier.
 * @param t    La table des symboles.
 * @return     1 si le nom est une fonction, 0 sinon.
 */
int estFonction(char *name, Tables *t);

/**
 * Calcule l'expression représentée par le nœud donné.
 *
 * @param node Le nœud de l'arbre syntaxique représentant l'expression.
 * @return La valeur calculée de l'expression.
 */
int calculExpression(Node* node);

/**
 * Calcule le type de l'expression représentée par le nœud donné.
 *
 * @param node Le nœud de l'arbre syntaxique représentant l'expression.
 * @param t    Les tables de symboles utilisées pour la vérification des types.
 * @param nameFonction Le nom de la fonction courante.
 * @return Le type de l'expression.
 */
char *calculTypeExpression(Node *node, Tables *t, char *nameFonction);

/**
 * Vérifie si l'appel de fonction est correct en comparant le nombre et les types des paramètres.
 *
 * @param node Le nœud de l'arbre syntaxique représentant l'appel de fonction.
 * @param t    Les tables de symboles utilisées pour la vérification des types.
 * @param nameFonction Le nom de la fonction courante.
 */
void verificationAppelFonction(Node* node, Tables *t, char *nameFonction);

/**
 * Vérifie si le nom spécifié est un tableau dans la table des symboles.
 *
 * @param name Le nom à vérifier.
 * @param t    La table des symboles.
 * @return     1 si le nom est un tableau, 0 sinon.
 */
int estTableau(char *name, Tables *t) ;

#endif
