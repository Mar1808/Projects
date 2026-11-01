#ifndef __AFFICHE__
#define __AFFICHE__

#include "QuadTree.h"

/*Affiche les point dans la fenêtre contenu dans le tableau tab*/
void affichePoint(int nb, Particule *tab);

/*Affiche le carre du noeud correpondant en fonction de sa largeur et de ses coordonnées*/
void affiche_carre(QuadTree quadtree);

/*Parcours l'arbre récursivement et affiche ses carrés*/
void affiche_quadtree(QuadTree q);

#endif