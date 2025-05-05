# EnSkred

**EnSkred** est une application réseau décentralisée écrite en Java, utilisant RSA pour assurer la communication sécurisée entre plusieurs nœuds connectés. Elle permet l'affichage dynamique du graphe des connexions, la messagerie publique et cachée, ainsi qu'une gestion robuste des connexions et déconnexions.

---

## Contenu de l’archive à déposer

L’archive ZIP à remettre sur e-learning contient :

- Le **code source complet** du projet Java
- Le **JAR exécutable** nommé `enskred.jar` généré dans le dossier `target`
- Le **script de compilation** (Maven)
- Ce fichier **README.md**
- Le **rapport au format PDF**

---

## Compilation

### Prérequis

- Java 23 ou supérieur
- Maven 3.6+

### Génération du JAR


**Pour compiler et générer le JAR :**

```bash
mvn clean package
```

***Le fichier*** **enskred.jar** **sera généré automatiquement dans le dossier **target/**.***

### Lancement de l'application client

Pour démarrer un nœud, utilisez la commande suivante:

```bash
java -jar target/enskred.jar <port> [<port_suivi>]
```
- <port>: le port local à utiliser.
- <port_suivi>: optionnel, permet de se connecter à un autre nœud existant du réseau.


Exemple de démarrage d’un premier nœud:
```bash
java -jar target/enskred.jar 7777
```

Exemple de démarrage d’un second nœud se connectant au premier:
```bash
java -jar target/enskred.jar 7778 7777
```

&nbsp;

---
**En ce qui concerne le niveau de log de l'appli, il est comme désactivé, car il est élevé tel que "LEVEL = Level.Severe", il est possible de changer cette valeur et ainsi d'abaisser le niveau à "Level.[Info/Warning]" si nécessaire.
Il s'agit d'une variable statique dans la classe principale Application.**
---



&nbsp;



## Fonctionnalités de l'application


Pour chaque fonctionnalité, les exemples de commande fournie suivent cette topologie.

##### Légende:
&nbsp;
```txt
    A représente PublicKeyRSA@5604d1d4 : /0.0.0.0:7780
    B représente PublicKeyRSA@a03215a7 : /0.0.0.0:7778
    C représente PublicKeyRSA@f7bd7a0b : /0.0.0.0:7779
    D représente PublicKeyRSA@df9cf66b : /0.0.0.0:7777
    E représente PublicKeyRSA@9962373d : /0.0.0.0:7781
```
```txt
        +-----------------------------------+
        | A: PublicKeyRSA@5604d1d4          |
        |    /0.0.0.0:7780                  |
        +-----------------------------------+
                  |         |         |
       /----------          |          ----------\
      /                     |                     \
     |                      |                      |
+----------------+     +----------------+     +-----------------+
| B: PublicKeyRSA|     | C: PublicKeyRSA|     | E: PublicKeyRSA |
|    @f7bd7a0b   |-----|    @a03215a7   |-----|     @9962373d   |
|  /0.0.0.0:7779 |     |  /0.0.0.0:7778 |     |  /0.0.0.0:7781  |
+----------------+     +----------------+     +-----------------+
            |               |
            |               |   
            |               |
        +-----------------------------------+
        | D: PublicKeyRSA@df9cf66b          |
        |    /0.0.0.0:7777                  |
        +-----------------------------------+
```

---

### Affichage des connexions (topologie) du réseau:

Utilisez la commande **"-r"** sur le terminal de l'application.

```sh
-r

--- Affichage des connexions du réseau ---
PublicKeyRSA@5604d1d4: /0.0.0.0:7780
        -> PublicKeyRSA@a03215a7: /0.0.0.0:7778
        -> PublicKeyRSA@f7bd7a0b: /0.0.0.0:7779
        -> PublicKeyRSA@9962373d: /0.0.0.0:7781

PublicKeyRSA@a03215a7: /0.0.0.0:7778
        -> PublicKeyRSA@5604d1d4: /0.0.0.0:7780
        -> PublicKeyRSA@f7bd7a0b: /0.0.0.0:7779
        -> PublicKeyRSA@df9cf66b: /0.0.0.0:7777
        -> PublicKeyRSA@9962373d: /0.0.0.0:7781

PublicKeyRSA@f7bd7a0b: /0.0.0.0:7779
        -> PublicKeyRSA@5604d1d4: /0.0.0.0:7780
        -> PublicKeyRSA@a03215a7: /0.0.0.0:7778
        -> PublicKeyRSA@df9cf66b: /0.0.0.0:7777

PublicKeyRSA@df9cf66b: /0.0.0.0:7777
        -> PublicKeyRSA@a03215a7: /0.0.0.0:7778
        -> PublicKeyRSA@f7bd7a0b: /0.0.0.0:7779

PublicKeyRSA@9962373d: /0.0.0.0:7781
        -> PublicKeyRSA@5604d1d4: /0.0.0.0:7780
        -> PublicKeyRSA@a03215a7: /0.0.0.0:7778

------------------------------------------
```

---


### Liste des utilisateurs connectés:

Utilisez la commande **"-l"** sur le terminal de l'application.

```sh
-l

Voici les utilisateurs connectés :
ID: 0 -> PublicKey: PublicKeyRSA@5604d1d4
ID: 1 -> PublicKey: PublicKeyRSA@a03215a7
ID: 2 -> PublicKey: PublicKeyRSA@f7bd7a0b
ID: 3 -> PublicKey: PublicKeyRSA@df9cf66b
ID: 4 -> PublicKey: PublicKeyRSA@9962373d
```

---

### Demande d'aide par commande help:

Utilisez la commande **"-h"** afin de connaître l'ensemble des commandes disponibles de l'application.

```txt
-----------------------------------------------------------------
Liste des commandes disponibles :
-mp <ID> <message> : Envoi un message public au client avec l'ID spécifié.
-mc <ID> <message> : Envoi un message privé caché au client avec l'ID spécifié.
-l               : Demande la liste des utilisateurs connectés.
-d               : Demande de déconnexion.
-r               : Demande l'état du réseau.
-h               : Obtenir les commandes disponible.

Exemple de commande :
-mp 2 Bonjour !  : Envoi un message public à l'utilisateur ayant l'ID 2.
-mc 3 Salut !    : Envoi un message privé caché à l'utilisateur ayant l'ID 3.
-----------------------------------------------------------------
```

---


### Messagerie publique:

Utilisez la commande **"-mp <id> <message>"**, tel que l'id correspond à l'id disponible à partir d'un appel à la commande "-l". Et message le message que l'on souhaite envoyer.

Exemple du côté de l'expéditeur:

```txt
-mp 3 Salut, c'est ton voisin d'en face !
```

Réception côté destinataire:

```txt
***** Nouveau message public reçu *****
Expéditeur: fr.uge.enskred.readers.UGEncrypt$PublicKeyRSA@9962373d
Message: Salut, c'est ton voisin d'en face !
***************************************
```
---

### Messagerie cachée:

Utilisez la commande **"-mc <id> <message>"**, tel que l'id correspond à l'id disponible à partir d'un appel à la commande "-l". Et message le message que l'on souhaite envoyer.

Exemple du côté de l'expéditeur:

```txt
-mc 3 Salut, c'est ton collègue d'en face !
```

Réception du côté du destinataire:

```txt
***** Nouveau message caché reçu *****
Expéditeur: PublicKeyRSA@9962373d
Message: Salut, c'est ton collègue d'en face !
Reçu en 93 ms
**************************************
```

l'ID du message est le timestamp, il est donc par définition unique, car on ne peut pas envoyer de message au même moment, à moins d'être plus rapide que le son et la lumière ou bien de trafiquer l'application pour déjouer cela.
Reçu correspond alors au temps que met le message de l'envoyeur vers le receveur.


De nouveau côté envoyeur:

```txt
***** Accusé de réception suite au message caché *****
Expéditeur: PublicKeyRSA@df9cf66b
ID de l'accusé de réception : 1746183046920
Retransmis en 119 ms
******************************************************
```

Retransmis correspond au temps qu'a mis le message caché à partir vers le destinataire, puis à revenir pour l'accusé de réception.

---

### Déconnexion:

Utilisez la commande **"-d"** pour vous déconnecter.
Vos connexions seront envoyées à une application choisie au hasard afin qu'elles reprennent le relais.
S'ensuivra ensuite d'un broadcast REMOVE_NODE, qui fera en sorte que chaque application retire l'application ayant initié la déconnexion afin qu'elle soit oubliée de tous.

---

### Déconnexion brutale:

L'application présente également une logique de déconnexion brutale dans une certaine mesure.
Prenons l'exemple qu'une application tombe.
Les autres applications connectées à celle-ci détectent que la key associée à un contexte ('Context') est annulée pendant un appel à la méthode 'interestOps()', on envoie alors à ses propres voisins les plus proches un broadcast REMOVE_NODE, afin que tout le monde le sache.
Malheureusement, contrairement à une déconnexion sécurisée, présentée ci-dessus, la personne qui se déconnecte brutalement ne propage pas ses connexions.
---
