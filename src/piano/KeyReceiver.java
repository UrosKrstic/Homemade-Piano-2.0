package piano;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KeyReceiver extends Thread {
    private final static int BACKOFF_TIME = 40;
    private ReceivedNoteHandler handler;
    private ArrayList<PianoKey> receivedKeys = new ArrayList<>();
    private boolean working = true;
    private boolean forPressed;

    public KeyReceiver(ReceivedNoteHandler handler, boolean forPressed) {
        this.forPressed = forPressed;
        this.handler = handler;
        start();
    }

    public synchronized void addKey(PianoKey key) {
        for (PianoKey recKey : receivedKeys) {
            if (recKey == key)
                return;
        }
        receivedKeys.add(key);
        notifyAll();
    }

    public synchronized void stopWorking() { working = false; }

    public void run() {
        while(working) {
            try {
                synchronized (this) { while(receivedKeys.size() == 0) wait(); }
                sleep(BACKOFF_TIME);
                ArrayList<PianoKey> tmpKeys = receivedKeys;
                receivedKeys = new ArrayList<>();
                if (forPressed) {
                    handler.addPressedKey(tmpKeys);
                }
                else {
                    handler.addReleasedKey(tmpKeys);
                }
            }
            catch(InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }




}
