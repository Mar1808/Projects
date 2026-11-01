#include <stdio.h>
#include <MLV/MLV_all.h>

#include "Affichage.h"
#include "Particule.h"


void affichePoint(int nbp, Particule *tab) {
    if(tab == NULL) {
        printf("TAB NULL\n");
        return ;
    }
    for(int i = 0; i < nbp; i++) {
        MLV_draw_filled_circle(tab[i].x, tab[i].y, 2, MLV_COLOR_RED);
    }
    MLV_actualise_window();
}

void affiche_carre(QuadTree quadtree){
    if(quadtree->plist != NULL) {
        MLV_draw_filled_rectangle(quadtree -> carre.x+1, quadtree -> carre.y+1, quadtree -> carre.largeur-1, quadtree -> carre.largeur-1, MLV_COLOR_LIGHTCYAN2);
        MLV_draw_rectangle(quadtree -> carre.x, quadtree -> carre.y, quadtree -> carre.largeur, quadtree -> carre.largeur, MLV_COLOR_BLACK);
    }
}

void affiche_quadtree(QuadTree q){
    if(q -> nbp > 0 && q -> plist != NULL){
        affiche_carre(q);
        return;
    }
    if(q->f1)
        affiche_quadtree(q->f1);
    if(q->f2)
        affiche_quadtree(q->f2);
    if(q->f3)
        affiche_quadtree(q->f3);
    if(q->f4)
        affiche_quadtree(q->f4);
    return ;
}