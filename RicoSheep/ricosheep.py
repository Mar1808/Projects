from fltk import*


lst_verif = ['0','1','2','3','4']  #Liste qui permet de vérifier la saisie des plateaux

direction = ['Left','Right','Up','Down']

l = ['map1.txt','map2.txt', 'map3.txt','test_move.txt','wide1.txt']

lst_img = ['grass.png', 'sheep.png', 'bush.png', 'sheep_grass.png']



def jouer(plateau, moutons, direction):
    '''Met à jour la position des moutons selon la direction choisis.
    :param plateau: liste de listes
    :param moutons: liste de tuples
    :param direction: liste de str
    :param value: liste de tuples

    >>> jouer([['None','B','None'],['G','None','G'],['B','None','None']], [(0,0),(0,2),(2,1)],'Right')
    [(0,0),(0,2),(2,2)]
    >>> jouer([['None','None','None'],['G','None','B'],['B','G','None']], [(1,1),(0,2),(2,2)],'Up')
    [(0,1),(0,2),(2,2)]
    >>> jouer([['None','B','None'],['G','None','G'],['None','None','None']], [(0,0),(0,2),(2,1)],'Down')
    [(2,0),(2,2),(2,1)]
    '''
    l = []

    moutons.sort() #On trie dans l'odre croissant

    if direction == 'Down':
        moutons.reverse()  #Si la direction est 'Down' il va falloir trier dans le sens inverse puisque si on trie dans l'orde croissant la plus petite coordonnée sera confronté à un autre mouton.
    
    if direction == 'Right':  #Meme chose que pour 'Down
        moutons.reverse()

    for m in moutons:
        a,b = m
        if direction == 'Left':
            while b-1 >= 0: #Si il y a au moins une case à gauche.
                if plateau[a][b-1] != 'B':  #Tant que la case de gauche est vide ou s'il y a de l'herbe et s'il n'y a pas de mouton
                    if plateau[a][b-1] != 'S' and plateau[a][b-1] != 'GS':
                        b -= 1
                        m = a,b

                        if plateau[a][b+1] == 'GS':  #Si l'ancienne case était le mouton dans l'herbe
                            plateau[a][b+1] = 'G'  #La case redevient juste une herbe
                        else:
                            plateau[a][b+1] = 'None'  #La case où était le mouton devient None

                        if plateau[a][b] == 'G':  #La nouvelle case devient 'S'
                            plateau[a][b] = 'GS'  #La nouvelle case devient 'GS'
                        else:
                            plateau[a][b] = 'S'  #La nouvelle case devient 'S'

                    else:
                        break  #On sort de la boucle
                else:
                    break
            l.append(m)

        if direction == 'Right':
            while b+1 < len(plateau[0]):  #Si il y a au moins une case à droite
                if plateau[a][b+1] != 'B':   #Tant que la case de droite est vide ou s'il y a de l'herbe et s'il n'y a pas de mouton
                    if plateau[a][b+1] != 'S' and plateau[a][b+1] != 'GS':
                        b += 1
                        m = a,b
                        
                        if plateau[a][b-1] == 'GS':
                            plateau[a][b-1] = 'G'
                        else:    
                            plateau[a][b-1] = 'None'

                        if plateau[a][b] == 'G':  #La nouvelle case devient 'S'
                            plateau[a][b] = 'GS'  #La nouvelle case devient 'GS'
                        else:
                            plateau[a][b] = 'S'  #La nouvelle case devient 'S'

                    else:
                        break
                else:
                    break
            l.append(m)

        if direction == 'Down':
            while a+1 < len(plateau):  #Si il y a au moins une case en bas  
                if plateau[a+1][b] != 'B':  #Tant que la case du dessous est vide ou s'il y a de l'herbe ou s'il n'y a pas de mouton.
                    if plateau[a+1][b] != 'S' and plateau[a+1][b] != 'GS':
                        a += 1
                        m = a,b

                        if plateau[a-1][b] == "GS":
                            plateau[a-1][b] = "G"
                        else:
                            plateau[a-1][b] = "None"

                        if plateau[a][b] == 'G':  #La nouvelle case devient 'S'
                            plateau[a][b] = 'GS'  #La nouvelle case devient 'GS'
                        else:
                            plateau[a][b] = 'S'  #La nouvelle case devient 'S'

                    else:
                        break
                else:
                    break
            l.append(m)

        if direction == 'Up':
            while a-1 >= 0:  #Si il y a au moins une case au dessus
                if plateau[a-1][b] != 'B':   #Tant que la case du haut est vide ou s'il y a de l'herbe ou s'il n'y a pas de mouton.
                    if plateau[a-1][b] != 'S' and plateau[a-1][b] != 'GS':
                        a -= 1
                        m = a,b

                        if plateau[a+1][b] == 'GS':
                            plateau[a+1][b] = 'G'
                        else:
                            plateau[a+1][b] = 'None'

                        if plateau[a][b] == 'G':  #La nouvelle case devient 'S'
                            plateau[a][b] = 'GS'  #La nouvelle case devient 'GS'
                        else:
                            plateau[a][b] = 'S'  #La nouvelle case devient 'S'

                    else:
                        break
                else:
                    break
            l.append(m)
    return l



