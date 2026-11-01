#include "Affichage.h"

// void afficheQuadrillage() {
//     for(int i = 0; i < NB_LIG + 1; i ++) {
//         MLV_draw_line(0, i * TAILLE_CASE, NB_COL * TAILLE_CASE, i * TAILLE_CASE, MLV_COLOR_BLACK);        
//     }
//     for(int j = 0; j < NB_COL + 1; j ++) {
//         MLV_draw_line(j * TAILLE_CASE, 0, j * TAILLE_CASE, NB_LIG * TAILLE_CASE, MLV_COLOR_BLACK);
//     }
// }

// void afficheGrille(Grille G) {
//     for(int i = 0; i < NB_LIG; i ++) {
//         for(int j = 0; j < NB_COL; j ++){
//             if(G.grille[i][j] == 1) {
//                 MLV_draw_filled_rectangle(j * TAILLE_CASE, i * TAILLE_CASE, TAILLE_CASE, TAILLE_CASE, MLV_COLOR_WHITE);
//             }
//             else {
//                 MLV_draw_filled_rectangle(j * TAILLE_CASE, i * TAILLE_CASE, TAILLE_CASE, TAILLE_CASE, MLV_COLOR_GREY);
//             }
//         }
//     }
//     // nid de monstre
//     //MLV_draw_filled_rectangle(g->nid_monstre.y * TAILLE_CASE, g->nid_monstre.x * TAILLE_CASE, TAILLE_CASE, TAILLE_CASE, MLV_COLOR_GREEN);
//     // camp joueur
//     MLV_draw_filled_rectangle(G.camp.y * TAILLE_CASE, G.camp.x * TAILLE_CASE, TAILLE_CASE, TAILLE_CASE, MLV_COLOR_RED);
//     afficheQuadrillage();
//     MLV_actualise_window();
// }

// void AfficheMonstre(Monstre *m) {
//     while(m->suiv != NULL) {
//         MLV_draw_filled_circle(m->position.x * TAILLE_CASE, m->position.y * TAILLE_CASE, 3, MLV_COLOR_DARK_RED);
//         m = m->suiv;
//     }
//     MLV_actualise_window();
// }
