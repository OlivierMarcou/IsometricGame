module catmeme {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;

    exports net.arkaine;
    opens net.arkaine to javafx.fxml;
    exports net.arkaine.model;
    opens net.arkaine.model to javafx.fxml;
    exports net.arkaine.inventory;
    opens net.arkaine.inventory to javafx.fxml;
}