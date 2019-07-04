package piano;

import javax.sound.midi.MidiChannel;
import java.util.ArrayList;

public class AutoPlayer extends Thread {
    private static final int EIGHT_NOTE_PLAYTIME = 130;
    private CompositionViewer compViewer;
    private boolean working = false;
    private Main mainProgram;
    private ReceivedNoteHandler handler;

    public AutoPlayer(CompositionViewer _compViewer, ReceivedNoteHandler _handler, Main _mainProgram) {
        compViewer = _compViewer;
        handler = _handler;
        mainProgram = _mainProgram;
        start();
    }

    public synchronized void startPlaying() {
        working = true;
        notifyAll();
    }

    public synchronized void stopPlaying() {
        working = false;
        compViewer.resetViewer();
        compViewer.drawComp();
    }

    public synchronized void stopWorking() {
        interrupt();
    }

    private void sleep(MusicSymbol symbol) throws InterruptedException {
        if (symbol.getDuration() == 1) {
            sleep(EIGHT_NOTE_PLAYTIME);
        }
        else {
            sleep(EIGHT_NOTE_PLAYTIME * 2);
        }
    }

    public void run() {
        while(!interrupted()) {
            try {
                synchronized (this) {
                    while (!working) wait();
                }
                if (!compViewer.reachedEnd()) {
                    MusicSymbol symbol = compViewer.getCurrentSymbol();
                    if (symbol instanceof Pause) {
                       sleep(symbol);
                    }
                    else if (symbol instanceof Note) {
                        Note note = (Note)symbol;
                        ArrayList<Note> notes = new ArrayList<>();
                        notes.add(note);
                        ArrayList<PianoKey> keys = handler.locatePianoKeys(notes);
                        handler.highlightKeys(keys);
                        sleep(symbol);
                        handler.unhighlightKeys(keys);
                    }
                    else  {
                        Chord chord = (Chord)symbol;
                        ArrayList<Note> notes = chord.getNotes();
                        ArrayList<PianoKey> keys = handler.locatePianoKeys(notes);
                        handler.highlightKeys(keys);
                        sleep(symbol);
                        handler.unhighlightKeys(keys);
                    }
                    compViewer.setCurrentSymbol();
                    compViewer.drawComp();
                }
                else {
                    stopPlaying();
                    mainProgram.stopAutoPlaying();

                }
            }
            catch (InterruptedException ie) {break;}
        }
    }
}
