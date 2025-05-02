#include <stdio.h>
#include <time.h>
#include <MLV/MLV_all.h>
#include <math.h>

#include "Carre.h"
#include "Particule.h"
#include "QuadTree.h"
#include "Affichage.h"

int main(int argc, char *argv[]) {
    srand(time(NULL));

    char distribution;
    char affichage;
    int N = 0;  //Nombre de point maximum
    int Kp = 0;  //Nombre de point maximum
    int wmin = 0;  //Taille minimale d'une feuille
    int hauteur = log2(TAILLE);  //Hauteur de l'arbre

    /*  Manque des options */
    if(argc < 4){
        printf("Il manque des arguments\n");
        printf("Veuillez saisir les paramètres pour le Quadtree :\n");
        printf("Type de distribution : ");
        scanf("%c", &distribution);
        if(distribution == 'a') {
            printf("Type d'affichage (Saisissez \"t\" pour un affichage terminal ou \"d\" pour un affichage dynamique) : ");
            scanf("%c", &affichage);
            while(affichage != 't' && affichage != 'd'){
                printf("Veuillez choisir le mode d'affichage:\nSaisissez \"t\" pour un affichage terminal et \"d\" pour un affichage dynamique : ");
                scanf("%c", &affichage);
            }
        }
        printf("Nombre de particules : ");
        scanf("%d", &N);
        printf("Nombre de particules maximum dans les feuilles : ");
        scanf("%d", &Kp);
        printf("Wmin : ");
        scanf("%d", &wmin);
        while(PuissanceDeQuatre(wmin) == 0) {  //Si wmin n'est pas une puissance de 4
            printf("Entrer incorrect !\t Wmin n'est pas une puissance de 4\nVeuillez entrer un entier étant une puissance de 4 : ");
            scanf("%d", &wmin);
        }
    }

    else{
        /* Distribution à la souris */
        if(strcmp(argv[1], "-s") == 0) {
            distribution = 's';
            N = atoi(argv[2]);
            Kp = atoi(argv[3]);
            wmin = atoi(argv[4]);
            while(PuissanceDeQuatre(wmin) == 0) {  //Si wmin n'est pas une puissance de 4
                printf("Entrer inccorect !\t Wmin n'est pas une puissance de 4\nVeuillez entrer un entier étant une puissance de 4 : ");
                scanf("%d", &wmin);
            }
        }
        /* Distribution aléatoire */
        if(strcmp(argv[1], "-a") == 0) {
            distribution = 'a';
            if(strcmp(argv[2], "-d") == 0)
                affichage = 'd';
            if(strcmp(argv[2], "-t") == 0)
                affichage = 't';
            N = atoi(argv[3]);
            Kp = atoi(argv[4]);
            wmin = atoi(argv[5]);
            while(PuissanceDeQuatre(wmin) == 0) {  //Si wmin n'est pas une puissance de 4
                printf("Entrer inccorect !\t Wmin n'est pas une puissance de 4\nVeuillez entrer un entier étant une puissance de 4 : ");
                scanf("%d", &wmin);
            }
        }
    }
    MLV_create_window("QuadTree", "QuadTree", TAILLE, TAILLE);
    MLV_clear_window(MLV_COLOR_WHITE);  //On ouvre une fenêtre blanche
    if(distribution == 'a') {
        MLV_draw_text(TAILLE / 2 - 75, TAILLE / 2, "Chargement en cours ...", MLV_COLOR_BLACK);
    }
    MLV_actualise_window();

    Particule tab[N];
    Particule tmp;
    QuadTree res = allouerArbre(N, hauteur, Kp, wmin, TAILLE*TAILLE);
    if(distribution == 's') {
        for(int i = 0; i < N; i++) {
            tmp = get_point_on_clic();
            tab[i] = tmp;
            insere(res, tmp, TAILLE*TAILLE);
            MLV_clear_window(MLV_COLOR_WHITE);
            affiche_quadtree(res);
            affichePoint(res->nbp,tab);
            MLV_actualise_window();
        }
        MLV_wait_seconds(5);
        return 1;
    }
    if(distribution == 'a') {
        if(affichage == 'd')
            nuagePoint_Dynamique(N, res, tab, TAILLE*TAILLE);
        if(affichage == 't')
            nuagePoint(N, res, tab, TAILLE * TAILLE);

        affiche_quadtree(res);
        affichePoint(res->nbp,tab);
        MLV_actualise_window();
    }
    MLV_wait_seconds(50);
    return 1;
}