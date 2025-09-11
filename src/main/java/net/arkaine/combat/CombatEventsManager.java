package net.arkaine.combat;

import net.arkaine.config.EnemyConfig;
import net.arkaine.model.GameModel;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * Gestionnaire d'événements spéciaux et d'améliorations du système de combat
 */
public class CombatEventsManager {

    private GameModel gameModel;
    private CombatSystem combatSystem;
    private Random random = new Random();

    // État des événements
    private boolean invasionActive = false;
    private double invasionEndTime = 0;
    private double gameStartTime = System.currentTimeMillis() / 1000.0;
    private double lastEventCheck = 0;

    public CombatEventsManager(GameModel gameModel, CombatSystem combatSystem) {
        this.gameModel = gameModel;
        this.combatSystem = combatSystem;
    }

    /**
     * Met à jour les événements de combat (appelé depuis GameModel)
     */
    public void update(double currentTime) {
        // Vérifier les événements toutes les 30 secondes
        if (currentTime - lastEventCheck >= 30.0) {
            lastEventCheck = currentTime;
            checkForSpecialEvents(currentTime);
        }

        // Gérer l'invasion en cours
        if (invasionActive && currentTime >= invasionEndTime) {
            endInvasion();
        }
    }

    /**
     * Vérifie et déclenche les événements spéciaux
     */
    private void checkForSpecialEvents(double currentTime) {
        double gameTimeMinutes = (currentTime - gameStartTime) / 60.0;

        // Plus le jeu dure, plus les événements sont probables
        double eventChanceMultiplier = 1.0 + (gameTimeMinutes * 0.1);

        // Invasion d'ennemis
        if (!invasionActive && random.nextDouble() < EnemyConfig.SpecialEvents.INVASION_CHANCE * eventChanceMultiplier) {
            startInvasion(currentTime);
        }

        // Patrouille d'élites
        if (random.nextDouble() < EnemyConfig.SpecialEvents.ELITE_PATROL_CHANCE * eventChanceMultiplier) {
            spawnElitePatrol();
        }

        // Renforts de boss
        if (isBossAlive() && random.nextDouble() < EnemyConfig.SpecialEvents.BOSS_REINFORCEMENT_CHANCE) {
            spawnBossReinforcements();
        }
    }

    /**
     * Démarre une invasion d'ennemis
     */
    private void startInvasion(double currentTime) {
        invasionActive = true;
        invasionEndTime = currentTime + EnemyConfig.SpecialEvents.INVASION_DURATION_SECONDS;

        System.out.println("🚨 INVASION! Une horde d'ennemis attaque!");
        gameModel.setMessageAbovePlayer("INVASION!");

        // Spawner des ennemis autour du joueur
        Point2D playerPos = gameModel.getPlayerPosition();

        for (int i = 0; i < EnemyConfig.SpecialEvents.INVASION_ENEMY_COUNT; i++) {
            Point2D spawnPos = findInvasionSpawnPosition(playerPos);
            if (spawnPos != null) {
                CombatSystem.EnemyClass enemyClass = getRandomInvasionEnemyClass();
                combatSystem.spawnSolitaryEnemy(spawnPos, enemyClass);
            }
        }
    }

    /**
     * Termine l'invasion
     */
    private void endInvasion() {
        invasionActive = false;
        System.out.println("✅ L'invasion est terminée!");
        gameModel.setMessageAbovePlayer("Invasion repelled!");

        // Récompenser le joueur s'il a survécu
        rewardInvasionSurvival();
    }

    /**
     * Spawne une patrouille d'élites
     */
    private void spawnElitePatrol() {
        System.out.println("⚔️ Une patrouille d'élites apparaît!");

        Point2D patrolCenter = findSafeSpawnPosition();
        if (patrolCenter != null) {
            for (int i = 0; i < EnemyConfig.SpecialEvents.ELITE_PATROL_SIZE; i++) {
                double angle = (2 * Math.PI * i) / EnemyConfig.SpecialEvents.ELITE_PATROL_SIZE;
                double radius = 2.0;
                Point2D spawnPos = new Point2D(
                        patrolCenter.getX() + Math.cos(angle) * radius,
                        patrolCenter.getY() + Math.sin(angle) * radius
                );

                CombatSystem.EnemyClass eliteClass = getRandomEliteClass();
                combatSystem.spawnSolitaryEnemy(spawnPos, eliteClass);
            }
        }
    }

    /**
     * Spawne des renforts pour le boss
     */
    private void spawnBossReinforcements() {
        System.out.println("👹 Le boss appelle des renforts!");

        CombatSystem.Entity boss = findBoss();
        if (boss != null) {
            for (int i = 0; i < EnemyConfig.SpecialEvents.BOSS_REINFORCEMENT_COUNT; i++) {
                double angle = (2 * Math.PI * i) / EnemyConfig.SpecialEvents.BOSS_REINFORCEMENT_COUNT;
                double radius = 4.0;
                Point2D spawnPos = new Point2D(
                        boss.position.getX() + Math.cos(angle) * radius,
                        boss.position.getY() + Math.sin(angle) * radius
                );

                if (gameModel.isValidTile((int)spawnPos.getX(), (int)spawnPos.getY()) &&
                        gameModel.canWalkThrough((int)spawnPos.getX(), (int)spawnPos.getY())) {

                    CombatSystem.EnemyClass reinforcementClass = getRandomBasicEnemyClass();
                    combatSystem.spawnSolitaryEnemy(spawnPos, reinforcementClass);
                }
            }
        }
    }

