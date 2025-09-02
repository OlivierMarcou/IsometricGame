package com.isometric.game;



        import java.awt.*;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.util.*;
        import java.util.List;

/**
 * Générateur de cartes réalistes pour créer un environnement
 * de village avec forêts, maisons, portes et fenêtres
 */
public class RealisticMapGenerator {

    private static final int MAP_SIZE = 100;
    private static final Random random = new Random();

    // Types d'images par catégorie
    public enum FloorType {
        GRASS(0, 15),           // Herbe (indices 0-14)
        DIRT_PATH(15, 20),      // Chemin de terre (indices 15-19)
        STONE_FLOOR(20, 25),    // Sol en pierre (indices 20-24)
        WATER(25, 30),          // Eau (indices 25-29)
        SAND(30, 35),           // Sable (indices 30-34)
        WOOD_FLOOR(35, 40);     // Plancher bois (indices 35-39)

        public final int startIndex, endIndex;
        FloorType(int start, int end) { this.startIndex = start; this.endIndex = end; }

        public int getRandomIndex() {
            return startIndex + random.nextInt(endIndex - startIndex);
        }
    }

    public enum WallType {
        WOOD_WALL(0, 10),       // Murs en bois (indices 0-9)
        STONE_WALL(10, 15),     // Murs en pierre (indices 10-14)
        BRICK_WALL(15, 20),     // Murs en brique (indices 15-19)
        GLASS_WINDOW(20, 25),   // Fenêtres en verre (indices 20-24)
        DOOR(25, 30),           // Portes (indices 25-29)
        BUSH(30, 35),           // Buissons (indices 30-34)
        TREE_TRUNK(35, 40);     // Troncs d'arbres (indices 35-39)

        public final int startIndex, endIndex;
        WallType(int start, int end) { this.startIndex = start; this.endIndex = end; }

        public int getRandomIndex() {
            return startIndex + random.nextInt(endIndex - startIndex);
        }
    }

    public enum CeilingType {
        THATCH_ROOF(0, 10),     // Toit de chaume (indices 0-9)
        TILE_ROOF(10, 15),      // Toit de tuiles (indices 10-14)
        WOOD_ROOF(15, 20),      // Toit en bois (indices 15-19)
        LEAVES(20, 25);         // Feuillage (indices 20-24)

        public final int startIndex, endIndex;
        CeilingType(int start, int end) { this.startIndex = start; this.endIndex = end; }

        public int getRandomIndex() {
            return startIndex + random.nextInt(endIndex - startIndex);
        }
    }

    // Structure pour représenter une maison
    static class House {
        int x, y, width, height;
        boolean hasDoor;
        List<Point> windows = new ArrayList<>();

        House(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    // Structure pour représenter les propriétés des murs
    static class WallProperties {
        boolean isOpen = false;     // Porte ouverte/fermée
        boolean isLocked = false;   // Porte verrouillée
        String keyId = null;        // ID de clé unique
        int health = 255;          // Vie des murs destructibles (0-255)

        WallProperties() {}

        WallProperties(boolean isOpen, boolean isLocked, String keyId, int health) {
            this.isOpen = isOpen;
            this.isLocked = isLocked;
            this.keyId = keyId;
            this.health = Math.max(0, Math.min(255, health));
        }
    }

    // Structure pour représenter une zone de forêt
    static class ForestArea {
        int centerX, centerY, radius;
        double density;

        ForestArea(int centerX, int centerY, int radius, double density) {
            this.centerX = centerX; this.centerY = centerY;
            this.radius = radius; this.density = density;
        }
    }

    public static class GeneratedMap {
        public int[][] floorMap;
        public int[][] wallMap;
        public int[][] ceilingMap;
        public IsometricGame.WallType[][] wallTypes;
        public WallProperties[][] wallProperties; // Nouvelles propriétés
        public List<IsometricGame.Item>[][] itemMap;

        @SuppressWarnings("unchecked")
        public GeneratedMap(int size) {
            floorMap = new int[size][size];
            wallMap = new int[size][size];
            ceilingMap = new int[size][size];
            wallTypes = new IsometricGame.WallType[size][size];
            wallProperties = new WallProperties[size][size]; // Initialiser les propriétés
            itemMap = new List[size][size];

            // Initialiser les listes d'items et propriétés des murs
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    itemMap[x][y] = new ArrayList<>();
                    wallTypes[x][y] = IsometricGame.WallType.NONE;
                    wallProperties[x][y] = new WallProperties(); // Propriétés par défaut
                    wallMap[x][y] = -1;
                    ceilingMap[x][y] = -1;
                }
            }
        }
    }

