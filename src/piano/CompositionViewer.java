package piano;

import com.sun.prism.Graphics;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class CompositionViewer extends Pane {

    private final static int NUM_OF_QUARTERS = 15;
    private final static int NUM_OF_EIGHTS = (NUM_OF_QUARTERS - 1) * 2;
    private final static int MAX_NOTES_IN_CHORD = 5;
    private Canvas bg, fg;
    private Composition comp;
    private int x, y, width, height, quarterW, bgLineHeight;
    private int startX = 3, currentSymbolIndex = 0;
    private boolean reachedEnd;
    private boolean codeShown;

    public CompositionViewer(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        quarterW = width / NUM_OF_QUARTERS;
        bgLineHeight = (int)(height * 0.833333);
        setLayoutX(x);
        setLayoutY(y);
        setWidth(width);
        setHeight(height);

        bg = new Canvas(width, height);
        fg = new Canvas(width, height);
        fg.toFront();
        getChildren().addAll(bg, fg);

       setBG();
    }

    private void setBG() {
        GraphicsContext gc = bg.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.WHITE);

        gc.fillRect(0, 0, width, height);
        gc.setLineWidth(1.5);
        for (int i = startX; i <= width; i += quarterW) {
            gc.strokeLine(i, bgLineHeight, i, height - 3);
        }
    }

    public void setComposition(Composition _comp) {
        comp = _comp;
        drawComp();
    }

    public boolean checkCurrentSymbol(MusicSymbol symbol) {
        if (comp != null) {
            //System.out.println(comp.getMusicSymbols().get(currentSymbolIndex));
            if (symbol.equals(comp.getMusicSymbols().get(currentSymbolIndex))) {
                currentSymbolIndex = (currentSymbolIndex + 1) % comp.getMusicSymbols().size();
                drawComp();
                if (currentSymbolIndex == 0) reachedEnd = true;
                else reachedEnd = false;
                return true;
            }
        }
        return false;
    }

    public void setCurrentSymbol() {
        currentSymbolIndex = (currentSymbolIndex + 1) % comp.getMusicSymbols().size();
        if (currentSymbolIndex == 0) reachedEnd = true;
    }

    public void setCodeShown(boolean b) { codeShown = b; }

    public boolean reachedEnd() { return reachedEnd; }

    public void resetViewer() {currentSymbolIndex = 0;}

    public void drawComp() {
        if (comp != null) {
            ArrayList<MusicSymbol> symbols = comp.getMusicSymbols();
            int k = currentSymbolIndex;
            int eightW = quarterW / 2;
            int x = startX, h = height / 6, singleSymbolY = (int)((4. / 6) * height);
            GraphicsContext gc = fg.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, width, (int)((5./6) * height));
            for (int i = 0; i < NUM_OF_EIGHTS;) {
                if (k < symbols.size()) {
                    MusicSymbol symbol = symbols.get(k);
                    if (symbol.getDuration() == 1) {
                        if (symbol instanceof Note) {
                            Note note = (Note)symbol;
                            gc.setFill(Color.LIGHTGREEN);
                            gc.setStroke(Color.BLACK);
                            gc.fillRect(x, singleSymbolY, eightW, h);
                            if (codeShown) {
                                gc.strokeText(note.getName(), x, singleSymbolY);
                            }
                            else {
                                gc.strokeText(Character.toString(note.getTextCode()), x + (int)(eightW * 0.4649), singleSymbolY + (int)(h * 0.665));
                            }
                        }
                        else if (symbol instanceof Pause) {
                            gc.setFill(Color.DARKGREEN);
                            gc.fillRect(x, singleSymbolY, eightW, h);
                        }
                        x += eightW;
                    } else {
                        if (symbol instanceof Note) {
                            Note note = (Note)symbol;
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            gc.fillRect(x, singleSymbolY, quarterW, h);
                            if (codeShown) {
                                gc.strokeText(note.getName(), x, singleSymbolY);
                            }
                            else {
                                gc.strokeText(Character.toString(note.getTextCode()), x + (int)(quarterW * 0.465), singleSymbolY + (int)(h * 0.665));
                            }
                        }
                        else if (symbol instanceof Pause) {
                            gc.setFill(Color.DARKRED);
                            gc.fillRect(x, singleSymbolY, quarterW, h);
                        }
                        else  {
                            Chord chord = (Chord)symbol;
                            ArrayList<Note> notes = chord.getNotes();
                            int iter = Math.min(MAX_NOTES_IN_CHORD, notes.size());
                            gc.setFill(Color.RED);
                            gc.setStroke(Color.BLACK);
                            int y = singleSymbolY;
                            for (int j = 0; j < iter; j++) {
                                gc.fillRect(x, y, quarterW, h);
                                gc.strokeText(Character.toString(notes.get(j).getTextCode()), x + (int)(quarterW * 0.465),
                                        y + (int)(h * 0.665));
                                y -= h;
                            }

                        }
                        x += quarterW;
                    }
                    i += symbols.get(k).getDuration();
                    k++;
                }
                else {
                    break;
                }
            }
        }
    }




}
