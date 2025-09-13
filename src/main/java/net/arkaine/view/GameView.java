package net.arkaine.view;

import net.arkaine.combat.CombatSystem;
import net.arkaine.model.GameModel;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import net.arkaine.world.RealisticItemGenerator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vue du jeu - Gère tout le rendu et l'affichage
 */
public class GameView {

    // Constantes de rendu
    public static final int TILE_WIDTH = 64;
    public static final int TILE_HEIGHT = 32;
    public static final int WALL_HEIGHT = 128;
    public static final int CANVAS_WIDTH = 1200;
    public static final int CANVAS_HEIGHT = 800;

    // Éléments d'affichage
    private Canvas canvas;
    private GraphicsContext gc;

    // Position de la caméra
    private double cameraX;
    private double cameraY;

    // Images
    private Map<String, Image> floorImages = new HashMap<>();
    private Map<String, Image> wallImages = new HashMap<>();
    private Map<String, Image> ceilingImages = new HashMap<>();

    // Position de la souris
    private Point2D mouseHoverPos = null;

    public GameView() {
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);

        initializeImages();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setMouseHoverPosition(Point2D position) {
        this.mouseHoverPos = position;
    }

    public void setCameraPosition(double x, double y) {
        this.cameraX = x;
        this.cameraY = y;
    }

    public double getCameraX() { return cameraX; }
    public double getCameraY() { return cameraY; }

    private void initializeImages() {
        System.out.println("Chargement des images PNG...");

        // Charger les images de sol
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/sol/floor_" + i + ".png");
            if (image != null) {
                floorImages.put("floor_" + i, image);
            }
        }

        // Charger les images de murs
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/murs/wall_" + i + ".png");
            if (image != null) {
                wallImages.put("wall_" + i, image);
            }

