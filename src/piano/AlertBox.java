package piano;

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
        window.setMinWidth(250);
        window.setMinHeight(150);

        Label label = new Label(message);
        Button button = new Button("Close");
        button.setOnAction(e -> window.close());

        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, button);//ovo je kul
        layout.setAlignment(Pos.CENTER); //ovo je kulje

        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
