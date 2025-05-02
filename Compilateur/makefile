CC = gcc
CFLAGS = -Wall -g -Iobj -Isrc
LDFLAGS = -lfl
PARSER = parser
LEXER = lexer

bin/tpcc: obj/$(LEXER).o obj/$(PARSER).tab.o obj/tree.o obj/symbole.o obj/symbole.o obj/traduction.o
	@mkdir -p bin
	$(CC) -o $@ $^ $(LDFLAGS)

obj/tree.o: src/tree.c src/tree.h
	$(CC) -c -o $@ $< $(CFLAGS)

obj/symbole.o: src/symbole.c src/symbole.h src/tree.h 
	$(CC) -c -o $@ $< $(CFLAGS)

obj/traduction.o: src/traduction.c src/traduction.h src/symbole.h 
	$(CC) -c -o $@ $< $(CFLAGS)

obj/$(PARSER).tab.o: obj/$(PARSER).tab.c src/tree.h src/symbole.h
obj/$(LEXER).o: obj/$(LEXER).c obj/$(PARSER).tab.h

obj/%.o: src/%.c
	$(CC) -c -o $@ $< $(CFLAGS)

obj/$(LEXER).c obj/$(PARSER).tab.h obj/$(PARSER).tab.c: src/$(PARSER).y src/tree.h src/$(LEXER).lex
	bison -d -o obj/$(PARSER).tab.c $<
	flex -o obj/$(LEXER).c src/$(LEXER).lex

_anonymous.o:
	nasm -f elf64 -o _anonymous.o _anonymous.asm

_anonymous: _anonymous.o
	$(CC) -o _anonymous _anonymous.o -nostartfiles -no-pie

clean:
	rm -r bin/*
	rm -r obj/*

clean_ano:	
	rm -f _anonymous.o _anonymous