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

    private static final int BACKOFF_TIME = 30, NOTE_SLEEP_TIME = 120;
    private static final int DEFAULT_INSTRUMENT = 1;
    private MidiChannel channel;
    private Piano piano;
    private boolean isRecording = false, isFirstNote = true, working = true;
    private ConcurrentLinkedQueue<PianoKey> pressedKeys = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<PianoKey> releasedKeys = new ConcurrentLinkedQueue<>();

    public synchronized void addPressedKey(PianoKey key) {
        pressedKeys.offer(key);
        notifyAll();
    }

    public void addReleasedKey(PianoKey key) {
        releasedKeys.offer(key);
    }

    public ReceivedNoteHandler(Piano _piano, int instrument) throws MidiUnavailableException {
        piano = _piano;
        channel = getChannel(instrument);
        start();
    }

    public void stopWorking() {
        working = false;
    }

    public void startRecordig() {
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
        isFirstNote = true;
    }

    public void run() {
        while (working) {
            try {
                if (!isRecording || isFirstNote) {
                    synchronized (this) {
                        while (pressedKeys.size() == 0) wait();
                        if (isRecording) isFirstNote = false;
                    }
                }
                releasedKeys.clear();
                sleep(BACKOFF_TIME);
                ArrayList<PianoKey> keys = new ArrayList<>(pressedKeys.size());
                while (pressedKeys.size() != 0) {
                    PianoKey key = pressedKeys.poll();
                    Rectangle rect = key.getKeyRect();
                    keys.add(key);
                    GraphicsContext gc = key.getNote().isSharp() ?
                            piano.getBlackKeysFront().getGraphicsContext2D() : piano.getWhiteKeysBack().getGraphicsContext2D();
                    gc.setFill(Color.RED);
                    gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                    play(key.getNote().getMIDIcode());
                }
                sleep(NOTE_SLEEP_TIME);
                boolean shortNote = releasedKeys.size() == keys.size();
                if (!shortNote) sleep(NOTE_SLEEP_TIME);
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
                if (isRecording) {
                    //TODO: RECORD THE NOTE/CHORD/PAUSE
                }
                pressedKeys.clear();
                releasedKeys.clear();
            }
            catch(InterruptedException ie) {ie.printStackTrace();}
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
