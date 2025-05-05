
      #################################################      Fonction      #########################################################

from random import *


def mode_jeu(m):
    ''' Détermine le mode de jeux saisie par l'utilisateur
    :param m: int
    :param j: int
    :param value: str
    >>> mode_jeu(1)
    Marienbad, Normal
    >>> mode_jeu(2)
    Misère
    >>> mode_jeu(5)
    None
    >>> mode_jeu(0)
    None
    ''' 
    strA = 0   # On initialise cette variable pour après convertir une liste en str
    liste = []
    if m != 1 and m != 2:
        return None
    if m == 1:
        liste.append("Normal")
    if m == 2:
        liste.append("Misère")
    strA = ", ".join(liste)  # " ".join() permet de convertir une lst en str
    return strA



def jouer_coup(nombre_objet, shot):
    '''Détermine le coup jouer et modifie le jeu en le soustrayant avec le coup
    :param liste: list
    :param shot: int
    >>> jouer_coup(21, 3)
    18
    >>> jouer_coup(5, 1)
    4
    >>> jouer_coup(21, 1)
    20
    >>> jouer_coup(5, 1)
    4
    >>> jouer_coup(4, 0)  # puisque 0 n'est pas dans la liste dans la fonction coup_possible
    None
    '''
    nombre_objet = nombre_objet - shot
    return nombre_objet   


def coup_possible(coup_autorise, shot):
    '''Détermine si un coup est possible ou pas en fonction des paramètres que l'utilisateur a saisie 
    :param t: list
    :param c: int   
    :param value: bool
    >>> coup_possible([1,2,3,4],4)
    True
    >>> coup_possible([1,2,3,4],5)
    False
    >>> coup_possible([],3)
    False
    >>> coup_possible([1,2,3],0)
    False
    >>> coup_possible([1,3],2)
    False
    '''
    for i in range(len(coup_autorise)):  # Boucle qui parcours les coup autorisés
        if shot == coup_autorise[i]:  # Si la valeur saisie par l'utilisateur est dans les coups autorisés alors on retourne cette valeur
            return True
    return False  # Boucle while si le coup n'est pas possible


def Joueur(joueur):
    ''' Permet d'alterner entre les joueurs après chaque coup joué
    :param joueur: int
    :param value: str
    >>> Joueur(4)
    "joueur2"
    >>> Joueur(3)
    "joueur1"
    >>> Joueur(1)
    "joueur1"
    '''
    if joueur % 2 == 0:
        return "joueur2"
    else:
        return "joueur1"


def Victoire(m, joueur):
    '''Détermine le vainqueur de la manche.
    :param m: str
    :param joueur: str
    :param value: str
    >>> Victoire("Normal", "joueur2")
    "Joueur 2 vous avez gagné ! Joueur 1 vous avez perdu."
    >>> Victoire("Normal", "joueur1")
    "Bravo au joueur 1 ! Joueur 2 peut-être une prochaine fois."
    >>> Victoire("Misère", "joueur1")
    "Félicitation Joueur 1 ! Joueur 2 dommage pour vous."
    '''
    joueur -= 1  # On soustrait 1 car à la fin du programme on ajoute 1 or il n'y a pas de suite puisque nombre_objet <= 0
    if Joueur(joueur) == "joueur2":
        if mode_jeu(m) == "Normal":  # Condition si le joueur à choisis le mode Normal alors le dernier joueur qui prend l'objet a gagné.
            return("Joueur 2 vous avez gagné ! Joueur 1 vous avez perdu.")
        else:
            return("Bravo au joueur 1 ! Joueur 2 peut-être une prochaine fois.")  # Si c'est pas le mode Normal alors c'est le mode Misère, le dernier joueur qui prend l'objet à perdu.

    if Joueur(joueur) == "joueur1":
        if mode_jeu(m) == "Normal":
            return("Félicitation Joueur 1 ! Joueur 2 dommage pour vous.")
        else:
            return("Joueur 2 c'est gagné ! Joueur 1 vous avez malheureusement perdu.")



