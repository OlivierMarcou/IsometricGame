package com.isometric.game;

import com.isometric.game.controller.GameController;
import com.isometric.game.model.GameModel;
import com.isometric.game.view.GameView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Application principale utilisant l'architecture MVC
 */
public class IsometricGameMVC extends Application {

    private GameModel model;
    private GameView view;
    private GameController controller;

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== Démarrage du Jeu Isométrique MVC ===");

        // Initialiser les composants MVC
        initializeMVC();

        // Configurer la scène
        StackPane root = new StackPane(view.getCanvas());
        Scene scene = new Scene(root, GameView.CANVAS_WIDTH, GameView.CANVAS_HEIGHT);

        // Gestion des événements clavier
        scene.setOnKeyPressed(e -> {
            switch (e.getCode().toString()) {
                case "F11":
                    primaryStage.setFullScreen(!primaryStage.isFullScreen());
                    System.out.println("Mode plein écran: " + (primaryStage.isFullScreen() ? "ON" : "OFF"));
                    break;
                default:
                    controller.handleKeyPressed(e.getCode().toString());
                    break;
            }
        });

        // Configurer la fenêtre
        primaryStage.setTitle("Jeu Isométrique MVC - Village (F11 = Plein écran)");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Donner le focus au canvas pour les événements
        view.getCanvas().requestFocus();

        // Afficher les instructions
        printInstructions();

        // Démarrer la boucle de jeu
        controller.startGameLoop();

        System.out.println("=== Jeu démarré avec succès ! ===");
    }

    private void initializeMVC() {
        System.out.println("Initialisation de l'architecture MVC...");

        // Créer le modèle
        model = new GameModel();

        // Créer la vue
        view = new GameView();

        // Créer le contrôleur avec les références au modèle, vue et stage
        controller = new GameController(model, view, null); // sera mis à jour après

        System.out.println("Architecture MVC initialisée");
    }

    private void printInstructions() {
        System.out.println();
        System.out.println("=== Instructions de jeu ===");
        System.out.println("- F11 pour basculer en plein écran (recommandé)");
        System.out.println("- C pour recentrer la caméra sur le personnage");
        System.out.println("- Bougez la souris pour orienter le personnage");
        System.out.println("- Cliquez pour vous déplacer (losange bleu = destination)");
        System.out.println("- Curseur vert = case accessible, rouge = inaccessible");
        System.out.println("- Cliquez sur une porte adjacente pour l'ouvrir/fermer");
        System.out.println("- Le personnage contourne automatiquement les obstacles");
        System.out.println();
        System.out.println("=== Architecture ===");
        System.out.println("- Modèle: GameModel (logique métier et données)");
        System.out.println("- Vue: GameView (rendu et affichage)");
        System.out.println("- Contrôleur: GameController (interactions utilisateur)");
    }

    @Override
    public void stop() {
        System.out.println("Arrêt du jeu...");
        // Nettoyage si nécessaire
    }

    public static void main(String[] args) {
        System.out.println("=== Jeu Isométrique avec Architecture MVC ===");
        System.out.println("Chargement des composants...");
        launch(args);
    }
}