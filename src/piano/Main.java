package piano;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
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
    private AutoPlayer autoPlayer;
    private ReceivedNoteHandler handler;
    private KeyReceiver pressedReceiver, releasedReceiver;
    private boolean isAutoPlaying = false;
    private MenuItem record = new MenuItem("Record"), play = new MenuItem("Play");
    private MenuItem pause = new MenuItem("Pause"), stop = new MenuItem("Stop");
    private Button recordButton = new Button(""), playButton = new Button("");
    private Button pauseButton = new Button(""), stopButton = new Button("");
    private QuoteGenerator quoteGenerator;
    private ImageView playImg, recordImg, pauseImg, stopImg;

    private void openSettings() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL); // BLOCKS OTHER WINDOWS USERS EVENTS
        stage.setTitle("Settings");
        stage.setWidth(400);
        stage.setHeight(350);
        Label settings = new Label("Settings");
        settings.setStyle("-fx-font-size: 26");
        Label filler = new Label();
        Label filler2 = new Label();
        CheckBox tags = new CheckBox("Key assist");
        CheckBox pitchDisplayed = new CheckBox("Note pitch display");
        CheckBox recordWhileAutoPlaying = new CheckBox("Record while auto-playing");
        tags.setSelected(piano.isHelperON());
        pitchDisplayed.setSelected(compViewer.isPitchShown());
        recordWhileAutoPlaying.setSelected(autoPlayer.isRecording());
        Button apply = new Button("Apply");
        apply.setOnAction(ae -> {
            piano.setHelper(tags.isSelected());
            compViewer.showPitch(pitchDisplayed.isSelected());
            autoPlayer.setRecording(recordWhileAutoPlaying.isSelected());
            stage.close();
        });

        VBox layout = new VBox(10);
        layout.setId("pane");
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(settings, filler, tags, pitchDisplayed, recordWhileAutoPlaying, filler2, apply);


        Scene scene = new Scene(layout, 400, 350);
        scene.getStylesheets().add("piano/alertStyle.css");
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void shutdownProgram() {
        if (handler != null)
            handler.stopWorking();
        try {
            ReceivedNoteHandler.closeSynth();
        }
        catch(MidiUnavailableException me) {
            AlertBox.display("Error", "Failed to get synthesizer and close the MIDI subsystem.");
        }
        if (pressedReceiver != null)
            pressedReceiver.stopWorking();
        if (releasedReceiver != null)
            releasedReceiver.stopWorking();
        if (autoPlayer != null)
            autoPlayer.stopWorking();
        if (quoteGenerator != null)
            quoteGenerator.stopWorking();
        window.close();
    }

    private void questioningShutdown() {
        boolean answer = ConfirmBox.display("Exit", "Do you wish to exit?");
        if (answer) {
            shutdownProgram();
        }
    }

    private void blockInput() {
        pressedReceiver.blockReceiver();
        releasedReceiver.blockReceiver();
        handler.blockHandler();
    }

    private void unblockInput() {
        pressedReceiver.unblockReceiver();
        releasedReceiver.unblockReceiver();
        handler.unblockHandler();
    }

    public void stopAutoPlaying() {
        isAutoPlaying = false;
        unblockInput();
        record.setDisable(false);
        play.setDisable(false);
        pause.setDisable(true);
        stop.setDisable(true);
    }

    private void recordAction() {
        record.setDisable(true);
        pause.setDisable(false);
        stop.setDisable(false);
        play.setDisable(true);
        recordButton.setDisable(true);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        playButton.setDisable(true);
        handler.startRecording();
    }

    private void playAction() {
        if (compViewer.hasComposition()) {
            blockInput();
            record.setDisable(true);
            play.setDisable(false);
            pause.setDisable(false);
            stop.setDisable(false);
            recordButton.setDisable(true);
            pauseButton.setDisable(false);
            playButton.setDisable(false);
            stopButton.setDisable(false);
            isAutoPlaying = true;
            autoPlayer.startPlaying();
        }
        else {
            AlertBox.display("Error", "No composition loaded");
        }
    }

    private void pauseAction() {
        if (isAutoPlaying) {
            autoPlayer.pausePlaying();
        }
        else {
            handler.pauseRecording();
        }
        pause.setDisable(true);
        pauseButton.setDisable(true);
    }

    public void stopAction() {
        if (isAutoPlaying) {
            autoPlayer.stopPlaying();
            isAutoPlaying = false;
            unblockInput();
        }
        else {
            handler.stopRecording();
        }
        record.setDisable(false);
        play.setDisable(false);
        pause.setDisable(true);
        stop.setDisable(true);
        recordButton.setDisable(false);
        playButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
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
                    try {
                        mainScene = initializeMainScene();
                        mainScene.getStylesheets().add("piano/mainStyle.css");
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
                    catch (MidiUnavailableException ie) {
                        AlertBox.display("Fatal error", "Midi Unavailable - Fatal Error");
                        shutdownProgram();
                    }
                    catch(FileException fe) {
                            AlertBox.display("Fatal error", fe.getMessage());
                            shutdownProgram();
                    }

                }
            });
            window.setScene(loadingScene);
            window.show();
            loadingThread.start();
            map.start();
            if (map.errorOccurred())
                throw new FileException("Incorrect format for input map file");
        } catch(FileException fe) {
            AlertBox.display("Fatal error", fe.getMessage());
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
        enterTextLabel.setStyle("-fx-background-color: white");
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
        Menu fileMenu = new Menu("_File");
        MenuItem openFile = new MenuItem("Open File...");
        openFile.setOnAction(e -> {
            File selectedFile = fileChooser.showOpenDialog(window);
            Composition comp = null;
            try {
                if (selectedFile != null) {
                    comp = new Composition(map);
                    comp.loadFromFile(selectedFile.toString());
                }
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
            questioningShutdown();
        });
        fileMenu.getItems().add(openFile);
        fileMenu.getItems().add(new SeparatorMenuItem());
        fileMenu.getItems().add(exitProgram);

        Menu editMenu = new Menu("_Edit");
        record.setOnAction(e -> recordAction());
        recordButton.setOnAction(e -> recordAction());
        play.setOnAction(e -> playAction());
        playButton.setOnAction(e -> playAction());
        pause.setDisable(true);
        pause.setOnAction(e -> pauseAction());
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> pauseAction());
        stop.setDisable(true);
        stop.setOnAction(e -> stopAction());
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopAction());
        MenuItem resetViewer = new MenuItem("Reset viewer");
        resetViewer.setOnAction(ae -> compViewer.resetViewer());
        editMenu.getItems().addAll(record, play, pause, stop, new SeparatorMenuItem(), resetViewer);

        Menu settingsMenu = new Menu("_Settings");
        MenuItem open = new MenuItem("Open");
        open.setOnAction(ae -> openSettings());
        settingsMenu.getItems().add(open);

        menuBar.getMenus().addAll(fileMenu, editMenu, settingsMenu);
    }

    private Scene initializeMainScene() throws MidiUnavailableException, FileException {
        BorderPane mainPane = new BorderPane();
        mainPane.setId("pane");
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text file", "*.txt"));
        initializeMenuBar();
        mainPane.setTop(menuBar);
        int pianoX = 81, pianoY = 290, pianoW = 882, pianoH = 211;
        Image image, img1, img2, img3, img4;
        try {
            image = new Image(new FileInputStream("src/piano/pianoBG_7.png"));
            img1 = new Image(new FileInputStream("src/piano/recordBttn.png"));
            img2 = new Image(new FileInputStream("src/piano/playBttn.png"));
            img3 = new Image(new FileInputStream("src/piano/pauseBttn.png"));
            img4 = new Image(new FileInputStream("src/piano/stopBttn.png"));
        } catch (FileNotFoundException e) {
            throw new FileException("Resources failed to load.");
        }
        ImageView imgView = new ImageView(image);
        // 43 195 954 370
        imgView.setLayoutX(0.530864 * pianoX);
        imgView.setLayoutY(0.672413 * pianoY);
        imgView.setFitWidth(1.081633 * pianoW);
        imgView.setFitHeight(1.753554 * pianoH);

        AnchorPane center = new AnchorPane();

        FlowPane upperCenter = new FlowPane();
        upperCenter.setAlignment(Pos.CENTER);
        upperCenter.setHgap(10);
        recordImg = new ImageView(img1);
        playImg = new ImageView(img2);
        pauseImg = new ImageView(img3);
        stopImg = new ImageView(img4);
        int width = 50, height = 40;
        recordImg.setFitWidth(width);
        recordImg.setFitHeight(height);
        playImg.setFitWidth(width);
        playImg.setFitHeight(height);
        pauseImg.setFitWidth(width);
        pauseImg.setFitHeight(height);
        stopImg.setFitWidth(width);
        stopImg.setFitHeight(height);
        recordButton.setGraphic(recordImg);
        playButton.setGraphic(playImg);
        pauseButton.setGraphic(pauseImg);
        stopButton.setGraphic(stopImg);
        upperCenter.getChildren().addAll(recordButton, playButton, pauseButton, stopButton);
        //318 234
        upperCenter.setLayoutX(3.876543 * pianoX);
        upperCenter.setLayoutY(0.806896 * pianoY);

        int compViewYoffset = 0;
        compViewer = new CompositionViewer(81, 36 + compViewYoffset, 882, 142);
        compViewer.setStyle("-fx-border-width: 5");
        compViewer.setStyle("-fx-border-color: #000000");
        piano = new Piano(map, pianoX, pianoY,  pianoW, pianoH);
        handler = new ReceivedNoteHandler(piano);
        handler.setCompositionViewer(compViewer);
        handler.setWindow(window);
        pressedReceiver = new KeyReceiver(handler, true);
        releasedReceiver = new KeyReceiver(handler, false);

        piano.setHandler(handler);
        piano.setPressedReceiver(pressedReceiver);
        piano.setReleasedReceiver(releasedReceiver);

        autoPlayer = new AutoPlayer(compViewer, handler, this);

        Label compViewerLabel = new Label("Composition Viewer");
        compViewerLabel.setId("viewCompLabel");
        compViewerLabel.setLayoutX(410);
        compViewerLabel.setLayoutY(compViewYoffset);

        AnchorPane bottom = new AnchorPane();
        Label quoteLabel = new Label("");
        quoteLabel.setLayoutX(320);
        quoteLabel.setLayoutY(10);
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(400);
        quoteLabel.setMaxHeight(76);
        quoteLabel.setId("quote");
        quoteLabel.setTextAlignment(TextAlignment.CENTER);
        Label authorLabel = new Label("");
        authorLabel.setLayoutX(680);
        authorLabel.setLayoutY(59);
        authorLabel.setMaxWidth(200);
        authorLabel.setMaxHeight(17);
        authorLabel.setId("author");
        //516
        bottom.setLayoutY(1.77931 * pianoY);
        bottom.getChildren().addAll(quoteLabel, authorLabel);
        quoteGenerator = new QuoteGenerator(quoteLabel, authorLabel);

        center.getChildren().addAll(compViewerLabel, imgView, compViewer, piano, upperCenter, bottom);
        mainPane.setCenter(center);



        return new Scene(mainPane, SCREEENWIDTH, SCREENHEIGHT);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
