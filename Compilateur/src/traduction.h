#ifndef TRADUCTION
#define TRADUCTION

#include "symbole.h"

/**
 * Effectue le parcours de l'arbre syntaxique à partir du nœud donné et génère le code assembleur correspondant.
 * 
 * @param node Le nœud racine de l'arbre syntaxique.
 * @param fichier Le fichier dans lequel écrire le code assembleur généré.
 * @param t Les tables de symboles utilisées pour la traduction.
 * @param nomFonction Le nom de la fonction en cours de traduction.
 */
void parcoursMain(Node *node, FILE* fichier, Tables *t, char *nomFonction);

/**
 * Génère le code assembleur des fonctions prédéfinies.
 * 
 * @param fichier Le fichier dans lequel écrire le code assembleur généré.
 */
void fonctionsASM(FILE* fichier);

/**
 * Effectue la traduction de l'arbre syntaxique à partir du nœud donné et génère le code assembleur correspondant.
 * 
 * @param node Le nœud racine de l'arbre syntaxique.
 * @param fichier Le fichier dans lequel écrire le code assembleur généré.
 * @param t Les tables de symboles utilisées pour la traduction.
 */
void traductionASM(Node* node, FILE* fichier, Tables *t);

#endif