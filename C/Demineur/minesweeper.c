
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <MLV/MLV_all.h>

#define background_COLOR MLV_COLOR_ROYAL_BLUE
#define gilleline_COLOR MLV_COLOR_WHEAT1
#define souris_COLOR MLV_COLOR_SKY_BLUE
#define chiffre_COLOR MLV_COLOR_BLACK
#define click_COLOR MLV_COLOR_LIGHT_PINK
#define mine_COLOR MLV_COLOR_RED1

#define LARGEUR 800
#define HAUTEUR 800


typedef struct _game {
int width;
int height;
int mines;
int **terrain;
int termine;
} Game;

void init_g(Game *g, int l, int h, int m){
    g->width = l;
    g->height = h;
    g->mines = m;
    g->terrain = malloc(sizeof(int *) * g->height);
    int i, j;
    for (i = 0; i < h; i++)
        g->terrain[i] = malloc(sizeof(int) * l);
    for(i = 0; i < h; i++){
        for(j = 0; j < l; j++) 
            g->terrain[i][j] = 0;
    }
    g->termine = 0;
}
void met_mines(Game *g, int seed){
    int i,x, y;
    for( i = 0; i < g->mines; i++ ){
        x = (unsigned int)rand() * seed % g->width;
        y = (unsigned int)rand() * seed % g->height;
        if(g->terrain[x][y] == 9)
            i--;
        g->terrain[x][y] = 9;
    }
}

int hasmine(Game *g, int i, int j){
    if( i < 0 || j < 0 || i >= g->height || j>= g->width)
        return 0;
    if( g->terrain[i][j] == 9 || g->terrain[i][j] == -9 || g->terrain[i][j] == 10)
        return 1;
    else 
        return 0;
}

int victoire(Game *g){
    int nom_drapeau = 0;
    int decouvert = 0;
    int i, j;
    for(i = 0; i < g->height; i++){
        for ( j = 0; j < g->width; j++)
        {
            if(g->terrain[i][j] == -9)
                nom_drapeau++;
            if((g->terrain[i][j] > 0 && g->terrain[i][j] < 9) || g->terrain[i][j] == -11)
                decouvert++;
        }
    }
    if(nom_drapeau == g->mines || decouvert >= g->height*g->width - g->mines){
        g->termine = 1;
        return 1;
    }
    return 0;
        
}

void Drapeau(Game *g, int x, int y){
    if(g->terrain[x][y] == 9)
        g->terrain[x][y] = -9;
    else if(g->terrain[x][y] == -9)
        g->terrain[x][y] = 9;
    else if(g->terrain[x][y] == -10)
        g->terrain[x][y] = 0;
    else if(g->terrain[x][y] == 0)
        g->terrain[x][y] = -10;
}

int nbmines(Game *g, int i, int j){
    return (hasmine(g, i-1, j-1) + hasmine(g, i-1, j) + hasmine(g, i-1, j+1) + hasmine(g, i, j-1) + hasmine(g, i, j+1) + hasmine(g, i+1, j-1) + hasmine(g, i+1, j) + hasmine(g, i+1, j+1));
}

void pied(Game *g, int x, int y){
    if(g->terrain[x][y] == 9){
        g->terrain[x][y] = 10;
        g->termine = 1;
    }
    if(g->terrain[x][y] == 0){
        g->terrain[x][y] = nbmines(g, x, y);
        if(g->terrain[x][y] == 0){
            g->terrain[x][y] = -11;
            for(int i = x-1; i <= x+1; i++){
                for(int j = y-1; j <= y+1; j++){
                    if(i >= 0 && j >= 0 && i < g->height && j < g->width)
                        pied(g, i, j);
                }
            }
        }
    }
}

void print_terrain(Game *g){
    int i, j;
    for(i = 0; i < g->height; i++){
        for(j = 0; j < g->width; j++){
            printf("%d ", g->terrain[i][j]);
        }
        printf("\n");
    }
    printf("\n");
}

