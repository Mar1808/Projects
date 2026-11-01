/* tree.c */
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "tree.h"
extern int lineno;       /* from lexer */
extern int columnno;     /* from lexer */

static const char *StringFromLabel[] = {
  "prog",
  "listexp",
  "declvars",
  "declfoncts",
  "declfonct",
  "exp",
  "arguments",
  "ident",
  "lvalue",
  "tab",
  "f",
  "character",
  "num",
  "addsub",
  "divstar",
  "order",
  "==",
  "!=",
  "&&",
  "||",
  "",
  "instr",
  "if",
  "else",
  "while",
  "return",
  "suiteinstr",
  "corps",
  "paramtab",
  "listtypvar",
  "parametres",
  "void",
  "entetefonct",
  "declarateurs",
  "type",
  "tabAffect",
  "=",
  "<",
  "<=",
  ">",
  ">=",
  "*",
  "/",
  "%",
  "+",
  "-",
  "!",
  "main",
  "AffectionTab",
  "fonction"
  /* list all other node labels, if any */
  /* The list must coincide with the label_t enum in tree.h */
  /* To avoid listing them twice, see https://stackoverflow.com/a/10966395 */
};

Node *makeNode(label_t label) {
  Node *node = malloc(sizeof(Node));
  if (!node) {
    printf("Run out of memory\n");
    exit(1);
  }
  node->label = label;
  node->attribute = NULL;
  node-> firstChild = node->nextSibling = NULL;
  node->lineno=lineno;
  node->columnno=columnno;
  node->size=-1;
  return node;
}

void addSibling(Node *node, Node *sibling) {
  Node *curr = node;
  while (curr->nextSibling != NULL) {
    curr = curr->nextSibling;
  }
  curr->nextSibling = sibling;
}

void addChild(Node *parent, Node *child) {
  if (parent->firstChild == NULL) {
    parent->firstChild = child;
  }
  else {
    addSibling(parent->firstChild, child);
  }
}

void deleteTree(Node *node) {
  if (node->firstChild) {
    deleteTree(node->firstChild);
  }
  if (node->nextSibling) {
    deleteTree(node->nextSibling);
  }
  free(node);
}

void printTree(Node *node) {
    static bool rightmost[128]; 
    static int depth = 0;       
    for (int i = 1; i < depth; i++) { 
      printf(rightmost[i] ? "    " : "\u2502   ");
    }
    if (depth > 0) { 
      printf(rightmost[depth] ? "\u2514\u2500\u2500 " : "\u251c\u2500\u2500 ");
    }
    if(node->attribute != NULL) 
      printf("%s : %s", StringFromLabel[node->label], node->attribute);
    else 
      printf("%s", StringFromLabel[node->label]);
    printf("\n");
    depth++;
    for (Node *child = node->firstChild; child != NULL; child = child->nextSibling) {
      rightmost[depth] = (child->nextSibling) ? false : true;
      printTree(child);
    }
    depth--;
}