            // Charger aussi les versions ouvertes des portes
            Image openImage = loadImage("/murs/wall_" + i + "_o.png");
            if (openImage != null) {
                wallImages.put("wall_" + i + "_o", openImage);
            }
        }

        // Charger les images de plafonds
        for (int i = 0; i < 30; i++) {
            Image image = loadImage("/plafonds/ceiling_" + i + ".png");
            if (image != null) {
                ceilingImages.put("ceiling_" + i, image);
            }
        }

        System.out.println("Images chargées: " + floorImages.size() + " sols, " +
                wallImages.size() + " murs, " + ceilingImages.size() + " plafonds");
    }

    private Image loadImage(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream != null) {
                return new Image(stream);
            }
        } catch (Exception e) {
            // Image par défaut si non trouvée
        }
        return createDefaultImage();
    }

    private Image createDefaultImage() {
        Canvas tempCanvas = new Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();

        tempGc.clearRect(0, 0, TILE_WIDTH, TILE_HEIGHT);

        double[] xPoints = {TILE_WIDTH/2.0, TILE_WIDTH, TILE_WIDTH/2.0, 0};
        double[] yPoints = {0, TILE_HEIGHT/2.0, TILE_HEIGHT, TILE_HEIGHT/2.0};

        tempGc.setFill(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8));
        tempGc.fillPolygon(xPoints, yPoints, 4);

        tempGc.setStroke(Color.BLACK.deriveColor(0, 1, 1, 0.8));
        tempGc.setLineWidth(1);
        tempGc.strokePolygon(xPoints, yPoints, 4);

        return tempCanvas.snapshot(null, null);
    }

    public void render(GameModel model) {
        renderWithEnhancedItems(model);
    }

    private void renderTile(GameModel model, int x, int y) {
        Point2D screenPos = tileToScreen(x, y);
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Culling
        if (screenX < -TILE_WIDTH || screenX > CANVAS_WIDTH + TILE_WIDTH ||
                screenY < -WALL_HEIGHT || screenY > CANVAS_HEIGHT + TILE_HEIGHT) {
            return;
        }

        // Rendu du sol
        int floorIndex = model.getFloorMap()[x][y];
        if (floorIndex >= 0) {
            Image floorImg = floorImages.get("floor_" + floorIndex);
            if (floorImg != null) {
                gc.drawImage(floorImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2);
            }
        }

        // Rendu des items
        List<GameModel.Item> items = model.getItemMap()[x][y];
        if (!items.isEmpty()) {
            gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.8));
            gc.fillOval(screenX - 6, screenY - 6, 12, 12);

            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(2);
            gc.strokeLine(screenX - 5, screenY, screenX + 5, screenY);
            gc.strokeLine(screenX, screenY - 5, screenX, screenY + 5);

            int totalItems = items.stream().mapToInt(item -> item.count).sum();
            if (totalItems > 1) {
                gc.setFill(Color.WHITE);
                gc.fillText(String.valueOf(totalItems), screenX + 6, screenY - 6);
            }
        }

        // Calcul de transparence
        double alpha = calculateAlpha(model, x, y);

        // Rendu des murs
        int wallIndex = model.getWallMap()[x][y];
        if (wallIndex >= 0) {
            String wallImageKey = "wall_" + wallIndex;

            // Cas spécial pour les portes ouvertes
            if (model.getWallTypes()[x][y] == GameModel.WallType.DOOR) {
                GameModel.WallProperties props = model.getWallProperties()[x][y];
                if (props != null && props.isOpen) {
                    wallImageKey = "wall_" + wallIndex + "_o";
                }
            }

            Image wallImg = wallImages.get(wallImageKey);
            if (wallImg == null) {
                wallImg = wallImages.get("wall_" + wallIndex);
            }

            if (wallImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(wallImg, screenX - TILE_WIDTH/2, screenY - WALL_HEIGHT + TILE_HEIGHT/2);
                gc.setGlobalAlpha(1.0);

                // Barre de santé pour murs destructibles
                if (model.getWallTypes()[x][y] == GameModel.WallType.DESTRUCTIBLE) {
                    GameModel.WallProperties props = model.getWallProperties()[x][y];
                    if (props != null && props.health < 255) {
                        renderHealthBar(screenX, screenY - WALL_HEIGHT + TILE_HEIGHT/2, props.health, 255);
                    }
                }
            }
        }

        // Rendu des plafonds
        int ceilingIndex = model.getCeilingMap()[x][y];
        if (ceilingIndex >= 0) {
            Image ceilingImg = ceilingImages.get("ceiling_" + ceilingIndex);
            if (ceilingImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(ceilingImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2 - WALL_HEIGHT);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private double calculateAlpha(GameModel model, int x, int y) {
        Point2D playerPos = model.getPlayerPosition();
        double distanceToPlayer = Math.abs(x - playerPos.getX()) + Math.abs(y - playerPos.getY());

        if (isInFrontOfPlayer(model, x, y)) {
            if (distanceToPlayer <= 1) {
                return 0.2; // 80% transparent
            } else if (distanceToPlayer <= 2) {
                return 0.5; // 50% transparent
            }
        }
        return 1.0;
    }

    private boolean isInFrontOfPlayer(GameModel model, int x, int y) {
        Point2D playerPos = model.getPlayerPosition();
        double dx = x - playerPos.getX();
        double dy = y - playerPos.getY();
        double angle = Math.atan2(dy, dx);
        double angleDiff = Math.abs(angle - model.getPlayerAngle());

        while (angleDiff > Math.PI) {
            angleDiff -= 2 * Math.PI;
        }
        while (angleDiff < -Math.PI) {
            angleDiff += 2 * Math.PI;
        }

        return Math.abs(angleDiff) < Math.PI / 2;
    }

    // ================================
    // RENDU DES ENNEMIS
    // ================================

    private void renderEnemies(GameModel model) {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();

        for (CombatSystem.Entity entity : entities) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            if (isEnemyVisible(entity)) {
                renderEnemy(entity);
            }
        }
    }

    private void renderEnemy(CombatSystem.Entity enemy) {
        Point2D screenPos = tileToScreen(enemy.position.getX(), enemy.position.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Couleur selon la classe d'ennemi
        Color enemyColor = getEnemyColor(enemy.entityClass);

        // Corps de l'ennemi
        double size = getEnemySize(enemy.entityClass);
        gc.setFill(enemyColor);
        gc.fillOval(screenX - size/2, screenY - size/2, size, size);

        // Contour plus sombre
        gc.setStroke(enemyColor.darker());
        gc.setLineWidth(2);
        gc.strokeOval(screenX - size/2, screenY - size/2, size, size);

        // Indicateur de classe (forme au centre)
        renderEnemyClassIndicator(screenX, screenY, enemy.entityClass);

        // Barre de santé
        if (enemy.stats.health < enemy.stats.maxHealth) {
            renderEnemyHealthBar(screenX, screenY - size/2 - 12, enemy.stats);
        }

        // Indicateur d'aggro (si l'ennemi vise le joueur)
        if (enemy instanceof CombatSystem.Enemy) {
            CombatSystem.Enemy e = (CombatSystem.Enemy) enemy;
            if (e.target != null) {
                renderAggroIndicator(screenX, screenY - size/2 - 22);
            }
        }
    }

    private Color getEnemyColor(CombatSystem.EnemyClass enemyClass) {
        switch (enemyClass) {
            case WARRIOR: return Color.BROWN;
            case MAGE: return Color.PURPLE;
            case ARCHER: return Color.DARKGREEN;
            case ELITE_WARRIOR: return Color.DARKRED;
            case ELITE_MAGE: return Color.DARKVIOLET;
            case ELITE_ARCHER: return Color.DARKSLATEGRAY;
            case BOSS: return Color.BLACK;
            default: return Color.GRAY;
        }
    }

    private double getEnemySize(CombatSystem.EnemyClass enemyClass) {
        switch (enemyClass) {
            case BOSS: return 20;
            case ELITE_WARRIOR:
            case ELITE_MAGE:
            case ELITE_ARCHER: return 16;
            default: return 12;
        }
    }

    private void renderEnemyClassIndicator(double x, double y, CombatSystem.EnemyClass enemyClass) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        switch (enemyClass) {
            case WARRIOR:
            case ELITE_WARRIOR:
                // Épée (ligne verticale)
                gc.strokeLine(x, y - 4, x, y + 4);
                gc.strokeLine(x - 2, y - 3, x + 2, y - 3); // Garde
                break;

            case MAGE:
            case ELITE_MAGE:
                // Étoile magique
                gc.fillOval(x - 2, y - 2, 4, 4);
                // Rayons de magie
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2;
                    double rayX = x + Math.cos(angle) * 3;
                    double rayY = y + Math.sin(angle) * 3;
                    gc.strokeLine(x, y, rayX, rayY);
                }
                break;

            case ARCHER:
            case ELITE_ARCHER:
                // Arc
                gc.strokeArc(x - 3, y - 3, 6, 6, 45, 90, ArcType.OPEN);
                // Flèche
                gc.strokeLine(x, y - 1, x + 2, y);
                break;

            case BOSS:
                // Couronne
                gc.setFill(Color.GOLD);
                gc.fillPolygon(new double[]{x-4, x-2, x, x+2, x+4, x+2, x, x-2},
                        new double[]{y+2, y-3, y+1, y-3, y+2, y-1, y+2, y-1}, 8);
                gc.setStroke(Color.DARKGOLDENROD);
                gc.strokePolygon(new double[]{x-4, x-2, x, x+2, x+4, x+2, x, x-2},
                        new double[]{y+2, y-3, y+1, y-3, y+2, y-1, y+2, y-1}, 8);
                break;
        }
    }

    private void renderEnemyHealthBar(double x, double y, CombatSystem.Stats stats) {
        double barWidth = 24;
        double barHeight = 4;
        double healthPercent = stats.getHealthPercent();

        // Fond noir
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
        gc.fillRect(x - barWidth/2, y, barWidth, barHeight);

        // Barre de santé colorée
        Color healthColor;
        if (healthPercent > 0.6) {
            healthColor = Color.LIMEGREEN;
        } else if (healthPercent > 0.3) {
            healthColor = Color.ORANGE;
        } else {
            healthColor = Color.RED;
        }

        gc.setFill(healthColor);
        gc.fillRect(x - barWidth/2, y, barWidth * healthPercent, barHeight);

        // Contour blanc
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.5);
        gc.strokeRect(x - barWidth/2, y, barWidth, barHeight);
    }

    private void renderAggroIndicator(double x, double y) {
        // Point d'exclamation rouge
        gc.setFill(Color.RED);
        gc.fillOval(x - 3, y - 3, 6, 6);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(8));
        gc.fillText("!", x - 2, y + 2);
    }

    // ================================
    // RENDU DES PROJECTILES
    // ================================

    private void renderProjectiles(GameModel model) {
        List<CombatSystem.Projectile> projectiles = model.getCombatSystem().getProjectiles();

        for (CombatSystem.Projectile projectile : projectiles) {
            if (!projectile.active) continue;

            renderProjectile(projectile);
        }
    }

    private void renderProjectile(CombatSystem.Projectile projectile) {
        Point2D screenPos = tileToScreen(projectile.position.getX(), projectile.position.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Culling
        if (screenX < -20 || screenX > CANVAS_WIDTH + 20 ||
                screenY < -20 || screenY > CANVAS_HEIGHT + 20) {
            return;
        }

        // Projectile principal
        gc.setFill(projectile.color);
        gc.fillOval(screenX - 3, screenY - 3, 6, 6);

        // Effet de traînée selon le type
        renderProjectileTrail(screenX, screenY, projectile);

        // Contour
        gc.setStroke(projectile.color.darker());
        gc.setLineWidth(1);
        gc.strokeOval(screenX - 3, screenY - 3, 6, 6);
    }

    private void renderProjectileTrail(double x, double y, CombatSystem.Projectile projectile) {
        double trailLength = 10;
        double trailX = x - projectile.velocity.getX() * trailLength / projectile.speed;
        double trailY = y - projectile.velocity.getY() * trailLength / projectile.speed;

        // Ligne de traînée avec transparence
        gc.setStroke(projectile.color.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(2);
        gc.strokeLine(x, y, trailX, trailY);

        // Effet spécial selon le type de dégât
        switch (projectile.damageType) {
            case FIRE:
                // Particules de feu
                for (int i = 0; i < 3; i++) {
                    double px = x + (Math.random() - 0.5) * 8;
                    double py = y + (Math.random() - 0.5) * 8;
                    gc.setFill(Color.ORANGE.deriveColor(0, 1, 1, 0.7));
                    gc.fillOval(px - 1, py - 1, 2, 2);
                }
                break;

            case ICE:
                // Cristaux de glace
                gc.setStroke(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.8));
                gc.setLineWidth(1);
                gc.strokeLine(x - 2, y - 2, x + 2, y + 2);
                gc.strokeLine(x - 2, y + 2, x + 2, y - 2);
                break;

            case LIGHTNING:
                // Éclairs
                gc.setStroke(Color.YELLOW.brighter());
                gc.setLineWidth(1);
                for (int i = 0; i < 2; i++) {
                    double zigX = x + (Math.random() - 0.5) * 6;
                    double zigY = y + (Math.random() - 0.5) * 6;
                    gc.strokeLine(x, y, zigX, zigY);
                }
                break;

            case POISON:
                // Bulles de poison
                gc.setFill(Color.GREEN.deriveColor(0, 1, 1, 0.6));
                gc.fillOval(x - 1, y - 1, 2, 2);
                gc.fillOval(x + 2, y - 2, 3, 3);
                break;
        }
    }

    // ================================
    // RENDU DU JOUEUR
    // ================================

    private void renderPlayer(GameModel model) {
        Point2D playerPos = model.getCurrentInterpolatedPosition();
        Point2D screenPos = tileToScreen(playerPos.getX(), playerPos.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Couleur selon orientation
        double hue = (model.getPlayerAngle() + Math.PI) / (2 * Math.PI) * 360;
        gc.setFill(Color.hsb(hue, 1.0, 1.0));
        gc.fillOval(screenX - 8, screenY - 8, 16, 16);

        // Direction
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        double dirX = screenX + Math.cos(model.getPlayerAngle()) * 12;
        double dirY = screenY + Math.sin(model.getPlayerAngle()) * 12;
        gc.strokeLine(screenX, screenY, dirX, dirY);

        // Contour du joueur
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(2);
        gc.strokeOval(screenX - 8, screenY - 8, 16, 16);

        // Point d'exclamation
        if (model.shouldShowExclamation()) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(screenX - 4, screenY - 24, 8, 8);
            gc.setFill(Color.RED);
            gc.fillText("!", screenX - 3, screenY - 18);
        }

        // Message au-dessus du personnage
        String message = model.getMessageAbovePlayer();
        if (message != null) {
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeText(message, screenX - message.length() * 3, screenY - 30);
            gc.fillText(message, screenX - message.length() * 3, screenY - 30);
        }
    }

    // ================================
    // INTERFACE DE COMBAT
    // ================================

    private void renderCombatUI(GameModel model) {
        // Statistiques du joueur en haut à gauche
        CombatSystem.Entity player = model.getCombatSystem().getEntities().stream()
                .filter(e -> e.isPlayer)
                .findFirst()
                .orElse(null);

        if (player != null) {
            renderPlayerStats(player.stats);
        }

        // Compteur d'ennemis en haut à droite
        renderEnemyCounter(model);

        // Mini-carte avec ennemis en bas à droite
        renderMiniMap(model);
    }

    private void renderPlayerStats(CombatSystem.Stats stats) {
        double x = 10;
        double y = 10;
        double width = 220;
        double height = 70;

        // Fond semi-transparent
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
        gc.fillRoundRect(x, y, width, height, 8, 8);

        // Contour
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 8, 8);

        // Texte des stats
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText("Santé: " + stats.health + "/" + stats.maxHealth, x + 10, y + 20);
        gc.fillText("Dégâts: " + stats.damage, x + 10, y + 35);
        gc.fillText("Portée: " + String.format("%.1f", stats.range), x + 10, y + 50);

        // Barre de santé
        double barWidth = width - 20;
        double barHeight = 8;
        double barX = x + 10;
        double barY = y + height - 15;

        // Fond de la barre
        gc.setFill(Color.DARKRED);
        gc.fillRect(barX, barY, barWidth, barHeight);

        // Santé actuelle
        double healthPercent = stats.getHealthPercent();
        Color healthColor = healthPercent > 0.6 ? Color.LIMEGREEN :
                healthPercent > 0.3 ? Color.ORANGE : Color.RED;
        gc.setFill(healthColor);
        gc.fillRect(barX, barY, barWidth * healthPercent, barHeight);

        // Contour de la barre
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(barX, barY, barWidth, barHeight);
    }

    private void renderEnemyCounter(GameModel model) {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();

        long aliveEnemies = entities.stream()
                .filter(e -> !e.isPlayer && e.stats.isAlive())
                .count();

        long totalEnemies = entities.stream()
                .filter(e -> !e.isPlayer)
                .count();

        // Position en haut à droite
        double x = CANVAS_WIDTH - 160;
        double y = 10;
        double width = 150;
        double height = 50;

        // Fond
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
        gc.fillRoundRect(x, y, width, height, 8, 8);

        // Contour
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 8, 8);

        // Texte
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText("Ennemis: " + aliveEnemies + "/" + totalEnemies, x + 10, y + 20);
        gc.fillText("Projectiles: " + model.getCombatSystem().getProjectiles().size(), x + 10, y + 35);
    }

    private void renderMiniMap(GameModel model) {
        double mapSize = 120;
        double x = CANVAS_WIDTH - mapSize - 10;
        double y = CANVAS_HEIGHT - mapSize - 10;

        // Fond de la mini-carte
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
        gc.fillRect(x, y, mapSize, mapSize);

        // Contour
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, mapSize, mapSize);

        // Échelle de la carte
        double scale = mapSize / GameModel.MAP_SIZE;

        // Position du joueur
        Point2D playerPos = model.getPlayerPosition();
        double playerX = x + playerPos.getX() * scale;
        double playerY = y + playerPos.getY() * scale;

        gc.setFill(Color.BLUE);
        gc.fillOval(playerX - 2, playerY - 2, 4, 4);

        // Ennemis sur la mini-carte
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();
        for (CombatSystem.Entity entity : entities) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            double enemyX = x + entity.position.getX() * scale;
            double enemyY = y + entity.position.getY() * scale;

            // Couleur selon la classe
            Color enemyColor = getEnemyColor(entity.entityClass);
            gc.setFill(enemyColor);

            // Taille selon la classe
            double dotSize = entity.entityClass == CombatSystem.EnemyClass.BOSS ? 3 :
                    (entity.entityClass.name().startsWith("ELITE") ? 2 : 1);
            gc.fillOval(enemyX - dotSize, enemyY - dotSize, dotSize * 2, dotSize * 2);
        }

        // Titre de la mini-carte
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(10));
        gc.fillText("Mini-carte", x, y - 5);
    }

    // ================================
    // INDICATEURS DE SOURIS
    // ================================

    private void renderMouseIndicators(GameModel model) {
        // Losange bleu sur la position cliquée
        Point2D clickedPos = model.getClickedPosition();
        if (clickedPos != null && model.isValidTile((int)clickedPos.getX(), (int)clickedPos.getY())) {
            renderTileIndicator(clickedPos, Color.BLUE, 0.6);
        }

        // Losange vert/rouge sur la position du curseur
        if (mouseHoverPos != null && model.isValidTile((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY())) {
            boolean canWalk = model.canWalkThrough((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY());

            // Vérifier s'il y a un ennemi à cette position
            boolean hasEnemy = hasEnemyAtPosition(mouseHoverPos, model);

            Color indicatorColor;
            if (hasEnemy) {
                indicatorColor = Color.RED; // Ennemi = rouge (attaque)
            } else if (canWalk) {
                indicatorColor = Color.LIME; // Libre = vert (déplacement)
            } else {
                indicatorColor = Color.DARKRED; // Bloqué = rouge foncé
            }

            renderTileIndicator(mouseHoverPos, indicatorColor, 0.4);
        }
    }

    private boolean hasEnemyAtPosition(Point2D position, GameModel model) {
        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();

        for (CombatSystem.Entity entity : entities) {
            if (!entity.isPlayer && entity.stats.isAlive()) {
                double distance = entity.position.distance(position);
                if (distance <= 0.7) {
                    return true;
                }
            }
        }
        return false;
    }

    private void renderTileIndicator(Point2D tilePos, Color color, double opacity) {
        Point2D screenPos = tileToScreen(tilePos.getX(), tilePos.getY());
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Forme losange isométrique
        double[] xPoints = {
                screenX,                    // Haut
                screenX + TILE_WIDTH/2,     // Droite
                screenX,                    // Bas
                screenX - TILE_WIDTH/2      // Gauche
        };
        double[] yPoints = {
                screenY - TILE_HEIGHT/2,    // Haut
                screenY,                    // Droite
                screenY + TILE_HEIGHT/2,    // Bas
                screenY                     // Gauche
        };

        // Dessiner le losange avec transparence
        gc.setFill(color.deriveColor(0, 1, 1, opacity));
        gc.fillPolygon(xPoints, yPoints, 4);

        // Contour plus visible
        gc.setStroke(color.deriveColor(0, 1, 0.7, 0.8));
        gc.setLineWidth(2);
        gc.strokePolygon(xPoints, yPoints, 4);
    }

    // ================================
    // UTILITAIRES DE RENDU
    // ================================

    private void renderHealthBar(double x, double y, int health, int maxHealth) {
        double barWidth = 30;
        double barHeight = 4;
        double healthPercent = (double) health / maxHealth;

        // Fond de la barre
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.7));
        gc.fillRect(x - barWidth/2, y - 10, barWidth, barHeight);

        // Barre de santé colorée
        Color healthColor;
        if (healthPercent > 0.6) {
            healthColor = Color.GREEN;
        } else if (healthPercent > 0.3) {
            healthColor = Color.ORANGE;
        } else {
            healthColor = Color.RED;
        }

        gc.setFill(healthColor);
        gc.fillRect(x - barWidth/2, y - 10, barWidth * healthPercent, barHeight);

        // Contour
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(x - barWidth/2, y - 10, barWidth, barHeight);
    }

    private boolean isEnemyVisible(CombatSystem.Entity enemy) {
        Point2D screenPos = tileToScreen(enemy.position.getX(), enemy.position.getY());

        return screenPos.getX() >= -50 && screenPos.getX() <= CANVAS_WIDTH + 50 &&
                screenPos.getY() >= -50 && screenPos.getY() <= CANVAS_HEIGHT + 50;
    }

    // ================================
    // TRANSFORMATIONS SPATIALES
    // ================================

    public Point2D screenToTile(double screenX, double screenY) {
        // CORRECTION : Conversion écran vers monde avec caméra précise
        double worldX = screenX - CANVAS_WIDTH / 2 + cameraX;
        double worldY = screenY - CANVAS_HEIGHT / 2 + cameraY;

        // Conversion isométrique vers coordonnées de grille
        double tileX = (worldX / (TILE_WIDTH / 2) + worldY / (TILE_HEIGHT / 2)) / 2;
        double tileY = (worldY / (TILE_HEIGHT / 2) - worldX / (TILE_WIDTH / 2)) / 2;

        return new Point2D(Math.floor(tileX), Math.floor(tileY));
    }

    public Point2D tileToScreen(double tileX, double tileY) {
        // Conversion grille vers coordonnées monde
        double worldX = (tileX - tileY) * (TILE_WIDTH / 2);
        double worldY = (tileX + tileY) * (TILE_HEIGHT / 2);

        // CORRECTION : Conversion monde vers coordonnées écran avec centrage précis
        double screenX = worldX - cameraX + CANVAS_WIDTH / 2;
        double screenY = worldY - cameraY + CANVAS_HEIGHT / 2;

        return new Point2D(screenX, screenY);
    }

    // ================================
    // GESTION DE LA CAMÉRA
    // ================================

    public void centerCameraOnPlayer(GameModel model) {
        Point2D playerPos = model.getPlayerPosition();

        // Calcul direct des coordonnées monde
        double worldX = (playerPos.getX() - playerPos.getY()) * (TILE_WIDTH / 2);
        double worldY = (playerPos.getX() + playerPos.getY()) * (TILE_HEIGHT / 2);

        cameraX = worldX;
        cameraY = worldY;

        System.out.println("Caméra centrée sur le personnage à la position (" +
                (int)playerPos.getX() + ", " + (int)playerPos.getY() + ")");
    }

    public void updateCameraToFollowPlayer(GameModel model) {
        Point2D currentPos = model.getCurrentInterpolatedPosition();

        // Calculer la position monde exacte du joueur
        double targetWorldX = (currentPos.getX() - currentPos.getY()) * (TILE_WIDTH / 2);
        double targetWorldY = (currentPos.getX() + currentPos.getY()) * (TILE_HEIGHT / 2);

        // CORRECTION : Suivi plus réactif et précis
        double lerpFactor = model.isMoving() ? 0.15 : 0.08; // Plus rapide en mouvement

        cameraX += (targetWorldX - cameraX) * lerpFactor;
        cameraY += (targetWorldY - cameraY) * lerpFactor;
    }

    // MÉTHODE ALTERNATIVE : Caméra instantanée (pour debug)
    public void snapCameraToPlayer(GameModel model) {
        Point2D currentPos = model.getCurrentInterpolatedPosition();

        cameraX = (currentPos.getX() - currentPos.getY()) * (TILE_WIDTH / 2);
        cameraY = (currentPos.getX() + currentPos.getY()) * (TILE_HEIGHT / 2);
    }
    // ================================
    // MÉTHODES UTILITAIRES PUBLIQUES
    // ================================

    public CombatSystem.Entity getEnemyAtScreenPosition(double screenX, double screenY, GameModel model) {
        Point2D worldPos = screenToTile(screenX, screenY);

        CombatSystem.Entity closestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        List<CombatSystem.Entity> entities = model.getCombatSystem().getEntities();
        for (CombatSystem.Entity entity : entities) {
            if (entity.isPlayer || !entity.stats.isAlive()) continue;

            double distance = entity.position.distance(worldPos);
            if (distance < minDistance && distance <= 1.0) { // Tolérance de clic
                minDistance = distance;
                closestEnemy = entity;
            }
        }

        return closestEnemy;
    }

    // ================================
    // EFFETS VISUELS AVANCÉS
    // ================================

    private void renderCombatEffects(GameModel model) {
        // Effets de dégâts flottants (à implémenter si nécessaire)
        // Effets d'explosion des projectiles
        // Animations de mort des ennemis
        // etc.
    }

    private void renderDebugInfo(GameModel model) {
        // Informations de debug en mode développement
        if (System.getProperty("debug.combat") != null) {
            gc.setFill(Color.YELLOW);
            gc.setFont(javafx.scene.text.Font.font(10));

            // Afficher FPS, nombre d'entités, etc.
            int entityCount = model.getCombatSystem().getEntities().size();
            int projectileCount = model.getCombatSystem().getProjectiles().size();

            gc.fillText("Entités: " + entityCount, 10, CANVAS_HEIGHT - 30);
            gc.fillText("Projectiles: " + projectileCount, 10, CANVAS_HEIGHT - 15);
        }
    }


    //----------------------------

    /**
     * Rendu amélioré des items avec gestion des coffres et raretés
     */
    private void renderEnhancedItems(GameModel model, int x, int y, double screenX, double screenY) {
        List<GameModel.Item> items = model.getItemMap()[x][y];
        if (items.isEmpty()) return;

        for (int i = 0; i < items.size(); i++) {
            GameModel.Item item = items.get(i);
            double itemX = screenX + (i * 8) - (items.size() * 4); // Décaler les items multiples
            double itemY = screenY;

            if (item.type.startsWith("chest_")) {
                renderChest(item, itemX, itemY);
            } else {
                renderEnhancedItem(item, itemX, itemY);
            }
        }
    }

    /**
     * Rendu spécialisé pour les coffres
     */
    private void renderChest(GameModel.Item chestItem, double x, double y) {
        boolean isOpen = chestItem.type.equals("chest_open");

        // Déterminer le type de coffre depuis l'ID
        String chestTypeId = extractChestTypeFromItem(chestItem.type);
        Color chestColor = getChestColor(chestTypeId);

        // Dimensions du coffre
        double chestWidth = 20;
        double chestHeight = 16;
        double chestDepth = 12;

        // Corps du coffre (perspective isométrique)
        renderChestBody(x, y, chestWidth, chestHeight, chestDepth, chestColor, isOpen);

        // Couvercle
        if (isOpen) {
            renderOpenChestLid(x, y, chestWidth, chestHeight, chestDepth, chestColor);
        } else {
            renderClosedChestLid(x, y, chestWidth, chestHeight, chestColor);
        }

        // Effets spéciaux selon le type
        renderChestEffects(x, y, chestTypeId, isOpen);

        // Serrure si fermé
        if (!isOpen) {
            renderChestLock(x, y, chestWidth, chestHeight);
        }
    }

    private void renderChestBody(double x, double y, double width, double height, double depth, Color color, boolean isOpen) {
        // Face avant
        gc.setFill(color);
        gc.fillRect(x - width/2, y - height/2, width, height);

        // Face droite (perspective)
        gc.setFill(color.darker());
        double[] rightX = {x + width/2, x + width/2 + depth/2, x + width/2 + depth/2, x + width/2};
        double[] rightY = {y - height/2, y - height/2 - depth/4, y + height/2 - depth/4, y + height/2};
        gc.fillPolygon(rightX, rightY, 4);

        // Face du dessus (si ouvert, montrer l'intérieur)
        if (isOpen) {
            gc.setFill(Color.rgb(101, 67, 33)); // Bois sombre intérieur
        } else {
            gc.setFill(color.brighter());
        }
        double[] topX = {x - width/2, x + width/2, x + width/2 + depth/2, x - width/2 + depth/2};
        double[] topY = {y - height/2, y - height/2, y - height/2 - depth/4, y - height/2 - depth/4};
        gc.fillPolygon(topX, topY, 4);

        // Contours
        gc.setStroke(color.darker().darker());
        gc.setLineWidth(1);
        gc.strokeRect(x - width/2, y - height/2, width, height);
        gc.strokePolygon(rightX, rightY, 4);
        gc.strokePolygon(topX, topY, 4);
    }

    private void renderClosedChestLid(double x, double y, double width, double height, Color color) {
        // Couvercle fermé
        double lidHeight = height * 0.4;

        gc.setFill(color.brighter());
        gc.fillRect(x - width/2, y - height/2 - lidHeight, width, lidHeight);

        // Perspective du couvercle
        gc.setFill(color);
        double[] lidRightX = {x + width/2, x + width/2 + 6, x + width/2 + 6, x + width/2};
        double[] lidRightY = {y - height/2 - lidHeight, y - height/2 - lidHeight - 3, y - height/2 - 3, y - height/2};
        gc.fillPolygon(lidRightX, lidRightY, 4);

        // Contours du couvercle
        gc.setStroke(color.darker().darker());
        gc.setLineWidth(1);
        gc.strokeRect(x - width/2, y - height/2 - lidHeight, width, lidHeight);
        gc.strokePolygon(lidRightX, lidRightY, 4);
    }

    private void renderOpenChestLid(double x, double y, double width, double height, double depth, Color color) {
        // Couvercle ouvert (basculé vers l'arrière)
        double lidHeight = height * 0.4;

        // Position du couvercle ouvert
        double lidX = x - width/4;
        double lidY = y - height/2 - depth/2;

        gc.setFill(color.brighter());
        gc.fillRect(lidX - width/4, lidY - lidHeight/2, width/2, lidHeight);

        // Charnières
        gc.setFill(Color.DARKGOLDENROD);
        gc.fillOval(lidX - 2, lidY - 2, 4, 4);
        gc.fillOval(lidX + width/4 - 2, lidY - 2, 4, 4);
    }

    private void renderChestLock(double x, double y, double width, double height) {
        // Serrure dorée
        gc.setFill(Color.GOLD);
        gc.fillOval(x - 3, y - 2, 6, 4);

        // Trou de serrure
        gc.setFill(Color.BLACK);
        gc.fillOval(x - 1, y - 1, 2, 2);

        // Contour de la serrure
        gc.setStroke(Color.DARKGOLDENROD);
        gc.setLineWidth(0.5);
        gc.strokeOval(x - 3, y - 2, 6, 4);
    }

    private void renderChestEffects(double x, double y, String chestType, boolean isOpen) {
        if (!isOpen) return;

        switch (chestType) {
            case "treasure_chest":
                // Particules dorées
                for (int i = 0; i < 5; i++) {
                    double particleX = x + (Math.random() - 0.5) * 30;
                    double particleY = y - 20 + (Math.random() - 0.5) * 10;
                    gc.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.7));
                    gc.fillOval(particleX - 1, particleY - 1, 2, 2);
                }
                break;

            case "legendary_chest":
                // Aura légendaire
                gc.setStroke(Color.PURPLE.deriveColor(0, 1, 1, 0.5));
                gc.setLineWidth(2);
                gc.strokeOval(x - 15, y - 15, 30, 30);

                // Étoiles magiques
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i) / 4;
                    double starX = x + Math.cos(angle) * 12;
                    double starY = y + Math.sin(angle) * 12;
                    renderMagicStar(starX, starY);
                }
                break;

            case "iron_chest":
                // Éclat métallique
                gc.setStroke(Color.LIGHTGRAY.deriveColor(0, 1, 1, 0.8));
                gc.setLineWidth(1);
                gc.strokeLine(x - 8, y - 8, x + 8, y + 8);
                gc.strokeLine(x - 8, y + 8, x + 8, y - 8);
                break;
        }
    }

    private void renderMagicStar(double x, double y) {
        gc.setFill(Color.PURPLE.deriveColor(0, 1, 1, 0.8));

        // Étoile à 4 branches
        double[] starX = {x, x + 3, x, x - 3};
        double[] starY = {y - 3, y, y + 3, y};
        gc.fillPolygon(starX, starY, 4);

        // Croix centrale
        gc.fillRect(x - 1, y - 1, 2, 2);
    }

    /**
     * Rendu amélioré pour les items normaux avec indicateurs de rareté
     */
    private void renderEnhancedItem(GameModel.Item item, double x, double y) {
        // Obtenir les informations de l'item
        RealisticItemGenerator.ItemDefinition itemDef = RealisticItemGenerator.getItemDefinition(item.type);

        if (itemDef != null) {
            renderItemWithRarity(item, itemDef, x, y);
        } else {
            // Rendu par défaut pour les items inconnus
            renderDefaultItem(item, x, y);
        }
    }

    private void renderItemWithRarity(GameModel.Item item, RealisticItemGenerator.ItemDefinition itemDef, double x, double y) {
        Color rarityColor = getRarityColor(itemDef.rarity);

        // Aura de rareté
        if (itemDef.rarity != RealisticItemGenerator.ItemRarity.COMMON) {
            gc.setStroke(rarityColor.deriveColor(0, 1, 1, 0.6));
            gc.setLineWidth(2);
            gc.strokeOval(x - 8, y - 8, 16, 16);
        }

        // Forme selon le type d'item
        renderItemShape(item, itemDef, x, y, rarityColor);

        // Quantité
        if (item.count > 1) {
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeText(String.valueOf(item.count), x + 6, y - 6);
            gc.fillText(String.valueOf(item.count), x + 6, y - 6);
        }

        // Effets spéciaux selon la rareté
        renderRarityEffects(x, y, itemDef.rarity);
    }

    private void renderItemShape(GameModel.Item item, RealisticItemGenerator.ItemDefinition itemDef,
                                 double x, double y, Color rarityColor) {

        switch (itemDef.type) {
            case WEAPON:
                renderWeaponIcon(x, y, rarityColor, item.type);
                break;
            case ARMOR:
            case HELMET:
            case PANTS:
                renderArmorIcon(x, y, rarityColor, itemDef.type);
                break;
            case RING:
                renderRingIcon(x, y, rarityColor);
                break;
            case CONSUMABLE:
                renderConsumableIcon(x, y, rarityColor, item.type);
                break;
            case TREASURE:
                renderTreasureIcon(x, y, rarityColor, item.type);
                break;
            case KEY:
                renderKeyIcon(x, y, rarityColor);
                break;
            case MISC:
                renderMiscIcon(x, y, rarityColor, item.type);
                break;
            default:
                renderDefaultItem(item, x, y);
                break;
        }
    }

    private void renderWeaponIcon(double x, double y, Color color, String weaponType) {
        gc.setStroke(color);
        gc.setLineWidth(2);

        if (weaponType.contains("sword")) {
            // Épée
            gc.strokeLine(x, y - 6, x, y + 6);
            gc.strokeLine(x - 3, y - 4, x + 3, y - 4); // Garde
            gc.setFill(color);
            gc.fillOval(x - 1, y + 4, 2, 4); // Poignée
        } else if (weaponType.contains("bow")) {
            // Arc
            gc.strokeArc(x - 4, y - 6, 8, 12, 45, 90, javafx.scene.shape.ArcType.OPEN);
            gc.strokeLine(x, y - 2, x + 3, y); // Flèche
        } else {
            // Arme générique
            gc.strokeLine(x, y - 6, x, y + 6);
            gc.strokeLine(x - 2, y - 3, x + 2, y - 3);
        }
    }

    private void renderArmorIcon(double x, double y, Color color, GameModel.ItemType armorType) {
        gc.setFill(color.deriveColor(0, 1, 1, 0.7));
        gc.setStroke(color);
        gc.setLineWidth(1);

        switch (armorType) {
            case HELMET:
                // Casque
                gc.fillArc(x - 4, y - 6, 8, 8, 0, 180, javafx.scene.shape.ArcType.ROUND);
                gc.strokeArc(x - 4, y - 6, 8, 8, 0, 180, javafx.scene.shape.ArcType.OPEN);
                break;
            case ARMOR:
                // Armure (torse)
                gc.fillRect(x - 4, y - 3, 8, 6);
                gc.strokeRect(x - 4, y - 3, 8, 6);
                break;
            case PANTS:
                // Pantalon
                gc.fillRect(x - 3, y - 1, 2, 5);
                gc.fillRect(x + 1, y - 1, 2, 5);
                gc.strokeRect(x - 3, y - 1, 2, 5);
                gc.strokeRect(x + 1, y - 1, 2, 5);
                break;
        }
    }

    private void renderRingIcon(double x, double y, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(2);
        gc.strokeOval(x - 3, y - 3, 6, 6);

        // Gemme
        gc.setFill(color.brighter());
        gc.fillOval(x - 1, y - 1, 2, 2);
    }

    private void renderConsumableIcon(double x, double y, Color color, String consumableType) {
        if (consumableType.contains("potion")) {
            // Fiole
            gc.setFill(color.deriveColor(0, 1, 1, 0.7));
            gc.fillRect(x - 2, y - 5, 4, 8);
            gc.fillRect(x - 1, y - 6, 2, 2); // Bouchon

            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeRect(x - 2, y - 5, 4, 8);
        } else if (consumableType.contains("bread") || consumableType.contains("food")) {
            // Nourriture
            gc.setFill(Color.SANDYBROWN);
            gc.fillOval(x - 3, y - 2, 6, 4);
            gc.setStroke(Color.SADDLEBROWN);
            gc.setLineWidth(1);
            gc.strokeOval(x - 3, y - 2, 6, 4);
        } else {
            // Consommable générique (parchemin)
            gc.setFill(Color.BEIGE);
            gc.fillRect(x - 3, y - 4, 6, 8);
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeRect(x - 3, y - 4, 6, 8);
        }
    }

    private void renderTreasureIcon(double x, double y, Color color, String treasureType) {
        if (treasureType.contains("coin")) {
            // Pièce
            gc.setFill(Color.GOLD);
            gc.fillOval(x - 3, y - 3, 6, 6);
            gc.setStroke(Color.DARKGOLDENROD);
            gc.setLineWidth(1);
            gc.strokeOval(x - 3, y - 3, 6, 6);

            // Symbole sur la pièce
            gc.setFill(Color.DARKGOLDENROD);
            gc.fillText("$", x - 2, y + 2);
        } else if (treasureType.contains("gem") || treasureType.contains("ruby") || treasureType.contains("diamond")) {
            // Gemme
            gc.setFill(color);
            double[] gemX = {x, x + 3, x, x - 3};
            double[] gemY = {y - 3, y, y + 3, y};
            gc.fillPolygon(gemX, gemY, 4);

            gc.setStroke(color.darker());
            gc.setLineWidth(1);
            gc.strokePolygon(gemX, gemY, 4);
        } else {
            // Trésor générique
            gc.setFill(Color.GOLD.deriveColor(0, 1, 1, 0.8));
            gc.fillRect(x - 3, y - 3, 6, 6);
            gc.setStroke(Color.DARKGOLDENROD);
            gc.setLineWidth(1);
            gc.strokeRect(x - 3, y - 3, 6, 6);
        }
    }

    private void renderKeyIcon(double x, double y, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(2);

        // Tige de la clé
        gc.strokeLine(x - 4, y, x + 2, y);

        // Anneau
        gc.strokeOval(x - 6, y - 2, 4, 4);

        // Dents
        gc.strokeLine(x + 2, y, x + 2, y - 2);
        gc.strokeLine(x + 2, y, x + 2, y + 2);
    }

    private void renderMiscIcon(double x, double y, Color color, String miscType) {
        if (miscType.contains("wood")) {
            // Bois
            gc.setFill(Color.SADDLEBROWN);
            gc.fillRect(x - 4, y - 2, 8, 4);
            gc.setStroke(Color.DARKRED);
            gc.setLineWidth(1);
            gc.strokeRect(x - 4, y - 2, 8, 4);

            // Anneaux du bois
            gc.strokeLine(x - 2, y - 2, x - 2, y + 2);
            gc.strokeLine(x + 2, y - 2, x + 2, y + 2);
        } else if (miscType.contains("ore") || miscType.contains("iron")) {
            // Minerai
            gc.setFill(Color.GRAY);
            gc.fillPolygon(new double[]{x-3, x+3, x+2, x-2}, new double[]{y-2, y-1, y+3, y+2}, 4);
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1);
            gc.strokePolygon(new double[]{x-3, x+3, x+2, x-2}, new double[]{y-2, y-1, y+3, y+2}, 4);
        } else if (miscType.contains("herb")) {
            // Herbe
            gc.setStroke(Color.GREEN);
            gc.setLineWidth(2);
            gc.strokeLine(x, y + 2, x, y - 2);
            gc.strokeLine(x - 2, y, x + 2, y - 1);
            gc.strokeLine(x - 1, y + 1, x + 1, y - 2);
        } else {
            // Objet divers générique
            gc.setFill(color.deriveColor(0, 1, 1, 0.7));
            gc.fillOval(x - 3, y - 3, 6, 6);
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeOval(x - 3, y - 3, 6, 6);
        }
    }

    private void renderDefaultItem(GameModel.Item item, double x, double y) {
        // Rendu par défaut (comme dans l'original mais amélioré)
        gc.setFill(Color.YELLOW.deriveColor(0, 1, 1, 0.8));
        gc.fillOval(x - 6, y - 6, 12, 12);

        gc.setStroke(Color.DARKRED);
        gc.setLineWidth(2);
        gc.strokeLine(x - 5, y, x + 5, y);
        gc.strokeLine(x, y - 5, x, y + 5);

        if (item.count > 1) {
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeText(String.valueOf(item.count), x + 6, y - 6);
            gc.fillText(String.valueOf(item.count), x + 6, y - 6);
        }
    }

    private void renderRarityEffects(double x, double y, RealisticItemGenerator.ItemRarity rarity) {
        switch (rarity) {
            case UNCOMMON:
                // Éclat vert léger
                gc.setStroke(Color.LIME.deriveColor(0, 1, 1, 0.3));
                gc.setLineWidth(1);
                gc.strokeOval(x - 10, y - 10, 20, 20);
                break;

            case RARE:
                // Particules bleues
                for (int i = 0; i < 3; i++) {
                    double particleX = x + (Math.random() - 0.5) * 20;
                    double particleY = y + (Math.random() - 0.5) * 20;
                    gc.setFill(Color.BLUE.deriveColor(0, 1, 1, 0.6));
                    gc.fillOval(particleX - 1, particleY - 1, 2, 2);
                }
                break;

            case EPIC:
                // Aura violette pulsante
                double pulseRadius = 12 + Math.sin(System.currentTimeMillis() / 200.0) * 3;
                gc.setStroke(Color.PURPLE.deriveColor(0, 1, 1, 0.5));
                gc.setLineWidth(2);
                gc.strokeOval(x - pulseRadius, y - pulseRadius, pulseRadius * 2, pulseRadius * 2);
                break;

            case LEGENDARY:
                // Effet arc-en-ciel rotatif
                double time = System.currentTimeMillis() / 100.0;
                for (int i = 0; i < 6; i++) {
                    double angle = (time + i * Math.PI / 3) % (Math.PI * 2);
                    double rayX = x + Math.cos(angle) * 15;
                    double rayY = y + Math.sin(angle) * 15;

                    Color rayColor = Color.hsb((time * 10 + i * 60) % 360, 1.0, 1.0, 0.7);
                    gc.setStroke(rayColor);
                    gc.setLineWidth(2);
                    gc.strokeLine(x, y, rayX, rayY);
                }
                break;
        }
    }

    // ================================
    // MÉTHODES UTILITAIRES
    // ================================

    private Color getRarityColor(RealisticItemGenerator.ItemRarity rarity) {
        switch (rarity) {
            case COMMON: return Color.WHITE;
            case UNCOMMON: return Color.LIME;
            case RARE: return Color.BLUE;
            case EPIC: return Color.PURPLE;
            case LEGENDARY: return Color.ORANGE;
            default: return Color.GRAY;
        }
    }

    private Color getChestColor(String chestType) {
        switch (chestType) {
            case "wooden_chest": return Color.SADDLEBROWN;
            case "iron_chest": return Color.GRAY;
            case "treasure_chest": return Color.DARKGOLDENROD;
            case "legendary_chest": return Color.PURPLE;
            default: return Color.BROWN;
        }
    }

    private String extractChestTypeFromItem(String itemType) {
        if (itemType.startsWith("chest_")) {
            return itemType.substring(6); // Enlever "chest_"
        }
        return "wooden_chest"; // Défaut
    }

    /**
     * Modification de renderTile pour utiliser le nouveau système d'items
     */
    private void renderTileWithEnhancedItems(GameModel model, int x, int y) {
        Point2D screenPos = tileToScreen(x, y);
        double screenX = screenPos.getX();
        double screenY = screenPos.getY();

        // Culling
        if (screenX < -TILE_WIDTH || screenX > CANVAS_WIDTH + TILE_WIDTH ||
                screenY < -WALL_HEIGHT || screenY > CANVAS_HEIGHT + TILE_HEIGHT) {
            return;
        }

        // Rendu du sol (inchangé)
        int floorIndex = model.getFloorMap()[x][y];
        if (floorIndex >= 0) {
            Image floorImg = floorImages.get("floor_" + floorIndex);
            if (floorImg != null) {
                gc.drawImage(floorImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2);
            }
        }

        // Rendu des items améliorés (remplace l'ancien système)
        renderEnhancedItems(model, x, y, screenX, screenY);

        // Calcul de transparence (inchangé)
        double alpha = calculateAlpha(model, x, y);

        // Rendu des murs (inchangé)
        int wallIndex = model.getWallMap()[x][y];
        if (wallIndex >= 0) {
            String wallImageKey = "wall_" + wallIndex;

            if (model.getWallTypes()[x][y] == GameModel.WallType.DOOR) {
                GameModel.WallProperties props = model.getWallProperties()[x][y];
                if (props != null && props.isOpen) {
                    wallImageKey = "wall_" + wallIndex + "_o";
                }
            }

            Image wallImg = wallImages.get(wallImageKey);
            if (wallImg == null) {
                wallImg = wallImages.get("wall_" + wallIndex);
            }

            if (wallImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(wallImg, screenX - TILE_WIDTH/2, screenY - WALL_HEIGHT + TILE_HEIGHT/2);
                gc.setGlobalAlpha(1.0);

                if (model.getWallTypes()[x][y] == GameModel.WallType.DESTRUCTIBLE) {
                    GameModel.WallProperties props = model.getWallProperties()[x][y];
                    if (props != null && props.health < 255) {
                        renderHealthBar(screenX, screenY - WALL_HEIGHT + TILE_HEIGHT/2, props.health, 255);
                    }
                }
            }
        }

        // Rendu des plafonds (inchangé)
        int ceilingIndex = model.getCeilingMap()[x][y];
        if (ceilingIndex >= 0) {
            Image ceilingImg = ceilingImages.get("ceiling_" + ceilingIndex);
            if (ceilingImg != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(ceilingImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2 - WALL_HEIGHT);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    /**
     * Tooltip pour les items au survol de la souris
     */
    private void renderItemTooltip(GameModel model, Point2D mousePos) {
        if (mousePos == null) return;

        int x = (int)mousePos.getX();
        int y = (int)mousePos.getY();

        if (!model.isValidTile(x, y)) return;

        List<GameModel.Item> items = model.getItemMap()[x][y];
        if (items.isEmpty()) return;

        // Position du tooltip
        Point2D screenPos = tileToScreen(x, y);
        double tooltipX = screenPos.getX() + 20;
        double tooltipY = screenPos.getY() - 30;

        // Fond du tooltip
        double tooltipWidth = 150;
        double tooltipHeight = Math.min(items.size() * 20 + 10, 120);

        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.8));
        gc.fillRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 5, 5);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRoundRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 5, 5);

        // Contenu du tooltip
        gc.setFont(javafx.scene.text.Font.font(10));
        double textY = tooltipY + 15;

        for (int i = 0; i < Math.min(items.size(), 5); i++) { // Limiter à 5 items
            GameModel.Item item = items.get(i);
            RealisticItemGenerator.ItemDefinition itemDef = RealisticItemGenerator.getItemDefinition(item.type);

            String displayName = itemDef != null ? itemDef.displayName : item.type;
            String quantityStr = item.count > 1 ? " x" + item.count : "";

            // Couleur selon la rareté
            Color textColor = Color.WHITE;
            if (itemDef != null) {
                textColor = getRarityColor(itemDef.rarity);
            }

            gc.setFill(textColor);
            gc.fillText(displayName + quantityStr, tooltipX + 5, textY);

            textY += 20;
        }

        // Indicateur s'il y a plus d'items
        if (items.size() > 5) {
            gc.setFill(Color.GRAY);
            gc.fillText("... et " + (items.size() - 5) + " de plus", tooltipX + 5, textY);
        }
    }

    /**
     * Indicateur visuel pour les coffres interactifs
     */
    private void renderChestInteractionIndicator(GameModel model, Point2D hoverPos) {
        if (hoverPos == null) return;

        int x = (int)hoverPos.getX();
        int y = (int)hoverPos.getY();

        if (!model.isValidTile(x, y)) return;

        // Vérifier s'il y a un coffre
        List<GameModel.Item> items = model.getItemMap()[x][y];
        boolean hasChest = items.stream().anyMatch(item -> item.type.startsWith("chest_"));

        if (hasChest) {
            Point2D screenPos = tileToScreen(x, y);
            double screenX = screenPos.getX();
            double screenY = screenPos.getY();

            // Indicateur de survol pour coffre
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(3);
            double radius = 15 + Math.sin(System.currentTimeMillis() / 200.0) * 3;
            gc.strokeOval(screenX - radius, screenY - radius, radius * 2, radius * 2);

            // Icône d'interaction
            gc.setFill(Color.YELLOW);
            gc.fillText("E", screenX - 3, screenY - 20);
        }
    }

    /**
     * Méthode principale de rendu mise à jour
     */
    public void renderWithEnhancedItems(GameModel model) {
        // Fond
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.rgb(20, 20, 30));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Rendu en ordre isométrique avec le nouveau système d'items
        for (int sum = 0; sum < GameModel.MAP_SIZE * 2; sum++) {
            for (int x = 0; x < GameModel.MAP_SIZE; x++) {
                int y = sum - x;
                if (y < 0 || y >= GameModel.MAP_SIZE) continue;

                renderTileWithEnhancedItems(model, x, y);
            }
        }

        // Rendu des ennemis (inchangé)
        renderEnemies(model);

        // Rendu du joueur (inchangé)
        renderPlayer(model);

        // Rendu des projectiles (inchangé)
        renderProjectiles(model);

        // Indicateurs de souris avec support des coffres
        renderMouseIndicatorsWithChests(model);

        // Interface de combat (inchangée)
        renderCombatUI(model);

        // Tooltip des items
        renderItemTooltip(model, mouseHoverPos);
    }

    private void renderMouseIndicatorsWithChests(GameModel model) {
        // Indicateurs existants...
        Point2D clickedPos = model.getClickedPosition();
        if (clickedPos != null && model.isValidTile((int)clickedPos.getX(), (int)clickedPos.getY())) {
            renderTileIndicator(clickedPos, Color.BLUE, 0.6);
        }

        if (mouseHoverPos != null && model.isValidTile((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY())) {
            boolean canWalk = model.canWalkThrough((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY());
            boolean hasEnemy = hasEnemyAtPosition(mouseHoverPos, model);

            // Vérifier s'il y a un coffre
            List<GameModel.Item> items = model.getItemMap()[(int)mouseHoverPos.getX()][(int)mouseHoverPos.getY()];
            boolean hasChest = items.stream().anyMatch(item -> item.type.startsWith("chest_"));

            Color indicatorColor;
            if (hasChest) {
                indicatorColor = Color.GOLD; // Coffre = doré
                renderChestInteractionIndicator(model, mouseHoverPos);
            } else if (hasEnemy) {
                indicatorColor = Color.RED; // Ennemi = rouge
            } else if (canWalk) {
                indicatorColor = Color.LIME; // Libre = vert
            } else {
                indicatorColor = Color.DARKRED; // Bloqué = rouge foncé
            }

            renderTileIndicator(mouseHoverPos, indicatorColor, 0.4);
        }
    }

}