//n : largeur_case, m : hauteur_case
void draw_terrain(Game *g, int n, int m, MLV_Image *flag, MLV_Image *mine){
    int i, j;
    for( i = 0; i < g->height; i++){
        for ( j = 0; j < g->width; j++)
        {
            if(g->terrain[i][j] == 9 || g->terrain[i][j] == 0){
                MLV_draw_text_box( j * n, i * m, n, m, "", 0, gilleline_COLOR, chiffre_COLOR, background_COLOR, MLV_TEXT_CENTER, MLV_HORIZONTAL_CENTER, MLV_VERTICAL_CENTER );
            }// case non decouvert
            if(g->terrain[i][j] == -9 || g->terrain[i][j] == -10){
                MLV_resize_image_with_proportions(flag, n, m);
                MLV_draw_image(flag, j * n, i * m);
            }//case avec drapeau
            if(g->terrain[i][j] > 0 && g->terrain[i][j] < 9){
                char s[5];
                sprintf(s, "%d", g->terrain[i][j]);
                MLV_draw_text_box(j * n, i * m, n, m, s, 0, gilleline_COLOR, chiffre_COLOR, click_COLOR, MLV_TEXT_CENTER, MLV_HORIZONTAL_CENTER, MLV_VERTICAL_CENTER);
            }//case decouvert sans mine mais autour mine
            if(g->terrain[i][j] == -11){
                MLV_draw_text_box(j * n, i * m, m, n, "", 0, gilleline_COLOR, chiffre_COLOR, click_COLOR, MLV_TEXT_CENTER, MLV_HORIZONTAL_CENTER, MLV_VERTICAL_CENTER);

            }//case decouvert sans mine sans autour mine
            if(g->terrain[i][j] == 10 || (g->termine == 1 && g->terrain[i][j] == 9)) {
                MLV_draw_filled_rectangle(j * n, i * m, m, n, mine_COLOR);
                MLV_resize_image_with_proportions(mine, n, m);
                MLV_draw_image(mine, j * n, i * m);
            }//case decouvert avec mine
        }
        
    }

}

void libere_terrain(Game *g){
    for (int i = 0; i < g->height; i++){
        free(g->terrain[i]);
    }
    free(g->terrain);
}

