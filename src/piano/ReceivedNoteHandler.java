package piano;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceivedNoteHandler extends Thread {

    private static final int EIGHT_NOTE_PLAYTIME = 130;
    private static final int DEFAULT_INSTRUMENT = 1;
    private MidiChannel channel;
    private Piano piano;
    private boolean isRecording = false, isFirstNote = true, working = true, playingPause;
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

    public synchronized void stopWorking() {
        working = false;
    }

    public synchronized void startRecordig() {
        isRecording = true;
    }

    public synchronized void stopRecording() {
        isRecording = false;
        isFirstNote = true;
        Composition comp = new Composition(recordingSymbols);
        //TODO: export MIDI or TXT
        recordingSymbols = new ArrayList<>();
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

    private void unhighlightKeys(ArrayList<PianoKey> keys) {
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
                gc.setLineWidth(1.5);
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.strokeText(key.getNote().getTextCode()+ "", (int)(rect.getX() + rect.getWidth() * 0.4), (int)(0.84 * rect.getHeight()));
            }
            release(key.getNote().getMIDIcode());
        }
    }

    public void run() {
        while (working) {
            try {
                if (isRecording && isFirstNote) {
                    synchronized (this) { while (pressedKeys.size() == 0) wait(); }
                }
                ArrayList<PianoKey> keys = pressedKeys.poll();
                playingPause = keys == null || keys.size() == 0;
                if (!playingPause) {
                    for (PianoKey key : keys) {
                        Rectangle rect = key.getKeyRect();
                        GraphicsContext gc = key.getNote().isSharp() ?
                                piano.getBlackKeysFront().getGraphicsContext2D() : piano.getWhiteKeysBack().getGraphicsContext2D();
                        gc.setFill(Color.RED);
                        gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                        play(key.getNote().getMIDIcode());
                    }
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
            catch (InterruptedException ie) {ie.printStackTrace();}
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
}
