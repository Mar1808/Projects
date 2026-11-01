CC=gcc
CFLAGS=-std=c17 -Wall
OBJ=Main.o Affichage.o Particule.o QuadTree.o

QuadTree: $(OBJ)
	$(CC) -o $@ $^ -lMLV -lm

Main.o: Main.c Affichage.h Carre.h Particule.h QuadTree.h
Affichage.o: Affichage.c Particule.h QuadTree.h
Particule.o: Particule.c QuadTree.h
QuadTree.o: QuadTree.c Carre.h Particule.h Affichage.h

%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

clean:
	-rm -f $(OBJECTS) $(OBJ) QuadTree