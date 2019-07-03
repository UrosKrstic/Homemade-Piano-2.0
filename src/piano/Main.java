package piano;

import exceptions.FileException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.sound.midi.MidiUnavailableException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Main extends Application {
    private final static int SCREEENWIDTH = 1024, SCREENHEIGHT = 640;
    private final static int cap = 60;
    private Stage window;
    private LoadingThread loadingThread;
    private KeyToMidiAndNoteMap map;
    private Scene mainScene, loadingScene;
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

            initializeLoadingScene();
            map = new KeyToMidiAndNoteMap("src/piano/map.csv", cap);
            window.setResizable(false);
            loadingScene.setOnKeyPressed(ke -> {
                if (ke.getCode() == KeyCode.ENTER && loadingThread.finished()) {
                    window.setScene(mainScene);
                    Composition comp = new Composition(map);
                    try {
                        comp.loadFromFile("src/piano/song2.txt");
                        comp.exportToTXT("src/piano/out.txt");
                        comp.exportToMIDI("src/piano/outmidi.mid");
                    } catch (IOException | FileException fe) {
                        fe.printStackTrace();
                    }
                    compViewer.setComposition(comp);

                    mainScene.setOnKeyPressed(e -> piano.keyPressed(e));
                    mainScene.setOnKeyReleased(e -> piano.keyReleased(e));
                    mainScene.setOnMousePressed(me -> piano.mousePressed(me));
                    mainScene.setOnMouseReleased(me -> piano.mouseReleased(me));
                    window.show();

                    window.setOnCloseRequest(e -> {
                        handler.stopWorking();
                        pressedReceiver.stopWorking();
                        releasedReceiver.stopWorking();
                    });
                }
            });
            window.setScene(loadingScene);
            window.show();
            loadingThread.start();
            map.start();

            mainScene = initializeMainScene();
            map.join();
            if (map.errorOccurred())
                throw new FileException("Incorrect format for input map file");


        } catch(FileException fe) {
            System.out.println(fe.getMessage());
        } catch (InterruptedException | MidiUnavailableException ie) {
            ie.printStackTrace();
        }

    }

    private void initializeLoadingScene() throws FileException {
        Image image;
        try {
            image = new Image(new FileInputStream("src/piano/HoP_Header.png"));
        } catch (FileNotFoundException e) {
            throw new FileException("HoP_Header.png not found.");
        }
        VBox pane = new VBox();
        AnchorPane anchor = new AnchorPane();
        ImageView imageView = new ImageView(image);
        ProgressBar ind = new ProgressBar(0);
        Label enterTextLabel = new Label("PRESS ENTER TO CONTINUE");
        enterTextLabel.setStyle("-fx-background-color: #FFFFFF");
        enterTextLabel.setStyle("-fx-font-size: 24");
        enterTextLabel.setLayoutX(361);
        enterTextLabel.setLayoutY(432);
        enterTextLabel.setVisible(false);
        imageView.setLayoutX(121);
        imageView.setLayoutY(98);
        imageView.setFitWidth(782);
        imageView.setFitHeight(235);
        ind.setLayoutX(155);
        ind.setLayoutY(431);
        ind.setPrefSize(700, 30);
        anchor.getChildren().addAll(imageView, ind, enterTextLabel);
        pane.setStyle("-fx-background-color: #FFFFFF");
        pane.getChildren().addAll(anchor);
        loadingScene = new Scene(pane, SCREEENWIDTH, SCREENHEIGHT);
        loadingThread = new LoadingThread(ind, enterTextLabel);
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

        return new Scene(mainPane, SCREEENWIDTH, SCREENHEIGHT);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
