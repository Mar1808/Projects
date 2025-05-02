#ifndef _MONSTRE_
#define _MONSTRE_

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "Position.h"

#define H 50

enum Type {
    NORMAL,  // 0
    AGILE,   // 1
    FOULE,   // 2
    BOSS     // 3
};

typedef struct monstre {
    enum Type type;              // Type du monstre
    int HP;                      // Point de vie
    int vitesse;                 // Vitesse de déplacement de case par seconde
    Position position;           // Position du monstre dans le plateau
    struct monstre *suiv;        // Pointeur vers le monstre suivant dans la vague
} Monstre;

typedef struct vague {
    enum Type type;         // Type de la vague
    int nb_monstre;         // Nombre de monstres dans la vague
    int nb_vague;           // Nombre de la n vague
    Position nid;           // Position du nid de monstre 
    Monstre *monstres;      // Liste de monstres dans la vague
    struct vague *suiv;     // Vague suivante
} VagueMonstre;


/* Fonction qui calcule 'a' à la puissance 'i' */
float exponentiation(float a, int i);

/* Comme on connaît déjà le nombre de monstre avant l'allocation on */
/* alloue jusqu'au dernier monstre de la liste de manière récursive */
Monstre* alloue_Monstre(enum Type type, int nb_monstre, int nb_vague, Position nid_monstre);

/* Fonction qui alloue la vague de monstre selon le type reçu en paramètre */
VagueMonstre* alloue_Vague(enum Type type, Position nid_monstre);

/* Fonction qui supprime les monstres qui n'ont plus de vie */
void supprimeMonstre(VagueMonstre *m);

/* Fonction qui affiche sur la sortie standard la liste de monstre passer en paramètre */
void afficheListe(Monstre *liste);

/* Tire un type de monstre aléatoirement */
int tirageMonstre(int nb_vague);

void MonsterMove(VagueMonstre *m);

/*
Une liste de Vague contient plusieurs vague,
Une vague de monstre contient une liste de monstre composé de plusieurs monstres,
Un monstre est représenter par sa vie et sa vitesse et sa position
*/

#endif