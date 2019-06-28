package piano;

import exceptions.FileException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class KeyToMidiAndNoteMap extends Thread {
    private ArrayList<Character> pianoOrderedKeys;
    private HashMap<Character, Pair<String, Integer>> keyToMidiAndNote;
    private HashMap<Integer, Character> midiAndNoteToKey;
    private String filePath;
    private boolean errorHappened;
    private Scanner mapFileStream;

    public KeyToMidiAndNoteMap(String fileName, int capacity) throws FileException {
        this.filePath = fileName;
        keyToMidiAndNote = new HashMap<>(capacity);
        midiAndNoteToKey = new HashMap<>(capacity);
        pianoOrderedKeys = new ArrayList<>(capacity);
        try { mapFileStream = new Scanner(new FileReader(filePath)); }
        catch (FileNotFoundException fe) {
            throw new FileException("file " + fileName + " not found"); }
    }

    public boolean errorOccurred() {
        return errorHappened;
    }

    public ArrayList<Character> getPianoOrderedKeys() { return pianoOrderedKeys; }

    public HashMap<Character, Pair<String, Integer>> getMap() {
        return keyToMidiAndNote;
    }

    public HashMap<Integer, Character> getReverseMap() {
        return midiAndNoteToKey;
    }

    public void run() {
        try {
            String line;
            Pattern regularExpression = Pattern.compile("^([a-zA-Z0-9!@$%^&*()]),([A-G]#?[2-6]),([0-9]{2})$");
            int lineNumber = 1;

            while (mapFileStream.hasNext()) {
                line = mapFileStream.next();
                Matcher matchedLine = regularExpression.matcher(line);
                if (matchedLine.matches()) {
                    keyToMidiAndNote.put(matchedLine.group(1).charAt(0),
                            new Pair<>(matchedLine.group(2), Integer.parseInt(matchedLine.group(3))));
                    pianoOrderedKeys.add(matchedLine.group(1).charAt(0));
                    midiAndNoteToKey.put(Integer.parseInt(matchedLine.group(3)),
                            matchedLine.group(1).charAt(0));
                    //System.out.println(keyToMidiAndNote.get(matchedLine.group(1).charAt(0)));
                }
                else {
                    throw new FileException("Wrong pattern: line " + lineNumber);
                }
                lineNumber++;
            }
        }
        catch(FileException e) {
            System.out.println(e.getMessage());
            errorHappened = true;
        }
        finally {
            mapFileStream.close();
        }
    }


}
