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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.midi.MidiUnavailableException;
import java.io.File;
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
    private FileChooser fileChooser = new FileChooser();
    private Piano piano;
    private CompositionViewer compViewer;
    private ReceivedNoteHandler handler;
    private KeyReceiver pressedReceiver, releasedReceiver;

    private void shutdownProgram() {
        handler.stopWorking();
        pressedReceiver.stopWorking();
        releasedReceiver.stopWorking();
        window.close();
    }

    private void questioningShutdown() {
        boolean answer = ConfirmBox.display("Don't leave me :(", "Do you wish to exit?");
        if (answer) {
            shutdownProgram();
        }
    }

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
                    mainScene.setOnKeyPressed(e -> piano.keyPressed(e));
                    mainScene.setOnKeyReleased(e -> piano.keyReleased(e));
                    mainScene.setOnMousePressed(me -> piano.mousePressed(me));
                    mainScene.setOnMouseReleased(me -> piano.mouseReleased(me));
                    window.show();
                    window.setOnCloseRequest(e -> {
                        e.consume();
                        questioningShutdown();
                    });
                }
            });
            window.setScene(loadingScene);
            window.show();
            loadingThread.start();
            map.start();
            mainScene = initializeMainScene();
            if (map.errorOccurred())
                throw new FileException("Incorrect format for input map file");
        } catch(FileException fe) {
            AlertBox.display("Fatal error", fe.getMessage());
            shutdownProgram();
        } catch (MidiUnavailableException ie) {
            AlertBox.display("Fatal error", "Midi Unavailable - Fatal Error");
            shutdownProgram();
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
        MenuItem openFile = new MenuItem("Open File...");
        openFile.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(window);
            Composition comp = new Composition(map);
            try {
                comp.loadFromFile(selectedFile.toString());
                //comp.exportToTXT("src/piano/out.txt");
                //comp.exportToMIDI("src/piano/outmidi.mid");
            } catch (IOException fe) {
                AlertBox.display("Error", "Error with opening file" + selectedFile.toString());
            }
            catch (FileException fe) {
                AlertBox.display("Error", fe.getMessage());
            }
            compViewer.setComposition(comp);
        });
        MenuItem exitProgram = new MenuItem("Exit...");
        exitProgram.setOnAction(e -> {
            e.consume();
            questioningShutdown();
        });
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(new SeparatorMenuItem());
        fileMenu.getItems().add(exitProgram);

        Menu editMenu = new Menu("Edit");
        MenuItem record = new MenuItem("Record");
        record.setOnAction(e -> handler.startRecording());
        MenuItem play = new MenuItem("Play");
        MenuItem pause = new MenuItem("Pause");
        pause.setOnAction(e -> {
            handler.pauseRecording();
        });
        MenuItem stop = new MenuItem("Stop");
        stop.setOnAction(e -> {
            handler.stopRecording();
        });
        editMenu.getItems().addAll(record, play, pause, stop);

        Menu settingsMenu = new Menu("Settings");
        Menu helpMenu = new Menu("Help");

        menuBar.getMenus().addAll(fileMenu, editMenu, settingsMenu, helpMenu);
    }

    private Scene initializeMainScene() throws MidiUnavailableException {
        BorderPane mainPane = new BorderPane();

        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text file", "*.txt"));
        initializeMenuBar();
        mainPane.setTop(menuBar);

        AnchorPane center = new AnchorPane();
        compViewer = new CompositionViewer(81, 36, 882, 142);
        piano = new Piano(map, 81, 223,  882, 211);
        handler = new ReceivedNoteHandler(piano);
        handler.setCompositionViewer(compViewer);
        handler.setWindow(window);
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
