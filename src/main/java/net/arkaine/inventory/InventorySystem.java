package net.arkaine.inventory;

import net.arkaine.model.GameModel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;

/**
 * Système d'inventaire avec équipement et interface graphique
 */
public class InventorySystem {

    public enum ItemType {
        CONSUMABLE,    // Potions, nourriture
        WEAPON,        // Armes
        ARMOR,         // Armures corps
        HELMET,        // Casques
        PANTS,         // Pantalons
        BELT,          // Ceintures
        RING,          // Bagues
        CLOTHING,      // Vêtements
        TREASURE,      // Objets de valeur
        KEY,           // Clés
        MISC           // Divers
    }

    public enum EquipmentSlot {
        RING, CLOTHING, ARMOR, HELMET, RIGHT_HAND, LEFT_HAND, BELT, PANTS
    }

    public static class InventoryItem {
        public String name;
        public ItemType type;
        public int count;
        public String description;
        public Map<String, Object> properties;

        public InventoryItem(String name, ItemType type, int count, String description) {
            this.name = name;
            this.type = type;
            this.count = count;
            this.description = description;
            this.properties = new HashMap<>();
        }


        public InventoryItem(GameModel.Item gameItem) {
            this.name = gameItem.type;
            this.count = gameItem.count;
            this.properties = new HashMap<>();

            // Détection améliorée du type d'objet
            String itemType = gameItem.type.toLowerCase();

            if (itemType.contains("key")) {
                this.type = ItemType.KEY;
                this.description = "Une clé qui peut ouvrir une porte";
            } else if (itemType.contains("potion") || itemType.contains("herb") || itemType.contains("berry")) {
                this.type = ItemType.CONSUMABLE;
                this.description = "Peut être consommé pour des effets";
            } else if (itemType.contains("armor") || itemType.contains("chest")) {
                this.type = ItemType.ARMOR;
                this.description = "Protection pour le corps";
            } else if (itemType.contains("helmet") || itemType.contains("hat")) {
                this.type = ItemType.HELMET;
                this.description = "Protection pour la tête";
            } else if (itemType.contains("sword") || itemType.contains("weapon") || itemType.contains("bow")) {
                this.type = ItemType.WEAPON;
                this.description = "Arme de combat";
            } else if (itemType.contains("ring")) {
                this.type = ItemType.RING;
                this.description = "Bijou magique";
            } else if (itemType.contains("gem") || itemType.contains("treasure") || itemType.contains("coin")) {
                this.type = ItemType.TREASURE;
                this.description = "Objet de valeur (" + gameItem.count + ")";
            } else if (itemType.contains("trophy") || itemType.contains("scroll") || itemType.contains("legendary")) {
                this.type = ItemType.MISC;
                this.description = "Objet spécial rare";
            } else {
                this.type = ItemType.MISC;
                this.description = "Objet divers";
            }
        }


        public boolean canEquipIn(EquipmentSlot slot) {
            switch (slot) {
                case RING: return type == ItemType.RING;
                case CLOTHING: return type == ItemType.CLOTHING;
                case ARMOR: return type == ItemType.ARMOR;
                case HELMET: return type == ItemType.HELMET;
                case RIGHT_HAND:
                case LEFT_HAND: return type == ItemType.WEAPON;
                case BELT: return type == ItemType.BELT;
                case PANTS: return type == ItemType.PANTS;
                default: return false;
            }
        }

        @Override
        public String toString() {
            return name + (count > 1 ? " (" + count + ")" : "");
        }
    }

    // Inventaire
    private List<InventoryItem> backpack = new ArrayList<>();
    private Map<EquipmentSlot, InventoryItem> equipment = new HashMap<>();
    private static final int MAX_BACKPACK_SIZE = 5;

    // Listeners pour notifier les changements
    private List<InventoryListener> listeners = new ArrayList<>();

    public interface InventoryListener {
        void onInventoryChanged();
        void onItemEquipped(EquipmentSlot slot, InventoryItem item);
        void onItemUnequipped(EquipmentSlot slot, InventoryItem item);
    }

