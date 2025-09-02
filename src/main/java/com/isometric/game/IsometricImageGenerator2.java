package com.isometric.game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Générateur d'images isométriques avec couleurs aléatoires uniques
 * et fond transparent pour sols, murs et plafonds
 */
public class IsometricImageGenerator2 extends Application {

    private static final int TILE_WIDTH = 64;
    private static final int TILE_HEIGHT = 32;
    private static final int WALL_HEIGHT = 128;

    // Palettes de couleurs pour différents types
    private static final Map<String, Color[]> COLOR_PALETTES = new HashMap<>();
    private static final Random random = new Random();
    private Set<String> usedColors = new HashSet<>();

    static {
        // Palette pour les sols
        COLOR_PALETTES.put("grass", new Color[]{
                Color.rgb(34, 139, 34), Color.rgb(50, 205, 50), Color.rgb(0, 128, 0),
                Color.rgb(124, 252, 0), Color.rgb(127, 255, 0), Color.rgb(173, 255, 47)
        });

        COLOR_PALETTES.put("stone", new Color[]{
                Color.rgb(105, 105, 105), Color.rgb(169, 169, 169), Color.rgb(128, 128, 128),
                Color.rgb(192, 192, 192), Color.rgb(112, 128, 144), Color.rgb(119, 136, 153)
        });

        COLOR_PALETTES.put("dirt", new Color[]{
                Color.rgb(139, 69, 19), Color.rgb(160, 82, 45), Color.rgb(210, 180, 140),
                Color.rgb(222, 184, 135), Color.rgb(205, 133, 63), Color.rgb(188, 143, 143)
        });

        COLOR_PALETTES.put("water", new Color[]{
                Color.rgb(0, 191, 255), Color.rgb(30, 144, 255), Color.rgb(100, 149, 237),
                Color.rgb(135, 206, 250), Color.rgb(173, 216, 230), Color.rgb(176, 224, 230)
        });

        // Palette pour les murs
        COLOR_PALETTES.put("wood", new Color[]{
                Color.rgb(139, 69, 19), Color.rgb(160, 82, 45), Color.rgb(205, 133, 63),
                Color.rgb(222, 184, 135), Color.rgb(139, 90, 43), Color.rgb(101, 67, 33)
        });

        COLOR_PALETTES.put("brick", new Color[]{
                Color.rgb(178, 34, 34), Color.rgb(220, 20, 60), Color.rgb(139, 0, 0),
                Color.rgb(165, 42, 42), Color.rgb(128, 0, 0), Color.rgb(205, 92, 92)
        });

        COLOR_PALETTES.put("glass", new Color[]{
                Color.rgb(240, 248, 255, 0.7), Color.rgb(230, 230, 250, 0.7), Color.rgb(248, 248, 255, 0.7),
                Color.rgb(176, 224, 230, 0.7), Color.rgb(173, 216, 230, 0.7), Color.rgb(135, 206, 250, 0.7)
        });

        // Palette pour les plafonds
        COLOR_PALETTES.put("thatch", new Color[]{
                Color.rgb(205, 133, 63), Color.rgb(210, 180, 140), Color.rgb(222, 184, 135),
                Color.rgb(244, 164, 96), Color.rgb(238, 203, 173), Color.rgb(255, 228, 196)
        });

        COLOR_PALETTES.put("tile", new Color[]{
                Color.rgb(178, 34, 34), Color.rgb(139, 0, 0), Color.rgb(128, 0, 0),
                Color.rgb(220, 20, 60), Color.rgb(165, 42, 42), Color.rgb(205, 92, 92)
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Créer les répertoires s'ils n'existent pas
        createDirectories();

        // Générer toutes les images
        generateFloorImages();
        generateWallImages();
        generateCeilingImages();

        System.out.println("Génération terminée !");
        primaryStage.close();
    }

    private void createDirectories() {
        new File("src/main/resources/sol").mkdirs();
        new File("src/main/resources/murs").mkdirs();
        new File("src/main/resources/plafonds").mkdirs();
    }

    private void generateFloorImages() throws IOException {
        System.out.println("Génération des images de sol...");

        String[] floorTypes = {"grass", "stone", "dirt", "water"};

        for (int i = 0; i < 50; i++) {
            String type = floorTypes[i % floorTypes.length];
            WritableImage image = createFloorTile(type, i);
            saveImage(image, "src/main/resources/sol/floor_" + i + ".png");
        }
    }

    private void generateWallImages() throws IOException {
        System.out.println("Génération des images de murs...");

        String[] wallTypes = {"wood", "brick", "glass"};

        for (int i = 0; i < 50; i++) {
            String type = wallTypes[i % wallTypes.length];

            // Générer l'image normale du mur
            WritableImage image = createWallTile(type, i);
            saveImage(image, "src/main/resources/murs/wall_" + i + ".png");

            // Si c'est une porte (indices 25-29 selon le générateur de carte),
            // générer aussi la version ouverte
            if (i >= 25 && i <= 29) {
                WritableImage openImage = createOpenDoorTile(type, i);
                saveImage(openImage, "src/main/resources/murs/wall_" + i + "_o.png");
                System.out.println("  ✅ Porte ouverte générée : wall_" + i + "_o.png");
            }
        }
    }

    private void generateCeilingImages() throws IOException {
        System.out.println("Génération des images de plafonds...");

        String[] ceilingTypes = {"thatch", "tile"};

        for (int i = 0; i < 30; i++) {
            String type = ceilingTypes[i % ceilingTypes.length];
            WritableImage image = createCeilingTile(type, i);
            saveImage(image, "src/main/resources/plafonds/ceiling_" + i + ".png");
        }
    }

    private WritableImage createFloorTile(String type, int variant) {
        Canvas canvas = new Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond transparent (ne pas effacer avec une couleur)
        gc.clearRect(0, 0, TILE_WIDTH, TILE_HEIGHT);

        Color baseColor = getUniqueColor(type, variant);
        Color lightColor = baseColor.brighter();
        Color darkColor = baseColor.darker();

        // Forme losange isométrique - les pixels en dehors resteront transparents
        double[] xPoints = {TILE_WIDTH/2.0, TILE_WIDTH, TILE_WIDTH/2.0, 0};
        double[] yPoints = {0, TILE_HEIGHT/2.0, TILE_HEIGHT, TILE_HEIGHT/2.0};

        // Dégradé selon le type
        LinearGradient gradient = createGradient(baseColor, lightColor, darkColor);
        gc.setFill(gradient);
        gc.fillPolygon(xPoints, yPoints, 4);

        // Détails selon le type
        addFloorDetails(gc, type, baseColor, xPoints, yPoints);

        // Contour
        gc.setStroke(darkColor.darker());
        gc.setLineWidth(1);
        gc.strokePolygon(xPoints, yPoints, 4);

        return canvas.snapshot(null, null);
    }

    private WritableImage createOpenDoorTile(String type, int variant) {
        Canvas canvas = new Canvas(TILE_WIDTH, WALL_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond transparent
        gc.clearRect(0, 0, TILE_WIDTH, WALL_HEIGHT);

        Color baseColor = getUniqueColor(type, variant);
        Color lightColor = baseColor.brighter();
        Color darkColor = baseColor.darker();

        double wallBottom = WALL_HEIGHT - TILE_HEIGHT/2;

        // Pour une porte ouverte, on dessine seulement les montants de porte
        // et l'ouverture au milieu

        // Montant gauche de la porte
        gc.setFill(createVerticalGradient(lightColor, baseColor));
        gc.fillRect(TILE_WIDTH/4, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/8, WALL_HEIGHT - TILE_HEIGHT);

        // Montant droit de la porte
        gc.fillRect(TILE_WIDTH*5/8, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/8, WALL_HEIGHT - TILE_HEIGHT);

        // Linteau (partie haute) - plus fin que dans la version fermée
        gc.fillRect(TILE_WIDTH/4, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/2, TILE_HEIGHT/4);

        // Face droite des montants (parallélogrammes)
        double[] rightX1 = {TILE_WIDTH*3/8.0, TILE_WIDTH*7/16.0, TILE_WIDTH*7/16.0, TILE_WIDTH*3/8.0};
        double[] rightY1 = {wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - TILE_HEIGHT/2, wallBottom};
        gc.setFill(createVerticalGradient(baseColor, darkColor));
        gc.fillPolygon(rightX1, rightY1, 4);

        double[] rightX2 = {TILE_WIDTH*11/16.0, TILE_WIDTH*3/4.0, TILE_WIDTH*3/4.0, TILE_WIDTH*11/16.0};
        double[] rightY2 = {wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - TILE_HEIGHT/2, wallBottom};
        gc.fillPolygon(rightX2, rightY2, 4);

        // Top des montants (petits losanges)
        double[] topX1 = {TILE_WIDTH*5/16.0, TILE_WIDTH*7/16.0, TILE_WIDTH*5/16.0, TILE_WIDTH*3/16.0};
        double[] topY1 = {wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT*3/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT};
        gc.setFill(lightColor.brighter());
        gc.fillPolygon(topX1, topY1, 4);

        double[] topX2 = {TILE_WIDTH*11/16.0, TILE_WIDTH*13/16.0, TILE_WIDTH*11/16.0, TILE_WIDTH*9/16.0};
        double[] topY2 = {wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT*3/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT};
        gc.fillPolygon(topX2, topY2, 4);

        // Ajouter des détails pour indiquer que c'est une porte ouverte
        addOpenDoorDetails(gc, type, baseColor);

        // Contours des montants
        gc.setStroke(darkColor.darker());
        gc.setLineWidth(1);
        gc.strokeRect(TILE_WIDTH/4, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/8, WALL_HEIGHT - TILE_HEIGHT);
        gc.strokeRect(TILE_WIDTH*5/8, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/8, WALL_HEIGHT - TILE_HEIGHT);
        gc.strokePolygon(rightX1, rightY1, 4);
        gc.strokePolygon(rightX2, rightY2, 4);
        gc.strokePolygon(topX1, topY1, 4);
        gc.strokePolygon(topX2, topY2, 4);

        return canvas.snapshot(null, null);
    }

    private void addOpenDoorDetails(GraphicsContext gc, String type, Color baseColor) {
        double wallBottom = WALL_HEIGHT - TILE_HEIGHT/2;

        switch (type) {
            case "wood":
                // Grain du bois sur les montants
                gc.setStroke(baseColor.darker());
                gc.setLineWidth(0.5);
                // Montant gauche
                for (int i = 0; i < 3; i++) {
                    double y = wallBottom - WALL_HEIGHT + TILE_HEIGHT + i * (WALL_HEIGHT - TILE_HEIGHT) / 4;
                    gc.strokeLine(TILE_WIDTH/4 + 1, y, TILE_WIDTH*3/8 - 1, y);
                }
                // Montant droit
                for (int i = 0; i < 3; i++) {
                    double y = wallBottom - WALL_HEIGHT + TILE_HEIGHT + i * (WALL_HEIGHT - TILE_HEIGHT) / 4;
                    gc.strokeLine(TILE_WIDTH*5/8 + 1, y, TILE_WIDTH*3/4 - 1, y);
                }
                break;

            case "brick":
                // Motif de briques sur les montants
                gc.setStroke(baseColor.darker().darker());
                gc.setLineWidth(0.5);
                int brickHeight = 8;
                for (int row = 0; row < (WALL_HEIGHT - TILE_HEIGHT) / brickHeight; row++) {
                    double y = TILE_HEIGHT + row * brickHeight;
                    // Montant gauche
                    gc.strokeRect(TILE_WIDTH/4, y, TILE_WIDTH/8, brickHeight);
                    // Montant droit
                    gc.strokeRect(TILE_WIDTH*5/8, y, TILE_WIDTH/8, brickHeight);
                }
                break;

            case "glass":
                // Reflets sur les montants en verre
                gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.7));
                gc.setLineWidth(1);
                gc.strokeLine(TILE_WIDTH/4 + 2, TILE_HEIGHT + 5, TILE_WIDTH/4 + 4, TILE_HEIGHT + 15);
                gc.strokeLine(TILE_WIDTH*5/8 + 2, TILE_HEIGHT + 5, TILE_WIDTH*5/8 + 4, TILE_HEIGHT + 15);
                break;
        }

        // Ajouter une ombre dans l'ouverture pour indiquer la profondeur
        gc.setFill(Color.BLACK.deriveColor(0, 1, 1, 0.3));
        gc.fillRect(TILE_WIDTH*3/8, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/4, WALL_HEIGHT - TILE_HEIGHT);
    }

    private WritableImage createWallTile(String type, int variant) {
        Canvas canvas = new Canvas(TILE_WIDTH, WALL_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond transparent
        gc.clearRect(0, 0, TILE_WIDTH, WALL_HEIGHT);

        Color baseColor = getUniqueColor(type, variant);
        Color lightColor = baseColor.brighter();
        Color darkColor = baseColor.darker();

        // Ajustement d'opacité pour les murs en verre
        double wallOpacity = 1.0;
        if ("glass".equals(type)) {
            wallOpacity = 0.7; // Verre semi-transparent
            baseColor = Color.color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), wallOpacity);
            lightColor = Color.color(lightColor.getRed(), lightColor.getGreen(), lightColor.getBlue(), wallOpacity);
            darkColor = Color.color(darkColor.getRed(), darkColor.getGreen(), darkColor.getBlue(), wallOpacity);
        }

        // Face avant du mur (rectangle)
        double wallBottom = WALL_HEIGHT - TILE_HEIGHT/2;
        gc.setFill(createVerticalGradient(lightColor, baseColor));
        gc.fillRect(TILE_WIDTH/4, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/2, WALL_HEIGHT - TILE_HEIGHT);

        // Face droite du mur (parallélogramme)
        double[] rightX = {TILE_WIDTH*3/4.0, TILE_WIDTH, TILE_WIDTH, TILE_WIDTH*3/4.0};
        double[] rightY = {wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - TILE_HEIGHT/2, wallBottom};

        gc.setFill(createVerticalGradient(baseColor, darkColor));
        gc.fillPolygon(rightX, rightY, 4);

        // Top du mur (losange)
        double[] topX = {TILE_WIDTH/2.0, TILE_WIDTH, TILE_WIDTH/2.0, 0};
        double[] topY = {wallBottom - WALL_HEIGHT + TILE_HEIGHT/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT, wallBottom - WALL_HEIGHT + TILE_HEIGHT*3/2, wallBottom - WALL_HEIGHT + TILE_HEIGHT};

        gc.setFill(lightColor.brighter());
        gc.fillPolygon(topX, topY, 4);

        // Détails spéciaux pour le type
        addWallDetails(gc, type, baseColor, variant);

        // Contours
        gc.setStroke(darkColor.darker());
        gc.setLineWidth(1);
        gc.strokeRect(TILE_WIDTH/4, wallBottom - WALL_HEIGHT + TILE_HEIGHT, TILE_WIDTH/2, WALL_HEIGHT - TILE_HEIGHT);
        gc.strokePolygon(rightX, rightY, 4);
        gc.strokePolygon(topX, topY, 4);

        return canvas.snapshot(null, null);
    }

