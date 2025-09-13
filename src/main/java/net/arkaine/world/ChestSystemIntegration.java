package net.arkaine.world;

import net.arkaine.model.GameModel;
import net.arkaine.world.RealisticItemGenerator.Chest;
import net.arkaine.world.RealisticItemGenerator.ChestType;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.*;

/**
 * Extension du GameModel pour supporter les coffres
 */
public class ChestSystemIntegration {

    // Extension des données du GameModel
    private Map<Point2D, Chest> chestMap = new HashMap<>();
    private GameModel gameModel;

    public ChestSystemIntegration(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    /**
     * Modifie le GameModel pour inclure le support des coffres
     */
    public void integrateChestSystem() {
        System.out.println("🔧 Intégration du système de coffres...");

        // Remplacer la génération d'items standard
        RealisticItemGenerator.enhanceMapWithRealisticItems(gameModel);

        // Initialiser les coffres
        initializeChestsFromItems();

        System.out.println("✅ Système de coffres intégré : " + chestMap.size() + " coffres");
    }

    /**
     * Initialise les coffres à partir des items "chest_*" placés sur la carte
     */
    private void initializeChestsFromItems() {
        for (int x = 0; x < GameModel.MAP_SIZE; x++) {
            for (int y = 0; y < GameModel.MAP_SIZE; y++) {
                List<GameModel.Item> items = gameModel.getItemMap()[x][y];

                for (int i = items.size() - 1; i >= 0; i--) {
                    GameModel.Item item = items.get(i);

                    if (item.type.startsWith("chest_")) {
                        // Extraire le type de coffre
                        String chestTypeId = item.type.substring(6); // Enlever "chest_"
                        ChestType chestType = findChestTypeById(chestTypeId);

                        if (chestType != null) {
                            // Créer le coffre et générer son contenu
                            Chest chest = new Chest(chestType);
                            String biome = determineBiome(x, y);
                            generateChestContents(chest, biome);

                            // Stocker le coffre
                            chestMap.put(new Point2D(x, y), chest);

                            // Remplacer l'item par un indicateur visuel
                            items.set(i, new GameModel.Item("chest_closed", 1));
                        }
                    }
                }
            }
        }
    }

    /**
     * Gère l'interaction avec un coffre
     */
    public boolean handleChestInteraction(int x, int y, Stage parentStage) {
        Point2D position = new Point2D(x, y);
        Chest chest = chestMap.get(position);

        if (chest == null) {
            return false;
        }

        // Vérifier la distance
        Point2D playerPos = gameModel.getPlayerPosition();
        double distance = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());
        if (distance > 1.5) {
            gameModel.setMessageAbovePlayer("Too far");
            return false;
        }

        // Vérifier si le coffre est verrouillé
        if (chest.isLocked && chest.keyId != null) {
            if (!gameModel.getPlayerKeys().contains(chest.keyId)) {
                gameModel.setMessageAbovePlayer("Chest locked");
                return false;
            } else {
                chest.isLocked = false;
                gameModel.setMessageAbovePlayer("Chest unlocked!");
            }
        }

        // Ouvrir le coffre
        if (!chest.isOpen) {
            chest.isOpen = true;
            updateChestVisual(x, y, true);
            gameModel.setMessageAbovePlayer("Chest opened!");
        }

        // Afficher l'interface du coffre
        showChestInterface(chest, parentStage, x, y);

        return true;
    }

    /**
     * Met à jour l'apparence visuelle du coffre
     */
    private void updateChestVisual(int x, int y, boolean isOpen) {
        List<GameModel.Item> items = gameModel.getItemMap()[x][y];

        for (int i = 0; i < items.size(); i++) {
            GameModel.Item item = items.get(i);
            if (item.type.equals("chest_closed") || item.type.equals("chest_open")) {
                items.set(i, new GameModel.Item(isOpen ? "chest_open" : "chest_closed", 1));
                break;
            }
        }
    }

    /**
     * Affiche l'interface d'interaction avec le coffre
     */
    private void showChestInterface(Chest chest, Stage parentStage, int x, int y) {
        Stage chestDialog = new Stage();
        chestDialog.initModality(Modality.APPLICATION_MODAL);
        chestDialog.initOwner(parentStage);
        chestDialog.setTitle(chest.type.displayName);

        BorderPane root = new BorderPane();

        // Titre avec informations du coffre
        Label titleLabel = new Label(chest.type.displayName);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label statusLabel = new Label(chest.isLocked ? "🔒 Verrouillé" : "🔓 Ouvert");
        statusLabel.setStyle("-fx-font-size: 12px;");

        VBox titleBox = new VBox(5, titleLabel, statusLabel);
        root.setTop(titleBox);

        // Liste des contenus du coffre
        ListView<GameModel.Item> chestContents = new ListView<>();
        chestContents.getItems().addAll(chest.contents);
        chestContents.setPrefHeight(200);

        // Personnaliser l'affichage des items
        chestContents.setCellFactory(listView -> new ListCell<GameModel.Item>() {
            @Override
            protected void updateItem(GameModel.Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    RealisticItemGenerator.ItemDefinition def =
                            RealisticItemGenerator.getItemDefinition(item.type);

                    String displayText = item.type;
                    String rarity = "Commun";

                    if (def != null) {
                        displayText = def.displayName;
                        rarity = def.rarity.displayName;
                    }

                    if (item.count > 1) {
                        displayText += " x" + item.count;
                    }

                    setText(displayText + " (" + rarity + ")");

                    // Couleur selon la rareté
                    String style = getRarityStyle(def != null ? def.rarity : RealisticItemGenerator.ItemRarity.COMMON);
                    setStyle(style);
                }
            }
        });

