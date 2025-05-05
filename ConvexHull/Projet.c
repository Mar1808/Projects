
#include <MLV/MLV_all.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <math.h>

#define TAILLE 512
#define MARGE 50

/* Structures */

typedef struct
{
    double x;
    double y;
} Point;

typedef struct _vrtx_
{
    Point* s;  /* un point de l'ensemble */
    struct _vrtx_ *prev;  /* le vertex précédent */
    struct _vrtx_ *next;  /* le vertex suivant */
} Vertex, *Polygon;

typedef struct
{
    Polygon pol;  /* le polygône */
    int curlen;  /* la longueur courante */
    int maxlen;  /* la longueur maximale */
} ConvexHull;

/* Fonction */

void menu();

int LenListe(Polygon l){
    Polygon temp = l;
    int len = 0;
    do
    {
        temp = temp->next;
        len++;
    }while(temp!=l);
    return len;
}

void afficheListe(Polygon l){
    if (!l) return; 
    if (! l->next) 
      {printf("[%lf, %lf]", l->s->x, l->s->y);return; }
    printf("[ ... <-> ");
    Polygon temp = l;
    do
    {
        printf("(%lf , %lf) <-> ", temp->s->x, temp->s->y);
        temp = temp->next;
    }while(temp!=l);
    printf("... ]\n");
}

Point* allouerPoint(Point p){
    Point *t;
    t = (Point *)malloc(sizeof(Point));
    if(t!=NULL){
        t->x = p.x;
        t->y = p.y;
        return t;
    }
    else{
        fprintf(stderr,"plus de memoire "); exit(-1);
        }
}

Polygon allouerCellule(Vertex v){
    Polygon l = NULL;
    l = malloc(sizeof(Vertex));
    if(l!=NULL){
        l->s = allouerPoint(*v.s);
        l->next = l;
        l->prev = l;
    }
    else{
        fprintf(stderr,"plus de memoire "); exit(-1);
    }
    return l;
}

void triangleIndirect(Polygon l, Polygon *liste, Vertex v){
    Polygon tmp = NULL;
    tmp = allouerCellule(v);

    if (!l) // Si c'est une liste vide
        { *liste = tmp; return; }
    tmp->next = l->next;  //Le suivant de tmp est le suivant de l
    if (l->next) 
        {tmp->next->prev = tmp;}
    tmp->prev = l;
    l->next = tmp;
} 

Polygon insererEnv(Polygon l, Polygon *liste, Point p, Vertex v){
    Polygon tmp = NULL;
    tmp = allouerCellule(v);
    Polygon q = l;  //Variable qui va nous permettre de parcourir la liste et de placer le nouveau point insérer en tête de liste

    if (!l) //Si c'est une liste vide
        { *liste = tmp; return tmp; }

    do{  //On parcours la liste 
        if(q->s->x == p.x && q->s->y == p.y){  //Si on trouve le Point qui précède le point qu'on va ajouter
            tmp->next = q->next;  //Le suivant de tmp est le suivant de l
            if (q->next) tmp->next->prev = tmp;
            tmp->prev = q;
            q->next = tmp;
            q = q->next;  //On place le point insérer en tête de liste
            return q;
        }
        q = q->next;  //On passe au suivant
    }while(q!=l);  //Tant qu'on a pas fini de parcourir la liste
    return q;
} 

void insererEnTete(Polygon l, Polygon *liste, Vertex v){
    Polygon tmp = NULL;
    tmp = allouerCellule(v);
    if (!l) // Si c'est une liste vide
       { *liste = tmp; return ; }
    tmp->next = l; // Le suivant de tmp = l, tmp passe en tete de liste
    tmp->prev = l->prev;  //Le precedent de tmp est le precedent de l qui etait en tete de liste
    l->prev = tmp ;  //le precedent de l est tmp
    if (tmp->prev == NULL) // Premier element
        { *liste = tmp; return; } // Mise a jour de liste 
    else
    {
    tmp->prev->next = tmp ;  //Le suivant de l est tmp
    }
    
} 

void supprimePoint(Polygon liste, Point p){
    for(; liste; liste=liste->next){
        if(liste->s->x == p.x && liste->s->y == p.y){
            liste->prev->next = liste->next;
            liste->next->prev = liste->prev;
            liste = NULL;
            free(liste);
            return;
        }
    }
    return;
}