    /**
     * Applique l'escalade de difficulté aux nouveaux ennemis
     */
    public void applyDifficultyScaling(CombatSystem.Entity enemy) {
        double gameTimeMinutes = (System.currentTimeMillis() / 1000.0 - gameStartTime) / 60.0;
        double difficultyMultiplier = EnemyConfig.getDifficultyMultiplier(gameTimeMinutes);

        if (difficultyMultiplier > 1.0) {
            EnemyConfig.applyDifficultyScaling(enemy.stats, difficultyMultiplier);
            System.out.println("📈 Difficulté appliquée: x" + String.format("%.1f", difficultyMultiplier));
        }
    }

    /**
     * Gère les récompenses quand un ennemi meurt
     */
    public void handleEnemyDeath(CombatSystem.Entity enemy, Point2D deathPosition) {
        // Chances de loot selon la classe
        double lootChance = getLootChance(enemy.entityClass);

        if (random.nextDouble() < lootChance) {
            spawnLoot(deathPosition, enemy.entityClass);
        }

        // Expérience (si système d'XP implémenté)
        int xpGained = getXpReward(enemy.entityClass);
        System.out.println("💰 +" + xpGained + " XP pour avoir vaincu " + enemy.entityClass);
    }

    /**
     * Génère du loot à la position de mort
     */
    private void spawnLoot(Point2D position, CombatSystem.EnemyClass enemyClass) {
        String[] lootTable = getLootTable(enemyClass);
        String lootType = lootTable[random.nextInt(lootTable.length)];
        int count = 1 + random.nextInt(3);

        int x = (int)position.getX();
        int y = (int)position.getY();

        if (gameModel.isValidTile(x, y)) {
            gameModel.dropItemAt(x, y, lootType, count);
            System.out.println("💎 Loot: " + lootType + " x" + count);
        }
    }

    /**
     * Récompense pour avoir survécu à une invasion
     */
    private void rewardInvasionSurvival() {
        Point2D playerPos = gameModel.getPlayerPosition();
        int x = (int)playerPos.getX();
        int y = (int)playerPos.getY();

        // Récompenses spéciales
        gameModel.dropItemAt(x, y, "invasion_trophy", 1);
        gameModel.dropItemAt(x + 1, y, "rare_gem", 3);
        gameModel.dropItemAt(x, y + 1, "experience_scroll", 1);

        System.out.println("🏆 Récompenses d'invasion reçues!");
    }

    // Méthodes utilitaires

