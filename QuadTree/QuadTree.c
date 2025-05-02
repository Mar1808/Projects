#include <stdlib.h>
#include <stdio.h>
#include <MLV/MLV_all.h>

#include "QuadTree.h"
#include "Particule.h"
#include "Carre.h"
#include "Affichage.h"

QuadTree allouerArbre(int N, int hauteur, int capacite, int wmin, int min) {
    if(hauteur == -1)
        return NULL;
    else {
        QuadTree quadtree = (QuadTree)malloc(sizeof(Noeud));
        quadtree -> carre.y = 0;
        quadtree -> carre.x = 0;
        quadtree -> carre.largeur = TAILLE;
        quadtree -> nbp = 0;
        quadtree -> capacite = capacite;
        quadtree -> wmin = wmin;

        if(min == wmin)  //Si on est dans une feuille selon wmin
            quadtree -> plist = allouerListe(N);
        else
            quadtree -> plist = allouerListe(capacite);

        quadtree -> f1 = allouerArbre(N, hauteur-1, capacite, wmin, min/4);
        quadtree -> f2 = allouerArbre(N, hauteur-1, capacite, wmin, min/4);
        quadtree -> f3 = allouerArbre(N, hauteur-1, capacite, wmin, min/4);
        quadtree -> f4 = allouerArbre(N, hauteur-1, capacite, wmin, min/4);
        return quadtree;
    }
}

void ajout_particule(QuadTree q, Particule p){
    if(q == NULL) {
        return;
    }
    ListeParticules tmp = q -> plist;
    for(int i = 0; i < q-> nbp; i ++) {
        if(tmp -> next){
            tmp = tmp -> next;
        }
    }
    tmp -> p -> x = p.x;
    tmp -> p -> y = p.y;
    q -> nbp ++;
}

void Purge(QuadTree q) {
    if (q == NULL || q -> plist == NULL) {
        return;
    }
    if(q -> nbp == q -> capacite)
        q -> plist = NULL;  //On vide la liste
}

void insere(QuadTree q, Particule p, int min) {
    if(q == NULL) {
        return;
    }
    //Si le nombre de noeud max dans un noeud n'est pas supérieur au nombre de noeud de tout l'arbre
    if(q -> nbp < q -> capacite) {
        ajout_particule(q, p);
        return;
    }
    //Si on est dans la feuille correspondant à la taille minimale de l'arbre
    if(min == q -> wmin) {
        ajout_particule(q, p);
        return;
    }
    //Si la liste est NULL alors on a déjà purger ce noeud on distribue la particule au bon fils
    if(q->plist == NULL) {
        divise_carre(q);
        q->nbp++;  //On va ajouter une particule à l'un de ses fils donc on incrémente nbp

        int new_largeur = q->carre.largeur/2;

        if (p.x < q->carre.x + new_largeur && p.y < q->carre.y + new_largeur) {
            if(q->f1 != NULL) {
                insere(q -> f1, p, min/4);
            }
        } 
        else 
            if (p.x >= q->carre.x + new_largeur && p.y < q->carre.y + new_largeur) {
                if(q->f2 != NULL) {
                    insere(q -> f2, p, min/4);
                }
            } 
        else 
            if (p.x < q->carre.x + new_largeur && p.y >= q->carre.y + new_largeur) {
                if(q->f3 != NULL) {
                    insere(q -> f3, p, min/4);
                }
            }
        else {
            if(q->f4 != NULL) {
                insere(q -> f4, p, min/4);
            }
        }
        return;
    }
    //Si nbp du noeud est égale à la capacité maximum du nombre de particules dans le noeud alors on dispatch et on purge le noeud
    if(q -> nbp == q -> capacite) {
        ListeParticules tmp = q->plist;
        Purge(q);
        divise_carre(q);
        

        int new_largeur = q -> carre.largeur/2;

        // parcourir la liste des particules du noeud courant
        while (tmp != NULL) {
            // répartir la particule dans le sous-noeud correspondant
            if (tmp -> p -> x < q -> carre.x + new_largeur && tmp -> p -> y < q -> carre.y + new_largeur) {
                if(q -> f1 != NULL) {
                    insere(q -> f1, *tmp -> p, min);
                }
            }
            else 
                if (tmp -> p -> x >= q -> carre.x + new_largeur && tmp -> p -> y < q -> carre.y + new_largeur) {
                    if(q -> f2 != NULL) {
                        insere(q -> f2, *tmp -> p, min);
                    }
                }
            else
                if (tmp -> p -> x < q -> carre.x + new_largeur && tmp -> p -> y >= q -> carre.y + new_largeur) {
                    if(q -> f3 != NULL) {
                        insere(q -> f3, *tmp -> p, min);
                    }
                }
            else {
                if(q -> f4 != NULL) {
                    insere(q -> f4, *tmp -> p, min);
                }
            }
            // passer à la particule suivante
            tmp = tmp->next;
        }
    }
    insere(q, p, min);
}

void divise_carre(QuadTree q){
    int x = q -> carre.x;
    int y = q -> carre.y;
    int largeur = q -> carre.largeur;
    if(q -> f1) {
        Carre c_f1 = {x, y, largeur / 2};
        q -> f1 -> carre = c_f1; 
    }
    if(q -> f2) {
        Carre c_f2 = {x + largeur / 2, y, largeur / 2};
        q -> f2 -> carre = c_f2; 
    }
    if(q -> f3) {
        Carre c_f3 = {x, y + largeur / 2, largeur / 2};
        q -> f3 -> carre = c_f3; 
    }
    if(q -> f4) {
        Carre c_f4 = {x + largeur / 2, y + largeur / 2, largeur / 2};
        q -> f4 -> carre = c_f4; 
    }
}

void nuagePoint(int N, QuadTree q, Particule *tab, int min) {
    Particule p;
    for(int i = 0; i < N; i ++) {
        p = point_aleatoire();
        insere(q, p, min);
        tab[i] = p;
    }
}

void nuagePoint_Dynamique(int N, QuadTree q, Particule *tab, int min) {
    Particule p;
    for(int i = 0; i < N; i ++) {
        p = point_aleatoire();
        insere(q, p, min);
        tab[i] = p;
        affiche_quadtree(q);
        affichePoint(q -> nbp, tab);
        MLV_actualise_window();
    }
}

int PuissanceDeQuatre(int n) {
    while(n % 4 == 0 && n != 0) {
        n = n / 4;
    }
    //Le nombre est une puissance de 4
    if(n == 1) {
        return 1;
    }
    //Le nombre n'est pas une puissance de 4
    return 0;
}