def Victoire_IA(m, joueur):
    '''Détermine la vainqueur de la manche entre l'ordinateur et le joueur.
    :param m: str
    :param joueur: str
    :param value: str
    >>> Victoire_IA("Misère", "joueur2")
    "Et j'ai gagné ! Joueur 1 vous avez malheureusement perdu."
    '''
    joueur -= 1
    if Joueur(joueur) == "joueur2":
        if mode_jeu(m) == "Normal":  # Condition si le joueur à choisis le mode Normal alors le dernier joueur qui prend l'objet a gagné.
            return("J'ai gagné ! Joueur 1 vous avez perdu.")
        else:
            return("Bravo au joueur 1 !")  # Si c'est pas le mode Normal alors c'est le mode Misère, le dernier joueur qui prend l'objet à perdu.

    if Joueur(joueur) == "joueur1":
        if mode_jeu(m) == "Normal":
            return("Félicitation Joueur 1 ! Dommage pour moi.")
        else:
            return("Et j'ai gagné ! Joueur 1 vous avez malheureusement perdu.")


def IA_Misere(nombre_objet):
    '''Détermine les coups que l'ordinateur va jouer en mode Misère, en fonction du nombre de rond dans la partie.
    :param nombre_objet: int
    :param shot: int
    :param value: int
    >>> IA_Misere(4)
    3
    >>> IA_Misere(6)
    1
    >>> IA_Misere(5)
    1
    >>> IA_Misere(3)
    2
    '''
    if nombre_objet % 4 == 0:
        shot = 3
    if nombre_objet % 4 == 1:
        shot = 1
    if nombre_objet % 4 == 2:
        shot = 1
    if nombre_objet % 4 == 3:
        shot = 2
    return shot


def IA_Normal(nombre_objet):
    ''' Détermine le coup que l'ordinateur va jouer en mode Normal, en fonction du nombre de rond dans la partie.
    :param nombre_objet: int
    :param value: int
    >>> IA_Normal(8)
    3
    >>> IA_Normal(9)
    1
    >>> IA_Normal(10)
    2
    >>> IA_Normal(11)
    3
    '''
    if nombre_objet % 4 == 0:
        shot = 3
    if nombre_objet % 4 == 1:
        shot = 1
    if nombre_objet % 4 == 2:
        shot = 2
    if nombre_objet % 4 == 3:
        shot = 3
    return shot


###############################################    Programme qui lance le jeu    ######################################################



nombre_joueur = int(input("Saisissez le nombre de joueur, le maximum autorisé est de 2 : "))
if nombre_joueur == 0 or nombre_joueur > 2:
    while nombre_joueur == 0 or nombre_joueur > 2:
        nombre_joueur = int(input("Vous devez sélectionné soit 1, soit 2 aucune autre valeur est autorisée : "))

Mode = input("Si vous voulez jouer au mode Marienbad saisissez o, sinon saisissez x  : ")
if Mode != "o" and Mode != "x":
    while Mode != "o" and Mode != "x":
        Mode = input("Saisissez soit o, soit x : ")

m = int(input("Saisir le mode de jeu, 1 pour Normal, 2 pour Misère : "))
while mode_jeu(m) == None:
    m = int(input("Vous devez saisir 1 pour mode Normal, ou 2 pour mode Misère : "))



