#include <stdio.h>

int cube(int *p){
   int a = *p;
   printf("%d", a*a*a);
   return 1;
}
int main(){
    cube(-3);
    return 1;
}