        // Double-clic pour ramasser
        chestContents.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                GameModel.Item selectedItem = chestContents.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    takeItemFromChest(chest, selectedItem, chestContents);
                }
            }
        });

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(
                new Label("Contenu du coffre (double-clic pour prendre):"),
                chestContents
        );
        root.setCenter(centerBox);

        // Boutons d'action
        HBox buttonBox = new HBox(10);

        Button takeAllButton = new Button("Tout prendre");
        takeAllButton.setOnAction(e -> {
            takeAllFromChest(chest, chestContents);
        });

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(e -> {
            chestDialog.close();
            // Vérifier si le coffre est vide pour le supprimer
            if (chest.contents.isEmpty()) {
                removeEmptyChest(x, y);
            }
        });

        buttonBox.getChildren().addAll(takeAllButton, closeButton);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 400, 350);
        chestDialog.setScene(scene);
        chestDialog.show();
    }

    /**
     * Prend un item spécifique du coffre
     */
    private void takeItemFromChest(Chest chest, GameModel.Item item, ListView<GameModel.Item> listView) {
        // Tenter d'ajouter à l'inventaire
        RealisticItemGenerator.ItemDefinition itemDef = RealisticItemGenerator.getItemDefinition(item.type);
        if (itemDef != null) {
            net.arkaine.inventory.InventorySystem.InventoryItem invItem =
                    new net.arkaine.inventory.InventorySystem.InventoryItem(item);

            if (gameModel.getInventory().addItem(invItem)) {
                chest.contents.remove(item);
                listView.getItems().remove(item);
                gameModel.setMessageAbovePlayer("Item taken!");

                System.out.println("📦 Item pris du coffre: " + item.type + " x" + item.count);
            } else {
                gameModel.setMessageAbovePlayer("Inventory full!");
            }
        }
    }

    /**
     * Prend tous les items du coffre
     */
    private void takeAllFromChest(Chest chest, ListView<GameModel.Item> listView) {
        List<GameModel.Item> itemsToRemove = new ArrayList<>();
        int takenCount = 0;

        for (GameModel.Item item : chest.contents) {
            RealisticItemGenerator.ItemDefinition itemDef = RealisticItemGenerator.getItemDefinition(item.type);
            if (itemDef != null) {
                net.arkaine.inventory.InventorySystem.InventoryItem invItem =
                        new net.arkaine.inventory.InventorySystem.InventoryItem(item);

                if (gameModel.getInventory().addItem(invItem)) {
                    itemsToRemove.add(item);
                    takenCount++;
                } else {
                    break; // Inventaire plein
                }
            }
        }

        // Retirer les items pris
        for (GameModel.Item item : itemsToRemove) {
            chest.contents.remove(item);
            listView.getItems().remove(item);
        }

        if (takenCount > 0) {
            gameModel.setMessageAbovePlayer("Taken " + takenCount + " items!");
            System.out.println("📦 " + takenCount + " items pris du coffre");
        } else {
            gameModel.setMessageAbovePlayer("Inventory full!");
        }
    }

    /**
     * Supprime un coffre vide de la carte
     */
    private void removeEmptyChest(int x, int y) {
        Point2D position = new Point2D(x, y);
        chestMap.remove(position);

        // Retirer l'indicateur visuel
        List<GameModel.Item> items = gameModel.getItemMap()[x][y];
        items.removeIf(item -> item.type.equals("chest_open") || item.type.equals("chest_closed"));

        System.out.println("📦 Coffre vide supprimé en (" + x + ", " + y + ")");
    }

    /**
     * Vérifie s'il y a un coffre à la position donnée
     */
    public boolean hasChestAt(int x, int y) {
        return chestMap.containsKey(new Point2D(x, y));
    }

    /**
     * Obtient le coffre à la position donnée
     */
    public Chest getChestAt(int x, int y) {
        return chestMap.get(new Point2D(x, y));
    }

    /**
     * Spawne un nouveau coffre à une position
     */
    public void spawnChestAt(int x, int y, ChestType chestType) {
        if (!gameModel.isValidTile(x, y) || !gameModel.canWalkThrough(x, y)) {
            return;
        }

        Chest chest = new Chest(chestType);
        String biome = determineBiome(x, y);
        generateChestContents(chest, biome);

        chestMap.put(new Point2D(x, y), chest);

        // Ajouter l'indicateur visuel
        gameModel.getItemMap()[x][y].add(new GameModel.Item("chest_closed", 1));

        System.out.println("📦 Nouveau coffre spawné: " + chestType.displayName + " en (" + x + ", " + y + ")");
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    private ChestType findChestTypeById(String id) {
        for (ChestType type : ChestType.values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    private String determineBiome(int x, int y) {
        int floorType = gameModel.getFloorMap()[x][y];

        if (floorType >= 0 && floorType <= 14) return "forest";
        if (floorType >= 15 && floorType <= 19) return "village";
        if (floorType >= 20 && floorType <= 24) return "dungeon";
        if (floorType >= 25 && floorType <= 29) return "shore";
        if (floorType >= 30 && floorType <= 34) return "desert";
        if (floorType >= 35 && floorType <= 39) return "village";

        return "field";
    }

    private void generateChestContents(Chest chest, String biome) {
        Random random = new Random();
        int itemCount = chest.type.minItems + random.nextInt(chest.type.maxItems - chest.type.minItems + 1);

        // Garantir au moins un item de la rareté du coffre
        chest.contents.addAll(RealisticItemGenerator.generateRandomLoot(biome, 1, chest.type.guaranteedRarity));
        itemCount--;

        // Remplir le reste avec des items aléatoires
        chest.contents.addAll(RealisticItemGenerator.generateRandomLoot(biome, itemCount, null));
    }

    private String getRarityStyle(RealisticItemGenerator.ItemRarity rarity) {
        switch (rarity) {
            case COMMON:
                return "-fx-text-fill: white;";
            case UNCOMMON:
                return "-fx-text-fill: lime; -fx-font-weight: bold;";
            case RARE:
                return "-fx-text-fill: blue; -fx-font-weight: bold;";
            case EPIC:
                return "-fx-text-fill: purple; -fx-font-weight: bold;";
            case LEGENDARY:
                return "-fx-text-fill: orange; -fx-font-weight: bold;";
            default:
                return "-fx-text-fill: gray;";
        }
    }

    /**
     * Affiche les statistiques des coffres
     */
    public void printChestStatistics() {
        System.out.println("=== Statistiques des Coffres ===");
        System.out.println("Nombre total de coffres : " + chestMap.size());

        Map<ChestType, Integer> chestTypeCounts = new HashMap<>();
        int lockedCount = 0;
        int openCount = 0;
        int totalItems = 0;

        for (Chest chest : chestMap.values()) {
            chestTypeCounts.put(chest.type, chestTypeCounts.getOrDefault(chest.type, 0) + 1);
            if (chest.isLocked) lockedCount++;
            if (chest.isOpen) openCount++;
            totalItems += chest.contents.size();
        }

        System.out.println("Coffres verrouillés : " + lockedCount);
        System.out.println("Coffres ouverts : " + openCount);
        System.out.println("Items totaux dans les coffres : " + totalItems);

        System.out.println("\nRépartition par type :");
        for (ChestType type : ChestType.values()) {
            int count = chestTypeCounts.getOrDefault(type, 0);
            System.out.println("  " + type.displayName + " : " + count);
        }

        System.out.println("================================");
    }

    /**
     * Méthodes de debug pour les coffres
     */
    public void debugSpawnChest(int x, int y) {
        ChestType[] types = ChestType.values();
        ChestType randomType = types[new Random().nextInt(types.length)];
        spawnChestAt(x, y, randomType);
    }

    public void debugUnlockAllChests() {
        for (Chest chest : chestMap.values()) {
            chest.isLocked = false;
        }
        System.out.println("🔓 Tous les coffres ont été déverrouillés");
    }

    public void debugFillRandomChest() {
        if (chestMap.isEmpty()) return;

        List<Point2D> positions = new ArrayList<>(chestMap.keySet());
        Point2D randomPos = positions.get(new Random().nextInt(positions.size()));
        Chest chest = chestMap.get(randomPos);

        // Ajouter des items aléatoires
        chest.contents.addAll(RealisticItemGenerator.generateRandomLoot("dungeon", 5, RealisticItemGenerator.ItemRarity.RARE));

        System.out.println("💎 Coffre rempli d'items rares en " + randomPos);
    }

    // Getters
    public Map<Point2D, Chest> getChestMap() {
        return new HashMap<>(chestMap);
    }

    public int getChestCount() {
        return chestMap.size();
    }

    public int getLockedChestCount() {
        return (int) chestMap.values().stream().filter(c -> c.isLocked).count();
    }
}