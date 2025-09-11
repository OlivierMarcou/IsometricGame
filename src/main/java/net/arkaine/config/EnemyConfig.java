package net.arkaine.config;

import net.arkaine.combat.CombatSystem;

/**
 * Configuration centralisée pour le système d'ennemis
 */
public class EnemyConfig {

    // Paramètres de spawn initial
    public static final int MIN_PACKS = 3;
    public static final int MAX_PACKS = 5;
    public static final int MIN_PACK_SIZE = 2;
    public static final int MAX_PACK_SIZE = 4;

    public static final int MIN_SOLITARY = 5;
    public static final int MAX_SOLITARY = 10;

    public static final int MIN_ELITES = 2;
    public static final int MAX_ELITES = 4;

    public static final int MIN_GUARDIANS = 1;
    public static final int MAX_GUARDIANS = 3;

    public static final double BOSS_SPAWN_CHANCE = 0.15; // 15% de chance

    // Paramètres de respawn
    public static final int MIN_ENEMIES_BEFORE_RESPAWN = 5;
    public static final int RESPAWN_AMOUNT_MIN = 3;
    public static final int RESPAWN_AMOUNT_MAX = 6;
    public static final double RESPAWN_INTERVAL_SECONDS = 45.0;

    // Distances de sécurité
    public static final double MIN_SPAWN_DISTANCE_FROM_PLAYER = 5.0;
    public static final double MAX_SPAWN_ATTEMPTS = 50;
    public static final double GUARDIAN_HOUSE_SEARCH_RADIUS = 3.0;
    public static final double BOSS_ISOLATION_RADIUS = 3.0;

    // Paramètres de comportement IA
    public static final double PACK_COORDINATION_RADIUS = 10.0;
    public static final double GUARDIAN_LEASH_RANGE = 8.0;
    public static final double PATHFIND_UPDATE_INTERVAL = 0.5;

    // Distribution des classes d'ennemis
    public static final double[] BASIC_CLASS_WEIGHTS = {0.4, 0.3, 0.3}; // Warrior, Mage, Archer
    public static final double[] ELITE_CLASS_WEIGHTS = {0.4, 0.3, 0.3}; // Elite versions
    public static final double[] PACK_CLASS_WEIGHTS = {0.6, 0.4}; // Warrior, Archer (pas de mages en meute)

    // Modificateurs de difficulté selon le temps de jeu
    public static final double DIFFICULTY_SCALE_PER_MINUTE = 0.1;
    public static final double MAX_DIFFICULTY_MULTIPLIER = 2.0;

    /**
     * Calcule le multiplicateur de difficulté basé sur le temps de jeu
     */
    public static double getDifficultyMultiplier(double gameTimeMinutes) {
        double multiplier = 1.0 + (gameTimeMinutes * DIFFICULTY_SCALE_PER_MINUTE);
        return Math.min(multiplier, MAX_DIFFICULTY_MULTIPLIER);
    }

    /**
     * Ajuste les stats d'un ennemi selon la difficulté
     */
    public static void applyDifficultyScaling(CombatSystem.Stats stats, double difficultyMultiplier) {
        stats.maxHealth = (int)(stats.maxHealth * difficultyMultiplier);
        stats.health = stats.maxHealth;
        stats.damage = (int)(stats.damage * difficultyMultiplier);
        stats.moveSpeed *= Math.min(difficultyMultiplier, 1.5); // Limiter la vitesse
        stats.attackSpeed *= Math.min(difficultyMultiplier, 1.3); // Limiter la vitesse d'attaque
    }

    /**
     * Configuration des biomes pour spawn spécialisé
     */
    public static class BiomeSpawning {
        // Indices de sol pour différents biomes (selon RealisticMapGenerator)
        public static final int[] GRASS_FLOOR_INDICES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        public static final int[] DIRT_PATH_INDICES = {15, 16, 17, 18, 19};
        public static final int[] STONE_FLOOR_INDICES = {20, 21, 22, 23, 24};
        public static final int[] WATER_INDICES = {25, 26, 27, 28, 29};
        public static final int[] SAND_INDICES = {30, 31, 32, 33, 34};
        public static final int[] WOOD_FLOOR_INDICES = {35, 36, 37, 38, 39};

        /**
         * Détermine les classes d'ennemis appropriées pour un biome
         */
        public static CombatSystem.EnemyClass[] getPreferredEnemiesForFloor(int floorIndex) {
            if (isInArray(floorIndex, GRASS_FLOOR_INDICES)) {
                // Forêt/herbe : plus d'archers et de druides
                return new CombatSystem.EnemyClass[]{
                        CombatSystem.EnemyClass.ARCHER,
                        CombatSystem.EnemyClass.WARRIOR
                };
            } else if (isInArray(floorIndex, STONE_FLOOR_INDICES)) {
                // Pierre : plus de guerriers et mages
                return new CombatSystem.EnemyClass[]{
                        CombatSystem.EnemyClass.WARRIOR,
                        CombatSystem.EnemyClass.MAGE,
                        CombatSystem.EnemyClass.ELITE_WARRIOR
                };
            } else if (isInArray(floorIndex, WOOD_FLOOR_INDICES)) {
                // Maisons : gardiens et élites
                return new CombatSystem.EnemyClass[]{
                        CombatSystem.EnemyClass.ELITE_WARRIOR,
                        CombatSystem.EnemyClass.ELITE_MAGE
                };
            } else {
                // Défaut : classes basiques
                return new CombatSystem.EnemyClass[]{
                        CombatSystem.EnemyClass.WARRIOR,
                        CombatSystem.EnemyClass.MAGE,
                        CombatSystem.EnemyClass.ARCHER
                };
            }
        }

        private static boolean isInArray(int value, int[] array) {
            for (int item : array) {
                if (item == value) return true;
            }
            return false;
        }
    }

    /**
     * Configuration des événements spéciaux de spawn
     */
    public static class SpecialEvents {
        public static final double INVASION_CHANCE = 0.05; // 5% par cycle de respawn
        public static final int INVASION_ENEMY_COUNT = 8;
        public static final double INVASION_DURATION_SECONDS = 120.0;

        public static final double ELITE_PATROL_CHANCE = 0.15; // 15% par cycle
        public static final int ELITE_PATROL_SIZE = 3;

        public static final double BOSS_REINFORCEMENT_CHANCE = 0.3; // 30% quand boss présent
        public static final int BOSS_REINFORCEMENT_COUNT = 4;
    }

    /**
     * Loot et récompenses
     */
    public static class Rewards {
        // Chances de drop par classe d'ennemi
        public static final double BASIC_ENEMY_LOOT_CHANCE = 0.3;
        public static final double ELITE_ENEMY_LOOT_CHANCE = 0.6;
        public static final double BOSS_LOOT_CHANCE = 0.9;

        // Types de loot possibles
        public static final String[] BASIC_LOOT = {"coin", "potion", "herb"};
        public static final String[] ELITE_LOOT = {"gem", "scroll", "weapon_part", "armor_piece"};
        public static final String[] BOSS_LOOT = {"legendary_weapon", "rare_armor", "boss_key", "treasure_map"};

        // Expérience gagnée
        public static final int BASIC_ENEMY_XP = 10;
        public static final int ELITE_ENEMY_XP = 25;
        public static final int BOSS_XP = 100;
    }
}