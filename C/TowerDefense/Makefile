CC = gcc
CFLAGS = -std=c17 -Wall
LDFLAGS = 
SRC = Main.c Monstre.c Grille.c Affichage.c
OBJ = $(SRC:.c=.o)
HEADERS = Monstre.h Grille.h Affichage.h
TARGET = tower

$(TARGET): $(OBJ)
	$(CC) $(OBJ) -o $@ $(LDFLAGS)

%.o: %.c $(HEADERS)
	$(CC) $(CFLAGS) -c $< -o $@ $(LDFLAGS)

clean:
	rm -f $(OBJ) $(TARGET)
