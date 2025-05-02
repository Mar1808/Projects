#ifndef __PARTICULE__
#define __PARTICULE__

#define TAILLE 512

typedef struct Particule{
    double x;
    double y;
} Particule;

typedef struct ListeParticules{
    Particule * p;
    struct ListeParticules * next;
} Cell, * ListeParticules;

/*Récupère les points à la souris avec une légère perturbation*/
Particule get_point_on_clic();

/*Génére un point aléatoirement*/
Particule point_aleatoire();

/*  */
Particule* allouerParticule(double x, double y);

/*Alloue l'espace nécéssaire à la liste pour contenir nb particules*/
ListeParticules allouerListe(int nb);

#endif