    /**
     * Génère une carte réaliste avec village et forêts et la sauvegarde en JSON
     */
    public static GeneratedMap generateVillageMap(int mapSize) {
        return generateVillageMap(mapSize, "generated_map.json");
    }

    /**
     * Génère une carte réaliste avec village et forêts et la sauvegarde en JSON
     */
    public static GeneratedMap generateVillageMap(int mapSize, String outputPath) {
        GeneratedMap map = new GeneratedMap(mapSize);

        // 1. Génération du terrain de base (herbe)
        generateBaseTerrain(map);

        // 2. Génération des zones de forêt
        List<ForestArea> forests = generateForests(map, 3, 8);

        // 3. Génération des chemins
        generatePaths(map);

        // 4. Génération des maisons du village
        List<House> houses = generateVillage(map);

        // 5. Génération des arbres dans les forêts
        generateTrees(map, forests);

        // 6. Génération des buissons
        generateBushes(map);

        // 7. Ajout des portes et fenêtres aux maisons
        addDoorsAndWindows(map, houses);

        // 8. Configuration des propriétés des murs (portes verrouillées, santé des murs)
        configureWallProperties(map);

        // 9. Génération des toits
        generateRoofs(map, houses, forests);

        // 10. Génération des items (incluant les clés)
        generateItems(map);

        // 11. Ajout de détails (lac, pont, etc.)
        addEnvironmentalDetails(map);

        // 11. Sauvegarde en JSON
        try {
            saveMapToJson(map, outputPath);
            System.out.println("Carte sauvegardée en JSON : " + outputPath);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde JSON : " + e.getMessage());
        }

        return map;
    }

