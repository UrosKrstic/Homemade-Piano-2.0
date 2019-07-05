package piano;

import java.util.ArrayList;

public class AutoPlayer extends Thread {
    private static final int EIGHT_NOTE_PLAYTIME = 120;
    private CompositionViewer compViewer;
    private boolean working = false;
    private Main mainProgram;
    private ReceivedNoteHandler handler;
    private boolean isRecording;
    private ArrayList<MusicSymbol> recordedSymbols = new ArrayList<>();

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

    public synchronized void setRecording(boolean b) { isRecording = b; }
    public synchronized boolean isRecording() { return isRecording; }

    public synchronized void pausePlaying() {
        working = false;
    }

    public synchronized void stopPlaying() {
        working = false;
        compViewer.resetViewer();
        compViewer.drawComp();
        if (isRecording) {
            handler.setRecordingSymbols(recordedSymbols);
            handler.fileWindow();
        }
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
                    if (isRecording) recordedSymbols.add(symbol);
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
                    mainProgram.stopAction();

                }
            }
            catch (InterruptedException ie) {break;}
        }
        System.out.println("Auto-player rip");
    }
}
