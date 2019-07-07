package piano;

import java.util.ArrayList;

public class KeyReceiver extends Thread {
    private final static int BACKOFF_TIME = 50;
    private static boolean[] IS_PRESSED = new boolean[256];
    private ReceivedNoteHandler handler;
    private ArrayList<PianoKey> receivedKeys = new ArrayList<>();
    private boolean working = true, blocked = false;
    private boolean forPressed;

    public KeyReceiver(ReceivedNoteHandler handler, boolean forPressed) {
        this.forPressed = forPressed;
        this.handler = handler;
        start();
    }

    public synchronized void addKey(PianoKey key) {
        if (forPressed) {
            if (!IS_PRESSED[key.getNote().getTextCode()]) {
                IS_PRESSED[key.getNote().getTextCode()] = true;
                receivedKeys.add(key);
                notifyAll();
            }
        }
        else {
            IS_PRESSED[key.getNote().getTextCode()] = false;
            receivedKeys.add(key);
            notifyAll();
        }
    }

    public synchronized void stopWorking() { working = false; interrupt(); }

    public synchronized void blockReceiver() { blocked = true; }
    public synchronized void unblockReceiver() { blocked = false; notifyAll();}

    public void run() {
        while(!interrupted()) {
            try {

                synchronized (this) { while(receivedKeys.size() == 0) wait(); }
                synchronized (this) {
                    if (blocked) {
                        while (blocked && working) wait();
                        receivedKeys.clear();
                    }
                }
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
            catch(InterruptedException ie) {interrupt();}
        }
        System.out.println("Receiver rip");
    }




}
