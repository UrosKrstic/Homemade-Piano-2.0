package piano;

import exceptions.FileException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javax.sound.midi.MidiUnavailableException;


public class Main extends Application {
    private final static int cap = 60;
    private Stage window;
    private LoadingScene loadingScene;
    private KeyToMidiAndNoteMap map;
    private Scene mainScene;
    private MenuBar menuBar;
    private Piano piano;

    @Override
    public void start(Stage primaryStage) throws Exception{
        try {
            window = primaryStage;
            window.setTitle("Homemade Piano");

            LoadingScene.initializeLoadingPane();
            map = new KeyToMidiAndNoteMap("src/piano/map.csv", cap);
            loadingScene = new LoadingScene(1024, 640);
            window.setScene(loadingScene);
            window.show();
            map.start();
            window.setResizable(false);
            mainScene = initializeMainScene();
            map.join();
            if (map.errorOccurred())
                throw new FileException("Incorrect format for input map file");

            System.out.println("FUK1");
            System.out.println("FUK2");
            loadingScene.join();
            window.setScene(mainScene);

            mainScene.setOnKeyPressed(ke -> {
                piano.keyPressed(ke);
            });
            mainScene.setOnKeyReleased(ke -> {
                piano.keyReleased(ke);
            });
            window.show();


        } catch(FileException fe) {
            System.out.println(fe.getMessage());
        } catch (InterruptedException | MidiUnavailableException ie) {
            ie.printStackTrace();
        }

    }

    private Scene initializeMainScene() throws MidiUnavailableException {
        BorderPane mainPane = new BorderPane();

        //TODO: add functionality to the menu bar
        menuBar = new MenuBar();
        mainPane.setTop(menuBar);

        AnchorPane center = new AnchorPane();
        //TODO: add composition view to center
        piano = new Piano(map, 81, 223,  882, 211);
        center.getChildren().add(piano);
        mainPane.setCenter(center);
        mainPane.setBottom(new Button("Hello m8"));

        return new Scene(mainPane, 1024, 640);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
