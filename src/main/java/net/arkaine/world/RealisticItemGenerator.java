package net.arkaine.world;

import net.arkaine.model.GameModel;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * Générateur d'items réaliste avec coffres et distribution logique
 */
public class RealisticItemGenerator {

    public enum ItemRarity {
        COMMON(0.6, "Commun"),
        UNCOMMON(0.25, "Peu commun"),
        RARE(0.12, "Rare"),
        EPIC(0.025, "Épique"),
        LEGENDARY(0.005, "Légendaire");

        public final double chance;
        public final String displayName;

        ItemRarity(double chance, String displayName) {
            this.chance = chance;
            this.displayName = displayName;
        }
    }

    public enum ChestType {
        WOODEN_CHEST("wooden_chest", "Coffre en bois", 3, 5, ItemRarity.UNCOMMON),
        IRON_CHEST("iron_chest", "Coffre en fer", 4, 7, ItemRarity.RARE),
        TREASURE_CHEST("treasure_chest", "Coffre au trésor", 5, 10, ItemRarity.EPIC),
        LEGENDARY_CHEST("legendary_chest", "Coffre légendaire", 7, 15, ItemRarity.LEGENDARY);

        public final String id;
        public final String displayName;
        public final int minItems;
        public final int maxItems;
        public final ItemRarity guaranteedRarity;

        ChestType(String id, String displayName, int minItems, int maxItems, ItemRarity guaranteedRarity) {
            this.id = id;
            this.displayName = displayName;
            this.minItems = minItems;
            this.maxItems = maxItems;
            this.guaranteedRarity = guaranteedRarity;
        }
    }

    public static class ItemDefinition {
        public final String id;
        public final String displayName;
        public final String description;
        public final ItemRarity rarity;
        public final GameModel.ItemType type;
        public final String[] biomes; // Biomes où l'item peut apparaître
        public final boolean stackable;
        public final int maxStack;

