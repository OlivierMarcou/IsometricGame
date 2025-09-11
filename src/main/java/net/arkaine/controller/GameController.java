package net.arkaine.controller;

import net.arkaine.combat.CombatSystem;
import net.arkaine.inventory.InventorySystem;
import net.arkaine.model.GameModel;
import net.arkaine.view.GameView;
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
    private Timeline respawnTimeline;

    // Variables pour le système de combat
    private double lastUpdateTime = 0;

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
                // Vérifier s'il y a un ennemi à cette position pour l'attaquer
                if (isEnemyAtPosition(clickedTile)) {
                    // NOUVEAU : Arrêter le mouvement avant d'attaquer
                    model.stopMovement();
                    model.playerAttack(clickedTile);
                } else {
                    // CORRECTION : Toujours permettre le déplacement
                    // (plus de vérification de model.isMoving())
                    handleMovementRequest(clickedTile);
                }
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
        // CORRECTION : Supprimer la vérification qui bloquait les nouveaux mouvements
        // if (model.isMoving()) return; // ← Cette ligne est supprimée

        // Toujours permettre un nouveau mouvement, même si un mouvement est en cours
        List<Point2D> path = findPath(model.getPlayerPosition(), target);
        model.startMovement(path, target, target);
    }

    private void handleDoorInteraction(int x, int y) {
        model.handleDoorInteraction(x, y);
    }

    // Nouvelle méthode pour vérifier la présence d'ennemis
    private boolean isEnemyAtPosition(Point2D position) {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();

        for (CombatSystem.Entity entity : entities) {
            if (!entity.isPlayer && entity.stats.isAlive()) {
                double distance = entity.position.distance(position);
                if (distance <= 0.7) { // Tolérance pour le clic
                    return true;
                }
            }
        }

        return false;
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
        // Initialiser le temps
        lastUpdateTime = System.currentTimeMillis() / 1000.0;

        // Boucle de jeu principale
        Timeline gameLoop = new Timeline(new KeyFrame(Duration.millis(16), e -> update()));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();

        // Timer pour respawn d'ennemis (toutes les 45 secondes selon la config)
        respawnTimeline = new Timeline(new KeyFrame(Duration.seconds(45), e -> {
            model.respawnEnemiesIfNeeded();
        }));
        respawnTimeline.setCycleCount(Timeline.INDEFINITE);
        respawnTimeline.play();
    }

    private void update() {
        double currentTime = System.currentTimeMillis() / 1000.0;
        double deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Mettre à jour le mouvement
        if (model.isMoving()) {
            model.updateMovement();
            view.updateCameraToFollowPlayer(model);
        }

        // Mettre à jour le système de combat
        if (!model.updateCombat(deltaTime)) {
            // Le joueur est mort - gérer la fin de partie
            handleGameOver();
            return;
        }

        // Rendre la vue
        view.render(model);
    }

    // Nouvelle méthode pour gérer la fin de partie
    private void handleGameOver() {
        System.out.println("💀 GAME OVER - Le joueur est mort!");

        // Arrêter toutes les animations
        if (moveTimeline != null) {
            moveTimeline.stop();
        }
        if (respawnTimeline != null) {
            respawnTimeline.stop();
        }

        // Afficher un message de fin
        model.setMessageAbovePlayer("GAME OVER");

        // Ici vous pourriez ajouter une interface de restart ou retour au menu
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
                showInventoryDialog();
                break;
            case "E":
                showEquipmentDialog();
                break;
            case "P":
                model.getInventory().printInventoryStats();
                break;
            case "H":
                // Debug - afficher la santé du joueur
                CombatSystem.Entity player = model.getCombatSystem().getEntities().stream()
                        .filter(e -> e.isPlayer)
                        .findFirst()
                        .orElse(null);
                if (player != null) {
                    System.out.println("💗 Santé du joueur: " + player.stats.health + "/" + player.stats.maxHealth);
                }
                break;
            case "R":
                // Debug - forcer le respawn d'ennemis
                model.respawnEnemiesIfNeeded();
                System.out.println("🔄 Respawn forcé");
                break;
            case "K":
                // Debug - tuer tous les ennemis
                debugKillAllEnemies();
                break;
            case "B":
                // Debug - spawner un boss
                model.debugSpawnBoss();
                System.out.println("👹 Boss de debug spawné!");
                break;
            case "V":
                // Debug - déclencher une invasion
                model.debugTriggerInvasion();
                System.out.println("🚨 Invasion de debug déclenchée!");
                break;
            case "X":
                // Debug - nettoyer tous les ennemis
                model.debugClearAllEnemies();
                break;
            case "S":
                // Debug - afficher les statistiques de combat
                model.debugPrintCombatStats();
                printCombatStats();
                break;
            case "F1":
                // Afficher l'aide des commandes
                printDebugHelp();
                break;
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

    // Méthode de debug pour tuer tous les ennemis
    private void debugKillAllEnemies() {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();
        int killedCount = 0;

        for (CombatSystem.Entity entity : entities) {
            if (!entity.isPlayer && entity.stats.isAlive()) {
                entity.stats.health = 0;
                killedCount++;
            }
        }

        System.out.println("💀 Debug: " + killedCount + " ennemis éliminés");
        model.setMessageAbovePlayer("Enemies defeated!");
    }

    // Méthode pour afficher les statistiques de combat
    private void printCombatStats() {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();

        long aliveEnemies = entities.stream()
                .filter(e -> !e.isPlayer && e.stats.isAlive())
                .count();

        long totalEnemies = entities.stream()
                .filter(e -> !e.isPlayer)
                .count();

        System.out.println("⚔️ Statistiques de combat:");
        System.out.println("  - Ennemis vivants: " + aliveEnemies);
        System.out.println("  - Total ennemis: " + totalEnemies);
        System.out.println("  - Projectiles actifs: " + model.getCombatSystem().getProjectiles().size());
    }

    // Méthode pour afficher l'aide des commandes de debug
    private void printDebugHelp() {
        System.out.println("\n=== Commandes de Debug - Combat ===");
        System.out.println("C - Recentrer la caméra");
        System.out.println("I - Afficher l'inventaire");
        System.out.println("E - Afficher l'équipement");
        System.out.println("P - Stats de l'inventaire");
        System.out.println("H - Santé du joueur");
        System.out.println("R - Forcer le respawn d'ennemis");
        System.out.println("K - Tuer tous les ennemis");
        System.out.println("B - Spawner un boss");
        System.out.println("V - Déclencher une invasion");
        System.out.println("X - Nettoyer tous les ennemis");
        System.out.println("S - Statistiques de combat");
        System.out.println("F1 - Afficher cette aide");
        System.out.println("=================================\n");
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