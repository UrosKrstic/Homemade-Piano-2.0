package piano;

import exceptions.FileException;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class LoadingThread extends Thread {
    private ProgressBar ind;
    private boolean finished = false;
    private Label enterLabel;

    public LoadingThread(ProgressBar _ind, Label _enterLabel) {
        ind = _ind;
        enterLabel = _enterLabel;
    }
    public synchronized boolean finished() { return finished; }
    @Override
    public void run() {
        try {
            for(int i = 0; i < 20; i++) {
                ind.setProgress(ind.getProgress() + 0.05F);
                Thread.sleep(150);
            }
        }
        catch(InterruptedException ie) {}
        finally { finished = true; ind.setVisible(false); enterLabel.setVisible(true); }
    }
}
