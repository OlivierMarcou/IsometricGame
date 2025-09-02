package com.isometric.game.controller;

import com.isometric.game.inventory.InventorySystem;
import com.isometric.game.model.GameModel;
import com.isometric.game.view.GameView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

/**
 * Contrôleur principal - Gère les interactions entre le modèle et la vue
 */
public class GameController implements GameModel.GameModelListener {

    private GameModel model;
    private GameView view;
    private Stage parentStage; // Pour les dialogues

    // Timers pour les animations
    private Timeline moveTimeline;
    private Timeline messageTimeline;
    private Timeline exclamationTimeline;

    public GameController(GameModel model, GameView view, Stage parentStage) {
        this.model = model;
        this.view = view;
        this.parentStage = parentStage;

        // S'enregistrer comme observateur du modèle
        model.addListener(this);

        // Configurer les événements de la vue
        setupViewEvents();

        // Initialiser la vue
        initializeView();
    }

    private void setupViewEvents() {
        view.getCanvas().setOnMouseMoved(this::onMouseMoved);
        view.getCanvas().setOnMouseClicked(this::onMouseClicked);
        view.getCanvas().setOnMousePressed(this::onMousePressed);
        view.getCanvas().setFocusTraversable(true);
    }

    private void initializeView() {
        // Charger la carte
        model.loadMapFromJson();

        // Centrer la caméra sur le joueur
        view.centerCameraOnPlayer(model);
    }

    // Gestion des événements souris
    private void onMouseMoved(MouseEvent e) {
        // Calculer l'angle du joueur vers la souris
        double centerX = GameView.CANVAS_WIDTH / 2;
        double centerY = GameView.CANVAS_HEIGHT / 2;
        double angle = Math.atan2(e.getY() - centerY, e.getX() - centerX);
        model.setPlayerAngle(angle);

        // Calculer la position de la tuile sous le curseur
        Point2D hoveredTile = view.screenToTile(e.getX(), e.getY());
        if (hoveredTile != null && model.isValidTile((int)hoveredTile.getX(), (int)hoveredTile.getY())) {
            view.setMouseHoverPosition(hoveredTile);
        } else {
            view.setMouseHoverPosition(null);
        }
    }

    private void onMouseClicked(MouseEvent e) {
        Point2D clickedTile = view.screenToTile(e.getX(), e.getY());
        if (clickedTile != null && model.isValidTile((int)clickedTile.getX(), (int)clickedTile.getY())) {

            if (e.getButton() == MouseButton.PRIMARY) {
                // Clic gauche - déplacement
                handleMovementRequest(clickedTile);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // Clic droit - ramasser des objets
                handleItemCollection((int)clickedTile.getX(), (int)clickedTile.getY());
            }
        }
    }

    private void onMousePressed(MouseEvent e) {
        Point2D clickedTile = view.screenToTile(e.getX(), e.getY());
        if (clickedTile != null && model.isValidTile((int)clickedTile.getX(), (int)clickedTile.getY())) {
            handleDoorInteraction((int)clickedTile.getX(), (int)clickedTile.getY());
        }
    }

    private void handleMovementRequest(Point2D target) {
        if (model.isMoving()) return;

        List<Point2D> path = findPath(model.getPlayerPosition(), target);
        model.startMovement(path, target, target);
    }

    private void handleDoorInteraction(int x, int y) {
        model.handleDoorInteraction(x, y);
    }

