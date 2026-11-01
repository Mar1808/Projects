#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "traduction.h"

extern int error;

int indexLabel = 0;

// fonction qui traduit la fonction nomFonction
void parcoursMain(Node *node, FILE* fichier, Tables *t, char *nomFonction) {
    if(node == NULL) 
      return ;
    switch(node->label) {
        case addsub:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            if(strcmp(node->attribute, "-") == 0) {
                fprintf(fichier, "    ; addition\n");
                fprintf(fichier, "    pop rax\n");
                fprintf(fichier, "    neg rax\n");
                fprintf(fichier, "    push rax\n");
            }
            return;
        case instr:;
            int adresse = recupAdresse(node->firstChild->attribute, t, nomFonction);
            if(adresse == -1) {
                fprintf(stderr, "Variable %s non déclarée\n", node->firstChild->attribute);
                error = 2;
            }
            fprintf(fichier, "    ; instruction\n");
            fprintf(fichier, "    mov rsi, %d\n", atoi(node->firstChild->nextSibling->attribute)); // adresse du tableau
            fprintf(fichier, "    imul rsi, 8\n");
            fprintf(fichier, "    add rsi, %d\n", adresse); 
            fprintf(fichier, "    mov [rsp + rsi], %d\n", atoi(node->firstChild->nextSibling->nextSibling->attribute));
            return;
        case num:
            fprintf(fichier, "    mov rsi, %s\n", node->attribute);
            fprintf(fichier, "    push rsi\n");
            return;
        case character:;
            char c = node->attribute[1];
            if(c == '\\') {
                switch(node->attribute[2]) {
                    case 'n':
                        c = '\n';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case '0':
                        c = '\0';
                        break;
                    case '\\':
                        c = '\\';
                        break;
                    case '\'':
                        c = '\'';
                        break;
                    case '\"':
                        c = '\"';
                        break;
                    default:
                        break;
                }
            }
            fprintf(fichier, "    mov rax, %d\n", c);
            fprintf(fichier, "    push rax\n");
            return;
        
        case ident:;
            // si c'est une fonction on l'appelle
            if(estFonction(node->attribute, t)) {
                // ajout des arguments
                Node *args = node->firstChild;
                while(args != NULL) {
                    parcoursMain(args, fichier, t, nomFonction);
                    args = args->firstChild;
                }
                fprintf(fichier, "    call %s\n", node->attribute);
                return;
            }
            // si c'est une des fonctions suivantes on l'appelle
            if(strcmp(node->attribute, "putchar") == 0) {
                // ajout des arguments
                parcoursMain(node->firstChild, fichier, t, nomFonction);
                fprintf(fichier, "    pop rax\n");
                fprintf(fichier, "    call %s\n", node->attribute);
                return;
            }
            if(strcmp(node->attribute, "getint") == 0 || strcmp(node->attribute, "getchar") == 0) {
                fprintf(fichier, "    call %s\n", node->attribute);
                fprintf(fichier, "    push rax\n");
                return;
            }
            if(strcmp(node->attribute, "putint") == 0) {
                // ajout des arguments
                parcoursMain(node->firstChild, fichier, t, nomFonction);
                fprintf(fichier, "    pop rax\n");
                fprintf(fichier, "    mov [number], rax\n");
                fprintf(fichier, "    call %s\n", node->attribute);
                return;
            }
            else {
                int adresse = recupAdresse(node->attribute, t, nomFonction);
                fprintf(fichier, "    mov rax, [rsp+%d]\n", adresse +8);
                fprintf(fichier, "    push rax\n");
                return;
            }
        case eq:;
            indexLabel ++;
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "eq%d:\n", indexLabel);
            fprintf(fichier, "    pop rbx\n");
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    cmp rax, rbx\n");
            fprintf(fichier, "    jne else_eq%d\n", indexLabel);
            fprintf(fichier, "    mov rax, 1\n");
            fprintf(fichier, "    je end_eq%d\n", indexLabel);
            fprintf(fichier, "else_eq%d:\n", indexLabel);
            fprintf(fichier, "    mov rax, 0\n");
            fprintf(fichier, "    push rax\n");
            fprintf(fichier, "end_eq%d:\n", indexLabel);
            return ;
        case different:;
            indexLabel ++;
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "different%d:\n", indexLabel);
            fprintf(fichier, "    pop rbx\n");
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    cmp rax, rbx\n");
            fprintf(fichier, "    je else_different%d\n", indexLabel);
            fprintf(fichier, "    mov rax, 1\n");
            fprintf(fichier, "    je end_different%d\n", indexLabel);
            fprintf(fichier, "else_different%d:\n", indexLabel);
            fprintf(fichier, "    mov rax, 0\n");
            fprintf(fichier, "end_different%d:\n", indexLabel);
            return ;
        case and:;
            indexLabel ++;
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "and%d\n", indexLabel);
            fprintf(fichier, "    pop r12\n");
            fprintf(fichier, "    pop r13\n");
            fprintf(fichier, "    cmp r12, 1\n");
            fprintf(fichier, "    jne and_false%d\n", indexLabel);
            fprintf(fichier, "    cmp r13, 1\n");
            fprintf(fichier, "    jne and_false%d\n", indexLabel);
            fprintf(fichier, "    mov rax, 1\n");
            fprintf(fichier, "    push rax\n");
            fprintf(fichier, "    jmp end_and%d\n", indexLabel);
            fprintf(fichier, "and_false%d:\n", indexLabel);
            fprintf(fichier, "    mov rax, 0\n");
            fprintf(fichier, "    push rax\n");
            fprintf(fichier, "end_and%d:\n", indexLabel);
            return;
        case or:;
            indexLabel ++;
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "or%d\n", indexLabel);
            fprintf(fichier, "    pop r12\n");
            fprintf(fichier, "    pop r13\n");
            fprintf(fichier, "    cmp r12, 1\n");
            fprintf(fichier, "    je or_true%d\n", indexLabel);
            fprintf(fichier, "    cmp r13, 1\n");
            fprintf(fichier, "    je or_true%d\n", indexLabel);
            fprintf(fichier, "    mov rax, 0\n");
            fprintf(fichier, "    push rax\n");
            fprintf(fichier, "    jmp end_or%d\n", indexLabel);
            fprintf(fichier, "or_true%d:\n", indexLabel);
            fprintf(fichier, "    mov rax, 1\n");
            fprintf(fichier, "    push rax\n");
            fprintf(fichier, "end_or%d:\n", indexLabel);
            return;
        case addition:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            fprintf(fichier, "    ; addition\n");
            fprintf(fichier, "    pop rbx\n");
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    add rax, rbx\n");
            fprintf(fichier, "    push rax\n");
            return;
        case soustraction:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            fprintf(fichier, "    ; soustraction\n");
            fprintf(fichier, "    pop rbx\n");
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    sub rax, rbx\n");
            fprintf(fichier, "    push rax\n");
            return;
        case multiplication:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            fprintf(fichier, "    ; multiplication\n");
            fprintf(fichier, "    pop rbx\n");
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    imul rax, rbx\n");
            fprintf(fichier, "    push rax\n");
            return;
        case division:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            fprintf(fichier, "    ; division\n");
            fprintf(fichier, "    mov rdx, 0\n");
            fprintf(fichier, "    pop rbx\n");
            parcoursMain(node->firstChild->nextSibling, fichier, t, nomFonction);
            fprintf(fichier, "    pop rax\n");
            fprintf(fichier, "    idiv rbx\n");
            fprintf(fichier, "    push rax\n");
            return;
        case _return_:
            parcoursMain(node->firstChild, fichier, t, nomFonction);
            fprintf(fichier, "    ; return\n");
            fprintf(fichier, "    pop rdi\n");
            return;
        
        default:
            break;
    }
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
        switch(child->label) {
            case tab:;
                int adresseTab = recupAdresse(child->firstChild->attribute, t, nomFonction);
                if(adresseTab == -1) {
                    fprintf(stderr, "Variable %s non déclarée\n", child->firstChild->attribute);
                    error = 2;
                }
                fprintf(fichier, "    mov rax, [rsp + %d]\n", adresseTab);                
                continue;
            case affectation:;
                int adresse = recupAdresse(child->firstChild->attribute, t, nomFonction);
                if(adresse == -1) {
                    fprintf(stderr, "Variable %s non déclarée\n", child->firstChild->attribute);
                    error = 2;
                }
                parcoursMain(child->firstChild->nextSibling, fichier, t, nomFonction);
                fprintf(fichier, "    ; affectation\n");
                // si c'est une fonction on l'appelle et on stocke le résultat depuis rdi
                if(child->firstChild->nextSibling->attribute && estFonction(child->firstChild->nextSibling->attribute, t)) {
                    fprintf(fichier, "    mov [rsp+%d], rdi\n", adresse + 8);
                }
                else {
                    fprintf(fichier, "    pop rax\n");
                    fprintf(fichier, "    mov [rsp+%d], rax\n", adresse + 8);
                }
                continue;
            case _if_:;
                indexLabel ++;
                parcoursMain(child->firstChild, fichier, t, nomFonction);
                fprintf(fichier, "if%d:\n", indexLabel);
                fprintf(fichier, "    pop rsi\n");
                fprintf(fichier, "    cmp rsi, 0\n");
                fprintf(fichier, "    je else_if%d\n", indexLabel);
                parcoursMain(child->firstChild->nextSibling, fichier, t, nomFonction);
                fprintf(fichier, "    jmp end_if%d\n", indexLabel);
                fprintf(fichier, "else_if%d:\n", indexLabel);
                parcoursMain(child->firstChild->nextSibling->nextSibling, fichier, t, nomFonction);
                fprintf(fichier, "end_if%d:\n", indexLabel);
                continue;
            case _while_:;
                indexLabel ++;
                fprintf(fichier, "while%d:\n", indexLabel);
                parcoursMain(child->firstChild, fichier, t, nomFonction);
                fprintf(fichier, "    pop rax\n");
                fprintf(fichier, "    cmp rax, 0\n");
                fprintf(fichier, "    je end_while%d\n", indexLabel);
                parcoursMain(child->firstChild->nextSibling, fichier, t, nomFonction);
                fprintf(fichier, "    jmp while%d\n", indexLabel);
                fprintf(fichier, "end_while%d:\n", indexLabel);
                continue;
            default:
                break;
        }
        parcoursMain(child, fichier, t, nomFonction);
    }
}

