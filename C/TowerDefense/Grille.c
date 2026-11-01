#include "Grille.h"

Grille initialiseGrille() {
    Grille G;  // On créer la grille que l'on va renvoyer
    // On initialise tout ses champs à 0
    for(int i = 0; i < NB_LIG; i ++) {
        for(int j = 0; j < NB_COL; j ++){
            G.grille[i][j] = 0;
        }
    }
    return G;
}

void afficheStdout(Grille g) {
    for(int i = 0; i < NB_LIG; i ++) {
        for(int j = 0; j < NB_COL; j ++){
            printf("%d ", g.grille[i][j]);
        }
        printf("\n");
    }
}

int distanceManhattan(int x1, int y1, int x2, int y2) {
    return abs(x1 - x2) + abs(y1 - y2);
}

Position Camp() {
    Position p;
    // Génère un nombre aléatoire compris entre 3 et 25 inclus
    p.x = rand() % (26 - 3) + 3;
    // Génère un nombre aléatoire compris entre 3 et 19 inclus
    p.y = rand() % (20 - 3) + 3;
    return p;
}

/* Vérifie si la position est à 3 cases ou plus des bords de la grille */
int caseValide(Position p) {
    return p.x >= 2 && p.x < NB_COL - 2 && p.y >= 2 && p.y < NB_LIG - 2;
}

/* Vérifie si la case ne rentre pas en contact d'une distance d'au moins de 2 avec une case qui occupe le chemin */
int AuPlus2Case(Grille G, Position P) {
    static const int offsetx[] = {1, -1, 0, 0};   // EST - OUEST
    static const int offsety[] = {0, 0,  -1, 1};  // NORD - SUD
    static const int offsetDiag[] = {-1, 1};      // Tableau pour gérer les diagonales

    Position pos_temp = P;
    Position tmp = P;
    
    for(int i = 0; i < 4; i++) {
        for (int j = 1; j <= 2; j++) { // Case à une distance allant jusqu'à deux 2
            tmp.x = pos_temp.x + j * offsetx[i];
            tmp.y = pos_temp.y + j * offsety[i];
        
            // Vérifier si la case de la grille n'est pas occupée
            if (G.grille[tmp.y][tmp.x] == 1) {
                return 0;
            }
            if (i < 2) {  // Pour vérifier les diagonales
                tmp.x = pos_temp.x + offsetDiag[i];
                tmp.y = pos_temp.y + offsetDiag[j - 1];

                if (G.grille[tmp.y][tmp.x] == 1) {
                    return 0;
                }
            }
        }
    }
    return 1;
}


/* Fonction qui retourne l'étendu pour une directions */
int etendu(Grille G, Position P, int dir) {
    static const int offsetx[] = {1, -1, 0, 0};  // EST - OUEST
    static const int offsety[] = {0, 0, -1, 1};  // NORD - SUD
    int etendu = 0;
    Position temp = P;
    // On veut vérifier l'étendu de la case suivante 
    // On met si possible la position à jour
    temp.x += offsetx[dir];
    temp.y += offsety[dir];
    G.grille[P.y][P.x] = 0;  // On met la position initiale a 0 dans la grille pour pas que cela cause problème pour l'étendu
    while (AuPlus2Case(G, temp) && caseValide(temp)) {
        temp.x += offsetx[dir];
        temp.y += offsety[dir];
        etendu++;
    }
    G.grille[P.y][P.x] = 1;
    return etendu;
}

int ProbaDirection(int Dir[]) {
    int probaE = Dir[0];
    int probaO = Dir[1];
    int probaN = Dir[2];
    int probaS = Dir[3];
    int sum = probaE + probaO + probaN + probaS;

    if(sum <= 2) {  // Si la somme des etendu <= 2 c'est qu'aucune etendu n'est supérieur à 2 on quitte le code
        return -1;
    }
    int n = rand() % sum;
    
    if (n < probaE) {
        return 0; // Direction Est
    } else if (n < probaE + probaO) {
        return 1; // Direction Ouest
    } else if (n < probaE + probaO + probaN) {
        return 2; // Direction Nord
    } else {
        return 3; // Direction Sud
    }
}

