package piano;

import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;

public class ConfirmBox {
    private static boolean answer;

    public static boolean display(String title, String message) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL); // BLOCKS OTHER WINDOWS USERS EVENTS
        window.setTitle(title);
        Label label = new Label(message);

        Button yes = new Button("Yes");
        Button no = new Button("No");

        yes.setOnAction(e -> {
            answer = true;
            window.close();
        });
        no.setOnAction(e -> {
            answer = false;
            window.close();
        });


        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setId("pane");
        FlowPane flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setHgap(15);
        flowPane.getChildren().addAll(yes, no);
        layout.getChildren().addAll(label, flowPane);
        Scene scene = new Scene(layout, 300, 240);
        scene.getStylesheets().add("piano/alertStyle.css");
        window.setScene(scene);
        window.showAndWait();

        return answer;
    }
}