Point get_point_on_clic(){
    Point p;
    double PERTURB = 0.0001/RAND_MAX;
    int x,y;
    MLV_wait_mouse(&x,&y);
    p.x = x+(rand()%2?+1.:-1.)*PERTURB*rand();
    p.y = y+(rand()%2?+1.:-1.)*PERTURB*rand();
    return p;
}

int direct(Point A, Point B, Point C){
    if(((B.x - A.x)*(C.y - A.y)) - ((C.x - A.x) * (B.y - A.y)) >= 0)
        return 1;  //Si c'est direct on retourne 1
    return 0;
}

Polygon triangle(Polygon liste, Point *tab){
    Vertex v;
    Point p;

    for(int a=0; a<3; a++)
    {
        p = get_point_on_clic();
        v.s = &p;  //On recupere le point dans un vertex
        if(a == 2){  //Si on a deux points donc un segment
            if(direct(p, *liste->s, *liste->next->s) == 0){  //Si le point est indirect -> à droite du demi plan
                triangleIndirect(liste, &liste, v);
            }
            else {
                insererEnTete(liste, &liste, v);  //Sinon on l'insère en tête de liste
            }
        } 
        else{ 
            insererEnTete(liste, &liste, v);
        }
        tab[a].x = p.x;  //On récupère le point dans la liste de point
        tab[a].y = p.y;
        MLV_draw_filled_circle( p.x, p.y, 3, MLV_COLOR_RED);
        MLV_actualise_window();
    }
    Polygon temp = liste;  //On créer une copie de la liste pour ne pas avoir de problème
    for(int i = 0; i<LenListe(liste); i++){
        MLV_draw_line(temp->s->x, temp->s->y, temp->next->s->x, temp->next->s->y, MLV_COLOR_BLUE_VIOLET);
        temp = temp->next;
    }
    MLV_actualise_window();
    return liste;
}

void nettoyageAvant(ConvexHull *env){
    Polygon temp = env->pol;
    do
    {
        if(direct(*temp->s, *temp->next->s, *temp->next->next->s) == 0){
            supprimePoint(temp->next, *temp->next->s);
            temp = env->pol;
        }
        temp = temp->next;
        
    } while (temp!=env->pol);
}

void nettoyageArriere(ConvexHull *env){
    Polygon temp = env->pol;
    do
    {
        if(direct(*temp->s, *temp->prev->prev->s, *temp->prev->s) == 0){
            supprimePoint(temp->prev, *temp->prev->s);
            temp = env->pol; //On rénitialise le parcours pour ne pas sauter de point
        }
        temp = temp->next;
        
    } while (temp!=env->pol);
}

void affichePoint(int nb, Point *tab, ConvexHull env){
    for(int i = 0; i<nb; i++){
        if(tab[i].x != 0.000000 && tab[i].y != 0.000000)  //Si le point à été initialiser puisqu'on à utiliser un calloc
            MLV_draw_filled_circle(tab[i].x, tab[i].y, 2, MLV_COLOR_BLUE);
    }
    Polygon tmp = env.pol;
    do{
        MLV_draw_filled_circle(tmp->s->x, tmp->s->y, 3, MLV_COLOR_RED);
        tmp = tmp->next;
    }
    while(tmp != env.pol);
}


void afficheEnv(ConvexHull env){
    Polygon temp = env.pol;
    do{
        MLV_draw_line(temp->s->x, temp->s->y, temp->next->s->x, temp->next->s->y, MLV_COLOR_BLUE_VIOLET);
        temp = temp->next;
    }while(temp != env.pol);
}

