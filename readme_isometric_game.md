# Jeu Isométrique Diablo-like

Un jeu vidéo en vue isométrique inspiré de Diablo, développé en Java avec JavaFX. Le jeu propose une expérience de type action-RPG avec système de combat en temps réel, génération procédurale de cartes, et système d'inventaire complet.

![Version](https://img.shields.io/badge/version-1.0--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.1-green)

## 🎮 Fonctionnalités

### ✨ Système de jeu
- **Vue isométrique 3D** avec rendu optimisé
- **Combat en temps réel** avec système d'IA avancé
- **Génération procédurale** de cartes de village avec forêts
- **Système d'inventaire** et d'équipement RPG
- **Pathfinding intelligent** avec support des mouvements diagonaux
- **Système de portes** interactives avec clés

### ⚔️ Combat
- **Multiple classes d'ennemis** : Guerriers, Mages, Archers, Élites, Boss
- **Système de projectiles** avec différents types de dégâts
- **IA comportementale** : meutes, solitaires, gardiens
- **Événements spéciaux** : invasions, patrouilles d'élites
- **Système de résistances** et multiplicateurs de dégâts
- **Escalade de difficulté** basée sur le temps de jeu

### 🏰 Monde
- **Villages générés** avec maisons, chemins et forêts
- **Différents biomes** : herbe, pierre, sable, eau
- **Objets interactifs** : portes, coffres, objets au sol
- **Système de clés** et portes verrouillées
- **Mini-carte** en temps réel

## 🛠️ Technologies

- **Java 21** - Langage principal
- **JavaFX 21.0.1** - Interface utilisateur et rendu graphique
- **Maven** - Gestion des dépendances et build
- **Architecture MVC** - Séparation claire des responsabilités

## 📦 Installation

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+
- JavaFX 21.0.1

### Compilation
```bash
# Cloner le projet
git clone <url-du-projet>
cd Isometric-Game

# Compiler avec Maven
mvn clean compile

# Lancer le jeu
mvn javafx:run
```

### Génération des ressources (optionnel)
```bash
# Générer les images isométriques
java -cp target/classes net.arkaine.IsometricImageGenerator2

# Corriger les noms de fichiers d'images
java -cp target/classes net.arkaine.ImageFileNameCorrector

# Générer une nouvelle carte de village
java -cp target/classes net.arkaine.RealisticMapGenerator
```

## 🎯 Contrôles

| Action | Contrôle |
|--------|----------|
| **Déplacement** | Clic gauche sur une case |
| **Attaque** | Clic gauche sur un ennemi |
| **Ramasser objets** | Clic droit sur une case avec objets |
| **Ouvrir/Fermer portes** | Clic sur une porte adjacente |
| **Recentrer caméra** | `C` |
| **Inventaire** | `I` |
| **Équipement** | `E` |
| **Plein écran** | `F11` |

### Commandes de debug
| Commande | Action |
|----------|--------|
| `H` | Afficher la santé du joueur |
| `R` | Forcer le respawn d'ennemis |
| `K` | Éliminer tous les ennemis |
| `B` | Spawner un boss |
| `V` | Déclencher une invasion |
| `S` | Afficher les statistiques de combat |
| `F1` | Afficher l'aide complète |

## 🏗️ Architecture

Le projet suit une architecture MVC stricte :

```
src/main/java/net/arkaine/
├── IsometricGameMVC.java          # Application principale
├── model/
│   └── GameModel.java             # Logique métier et données
├── view/
│   └── GameView.java              # Rendu et affichage
├── controller/
│   └── GameController.java        # Gestion des interactions
├── combat/
│   ├── CombatSystem.java          # Système de combat
│   └── CombatEventsManager.java   # Événements spéciaux
├── inventory/
│   └── InventorySystem.java       # Système d'inventaire
├── config/
│   └── EnemyConfig.java           # Configuration des ennemis
└── utils/
    ├── RealisticMapGenerator.java # Génération de cartes
    ├── IsometricImageGenerator2.java # Génération d'images
    └── ImageFileNameCorrector.java # Utilitaires d'images
```

## 🎨 Système graphique

### Images générées
Le jeu utilise un système de génération d'images PNG :
- **Sols** : 50 variations (herbe, pierre, sable, bois)
- **Murs** : 50 variations (bois, brique, verre, portes)
- **Plafonds** : 30 variations (chaume, tuiles, feuillage)

### Rendu isométrique
- Tiles 64x32 pixels (sols et plafonds)
- Murs 64x128 pixels avec transparence
- Tri en profondeur automatique
- Culling d'optimisation

## ⚡ Systèmes avancés

### IA des ennemis
- **Pathfinding A\*** avec support diagonal
- **États comportementaux** : patrouille, poursuite, attaque
- **Coordination de meute** pour les groupes
- **Système d'aggro** intelligent

### Génération procédurale
- **Villages réalistes** avec maisons et chemins
- **Forêts dynamiques** avec densité variable
- **Placement d'objets** contextuel selon les biomes
- **Équilibrage automatique** des ressources

### Système de combat
- **Types de dégâts** : Physique, Feu, Glace, Poison, Foudre
- **Résistances** par classe d'ennemi
- **Projectiles** avec physique et effets visuels
- **Événements aléatoires** (invasions, boss)

## 📊 Configuration

### Paramètres d'ennemis (EnemyConfig.java)
```java
// Spawn initial
MIN_PACKS = 3;                    // Minimum de meutes
MAX_PACKS = 5;                    // Maximum de meutes
BOSS_SPAWN_CHANCE = 0.15;         // Chance d'apparition de boss

// Difficulté
DIFFICULTY_SCALE_PER_MINUTE = 0.1; // Montée en difficulté
MAX_DIFFICULTY_MULTIPLIER = 2.0;   // Multiplicateur maximum
```

### Taille de la carte
```java
public static final int MAP_SIZE = 50; // Taille 50x50 par défaut
```

## 🐛 Debug et développement

### Mode debug
Définir la propriété système pour activer les informations de debug :
```bash
java -Ddebug.combat=true -cp target/classes net.arkaine.IsometricGameMVC
```

### Logs utiles
Le jeu produit des logs détaillés pour :
- Chargement des cartes JSON
- Génération d'ennemis
- États de combat
- Événements spéciaux

## 🗂️ Ressources

### Structure des ressources
```
src/main/resources/
├── sol/           # Images de sols (floor_0.png à floor_49.png)
├── murs/          # Images de murs (wall_0.png à wall_49.png)
├── plafonds/      # Images de plafonds (ceiling_0.png à ceiling_29.png)
└── village_map.json # Carte sauvegardée
```

### Format de carte JSON
Les cartes sont sauvegardées en JSON avec :
- `floorMap` : Indices des textures de sol
- `wallMap` : Indices des textures de mur  
- `wallTypes` : Types de murs (DOOR, DESTRUCTIBLE, etc.)
- `wallProperties` : Propriétés (verrouillage, santé)
- `itemMap` : Objets présents sur chaque case

## 🔄 Cycle de développement

1. **Génération de contenu** : Utiliser les générateurs pour créer cartes et images
2. **Test** : Lancer avec `mvn javafx:run`
3. **Debug** : Utiliser les commandes de debug intégrées
4. **Itération** : Modifier la configuration et régénérer

## 🚀 Extensions possibles

- Système d'expérience et de niveaux
- Plus de classes de personnages
- Crafting et alchimie
- Donjons générés procéduralement
- Mode multijoueur
- Sauvegarde de progression
- Sons et musiques
- Animations d'attaque avancées

## 🤝 Contribution

Le projet est structuré pour faciliter les contributions :
- Code organisé en modules clairs
- Configuration externalisée
- Système de debug intégré
- Architecture MVC respectée

## 📝 Licence

Ce projet est un prototype de démonstration. Consultez les licences des dépendances JavaFX.

---

**Note technique** : Ce jeu utilise la programmation orientée objet avancée avec des patterns comme Observer, Strategy et Factory pour une architecture maintenable et extensible.