    private Point2D findInvasionSpawnPosition(Point2D playerPos) {
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 6 + random.nextDouble() * 4; // 6-10 cases du joueur

            int x = (int)(playerPos.getX() + Math.cos(angle) * distance);
            int y = (int)(playerPos.getY() + Math.sin(angle) * distance);

            if (gameModel.isValidTile(x, y) && gameModel.canWalkThrough(x, y)) {
                return new Point2D(x, y);
            }
        }
        return null;
    }

    private Point2D findSafeSpawnPosition() {
        for (int attempts = 0; attempts < 50; attempts++) {
            int x = 5 + random.nextInt(GameModel.MAP_SIZE - 10);
            int y = 5 + random.nextInt(GameModel.MAP_SIZE - 10);

            Point2D playerPos = gameModel.getPlayerPosition();
            double distance = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());

            if (distance >= EnemyConfig.MIN_SPAWN_DISTANCE_FROM_PLAYER &&
                    gameModel.isValidTile(x, y) &&
                    gameModel.canWalkThrough(x, y)) {
                return new Point2D(x, y);
            }
        }
        return null;
    }

    private boolean isBossAlive() {
        return combatSystem.getEntities().stream()
                .anyMatch(e -> !e.isPlayer &&
                        e.entityClass == CombatSystem.EnemyClass.BOSS &&
                        e.stats.isAlive());
    }

    private CombatSystem.Entity findBoss() {
        return combatSystem.getEntities().stream()
                .filter(e -> !e.isPlayer &&
                        e.entityClass == CombatSystem.EnemyClass.BOSS &&
                        e.stats.isAlive())
                .findFirst()
                .orElse(null);
    }

    private CombatSystem.EnemyClass getRandomInvasionEnemyClass() {
        // Invasion: plus d'ennemis basiques et quelques élites
        CombatSystem.EnemyClass[] invasionClasses = {
                CombatSystem.EnemyClass.WARRIOR,
                CombatSystem.EnemyClass.WARRIOR, // Plus de guerriers
                CombatSystem.EnemyClass.ARCHER,
                CombatSystem.EnemyClass.MAGE,
                CombatSystem.EnemyClass.ELITE_WARRIOR // Quelques élites
        };
        return invasionClasses[random.nextInt(invasionClasses.length)];
    }

    private CombatSystem.EnemyClass getRandomEliteClass() {
        CombatSystem.EnemyClass[] eliteClasses = {
                CombatSystem.EnemyClass.ELITE_WARRIOR,
                CombatSystem.EnemyClass.ELITE_MAGE,
                CombatSystem.EnemyClass.ELITE_ARCHER
        };
        return eliteClasses[random.nextInt(eliteClasses.length)];
    }

    private CombatSystem.EnemyClass getRandomBasicEnemyClass() {
        CombatSystem.EnemyClass[] basicClasses = {
                CombatSystem.EnemyClass.WARRIOR,
                CombatSystem.EnemyClass.MAGE,
                CombatSystem.EnemyClass.ARCHER
        };
        return basicClasses[random.nextInt(basicClasses.length)];
    }

    private double getLootChance(CombatSystem.EnemyClass enemyClass) {
        switch (enemyClass) {
            case BOSS:
                return EnemyConfig.Rewards.BOSS_LOOT_CHANCE;
            case ELITE_WARRIOR:
            case ELITE_MAGE:
            case ELITE_ARCHER:
                return EnemyConfig.Rewards.ELITE_ENEMY_LOOT_CHANCE;
            default:
                return EnemyConfig.Rewards.BASIC_ENEMY_LOOT_CHANCE;
        }
    }

    private String[] getLootTable(CombatSystem.EnemyClass enemyClass) {
        switch (enemyClass) {
            case BOSS:
                return EnemyConfig.Rewards.BOSS_LOOT;
            case ELITE_WARRIOR:
            case ELITE_MAGE:
            case ELITE_ARCHER:
                return EnemyConfig.Rewards.ELITE_LOOT;
            default:
                return EnemyConfig.Rewards.BASIC_LOOT;
        }
    }

    private int getXpReward(CombatSystem.EnemyClass enemyClass) {
        switch (enemyClass) {
            case BOSS:
                return EnemyConfig.Rewards.BOSS_XP;
            case ELITE_WARRIOR:
            case ELITE_MAGE:
            case ELITE_ARCHER:
                return EnemyConfig.Rewards.ELITE_ENEMY_XP;
            default:
                return EnemyConfig.Rewards.BASIC_ENEMY_XP;
        }
    }

    /**
     * Spawne un boss à une position spécifique
     */
    public void spawnBossAt(Point2D position) {
        System.out.println("👹 Un boss apparaît!");
        gameModel.setMessageAbovePlayer("BOSS APPEARED!");

        CombatSystem.Entity boss = combatSystem.spawnSolitaryEnemy(position, CombatSystem.EnemyClass.BOSS);
        if (boss != null) {
            applyDifficultyScaling(boss);
        }
    }

    /**
     * Déclenche manuellement une invasion (pour debug/test)
     */
    public void forceTriggerInvasion() {
        if (!invasionActive) {
            startInvasion(System.currentTimeMillis() / 1000.0);
        }
    }

    /**
     * Nettoie tous les ennemis (pour debug/reset)
     */
    public void clearAllEnemies() {
        List<CombatSystem.Entity> enemies = new ArrayList<>(combatSystem.getEntities());
        for (CombatSystem.Entity entity : enemies) {
            if (!entity.isPlayer) {
                entity.stats.health = 0;
            }
        }
        System.out.println("🧹 Tous les ennemis ont été éliminés");
    }

    /**
     * Statistiques des événements
     */
    public void printEventStats() {
        double gameTimeMinutes = (System.currentTimeMillis() / 1000.0 - gameStartTime) / 60.0;
        double difficultyMultiplier = EnemyConfig.getDifficultyMultiplier(gameTimeMinutes);

        long aliveEnemies = combatSystem.getEntities().stream()
                .filter(e -> !e.isPlayer && e.stats.isAlive())
                .count();

        System.out.println("📊 Statistiques des événements:");
        System.out.println("  - Temps de jeu: " + String.format("%.1f", gameTimeMinutes) + " minutes");
        System.out.println("  - Multiplicateur de difficulté: x" + String.format("%.2f", difficultyMultiplier));
        System.out.println("  - Invasion active: " + (invasionActive ? "OUI" : "NON"));
        System.out.println("  - Ennemis vivants: " + aliveEnemies);
        System.out.println("  - Boss présent: " + (isBossAlive() ? "OUI" : "NON"));
    }

    // Getters pour l'état des événements
    public boolean isInvasionActive() { return invasionActive; }
    public double getGameTimeMinutes() {
        return (System.currentTimeMillis() / 1000.0 - gameStartTime) / 60.0;
    }
    public double getDifficultyMultiplier() {
        return EnemyConfig.getDifficultyMultiplier(getGameTimeMinutes());
    }
}