package piano;

import exceptions.FileException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Composition {
    private ArrayList<MusicSymbol> symbols;
    private KeyToMidiAndNoteMap mapObj;

    public ArrayList<MusicSymbol> getMusicSymbols() { return symbols; }

    public Composition(KeyToMidiAndNoteMap mapObj) {
        this.mapObj = mapObj;
        symbols = new ArrayList<>();
    }

    public Composition(KeyToMidiAndNoteMap mapObj, ArrayList<MusicSymbol> symbols) {
        this.mapObj = mapObj;
        this.symbols = symbols;
    }

    public Composition(ArrayList<MusicSymbol> symbols) {
        this.symbols = symbols;
    }

    public void loadFromFile(String path) throws FileNotFoundException, FileException {
        File file = new File(path);
        Scanner sc = new Scanner(file);
        //System.out.println("It's time to parse the input file.");

        Pattern regularExpression = Pattern.compile("(\\[[^\\[\\]]*\\])|([^\\[\\]\\| ])|(\\|)|( )");
        Pattern chord = Pattern.compile("\\[([^\\| ]*)\\]|\\[(.*)\\]");

        HashMap<Character, Pair<String, Integer>> map = mapObj.getMap();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            Matcher match = regularExpression.matcher(line);
            while (match.find()) {
                if (match.group(1) != null) {
                    Matcher submatch = chord.matcher(match.group(1));
                    if (submatch.find()) {
                        if (submatch.group(1) != null) {
                           // System.out.println("It's a real chord: " + submatch.group(1));
                            String notesString = submatch.group(1);
                            ArrayList<Note> notesOfChord = new ArrayList<>(notesString.length());
                            for (int i = 0; i < notesString.length(); i++) {
                                Pair<String, Integer> pair = map.get(notesString.charAt(i));
                                //System.out.print(notesString.charAt(i) + " ");
                                if (pair == null) {
                                    throw new FileException("Incorrect file: " + path);
                                }
                                notesOfChord.add(new Note(notesString.charAt(i), pair.second, pair.first, 2));
                            }
                            symbols.add(new Chord(notesOfChord));
                        }
                        else if (submatch.group(2) != null) {
                           // System.out.println("It's eight notes: " + submatch.group(2));
                            submatch = regularExpression.matcher(submatch.group(2));
                            while (submatch.find()) {
                                if (submatch.group(2) != null) {
                                  //  System.out.println("It's a note: " + submatch.group(2));
                                    Pair<String, Integer> pair = map.get(submatch.group(2).charAt(0));
                                    symbols.add(new Note(submatch.group(2).charAt(0), pair.second, pair.first, 1));
                                }
                                else if (submatch.group(3) != null) {
                                   // System.out.println("It's a long pause: " + submatch.group(3));
                                    symbols.add(new Pause(2));
                                }
                                else if (submatch.group(4) != null) {
                                   // System.out.println("It's a short pause: " + submatch.group(4));
                                    symbols.add(new Pause(1));
                                }
                                else if (submatch.group(1) != null) {
                                    throw new FileException("Incorrect file: " + path);
                                }
                            }
                        }
                    }
                    else {
                        throw new FileException("Incorrect file: " + path);
                    }
                }
                else if (match.group(2) != null) {
                   /// System.out.println("It's a note: " + match.group(2));
                    Pair<String, Integer> pair = map.get(match.group(2).charAt(0));
                    symbols.add(new Note(match.group(2).charAt(0), pair.second, pair.first, 2));
                }
                else if (match.group(3) != null) {
                    //System.out.println("It's a long pause: " + match.group(3));
                    symbols.add(new Pause(2));
                }
                else if (match.group(4) != null) {
                    //System.out.println("It's a short pause: " + match.group(4));
                    symbols.add(new Pause(1));
                }
                else {
                    throw new FileException("Incorrect file: " + path);
                }
            }
        }
        //System.out.println("Done parsing.");
    }

    public void exportToTXT(String path) {}
    public void exportToMIDI(String path) {}
}