def victoire(plateau,moutons,herbe):
    '''Indique si le joueur a gagné la partie entre outre si toutes les herbes sont occupées par un mouton
    :param plateau: liste de listes
    :param moutons: liste de tuples
    :param herbe: int
    :param value: bool

    >>> victoire ([['GS','GS','None']], [(0,0),(0,1)], 2)
    True
    >>> victoire ([['GS','G','None']], [(0,0),(0,2)], 2)
    None
    '''
    mouton = 0
    
    for m in (moutons):  # On parcours les moutons
        if plateau[m[0]][m[1]] == 'GS':  #Si le mouton est dans l'herbe
            mouton += 1
            if mouton == herbe:
                return True



def charger(fichier):
    '''
    Lis les fichiers en vérifiant leur validité et les transformes en liste de liste
    :param fichier: str
    :return value: list

    >>> charger('wide1.txt')
    [['G', 'B', None, None, None], 
    [None, 'B', 'B', None, 'S'], 
    [None, None, 'S', 'B', None], 
    [None, None, 'G', 'S', None], 
    ['S', None, None, None, 'B']]

    >>> charger('map1.txt')
    [[None, None, None, 'G', None], 
    [None, None, None, None, 'B'], 
    [None, None, None, None, None], 
    ['B', 'B', None, None, None], 
    ['S', 'S', None, None, None], 
    ['B', 'B', None, None, 'S']]
    '''
    doc = open(fichier,'r') 
    lecture = doc.readlines()
    contenu = []
    for l in lecture:
        ajt = []
        for i in range(len(l)):
            if l[i] != "\n":
                if  l[i]!= "_" and l[i] != "G" and l[i] != 'B'and l[i] != 'S':
                    return None
                else :
                    ajt.append(l[i])
        contenu.append(ajt)

    for l in contenu:
        for i in range(len(l)):
            if l[i] == '_':
                l[i] = None

    return contenu
    fichier.close()


def moutons(fichier):
    ''' Extrait les coordonnées des moutons et les ajoutes à une liste sous forme de tuples
    :param fichier: str
    :return value: list
    >>> moutons('wide1.txt')
        [(0,4),(1,4)]
    >>> moutons('wide2.txt')
        [(0,4),(1,3),(2,4),(4,4)]
    >>> moutons('teste_move.txt')
        [(0,2),(1,1),(1,3),(2,2),(2,3)]
    '''
    lst_moutons = []
    a = charger(fichier)
    for i in range(len(a)):
        for j in range(len(a[i])):
            if a[i][j] == 'S':
                lst_moutons.append((i,j))
    return lst_moutons
  


def carre(x, y, cote):  #Créer un carré
    rectangle(x, y, x + cote, y + cote, "black", epaisseur = 3, tag='carre')



def fltk_plateau(plateau):
    '''Créer un plateau
    :param plateau: liste de listes
    '''
    cmpt = 0  #Un compteur
    x = 130  #abcsicce
    y = 50  #ordonnée
    for i in plateau:  #On parcours le nombre de liste dans la liste plateau, donc le nombre de ligne
        for j in i:  #On parcours le nombre d'éléments dans la liste i, donc le nombre de colonne
            if cmpt == len(i):  #Si la longueur de la liste i est atteinte
                y += 100  #On redescend juste en dessous du premier carré
                carre(x, y, 100)
                cmpt = 0  #On rénitialise le compteur à 0
            else:
                carre(x + 100 *cmpt, y, 100)  #On créer un carré où cmpt correspond au num de la colonne
            cmpt += 1



def éléments(fichier):
    '''Crée une liste contenant des coordonnes des éléments fixes du plateau
    :param: str
    :return value: list
    >>> élément('map2.txt')
        [(0, 1), (0, 3), (1, 0), (1, 1), (2, 1), (2, 2), (2, 3), (3, 1), (3, 2), (4, 3)]
    >>> élément('map1.txt')
        [(0, 3), (3, 0), (3, 1)]
    >>> élément('wide1.txt')
        [(0, 0), (0, 1), (1, 2), (1, 6), (1, 8), (2, 0), (2, 3), (2, 5), (2, 6), (2, 7), (3, 0), (3, 1), (3, 5)]
    '''
    lst_elem = []
    a = charger(fichier)
    for i in range(len(a)):
        for j in range(len(a[i])):
            if a[i][j] == 'B' or a[i][j] == 'G' or a[i][j] == 'GS':
                lst_elem.append((i,j))
    return lst_elem
    


