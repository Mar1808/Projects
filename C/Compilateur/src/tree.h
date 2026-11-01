/* tree.h */

typedef enum {
  prog,
  listexp,
  declvars,
  declfoncts,
  declfonct,
  exp,
  arguments,
  ident,
  lvalue,
  tab,
  f,
  character,
  num,
  addsub,
  divstar,
  order,
  eq,
  neq,
  and,
  or,
  noop,
  instr,
  _if_,
  _else_,
  _while_,
  _return_,
  suiteinstr,
  corps,
  paramtab,
  listtypvar,
  parametres,
  _void_,
  entetefonct,
  declarateurs,
  type,
  tabAffect,
  affectation,
  inferieur,
  inferieurEgal,
  superieur,
  superieurEgal,
  multiplication,
  division,
  modulo,
  addition,
  soustraction,
  different,
  _main_,
  AffectationTab,
  fonction

  /* list all other node labels, if any */
  /* The list must coincide with the string array in tree.c */
  /* To avoid listing them twice, see https://stackoverflow.com/a/10966395 */
} label_t;

typedef struct Node {
  label_t label;
  char *attribute;
  int size;
  struct Node *firstChild, *nextSibling;
  int lineno;
  int columnno;
} Node;

Node *makeNode(label_t label);
void addSibling(Node *node, Node *sibling);
void addChild(Node *parent, Node *child);
void deleteTree(Node*node);
void printTree(Node *node);

#define FIRSTCHILD(node) node->firstChild
#define SECONDCHILD(node) node->firstChild->nextSibling
#define THIRDCHILD(node) node->firstChild->nextSibling->nextSibling