void drawEnv(ConvexHull env, Point *tab){
    Point p;
    Vertex v;

    Polygon temp = env.pol;
    for(int i = 3; i<env.maxlen; i++){
        p = get_point_on_clic();
        v.s = &p;
        tab[i].x = p.x;  //On récupère le point dans la liste de point
        tab[i].y = p.y;
        do
        {
            if (direct(p, *temp->s, *temp->next->s) == 0){
                env.pol = insererEnv(env.pol, &env.pol, *temp->s, v);
            }

            nettoyageAvant(&env);
            nettoyageArriere(&env);

            MLV_clear_window(MLV_COLOR_GREY);
            afficheEnv(env);  //On affiche l'enveloppe
            affichePoint(env.maxlen, tab, env);  //On affiche tout les points de l'enveloppe ou non
            MLV_actualise_window();
            temp = temp->next;
            
        }while(temp!=env.pol);
        if( MLV_get_keyboard_state( MLV_KEYBOARD_ESCAPE ) == MLV_PRESSED ){ // Pour arrêter le programme, appuyer sur 'échap' et cliquer sur la page avec la souris simultanément
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_draw_text(
                TAILLE/2 - 50, TAILLE/2,
                "Merci à bientôt !!!",
                MLV_COLOR_BLUE
            );
            MLV_wait_seconds(5);
            return;
        }
        if( MLV_get_keyboard_state( MLV_KEYBOARD_r ) == MLV_PRESSED ){ // Pour recommencer le programme, appuyer sur 'r' et cliquer sur la page avec la souris simultanément
            MLV_free_window();
            env.pol = NULL;
            free(tab);
            Point *tab = calloc(env.maxlen, sizeof(Point));
            MLV_create_window("Distribution à la souris", "Distribution à la souris", TAILLE, TAILLE);
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_actualise_window();
            env.pol = triangle(env.pol, tab);  //On dessine le triangle qui est l'enveloppe de départ
            drawEnv(env, tab);
            MLV_actualise_window();
            MLV_wait_seconds(10);
            return;
            } 
        if( MLV_get_keyboard_state( MLV_KEYBOARD_m ) == MLV_PRESSED ){ // Pour accéder au menu, appuyer sur 'm' et cliquer sur la page avec la souris simultanément
            MLV_free_window();
            menu();
            MLV_actualise_window();
            return;
        }
        
    }
    MLV_actualise_window();
}


double randDouble(){
    return ((double)rand()) / (double)RAND_MAX;
}

double randDoubleI(double n, double m){  //Renvoie un intervalle -> [n, m]
    return randDouble() * (m-n) + n;
}


 /* ---------------------------------------------------carre---------------------------------------------------------------- */


Point generateurCarre(float *cmpt, int spirale){
    Point p;
    Point c;  //Le centre 
    c.x = TAILLE/2;
    c.y = TAILLE/2;
    double PERTURB = 0.0001/RAND_MAX;
    *cmpt = *cmpt+0.4;
    int marge = TAILLE/3 - (int)*cmpt;
    if (marge<=0) marge = 5;
    //else int marge = TAILLE/3 - (int)*cmpt;
     
    if(spirale == 1){
        p.x = marge + rand()%((int)c.x + TAILLE/2 - 2*marge);
        p.y = marge + rand()%((int)c.x + TAILLE/2 - 2*marge);
    }
    
    else{

        p.x = MARGE+rand()%(TAILLE-2*MARGE);
        p.y = MARGE+rand()%(TAILLE-2*MARGE);
    }

    p.x = p.x+(rand()%2?+1.:-1.)*PERTURB*rand();
    p.y = p.y+(rand()%2?+1.:-1.)*PERTURB*rand();
    
    return p;
}

Polygon carre(Polygon liste, Point *tab, float *cmpt, int spirale){
    Vertex v;
    Point p;

    for(int i=0; i<3; i++){
        p = generateurCarre(cmpt, spirale);
        v.s = &p;
        tab[i].x = p.x;
        tab[i].y = p.y;
        if(i == 2){  //Si on a deux points donc un segment
            if(direct(p, *liste->s, *liste->next->s) == 0){  //Si le point est indirect -> à droite du demi plan
                triangleIndirect(liste, &liste, v);
            }
            else {
                insererEnTete(liste, &liste, v);  //Sinon on l'insère en tête de liste
            }
        }
        else{
            insererEnTete(liste, &liste, v);
        }
        
        MLV_draw_filled_circle( tab[i].x, tab[i].y, 3, MLV_COLOR_RED);
        MLV_actualise_window();
    }
    Polygon temp = liste;  //On créer une copie de la liste pour ne pas avoir de problème
    for(int i = 0; i<=LenListe(liste); i++){
        MLV_draw_line(temp->s->x, temp->s->y, temp->next->s->x, temp->next->s->y, MLV_COLOR_BLUE_VIOLET);
        temp = temp->next;
    }
    MLV_actualise_window();
    return liste;
}