    private static void generateBaseTerrain(GeneratedMap map) {
        // Remplir toute la carte d'herbe par défaut
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                map.floorMap[x][y] = FloorType.GRASS.getRandomIndex();
            }
        }
    }

    private static List<ForestArea> generateForests(GeneratedMap map, int minForests, int maxForests) {
        List<ForestArea> forests = new ArrayList<>();
        int numForests = minForests + random.nextInt(maxForests - minForests + 1);

        for (int i = 0; i < numForests; i++) {
            int centerX = 10 + random.nextInt(MAP_SIZE - 20);
            int centerY = 10 + random.nextInt(MAP_SIZE - 20);
            int radius = 8 + random.nextInt(12);
            double density = 0.4 + random.nextDouble() * 0.4;

            forests.add(new ForestArea(centerX, centerY, radius, density));
        }

        return forests;
    }

    private static void generatePaths(GeneratedMap map) {
        // Chemin principal horizontal
        int pathY = MAP_SIZE / 2;
        for (int x = 0; x < MAP_SIZE; x++) {
            int width = 2 + random.nextInt(2);
            for (int dy = -width/2; dy <= width/2; dy++) {
                if (pathY + dy >= 0 && pathY + dy < MAP_SIZE) {
                    map.floorMap[x][pathY + dy] = FloorType.DIRT_PATH.getRandomIndex();
                }
            }
        }

        // Chemin principal vertical
        int pathX = MAP_SIZE / 2;
        for (int y = 0; y < MAP_SIZE; y++) {
            int width = 2 + random.nextInt(2);
            for (int dx = -width/2; dx <= width/2; dx++) {
                if (pathX + dx >= 0 && pathX + dx < MAP_SIZE) {
                    map.floorMap[pathX + dx][y] = FloorType.DIRT_PATH.getRandomIndex();
                }
            }
        }

        // Chemins secondaires
        for (int i = 0; i < 3; i++) {
            int startX = random.nextInt(MAP_SIZE);
            int startY = random.nextInt(MAP_SIZE);
            int length = 15 + random.nextInt(25);

            createRandomPath(map, startX, startY, length);
        }
    }

    private static void createRandomPath(GeneratedMap map, int startX, int startY, int length) {
        int currentX = startX, currentY = startY;

        for (int i = 0; i < length; i++) {
            if (currentX >= 0 && currentX < MAP_SIZE && currentY >= 0 && currentY < MAP_SIZE) {
                map.floorMap[currentX][currentY] = FloorType.DIRT_PATH.getRandomIndex();

                // Élargir le chemin parfois
                if (random.nextDouble() < 0.3) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int newX = currentX + dx;
                            int newY = currentY + dy;
                            if (newX >= 0 && newX < MAP_SIZE && newY >= 0 && newY < MAP_SIZE) {
                                map.floorMap[newX][newY] = FloorType.DIRT_PATH.getRandomIndex();
                            }
                        }
                    }
                }
            }

            // Direction aléatoire
            int direction = random.nextInt(4);
            switch (direction) {
                case 0: currentX++; break;
                case 1: currentX--; break;
                case 2: currentY++; break;
                case 3: currentY--; break;
            }
        }
    }

    private static List<House> generateVillage(GeneratedMap map) {
        List<House> houses = new ArrayList<>();

        // Centre du village
        int villageX = MAP_SIZE / 2;
        int villageY = MAP_SIZE / 2;

        // Génération de 5-10 maisons autour du centre
        int numHouses = 5 + random.nextInt(6);

        for (int i = 0; i < numHouses; i++) {
            int attempts = 0;
            while (attempts < 50) {
                // Position aléatoire autour du centre
                int houseX = villageX + (random.nextInt(30) - 15);
                int houseY = villageY + (random.nextInt(30) - 15);

                int width = 3 + random.nextInt(4);
                int height = 3 + random.nextInt(4);

                if (canPlaceHouse(map, houseX, houseY, width, height)) {
                    House house = new House(houseX, houseY, width, height);
                    buildHouse(map, house);
                    houses.add(house);
                    break;
                }
                attempts++;
            }
        }

        return houses;
    }

    private static boolean canPlaceHouse(GeneratedMap map, int x, int y, int width, int height) {
        // Vérifier si la zone est libre (avec marge)
        for (int dx = -1; dx <= width + 1; dx++) {
            for (int dy = -1; dy <= height + 1; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;

                if (checkX < 0 || checkX >= MAP_SIZE || checkY < 0 || checkY >= MAP_SIZE) {
                    return false;
                }

                if (map.wallMap[checkX][checkY] != -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void buildHouse(GeneratedMap map, House house) {
        // Sol intérieur en bois
        for (int x = house.x + 1; x < house.x + house.width - 1; x++) {
            for (int y = house.y + 1; y < house.y + house.height - 1; y++) {
                if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                    map.floorMap[x][y] = FloorType.WOOD_FLOOR.getRandomIndex();
                }
            }
        }

        // Murs extérieurs
        for (int x = house.x; x < house.x + house.width; x++) {
            for (int y = house.y; y < house.y + house.height; y++) {
                if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                    // Murs sur les bords
                    if (x == house.x || x == house.x + house.width - 1 ||
                            y == house.y || y == house.y + house.height - 1) {

                        map.wallMap[x][y] = WallType.WOOD_WALL.getRandomIndex();
                        map.wallTypes[x][y] = IsometricGame.WallType.INDESTRUCTIBLE;
                    }
                }
            }
        }
    }

    private static void generateTrees(GeneratedMap map, List<ForestArea> forests) {
        for (ForestArea forest : forests) {
            for (int x = forest.centerX - forest.radius; x <= forest.centerX + forest.radius; x++) {
                for (int y = forest.centerY - forest.radius; y <= forest.centerY + forest.radius; y++) {
                    if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                        double distance = Math.sqrt(Math.pow(x - forest.centerX, 2) + Math.pow(y - forest.centerY, 2));

                        if (distance <= forest.radius) {
                            double probability = forest.density * (1 - distance / forest.radius);

                            if (random.nextDouble() < probability && map.wallMap[x][y] == -1) {
                                // Placer un arbre
                                map.wallMap[x][y] = WallType.TREE_TRUNK.getRandomIndex();
                                map.wallTypes[x][y] = IsometricGame.WallType.INDESTRUCTIBLE;

                                // Feuillage au-dessus
                                map.ceilingMap[x][y] = CeilingType.LEAVES.getRandomIndex();
                            }
                        }
                    }
                }
            }
        }
    }

    private static void generateBushes(GeneratedMap map) {
        // Buissons dispersés dans toute la carte
        for (int i = 0; i < MAP_SIZE * MAP_SIZE / 50; i++) {
            int x = random.nextInt(MAP_SIZE);
            int y = random.nextInt(MAP_SIZE);

            if (map.wallMap[x][y] == -1 && map.floorMap[x][y] < FloorType.DIRT_PATH.startIndex) {
                map.wallMap[x][y] = WallType.BUSH.getRandomIndex();
                map.wallTypes[x][y] = IsometricGame.WallType.TRAVERSABLE;
            }
        }
    }

    private static void addDoorsAndWindows(GeneratedMap map, List<House> houses) {
        for (House house : houses) {
            // Ajouter une porte
            addDoor(map, house);

            // Ajouter 1-3 fenêtres
            int numWindows = 1 + random.nextInt(3);
            for (int i = 0; i < numWindows; i++) {
                addWindow(map, house);
            }
        }
    }

    private static void addDoor(GeneratedMap map, House house) {
        // Choisir un mur aléatoire pour la porte
        List<Point> wallPositions = new ArrayList<>();

        // Murs horizontaux
        for (int x = house.x + 1; x < house.x + house.width - 1; x++) {
            wallPositions.add(new Point(x, house.y));
            wallPositions.add(new Point(x, house.y + house.height - 1));
        }

        // Murs verticaux
        for (int y = house.y + 1; y < house.y + house.height - 1; y++) {
            wallPositions.add(new Point(house.x, y));
            wallPositions.add(new Point(house.x + house.width - 1, y));
        }

        if (!wallPositions.isEmpty()) {
            Point doorPos = wallPositions.get(random.nextInt(wallPositions.size()));
            map.wallMap[doorPos.x][doorPos.y] = WallType.DOOR.getRandomIndex();
            map.wallTypes[doorPos.x][doorPos.y] = IsometricGame.WallType.DOOR;

            // Configurer les propriétés de la porte
            WallProperties props = map.wallProperties[doorPos.x][doorPos.y];
            props.isOpen = false; // Porte fermée par défaut

            // 30% de chance d'être verrouillée
            if (random.nextDouble() < 0.3) {
                props.isLocked = true;
                props.keyId = "house_key_" + house.x + "_" + house.y; // ID basé sur la position de la maison
            }
        }
    }

    private static void addWindow(GeneratedMap map, House house) {
        // Similaire à la porte mais avec des fenêtres
        List<Point> wallPositions = new ArrayList<>();

        for (int x = house.x + 1; x < house.x + house.width - 1; x++) {
            if (map.wallMap[x][house.y] != -1 && map.wallTypes[x][house.y] != IsometricGame.WallType.DOOR) {
                wallPositions.add(new Point(x, house.y));
            }
            if (map.wallMap[x][house.y + house.height - 1] != -1 && map.wallTypes[x][house.y + house.height - 1] != IsometricGame.WallType.DOOR) {
                wallPositions.add(new Point(x, house.y + house.height - 1));
            }
        }

        for (int y = house.y + 1; y < house.y + house.height - 1; y++) {
            if (map.wallMap[house.x][y] != -1 && map.wallTypes[house.x][y] != IsometricGame.WallType.DOOR) {
                wallPositions.add(new Point(house.x, y));
            }
            if (map.wallMap[house.x + house.width - 1][y] != -1 && map.wallTypes[house.x + house.width - 1][y] != IsometricGame.WallType.DOOR) {
                wallPositions.add(new Point(house.x + house.width - 1, y));
            }
        }

        if (!wallPositions.isEmpty()) {
            Point windowPos = wallPositions.get(random.nextInt(wallPositions.size()));
            map.wallMap[windowPos.x][windowPos.y] = WallType.GLASS_WINDOW.getRandomIndex();
            map.wallTypes[windowPos.x][windowPos.y] = IsometricGame.WallType.TRANSPARENT;
        }
    }

    private static void generateRoofs(GeneratedMap map, List<House> houses, List<ForestArea> forests) {
        // Toits pour les maisons
        for (House house : houses) {
            for (int x = house.x; x < house.x + house.width; x++) {
                for (int y = house.y; y < house.y + house.height; y++) {
                    if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                        // Choisir le type de toit
                        CeilingType roofType = random.nextBoolean() ? CeilingType.THATCH_ROOF : CeilingType.TILE_ROOF;
                        map.ceilingMap[x][y] = roofType.getRandomIndex();
                    }
                }
            }
        }
    }

    private static void configureWallProperties(GeneratedMap map) {
        System.out.println("Configuration des propriétés des murs...");

        int lockedDoors = 0;
        int destructibleWalls = 0;

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                WallProperties props = map.wallProperties[x][y];
                IsometricGame.WallType wallType = map.wallTypes[x][y];

                if (wallType == IsometricGame.WallType.DOOR) {
                    // Configuration des portes
                    props.isOpen = false; // Toutes fermées par défaut

                    // Portes de maison déjà configurées dans addDoor()
                    // Ajouter quelques portes verrouillées aléatoires
                    if (props.keyId == null && random.nextDouble() < 0.1) {
                        props.isLocked = true;
                        props.keyId = "random_key_" + (x * MAP_SIZE + y);
                        lockedDoors++;
                    }

                } else if (wallType == IsometricGame.WallType.DESTRUCTIBLE) {
                    // Configuration des murs destructibles
                    props.health = 80 + random.nextInt(176); // Santé entre 80 et 255
                    destructibleWalls++;

                } else if (wallType == IsometricGame.WallType.TRANSPARENT) {
                    // Les fenêtres peuvent être cassées
                    if (random.nextDouble() < 0.2) {
                        props.health = 30 + random.nextInt(50); // Fenêtres fragiles (30-80)
                    }
                }
            }
        }

        System.out.println("✅ Propriétés configurées:");
        System.out.println("  - Portes verrouillées: " + lockedDoors);
        System.out.println("  - Murs destructibles: " + destructibleWalls);
    }
    private static void generateItems(GeneratedMap map) {
        System.out.println("Génération des items et des clés...");

        Set<String> keysGenerated = new HashSet<>();
        int totalKeys = 0;

        // Collecter toutes les clés nécessaires
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                WallProperties props = map.wallProperties[x][y];
                if (props.keyId != null) {
                    keysGenerated.add(props.keyId);
                }
            }
        }

        // Items dans les maisons
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (map.floorMap[x][y] >= FloorType.WOOD_FLOOR.startIndex &&
                        map.floorMap[x][y] < FloorType.WOOD_FLOOR.endIndex) {

                    if (random.nextDouble() < 0.15) { // 15% chance dans les maisons
                        // Chance d'avoir une clé dans les maisons
                        if (!keysGenerated.isEmpty() && random.nextDouble() < 0.3) {
                            String randomKey = keysGenerated.toArray(new String[0])[random.nextInt(keysGenerated.size())];
                            map.itemMap[x][y].add(new IsometricGame.Item("key_" + randomKey, 1));
                            keysGenerated.remove(randomKey); // Éviter les doublons
                            totalKeys++;
                        } else {
                            // Items normaux
                            addRandomItems(map, x, y, true);
                        }
                    }
                } else if (map.wallTypes[x][y] == IsometricGame.WallType.TRAVERSABLE) {
                    // Items dans les buissons
                    if (random.nextDouble() < 0.05) { // 5% chance
                        addRandomItems(map, x, y, false);
                    }
                } else if (random.nextDouble() < 0.02) { // 2% chance partout ailleurs
                    addRandomItems(map, x, y, false);
                }
            }
        }

        // Placer les clés restantes aléatoirement
        List<String> remainingKeys = new ArrayList<>(keysGenerated);
        for (String keyId : remainingKeys) {
            // Trouver une position aléatoire accessible
            for (int attempts = 0; attempts < 50; attempts++) {
                int x = random.nextInt(MAP_SIZE);
                int y = random.nextInt(MAP_SIZE);

                if (map.wallTypes[x][y] == IsometricGame.WallType.NONE && map.itemMap[x][y].isEmpty()) {
                    map.itemMap[x][y].add(new IsometricGame.Item("key_" + keyId, 1));
                    totalKeys++;
                    break;
                }
            }
        }

        System.out.println("✅ Items générés, dont " + totalKeys + " clés");
    }

    private static void addRandomItems(GeneratedMap map, int x, int y, boolean isIndoors) {
        String[] indoorItems = {"coin", "potion", "key", "scroll", "gem"};
        String[] outdoorItems = {"herb", "berry", "stick", "stone", "flower"};

        String[] itemTypes = isIndoors ? indoorItems : outdoorItems;
        String itemType = itemTypes[random.nextInt(itemTypes.length)];
        int count = 1 + random.nextInt(3);

        map.itemMap[x][y].add(new IsometricGame.Item(itemType, count));
    }

    private static void addEnvironmentalDetails(GeneratedMap map) {
        // Petit lac
        int lakeX = 15 + random.nextInt(MAP_SIZE - 30);
        int lakeY = 15 + random.nextInt(MAP_SIZE - 30);
        int lakeRadius = 3 + random.nextInt(4);

        for (int x = lakeX - lakeRadius; x <= lakeX + lakeRadius; x++) {
            for (int y = lakeY - lakeRadius; y <= lakeY + lakeRadius; y++) {
                if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                    double distance = Math.sqrt(Math.pow(x - lakeX, 2) + Math.pow(y - lakeY, 2));
                    if (distance <= lakeRadius) {
                        map.floorMap[x][y] = FloorType.WATER.getRandomIndex();
                        // Retirer les murs/plafonds dans l'eau
                        map.wallMap[x][y] = -1;
                        map.ceilingMap[x][y] = -1;
                        map.wallTypes[x][y] = IsometricGame.WallType.NONE;
                    }
                }
            }
        }

        // Zones de sable près de l'eau
        for (int x = lakeX - lakeRadius - 2; x <= lakeX + lakeRadius + 2; x++) {
            for (int y = lakeY - lakeRadius - 2; y <= lakeY + lakeRadius + 2; y++) {
                if (x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE) {
                    double distance = Math.sqrt(Math.pow(x - lakeX, 2) + Math.pow(y - lakeY, 2));
                    if (distance > lakeRadius && distance <= lakeRadius + 2 && random.nextDouble() < 0.4) {
                        if (map.floorMap[x][y] < FloorType.DIRT_PATH.startIndex) {
                            map.floorMap[x][y] = FloorType.SAND.getRandomIndex();
                        }
                    }
                }
            }
        }
    }

    /**
     * Sauvegarde la carte générée en format JSON
     */
    private static void saveMapToJson(GeneratedMap map, String filePath) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"mapSize\": ").append(MAP_SIZE).append(",\n");
        json.append("  \"generatedAt\": \"").append(new Date()).append("\",\n");

        // Sauvegarde de la carte des sols
        json.append("  \"floorMap\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append(map.floorMap[x][y]);
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Sauvegarde de la carte des murs
        json.append("  \"wallMap\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append(map.wallMap[x][y]);
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Sauvegarde de la carte des plafonds
        json.append("  \"ceilingMap\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append(map.ceilingMap[x][y]);
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Sauvegarde des types de murs
        json.append("  \"wallTypes\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append("\"").append(map.wallTypes[x][y].name()).append("\"");
                if (y < MAP_SIZE - 1) json.append(", ");
            }
            json.append("]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Sauvegarde des propriétés des murs (nouveau)
        json.append("  \"wallProperties\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [\n");
            for (int y = 0; y < MAP_SIZE; y++) {
                WallProperties props = map.wallProperties[x][y];
                json.append("      {");
                json.append("\"isOpen\":").append(props.isOpen).append(", ");
                json.append("\"isLocked\":").append(props.isLocked).append(", ");
                json.append("\"keyId\":").append(props.keyId != null ? "\"" + props.keyId + "\"" : "null").append(", ");
                json.append("\"health\":").append(props.health);
                json.append("}");
                if (y < MAP_SIZE - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Sauvegarde des items
        json.append("  \"itemMap\": [\n");
        for (int x = 0; x < MAP_SIZE; x++) {
            json.append("    [\n");
            for (int y = 0; y < MAP_SIZE; y++) {
                json.append("      [");
                List<IsometricGame.Item> items = map.itemMap[x][y];
                for (int i = 0; i < items.size(); i++) {
                    IsometricGame.Item item = items.get(i);
                    json.append("{\"type\":\"").append(item.type).append("\", \"count\":").append(item.count).append("}");
                    if (i < items.size() - 1) json.append(", ");
                }
                json.append("]");
                if (y < MAP_SIZE - 1) json.append(",");
                json.append("\n");
            }
            json.append("    ]");
            if (x < MAP_SIZE - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        // Écriture du fichier
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json.toString());
        }
    }

    /**
     * Charge une carte depuis un fichier JSON (méthode utilitaire pour plus tard)
     */
    public static GeneratedMap loadMapFromJson(String filePath) {
        // Cette méthode pourra être implémentée plus tard pour charger des cartes sauvegardées
        System.out.println("Chargement depuis JSON pas encore implémenté : " + filePath);
        return null;
    }

    /**
     * Méthode utilitaire pour tester le générateur de cartes
     */
    public static void main(String[] args) {
        System.out.println("=== Générateur de Cartes Réalistes ===");
        System.out.println("Génération d'une carte de village avec forêts...");

        GeneratedMap map = generateVillageMap(100, "village_map.json");

        // Statistiques de génération
        int totalWalls = 0, totalItems = 0, totalKeys = 0;
        int totalDoors = 0, totalLockedDoors = 0;
        Map<String, Integer> itemStats = new HashMap<>();

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (map.wallMap[x][y] != -1) {
                    totalWalls++;
                    if (map.wallTypes[x][y] == IsometricGame.WallType.DOOR) {
                        totalDoors++;
                        if (map.wallProperties[x][y].isLocked) {
                            totalLockedDoors++;
                        }
                    }
                }

                for (IsometricGame.Item item : map.itemMap[x][y]) {
                    totalItems += item.count;
                    if (item.type.startsWith("key_")) {
                        totalKeys += item.count;
                    }
                    itemStats.put(item.type, itemStats.getOrDefault(item.type, 0) + item.count);
                }
            }
        }

        System.out.println("\n=== Statistiques de génération ===");
        System.out.println("Taille de la carte : " + MAP_SIZE + "x" + MAP_SIZE);
        System.out.println("Nombre total de murs/obstacles : " + totalWalls);
        System.out.println("Portes totales : " + totalDoors + " (dont " + totalLockedDoors + " verrouillées)");
        System.out.println("Nombre total d'items : " + totalItems + " (dont " + totalKeys + " clés)");
        System.out.println("Répartition des items :");
        itemStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach((entry) ->
                        System.out.println("  " + entry.getKey() + " : " + entry.getValue())
                );

        System.out.println("\nCarte sauvegardée avec succès !");
    }
}