void fonctionsASM(FILE* fichier) {
    // fonction putchar
    // modifier char_to_print
    // exemple
    // mov byte[char_to_print], 'n'
    // mov rdi, char_to_print    
    // call putchar
    fprintf(fichier, "\nputchar: ;fonction putchar\n");
    fprintf(fichier, "    push rax\n");
    fprintf(fichier, "    mov rax, 1 ; sys_write\n");
    fprintf(fichier, "    mov rdi, 1 ; fd stdout\n");
    fprintf(fichier, "    mov rsi, rsp ; pointeur sur la chaine\n");
    fprintf(fichier, "    mov rdx, 1 ; longueur de la chaine\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    pop rax\n");
    fprintf(fichier, "    ret\n\n");

    // fonction getchar
    fprintf(fichier, "\ngetchar: ;fonction getchar\n");
    fprintf(fichier, "    sub rsp, 8\n");
    fprintf(fichier, "    mov rax, 0 ; sys_read\n");
    fprintf(fichier, "    mov rdi, 0 ; fd stdin\n");
    fprintf(fichier, "    mov rsi, rsp ; pointeur sur la chaine\n");
    fprintf(fichier, "    mov rdx, 1 ; longueur de la chaine\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    pop rax\n");
    fprintf(fichier, "    ret\n\n");

    // fonction getint
    fprintf(fichier, "getint:\n");
    fprintf(fichier, "    mov rax, 0\n");
    fprintf(fichier, "    mov [number], rax\n");
    fprintf(fichier, "    mov r12, 0 ; 0 si le nombre est positif, 1 si le nombre est négatif\n");
    fprintf(fichier, "    ; Lire un caractère\n");
    fprintf(fichier, "    read_carac:\n");
    fprintf(fichier, "    mov rax, 0\n");
    fprintf(fichier, "    mov rdi, 0\n");
    fprintf(fichier, "    mov rsi, digit\n");
    fprintf(fichier, "    mov rdx, 1\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    ;Vérifier si le caractère est un chiffre\n");
    fprintf(fichier, "    mov al, [digit]\n");
    fprintf(fichier, "    cmp al, '+' ; si on trouve le signe '+'\n");
    fprintf(fichier, "    je read_carac ; on l'ignore et on passe au caractère suivant\n");
    fprintf(fichier, "    cmp al, '-' ; si on trouve le signe '-'\n");
    fprintf(fichier, "    je getint_negatif\n");
    fprintf(fichier, "    cmp al, '0'\n");
    fprintf(fichier, "    jl end_fonct\n");
    fprintf(fichier, "    cmp al, '9'\n");
    fprintf(fichier, "    jg end_fonct\n");
    fprintf(fichier, "    sub al, '0'\n");
    fprintf(fichier, "    movzx rbx, al\n");
    fprintf(fichier, "    ; Multiplier [number] par 10\n");
    fprintf(fichier, "    mov rax, [number]\n");
    fprintf(fichier, "    imul rax, 10\n");
    fprintf(fichier, "    ; Ajouter [digit] à [number]\n");
    fprintf(fichier, "    add rax, rbx\n");
    fprintf(fichier, "    mov [number], rax\n");
    fprintf(fichier, "    jmp read_carac\n");
    fprintf(fichier, "    end_fonct:\n");
    fprintf(fichier, "    mov rax, [number]\n");
    fprintf(fichier, "    ; Si le nombre est négatif, changer le signe\n");
    fprintf(fichier, "    cmp r12, 0\n");
    fprintf(fichier, "    je end_getint\n");
    fprintf(fichier, "    neg rax\n");
    fprintf(fichier, "    end_getint:\n");
    fprintf(fichier, "    ret\n");
    fprintf(fichier, "    getint_negatif:\n");
    fprintf(fichier, "    mov r12, 1 ; on met r12 à 1 pour indiquer que le nombre est négatif\n");
    fprintf(fichier, "    jmp read_carac ; on passe au caractère suivant\n");

    // fonction putint
    fprintf(fichier, "\nputint: \n");
    fprintf(fichier, "    ; initialiser i à imax\n");
    fprintf(fichier, "    mov rdi, 24\n");
    fprintf(fichier, "    mov rsi, rdi\n");
    fprintf(fichier, "    ; Vérifier si le nombre est négatif\n");
    fprintf(fichier, "    mov r13, [number]\n");
    fprintf(fichier, "    cmp r13, 0\n");
    fprintf(fichier, "    jge loop ; s'il est positif, continuer l'affichage\n");
    fprintf(fichier, "    ; si le nombre est négatif, afficher le signe\n");
    fprintf(fichier, "    mov rax, 1         ; sys_write\n");
    fprintf(fichier, "    mov rdi, 1         ; file descriptor 1 (stdout)\n");
    fprintf(fichier, "    lea rsi, [msg_negative]        ; '-'\n");
    fprintf(fichier, "    mov rdx, 1\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    ; changer le signe du nombre\n");
    fprintf(fichier, "    neg r13\n");
    fprintf(fichier, "    mov [number], r13\n");
    fprintf(fichier, "    call putint\n");
    fprintf(fichier, "    ret\n");
    fprintf(fichier, "    loop:\n");
    fprintf(fichier, "    mov rax, [number]\n");
    fprintf(fichier, "    xor rdx, rdx ;vider rdx\n");
    fprintf(fichier, "    mov rcx, 10\n");
    fprintf(fichier, "    div rcx\n");
    fprintf(fichier, "    mov [number], rax\n");
    fprintf(fichier, "    add dl, 48\n");
    fprintf(fichier, "    mov [digit + rsi], dl\n");
    fprintf(fichier, "    dec rsi\n");
    fprintf(fichier, "    cmp qword [number], 0\n");
    fprintf(fichier, "    jg loop\n");
    fprintf(fichier, "    mov rax, rdi       ; imax\n");
    fprintf(fichier, "    sub rax, rsi       ; imax - i\n");
    fprintf(fichier, "    mov rcx, rax       ; longueur de la chaîne\n");
    fprintf(fichier, "    inc rcx            ; inclure le caractère final\n");
    fprintf(fichier, "    mov rax, 1         ; sys_write\n");
    fprintf(fichier, "    mov rdi, 1         ; file descriptor 1 (stdout)\n");
    fprintf(fichier, "    lea rsi, [digit + rsi + 1]\n");
    fprintf(fichier, "    mov rdx, rcx       ; nombre de caractères\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    mov rax, 1         ; sys_write\n");
    fprintf(fichier, "    mov rdi, 1         ; file descriptor 1 (stdout)\n");
    fprintf(fichier, "    mov rsi, newline\n");
    fprintf(fichier, "    mov rdx, 1\n");
    fprintf(fichier, "    syscall\n");
    fprintf(fichier, "    ret\n");
}