void NuagePointCarre(ConvexHull env, int nb, int seed, Point *tab, float *cmpt, int actualise, int spirale){
    Point p;
    Vertex v;
    Polygon temp = env.pol;
    for(int i=3; i<nb; i++){
        p = generateurCarre(cmpt, spirale);
        v.s = &p;
        tab[i].x = p.x;
        tab[i].y = p.y;
        if( MLV_get_keyboard_state( MLV_KEYBOARD_ESCAPE ) == MLV_PRESSED ){ // Pour arrêter le programme, appuyer sur 'échap'
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_draw_text(
                TAILLE/2 - 50, TAILLE/2,
                "Merci à bientôt !!!",
                MLV_COLOR_BLUE
            );
            MLV_wait_seconds(5);
            return;
        }
        if( MLV_get_keyboard_state( MLV_KEYBOARD_r ) == MLV_PRESSED ){ // Pour recommencer le programme, appuyer sur 'r'
            free(tab);
            Point *tab = calloc(nb, sizeof(Point));
            env.pol = NULL;
            float cmp=0;
            MLV_free_window();
            if(spirale == 1)
                MLV_create_window("Carre Pseudo Spirale", "Carre Pseudo Spirale", TAILLE, TAILLE);
            else
                MLV_create_window("Carre", "Carre", TAILLE, TAILLE);
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_actualise_window();
            env.pol = carre(env.pol, tab, cmpt, 0);
            NuagePointCarre(env, nb, seed, tab, &cmp, actualise, spirale);
            return;
        } 
        if( MLV_get_keyboard_state( MLV_KEYBOARD_m ) == MLV_PRESSED ){ // Pour accéder au menu, appuyer sur 'm'
            MLV_free_window();
            menu();
            return;
        }
        do
        {      
            
            if (direct(p, *temp->s, *temp->next->s) == 0){
                env.pol = insererEnv(env.pol, &env.pol, *temp->s, v);
            }
            nettoyageAvant(&env);
            nettoyageArriere(&env);

            MLV_clear_window(MLV_COLOR_GREY);
            if(actualise == 1){
                afficheEnv(env);  //On affiche l'enveloppe
                affichePoint(nb, tab, env);  //On affiche tout les points de l'enveloppe ou non
            }
            MLV_actualise_window();
            temp = temp->next;
        
        }while(temp!=env.pol);
        if(actualise == 0 && i == nb-1){
            afficheEnv(env);  //On affiche l'enveloppe
            affichePoint(nb, tab, env);  //On affiche tout les points de l'enveloppe ou non
        }
            
        MLV_actualise_window();
    }

}
 /* ---------------------------------------------------cercle---------------------------------------------------------------- */

Point generateurCercle(float *cmpt, int spirale){


    Point c;  //Le centre 
    Point p;
    c.x = TAILLE/2;
    c.y = TAILLE/2;
    double pi = 3.14159265358979323846;
    float r = 0;

/*---------------------------- SI Spirale ------------------------------*/  
    if (spirale == 1){
        r = 0;
        if(*cmpt<TAILLE/2){  //On ne sort pas de la fenêtre
            *cmpt = *cmpt + 1.0;
            r = r + *cmpt;
        }
        else
            r = *cmpt;
    }
/*-------------------------------SINON-----------------------------------*/  
    else {r = (TAILLE-2*MARGE)/2;}  //Rayon du cercle}

    double o = ((double) rand() * 0.0001)* pi ;
    p.x = c.x + randDoubleI(0, (r * cos(r * o)));  //Intervalle qui correspond au rayon
    p.y = c.y + randDoubleI(0, (r * sin(r * o)));
    
    return p;
}


