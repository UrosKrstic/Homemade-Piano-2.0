package piano;

import javafx.scene.shape.Rectangle;

public class PianoKey {

    private Note note;
    private Rectangle keyRect;

    public Note getNote() {
        return note;
    }

    public Rectangle getKeyRect() {
        return keyRect;
    }

    public PianoKey(Note _note, Rectangle _keyRect) {
        note = _note;
        keyRect = _keyRect;
    }
}