if nombre_joueur == 1:  # Le joueur est seul il va donc jouer contre l'ordinateur (IA)
    if Mode == "x":  # Si le joueur saisis "x" donc jouer sans le mode Marienbad.
        nombre_objet = int(input("Saisissez le nombre d'objet que vous voulez dans la partie : "))  # Demande au joueur de saisir le nombre d'objet qu'il veut dans la partie.

        # Affiche le nombre d'objet qu'il y a dans la partie et les coups autorisés à être joué.
        print("La partie est en mode :",mode_jeu(m))
        print("Il y a :", nombre_objet, "ronds dans la partie. \nLe nombre de rond que vous pouvez retirer dans la partie est : 1, 2 ou 3")
        
        print("")  # Pour donner plus d'espace dans le terminal
        j = int(input("Si vous voulez jouer en premier, saisissez 1, sinon 2 pour que l'IA commence : "))
        while j != 1 and j != 2:
            j = int(input("Vous devez soit saisir 1 pour commencer a jouer, 2 sinon : "))
        # Demande qui joue en premier entre l'odinateur et le joueur.
        if j == 1:
            joueur = 1  # On initialise cette variable à 1 pour la fonction Joueur qui va nous permettre d'alterner joueur par joueur
        else:
            joueur = 0

        while nombre_objet > 0:  # Tant qu'il y a des objets dans la partie. 
            if Joueur(joueur) == "joueur1":  # Si la fonction Joueur renvoie joueur1 alors c'est au Joueur 1 de jouer
                print("")
                print("Joueur 1 veuillez jouer.")

                shot = int(input("Quel coup vous voulez jouer ? : "))  # Demande le nombre d'objet que le joueur veut retirer.
                if shot != 1 and shot != 2 and shot != 3:
                    shot = int(input("Vous ne pouvez pas jouer ce coup. Tentez un autre coup :"))  # Affiche cette phrase.
                
                nombre_objet = jouer_coup(nombre_objet, shot)  # Le coup est joué et la fonction va enlever n élément au nombre d'objet
                if nombre_objet <= 0:  # Si le nombre d'objet est inférieur à 0 on affiche 0. Au lieu de -2 par exemple on affiche 0
                    nombre_objet = 0
                    print("Il n'y a plus de rond.")
                else:
                    print("Il reste", nombre_objet, "ronds :", "o " * nombre_objet)
                joueur += 1
            
            else:
                print("")
                print("C'est à l'IA de jouer.")
                if mode_jeu(m) == "Misère":
                    shot = IA_Misere(nombre_objet)
                    print("J'ai joué", IA_Misere(nombre_objet))
                    nombre_objet = jouer_coup(nombre_objet, shot)
                    if nombre_objet <= 0:  # Si le nombre d'objet est inférieur à 0 on affiche 0. Au lieu de -2 par exemple on affiche 0
                        nombre_objet = 0
                        print("Il n'y a plus de rond.")
                        joueur += 1
                    else:
                        print("Il reste", nombre_objet, "ronds :", "o " * nombre_objet)
                        joueur += 1  # Comme il y a encore des ronds en jeu, on alerne les joueurs
                else:
                    shot = IA_Normal(nombre_objet)
                    print("J'ai joué", IA_Normal(nombre_objet))
                    nombre_objet = jouer_coup(nombre_objet, shot)
                    if nombre_objet <= 0:  # Si le nombre d'objet est inférieur à 0 on affiche 0. Au lieu de -2 par exemple on affiche 0
                        nombre_objet = 0
                        print("Il n'y a plus de rond.")
                        joueur += 1
                    else:
                        print("Il reste", nombre_objet, "ronds :", "o " * nombre_objet)
                        joueur += 1  # Comme il y a encore des ronds en jeu, on alerne les joueurs

        print("")
        print(Victoire_IA(m, joueur))  # Affiche le gagant de la partie.



    else:  # Mode Marienbad contre l'IA.
        liste_objet = [["o", "o", "o", "o", "o", "o", "o"],["o", "o", "o", "o", "o"], ["o", "o", "o"], ["o"]]  # Liste d'objet dans le jeu
        
        lst=[]  # On initialise une liste vide pour lui ajouté des valeurs qui vont nous permettre de vérifier si la rangé sélectionné du joueur existe.
        for i in range(len(liste_objet)):
            lst.append(i+1) # +1 pour commencer à l'indice 1 et pas à 0

        j = int(input("Si vous voulez jouer en premier, saisissez 1, sinon 2 pour que l'IA commence : "))
        while j != 1 and j != 2:
            j = int(input("Vous devez soit saisir 1 pour commencer a jouer, 2 sinon : "))
        # Demande qui joue en premier entre l'odinateur et le joueur.
        if j == 1:
            joueur = 1  # On initialise cette variable à 1 pour la fonction Joueur qui va nous permettre d'alterner joueur par joueur
        else:
            joueur = 0

        print("La manche est en mode : ",mode_jeu(m))

        for i in liste_objet:
                    print(" ".join(i))

        while liste_objet != [[], [], [], []]:  # Tant qu'il y a des objets dans le jeu 
                
            if Joueur(joueur) == "joueur1":  # Permet d'alterner le tour des joueurs
                print("")
                print("Joueur 1 veuillez jouer.")

                shot1 = int(input("Choisissez la rangé dans laquelle vous voulez enlevez des ronds : "))
                if shot1 not in lst:   
                    while shot1 not in lst:
                        shot1 = int(input("Cette rangé n'existe pas, veuillez saisir une rangé existante : "))
                
                if liste_objet[shot1 - 1] == []:    #  Plantage si on saisie une rangée qui n'existe pas après avoir contrôlé la saisie "il n'y a plus de rond", et vice versa.
                    while liste_objet[shot1 - 1] == []:
                        shot1 = int(input("Il n'y a plus de rond dans cette rangé, veuillez en saisir une autre : "))
                
                
                range_select = liste_objet[shot1 - 1]  # Accède à l'indice shot1 - 1 de liste_objet car l'indice commence à 0
                print(" ".join(range_select))  # Affiche la rangé sous forme de str et non de liste

                shot2 = int(input("Combien de rond voulez vous enlever : "))
                while shot2 > len(range_select) or shot2 <= 0:  # Si le nombre d'élément dans la rangé sélectionner est inférieur à la saisie de l'utilisateur ou si la saisie de l'utilisatur <= 0
                    shot2 = int(input("Vous ne pouvez pas jouer cette valeur, veuillez saisir une autre valeur : "))
                del range_select[0:shot2]  # Enlève le shot2 objet de range select en commençant à partir de l'indice 0
                print("")

                for i in liste_objet:
                    print(" ".join(i))
                joueur += 1  # Pour la fonction joueur
            
            else:
                print("C'est à l'IA de jouer.")
                
                shot1 = randint(0,3)  # L'ordinateur va pouvoir jouer une rangé au hasard, apporte du changement entre chaque partie.
                while liste_objet[shot1] == []:  # vérifie si la rangée est vide ou pas.
                    shot1 = randint(0,3)  # si elle est vide l'ordinateur en cherche un autre.
                
                range_select = liste_objet[shot1]
                shot2 = randint(1,len(range_select))
                while shot2 < 1 or shot2 > len(range_select):
                    shot2 = randint(0,len(range_select))

                if shot1 == 0:
                    print("J'ai retiré", shot2 ,"ronds de la ", shot1 + 1, "ère rangée.")
                else:
                    print("J'ai retiré", shot2 ,"ronds de la ", shot1 + 1, "ème rangée.")
                del range_select[0:shot2]
                
                for i in liste_objet:
                    print(" ".join(i))
                joueur += 1  # Pour la fonction joueur
            
        print(Victoire_IA(m, joueur))



