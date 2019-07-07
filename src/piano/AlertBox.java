package piano;

import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;

public class AlertBox {
    public static void display(String title, String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL); // BLOCKS OTHER WINDOWS USERS EVENTS
        window.setTitle(title);

        Label label = new Label(message);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        Button button = new Button("Close");
        button.setOnAction(e -> window.close());

        VBox layout = new VBox(20);
        layout.setId("pane");
        layout.getChildren().addAll(label, button);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 230, 180);
        scene.getStylesheets().add("piano/alertStyle.css");
        window.setScene(scene);
        window.showAndWait();
    }
}