def afficher_elmt(coordonnees, plateau):
    '''Affiche les éléments fixes buisson, herbe selon leurs coordonnées.
    :param coordonnes: liste de tuples
    :param plateau: liste de listes
    '''
    for elem in coordonnees:
        x,y = elem
        if plateau[x][y] == 'B':
            image(y*100 + 50 + 130, x*100 + 25 + 80, 'bush.png')  # y + 130 et x + 80 puisque le plateau commence à ces valeurs là.
        if plateau[x][y] == 'G':
            image(y*100 + 50 + 130, x*100 + 25 + 80, 'grass.png')  # +50 et +25 pour les affciher au milieu de la case
        if plateau[x][y] == 'GS':
            image(y*100 + 50 + 130, x*100 + 25 + 80, 'sheep_grass.png')  # *100 puisque les coordonnées sont exprimées en unités et non en centaine comme le plateau



def afficher_moutons(mouton, plateau):
    '''Affiche les moutons dans le plateau selon leurs coordonnées.
    :param mouton: liste de tuples
    :param plateau: liste de listes
    '''
    for i in mouton:
        x, y = i
        if plateau[x][y] == 'GS':
            image(y*100 + 50 + 130, x*100 + 25 + 80,'sheep_grass.png', tag = 'mouton')
        else:
            image(y*100 + 50 + 130, x*100 + 25 + 80,'sheep.png', tag = 'mouton')



#################################################         Interface graphique          ########################################################

cree_fenetre(800,800)

#Page de bienvenue:

texte(115,250, "Bienvenue dans RicoSheep", couleur= 'green', police= "Comic Sans MS",taille=35)
image(60,270,'sheep_grass.png')
image(750,270,'sheep_grass.png')
texte(160,350, '[Clique gauche pour accéder au menu]', couleur= 'red', police= "Comic Sans MS", taille=20)

attend_clic_gauche()
efface_tout()

