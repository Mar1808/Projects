#include <stdio.h>
#include <MLV/MLV_all.h>

#include "Particule.h"

Particule get_point_on_clic(){
    Particule p;
    double PERTURB = 0.0001 / RAND_MAX;
    int x, y;
    MLV_wait_mouse(&x,&y);
    p.x = x + (rand() % 2 ? + 1.:-1.) * PERTURB * rand();
    p.y = y + (rand() % 2 ? + 1.:-1.) * PERTURB * rand();
    return p;
}

Particule point_aleatoire() {
    Particule p;
    p.x = ((double) rand() / (double) RAND_MAX) * TAILLE;
    p.y = ((double) rand() / (double) RAND_MAX) * TAILLE;
    return p;
}

Particule* allouerParticule(double x, double y) {
    Particule* p = (Particule*) malloc(sizeof(Particule));
    p -> x = x;
    p -> y = y;
    return p;
}

ListeParticules allouerListe(int nb){
    if(nb == 0)
        return NULL;
    else {
        ListeParticules lst = malloc(sizeof(ListeParticules));
        lst -> p = allouerParticule(0, 0);
        lst -> next = allouerListe(nb - 1);
        return lst;
    }
}