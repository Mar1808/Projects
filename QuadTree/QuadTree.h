#ifndef __QT__
#define __QT__

#include "Carre.h"
#include "Particule.h"

#define TAILLE 512

typedef struct QuadTree{
    struct QuadTree *f1, *f2, *f3, *f4;     /* pointeurs vers ses 4 noeuds fils */
    Carre carre;                            /* informations géométriques du noeud */
    ListeParticules plist;                  /* liste de cellules pointant vers les particules présentes dans le noeud */
    int nbp;                                /* nombre de particules «couvertes» par le noeud */
    int capacite;                           /* nombre de points maximal autorise */
    int wmin;                               /* Correspond à la hauteur du noeud max dans l'arbre*/
} Noeud, *QuadTree;

/*  */
QuadTree allouerArbre(int N, int hauteur, int capacite, int wmin, int min);

/*  */
void ajout_particule(QuadTree q, Particule p);

/*  */
void Purge(QuadTree q);

/* Insère le point p dans q */
void insere(QuadTree q, Particule p, int min);

/* Génère un nuage de point qui sont insérer dans un QuadTree */
void nuagePoint(int N, QuadTree q, Particule *tab, int min);

/* Génère un nuage de point, les insère et les affiche un par un */
void nuagePoint_Dynamique(int N, QuadTree q, Particule *tab, int min);

/* Divise en 4 le carré pour distribuer les particules aux fils du noeud actuel */
void divise_carre(QuadTree q);

/* Retourne 1 si n est une puissance de 4, sinon retourne 0 */
int PuissanceDeQuatre(int n);

#endif