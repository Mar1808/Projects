def mode_jeu(m, j):
    ''' Détermine le mode de jeux saisie par l'utilisateur
    :param m: int
    :param j: int
    :param value: str
    >>> mode_jeu(1, 2)
    Marienbad, Normal
    >>> mode_jeu(0, 3)
    Misère
    >>> mode_jeu(5, 2)
    None
    >>> mode_jeu(0, 0)
    None
    ''' 
    strA = 0    # On initialise cette variable pour après convertir une liste en str
    liste = []
    if m != 0 and m != 1:
        return None
    if j != 2 and j != 3:
        return None
    if m == 1:
        liste.append("Marienbad")
    if j == 2:
        liste.append("Normal")
    if j == 3:
        liste.append("Misère")
    strA = ", ".join(liste)  # " ".join() permet de convertir une lst en str
    return strA


def parametre_1(nombre_objet, coup_autorise):  # Cette fonction change selon le mode de jeu Marienbad on peut enlever    
    '''Détermine le nombre d'objet que l'utilisateur veut mettre en jeu et le nombre d'objet que l'on peut retiré en un coup,
    cette fonction créé une liste et vérifie si l'intervalle que l'utilisateur à saisie est dans la liste.
    :param nombre_objet: int
    :param coup_autorise: lst
    :param n: int
    :param value: bool
    >>> parametre_1(10, [5,2,14])
    None
    >>> parametre_1(10, [1,2,3])
    True
    >>> parametre(10, [0,0,0])
    None
    >>> parametre_1(10, [1,2,3])
    True
    '''
    liste = []
    for i in range(1, nombre_objet + 1):  # La boucle sert à ajouter les valeurs de 1 au nombre d'objet
        liste.append(i)                   # saisie par l'utilisateur dans une liste
    for i in range(len(coup_autorise)):   # Boucle qui parcours les coups autorisés
        if coup_autorise[i] > len(liste) or coup_autorise[i]<= 0:   # Si un coup autorisé est supérieur aux éléments de la liste alors
            return None                                             # on ne peut pas les jouer on retourne donc None
    return True

def jouer_coup(nombre_objet, shot):
    '''Détermine le coup jouer et modifie le jeu en le soustrayant avec le coup
    :param liste: list
    :param shot: int
    >>> jouer_coup([1,2,3,...,21], 3)
    [1, 2, 3, ..., 18]
    >>> jouer_coup(5, 1)
    [1, 2, 3, 4]
    >>> jouer_coup([1,2,3,...,21], 1)
    [1, 2, 3, 4, ..., 20]
    >>> jouer_coup([1,2,3,4,5], 1)
    [1, 2, 3, 4]
    >>> jouer_coup([1,2,3,4], 0)  # puisque 0 n'est pas dans la liste dans la fonction coup_possible
    None
    '''
    del nombre_objet[-shot:]   # enlève les derniers éléments de la liste
    return nombre_objet    # retourne la liste après lui avoir retirer les éléments   


def coup_possible(t, c):
    '''Détermine si un coup est possible ou pas en fonction des paramètres que l'utilisateur a saisie 
    :param t: list  # liste que l'utilisateur avait saisie dans la fonction parametre
    :param c: int   
    :param value: int
    >>> coup_possible([1,2,3,4],4)
    4
    >>> coup_possible([1,2,3,4],5)
    None
    >>> coup_possible([],3)
    None
    >>> coup_possible([1,2,3],0)
    None
    >>> coup_possible([1,3],2)
    None
    '''
    for i in range(len(t)): # Boucle qui parcours les coup autorisés
        if c == t[i]:   # Si la valeur saisie par l'utilisateur est dans les coups autorisés alors on retourne cette valeur
            return c
    return None  # Boucle while si le coup n'est pas possible



def Jouer():
    m = int(input("Saisir le mode de jeu, 1 pour Marienbad, 0 sinon : "))
    j = int(input("Veuillez encore saisir le mode de jeu, 2 pour Normal, 3 pour Misère : "))
    if mode_jeu(m, j) == None:
        while mode_jeu(m, j) == None:
            m = int(input("Vous devez saisir soit 1 pour Marienbad, soit 0. Aucune autre valeur : "))
            j = int(input("Vous devez saisir soit 2 pour Normal, soit 3 pour Misère. Aucune autre valeur : "))
    else:
        a = int(input("Saisir le nombre d'objet que vous voulez dans la partie : "))
        cmpt_1 = 0
        while cmpt_1 < 3:
            e = int(input("Maintenant saisissez les 3 valeurs que l'on peut retiré dans le jeu. Il y a une limite, la valeur ne peut pas être plus grande que le jeu lui même ou encore inférieur ou égal à 0 : "))
            cmpt_1 += 1
        if parametre_1(a, e) == True:
            play = int(input("Quel coup vous voulez jouer : "))


Jouer()