    private WritableImage createCeilingTile(String type, int variant) {
        Canvas canvas = new Canvas(TILE_WIDTH, TILE_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond transparent
        gc.clearRect(0, 0, TILE_WIDTH, TILE_HEIGHT);

        Color baseColor = getUniqueColor(type, variant);
        Color lightColor = baseColor.brighter();
        Color darkColor = baseColor.darker();

        // Forme losange isométrique (identique au sol mais avec d'autres couleurs)
        double[] xPoints = {TILE_WIDTH/2.0, TILE_WIDTH, TILE_WIDTH/2.0, 0};
        double[] yPoints = {0, TILE_HEIGHT/2.0, TILE_HEIGHT, TILE_HEIGHT/2.0};

        // Dégradé
        LinearGradient gradient = createGradient(baseColor, lightColor, darkColor);
        gc.setFill(gradient);
        gc.fillPolygon(xPoints, yPoints, 4);

        // Détails selon le type
        addCeilingDetails(gc, type, baseColor, xPoints, yPoints);

        // Contour
        gc.setStroke(darkColor.darker());
        gc.setLineWidth(1);
        gc.strokePolygon(xPoints, yPoints, 4);

        return canvas.snapshot(null, null);
    }

    private void addFloorDetails(GraphicsContext gc, String type, Color baseColor, double[] xPoints, double[] yPoints) {
        switch (type) {
            case "grass":
                // Petites taches d'herbe
                gc.setFill(baseColor.darker());
                for (int i = 0; i < 5; i++) {
                    double x = random.nextDouble() * TILE_WIDTH/2 + TILE_WIDTH/4;
                    double y = random.nextDouble() * TILE_HEIGHT/2 + TILE_HEIGHT/4;
                    gc.fillOval(x-1, y-1, 2, 2);
                }
                break;

            case "stone":
                // Fissures dans la pierre
                gc.setStroke(baseColor.darker().darker());
                gc.setLineWidth(0.5);
                double startX = TILE_WIDTH/4 + random.nextDouble() * TILE_WIDTH/2;
                double startY = TILE_HEIGHT/4 + random.nextDouble() * TILE_HEIGHT/2;
                double endX = startX + (random.nextDouble() - 0.5) * 20;
                double endY = startY + (random.nextDouble() - 0.5) * 10;
                gc.strokeLine(startX, startY, endX, endY);
                break;

            case "water":
                // Ondulations
                gc.setStroke(baseColor.brighter());
                gc.setLineWidth(1);
                for (int i = 0; i < 3; i++) {
                    double y = TILE_HEIGHT/4 + i * TILE_HEIGHT/6;
                    gc.strokeLine(TILE_WIDTH/4, y, TILE_WIDTH*3/4, y);
                }
                break;
        }
    }

    private void addWallDetails(GraphicsContext gc, String type, Color baseColor, int variant) {
        switch (type) {
            case "wood":
                // Planches de bois verticales
                gc.setStroke(baseColor.darker());
                gc.setLineWidth(1);
                for (int i = 1; i < 4; i++) {
                    double x = TILE_WIDTH/4 + i * TILE_WIDTH/8;
                    gc.strokeLine(x, TILE_HEIGHT, x, WALL_HEIGHT - TILE_HEIGHT);
                }
                break;

            case "brick":
                // Motif de briques
                gc.setStroke(baseColor.darker().darker());
                gc.setLineWidth(0.5);
                int brickHeight = 8;
                for (int row = 0; row < (WALL_HEIGHT - TILE_HEIGHT) / brickHeight; row++) {
                    double y = TILE_HEIGHT + row * brickHeight;
                    double offset = (row % 2) * TILE_WIDTH/8;
                    for (int col = 0; col < 4; col++) {
                        double x = TILE_WIDTH/4 + col * TILE_WIDTH/8 + offset;
                        if (x < TILE_WIDTH*3/4) {
                            gc.strokeRect(x, y, TILE_WIDTH/8, brickHeight);
                        }
                    }
                }
                break;

            case "glass":
                // Reflets sur le verre
                gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.5));
                gc.setLineWidth(2);
                gc.strokeLine(TILE_WIDTH/3, TILE_HEIGHT + 10, TILE_WIDTH/3 + 5, TILE_HEIGHT + 30);
                gc.strokeLine(TILE_WIDTH*2/3, TILE_HEIGHT + 20, TILE_WIDTH*2/3 + 3, TILE_HEIGHT + 40);
                break;
        }
    }

    private void addCeilingDetails(GraphicsContext gc, String type, Color baseColor, double[] xPoints, double[] yPoints) {
        switch (type) {
            case "thatch":
                // Texture de chaume
                gc.setStroke(baseColor.darker());
                gc.setLineWidth(0.5);
                for (int i = 0; i < 8; i++) {
                    double x1 = TILE_WIDTH/4 + random.nextDouble() * TILE_WIDTH/2;
                    double y1 = TILE_HEIGHT/4 + random.nextDouble() * TILE_HEIGHT/2;
                    double x2 = x1 + (random.nextDouble() - 0.5) * 10;
                    double y2 = y1 + (random.nextDouble() - 0.5) * 5;
                    gc.strokeLine(x1, y1, x2, y2);
                }
                break;

            case "tile":
                // Tuiles
                gc.setStroke(baseColor.darker());
                gc.setLineWidth(1);
                for (int i = 1; i < 4; i++) {
                    double startX = TILE_WIDTH/2 - i * TILE_WIDTH/8;
                    double endX = TILE_WIDTH/2 + i * TILE_WIDTH/8;
                    double y = TILE_HEIGHT/4 + i * TILE_HEIGHT/8;
                    gc.strokeLine(startX, y, endX, y);
                }
                break;
        }
    }

    private Color getUniqueColor(String type, int variant) {
        Color[] palette = COLOR_PALETTES.get(type);
        if (palette == null) {
            return Color.GRAY;
        }

        // Sélectionner une couleur de base
        Color baseColor = palette[variant % palette.length];

        // Créer une variation unique
        double hueShift = (variant * 137.5) % 360; // Nombre d'or pour répartition uniforme
        double saturation = 0.7 + (variant % 3) * 0.1;
        double brightness = 0.8 + (variant % 2) * 0.1;

        return Color.hsb((baseColor.getHue() + hueShift) % 360, saturation, brightness);
    }

    private LinearGradient createGradient(Color base, Color light, Color dark) {
        return new LinearGradient(0, 0, 1, 1, true, null,
                new Stop(0, light),
                new Stop(0.5, base),
                new Stop(1, dark)
        );
    }

    private LinearGradient createVerticalGradient(Color top, Color bottom) {
        return new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, top),
                new Stop(1, bottom)
        );
    }

    private void saveImage(WritableImage image, String path) throws IOException {
        // Conversion manuelle de WritableImage vers BufferedImage avec transparence
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        // Créer une image avec support de transparence (ARGB)
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = image.getPixelReader();

        // Copier pixel par pixel en conservant la transparence
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = pixelReader.getColor(x, y);

                // Convertir la couleur JavaFX en ARGB avec transparence
                int alpha = (int) (color.getOpacity() * 255);
                int red = (int) (color.getRed() * 255);
                int green = (int) (color.getGreen() * 255);
                int blue = (int) (color.getBlue() * 255);

                int argb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                bufferedImage.setRGB(x, y, argb);
            }
        }

        File output = new File(path);
        output.getParentFile().mkdirs();

        // Sauvegarder en PNG pour préserver la transparence
        ImageIO.write(bufferedImage, "PNG", output);
        System.out.println("Image sauvegardée : " + path);
    }

    public static void main(String[] args) {
        launch(args);
    }
}