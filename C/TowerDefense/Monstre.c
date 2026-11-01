#include "Monstre.h"

/* Fonction qui calcule 'a' à la puissance 'i' */
float exponentiation(float a, int i) {
    if (i <= 0) {   // La première vague est n = 1
        printf("ERREUR 'vague %d' not found\n", i);
        return -1;
    }
    float res = 1.0;
    for (int j = 1; j <= i; j++) {
        res *= a;
    }
    return res;
}

/* Comme on connaît déjà le nombre de monstre avant l'allocation on */
/* alloue jusqu'au dernier monstre de la liste de manière récursive */
Monstre* alloue_Monstre(enum Type type, int nb_monstre, int nb_vague, Position nid_monstre) {
    if (nb_monstre <= 0) {  // S'il n'y a aucun monstre à allouer
        printf("Nombre de monstres incorrect\n");
        return NULL;
    }

    Monstre *monstre = malloc(sizeof(Monstre));
    // Si l'allocation à échouer
    if (monstre == NULL) {
        printf("Erreur lors de l'allocation pour les monstres\n");
        return NULL;
    }

    monstre->type = type;
    monstre->position = nid_monstre;
    int health = H * exponentiation(1.2, nb_vague);  // Valeur qui représente les HP (point de vie)
    
    switch(type) {  // Les HP et la vitesse vont être différent selon le type
        case NORMAL:
            monstre->HP = health;
            monstre->vitesse = 1;
            break;
        case AGILE:
            monstre->HP = health;
            monstre->vitesse = 2;
            break;
        case FOULE:
            monstre->HP = health;
            monstre->vitesse = 1;
            break;
        case BOSS:
            monstre->HP = 12 * H * health;  // C'est un BOSS la formule change pour calculer les HP
            monstre->vitesse = 1;
            break;
    }

    if (nb_monstre > 1) {  //S'il y a encore plus d'un monstre à allouer
        // nid_monstre.x -= 1.0;
        monstre->suiv = alloue_Monstre(type, nb_monstre - 1, nb_vague, nid_monstre);
        if (monstre->suiv == NULL) {  // Si l'allocation du suivant à échouer
            free(monstre);  // On libère la mémoire
            return NULL;
        }
    }
    else {
        monstre->suiv = NULL;  // Si on a allouer tout les monstres il n'y a plus de suivant
    }
    return monstre;
}

/* Fonction qui alloue la vague de monstre selon le type reçu en paramètre */
VagueMonstre* alloue_Vague(enum Type type, Position nid_monstre) {
    VagueMonstre *m = (VagueMonstre *)malloc(sizeof(VagueMonstre));
    // Si echec de l'allocation de la Vague de monstre m.
    if (m == NULL) {
        printf("Erreur lors de l'allocation\n");
        return NULL;
    }
    m->type = type;
    // Selon le type de la vague le nombre de monstre est différent
    switch (type) {
        case NORMAL:
            m->nb_monstre = 12;
            break;
        case AGILE:
            m->nb_monstre = 12;
            break;
        case FOULE:
            m->nb_monstre = 24;
            break;
        case BOSS:
            m->nb_monstre = 2;
            break;
        // Si on est ici le type de la vague n'est pas reconnu on renvoie une erreur
        default:
            printf("Type de vague non reconnu\n");
            free(m); // Libération de la mémoire si le type de vague n'est pas reconnu
            return NULL;
    }
    m->monstres = NULL;
    m->nid = nid_monstre;
    return m;
}

/* Fonction qui supprime les monstres qui n'ont plus de vie */
void supprimeMonstre(VagueMonstre *m) {
    if (m == NULL || m->monstres == NULL) {
        printf("Rien à supprimer, la liste est vide\n");
        return;
    }
    Monstre *newm = m->monstres;    // Copie du monstre pour les itérations
    Monstre *tmp_prec = NULL;       // Pointeur pour conserver le precedent et le relier au suivant du monstre courant
    Monstre *temp = NULL;           // Monstre courant avec lequel on va apporter les modifications de la vague

    while (newm != NULL) {
        temp = newm;
        // Avance vers le prochain monstre
        newm = newm->suiv;
        // Si le monstre courant n'a plus de points de vie, on le supprime
        if (temp->HP <= 0) {
            // Si le monstre à supprimer est en tête de liste
            if (temp == m->monstres) {
                m->monstres = temp->suiv; // Met à jour la tête de liste
            } else {
                tmp_prec->suiv = temp->suiv; // Met à jour le lien précédent
            }
            free(temp); // Libère la mémoire du monstre supprimé
            m->nb_monstre --;
        } else {
            tmp_prec = temp; // Met à jour le lien précédent pour le prochain tour
        }
    }
}

/* Fonction qui affiche sur la sortie standard la liste de monstre passer en paramètre */
void afficheListe(Monstre *liste) {
    if (liste == NULL) {
        printf("La liste est vide\n");
        return;
    }
    int i = 1;
    Monstre *monstreCourant = liste;
    while (monstreCourant != NULL) {
        printf("(%d) \tType = %d   HP = %d   Vitesse = %d  Position = (%d, %d)\n", i, monstreCourant->type, monstreCourant->HP, monstreCourant->vitesse, monstreCourant->position.x, monstreCourant->position.y);
        monstreCourant = monstreCourant->suiv;
        i++;
    }
}

/* Tire un type de monstre aléatoirement */
int tirageMonstre(int nb_vague) {
    int n;
    // Il n'existe pas de vague à 0 ou moins.
    if(nb_vague < 1) {
        printf("La vague n'existe pas\n");
        return -1;  // On retourne une erreur
    }

    // Les BOSS ne peuvent pas encore être tirés à ce nombre de vague
    if(nb_vague < 5) {
        n = rand() % 90;  // Tirage entre 0 et 89 inclus
        if(n < 50) {
            return 0;  // NORMAL
        } else if(n < 70) {
            return 1; // AGILE
        } else {
            return 2;  // FOULE
        }
    } else {  // Tirage entre 0 et 99 inclus, donc tirage possible des BOSS
        n = rand() % 100;
        if(n < 50) {
            return 0;  // NORMAL
        } else if(n < 70) {
            return 1; // AGILE
        } else if(n < 90) {
            return 2;  // FOULE
        } else {
            return 3;  // BOSS
        }
    }
}

void MonsterMove(VagueMonstre *m) {
    if (m == NULL || m->monstres == NULL) {
        printf("La vague de monstre est NULL ou ne contient aucun monstre\n");
        return;
    }
    while (m != NULL) {
        Monstre *premierMonstre = m->monstres; // On conserve le pointeur au début de la liste de monstres
        for (Monstre *monstreCourant = m->monstres; monstreCourant != NULL; monstreCourant = monstreCourant->suiv) {
            Monstre *debut = premierMonstre;
            Monstre *secondMonstre = monstreCourant;

            while (debut != secondMonstre) {  // Va incrémenter la position du premier monstre de la liste jusqu'au monstre courant
                debut->position.x += 1.0;
                debut = debut->suiv;
            }
        }
        m = m->suiv; // Passer à la vague suivante
    }
}