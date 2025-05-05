#Projet informatique : Enveloppe Convexe dans le plan

------------------------------------------

Compilation : clang -std=c17 -Wall Projet.c -o projet -lMLV -lm

Exécution avec le terminal:

./projet -[mode] -n [nombre points] -[mode d'affichage] -a [graine]

Exemple d'exécution:

Pour une sélection manuelle de 20 points:

        ./projet -souris -n 20

Pour un cercle avec 250 points, un affichage dynamique et une graine de 50: 

        ./projet -cercle -n 250 -D -a 52


Pour un carré pseudo spirale avec 370 points, un affichage terminal et une graine de 32:

        ./projet -carreps -n 370 -T -a 32

Points à améliorer;
    _ Modifier les fonctionalités pour quitter le programme, pour recommencer le programme et pour la redirection vers le menu lors de la sélection à la souris: Il faut à la fois appuyer sur 'échap' et cliquer sur la page pour pouvoir quitter, même problème pour recommencer le programme et lancer le menu.
