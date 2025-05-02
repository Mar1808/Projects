from fltk import *

cree_fenetre(800, 520)
a = 0
for i in range(21):
    a += 30
    objet = cercle(50 + a, 250, 8, 'purple', 'purple', 3)  # on peut définir ça en tant que variable.
    

attend_ev()  #  Si le coup est possible et si il est joué alors on peut effacer le ou les objets.
efface(objet)


attend_ev()
ferme_fenetre()