    public InventorySystem() {
        // Initialiser les slots d'équipement
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.put(slot, null);
        }
    }

    public void addListener(InventoryListener listener) {
        listeners.add(listener);
    }

    public void removeListener(InventoryListener listener) {
        listeners.remove(listener);
    }

    private void notifyInventoryChanged() {
        for (InventoryListener listener : listeners) {
            listener.onInventoryChanged();
        }
    }

    private void notifyItemEquipped(EquipmentSlot slot, InventoryItem item) {
        for (InventoryListener listener : listeners) {
            listener.onItemEquipped(slot, item);
        }
    }

    private void notifyItemUnequipped(EquipmentSlot slot, InventoryItem item) {
        for (InventoryListener listener : listeners) {
            listener.onItemUnequipped(slot, item);
        }
    }

    // Méthodes d'accès
    public List<InventoryItem> getBackpack() { return new ArrayList<>(backpack); }
    public Map<EquipmentSlot, InventoryItem> getEquipment() { return new HashMap<>(equipment); }
    public int getBackpackSize() { return backpack.size(); }
    public boolean isBackpackFull() { return backpack.size() >= MAX_BACKPACK_SIZE; }

    // Gestion des objets
    public boolean addItem(InventoryItem item) {
        if (isBackpackFull()) {
            System.out.println("❌ Sac plein: impossible d'ajouter " + item.name);
            return false;
        }

        // Essayer de fusionner avec un objet existant
        for (InventoryItem existing : backpack) {
            if (existing.name.equals(item.name) && existing.type == item.type) {
                existing.count += item.count;
                System.out.println("🔄 Objets fusionnés: " + item.name + " (maintenant x" + existing.count + ")");
                notifyInventoryChanged();
                return true;
            }
        }

        // Ajouter comme nouvel objet
        backpack.add(item);
        System.out.println("✅ Ajouté à l'inventaire: " + item.name + " x" + item.count);
        notifyInventoryChanged();
        return true;
    }

    public boolean removeItem(InventoryItem item, int count) {
        InventoryItem found = null;
        for (InventoryItem existing : backpack) {
            if (existing.name.equals(item.name) && existing.type == item.type) {
                found = existing;
                break;
            }
        }

        if (found == null || found.count < count) {
            return false;
        }

        found.count -= count;
        if (found.count <= 0) {
            backpack.remove(found);
        }

        notifyInventoryChanged();
        return true;
    }

    public boolean equipItem(InventoryItem item, EquipmentSlot slot) {
        if (!item.canEquipIn(slot)) {
            return false;
        }

        // Déséquiper l'objet actuel s'il y en a un
        InventoryItem currentItem = equipment.get(slot);
        if (currentItem != null) {
            unequipItem(slot);
        }

        // Retirer de l'inventaire et équiper
        if (removeItem(item, 1)) {
            equipment.put(slot, item);
            notifyItemEquipped(slot, item);
            return true;
        }

        return false;
    }

    public boolean unequipItem(EquipmentSlot slot) {
        InventoryItem item = equipment.get(slot);
        if (item == null) {
            return false;
        }

        if (isBackpackFull()) {
            return false; // Pas de place dans l'inventaire
        }

        equipment.put(slot, null);
        addItem(item);
        notifyItemUnequipped(slot, item);
        return true;
    }

    // Collecte d'objets depuis la carte
    public List<InventoryItem> collectItems(GameModel model, int x, int y) {
        List<GameModel.Item> groundItems = model.getItemMap()[x][y];
        List<InventoryItem> collected = new ArrayList<>();

        for (GameModel.Item gameItem : groundItems) {
            InventoryItem invItem = new InventoryItem(gameItem);

            if (addItem(invItem)) {
                collected.add(invItem);
            }
        }

        // Retirer les objets collectés de la carte
        if (!collected.isEmpty()) {
            groundItems.clear();
            System.out.println("Objets collectés: " + collected.size());
        }

        return collected;
    }

    // Interface graphique de gestion d'inventaire

    public void showInventoryManagementDialog(List<InventoryItem> itemsToCollect, Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Gestion d'Inventaire - Sac plein");

        BorderPane root = new BorderPane();

        // Créer les listes avec handlers améliorés
        ListView<InventoryItem> inventoryList = createInventoryListView();
        ListView<InventoryItem> itemsToCollectList = createItemsToCollectListView(itemsToCollect);

        // CORRECTION : Synchronisation en temps réel des listes
        inventoryList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                InventoryItem selected = inventoryList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Jeter l'objet et l'ajouter à la liste de collecte
                    if (removeItem(selected, 1)) {
                        itemsToCollectList.getItems().add(selected);
                        inventoryList.getItems().remove(selected);
                        System.out.println("Objet jeté: " + selected.name);
                    }
                }
            }
        });

        itemsToCollectList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                InventoryItem selected = itemsToCollectList.getSelectionModel().getSelectedItem();
                if (selected != null && !isBackpackFull()) {
                    // Ramasser l'objet
                    if (addItem(selected)) {
                        inventoryList.getItems().add(selected);
                        itemsToCollectList.getItems().remove(selected);
                        System.out.println("Objet ramassé: " + selected.name);
                    }
                }
            }
        });

        // Panneaux gauche et droite
        VBox leftPanel = createPanelWithTitle("Inventaire actuel (" + backpack.size() + "/" + MAX_BACKPACK_SIZE + ")", inventoryList);
        VBox rightPanel = createPanelWithTitle("Objets à ramasser", itemsToCollectList);

        // Layout principal
        HBox mainLayout = new HBox(10);
        mainLayout.getChildren().addAll(leftPanel, rightPanel);

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(e -> dialog.close());

        Button dropAllButton = new Button("Tout jeter au sol");
        dropAllButton.setOnAction(e -> {
            // Implémenter la logique pour remettre tous les objets au sol
            dialog.close();
        });

        buttonBox.getChildren().addAll(closeButton, dropAllButton);

        root.setCenter(mainLayout);
        root.setBottom(buttonBox);
        root.setPrefSize(600, 400);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.show();
    }

    private ListView<InventoryItem> createInventoryListView() {
        ListView<InventoryItem> listView = new ListView<>();
        listView.getItems().addAll(backpack);

        // Double-clic pour jeter au sol
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                InventoryItem selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    removeItem(selected, 1);
                    listView.getItems().remove(selected);
                    // Ici, on devrait remettre l'objet au sol dans le modèle
                    System.out.println("Objet jeté: " + selected.name);
                }
            }
        });

        return listView;
    }

    private ListView<InventoryItem> createItemsToCollectListView(List<InventoryItem> items) {
        ListView<InventoryItem> listView = new ListView<>();
        listView.getItems().addAll(items);

        // Double-clic pour ramasser
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                InventoryItem selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && !isBackpackFull()) {
                    addItem(selected);
                    listView.getItems().remove(selected);
                    System.out.println("Objet ramassé: " + selected.name);
                }
            }
        });

        return listView;
    }

    private VBox createPanelWithTitle(String title, ListView<InventoryItem> listView) {
        VBox panel = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        panel.getChildren().addAll(titleLabel, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);

        return panel;
    }

    // Interface d'équipement
    public void showEquipmentDialog(Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Équipement");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Label slotLabel = new Label(slot.name().replace("_", " ") + ":");
            InventoryItem equipped = equipment.get(slot);
            Label itemLabel = new Label(equipped != null ? equipped.toString() : "Vide");

            Button unequipButton = new Button("Retirer");
            unequipButton.setDisable(equipped == null);
            unequipButton.setOnAction(e -> {
                if (unequipItem(slot)) {
                    itemLabel.setText("Vide");
                    unequipButton.setDisable(true);
                }
            });

            grid.add(slotLabel, 0, row);
            grid.add(itemLabel, 1, row);
            grid.add(unequipButton, 2, row);
            row++;
        }

        Button closeButton = new Button("Fermer");
        closeButton.setOnAction(e -> dialog.close());
        grid.add(closeButton, 1, row);

        Scene scene = new Scene(grid, 400, 300);
        dialog.setScene(scene);
        dialog.show();
    }

    // Méthodes utilitaires
    public void printInventoryStats() {
        System.out.println("=== État de l'Inventaire ===");
        System.out.println("📦 Sac à dos: " + backpack.size() + "/" + MAX_BACKPACK_SIZE);

        if (!backpack.isEmpty()) {
            System.out.println("🎒 Contenu du sac:");
            Map<ItemType, Integer> typeStats = new HashMap<>();

            for (InventoryItem item : backpack) {
                System.out.println("  - " + item.name + " x" + item.count + " (" + item.type + ")");
                typeStats.put(item.type, typeStats.getOrDefault(item.type, 0) + item.count);
            }

            System.out.println("📊 Par catégorie:");
            typeStats.forEach((type, count) ->
                    System.out.println("  " + type + ": " + count + " objets"));
        }

        System.out.println("⚔️ Équipement:");
        boolean hasEquipment = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            InventoryItem item = equipment.get(slot);
            if (item != null) {
                System.out.println("  " + slot.name().replace("_", " ") + ": " + item.name);
                hasEquipment = true;
            }
        }

        if (!hasEquipment) {
            System.out.println("  (Aucun équipement)");
        }

        System.out.println("===============================");
    }
}