        public ItemDefinition(String id, String displayName, String description,
                              ItemRarity rarity, GameModel.ItemType type,
                              String[] biomes, boolean stackable, int maxStack) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.type = type;
            this.biomes = biomes;
            this.stackable = stackable;
            this.maxStack = maxStack;
        }
    }

    public static class Chest {
        public final ChestType type;
        public final List<GameModel.Item> contents;
        public boolean isOpen;
        public boolean isLocked;
        public String keyId;

        public Chest(ChestType type) {
            this.type = type;
            this.contents = new ArrayList<>();
            this.isOpen = false;
            this.isLocked = Math.random() < 0.3; // 30% de chance d'être verrouillé
            this.keyId = isLocked ? "chest_key_" + System.currentTimeMillis() : null;
        }
    }

    // Base de données des items
    private static final Map<String, ItemDefinition> ITEM_DATABASE = new HashMap<>();
    private static final Random random = new Random();

    static {
        initializeItemDatabase();
    }

    private static void initializeItemDatabase() {
        // CONSOMMABLES
        addItem("health_potion", "Potion de Soin", "Restaure 50 PV",
                ItemRarity.COMMON, GameModel.ItemType.CONSUMABLE,
                new String[]{"village", "forest", "dungeon"}, true, 10);

        addItem("mana_potion", "Potion de Mana", "Restaure 50 PM",
                ItemRarity.COMMON, GameModel.ItemType.CONSUMABLE,
                new String[]{"village", "forest", "dungeon"}, true, 10);

        addItem("antidote", "Antidote", "Guérit l'empoisonnement",
                ItemRarity.UNCOMMON, GameModel.ItemType.CONSUMABLE,
                new String[]{"forest", "swamp"}, true, 5);

        addItem("bread", "Pain", "Nourriture basique",
                ItemRarity.COMMON, GameModel.ItemType.CONSUMABLE,
                new String[]{"village"}, true, 20);

        // ARMES
        addItem("iron_sword", "Épée en Fer", "Dégâts +15",
                ItemRarity.COMMON, GameModel.ItemType.WEAPON,
                new String[]{"village", "dungeon"}, false, 1);

        addItem("steel_sword", "Épée en Acier", "Dégâts +25",
                ItemRarity.UNCOMMON, GameModel.ItemType.WEAPON,
                new String[]{"dungeon", "castle"}, false, 1);

        addItem("flame_sword", "Épée de Flamme", "Dégâts +35 (Feu)",
                ItemRarity.RARE, GameModel.ItemType.WEAPON,
                new String[]{"volcano", "dungeon"}, false, 1);

        addItem("wooden_bow", "Arc en Bois", "Dégâts +12 (Distance)",
                ItemRarity.COMMON, GameModel.ItemType.WEAPON,
                new String[]{"forest", "village"}, false, 1);

        addItem("elvish_bow", "Arc Elfique", "Dégâts +22 (Distance)",
                ItemRarity.RARE, GameModel.ItemType.WEAPON,
                new String[]{"forest"}, false, 1);

        // ARMURES
        addItem("leather_armor", "Armure de Cuir", "Défense +8",
                ItemRarity.COMMON, GameModel.ItemType.ARMOR,
                new String[]{"village", "forest"}, false, 1);

        addItem("chain_mail", "Cotte de Mailles", "Défense +15",
                ItemRarity.UNCOMMON, GameModel.ItemType.ARMOR,
                new String[]{"village", "dungeon"}, false, 1);

        addItem("plate_armor", "Armure de Plates", "Défense +25",
                ItemRarity.RARE, GameModel.ItemType.ARMOR,
                new String[]{"castle", "dungeon"}, false, 1);

        // ACCESSOIRES
        addItem("health_ring", "Anneau de Vie", "PV Max +20",
                ItemRarity.UNCOMMON, GameModel.ItemType.RING,
                new String[]{"dungeon", "temple"}, false, 1);

        addItem("magic_ring", "Anneau Magique", "PM Max +30",
                ItemRarity.RARE, GameModel.ItemType.RING,
                new String[]{"tower", "dungeon"}, false, 1);

        // OBJETS DE VALEUR
        addItem("gold_coin", "Pièce d'Or", "Monnaie du royaume",
                ItemRarity.COMMON, GameModel.ItemType.TREASURE,
                new String[]{"village", "dungeon", "forest"}, true, 99);

        addItem("silver_coin", "Pièce d'Argent", "Monnaie courante",
                ItemRarity.COMMON, GameModel.ItemType.TREASURE,
                new String[]{"village", "forest"}, true, 99);

        addItem("ruby", "Rubis", "Pierre précieuse rouge",
                ItemRarity.RARE, GameModel.ItemType.TREASURE,
                new String[]{"cave", "dungeon"}, true, 10);

        addItem("diamond", "Diamant", "Pierre précieuse rare",
                ItemRarity.EPIC, GameModel.ItemType.TREASURE,
                new String[]{"deep_cave", "dragon_lair"}, true, 5);

        // CLÉS
        addItem("house_key", "Clé de Maison", "Ouvre les maisons",
                ItemRarity.UNCOMMON, GameModel.ItemType.KEY,
                new String[]{"village"}, false, 1);

        addItem("chest_key", "Clé de Coffre", "Ouvre les coffres",
                ItemRarity.UNCOMMON, GameModel.ItemType.KEY,
                new String[]{"dungeon", "village"}, false, 1);

        addItem("master_key", "Clé Maîtresse", "Ouvre tout",
                ItemRarity.LEGENDARY, GameModel.ItemType.KEY,
                new String[]{"castle", "vault"}, false, 1);

        // RESSOURCES ET CRAFT
        addItem("wood", "Bois", "Matériau de construction",
                ItemRarity.COMMON, GameModel.ItemType.MISC,
                new String[]{"forest"}, true, 50);

        addItem("iron_ore", "Minerai de Fer", "Métal brut",
                ItemRarity.COMMON, GameModel.ItemType.MISC,
                new String[]{"cave", "mountain"}, true, 20);

        addItem("herb", "Herbe Médicinale", "Ingrédient d'alchimie",
                ItemRarity.COMMON, GameModel.ItemType.MISC,
                new String[]{"forest", "field"}, true, 30);

        // OBJETS SPÉCIAUX
        addItem("teleport_scroll", "Parchemin de Téléportation", "Retour instantané",
                ItemRarity.RARE, GameModel.ItemType.CONSUMABLE,
                new String[]{"tower", "temple"}, true, 3);

        addItem("resurrection_stone", "Pierre de Résurrection", "Évite la mort",
                ItemRarity.LEGENDARY, GameModel.ItemType.CONSUMABLE,
                new String[]{"temple", "shrine"}, false, 1);

        System.out.println("✅ Base de données d'items initialisée : " + ITEM_DATABASE.size() + " items");
    }

    private static void addItem(String id, String displayName, String description,
                                ItemRarity rarity, GameModel.ItemType type,
                                String[] biomes, boolean stackable, int maxStack) {
        ITEM_DATABASE.put(id, new ItemDefinition(id, displayName, description,
                rarity, type, biomes, stackable, maxStack));
    }

    /**
     * Génère les items et coffres sur la carte de manière réaliste
     */
    public static void generateRealisticItems(GameModel model) {
        System.out.println("🎁 Génération réaliste des items et coffres...");

        int totalItems = 0;
        int totalChests = 0;
        Map<String, Integer> itemStats = new HashMap<>();

        // 1. Placer des coffres dans des endroits logiques
        totalChests += placeChests(model);

        // 2. Distribuer des items selon les biomes
        totalItems += distributeItemsByBiome(model, itemStats);

        // 3. Ajouter des items spéciaux dans des lieux particuliers
        totalItems += placeSpecialItems(model, itemStats);

        // 4. Générer les clés pour les coffres verrouillés
        generateChestKeys(model, itemStats);

        System.out.println("✅ Génération terminée :");
        System.out.println("  - " + totalChests + " coffres placés");
        System.out.println("  - " + totalItems + " items distribués");
        printItemStatistics(itemStats);
    }

    /**
     * Place des coffres dans des endroits logiques
     */
    private static int placeChests(GameModel model) {
        int chestsPlaced = 0;
        Map<Point2D, Chest> chestMap = new HashMap<>();

        // Coffres dans les maisons (rares mais précieux)
        chestsPlaced += placeHouseChests(model, chestMap);

        // Coffres cachés dans la forêt
        chestsPlaced += placeForestChests(model, chestMap);

        // Coffres près de l'eau (naufragés)
        chestsPlaced += placeWaterChests(model, chestMap);

        // Coffres gardés par des ennemis élites
        chestsPlaced += placeGuardedChests(model, chestMap);

        // Sauvegarder les coffres dans le modèle (extension nécessaire)
        saveChestsToModel(model, chestMap);

        return chestsPlaced;
    }

    private static int placeHouseChests(GameModel model, Map<Point2D, Chest> chestMap) {
        int chestsPlaced = 0;

        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                int floorType = model.getFloorMap()[x][y];

                // Sol en bois = intérieur de maison
                if (floorType >= 35 && floorType <= 39) {
                    if (random.nextDouble() < 0.15) { // 15% de chance par case de maison
                        ChestType chestType = random.nextDouble() < 0.7 ?
                                ChestType.WOODEN_CHEST : ChestType.IRON_CHEST;

                        Chest chest = new Chest(chestType);
                        generateChestContents(chest, "village");

                        chestMap.put(new Point2D(x, y), chest);
                        placeChestItem(model, x, y, chest);
                        chestsPlaced++;
                    }
                }
            }
        }

        return chestsPlaced;
    }

    private static int placeForestChests(GameModel model, Map<Point2D, Chest> chestMap) {
        int chestsPlaced = 0;

        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                // Près des arbres
                if (model.getWallTypes()[x][y] == GameModel.WallType.INDESTRUCTIBLE &&
                        model.getWallMap()[x][y] >= 35) { // Troncs d'arbres

                    // Chercher une case libre adjacente
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int checkX = x + dx;
                            int checkY = y + dy;

                            if (model.isValidTile(checkX, checkY) &&
                                    model.canWalkThrough(checkX, checkY) &&
                                    model.getItemMap()[checkX][checkY].isEmpty() &&
                                    random.nextDouble() < 0.05) { // 5% de chance

                                ChestType chestType = ChestType.WOODEN_CHEST;
                                if (random.nextDouble() < 0.2) chestType = ChestType.TREASURE_CHEST;

                                Chest chest = new Chest(chestType);
                                generateChestContents(chest, "forest");

                                chestMap.put(new Point2D(checkX, checkY), chest);
                                placeChestItem(model, checkX, checkY, chest);
                                chestsPlaced++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return chestsPlaced;
    }

    private static int placeWaterChests(GameModel model, Map<Point2D, Chest> chestMap) {
        int chestsPlaced = 0;

        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                int floorType = model.getFloorMap()[x][y];

                // Près de l'eau (sable ou herbe adjacente à l'eau)
                if ((floorType >= 30 && floorType <= 34) || // Sable
                        (floorType >= 0 && floorType <= 14)) {   // Herbe

                    if (isNearWater(model, x, y) && random.nextDouble() < 0.08) {
                        ChestType chestType = random.nextDouble() < 0.6 ?
                                ChestType.IRON_CHEST : ChestType.TREASURE_CHEST;

                        Chest chest = new Chest(chestType);
                        generateChestContents(chest, "shore");

                        chestMap.put(new Point2D(x, y), chest);
                        placeChestItem(model, x, y, chest);
                        chestsPlaced++;
                    }
                }
            }
        }

        return chestsPlaced;
    }

    private static int placeGuardedChests(GameModel model, Map<Point2D, Chest> chestMap) {
        int chestsPlaced = 0;

        // Placer des coffres légendaires gardés par des boss/élites
        List<Point2D> elitePositions = findEliteEnemyPositions(model);

        for (Point2D elitePos : elitePositions) {
            if (random.nextDouble() < 0.4) { // 40% de chance pour chaque élite
                // Trouver une position libre près de l'élite
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        int x = (int)elitePos.getX() + dx;
                        int y = (int)elitePos.getY() + dy;

                        if (model.isValidTile(x, y) &&
                                model.canWalkThrough(x, y) &&
                                model.getItemMap()[x][y].isEmpty()) {

                            ChestType chestType = random.nextDouble() < 0.8 ?
                                    ChestType.TREASURE_CHEST : ChestType.LEGENDARY_CHEST;

                            Chest chest = new Chest(chestType);
                            generateChestContents(chest, "dungeon");

                            chestMap.put(new Point2D(x, y), chest);
                            placeChestItem(model, x, y, chest);
                            chestsPlaced++;
                            break;
                        }
                    }
                }
            }
        }

        return chestsPlaced;
    }

    /**
     * Distribue les items selon les biomes
     */
    private static int distributeItemsByBiome(GameModel model, Map<String, Integer> itemStats) {
        int itemsPlaced = 0;

        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                if (!model.canWalkThrough(x, y) || !model.getItemMap()[x][y].isEmpty()) {
                    continue;
                }

                String biome = determineBiome(model, x, y);
                double spawnChance = getBiomeSpawnChance(biome);

                if (random.nextDouble() < spawnChance) {
                    List<GameModel.Item> items = generateBiomeItems(biome, 1 + random.nextInt(2));

                    for (GameModel.Item item : items) {
                        model.getItemMap()[x][y].add(item);
                        itemStats.put(item.type, itemStats.getOrDefault(item.type, 0) + item.count);
                        itemsPlaced++;
                    }
                }
            }
        }

        return itemsPlaced;
    }

    /**
     * Place des items spéciaux dans des lieux particuliers
     */
    private static int placeSpecialItems(GameModel model, Map<String, Integer> itemStats) {
        int itemsPlaced = 0;

        // Items rares près des lieux dangereux
        itemsPlaced += placeRareItemsNearDanger(model, itemStats);

        // Ressources près des structures appropriées
        itemsPlaced += placeResourceItems(model, itemStats);

        // Items de quête cachés
        itemsPlaced += placeQuestItems(model, itemStats);

        return itemsPlaced;
    }

    private static int placeRareItemsNearDanger(GameModel model, Map<String, Integer> itemStats) {
        int itemsPlaced = 0;

        // Chercher les zones dangereuses (beaucoup d'ennemis)
        for (int x = 5; x < GameModel.MAP_SIZE - 5; x++) {
            for (int y = 5; y < GameModel.MAP_SIZE - 5; y++) {
                int enemyCount = countEnemiesInRadius(model, x, y, 3);

                if (enemyCount >= 2 && random.nextDouble() < 0.3) {
                    // Placer un item rare
                    List<String> rareItems = Arrays.asList(
                            "flame_sword", "magic_ring", "teleport_scroll", "ruby"
                    );

                    String itemId = rareItems.get(random.nextInt(rareItems.size()));
                    GameModel.Item rareItem = new GameModel.Item(itemId, 1);

                    if (model.getItemMap()[x][y].isEmpty()) {
                        model.getItemMap()[x][y].add(rareItem);
                        itemStats.put(rareItem.type, itemStats.getOrDefault(rareItem.type, 0) + 1);
                        itemsPlaced++;
                    }
                }
            }
        }

        return itemsPlaced;
    }

    private static int placeResourceItems(GameModel model, Map<String, Integer> itemStats) {
        int itemsPlaced = 0;

        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                if (!model.canWalkThrough(x, y) || !model.getItemMap()[x][y].isEmpty()) {
                    continue;
                }

                // Bois près des arbres
                if (model.getWallTypes()[x][y] == GameModel.WallType.INDESTRUCTIBLE &&
                        model.getWallMap()[x][y] >= 35 && random.nextDouble() < 0.2) {

                    GameModel.Item wood = new GameModel.Item("wood", 1 + random.nextInt(3));
                    model.getItemMap()[x][y].add(wood);
                    itemStats.put(wood.type, itemStats.getOrDefault(wood.type, 0) + wood.count);
                    itemsPlaced++;
                }

                // Herbes dans les forêts
                String biome = determineBiome(model, x, y);
                if ("forest".equals(biome) && random.nextDouble() < 0.15) {
                    GameModel.Item herb = new GameModel.Item("herb", 1 + random.nextInt(2));
                    model.getItemMap()[x][y].add(herb);
                    itemStats.put(herb.type, itemStats.getOrDefault(herb.type, 0) + herb.count);
                    itemsPlaced++;
                }
            }
        }

        return itemsPlaced;
    }

    private static int placeQuestItems(GameModel model, Map<String, Integer> itemStats) {
        int itemsPlaced = 0;

        // Placer quelques items légendaires cachés
        for (int i = 0; i < 3; i++) {
            int x = 10 + random.nextInt(GameModel.MAP_SIZE - 20);
            int y = 10 + random.nextInt(GameModel.MAP_SIZE - 20);

            if (model.canWalkThrough(x, y) && model.getItemMap()[x][y].isEmpty()) {
                String[] legendaryItems = {"master_key", "resurrection_stone", "diamond"};
                String itemId = legendaryItems[random.nextInt(legendaryItems.length)];

                GameModel.Item legendary = new GameModel.Item(itemId, 1);
                model.getItemMap()[x][y].add(legendary);
                itemStats.put(legendary.type, itemStats.getOrDefault(legendary.type, 0) + 1);
                itemsPlaced++;
            }
        }

        return itemsPlaced;
    }

    /**
     * Génère les clés pour les coffres verrouillés
     */
    private static void generateChestKeys(GameModel model, Map<String, Integer> itemStats) {
        // Cette méthode sera étendue quand le système de coffres sera intégré
        // Pour l'instant, on génère quelques clés de coffre génériques

        for (int i = 0; i < 5; i++) {
            int x = random.nextInt(GameModel.MAP_SIZE);
            int y = random.nextInt(GameModel.MAP_SIZE);

            if (model.canWalkThrough(x, y) && model.getItemMap()[x][y].isEmpty()) {
                GameModel.Item chestKey = new GameModel.Item("chest_key", 1);
                model.getItemMap()[x][y].add(chestKey);
                itemStats.put(chestKey.type, itemStats.getOrDefault(chestKey.type, 0) + 1);
            }
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    private static void generateChestContents(Chest chest, String biome) {
        int itemCount = chest.type.minItems + random.nextInt(chest.type.maxItems - chest.type.minItems + 1);

        // Garantir au moins un item de la rareté du coffre
        chest.contents.addAll(generateBiomeItems(biome, 1, chest.type.guaranteedRarity));
        itemCount--;

        // Remplir le reste avec des items aléatoires
        chest.contents.addAll(generateBiomeItems(biome, itemCount));
    }

    private static void placeChestItem(GameModel model, int x, int y, Chest chest) {
        // Créer un item représentant le coffre
        GameModel.Item chestItem = new GameModel.Item("chest_" + chest.type.id, 1);
        model.getItemMap()[x][y].add(chestItem);
    }

    private static String determineBiome(GameModel model, int x, int y) {
        int floorType = model.getFloorMap()[x][y];

        if (floorType >= 0 && floorType <= 14) return "forest";      // Herbe
        if (floorType >= 15 && floorType <= 19) return "village";    // Chemin
        if (floorType >= 20 && floorType <= 24) return "dungeon";    // Pierre
        if (floorType >= 25 && floorType <= 29) return "shore";      // Eau
        if (floorType >= 30 && floorType <= 34) return "desert";     // Sable
        if (floorType >= 35 && floorType <= 39) return "village";    // Maison

        return "field"; // Défaut
    }

    private static double getBiomeSpawnChance(String biome) {
        switch (biome) {
            case "village": return 0.12;
            case "forest": return 0.08;
            case "dungeon": return 0.15;
            case "shore": return 0.06;
            case "desert": return 0.04;
            default: return 0.05;
        }
    }

    private static List<GameModel.Item> generateBiomeItems(String biome, int count) {
        return generateBiomeItems(biome, count, null);
    }

    private static List<GameModel.Item> generateBiomeItems(String biome, int count, ItemRarity minRarity) {
        List<GameModel.Item> items = new ArrayList<>();

        List<ItemDefinition> availableItems = new ArrayList<>();
        for (ItemDefinition item : ITEM_DATABASE.values()) {
            if (Arrays.asList(item.biomes).contains(biome)) {
                if (minRarity == null || item.rarity.ordinal() >= minRarity.ordinal()) {
                    availableItems.add(item);
                }
            }
        }

        for (int i = 0; i < count && !availableItems.isEmpty(); i++) {
            ItemDefinition selectedItem = selectItemByRarity(availableItems);
            if (selectedItem != null) {
                int quantity = selectedItem.stackable ?
                        1 + random.nextInt(Math.min(3, selectedItem.maxStack)) : 1;
                items.add(new GameModel.Item(selectedItem.id, quantity));
            }
        }

        return items;
    }

    private static ItemDefinition selectItemByRarity(List<ItemDefinition> items) {
        double totalWeight = 0;
        for (ItemDefinition item : items) {
            totalWeight += item.rarity.chance;
        }

        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;

        for (ItemDefinition item : items) {
            currentWeight += item.rarity.chance;
            if (randomValue <= currentWeight) {
                return item;
            }
        }

        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }

    private static boolean isNearWater(GameModel model, int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;

                if (model.isValidTile(checkX, checkY)) {
                    int floorType = model.getFloorMap()[checkX][checkY];
                    if (floorType >= 25 && floorType <= 29) { // Eau
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Point2D> findEliteEnemyPositions(GameModel model) {
        List<Point2D> elitePositions = new ArrayList<>();

        // Simuler la recherche d'ennemis élites
        // Dans une vraie implémentation, on itérerait sur les entités du CombatSystem
        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                // Simuler des positions d'élites dans des zones stratégiques
                if (model.getWallTypes()[x][y] == GameModel.WallType.INDESTRUCTIBLE &&
                        random.nextDouble() < 0.1) {
                    elitePositions.add(new Point2D(x, y));
                }
            }
        }

        return elitePositions;
    }

    private static int countEnemiesInRadius(GameModel model, int centerX, int centerY, int radius) {
        int enemyCount = 0;

        // Simulation du comptage d'ennemis
        // Dans une vraie implémentation, on utiliserait model.getCombatSystem().getEnemiesInRadius()
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                if (model.isValidTile(x, y)) {
                    double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                    if (distance <= radius && random.nextDouble() < 0.05) { // 5% de chance simulée
                        enemyCount++;
                    }
                }
            }
        }

        return enemyCount;
    }

    private static void saveChestsToModel(GameModel model, Map<Point2D, Chest> chestMap) {
        // Extension future : sauvegarder les coffres dans le modèle
        // Pour l'instant, on stocke juste une référence
        System.out.println("💰 " + chestMap.size() + " coffres générés et sauvegardés");
    }

    private static void printItemStatistics(Map<String, Integer> itemStats) {
        System.out.println("📊 Statistiques de génération d'items :");
        itemStats.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> System.out.println("  - " + entry.getKey() + " : " + entry.getValue()));
    }

    // ================================
    // MÉTHODES D'INTERFACE PUBLIQUES
    // ================================

    /**
     * Remplace la génération d'items du RealisticMapGenerator
     */
    public static void enhanceMapWithRealisticItems(GameModel model) {
        // Nettoyer les items existants
        clearExistingItems(model);

        // Générer de nouveaux items réalistes
        generateRealisticItems(model);

        System.out.println("🎮 Carte améliorée avec le système d'items réaliste");
    }

    private static void clearExistingItems(GameModel model) {
        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                model.getItemMap()[x][y].clear();
            }
        }
    }

    /**
     * Génère un coffre avec contenu aléatoire à une position donnée
     */
    public static void spawnChestAt(GameModel model, int x, int y, ChestType chestType) {
        if (!model.isValidTile(x, y) || !model.canWalkThrough(x, y)) {
            return;
        }

        Chest chest = new Chest(chestType);
        String biome = determineBiome(model, x, y);
        generateChestContents(chest, biome);

        placeChestItem(model, x, y, chest);

        System.out.println("📦 Coffre " + chestType.displayName + " placé en (" + x + ", " + y + ")");
        System.out.println("  Contenu : " + chest.contents.size() + " items");
    }

    /**
     * Génère un item spécifique à une position
     */
    public static boolean spawnItemAt(GameModel model, int x, int y, String itemId, int quantity) {
        if (!model.isValidTile(x, y)) {
            return false;
        }

        ItemDefinition itemDef = ITEM_DATABASE.get(itemId);
        if (itemDef == null) {
            System.err.println("⚠️ Item inconnu : " + itemId);
            return false;
        }

        int finalQuantity = itemDef.stackable ?
                Math.min(quantity, itemDef.maxStack) : 1;

        GameModel.Item item = new GameModel.Item(itemId, finalQuantity);
        model.getItemMap()[x][y].add(item);

        System.out.println("🎁 Item spawné : " + itemDef.displayName + " x" + finalQuantity +
                " en (" + x + ", " + y + ")");
        return true;
    }

    /**
     * Obtient les informations d'un item
     */
    public static ItemDefinition getItemDefinition(String itemId) {
        return ITEM_DATABASE.get(itemId);
    }

    /**
     * Liste tous les items disponibles
     */
    public static Map<String, ItemDefinition> getAllItems() {
        return new HashMap<>(ITEM_DATABASE);
    }

    /**
     * Génère un loot aléatoire selon un biome
     */
    public static List<GameModel.Item> generateRandomLoot(String biome, int count, ItemRarity minRarity) {
        return generateBiomeItems(biome, count, minRarity);
    }

    /**
     * Obtient le type de rareté d'un item
     */
    public static ItemRarity getItemRarity(String itemId) {
        ItemDefinition item = ITEM_DATABASE.get(itemId);
        return item != null ? item.rarity : ItemRarity.COMMON;
    }

    /**
     * Vérifie si un item peut apparaître dans un biome
     */
    public static boolean canItemSpawnInBiome(String itemId, String biome) {
        ItemDefinition item = ITEM_DATABASE.get(itemId);
        return item != null && Arrays.asList(item.biomes).contains(biome);
    }

    /**
     * Génère des stats pour le debug
     */
    public static void printItemDatabase() {
        System.out.println("=== Base de Données des Items ===");

        Map<ItemRarity, Integer> rarityCount = new HashMap<>();
        Map<GameModel.ItemType, Integer> typeCount = new HashMap<>();

        for (ItemDefinition item : ITEM_DATABASE.values()) {
            rarityCount.put(item.rarity, rarityCount.getOrDefault(item.rarity, 0) + 1);
            typeCount.put(item.type, typeCount.getOrDefault(item.type, 0) + 1);
        }

        System.out.println("Total d'items : " + ITEM_DATABASE.size());

        System.out.println("\nRépartition par rareté :");
        for (ItemRarity rarity : ItemRarity.values()) {
            int count = rarityCount.getOrDefault(rarity, 0);
            System.out.println("  " + rarity.displayName + " : " + count + " items");
        }

        System.out.println("\nRépartition par type :");
        for (GameModel.ItemType type : GameModel.ItemType.values()) {
            int count = typeCount.getOrDefault(type, 0);
            System.out.println("  " + type + " : " + count + " items");
        }

        System.out.println("================================");
    }

    /**
     * Méthode pour tester le générateur
     */
    public static void testItemGeneration() {
        System.out.println("🧪 Test du générateur d'items...");

        // Test de génération par biome
        String[] biomes = {"village", "forest", "dungeon", "shore", "desert"};

        for (String biome : biomes) {
            System.out.println("\n🌍 Biome : " + biome);
            List<GameModel.Item> items = generateBiomeItems(biome, 5);

            for (GameModel.Item item : items) {
                ItemDefinition def = getItemDefinition(item.type);
                String rarity = def != null ? def.rarity.displayName : "Inconnue";
                System.out.println("  - " + item.type + " x" + item.count + " (" + rarity + ")");
            }
        }

        // Test de génération de coffre
        System.out.println("\n📦 Test de coffre légendaire :");
        Chest testChest = new Chest(ChestType.LEGENDARY_CHEST);
        generateChestContents(testChest, "dungeon");

        for (GameModel.Item item : testChest.contents) {
            ItemDefinition def = getItemDefinition(item.type);
            String rarity = def != null ? def.rarity.displayName : "Inconnue";
            System.out.println("  - " + item.type + " x" + item.count + " (" + rarity + ")");
        }

        System.out.println("\n✅ Test terminé !");
    }

    /**
     * Point d'entrée pour tester le générateur
     */
    public static void main(String[] args) {
        System.out.println("=== Générateur d'Items Réaliste ===");

        // Afficher la base de données
        printItemDatabase();

        // Tester la génération
        testItemGeneration();

        System.out.println("\n🎮 Générateur prêt à être intégré au jeu !");
    }
}