else:  # Si il y a deux joueurs
    if Mode == "x":  # Si le joueur saisis "x" donc jouer sans le mode Marienbad.
        nombre_objet = int(input("Saisissez le nombre de rond que vous voulez dans la partie : "))  # Demande au joueur de saisir le nombre d'objet qu'il veut dans la partie.
        
        print(" ")

        coup_autorise = []
        print("Saisissez 3 valeurs, qui vont déterminer le nombre de rond que le joueur peut retirer.")
        
        a = int(input("La valeur ne peut pas être plus grande que 6 et inférieur ou égal à 0 : "))
        while a <= 0 or a > 6:
            a = int(input("Vous ne pouvez pas retirer ce nombre de rond, saisissez une autre valeur : "))
        coup_autorise.append(a)
        print(" ")  # Permet d'espacer et rendre le terminal plus propre.

        b = int(input("La valeur ne peut pas être plus grande que 6 et inférieur ou égal à 0 : "))
        while b <= 0 or b > 6:
            b = int(input("Vous ne pouvez pas retirer ce nombre de rond, saisissez une autre valeur : "))
        coup_autorise.append(b)
        print(" ")

        c = int(input("La valeur ne peut pas être plus grande que 6 et inférieur ou égal à 0 : "))
        while c <= 0 or c > 6:
            c = int(input("Vous ne pouvez pas retirer ce nombre de rond, saisissez une autre valeur : "))
        coup_autorise.append(c)
        
        cp = ", ".join(map(str, coup_autorise))  # Converti une liste de int en str.

        print("")

        j = int(input("Si vous voulez que le Joueur 1 commence saisissez 1, saisissez 2 pour que le Joueur 2 commence : "))
        while j != 1 and j != 2:
            j = int(input("Vous devez saisir 1 --> Joueur 1 commence ou 2 --> Joueur 2 comence : "))
        # Demande qui joue en premier entre l'odinateur et le joueur.
        if j == 1:
            joueur = 1  # On initialise cette variable à 1 pour la fonction Joueur qui va nous permettre d'alterner joueur par joueur
        else:
            joueur = 0

        print("")
        # Affiche le nombre d'objet qu'il y a dans la partie et les coups autorisés à être joué.
        print("La partie est en mode :",mode_jeu(m))
        print("Il y a :", nombre_objet, "ronds dans la partie : ", "o " * nombre_objet)
        print("Les valeurs autorisées à être jouées dans la partie sont : ", cp)

        while nombre_objet > 0:  # Tant qu'il y a des objets dans la partie. 
            if Joueur(joueur) == "joueur1":  # Si la fonction Joueur renvoie joueur1 alors c'est au Joueur 1 de jouer
                print("")
                print("Joueur 1 veuillez jouer.")
            else:
                print("")
                print("Joueur 2 c'est à votre tour.") 

            shot = int(input("Quel coup vous voulez jouer ? : "))  # Demande le nombre d'objet que le joueur veut retirer.
            if coup_possible(coup_autorise, shot) == False:  # Si le coup est impossible on demande a ce qu'il en saisie un autre.
                while coup_possible(coup_autorise, shot) == False:  # Boucle pour contrôler la saisie du joueur
                    shot = int(input("Vous ne pouvez pas jouer ce coup. Tentez un autre coup :"))  # Affiche cette phrase.

            if coup_possible(coup_autorise, shot) == True:  # Si le coup que le joueur a saisie est possible
                nombre_objet = jouer_coup(nombre_objet, shot)  # Le coup est joué et la fonction va enlever n élément au nombre d'objet
                if nombre_objet <= 0:  # Si le nombre d'objet est inférieur à 0 on affiche 0. Au lieu de -2 par exemple on affiche 0
                    nombre_objet = 0
                    print("Il n'y a plus de rond.")
                else:
                    print("Il reste", nombre_objet, "ronds :", "o " * nombre_objet)
                joueur += 1

        print(Victoire(m, joueur))  # Affiche le gagant de la partie.




    else:  # Si les deux joueurs veulent jouer au mode Marienbad donc qu'ils ont saisis "o".
        liste_objet = [["o", "o", "o", "o", "o", "o", "o"],["o", "o", "o", "o", "o"], ["o", "o", "o"], ["o"]]  # Liste d'objet dans le jeu
        
        lst=[]  # On initialise une liste vide pour lui ajouté des valeurs qui vont nous permettre de vérifier si la rangé sélectionné du joueur existe.
        for i in range(len(liste_objet)):
            lst.append(i + 1)  # +1 pour commencer à 1 au lieu de 0

        j = int(input("Si vous voulez que le Joueur 1 commence saisissez 1, saisissez 2 pour que le Joueur 2 commence : "))
        while j != 1 and j != 2:
            j = int(input("Vous devez saisir 1 --> Joueur 1 commence ou 2 --> Joueur 2 comence : "))
        # Demande qui joue en premier entre l'odinateur et le joueur.
        if j == 1:
            joueur = 1  # On initialise cette variable à 1 pour la fonction Joueur qui va nous permettre d'alterner joueur par joueur
        else:
            joueur = 0

        print("Le jeu est mode : ", mode_jeu(m))

        for i in liste_objet:
                    print(" ".join(i))

        while liste_objet != [[], [], [], []]:  # Tant qu'il y à des objets dans le jeu 

            for j in liste_objet:  # Boucle qui parcours les éléments de la liste
                if Joueur(joueur) == "joueur1":  # Permet d'alterner le tour des joueurs
                    print("")
                    print("Joueur 1 veuillez jouer.")
                else:
                    print("")
                    print("Joueur 2 c'est à votre tour.")

                shot1 = int(input("Choisissez la rangé dans laquelle vous voulez enlevez des ronds : "))
                if shot1 not in lst:   
                    while shot1 not in lst:
                        shot1 = int(input("Cette rangé n'existe pas, veuillez saisir une rangé existante : "))
                
                if liste_objet[shot1 - 1] == []:
                    while liste_objet[shot1 - 1] == []:
                        shot1 = int(input("Il n'y a plus de rond dans cette rangé, veuillez en saisir une autre : "))
                
                
                range_select = liste_objet[shot1 - 1]  # Accède à l'indice shot1 de liste_objet
                print(" ".join(range_select))  # Affiche la rangé sous forme de str et non de liste

                shot2 = int(input("Combien de rond voulez vous enlever : "))
                while shot2 > len(range_select) or shot2 <= 0:  # Si le nombre d'élément dans la rangé sélectionner est inférieur à la saisie de l'utilisateur ou si la saisie de l'utilisatur <= 0
                    shot2 = int(input("Vous ne pouvez pas jouer cette valeur, veuillez saisir une autre valeur : "))
                del range_select[0:shot2]  # Enlève le shot2 objet de range select en commençant à partir de l'indice 0
                
                print("")
                for i in liste_objet:
                    print(" ".join(i))
                joueur += 1  # Pour la fonction joueur
    
        print(Victoire(m, joueur))


        

        
