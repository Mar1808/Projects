# 📖 Documentation Utilisateur — Application RUGE

> RUGE est une application mobile Android de partage et découverte de recettes de cuisine. Elle vous permet de publier vos propres recettes, d'interagir avec celles des autres utilisateurs, et de gérer votre portefeuille intégré.

---

## Sommaire

1. [Navigation générale](#1-navigation-générale)
2. [Créer un compte / Se connecter](#2-créer-un-compte--se-connecter)
3. [Parcourir les recettes](#3-parcourir-les-recettes)
4. [Consulter une recette](#4-consulter-une-recette)
5. [Créer une recette](#5-créer-une-recette)
6. [Créer une variante de recette](#6-créer-une-variante-de-recette)
7. [Mon compte](#7-mon-compte)
8. [Mon portefeuille](#8-mon-portefeuille)
9. [Espace Administration (Admins)](#9-espace-administration-admins)

---

## 1. Navigation générale

L'application est structurée autour de deux éléments de navigation permanents.

### En-tête (Header)

La barre en haut de l'écran affiche le logo et le nom **RUGE**. Si vous êtes connecté, une icône de compte apparaît à droite — appuyez dessus pour accéder directement à votre profil.

### Barre de navigation inférieure (Footer)

La barre du bas comporte trois boutons :

| Icône | Destination |
|-------|-------------|
| 🧁 Muffin | Accueil — liste des recettes |
| 🔑 Connexion | Page de connexion |
| 👤 Inscription | Page de création de compte |

Le bouton correspondant à la page active est mis en surbrillance.

---

## 2. Créer un compte / Se connecter

### Se connecter

1. Appuyez sur l'icône **Connexion** dans la barre du bas.
2. Saisissez votre **adresse e-mail** et votre **mot de passe**.
3. Appuyez sur **Valider la connexion**.

Si vos identifiants sont corrects, vous serez redirigé vers l'écran d'accueil et recevrez une notification de confirmation. En cas d'erreur, un message s'affiche sous les champs pour vous en informer.

> **Note :** L'application peut vous demander l'autorisation d'envoyer des notifications lors de votre première connexion. Acceptez pour recevoir les confirmations.

### S'inscrire

Appuyez sur l'icône **Inscription** dans la barre du bas pour créer un nouveau compte.

---

## 3. Parcourir les recettes

L'écran d'accueil (🧁) affiche la liste des recettes disponibles sur la plateforme. Appuyez sur une recette pour en voir le détail.

---

## 4. Consulter une recette

La page d'une recette présente toutes les informations nécessaires pour la réaliser et interagir avec elle.

### Informations affichées

- **Photo** de la recette (en haut de l'écran)
- **Nom** de la recette
- **Durée totale**, nombre de **likes** et nom du **créateur**
- **Badge « Variante »** si la recette est dérivée d'une autre (appuyez dessus pour voir la recette originale)
- **Description**, liste des **ingrédients** et **étapes de préparation**
- **Avis et commentaires** laissés par les autres utilisateurs

### Actions disponibles (utilisateurs connectés)

> Ces actions ne sont disponibles que si vous êtes connecté **et** que vous n'êtes pas le créateur de la recette.

| Action | Description |
|--------|-------------|
| **J'aime** | Ajouter ou retirer un like sur la recette |
| **Suivre / Suivi** | S'abonner aux mises à jour de la recette |
| **Super Like** | Exprimer un enthousiasme fort (action payante selon le prix affiché, irréversible) |
| **Créer une variante** | Partir de cette recette pour en créer une version personnalisée |

#### Le Super Like

Lorsque vous appuyez sur **Super Like**, une fenêtre de confirmation s'ouvre indiquant le prix. Si la recette est offerte, le prix affiché est **0 € (Offert !)**. Confirmez ou annulez. Cette action débite votre portefeuille et **ne peut pas être annulée**.

### Laisser un avis

En bas de la page recette, un champ de texte vous permet de rédiger un commentaire. Appuyez sur **Publier l'avis** pour le soumettre. Votre avis apparaît ensuite dans la liste avec votre nom et la date.

---

## 5. Créer une recette

Accessible depuis **Mon Compte → Créer une recette**.

### Étapes

1. **Photo** : Appuyez sur la zone photo pour prendre une photo avec l'appareil photo de votre téléphone. (Optionnel — une URL d'image peut aussi être fournie.)
2. **Nom** : Donnez un titre à votre recette.
3. **Description** : Rédigez une présentation de la recette.
4. **URL d'image** *(optionnel)* : Si vous avez déjà une image en ligne, collez son adresse ici.
5. **Durée totale** : Indiquez la durée en minutes.
6. **Ingrédients** : Renseignez le nom et la quantité, puis appuyez sur **Add ingredient**. Répétez pour chaque ingrédient. Appuyez sur **Remove** pour en supprimer un.
7. **Étapes** : Rédigez chaque étape avec sa durée, puis appuyez sur **Add step**. Répétez pour chaque étape. Appuyez sur **Remove** pour en supprimer une.
8. Appuyez sur **Create recipe** pour publier.

> Vous devez être connecté pour publier une recette. Au minimum un ingrédient et une étape sont requis.

---

## 6. Créer une variante de recette

Accessible depuis la page d'une recette → bouton **Créer une variante**.

Une variante est une version modifiée d'une recette existante. Vous partez des ingrédients et étapes de l'original, et vous les adaptez à votre guise.

### Ce que vous pouvez faire

- **Modifier le titre** et la **description** de la variante.
- **Ingrédients :**
    - Modifier le texte ou la quantité d'un ingrédient existant.
    - **Supprimer** un ingrédient original (il apparaîtra barré). Appuyez sur **Restaurer** pour l'annuler.
    - **Ajouter** de nouveaux ingrédients.
- **Étapes :**
    - **Modifier** le texte ou la durée d'une étape existante directement dans le champ.
    - **Réordonner** les étapes avec les boutons **Monter** et **Descendre**.
    - **Supprimer** une étape originale (appuyez sur **Restaurer** pour annuler).
    - **Ajouter** de nouvelles étapes.

### Soumettre

Une fois satisfait, appuyez sur **Créer la variante**. Vous serez redirigé vers votre profil.

> Vous devez être connecté. Il est obligatoire de conserver au moins un ingrédient et une étape non supprimés.

---

## 7. Mon compte

Accessible via l'icône de compte dans l'en-tête ou via le menu de navigation.

### Que trouve-t-on sur cette page ?

#### Carte de profil

Affiche un résumé de votre activité :
- Nombre de **recettes** publiées
- Nombre de **likes** reçus
- Nombre d'**avis** reçus
- Votre **solde** de portefeuille

#### Mon portefeuille

Un raccourci vers la gestion de votre portefeuille (voir section suivante). Appuyez sur la bannière **Mon Portefeuille** pour y accéder.

#### Mes recettes publiées

La liste de toutes vos recettes. Appuyez sur une recette pour en voir le détail. Le bouton **Créer une recette** permet d'en publier une nouvelle.

#### Se déconnecter

Le bouton **Se déconnecter** en bas de page met fin à votre session et vous redirige vers l'écran de connexion.

---

## 8. Mon portefeuille

Accessible depuis **Mon Compte → Mon Portefeuille**.

Cette section vous permet de gérer votre solde utilisé pour les Super Likes et autres fonctionnalités payantes.

### Solde actuel

Le solde disponible sur votre compte est affiché en haut de la page, en euros.

### Recharger votre portefeuille

1. Saisissez le montant souhaité dans le champ **Montant (EUR)**.
2. Appuyez sur **Recharger votre wallet**.
3. Vous serez redirigé vers une page de paiement sécurisée (Stripe) dans votre navigateur.

### Retirer des fonds

1. Saisissez le montant à retirer dans le champ **Montant à retirer (EUR)**.
2. Appuyez sur **Retirer vers mon compte bancaire**.
3. La transaction est traitée vers votre compte bancaire enregistré.

> En cas d'erreur ou de succès, un message de confirmation s'affiche brièvement à l'écran.

---

## 9. Espace Administration (Admins)

Si votre compte dispose des droits **administrateur**, une bannière **Espace Administration** apparaît sur la page Mon Compte. Appuyez dessus pour accéder au tableau de bord d'administration permettant de gérer les contenus et les utilisateurs de la plateforme.

---

*Documentation pour l'application RUGE — Université Gustave Eiffel.*