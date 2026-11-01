# Projet RUGE - Site de partage de recettes culinaires

## Équipe du Projet
* **HONORE Mathieu** - mathieu.honore@edu.univ-eiffel.fr
* **CAMARA William** - william.camara@edu.univ-eiffel.fr
* **KAOUANE Marwane** - marwane.kaouane@edu.univ-eiffel.fr
* **NEJI Wadi** - wadi.neji@edu.univ-eiffel.fr
* **ZERROU Hamza** - hamza.zerrou@edu.univ-eiffel.fr

---

## À quoi sert ce projet ?

Ruge est une plateforme innovante de partage de recettes de cuisine intégrant un modèle économique unique et une recherche par Intelligence Artificielle. Le projet répond aux besoins suivants :
* **Création et Variantes :** Les utilisateurs peuvent poster des recettes de base ou créer des "variantes" de recettes existantes (ex: modification d'ingrédients ou de temps de cuisson).
* **Monétisation et Redistribution :** Un système de "Super-Likes" (payants via Stripe) permet de rémunérer les auteurs. Les gains sont redistribués récursivement entre la recette likée et ses recettes parentes (60/40).
* **Recherche Intelligente (LLM) :** Un moteur de recherche basé sur un LLM local permet aux utilisateurs de trouver des recettes en langage naturel (ex: "un dessert sans gluten pour 4 personnes").
* **Administration :** Un panel d'administration dédié permet de gérer les utilisateurs, modérer les recettes, suivre les portefeuilles virtuels et planifier des promotions commerciales.

---

## Comment l'utiliser ? (Installation et Lancement)

### Prérequis
* Java 17+
* Maven 3.8+
* Une base de données (ex: PostgreSQL configurée et lancée en local ou Supabase).
* Un LLM local accessible (ex: Ollama) pour le module Spring AI.

### Configuration
1. Clonez le dépôt : `git clone https://gitlab.com/JustHNR/ruge.git`
2. Ouvrez le fichier `src/main/resources/application.yml` et y ajoutez les configurations adéquates :

### Lancement du Backend et du Client Léger (Spring MVC)
À la racine du projet, exécutez les commandes suivantes :
```bash
mvn clean install
mvn spring-boot:run
```

L'application web (client léger) sera accessible sur : http://localhost:8080

### Lancement du Client Lourd (JavaFX)
1. Ouvrez le projet dans votre IDE (ex: IntelliJ IDEA).
2. Exécutez la classe `src/main/java/fr/uge/project/ruge/RugeApplication.java` pour lancer le client lourd JavaFX.
3. Le client lourd se connectera automatiquement au backend Spring MVC pour récupérer les données.
4. L'interface du client lourd vous permettra de naviguer dans les recettes, créer des variantes, et interagir avec les fonctionnalités de monétisation et de recherche intelligente.

### Limitations connues
On ne peut pas consulter toutes les recettes dans la partie Admin.

### Bugs non résolus
Non intégration de stripe lié a un problème de base URL