    // Algorithme A* pour le pathfinding avec diagonales
    private List<Point2D> findPath(Point2D start, Point2D end) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node((int)start.getX(), (int)start.getY(), null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(startNode.getKey(), startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            String currentKey = current.getKey();

            if (closedSet.contains(currentKey)) continue;
            closedSet.add(currentKey);

            if (current.x == (int)end.getX() && current.y == (int)end.getY()) {
                return reconstructPath(current);
            }

            // Directions incluant les diagonales
            int[][] directions = {
                    {-1, 0}, {1, 0}, {0, -1}, {0, 1},  // Orthogonales
                    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonales
            };

            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String newKey = newX + "," + newY;

                if (!model.canWalkThrough(newX, newY) || closedSet.contains(newKey)) continue;

                // Vérifier les diagonales - éviter de passer "à travers" les murs
                boolean isDiagonal = (dir[0] != 0 && dir[1] != 0);
                if (isDiagonal) {
                    // Pour une diagonale, vérifier que les cases adjacentes sont libres
                    boolean canMoveDiagonally =
                            model.canWalkThrough(current.x + dir[0], current.y) &&  // Côté horizontal
                                    model.canWalkThrough(current.x, current.y + dir[1]);     // Côté vertical

                    if (!canMoveDiagonally) continue;
                }

                // Coût selon la direction (diagonale = sqrt(2) ≈ 1.414)
                double moveCost = isDiagonal ? 1.414 : 1.0;
                double newG = current.g + moveCost;
                double newH = heuristic(new Point2D(newX, newY), end);
                Node neighbor = new Node(newX, newY, current, newG, newH);

                Node existing = allNodes.get(newKey);
                if (existing == null || newG < existing.g) {
                    allNodes.put(newKey, neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>();
    }

    private double heuristic(Point2D a, Point2D b) {
        // Heuristique diagonale (distance de Chebyshev)
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        return Math.max(dx, dy);
    }

    private List<Point2D> reconstructPath(Node endNode) {
        List<Point2D> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(0, new Point2D(current.x, current.y));
            current = current.parent;
        }

        return path;
    }

    // Méthodes publiques pour le contrôle externe
    public void startGameLoop() {
        Timeline gameLoop = new Timeline(new KeyFrame(Duration.millis(16), e -> update()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    private void update() {
        // Mettre à jour le mouvement
        if (model.isMoving()) {
            model.updateMovement();
            view.updateCameraToFollowPlayer(model);
        }

        // Rendre la vue
        view.render(model);
    }

    private void handleItemCollection(int x, int y) {
        List<GameModel.Item> groundItems = model.getGroundItemsAt(x, y);
        if (groundItems.isEmpty()) {
            return;
        }

        boolean success = model.tryCollectItems(x, y);
        if (!success) {
            // Inventaire plein - afficher l'interface de gestion
            showInventoryManagementDialog(x, y);
        }
    }

    private void showInventoryManagementDialog(int x, int y) {
        // Récupérer les objets restants au sol
        List<InventorySystem.InventoryItem> itemsToCollect = new ArrayList<>();
        for (GameModel.Item gameItem : model.getGroundItemsAt(x, y)) {
            itemsToCollect.add(new InventorySystem.InventoryItem(gameItem));
        }

        // Afficher l'interface de gestion
        model.getInventory().showInventoryManagementDialog(itemsToCollect, parentStage);
    }

    public void handleKeyPressed(String keyCode) {
        switch (keyCode) {
            case "C":
                view.centerCameraOnPlayer(model);
                System.out.println("Caméra recentrée sur le personnage");
                break;
            case "I":
                // Afficher l'inventaire
                showInventoryDialog();
                break;
            case "E":
                // Afficher l'équipement
                showEquipmentDialog();
                break;
            case "P":
                // Debug - afficher les stats de l'inventaire
                model.getInventory().printInventoryStats();
                break;
            // Ajouter d'autres commandes clavier si nécessaire
        }
    }

    private void showInventoryDialog() {
        InventorySystem inventory = model.getInventory();
        System.out.println("Ouverture de l'inventaire:");
        inventory.printInventoryStats();
        // Ici on pourrait créer une interface graphique plus complexe pour l'inventaire
    }

    private void showEquipmentDialog() {
        InventorySystem inventory = model.getInventory();
        inventory.showEquipmentDialog(parentStage);
    }

    // Implémentation des callbacks du modèle
    @Override
    public void onMapLoaded() {
        System.out.println("Carte chargée - mise à jour de la vue");
        view.centerCameraOnPlayer(model);
    }

    @Override
    public void onPlayerMoved(Point2D newPosition) {
        // Pas d'action spéciale nécessaire - la vue se met à jour automatiquement
    }

    @Override
    public void onMovementStarted(List<Point2D> path) {
        System.out.println("Mouvement démarré vers " + path.get(path.size() - 1));

        // Démarrer l'animation de mouvement
        if (moveTimeline != null) {
            moveTimeline.stop();
        }

        moveTimeline = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            // La logique de mouvement est gérée dans update()
        }));
        moveTimeline.setCycleCount(Timeline.INDEFINITE);
        moveTimeline.play();
    }

    @Override
    public void onMovementFinished() {
        System.out.println("Mouvement terminé");
        if (moveTimeline != null) {
            moveTimeline.stop();
        }
    }

    @Override
    public void onDoorStateChanged(int x, int y, boolean isOpen) {
        System.out.println("Porte à (" + x + ", " + y + ") " + (isOpen ? "ouverte" : "fermée"));
    }

    @Override
    public void onMessageChanged(String message) {
        // Démarrer le timer pour effacer le message
        if (messageTimeline != null) {
            messageTimeline.stop();
        }

        if (message != null) {
            messageTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> model.setMessageAbovePlayer(null)));
            messageTimeline.play();
        }
    }

    @Override
    public void onExclamationStateChanged(boolean show) {
        if (show) {
            // Démarrer le timer pour masquer l'exclamation
            if (exclamationTimeline != null) {
                exclamationTimeline.stop();
            }

            exclamationTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> model.setShowExclamation(false)));
            exclamationTimeline.play();
        }
    }

    // Classe Node pour l'algorithme A*
    private static class Node implements Comparable<Node> {
        int x, y;
        Node parent;
        double g, h;

        Node(int x, int y, Node parent, double g, double h) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        double getF() { return g + h; }
        String getKey() { return x + "," + y; }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getF(), other.getF());
        }
    }
}