void traductionASM(Node* node, FILE* fichier, Tables *t) {
    if(node == NULL) 
      return ;
    if(node->label == prog) {
        fprintf(fichier, "section .bss\n");
        fprintf(fichier, "    number resq 1 ;  Variable pour stocker le nombre final (putint, getint)\n");
        fprintf(fichier, "    digit resb 25 ; Variable pour stocker le caractère lu (putint, getint)\n");
        fprintf(fichier, "section .data\n");
        fprintf(fichier, "    newline db 10 ; caractère de nouvelle ligne\n");
        fprintf(fichier, "    msg_negative db '-', 0    ; Le signe négatif à afficher\n");
        // traductions des variables globales
        if(node->firstChild->label == declvars) {
            // parcours des variables globales
            char *type_var = NULL;
            for (Node *child2 = node->firstChild->firstChild; child2 != NULL; child2 = child2->nextSibling) {
                if(child2->label == type) {
                    type_var = child2->attribute;
                }
                if(child2->label == ident) {
                    if(strcmp(type_var, "int") == 0) {
                        fprintf(fichier, "    %s resq 1\n", child2->attribute);
                    }
                    if(strcmp(type_var, "char") == 0) {
                        fprintf(fichier, "    %s resb 1\n", child2->attribute);
                    }
                }
            }
        }
        fprintf(fichier, "global _start\nsection .text\nextern show_registers\n");
        fonctionsASM(fichier);  // écriture des fonctions getint, putint, getchar, putchar
    }

    // traduction des fonctions
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
        if(child->label == declfonct){
            if(strcmp(child->firstChild->firstChild->nextSibling->attribute, "main") == 0) {
                fprintf(fichier, "_start:\n");
                parcoursMain(child->firstChild->nextSibling, fichier, t, "main");
                fprintf(fichier, "    mov rax, 60\nsyscall\n");
                return;
            }
            else {
                fprintf(fichier, "%s:\n", child->firstChild->firstChild->nextSibling->attribute);
                parcoursMain(child->firstChild->nextSibling, fichier, t, child->firstChild->firstChild->nextSibling->attribute);
                fprintf(fichier, "    ret\n");
            }
        }
        traductionASM(child, fichier, t);
    }
}