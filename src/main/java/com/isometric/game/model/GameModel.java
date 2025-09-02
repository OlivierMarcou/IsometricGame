package com.isometric.game.model;

import com.isometric.game.inventory.InventorySystem;
import javafx.geometry.Point2D;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

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
    private double playerAngle = 0;
    private Set<String> playerKeys = new HashSet<>();
    private InventorySystem inventory = new InventorySystem(); // Système d'inventaire

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
        try {
            InputStream stream = getClass().getResourceAsStream("/village_map.json");
            if (stream == null) {
                System.out.println("village_map.json non trouvé, génération d'une carte par défaut");
                generateDefaultMap();
                return false;
            }

            StringBuilder json = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
            }
            reader.close();

            boolean success = parseJson(json.toString());
            if (success) {
                initializeWallProperties();
                notifyMapLoaded();
            }
            return success;

        } catch (Exception e) {
            System.err.println("Erreur chargement JSON: " + e.getMessage());
            generateDefaultMap();
            return false;
        }
    }

    private boolean parseJson(String json) {
        // Parsing simplifié - vous pouvez réutiliser votre code existant
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
        // Essayer plusieurs variantes du marqueur de début
        String[] markers = {
                "\"" + arrayName + "\":[",     // Sans espace
                "\"" + arrayName + "\": [",    // Avec espace après :
                "\"" + arrayName + "\" :[",    // Avec espace avant :
                "\"" + arrayName + "\" : ["    // Avec espaces autour de :
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
            System.out.println("Recherché: " + Arrays.toString(markers));
            // Debug : afficher un échantillon du JSON autour du nom recherché
            int pos = json.indexOf("\"" + arrayName + "\"");
            if (pos != -1) {
                int start = Math.max(0, pos - 20);
                int end = Math.min(json.length(), pos + 50);
                System.out.println("Trouvé dans JSON: ..." + json.substring(start, end) + "...");
            }
            return null;
        }

        System.out.println("✅ Marqueur trouvé: " + usedMarker);
        startIndex += usedMarker.length() - 1; // Position sur '['

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
                // Sol avec plus de variété
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

        // Statistiques de la carte générée
        int floorVariety = 0, wallCount = 0;
        Set<Integer> floorTypesUsed = new HashSet<>();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                floorTypesUsed.add(floorMap[x][y]);
                if (wallMap[x][y] != -1) wallCount++;
            }
        }

        System.out.println("✅ Carte par défaut générée:");
        System.out.println("  - Types de sol utilisés: " + floorTypesUsed.size());
        System.out.println("  - Murs placés: " + wallCount);
        System.out.println("  - Plage de valeurs sol: 0-49");

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

        // Vérifier si le joueur est assez proche (case actuelle ou adjacente)
        double distance = Math.abs(x - playerPosition.getX()) + Math.abs(y - playerPosition.getY());
        if (distance > 1.5) {
            setMessageAbovePlayer("Too far");
            return false;
        }

        List<Item> groundItems = itemMap[x][y];
        if (groundItems.isEmpty()) {
            return false;
        }

        // Convertir les objets au format inventaire
        List<InventorySystem.InventoryItem> itemsToCollect = new ArrayList<>();
        for (Item gameItem : groundItems) {
            itemsToCollect.add(new InventorySystem.InventoryItem(gameItem));
        }

        // Essayer de tout ramasser
        boolean collectedAll = true;
        List<InventorySystem.InventoryItem> remaining = new ArrayList<>();

        for (InventorySystem.InventoryItem invItem : itemsToCollect) {
            if (!inventory.addItem(invItem)) {
                remaining.add(invItem);
                collectedAll = false;
            }
        }

        if (collectedAll) {
            // Tout a été ramassé
            groundItems.clear();
            setMessageAbovePlayer("Items collected");
            System.out.println("Tous les objets ont été ramassés");
            return true;
        } else {
            // Inventaire plein - interface de gestion nécessaire
            setMessageAbovePlayer("Inventory full");
            System.out.println("Inventaire plein - " + remaining.size() + " objets non ramassés");

            // Retourner les informations pour afficher l'interface de gestion
            // (sera géré dans le contrôleur)
            return false;
        }
    }

    public List<Item> getGroundItemsAt(int x, int y) {
        if (!isValidTile(x, y)) return new ArrayList<>();
        return new ArrayList<>(itemMap[x][y]);
    }

    public void dropItemAt(int x, int y, String itemName, int count) {
        if (!isValidTile(x, y)) return;

        // Créer un objet au sol
        Item droppedItem = new Item(itemName, count);
        itemMap[x][y].add(droppedItem);

        System.out.println("Objet jeté au sol: " + itemName + " (" + count + ")");
    }

    public void startMovement(List<Point2D> path, Point2D target, Point2D clicked) {
        if (path.isEmpty()) {
            setShowExclamation(true);
            return;
        }

        this.currentPath = new ArrayList<>(path);
        this.targetPosition = target;
        this.clickedPosition = clicked;
        this.isMoving = true;
        this.currentPathIndex = 0;
        this.moveProgress = 0.0;

        notifyMovementStarted(path);
    }

    public boolean updateMovement() {
        if (!isMoving || currentPath.isEmpty()) return false;

        moveProgress += 0.05;

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

    public Point2D getCurrentInterpolatedPosition() {
        if (!isMoving || currentPathIndex >= currentPath.size()) {
            return playerPosition;
        }

        Point2D nextPos = currentPath.get(currentPathIndex);
        double interpX = playerPosition.getX() + (nextPos.getX() - playerPosition.getX()) * moveProgress;
        double interpY = playerPosition.getY() + (nextPos.getY() - playerPosition.getY()) * moveProgress;

        return new Point2D(interpX, interpY);
    }

    // Gestion des observateurs
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