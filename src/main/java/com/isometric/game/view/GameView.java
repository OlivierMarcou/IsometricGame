package com.isometric.game.view;

import com.isometric.game.model.GameModel;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

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
        int floorCount = 0;
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/sol/floor_" + i + ".png");
            if (image != null) {
                floorImages.put("floor_" + i, image);
                floorCount++;
            }
        }

        // Charger les images de murs
        int wallCount = 0;
        for (int i = 0; i < 50; i++) {
            Image image = loadImage("/murs/wall_" + i + ".png");
            if (image != null) {
                wallImages.put("wall_" + i, image);
                wallCount++;
            }

            // Charger aussi les images de portes ouvertes
            Image openImage = loadImage("/murs/wall_" + i + "_o.png");
            if (openImage != null) {
                wallImages.put("wall_" + i + "_o", openImage);
            }
        }

        // Charger les images de plafonds
        int ceilingCount = 0;
        for (int i = 0; i < 30; i++) {
            Image image = loadImage("/plafonds/ceiling_" + i + ".png");
            if (image != null) {
                ceilingImages.put("ceiling_" + i, image);
                ceilingCount++;
            }
        }

        System.out.println("Images chargées: " + floorCount + " sols, " +
                wallCount + " murs, " + ceilingCount + " plafonds");

        // Debug détaillé si peu d'images
        if (floorCount < 10) {
            System.out.println("⚠️ Peu d'images de sol trouvées. Vérifiez le dossier /sol/");
        }
        if (wallCount < 10) {
            System.out.println("⚠️ Peu d'images de murs trouvées. Vérifiez le dossier /murs/");
        }
        if (ceilingCount < 5) {
            System.out.println("⚠️ Peu d'images de plafonds trouvées. Vérifiez le dossier /plafonds/");
        }
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
        // Fond
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(Color.rgb(20, 20, 30));
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Rendu en ordre isométrique
        for (int sum = 0; sum < GameModel.MAP_SIZE * 2; sum++) {
            for (int x = 0; x < GameModel.MAP_SIZE; x++) {
                int y = sum - x;
                if (y < 0 || y >= GameModel.MAP_SIZE) continue;

                renderTile(model, x, y);
            }
        }

        renderPlayer(model);
        renderMouseIndicators(model);
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

        // Debug pour une tuile spécifique (centre de la carte)
        boolean isDebugTile = (x == GameModel.MAP_SIZE/2 && y == GameModel.MAP_SIZE/2);

        // Rendu du sol
        int floorIndex = model.getFloorMap()[x][y];
        if (floorIndex >= 0) {
            Image floorImg = floorImages.get("floor_" + floorIndex);
            if (floorImg != null) {
                gc.drawImage(floorImg, screenX - TILE_WIDTH/2, screenY - TILE_HEIGHT/2);
            } else if (isDebugTile) {
                System.out.println("Image sol manquante: floor_" + floorIndex);
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
                        renderHealthBar(screenX, screenY - WALL_HEIGHT + TILE_HEIGHT/2, props.health);
                    }
                }
            } else if (isDebugTile) {
                System.out.println("Image mur manquante: " + wallImageKey);
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
            } else if (isDebugTile) {
                System.out.println("Image plafond manquante: ceiling_" + ceilingIndex);
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
            gc.fillText(message, screenX - message.length() * 3, screenY - 30);
        }
    }

    private void renderHealthBar(double x, double y, int health) {
        double barWidth = 30;
        double barHeight = 4;
        double healthPercent = health / 255.0;

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

    private void renderMouseIndicators(GameModel model) {
        // Losange bleu sur la position cliquée
        Point2D clickedPos = model.getClickedPosition();
        if (clickedPos != null && model.isValidTile((int)clickedPos.getX(), (int)clickedPos.getY())) {
            renderTileIndicator(clickedPos, Color.BLUE, 0.6);
        }

        // Losange vert/rouge sur la position du curseur
        if (mouseHoverPos != null && model.isValidTile((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY())) {
            boolean canWalk = model.canWalkThrough((int)mouseHoverPos.getX(), (int)mouseHoverPos.getY());
            Color indicatorColor = canWalk ? Color.LIME : Color.RED;
            renderTileIndicator(mouseHoverPos, indicatorColor, 0.4);
        }
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

    public Point2D screenToTile(double screenX, double screenY) {
        // Convertir en coordonnées monde (avec la caméra)
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

        // Conversion monde vers coordonnées écran (avec la caméra)
        double screenX = worldX - cameraX + CANVAS_WIDTH / 2;
        double screenY = worldY - cameraY + CANVAS_HEIGHT / 2;

        return new Point2D(screenX, screenY);
    }

    public void centerCameraOnPlayer(GameModel model) {
        Point2D playerPos = model.getPlayerPosition();
        Point2D playerScreenPos = tileToScreen(playerPos.getX(), playerPos.getY());

        cameraX = playerScreenPos.getX() - CANVAS_WIDTH / 2;
        cameraY = playerScreenPos.getY() - CANVAS_HEIGHT / 2;

        System.out.println("Caméra centrée sur le personnage à la position (" +
                (int)playerPos.getX() + ", " + (int)playerPos.getY() + ")");
    }

    public void updateCameraToFollowPlayer(GameModel model) {
        Point2D currentPos = model.getCurrentInterpolatedPosition();
        Point2D playerScreenPos = tileToScreen(currentPos.getX(), currentPos.getY());

        double targetCameraX = playerScreenPos.getX() - CANVAS_WIDTH / 2;
        double targetCameraY = playerScreenPos.getY() - CANVAS_HEIGHT / 2;

        // Mouvement fluide de caméra
        cameraX += (targetCameraX - cameraX) * 0.05;
        cameraY += (targetCameraY - cameraY) * 0.05;
    }
}