int main(int argc, char *argv[]){        
    Game g;
    int h = 0, l = 0, m = 0;

    srand(time(NULL));
    int seed = rand();

    if(strcmp(argv[1],"-a") == 0){
        seed = atoi(argv[2]);
    }
    if(strcmp(argv[3],"-j") == 0){
        h = atoi(argv[4]);
        l = atoi(argv[5]);
        m = atoi(argv[6]);
        printf("%d %d %d\n", h, l, m);
        if(m >= l*h){
            printf("le jeu signale une erreur! ");
            return -1;
        }
    }
        
        else{
        for(int i = 1; i < argc; i++){  //On commence a 1 pour ne pas prendre en compte le nom du fichier

            char valeur[3];  //Tableau qui va contenir les paramètres du terrain.
            FILE *f_open;
            f_open = fopen(argv[i], "r");
            if(f_open == NULL){  //Si le fichier ne peut pas etre lu on essaye de lire le fichier par defaut mines.ga
                printf("%s : ne peut pas être lu\n", argv[i]);
                f_open = fopen("mines.ga", "r");
                if(f_open == NULL){  //Si l'ouverture de mines.ga est un echec
                    printf("%s : ne peut pas être lu\n", argv[i]);
                    h = 10;  
                    l = 10;
                    m = 10;  //10 valeur par defaut
                }
                else{
                    printf("On ouvre le Fichier par defaut mines.ga\n");
                    int a = 0;  //Un compteur
                    char f;  //Variable qui va nous servir à récupérer les éléments du fichier
                    while(a<3){  //Tant qu'on a pas recuperer les 3 parametres
                        f = fgetc (f_open);
                        if(f != ' '){  //Si ce n'est pas un espace
                            valeur[a] = f;  //On ajoute au tableau le paramètre
                            a++;  //On ajoute 1 au compteur
                        }
                    }
                h = (int)valeur[0] - 48;
                l = (int)valeur[1] - 48;
                m = (int)valeur[2] - 48;
                    break;
                }
            }
            int a = 0;  //Un compteur
            char f;  //Variable qui va nous servir à récupérer les éléments du fichier
            while(a<3){  //Tant qu'on a pas recuperer les 3 parametres
                f = fgetc (f_open);
                if(f != ' '){  //Si ce n'est pas un espace
                    valeur[a] = f;  //On ajoute au tableau le paramètre
                    a++;  //On ajoute 1 au compteur
                }
            h = (int)valeur[0] - 48;
            l = (int)valeur[1] - 48;
            m = (int)valeur[2] - 48;
        }
        }
    }
    if(l <= 0 || h <= 0 || m <= 0) return -1;

    init_g(&g, l, h, m);
    met_mines(&g, seed);
    print_terrain(&g);

    

    int largeur_case = LARGEUR/l;
    int hauteur_case = HAUTEUR/h;

    MLV_create_window( "MINE SWEEPER", "shapes", LARGEUR, HAUTEUR);

    MLV_Image *flag;
    MLV_Image *mine;
    flag = MLV_load_image("flag.png");
    mine = MLV_load_image("mine.png");
    
    MLV_actualise_window();
    MLV_update_window();

    int x, y, v;

    MLV_Mouse_button button;
    MLV_Button_state state;
    while (!g.termine)
    {
        MLV_clear_window(background_COLOR);
        double time_spent = 0.0;
        clock_t begin = clock();
        draw_terrain(&g, largeur_case, hauteur_case, flag, mine);
        clock_t end = clock();
        // calcule le temps écoulé en trouvant la différence (end - begin) et
        // divisant la différence par CLOCKS_PER_SEC pour convertir en secondes
        time_spent += (double)(end - begin) / CLOCKS_PER_SEC;
        

        MLV_actualise_window();

        while(MLV_wait_event(NULL, NULL, NULL, NULL, NULL, &x, &y, &button, &state) != MLV_MOUSE_BUTTON || state != MLV_PRESSED){}
        
        //if (MLV_get_mouse_button_state( MLV_BUTTON_LEFT) == MLV_PRESSED){
        if (button == MLV_BUTTON_LEFT){
            pied(&g, y / hauteur_case,x / largeur_case ); 
            //draw_terrain(&g, largeur_case, hauteur_case);
        }
        //if (MLV_get_mouse_button_state( MLV_BUTTON_RIGHT) == MLV_PRESSED){
        if (button == MLV_BUTTON_RIGHT){
            Drapeau(&g, y / hauteur_case, x / largeur_case);
            //draw_terrain(&g, largeur_case, hauteur_case);
        }
        
        v = victoire(&g);
 
        printf("The elapsed time is %f seconds \n", time_spent);
    }
    draw_terrain(&g, largeur_case, hauteur_case, flag, mine);
    {

        MLV_actualise_window();
        MLV_wait_seconds(2);
        
        MLV_clear_window(MLV_COLOR_WHITE_SMOKE);
        MLV_draw_adapted_text_box(
            LARGEUR/2.5,
            HAUTEUR/4,
            "FELICITATION !",
            0,
            MLV_COLOR_WHITE_SMOKE,
            MLV_COLOR_RED,
            MLV_COLOR_WHITE_SMOKE,
            MLV_TEXT_CENTER
        );
    }
    
    else {
        MLV_actualise_window();
        MLV_wait_seconds(2);  //Laisse le temps au joueur de voir qu'il a clique sur une mine
        
        MLV_clear_window(MLV_COLOR_WHITE_SMOKE);


        MLV_draw_adapted_text_box(
            LARGEUR/2.8,
            HAUTEUR/4,
            "BOOOOOOOOOOOM !",
            0,
            MLV_COLOR_WHITE_SMOKE,
            MLV_COLOR_RED,
            MLV_COLOR_WHITE_SMOKE,
            MLV_TEXT_CENTER
        );
    }

    libere_terrain(&g);
    MLV_update_window();
	MLV_wait_seconds( 10000000 );
	
	//
	// Fermer la fenêtre
	//
	MLV_free_window();

    return 0;
}






