package net.arkaine.model;

import javafx.geometry.Point2D;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import net.arkaine.combat.CombatSystem;
import net.arkaine.combat.CombatEventsManager;
import net.arkaine.config.EnemyConfig;
import net.arkaine.inventory.InventorySystem;

/**
 * Modèle du jeu - Contient toutes les données et la logique métier
 */
public class GameModel {

    public static final int MAP_SIZE = 50;

    // Données de la carte
    private int[][] floorMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] wallMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] ceilingMap = new int[MAP_SIZE][MAP_SIZE];
    private WallType[][] wallTypes = new WallType[MAP_SIZE][MAP_SIZE];
    private WallProperties[][] wallProperties = new WallProperties[MAP_SIZE][MAP_SIZE];
    private List<Item>[][] itemMap = new List[MAP_SIZE][MAP_SIZE];

    // État du joueur
    private Point2D playerPosition = new Point2D(MAP_SIZE / 2, MAP_SIZE / 2);
    CombatSystem.Entity playerEntity = new CombatSystem.Player(playerPosition);
    private double playerAngle = 0;
    private Set<String> playerKeys = new HashSet<>();
    private InventorySystem inventory = new InventorySystem(); // Système d'inventaire

    // Système de combat
    private CombatSystem combatSystem = new CombatSystem();
    private CombatEventsManager combatEventsManager;

    // État du mouvement
    private List<Point2D> currentPath = new ArrayList<>();
    private Point2D targetPosition = null;
    private Point2D clickedPosition = null;
    private boolean isMoving = false;
    private int currentPathIndex = 0;
    private double moveProgress = 0.0;

    // Messages et notifications
    private String messageAbovePlayer = null;
    private boolean showExclamation = false;

    // Observateurs pour notifier les changements
    private List<GameModelListener> listeners = new ArrayList<>();

    public enum WallType {
        NONE, TRAVERSABLE, TRANSPARENT, DOOR, DESTRUCTIBLE, INDESTRUCTIBLE
    }

    public static class Item {
        public final String type;
        public final int count;

        public Item(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    public static class WallProperties {
        public boolean isOpen = false;
        public boolean isLocked = false;
        public String keyId = null;
        public int health = 255;

        public WallProperties() {}

        public WallProperties(boolean isOpen, boolean isLocked, String keyId, int health) {
            this.isOpen = isOpen;
            this.isLocked = isLocked;
            this.keyId = keyId;
            this.health = Math.max(0, Math.min(255, health));
        }
    }

    public interface GameModelListener {
        void onMapLoaded();
        void onPlayerMoved(Point2D newPosition);
        void onMovementStarted(List<Point2D> path);
        void onMovementFinished();
        void onDoorStateChanged(int x, int y, boolean isOpen);
        void onMessageChanged(String message);
        void onExclamationStateChanged(boolean show);
    }

    public GameModel() {
        initializeItemMap();

        // Initialiser le gestionnaire d'événements de combat
        combatEventsManager = new CombatEventsManager(this, combatSystem);
    }

    @SuppressWarnings("unchecked")
    private void initializeItemMap() {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                itemMap[x][y] = new ArrayList<>();
                wallTypes[x][y] = WallType.NONE;
                wallProperties[x][y] = new WallProperties();
                wallMap[x][y] = -1;
                ceilingMap[x][y] = -1;
                floorMap[x][y] = 0;
            }
        }
    }

    // Getters pour accès aux données
    public int[][] getFloorMap() { return floorMap; }
    public int[][] getWallMap() { return wallMap; }
    public int[][] getCeilingMap() { return ceilingMap; }
    public WallType[][] getWallTypes() { return wallTypes; }
    public WallProperties[][] getWallProperties() { return wallProperties; }
    public List<Item>[][] getItemMap() { return itemMap; }

    public Point2D getPlayerPosition() { return playerPosition; }
    public double getPlayerAngle() { return playerAngle; }
    public Set<String> getPlayerKeys() { return playerKeys; }
    public InventorySystem getInventory() { return inventory; }

    // Getters pour le système de combat
    public CombatSystem getCombatSystem() { return combatSystem; }
    public CombatEventsManager getCombatEventsManager() { return combatEventsManager; }

    public List<Point2D> getCurrentPath() { return currentPath; }
    public Point2D getTargetPosition() { return targetPosition; }
    public Point2D getClickedPosition() { return clickedPosition; }
    public boolean isMoving() { return isMoving; }
    public int getCurrentPathIndex() { return currentPathIndex; }
    public double getMoveProgress() { return moveProgress; }

    public String getMessageAbovePlayer() { return messageAbovePlayer; }
    public boolean shouldShowExclamation() { return showExclamation; }

    // Setters avec notification
    public void setPlayerAngle(double angle) {
        this.playerAngle = angle;
    }

    public void setPlayerPosition(Point2D position) {
        this.playerPosition = position;
        notifyPlayerMoved(position);
    }

    public void setMessageAbovePlayer(String message) {
        this.messageAbovePlayer = message;
        notifyMessageChanged(message);
    }

    public void setShowExclamation(boolean show) {
        this.showExclamation = show;
        notifyExclamationStateChanged(show);
    }

    // Logique métier
    public boolean loadMapFromJson() {
        generateDefaultMap();return true;
//
//        try {
//            InputStream stream = getClass().getResourceAsStream("/village_map.json");
//            if (stream == null) {
//                System.out.println("village_map.json non trouvé, génération d'une carte par défaut");
//                generateDefaultMap();
//                return false;
//            }
//
//            StringBuilder json = new StringBuilder();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                json.append(line).append("\n");
//            }
//            reader.close();
//
//            boolean success = parseJson(json.toString());
//            if (success) {
//                initializeWallProperties();
//
//                // Générer les ennemis après le chargement de la carte
//                spawnInitialEnemies();
//
//                notifyMapLoaded();
//            }
//            return success;
//
//        } catch (Exception e) {
//            System.err.println("Erreur chargement JSON: " + e.getMessage());
//            generateDefaultMap();
//            return false;
//        }
    }

    private boolean parseJson(String json) {
        try {
            System.out.println("🔍 Début du parsing JSON...");

            boolean floorSuccess = parseFloorMap(json);
            boolean wallSuccess = parseWallMap(json);
            boolean ceilingSuccess = parseCeilingMap(json);
            boolean wallTypeSuccess = parseWallTypes(json);

            System.out.println("📊 Résultats du parsing:");
            System.out.println("  - FloorMap: " + (floorSuccess ? "✅" : "❌"));
            System.out.println("  - WallMap: " + (wallSuccess ? "✅" : "❌"));
            System.out.println("  - CeilingMap: " + (ceilingSuccess ? "✅" : "❌"));
            System.out.println("  - WallTypes: " + (wallTypeSuccess ? "✅" : "❌"));

            return floorSuccess && wallSuccess && wallTypeSuccess; // ceilingMap est optionnel

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseFloorMap(String json) {
        try {
            String floorData = extractJsonArray(json, "floorMap");
            if (floorData == null) {
                System.out.println("❌ FloorMap non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing FloorMap, taille données: " + floorData.length());
            parseIntArrayData(floorData, floorMap);

            // Vérification
            int nonZeroCount = 0;
            int minValue = Integer.MAX_VALUE;
            int maxValue = Integer.MIN_VALUE;

            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    if (floorMap[x][y] >= 0) {
                        nonZeroCount++;
                        minValue = Math.min(minValue, floorMap[x][y]);
                        maxValue = Math.max(maxValue, floorMap[x][y]);
                    }
                }
            }

            System.out.println("✅ FloorMap chargé:");
            System.out.println("  - Cases valides: " + nonZeroCount);
            System.out.println("  - Valeurs: " + minValue + " à " + maxValue);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing FloorMap: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseWallMap(String json) {
        try {
            String wallData = extractJsonArray(json, "wallMap");
            if (wallData == null) {
                System.out.println("❌ WallMap non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing WallMap, taille données: " + wallData.length());
            parseIntArrayData(wallData, wallMap);

            // Vérification
            int wallCount = 0;
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    if (wallMap[x][y] != -1) wallCount++;
                }
            }
            System.out.println("✅ WallMap chargé - " + wallCount + " murs trouvés");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing WallMap: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseCeilingMap(String json) {
        try {
            String ceilingData = extractJsonArray(json, "ceilingMap");
            if (ceilingData == null) {
                System.out.println("⚠️ CeilingMap non trouvé dans le JSON (optionnel)");
                return true; // Non critique
            }

            System.out.println("🔍 Parsing CeilingMap, taille données: " + ceilingData.length());
            parseIntArrayData(ceilingData, ceilingMap);

            // Vérification
            int ceilingCount = 0;
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    if (ceilingMap[x][y] != -1) ceilingCount++;
                }
            }
            System.out.println("✅ CeilingMap chargé - " + ceilingCount + " plafonds trouvés");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing CeilingMap: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean parseWallTypes(String json) {
        try {
            String wallTypeData = extractJsonArray(json, "wallTypes");
            if (wallTypeData == null) {
                System.out.println("❌ WallTypes non trouvé dans le JSON");
                return false;
            }

            System.out.println("🔍 Parsing WallTypes...");
            parseWallTypeArrayData(wallTypeData);
            System.out.println("✅ WallTypes chargé");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur parsing WallTypes: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String extractJsonArray(String json, String arrayName) {
        String[] markers = {
                "\"" + arrayName + "\":[",
                "\"" + arrayName + "\": [",
                "\"" + arrayName + "\" :[",
                "\"" + arrayName + "\" : ["
        };

        int startIndex = -1;
        String usedMarker = null;

        for (String marker : markers) {
            startIndex = json.indexOf(marker);
            if (startIndex != -1) {
                usedMarker = marker;
                break;
            }
        }

        if (startIndex == -1) {
            System.out.println("❌ Aucun marqueur trouvé pour: " + arrayName);
            return null;
        }

        System.out.println("✅ Marqueur trouvé: " + usedMarker);
        startIndex += usedMarker.length() - 1;

        int brackets = 0;
        int endIndex = startIndex;

        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
                if (brackets == 0) {
                    endIndex = i + 1;
                    break;
                }
            }
        }

        if (brackets != 0) {
            System.out.println("❌ Crochets non équilibrés pour: " + arrayName);
            return null;
        }

        String result = json.substring(startIndex, endIndex);
        System.out.println("✅ Array " + arrayName + " extrait, longueur: " + result.length());

        return result;
    }

    private void parseIntArrayData(String arrayData, int[][] target) {
        arrayData = arrayData.trim();
        if (arrayData.startsWith("[")) arrayData = arrayData.substring(1);
        if (arrayData.endsWith("]")) arrayData = arrayData.substring(0, arrayData.length() - 1);

        String[] lines = arrayData.split("\\],\\s*\\[");

        for (int x = 0; x < Math.min(lines.length, MAP_SIZE); x++) {
            String line = lines[x].replaceAll("[\\[\\]\\s]", "");
            String[] values = line.split(",");

            for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                try {
                    target[x][y] = Integer.parseInt(values[y].trim());
                } catch (NumberFormatException e) {
                    target[x][y] = -1;
                }
            }
        }
    }

    private void parseWallTypeArrayData(String arrayData) {
        arrayData = arrayData.trim();
        if (arrayData.startsWith("[")) arrayData = arrayData.substring(1);
        if (arrayData.endsWith("]")) arrayData = arrayData.substring(0, arrayData.length() - 1);

        String[] lines = arrayData.split("\\],\\s*\\[");

        for (int x = 0; x < Math.min(lines.length, MAP_SIZE); x++) {
            String line = lines[x].replaceAll("[\\[\\]\\s]", "");
            String[] values = line.split(",");

            for (int y = 0; y < Math.min(values.length, MAP_SIZE); y++) {
                try {
                    String wallTypeName = values[y].trim().replace("\"", "");
                    wallTypes[x][y] = WallType.valueOf(wallTypeName);
                } catch (Exception e) {
                    wallTypes[x][y] = WallType.NONE;
                }
            }
        }
    }

    private void generateDefaultMap() {
        System.out.println("🔧 Génération d'une carte par défaut...");
        Random rand = new Random();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                floorMap[x][y] = rand.nextInt(50);

                if (rand.nextDouble() < 0.1) {
                    wallMap[x][y] = rand.nextInt(50);
                    wallTypes[x][y] = WallType.values()[1 + rand.nextInt(WallType.values().length - 1)];
                } else {
                    wallMap[x][y] = -1;
                    wallTypes[x][y] = WallType.NONE;
                }

                if (rand.nextDouble() < 0.05) {
                    ceilingMap[x][y] = rand.nextInt(30);
                } else {
                    ceilingMap[x][y] = -1;
                }

                if (rand.nextDouble() < 0.03) {
                    itemMap[x][y].add(new Item("treasure", 1 + rand.nextInt(3)));
                }
            }
        }

        initializeWallProperties();

        // Générer les ennemis sur la carte par défaut
        spawnInitialEnemies();

        System.out.println("✅ Carte par défaut générée");
        notifyMapLoaded();
    }

    private void initializeWallProperties() {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (wallTypes[x][y] == WallType.DOOR) {
                    wallProperties[x][y].isOpen = false;
                    if (Math.random() < 0.2) {
                        wallProperties[x][y].isLocked = true;
                        wallProperties[x][y].keyId = "key_" + (x * MAP_SIZE + y);
                    }
                } else if (wallTypes[x][y] == WallType.DESTRUCTIBLE) {
                    wallProperties[x][y].health = 100 + (int)(Math.random() * 156);
                }
            }
        }

        playerKeys.add("key_1250");
        playerKeys.add("key_750");
    }

    // ================================
    // SYSTÈME DE COMBAT ET ENNEMIS
    // ================================

    private void spawnInitialEnemies() {
        System.out.println("🐺 Génération des ennemis sur la carte...");

        // Ajouter le joueur au système de combat
        combatSystem.addEntity(playerEntity);

        Random rand = new Random();
        int totalEnemies = 0;

        // 1. Spawner quelques meutes (utiliser la config)
        int numPacks = EnemyConfig.MIN_PACKS + rand.nextInt(EnemyConfig.MAX_PACKS - EnemyConfig.MIN_PACKS + 1);
        for (int i = 0; i < numPacks; i++) {
            Point2D packPos = findSafeSpawnPosition(rand);
            if (packPos != null) {
                CombatSystem.EnemyClass packClass = rand.nextDouble() < EnemyConfig.PACK_CLASS_WEIGHTS[0] ?
                        CombatSystem.EnemyClass.WARRIOR : CombatSystem.EnemyClass.ARCHER;

                int packSize = EnemyConfig.MIN_PACK_SIZE + rand.nextInt(EnemyConfig.MAX_PACK_SIZE - EnemyConfig.MIN_PACK_SIZE + 1);
                combatSystem.spawnEnemyPack(this, packPos, packClass, packSize);
                totalEnemies += packSize;
            }
        }

        // 2. Spawner des ennemis solitaires
        int numSolitary = EnemyConfig.MIN_SOLITARY + rand.nextInt(EnemyConfig.MAX_SOLITARY - EnemyConfig.MIN_SOLITARY + 1);
        for (int i = 0; i < numSolitary; i++) {
            Point2D soloPos = findSafeSpawnPosition(rand);
            if (soloPos != null) {
                CombatSystem.EnemyClass soloClass = getRandomBasicEnemyClass(rand);
                combatSystem.spawnSolitaryEnemy(soloPos, soloClass);
                totalEnemies++;
            }
        }

        // 3. Spawner quelques élites
        int numElites = EnemyConfig.MIN_ELITES + rand.nextInt(EnemyConfig.MAX_ELITES - EnemyConfig.MIN_ELITES + 1);
        for (int i = 0; i < numElites; i++) {
            Point2D elitePos = findSafeSpawnPosition(rand);
            if (elitePos != null) {
                CombatSystem.EnemyClass eliteClass = getRandomEliteEnemyClass(rand);
                combatSystem.spawnSolitaryEnemy(elitePos, eliteClass);
                totalEnemies++;
            }
        }

        // 4. Spawner quelques gardiens près des maisons
        int numGuardians = EnemyConfig.MIN_GUARDIANS + rand.nextInt(EnemyConfig.MAX_GUARDIANS - EnemyConfig.MIN_GUARDIANS + 1);
        for (int i = 0; i < numGuardians; i++) {
            Point2D guardPos = findGuardianSpawnPosition(rand);
            if (guardPos != null) {
                CombatSystem.EnemyClass guardClass = rand.nextBoolean() ?
                        CombatSystem.EnemyClass.ELITE_WARRIOR : CombatSystem.EnemyClass.ELITE_MAGE;
                combatSystem.spawnGuardian(guardPos, guardClass);
                totalEnemies++;
            }
        }

        // 5. Un boss rare
        if (rand.nextDouble() < EnemyConfig.BOSS_SPAWN_CHANCE) {
            Point2D bossPos = findBossSpawnPosition(rand);
            if (bossPos != null) {
                combatEventsManager.spawnBossAt(bossPos);
                totalEnemies++;
            }
        }

        System.out.println("✅ " + totalEnemies + " ennemis générés sur la carte");
    }

    private Point2D findSafeSpawnPosition(Random rand) {
        for (int attempts = 0; attempts < 50; attempts++) {
            int x = 5 + rand.nextInt(MAP_SIZE - 10);
            int y = 5 + rand.nextInt(MAP_SIZE - 10);

            if (isValidSpawnPosition(x, y)) {
                return new Point2D(x, y);
            }
        }
        return null;
    }

    private Point2D findGuardianSpawnPosition(Random rand) {
        for (int attempts = 0; attempts < 30; attempts++) {
            int x = rand.nextInt(MAP_SIZE);
            int y = rand.nextInt(MAP_SIZE);

            if (isNearHouse(x, y) && isValidSpawnPosition(x, y)) {
                return new Point2D(x, y);
            }
        }

        return findSafeSpawnPosition(rand);
    }

    private Point2D findBossSpawnPosition(Random rand) {
        for (int attempts = 0; attempts < 30; attempts++) {
            int x = 10 + rand.nextInt(MAP_SIZE - 20);
            int y = 10 + rand.nextInt(MAP_SIZE - 20);

            if (isValidSpawnPosition(x, y) && isIsolatedPosition(x, y)) {
                return new Point2D(x, y);
            }
        }

        return findSafeSpawnPosition(rand);
    }

    private boolean isValidSpawnPosition(int x, int y) {
        if (!isValidTile(x, y)) return false;

        double distanceToPlayer = Math.abs(x - playerPosition.getX()) + Math.abs(y - playerPosition.getY());
        if (distanceToPlayer < EnemyConfig.MIN_SPAWN_DISTANCE_FROM_PLAYER) return false;

        if (!canWalkThrough(x, y)) return false;

        for (Item item : itemMap[x][y]) {
            if (item.type.contains("key") || item.type.contains("treasure")) {
                return false;
            }
        }

        return true;
    }

    private boolean isNearHouse(int x, int y) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;

                if (isValidTile(checkX, checkY)) {
                    int floor = floorMap[checkX][checkY];
                    if (floor >= 35 && floor <= 39) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isIsolatedPosition(int x, int y) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;

                if (isValidTile(checkX, checkY) && wallMap[checkX][checkY] != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private CombatSystem.EnemyClass getRandomBasicEnemyClass(Random rand) {
        CombatSystem.EnemyClass[] basicClasses = {
                CombatSystem.EnemyClass.WARRIOR,
                CombatSystem.EnemyClass.MAGE,
                CombatSystem.EnemyClass.ARCHER
        };
        return basicClasses[rand.nextInt(basicClasses.length)];
    }

    private CombatSystem.EnemyClass getRandomEliteEnemyClass(Random rand) {
        CombatSystem.EnemyClass[] eliteClasses = {
                CombatSystem.EnemyClass.ELITE_WARRIOR,
                CombatSystem.EnemyClass.ELITE_MAGE,
                CombatSystem.EnemyClass.ELITE_ARCHER
        };
        return eliteClasses[rand.nextInt(eliteClasses.length)];
    }

    public boolean updateCombat(double deltaTime) {
        // Mettre à jour le système de combat
        combatSystem.update(this, deltaTime);

        // Mettre à jour les événements de combat
        combatEventsManager.update(System.currentTimeMillis() / 1000.0);

        // Gérer les morts d'ennemis pour les récompenses
        handleEnemyDeaths();

        if (!playerEntity.stats.isAlive()) {
            setMessageAbovePlayer("GAME OVER");
            return false;
        }

        return true;
    }

    private void handleEnemyDeaths() {
        List<CombatSystem.Entity> deadEnemies = new ArrayList<>();

        for (CombatSystem.Entity entity : combatSystem.getEntities()) {
            if (!entity.isPlayer && !entity.stats.isAlive()) {
                deadEnemies.add(entity);
            }
        }

        for (CombatSystem.Entity deadEnemy : deadEnemies) {
            combatEventsManager.handleEnemyDeath(deadEnemy, deadEnemy.position);
        }
    }

    public void respawnEnemiesIfNeeded() {
        List<CombatSystem.Entity> currentEnemies = combatSystem.getEntities();
        long aliveEnemies = currentEnemies.stream()
                .filter(e -> !e.isPlayer && e.stats.isAlive())
                .count();

        if (aliveEnemies < EnemyConfig.MIN_ENEMIES_BEFORE_RESPAWN) {
            Random rand = new Random();
            int enemiesToSpawn = EnemyConfig.RESPAWN_AMOUNT_MIN +
                    rand.nextInt(EnemyConfig.RESPAWN_AMOUNT_MAX - EnemyConfig.RESPAWN_AMOUNT_MIN + 1);

            for (int i = 0; i < enemiesToSpawn; i++) {
                Point2D spawnPos = findSafeSpawnPosition(rand);
                if (spawnPos != null) {
                    int floorType = floorMap[(int)spawnPos.getX()][(int)spawnPos.getY()];
                    CombatSystem.EnemyClass[] preferredClasses =
                            EnemyConfig.BiomeSpawning.getPreferredEnemiesForFloor(floorType);

                    CombatSystem.EnemyClass enemyClass = preferredClasses[rand.nextInt(preferredClasses.length)];
                    combatSystem.spawnSolitaryEnemy(spawnPos, enemyClass);
                }
            }

            System.out.println("🔄 " + enemiesToSpawn + " nouveaux ennemis sont apparus!");
            setMessageAbovePlayer("Enemies respawned!");
        }
    }

    public void playerAttack(Point2D targetPosition) {
        if (!playerEntity.canAttack(System.currentTimeMillis() / 1000.0)) {
            return;
        }

        CombatSystem.Entity target = null;
        double minDistance = Double.MAX_VALUE;

        for (CombatSystem.Entity entity : combatSystem.getEntities()) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            double distance = entity.position.distance(targetPosition);
            if (distance < minDistance && distance <= playerEntity.stats.range) {
                minDistance = distance;
                target = entity;
            }
        }

        if (target != null) {
            CombatSystem.DamageType damageType = CombatSystem.DamageType.PHYSICAL;
            int damage = playerEntity.stats.damage + (int)(Math.random() * 10 - 5);
            int finalDamage = target.takeDamage(damage, damageType);

            setMessageAbovePlayer("Hit for " + finalDamage + "!");
            playerEntity.lastAttackTime = System.currentTimeMillis() / 1000.0;

            System.out.println("Joueur attaque " + target.entityClass + " pour " + finalDamage + " dégâts!");
        } else {
            setMessageAbovePlayer("Miss!");
        }
    }

    // ================================
    // MÉTHODES DE DEBUG POUR LE COMBAT
    // ================================

    public void debugTriggerInvasion() {
        combatEventsManager.forceTriggerInvasion();
    }

    public void debugClearAllEnemies() {
        combatEventsManager.clearAllEnemies();
    }

    public void debugSpawnBoss() {
        Point2D playerPos = getPlayerPosition();
        Point2D bossPos = new Point2D(playerPos.getX() + 5, playerPos.getY() + 5);
        combatEventsManager.spawnBossAt(bossPos);
    }

    public void debugPrintCombatStats() {
        combatEventsManager.printEventStats();
    }

    // ================================
    // LOGIQUE DE JEU EXISTANTE
    // ================================

    public boolean isValidTile(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE;
    }

    public boolean canWalkThrough(int x, int y) {
        if (!isValidTile(x, y)) return false;

        WallType wall = wallTypes[x][y];

        if (wall == WallType.DOOR) {
            WallProperties props = wallProperties[x][y];
            return props != null && props.isOpen;
        }

        return wall == WallType.NONE || wall == WallType.TRAVERSABLE || wall == WallType.TRANSPARENT;
    }

    public boolean handleDoorInteraction(int x, int y) {
        if (wallTypes[x][y] != WallType.DOOR || wallMap[x][y] == -1) {
            return false;
        }

        double distance = Math.abs(x - playerPosition.getX()) + Math.abs(y - playerPosition.getY());
        if (distance > 1.5) {
            return false;
        }

        WallProperties props = wallProperties[x][y];
        if (props == null) {
            props = new WallProperties();
            wallProperties[x][y] = props;
        }

        if (props.isLocked && props.keyId != null) {
            if (!playerKeys.contains(props.keyId)) {
                setMessageAbovePlayer("Need Key");
                return false;
            } else {
                props.isLocked = false;
                setMessageAbovePlayer("Unlocked!");
            }
        }

        props.isOpen = !props.isOpen;
        setMessageAbovePlayer(props.isOpen ? "Opened" : "Closed");
        notifyDoorStateChanged(x, y, props.isOpen);
        return true;
    }



    public boolean tryCollectItems(int x, int y) {
        if (!isValidTile(x, y)) return false;

        // Vérifier la distance
        double distance = Math.abs(x - playerPosition.getX()) + Math.abs(y - playerPosition.getY());
        if (distance > 1.5) {
            setMessageAbovePlayer("Too far");
            return false;
        }

        List<Item> groundItems = itemMap[x][y];
        if (groundItems.isEmpty()) {
            setMessageAbovePlayer("Nothing here");
            return false;
        }

        System.out.println("🎒 Tentative de collecte de " + groundItems.size() + " types d'objets");

        // Collecter objet par objet avec gestion fine
        List<Item> toRemove = new ArrayList<>();
        int collectedCount = 0;
        int totalAttempts = 0;

        for (Item gameItem : groundItems) {
            totalAttempts++;
            InventorySystem.InventoryItem invItem = new InventorySystem.InventoryItem(gameItem);

            if (inventory.addItem(invItem)) {
                toRemove.add(gameItem);
                collectedCount++;
                System.out.println("✅ Collecté: " + gameItem.type + " x" + gameItem.count);
            } else {
                System.out.println("❌ Inventaire plein pour: " + gameItem.type);
                break; // Arrêter dès que l'inventaire est plein
            }
        }

        // Supprimer les objets collectés
        groundItems.removeAll(toRemove);

        // Messages et retours appropriés
        if (collectedCount > 0) {
            if (collectedCount == totalAttempts) {
                setMessageAbovePlayer("All collected (" + collectedCount + ")");
            } else {
                setMessageAbovePlayer("Collected " + collectedCount + "/" + totalAttempts);
            }
        }

        // Retourner false si inventaire plein pour déclencher l'interface
        boolean inventoryFull = (collectedCount < totalAttempts) && !groundItems.isEmpty();
        if (inventoryFull) {
            setMessageAbovePlayer("Inventory full!");
        }

        return !inventoryFull;
    }


    public List<Item> getGroundItemsAt(int x, int y) {
        if (!isValidTile(x, y)) return new ArrayList<>();
        return new ArrayList<>(itemMap[x][y]);
    }



    public boolean dropItemAt(int x, int y, String itemName, int count) {
        if (!isValidTile(x, y)) {
            System.out.println("❌ Position invalide: (" + x + ", " + y + ")");
            return false;
        }

        // Vérifier si on peut jeter des objets ici
        if (wallMap[x][y] != -1 && !canWalkThrough(x, y)) {
            System.out.println("❌ Impossible de jeter sur un obstacle");
            return false;
        }

        // Limiter les objets par case
        List<Item> currentItems = itemMap[x][y];
        if (currentItems.size() >= 15) {
            System.out.println("❌ Trop d'objets sur cette case (" + currentItems.size() + "/15)");
            return false;
        }

        // Vérifier si on peut fusionner avec un objet existant
        for (Item existingItem : currentItems) {
            if (existingItem.type.equals(itemName)) {
                // Créer un nouvel objet fusionné et remplacer l'ancien
                Item mergedItem = new Item(itemName, existingItem.count + count);
                currentItems.remove(existingItem);
                currentItems.add(mergedItem);
                System.out.println("🔄 Objets fusionnés: " + itemName + " (maintenant x" + mergedItem.count + ")");
                return true;
            }
        }

        // Ajouter comme nouvel objet
        Item droppedItem = new Item(itemName, count);
        currentItems.add(droppedItem);

        System.out.println("📦 Objet jeté: " + itemName + " x" + count + " à (" + x + ", " + y + ")");
        return true;
    }



    // ================================
    // GESTION DU MOUVEMENT
    // ================================
    public void startMovement(List<Point2D> path, Point2D target, Point2D clicked) {
        if (path.isEmpty()) {
            setShowExclamation(true);
            return;
        }

        // CORRECTION : Toujours accepter un nouveau mouvement
        // Interrompre le mouvement actuel s'il y en a un
        if (isMoving) {
            // Mettre à jour la position actuelle du joueur à sa position interpolée
            Point2D currentInterpolated = getCurrentInterpolatedPosition();
            setPlayerPosition(currentInterpolated);
        }

        this.currentPath = new ArrayList<>(path);
        this.targetPosition = target;
        this.clickedPosition = clicked;
        this.isMoving = true;
        this.currentPathIndex = 0;
        this.moveProgress = 0.0;

        notifyMovementStarted(path);
    }
    // NOUVELLE MÉTHODE : Arrêter le mouvement en cours
    public void stopMovement() {
        if (isMoving) {
            // Fixer la position du joueur à sa position interpolée actuelle
            Point2D currentInterpolated = getCurrentInterpolatedPosition();
            setPlayerPosition(currentInterpolated);

            // Arrêter le mouvement
            isMoving = false;
            currentPath.clear();
            targetPosition = null;
            clickedPosition = null;

            notifyMovementFinished();
        }
    }

    public boolean updateMovement() {
        if (!isMoving || currentPath.isEmpty()) return false;

        // CORRECTION : Vitesse de mouvement ajustée pour un suivi plus fluide
        moveProgress += 0.08; // Augmenté de 0.05 à 0.08 pour un mouvement plus rapide

        if (moveProgress >= 1.0) {
            moveProgress = 0.0;
            currentPathIndex++;

            if (currentPathIndex >= currentPath.size()) {
                isMoving = false;
                setPlayerPosition(targetPosition);
                clickedPosition = null;
                notifyMovementFinished();
                return false;
            }

            setPlayerPosition(currentPath.get(currentPathIndex - 1));
        }

        return true;
    }


    public void validateGroundItems() {
        int totalGroundItems = 0;
        int totalStacks = 0;
        int invalidPositions = 0;
        int overworldPositions = 0;
        Map<String, Integer> itemTypeStats = new HashMap<>();

        System.out.println("🔍 Validation des objets au sol...");

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                List<Item> items = itemMap[x][y];
                if (!items.isEmpty()) {
                    totalStacks++;

                    for (Item item : items) {
                        totalGroundItems += item.count;
                        itemTypeStats.put(item.type,
                                itemTypeStats.getOrDefault(item.type, 0) + item.count);
                    }

                    // Vérifications
                    if (!isValidTile(x, y)) {
                        invalidPositions++;
                        System.out.println("⚠️ Position invalide: (" + x + ", " + y + ") avec " + items.size() + " objets");
                    }

                    if (items.size() > 10) {
                        overworldPositions++;
                        System.out.println("⚠️ Surcharge: (" + x + ", " + y + ") avec " + items.size() + " types d'objets");
                    }
                }
            }
        }

        System.out.println("📊 Rapport de validation:");
        System.out.println("  📦 Total objets au sol: " + totalGroundItems);
        System.out.println("  📍 Positions avec objets: " + totalStacks);
        System.out.println("  ❌ Positions invalides: " + invalidPositions);
        System.out.println("  ⚠️ Positions surchargées: " + overworldPositions);

        System.out.println("  🏷️ Répartition par type:");
        itemTypeStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry ->
                        System.out.println("    " + entry.getKey() + ": " + entry.getValue()));
    }

    public void cleanupGroundItems() {
        int removed = 0;

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                List<Item> items = itemMap[x][y];

                // Supprimer les objets en position invalide
                if (!isValidTile(x, y) && !items.isEmpty()) {
                    removed += items.size();
                    items.clear();
                }

                // Limiter à 10 types d'objets par case
                if (items.size() > 10) {
                    int excess = items.size() - 10;
                    for (int i = 0; i < excess; i++) {
                        items.remove(items.size() - 1);
                    }
                    removed += excess;
                }
            }
        }

        System.out.println("🧹 Nettoyage terminé: " + removed + " objets supprimés");
    }

    public Point2D getCurrentInterpolatedPosition() {
        if (!isMoving || currentPathIndex >= currentPath.size()) {
            return playerPosition;
        }

        Point2D nextPos = currentPath.get(currentPathIndex);

        // CORRECTION : Interpolation plus précise
        double interpX = playerPosition.getX() + (nextPos.getX() - playerPosition.getX()) * moveProgress;
        double interpY = playerPosition.getY() + (nextPos.getY() - playerPosition.getY()) * moveProgress;

        return new Point2D(interpX, interpY);
    }

    // ================================
    // GESTION DES OBSERVATEURS
    // ================================

    public void addListener(GameModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameModelListener listener) {
        listeners.remove(listener);
    }

    private void notifyMapLoaded() {
        for (GameModelListener listener : listeners) {
            listener.onMapLoaded();
        }
    }

    private void notifyPlayerMoved(Point2D position) {
        for (GameModelListener listener : listeners) {
            listener.onPlayerMoved(position);
        }
    }

    private void notifyMovementStarted(List<Point2D> path) {
        for (GameModelListener listener : listeners) {
            listener.onMovementStarted(path);
        }
    }

    private void notifyMovementFinished() {
        for (GameModelListener listener : listeners) {
            listener.onMovementFinished();
        }
    }

    private void notifyDoorStateChanged(int x, int y, boolean isOpen) {
        for (GameModelListener listener : listeners) {
            listener.onDoorStateChanged(x, y, isOpen);
        }
    }

    private void notifyMessageChanged(String message) {
        for (GameModelListener listener : listeners) {
            listener.onMessageChanged(message);
        }
    }

    private void notifyExclamationStateChanged(boolean show) {
        for (GameModelListener listener : listeners) {
            listener.onExclamationStateChanged(show);
        }
    }
}