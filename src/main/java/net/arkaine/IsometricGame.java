package net.arkaine;

import net.arkaine.model.GameModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IsometricGame   {

    // Constantes
    private static final int MAP_SIZE = 50;

    // Cartes du jeu
    private int[][] floorMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] wallMap = new int[MAP_SIZE][MAP_SIZE];
    private int[][] ceilingMap = new int[MAP_SIZE][MAP_SIZE];
    private WallType[][] wallTypes = new WallType[MAP_SIZE][MAP_SIZE];
    private GameModel.WallProperties[][] wallProperties = new GameModel.WallProperties[MAP_SIZE][MAP_SIZE];
    private List<Item>[][] itemMap = new List[MAP_SIZE][MAP_SIZE];
    private Set<String> playerKeys = new HashSet<>(); // Clés possédées par le joueur

    // Animation
    // Types de murs
    enum WallType {
        NONE, TRAVERSABLE, TRANSPARENT, DOOR, DESTRUCTIBLE, INDESTRUCTIBLE
    }

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
    // Classe pour les items
    static class Item {
        String type;
        int count;

        Item(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }

}