Polygon cercle(Polygon liste, Point *tab, float *cmpt, int spirale){
    Vertex v;
    Point p;

    for(int i=0; i<3; i++){
        p = generateurCercle(cmpt, spirale);
        v.s = &p;
        tab[i].x = p.x;
        tab[i].y = p.y;
        if(i == 2){  //Si on a deux points donc un segment
            if(direct(p, *liste->s, *liste->next->s) == 0){  //Si le point est indirect -> à droite du demi plan
                triangleIndirect(liste, &liste, v);
            }
            else {
                insererEnTete(liste, &liste, v);  //Sinon on l'insère en tête de liste
            }
        }
        else{
            insererEnTete(liste, &liste, v);
        }
        
        MLV_draw_filled_circle( tab[i].x, tab[i].y, 3, MLV_COLOR_RED);
        MLV_actualise_window();
    }
    Polygon temp = liste;  //On créer une copie de la liste pour ne pas avoir de problème
    for(int i = 0; i<=LenListe(liste); i++){
        MLV_draw_line(temp->s->x, temp->s->y, temp->next->s->x, temp->next->s->y, MLV_COLOR_BLUE_VIOLET);
        temp = temp->next;
    }
    MLV_actualise_window();
    return liste;
}


void NuagePointCercle(ConvexHull env, int nb, int seed, Point *tab, float *cmpt, int actualise, int spirale){
    Point p;
    Vertex v;
    Polygon temp = env.pol;

    for(int i=3; i<nb; i++){
        p = generateurCercle(cmpt, spirale);
        v.s = &p;
        tab[i].x = p.x;
        tab[i].y = p.y;
        if( MLV_get_keyboard_state( MLV_KEYBOARD_ESCAPE ) == MLV_PRESSED ){ // Pour arrêter le programme, appuyer sur 'échap'
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_draw_text(
                TAILLE/2 - 50, TAILLE/2,
                "Merci à bientôt !!!",
                MLV_COLOR_BLUE
            );
            MLV_wait_seconds(5);
            return;
        }
        if( MLV_get_keyboard_state( MLV_KEYBOARD_r ) == MLV_PRESSED ){ // Pour recommencer le programme, appuyer sur 'r'
            free(tab);
            Point *tab = calloc(nb, sizeof(Point));
            env.pol = NULL;
            float cmp = 0;
            MLV_free_window();
            if(spirale == 1)
                MLV_create_window("Cercle Pseudo Spirale", "Cercle Pseudo Spirale", TAILLE, TAILLE);
            else
                MLV_create_window("Cercle", "Cercle", TAILLE, TAILLE);
            MLV_clear_window(MLV_COLOR_GREY);
            MLV_actualise_window();
            env.pol = cercle(env.pol, tab, cmpt, 0);
            NuagePointCercle(env, nb, seed, tab, &cmp, actualise, spirale);
            return;
        }
        if( MLV_get_keyboard_state( MLV_KEYBOARD_m ) == MLV_PRESSED ){ // Pour accéder au menu, appuyer sur menu
            MLV_free_window();
            menu();
            return;
        }
        do
        {
            if (direct(p, *temp->s, *temp->next->s) == 0){
                env.pol = insererEnv(env.pol, &env.pol, *temp->s, v);
            }
            nettoyageAvant(&env);
            nettoyageArriere(&env);

            MLV_clear_window(MLV_COLOR_GREY);
            if(actualise == 1){
                afficheEnv(env);  //On affiche l'enveloppe
                affichePoint(nb, tab, env);  //On affiche tout les points de l'enveloppe ou non
            }
            MLV_actualise_window();
            temp = temp->next;
        
        }while(temp!=env.pol);
        if(actualise == 0 && i ==nb-1){
            afficheEnv(env);  //On affiche l'enveloppe
            affichePoint(nb, tab, env);  //On affiche tout les points de l'enveloppe ou non
        }
    }

}


/*---------------------------------------------------------------------------------------------------------------------------------*/


