#include <stdio.h>
#include <stdlib.h>
#include <time.h>
//#include <MLV/MLV_all.h>

#include "Grille.h"
#include "Monstre.h"
#include "Position.h"
#include "Affichage.h"

int main() {
    srand( time( NULL ) );
    //MLV_create_window("", "", 1000, 800);
    Grille G = Chemin();

    // afficheGrille(G);
    // MLV_actualise_window();
    // MLV_free_window();
    // // return 0;

    int nb_vague = 1;
    Position nid = G.nid_monstre;
    enum Type type = tirageMonstre(nb_vague); // Tirage du type de monstre pour la première vague
    if (type == -1) { // Erreur, ce type n'existe pas
        return 1;
    }

    VagueMonstre *lst = alloue_Vague(AGILE, nid); // La première vague

    // // // On alloue les monstres dans la première vague
    lst->monstres = alloue_Monstre(type, lst->nb_monstre, nb_vague, nid);
    // afficheListe(lst->monstres);

    //AfficheMonstre(lst->monstres);
    // MLV_actualise_window();

    // MLV_wait_seconds(90);

    MonsterMove(lst);
    printf("*********************\n");
    afficheListe(lst->monstres);
    return 0;
}