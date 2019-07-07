package piano;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.util.Random;

public class QuoteGenerator extends Thread{
    private final String[] quotes = {
            "Music is a moral law. It gives soul to the universe, wings to the mind," +
                    " flight to the imagination, and charm and gaiety to life and to everything.",
            "Music, at its essence, is what gives us memories. And the longer" +
                    " a song has existed in our lives, the more memories we have of it.",
            "Music doesn't lie. If there is something to be changed" +
                    " in this world, then it can only happen through music.",
            "Music is a language that doesn’t speak in particular words. " +
                    "It speaks in emotions, and if it’s in the bones, it’s in the bones.",
            "And those who were seen dancing were thought to be " +
                    "insane by those who could not hear the music.",
            "If music be the food of love, play on, Give me excess of it; " +
                    "that surfeiting, The appetite may sicken, and so die."
    };
    private final String[] authors = { "Plato", "Stevie Wonder", "Jimi Hendrix", "Keith Richards", "Friedrich Nietzsche", "William Shakespeare" };
    private Label quoteLabel;
    private Label authorLabel;
    private Random generator = new Random(System.currentTimeMillis());

    public synchronized void stopWorking() {
        interrupt();
    }

    public QuoteGenerator(Label _quoteLabel, Label _authorLabel) {
        quoteLabel = _quoteLabel;
        authorLabel = _authorLabel;
        start();
    }

    public void run() {
        try {
            while(!interrupted()) {
                int ind = generator.nextInt(quotes.length);
                Platform.runLater(() -> {
                    quoteLabel.setText("\"" + quotes[ind] + "\"");
                    authorLabel.setText("- " + authors[ind]);
                });
                sleep(60_000);
            }
        }
        catch(InterruptedException ie) {}
        System.out.println("Quote generator rip");
    }
}
