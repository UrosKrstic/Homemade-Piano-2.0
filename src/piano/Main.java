package piano;

import exceptions.FileException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
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
    private MenuBar menuBar = new MenuBar();
    private Piano piano;
    private CompositionViewer compViewer;
    private ReceivedNoteHandler handler;
    private KeyReceiver pressedReceiver, releasedReceiver;

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

            loadingScene.join();
            window.setScene(mainScene);
            Composition comp = new Composition(map);
            comp.loadFromFile("src/piano/song.txt");
            compViewer.setComposition(comp);

            mainScene.setOnKeyPressed(ke -> piano.keyPressed(ke));
            mainScene.setOnKeyReleased(ke -> piano.keyReleased(ke));
            mainScene.setOnMousePressed(me -> piano.mousePressed(me));
            mainScene.setOnMouseReleased(me -> piano.mouseReleased(me));
            window.show();

            window.setOnCloseRequest(e -> {
                handler.stopWorking();
                pressedReceiver.stopWorking();
                releasedReceiver.stopWorking();
            });


        } catch(FileException fe) {
            System.out.println(fe.getMessage());
        } catch (InterruptedException | MidiUnavailableException ie) {
            ie.printStackTrace();
        }

    }

    private void initializeMenuBar() {
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().add(new MenuItem("Open..."));
        fileMenu.getItems().add(new MenuItem("Exit..."));

        Menu editMenu = new Menu("Edit");
        editMenu.getItems().add(new MenuItem("Record"));
        editMenu.getItems().add(new MenuItem("Play"));
        editMenu.getItems().add(new MenuItem("Stop"));

        Menu settingsMenu = new Menu("Settings");
        Menu helpMenu = new Menu("Help");

        menuBar.getMenus().addAll(fileMenu, editMenu, settingsMenu, helpMenu);
    }

    private Scene initializeMainScene() throws MidiUnavailableException {
        BorderPane mainPane = new BorderPane();

        //TODO: add functionality to the menu bar
        initializeMenuBar();
        mainPane.setTop(menuBar);

        AnchorPane center = new AnchorPane();
        compViewer = new CompositionViewer(81, 36, 882, 142);
        piano = new Piano(map, 81, 223,  882, 211);
        handler = new ReceivedNoteHandler(piano);
        handler.setCompositionViewer(compViewer);
        pressedReceiver = new KeyReceiver(handler, true);
        releasedReceiver = new KeyReceiver(handler, false);
        piano.setHandler(handler);
        piano.setPressedReceiver(pressedReceiver);
        piano.setReleasedReceiver(releasedReceiver);
        center.getChildren().addAll(compViewer, piano);
        mainPane.setCenter(center);
        mainPane.setBottom(new Button("Hello m8"));

        return new Scene(mainPane, 1024, 640);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
