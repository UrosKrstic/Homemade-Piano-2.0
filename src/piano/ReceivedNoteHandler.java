package piano;

import exceptions.FileException;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceivedNoteHandler extends Thread {

    private static final int EIGHT_NOTE_PLAYTIME = 130;
    private static final int DEFAULT_INSTRUMENT = 1;
    private Stage window;
    private MidiChannel channel;
    private Piano piano;
    private boolean isRecording = false, isFirstNote = true, working = true, playingPause, blocked = false;
    private CompositionViewer compViewer;
    private ConcurrentLinkedQueue<ArrayList<PianoKey>> pressedKeys = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<ArrayList<PianoKey>> releasedKeys = new ConcurrentLinkedQueue<>();
    private ArrayList<MusicSymbol> recordingSymbols = new ArrayList<>();

    public synchronized void addPressedKey(ArrayList<PianoKey> keys) {
        pressedKeys.offer(keys);
        notifyAll();
        if (playingPause) {
            interrupt();
        }
    }

    public synchronized void addReleasedKey(ArrayList<PianoKey> keys) {
        releasedKeys.offer(keys);
        interrupt();
    }

    public ReceivedNoteHandler(Piano _piano, int instrument) throws MidiUnavailableException {
        piano = _piano;
        channel = getChannel(instrument);
        start();
    }
    public void setWindow(Stage _window) { window = _window; }

    public synchronized void stopWorking() {
        working = false;
    }

    public synchronized void startRecording() {
        isRecording = true;
    }

    public synchronized void pauseRecording() { isRecording = false; }

    private void fileWindow() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL); // BLOCKS OTHER WINDOWS USERS EVENTS
        stage.setTitle("File Export");
        stage.setWidth(300);
        stage.setMinWidth(300);
        stage.setOnCloseRequest(e -> {
            recordingSymbols.clear();
        });
        Label label = new Label("Do you wish to save your recording?");
        Button yes = new Button("yes");
        Button no = new Button("no");
        yes.setOnAction(e -> {
            ChoiceBox<String> choiceBox = new ChoiceBox<>();
            choiceBox.getItems().addAll("Export to TXT format", "Export to MIDI format");
            choiceBox.setValue("Export to TXT format");
            Button cont = new Button("Continue");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export composition to file");
            Composition comp = new Composition(recordingSymbols);
            recordingSymbols = new ArrayList<>();
            cont.setOnAction(ae -> {
                if (choiceBox.getValue().equals("Export to TXT format")) {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        try {
                            comp.exportToTXT(file.toString());
                        } catch (IOException ie) {
                            AlertBox.display("Error", "Error with opening file:" + file.toString());
                        }
                    }
                }
                else {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MIDI files", "*.mid"));
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        try {
                            comp.exportToMIDI(file.toString());
                        } catch (IOException ie) {
                            AlertBox.display("Error", "Error with opening file: " + file.toString());
                        }
                    }
                }
                stage.close();
            });
            VBox layout2 = new VBox(30);
            layout2.getChildren().addAll(choiceBox, cont);
            layout2.setAlignment(Pos.CENTER);
            Scene contScene = new Scene(layout2, 300, 300);
            stage.setScene(contScene);
        });
        no.setOnAction(e -> {
            recordingSymbols.clear();
            stage.close();
        });
        FlowPane pane = new FlowPane();
        pane.setAlignment(Pos.CENTER);
        pane.getChildren().addAll(yes, no);
        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, pane);
        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(layout, 300, 300);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public synchronized void stopRecording() {
        isRecording = false;
        isFirstNote = true;
        for (int i = recordingSymbols.size() - 1; i >= 0; i--) {
            if (recordingSymbols.get(i) instanceof Note || recordingSymbols.get(i) instanceof Chord)
                break;
            else {
                recordingSymbols.remove(i);
            }
        }
        fileWindow();
    }

    public void setCompositionViewer(CompositionViewer compViewer) { this.compViewer = compViewer; }

    private void updateStatus(ArrayList<PianoKey> keys, boolean isQuarterNote) {
        if (keys.size() == 1) {
            Note note = keys.get(0).getNote();
            if (isQuarterNote) note.setDuration(2);
            else note.setDuration(1);
            compViewer.checkCurrentSymbol(note);
            System.out.println(note);
            if (isRecording) {
                recordingSymbols.add(note);
            }
        }
        else {
            ArrayList<Note> notes = new ArrayList<>(keys.size());
            for(PianoKey key : keys) {
                key.getNote().setDuration(2);
                notes.add(key.getNote());
            }
            Chord chord = new Chord(notes);
            System.out.println(chord);
            compViewer.checkCurrentSymbol(chord);
            if (isRecording) {
                recordingSymbols.add(chord);
            }
        }
    }

    public ArrayList<PianoKey> locatePianoKeys(ArrayList<Note> notes) {
        ArrayList<PianoKey> keys = new ArrayList<>();
        ArrayList<PianoKey> pianoCheckUpKeys = piano.getBlackKeys();
        pianoCheckUpKeys.addAll(piano.getWhiteKeys());
        for (Note note : notes) {
            for (PianoKey key : pianoCheckUpKeys) {
                if (note.getTextCode() == key.getNote().getTextCode()) {
                    keys.add(key);
                    break;
                }
            }
        }
        return keys;
    }

    public void highlightKeys(ArrayList<PianoKey> keys) {
        for (PianoKey key : keys) {
            Rectangle rect = key.getKeyRect();
            if (key.getNote().isSharp()) {
                GraphicsContext gc = piano.getBlackKeysFront().getGraphicsContext2D();
                gc.setFill(Color.RED);
                gc.setStroke(Color.WHITE);
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeText(key.getNote().getTextCode()+ "", (int)(rect.getX() + rect.getWidth() * 0.24), (int)(0.78 * rect.getHeight()));
            }
            else {
                GraphicsContext gc = piano.getWhiteKeysBack().getGraphicsContext2D();
                gc.setFill(Color.RED);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeText(key.getNote().getTextCode()+ "", (int)(rect.getX() + rect.getWidth() * 0.4), (int)(0.84 * rect.getHeight()));
            }
            play(key.getNote().getMIDIcode());
        }
    }

    public void unhighlightKeys(ArrayList<PianoKey> keys) {
        for (PianoKey key : keys) {
            Rectangle rect = key.getKeyRect();
            if (key.getNote().isSharp()) {
                GraphicsContext gc = piano.getBlackKeysFront().getGraphicsContext2D();
                gc.setFill(Color.BLACK);
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeText(key.getNote().getTextCode()+ "", (int)(rect.getX() + rect.getWidth() * 0.24), (int)(0.78 * rect.getHeight()));
            }
            else {
                GraphicsContext gc = piano.getWhiteKeysBack().getGraphicsContext2D();
                gc.setFill(Color.WHITE);
                gc.setStroke(Color.BLACK);
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.setLineWidth(1.5);
                gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeText(key.getNote().getTextCode()+ "", (int)(rect.getX() + rect.getWidth() * 0.4), (int)(0.84 * rect.getHeight()));
            }
            release(key.getNote().getMIDIcode());
        }
    }

    public synchronized void blockHandler() { blocked = true; }
    public synchronized void unblockHandler() { blocked = false; notifyAll();}

    public void run() {
        while (working) {
            if (isRecording && isFirstNote) {
               try { synchronized (this) { while (pressedKeys.size() == 0) wait(); } }
               catch (InterruptedException ie) {}
                isFirstNote = false;
            }
            //added for blocking
            if (blocked) {
                try {
                    synchronized (this) {
                        while (blocked) wait();
                    }
                } catch (InterruptedException ie) {}
                pressedKeys.clear();
                releasedKeys.clear();
                continue;
            }

            ArrayList<PianoKey> keys = pressedKeys.poll();
            playingPause = keys == null || keys.size() == 0;
            if (!playingPause) {
               highlightKeys(keys);
            }
            boolean isQuarterNote = true;
            try {
                sleep(EIGHT_NOTE_PLAYTIME);
            }
            catch (InterruptedException ie) {
                if (playingPause) continue;
                else {
                    ArrayList<PianoKey> released = releasedKeys.peek();
                    if (released != null) {
                        releasedKeys.poll();
                        updateStatus(keys, false);
                        unhighlightKeys(keys);
                        continue;
                    }
                }
            }

            if (!playingPause) {
                ArrayList<PianoKey> released = releasedKeys.peek();
                isQuarterNote = released == null;
            }
            else {
                Pause pause = new Pause(1);
                if (compViewer.checkCurrentSymbol(pause))
                    continue;
            }
            if (isQuarterNote) {
                try {
                    sleep(EIGHT_NOTE_PLAYTIME + EIGHT_NOTE_PLAYTIME / 3);
                }
                catch (InterruptedException ie) {
                    if (playingPause) {
                        Pause pause = new Pause(1);
                        compViewer.checkCurrentSymbol(pause);
                        if (isRecording) {
                            recordingSymbols.add(pause);
                        }
                        continue;
                    }
                    else {
                        ArrayList<PianoKey> released = releasedKeys.peek();
                        if (released != null) {
                            releasedKeys.poll();
                            updateStatus(keys, true);
                            unhighlightKeys(keys);
                            continue;
                        }
                    }
                }
            }
            releasedKeys.poll();
            if (playingPause) {
                Pause pause = new Pause(2);
                compViewer.checkCurrentSymbol(pause);
                if (isRecording) {
                    recordingSymbols.add(pause);
                }
            }
            else {
                updateStatus(keys, isQuarterNote);
                unhighlightKeys(keys);
            }
        }
    }

    public ReceivedNoteHandler(Piano _piano) throws MidiUnavailableException {
        this(_piano, DEFAULT_INSTRUMENT);
    }

    public void play(final int note) {
        channel.noteOn(note, 50);
    }

    public void release(final int note) {
        channel.noteOff(note, 50);
    }

    private static MidiChannel getChannel(int instrument)
            throws MidiUnavailableException {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        return synthesizer.getChannels()[instrument];
    }
    public static void closeSynth() throws MidiUnavailableException {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.close();
    }
}
