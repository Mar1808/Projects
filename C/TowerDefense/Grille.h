#ifndef _GRILLE_
#define _GRILLE_

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include "Position.h"

#define NB_COL 28
#define NB_LIG 22
#define TAILLE_CASE 20

enum Direction {
    EST,
    OUEST,
    NORD,
    SUD
};

typedef struct grille {
    int grille[NB_LIG][NB_COL];
    Position nid_monstre;
    Position camp;
    Position position_courante;
    Position prec1;
    Position prec2;
} Grille;

Grille initialiseGrille();

void afficheStdout(Grille g);

Position Camp();

int caseValide(Position p);

int AuPlus2Case(Grille G, Position P);

int etendu(Grille G, Position P, int dir);

int distanceManhattan(int x1, int y1, int x2, int y2);

void Virage(Grille G, int dir_courante, int tabEtendu[]);

int ProbaDirection(int Dir[]);

int max(int a, int b);

Grille Chemin();

#endif