int valeurAleatoire(int n) {
    int s = 0;
    for(int i = 0; i < n; i++) {
        if ((rand() % 4) < 3) {
            s += 1;
        }
    }
    return s;
}

int max(int a, int b){
    return a > b ? a : b; 
}

int nbCase(int n) {
    int s = valeurAleatoire(n);
    return max(s, 3);
}

/* Met a jour les cases de la grille et change la valeur de la position courante */
void avanceCase(Grille *G, Position *p, int nb_case, int dir) {
    static const int offsetx[] = {1, -1, 0, 0};  // EST - OUEST
    static const int offsety[] = {0, 0, -1, 1};  // NORD - SUD

    Position prec_prec = *p;  // Position précédant prec1

    for (int i = 0; i < nb_case; i++) {
        if (i == nb_case - 2) {
            G->prec2 = prec_prec;  // Mise à jour de prec2
        } else if (i == nb_case - 1) {
            G->prec1 = *p;  // Mise à jour de prec1
        }

        prec_prec = *p;  // Mettre à jour la position précédente à chaque itération
        p->x = p->x + offsetx[dir];
        p->y = p->y + offsety[dir];
        G->grille[p->y][p->x] = 1;
    }
}

void Virage(Grille G, int dir_courante, int tabEtendu[]) {
    //Lors des virages pour calculer l'étendu on doit ignorer les deux dernières cases précédentes qui sont dans le chemin
    G.grille[G.prec1.y][G.prec1.x] = 0;
    G.grille[G.prec2.y][G.prec2.x] = 0;
    
    if (dir_courante == 0 || dir_courante == 1) {
        tabEtendu[0] = tabEtendu[1] = 0;
        tabEtendu[2] = etendu(G, G.position_courante, 2);
        tabEtendu[3] = etendu(G, G.position_courante, 3);
    } else {
        tabEtendu[2] = tabEtendu[3] = 0;
        tabEtendu[0] = etendu(G, G.position_courante, 0);
        tabEtendu[1] = etendu(G, G.position_courante, 1);
    }
    // On les remets à 1 après avoir calculer l'étendu
    G.grille[G.prec1.y][G.prec1.x] = 1;
    G.grille[G.prec2.y][G.prec2.x] = 1;
}

Grille Chemin() {
    while(1) {  // Boucle pour démarrer et recommencer si l'algorithme à échouer
        Grille G = initialiseGrille();
        G.camp = Camp();
        G.position_courante = G.camp;
        int longueur_chemin = 0;
        int virage = 0;

        G.grille[G.camp.y][G.camp.x] = 1;
        longueur_chemin ++;
        
        int tabEtendu[4];
        for (int i = 0; i < 4; i++) {  // On initialise l'étendu de chaque direction du camp
            tabEtendu[i] = etendu(G, G.position_courante, i);
        }
        int dir_courante = ProbaDirection(tabEtendu);   // On tire aléatoirement une direction
        int nb_case = nbCase(tabEtendu[dir_courante]);  // Nombre de case que l'on va parcourir
        avanceCase(&G, &G.position_courante, nb_case, dir_courante);  // On met a jour la position courante et les cases par lesquels on est passer
        longueur_chemin += nb_case;

        while(1) {  // Boucle pour faire des virages et construire le chemin
            Virage(G, dir_courante, tabEtendu);
            dir_courante = ProbaDirection(tabEtendu);
            if(dir_courante == -1 || tabEtendu[dir_courante] <= 2) {  // Si l'étendu est inférieur à 3 on quitte la boucle
                break;
            }
            nb_case = nbCase(tabEtendu[dir_courante]);
            avanceCase(&G, &G.position_courante, nb_case, dir_courante);
            virage++;
            longueur_chemin += nb_case;
        }
        if (virage >= 7 && longueur_chemin >= 75) {  // Si l'algorithme est un succès on retourne la grille sinon on recommence
            G.nid_monstre = G.position_courante;
            return G;
        }
    }
}