void menu(){
    char* affichage;
    char* distribution;
    char* nombre;
    char* seed;
    ConvexHull env;
    env.pol = NULL;
    float cmpt = 0;

    MLV_create_window("Choix du programme", "", TAILLE, TAILLE);
    MLV_clear_window( MLV_COLOR_GREY);
    
    MLV_draw_text(
        10, 400,
        "Une fois lancé vous pouvez revenir au menu avec : m",
        MLV_COLOR_BLUE
    );

    MLV_draw_text(
        10, 440,
        "Vous pouvez quitter avec : echap",
        MLV_COLOR_BLUE
    );

    MLV_draw_text(
        10, 420,
        "Pour recommencer l'enveloppe : r",
        MLV_COLOR_BLUE
    );

    MLV_draw_text(
        10, 10,
        "Saisissez le mode de distribution : ",
        MLV_COLOR_BLACK
    );
    MLV_draw_adapted_text_box(40, 30,"_A la souris (Tapez 'S')\n_Aléatoire controlée:\n     Carré (Tapez 'carre')\n     Cercle (Tapez 'cercle)\n     Carré pseudo-spirale (Tapez 'carre ps')\n     Cercle pseudo-spirale (Tapez 'cercle ps')", 9,MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY, MLV_TEXT_LEFT);

    
    MLV_draw_text(
        10, 190,
        "Veuillez saisir le mode d'affichage parmi les suivants :",
        MLV_COLOR_BLACK
    );
    MLV_draw_adapted_text_box(40, 210,"_ Affichage Dynamique(Tapez 'D')\n_ Affichage Terminal(Tapez 'T')", 9,MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY, MLV_TEXT_CENTER);
   
    MLV_wait_input_box( // on récupère le mode distribution souhaité
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Mode de distribution : ",
        &distribution
    );

    while(strcmp(distribution, "S") !=0 && strcmp(distribution, "carre") !=0 && strcmp(distribution, "cercle") !=0 
    && strcmp(distribution, "carre ps") !=0 && strcmp(distribution, "cercle ps") !=0){ 
        // on fait la saisis controlée du mode de distribution
        MLV_wait_input_box(
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Cette distribution n'existe pas , Mode de distribution : ",
        &distribution
        );
    }

    MLV_wait_input_box( // on récupère le nombre de points saisis
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Nombre de points : ",
        &nombre
    );

    while(atoi(nombre) <3){ // on vérifie que le nombre de points saisis est supèrieur à 3
        MLV_wait_input_box(
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Saisissez un nombre de points supérieur à 3: ",
        &nombre
        );
    }

    Point *tab = calloc(atoi(nombre), sizeof(Point));
    env.maxlen = atoi(nombre);        

    if (strstr(distribution, "S")){ // on lance la distribution à la souris
        MLV_free_window();
        MLV_create_window("Distribution à la souris", "Distribution à la souris", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        env.pol = triangle(env.pol, tab);  //On dessine le triangle qui est l'enveloppe de départ
        drawEnv(env, tab);
        MLV_actualise_window();
        MLV_wait_seconds(10);
        return;
    }
    
    MLV_actualise_window();

    MLV_wait_input_box( // on récupère le mode d'affichage
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Mode d'affichage : ",
        &affichage
    );

    while(strcmp(affichage, "D") != 0 && strcmp(affichage, "T") != 0){ // on vérifie que l'affichage est soit dynamique ou soit terminal
        MLV_wait_input_box(
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Cet affichage n'existe pas, Mode d'affichage : ",
        &affichage
    );
    }

    MLV_wait_input_box( // on récupère la graine
        10,280,
        450,90,
        MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY,
        "Graine (Saisissez 42 par défaut) : ",
        &seed
    );
    
    if (strcmp(distribution, "carre") == 0 && strstr(affichage, "D")){ // on lance le programme carre avec affichage dynamique
        MLV_free_window();
        MLV_create_window("Carre", "Carre", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = carre(env.pol, tab, &cmpt, 0);
        NuagePointCarre(env, atoi(nombre), atoi(seed), tab, &cmpt, 1, 0);
        MLV_actualise_window();
    }

    if(strcmp(distribution, "cercle") == 0 && strstr(affichage, "D")){ // on lance le programme cercle avec affichage dynamique
        float cmpt = 0;
        MLV_free_window();
        MLV_create_window("Cercle", "Cercle", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = cercle(env.pol, tab, &cmpt, 0);
        NuagePointCercle(env, atoi(nombre), atoi(seed), tab, &cmpt, 1, 0);
        MLV_actualise_window();
    }

    if(strcmp(distribution, "carre ps") == 0 && strstr(affichage, "D")){ // on lance le programme carre pseudo spirale avec affichage dynamique
        MLV_free_window();
        MLV_create_window("Carre Pseudo Spirale", "Carre Pseudo Spirale", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = carre(env.pol, tab, &cmpt, 1);
        NuagePointCarre(env, atoi(nombre), atoi(seed), tab, &cmpt, 1, 1);
        MLV_actualise_window();
    }

    if(strcmp(distribution, "cercle ps") == 0 && strstr(affichage, "D")){ // on lance le programme cercle pseudo spirale avec affichage dynamique
        float cmpt = 0;
        MLV_free_window();
        MLV_create_window("Cercle Pseudo Spirale", "Cercle Pseudo Spirale", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = cercle(env.pol, tab, &cmpt, 1);
        NuagePointCercle(env, atoi(nombre), atoi(seed), tab, &cmpt, 1, 1);
        MLV_actualise_window();
    }

    if (strcmp(distribution, "carre") == 0 && strstr(affichage, "T")){ // on lance le programme carre avec affichage terminal
        MLV_free_window();
        MLV_create_window("Carre", "Carre", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = carre(env.pol, tab, &cmpt, 0);
        NuagePointCarre(env, atoi(nombre), atoi(seed), tab,&cmpt, 0, 0);
        MLV_actualise_window();
    }
    if (strcmp(distribution, "cercle") == 0 && strstr(affichage, "T")){ // on lance le programme cercle avec affichage terminal
        float cmpt = 0;
        MLV_free_window();
        MLV_create_window("Cercle", "Cercle", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = cercle(env.pol, tab, &cmpt, 0);
        NuagePointCercle(env, atoi(nombre), atoi(seed), tab, &cmpt, 0, 0);
        MLV_actualise_window();
    }
    if (strcmp(distribution, "carre ps") == 0 && strstr(affichage, "T")){ // on lance le programme carre pseudo spirale avec affichage terminal
        MLV_free_window();
        MLV_create_window("Carre Pseudo Spirale", "Carre Pseudo Spirale", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = carre(env.pol, tab, &cmpt, 1);
        NuagePointCarre(env, atoi(nombre), atoi(seed), tab, &cmpt, 0, 1);
        MLV_actualise_window();
    }
    if (strcmp(distribution, "cercle ps") == 0 && strstr(affichage, "T")){ // on lance le programme cercle pseudo spirale avec affichage terminal
        float cmpt = 0;
        MLV_free_window();
        MLV_create_window("Cercle Pseudo Spirale", "Cercle Pseudo Spirale", TAILLE, TAILLE);
        MLV_clear_window( MLV_COLOR_GREY);
        srand(time(NULL));
        env.pol = cercle(env.pol, tab, &cmpt, 1);
        NuagePointCercle(env, atoi(nombre), atoi(seed), tab, &cmpt, 0, 1);
        MLV_actualise_window();
    }
    free( affichage );
    free( distribution );
    MLV_wait_seconds( 5 );
    return ;
}

void Option(int argc, char *argv[]){
    float cmpt = 0;
    int seed;
    int nombre;
    srand(time(NULL));
    if(argc == 1){ // si il n'y pas d'argumens dans le terminal on lance le menu
        menu();
        return;
    }
    ConvexHull env;
    env.pol = NULL;

    if(strcmp(argv[2],"-n") == 0){ // on récupère le nombre de points souhaité
        nombre = atoi(argv[3]);
    }


    if(strcmp(argv[1],"-souris") == 0){ // on lance le programme distribution à la souris
        Point *tab = calloc(nombre, sizeof(Point));
        env.maxlen = nombre;
        MLV_create_window("Distribution à la souris", "Distribution à la souris", TAILLE, TAILLE);
        MLV_clear_window(MLV_COLOR_GREY);
        MLV_draw_adapted_text_box( 100, 10, "Appuyer sur 'Echap' pour QUITTER,\nAppuyer sur 'r' pour relancer le programme\nAppuyer sur 'm' pour accéder au menu", 5,MLV_COLOR_BLUE, MLV_COLOR_BLACK, MLV_COLOR_GREY, MLV_TEXT_CENTER );
        MLV_actualise_window();
        env.pol = triangle(env.pol, tab);  //On dessine le triangle qui est l'enveloppe de départ
        drawEnv(env, tab);
        MLV_actualise_window();
        MLV_wait_seconds(3);
    }

    if(strcmp(argv[5],"-a") == 0){ // on récupère la graine
        seed = atoi(argv[6]);
    }

    if(strcmp(argv[1],"-carre") == 0){
        Point *tab = calloc(nombre, sizeof(Point));
        env.maxlen = nombre;
        if(strcmp(argv[4], "-D") == 0){ // on lance le programme carre avec affichage dynamique
            MLV_create_window("Carre", "Carre", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = carre(env.pol, tab, &cmpt, 0);
            NuagePointCarre(env, nombre, seed, tab, &cmpt, 1, 0);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        }
        if(strcmp(argv[4], "-T") == 0){ // on lance le programme carre avec affichage terminal
            MLV_create_window("Carre", "Carre", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = carre(env.pol, tab, &cmpt, 0);
            NuagePointCarre(env, nombre, atoi(argv[4]), tab, &cmpt, 0, 0);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        } 
    }
    if(strcmp(argv[1],"-cercle") == 0){
        Point *tab = calloc(nombre, sizeof(Point));
        env.maxlen = nombre;
        if(strcmp(argv[4], "-D") == 0){ // on lance le programme cercle avec affichage dynamique
            float cmpt = 0;
            MLV_create_window("Cercle", "Cercle", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = cercle(env.pol, tab, &cmpt, 0);
            NuagePointCercle(env, nombre, atoi(argv[4]), tab, &cmpt, 1, 0);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        }
        if(strcmp(argv[4], "-T") == 0){ // on lance le programme cercle avec affichage terminal
            float cmpt = 0;
            MLV_create_window("Cercle", "Cercle", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = cercle(env.pol, tab, &cmpt, 0);
            NuagePointCercle(env, nombre, atoi(argv[4]), tab, &cmpt, 0, 0);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        } 
    }
    if(strcmp(argv[1],"-carreps") == 0){
        Point *tab = calloc(nombre, sizeof(Point));
        env.maxlen = nombre;
        if(strcmp(argv[4], "-D") == 0){ // On lance le programme carre pseudo spirale avec affichage dynamique
            MLV_create_window("Carre Pseudo Spirale", "Carre Pseudo Spirale", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = carre(env.pol, tab, &cmpt, 1);
            NuagePointCarre(env, nombre, atoi(argv[4]), tab, &cmpt, 1, 1);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        }
        if(strcmp(argv[4], "-T") == 0){ // On lance le programme carre pseudo spirale avec affichage terminal
            MLV_create_window("Carre Pseudo Spirale", "Carre Pseudo Spirale", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = carre(env.pol, tab, &cmpt, 1);
            NuagePointCarre(env, nombre, atoi(argv[4]), tab, &cmpt, 0, 1);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        } 
    }
    if(strcmp(argv[1],"-cercleps") == 0){
        Point *tab = calloc(nombre, sizeof(Point));
        env.maxlen = nombre;
        if(strcmp(argv[4], "-D") == 0){ // On lance le programme cercle pseudo spirale avec affichage dynamique
            float cmpt = 0;
            MLV_create_window("Cercle Pseudo Spirale", "Cercle Pseudo Spirale", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = cercle(env.pol, tab, &cmpt, 1);
            NuagePointCercle(env, nombre, atoi(argv[4]), tab, &cmpt, 1, 1);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        }
        if(strcmp(argv[4], "-T") == 0){ // On lance le programme cercle pseudo spirale avec affichage Terminal
            float cmpt = 0;
            MLV_create_window("Cercle Pseudo Spirale", "Cercle Pseudo Spirale", TAILLE, TAILLE);
            srand(time(NULL));
            env.pol = cercle(env.pol, tab, &cmpt, 1);
            NuagePointCercle(env, nombre, atoi(argv[4]), tab, &cmpt, 0, 1);
            MLV_actualise_window();
            MLV_wait_seconds(3);
        } 
    }
    
}


int main(int argc, char *argv[])
{
    Option(argc, argv);

    return 0;
}