def Play():  #On utilise une fonction pour que le joueur à la fin puisse revenir au Menu selon sa sélection

    #Pour le menu:

    texte(350,40, 'Menu', couleur= 'red', police= "Comic Sans MS")
    texte(150,100, 'Choisissez le plateau qui vous convient :', couleur= 'blue', police= 'Comic Sans MS', taille= 20)
    image(150,250,"plateau0.png",ancrage='center')
    texte(150,350,"0",police= "Comic Sans MS",taille=15)
    image(550,250,"plateau1.png",ancrage='center')
    texte(555,350,"1",police= "Comic Sans MS",taille=15)
    image(350,400,"plateau3.png",ancrage='center')
    texte(150,600,"2",police= "Comic Sans MS",taille=15)
    image(150,500,"plateau2.png",ancrage='center')
    texte(350,470,'3',police= "Comic Sans MS",taille=15)
    image(550,500,"plateau4.png",ancrage='center')
    texte(550,600,'4',police= "Comic Sans MS",taille=15)

    ev = attend_ev()
    tev = type_ev(ev)

    if tev == 'Touche':

        while str(touche(ev)) not in lst_verif:  #Si la touche choisis n'est pas dans liste_plateau
            texte(190,600, '! Il faut sélectionner un plateau qui existe !', couleur = 'red',police= 'Comic sans MS', taille = 13, tag = 'erreur')
            mise_a_jour()
            ev = attend_ev()  #Redemande de sélectionner le plateau à choisir

        n = int(touche(ev))

        mise_a_jour()
        efface_tout()

    #Mise en place du jeu:

    plateau = charger(l[n])  #Appelle la fonction et créer le plateau en fonction du plateau choisis

    mouton = moutons(l[n])  #coordonnées des moutons
    coordonnees = éléments(l[n])  #coordonnées des éléments fixe

    fltk_plateau(plateau)  #Créer le plateau

    afficher_moutons(mouton, plateau)  #Affiche les moutons dans le plateau

    afficher_elmt(coordonnees, plateau)  #Affiche les autres éléments dans le plateau

    mise_a_jour()


    herbe = 0

    for g in plateau:
        for h in g:
            if h == 'G':  #On compte le nombre d'herbe dans le plateau avant de commencer la partie
                herbe += 1


    #Début du jeu:

    while victoire(plateau, mouton, herbe) != True:  #Tant que le joueur n'a pas gagné.
        ev = attend_ev()
        tev = type_ev(ev)

        if tev == 'Touche':  # on indique la touche pressée

            if touche(ev) == 'Escape':
                efface_tout()
                texte(350,40, 'Menu', couleur= 'red', police= "Comic Sans MS")
                texte(150,100, 'Choisissez le plateau qui vous convient :', couleur= 'blue', police= 'Comic Sans MS', taille= 20)
                image(150,250,"plateau0.png",ancrage='center')
                texte(150,350,"0",police= "Comic Sans MS",taille=15)
                image(550,250,"plateau1.png",ancrage='center')
                texte(555,350,"1",police= "Comic Sans MS",taille=15)
                image(350,400,"plateau3.png",ancrage='center')
                texte(150,600,"2",police= "Comic Sans MS",taille=15)
                image(150,500,"plateau2.png",ancrage='center')
                texte(350,470,'3',police= "Comic Sans MS",taille=15)
                image(550,500,"plateau4.png",ancrage='center')
                texte(550,600,'4',police= "Comic Sans MS",taille=15)

                ev = attend_ev()
                tev = type_ev(ev)
                

                if tev == 'Touche':
                    while str(touche(ev)) not in lst_verif:  #Si la touche choisis n'est pas dans liste_plateau
                        texte(190,600, '! Il faut sélectionner un plateau qui existe !', couleur = 'red',police= 'Comic sans MS', taille = 13, tag = 'erreur')
                        mise_a_jour()
                        ev = attend_ev()  #Redemande de sélectionner le plateau à choisir

                    n = int(touche(ev))

                    mise_a_jour()
                    efface_tout()

                else:
                    while tev != 'Touche' and str(touche(ev)) not in lst_verif:
                        texte(190,600, '! Il faut sélectionner un plateau qui existe !', couleur = 'red',police= 'Comic sans MS', taille = 13, tag = 'erreur')
                        mise_a_jour()
                        ev = attend_ev()  #Redemande de sélectionner le plateau à choisir

                    n = int(touche(ev))

                    mise_a_jour()
                    efface_tout()

                #Mise en place du jeu:

                plateau = charger(l[n])  #Appelle la fonction et créer le plateau en fonction du plateau choisis

                mouton = moutons(l[n])  #coordonnées des moutons
                coordonnees = éléments(l[n])  #coordonnées des éléments fixe

                fltk_plateau(plateau)  #Créer le plateau

                afficher_moutons(mouton, plateau)  #Affiche les moutons dans le plateau

                afficher_elmt(coordonnees, plateau)  #Affiche les autres éléments dans le plateau

                mise_a_jour()


                herbe = 0

                for g in plateau:
                    for h in g:
                        if h == 'G':  #On compte le nombre d'herbe dans le plateau avant de commencer la partie
                            herbe += 1
            
            elif touche(ev) == 'r':

                efface_tout()
                plateau = charger(l[n])  #Appelle la fonction et créer le plateau en fonction du plateau choisis

                mouton = moutons(l[n])  #coordonnées des moutons
                coordonnees = éléments(l[n])  #coordonnées des éléments fixe

                fltk_plateau(plateau)  #Créer le plateau

                afficher_moutons(mouton, plateau)  #Affiche les moutons dans le plateau

                afficher_elmt(coordonnees, plateau)  #Affiche les autres éléments dans le plateau

                mise_a_jour()


                herbe = 0

                for g in plateau:
                    for h in g:
                        if h == 'G':  #On compte le nombre d'herbe dans le plateau avant de commencer la partie
                            herbe += 1

            else:
                while touche(ev) not in direction:  #Si la touche choisis est différente des flèches
                    texte(180, 650, 'Sélectionnez une direction avec les flèches', couleur= 'red', police= "Comic Sans MS",taille= 15, tag= "erreur")
                    mise_a_jour()
                    ev = attend_ev()
                
                else:
                    efface('erreur')  #Efface le message d'erreur de direction
                    mouton = jouer(plateau, mouton, touche(ev))  #Les moutons changent de coordonnées après avoir jouer
                    efface('mouton')  #Efface les moutons joué juste avant
                    afficher_moutons(mouton, plateau)  #Affiche les nouveaux moutons à leurs place

                    mise_a_jour()

    efface_tout()

    texte(120,200, 'Félicitations vous avez gagné !', police='Comic Sans MS', taille = 30, couleur='Blue')
    texte(85,500, "Appuyez sur une autre touche pour fermer la page", police='Comic Sans MS', taille = 20, couleur= 'Green')
    texte(160,400, "Appuyez sur 'm' pour revenir au Menu", police='Comic Sans MS', taille = 20, couleur= 'Green')

    image(200,300,'sheep_grass.png')
    image(400,300,'sheep_grass.png')
    image(600,300,'sheep_grass.png')

    ev = attend_ev()
    tev = type_ev(ev)

    if touche(ev) == 'm':
        efface_tout()
        Play()

    mise_a_jour()

    ferme_fenetre()

Play()
