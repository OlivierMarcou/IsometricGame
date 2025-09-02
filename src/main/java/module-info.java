module catmeme {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;

    exports com.isometric.game;
    opens com.isometric.game to javafx.fxml;
    exports com.isometric.game.model;
    opens com.isometric.game.model to javafx.fxml;
    exports com.isometric.game.inventory;
    opens com.isometric.game.inventory to javafx.fxml;
}