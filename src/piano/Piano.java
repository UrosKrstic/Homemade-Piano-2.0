package piano;

import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javax.sound.midi.MidiUnavailableException;
import java.util.ArrayList;
import java.util.HashMap;

public class Piano extends Pane {

    private static final int NUM_OF_WHITE_KEYS = 35, NUM_OF_BLACK_KEYS = 25;
    private final static char[] lowercaseMapForNumbers = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'};

    private ReceivedNoteHandler handler;
    private boolean helperON = true;
    private int i = 0, x, y;
    private Canvas blackKeysFront, whiteKeysBack;
    private KeyReceiver pressedReceiver, releasedReceiver;
    private ArrayList<PianoKey> blackKeys = new ArrayList<>(NUM_OF_BLACK_KEYS), whiteKeys = new ArrayList<>(NUM_OF_WHITE_KEYS);

    public void setHandler(ReceivedNoteHandler _handler) { handler = _handler; }

    public void setPressedReceiver(KeyReceiver _receiver) { pressedReceiver = _receiver; }
    public void setReleasedReceiver(KeyReceiver _receiver) { releasedReceiver = _receiver; }

    public Canvas getBlackKeysFront() {
        return blackKeysFront;
    }

    public Canvas getWhiteKeysBack() {
        return whiteKeysBack;
    }

    public ArrayList<PianoKey> getBlackKeys() {
        return blackKeys;
    }

    public ArrayList<PianoKey> getWhiteKeys() {
        return whiteKeys;
    }

    public ArrayList<PianoKey> getAllKeys() {
        ArrayList<PianoKey> allKeys = new ArrayList<>();
        allKeys.addAll(blackKeys);
        allKeys.addAll(whiteKeys);
        return allKeys;
    }

    private char getClickedChar(KeyEvent ke) {
        char clickedChar;
        if (ke.getCode().isDigitKey()) {
            if (ke.getCode().getCode() - '0' - 1 < lowercaseMapForNumbers.length)
                clickedChar = lowercaseMapForNumbers[ke.getCode().getCode() - '0' - 1];
            else {
                clickedChar = '\0';
            }
        }
        else {
            clickedChar = (char)ke.getCode().getCode();
        }
        return clickedChar;
    }

    private void sendKey(KeyEvent ke, KeyReceiver receiver) {
        if (ke.isShiftDown()) {
            for (PianoKey key : blackKeys) {
                char clickedChar = getClickedChar(ke);
                if (key.getNote().getTextCode() == clickedChar)
                    receiver.addKey(key);
            }
        }
        else {
            for (PianoKey key : whiteKeys) {
                if (key.getNote().getTextCode() == Character.toLowerCase(ke.getCode().getCode()))
                    receiver.addKey(key);
            }
        }
    }

    public boolean isHelperON() { return helperON; }
    public void setHelper(boolean b) {
        helperON = b;
        handler.unhighlightKeys(getAllKeys());
    }

    public void keyPressed(KeyEvent ke) {
      sendKey(ke, pressedReceiver);
    }

    public void keyReleased(KeyEvent ke) {
     sendKey(ke, releasedReceiver);
    }

    public void sendMousePressKey(MouseEvent me, KeyReceiver receiver) {
        for (PianoKey key : blackKeys) {
            if (key.getKeyRect().contains(me.getX() - x, me.getY() - y - 25)) {
                receiver.addKey(key);
                return;
            }
        }
        for (PianoKey key : whiteKeys) {
            if (key.getKeyRect().contains(me.getX() - x, me.getY() - y - 25)) {
                receiver.addKey(key);
                return;
            }
        }
    }

    public void mousePressed(MouseEvent me) {
        sendMousePressKey(me, pressedReceiver);
    }

    public void mouseReleased(MouseEvent me) {
        sendMousePressKey(me, releasedReceiver);
    }

    public Piano(KeyToMidiAndNoteMap mappingObj, int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        //pane repositioning
        setLayoutX(x);
        setLayoutY(y);
        setWidth(width);
        setHeight(height);

        //canvas init
        blackKeysFront = new Canvas(width, height);
        whiteKeysBack = new Canvas(width, height);
        getChildren().addAll(blackKeysFront, whiteKeysBack);
        blackKeysFront.toFront();

        //gc init
        GraphicsContext gcBlack = blackKeysFront.getGraphicsContext2D();
        GraphicsContext gcWhite = whiteKeysBack.getGraphicsContext2D();
        gcBlack.setFill(Color.BLACK);
        gcBlack.setStroke(Color.WHITE);
        gcWhite.setFill(Color.WHITE);
        gcWhite.setStroke(Color.BLACK);
        gcWhite.setLineWidth(1.5);


        //drawing coordinates init
        int xWhiteStep = width / NUM_OF_WHITE_KEYS;
        int xBlackStepLarge = xWhiteStep * 2;
        int blackWidth = xWhiteStep / 2, blackHeight = (int)((2. / 3) * height);
        int xBlack = (int)((3. / 4) * xWhiteStep), xWhite = 0;

        //drawing and key init
        ArrayList<Character> pianoOrderedKeys = mappingObj.getPianoOrderedKeys();
        HashMap<Character, Pair<String, Integer>> map = mappingObj.getMap();
        for (char key : pianoOrderedKeys) {
            Pair<String, Integer> pair = map.get(key);
            Note note = new Note(key, pair.second, pair.first);
            if (note.isSharp()) {
                Rectangle rect = new Rectangle(xBlack, 0, blackWidth, blackHeight);
                blackKeys.add(new PianoKey(note, rect));
                gcBlack.fillRect(xBlack, 0, blackWidth, blackHeight);
                if (helperON)
                    gcBlack.strokeText(note.getTextCode()+ "", (int)(xBlack + blackWidth * 0.24), (int)(0.78 * blackHeight));
                if (note.getName().charAt(0) == 'D' || note.getName().charAt(0) == 'A') {
                    xBlack += xBlackStepLarge;
                }
                else {
                    xBlack += xWhiteStep;
                }
            }
            else {
                Rectangle rect = new Rectangle(xWhite, 0, xWhiteStep, height);
                whiteKeys.add(new PianoKey(note, rect));
                gcWhite.fillRect(xWhite, 0, xWhiteStep, height);
                gcWhite.strokeRect(xWhite, 0, xWhiteStep, height);
                if (helperON)
                    gcWhite.strokeText(note.getTextCode()+ "", (int)(xWhite + xWhiteStep * 0.4), (int)(0.84 * height));
                xWhite += xWhiteStep;
            }
        }
    }
}
