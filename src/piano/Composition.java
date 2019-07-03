package piano;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;
import exceptions.FileException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    }

    public void exportToTXT(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        boolean firstEight = true;
        for (int i = 0; i < symbols.size(); i++) {
            MusicSymbol symbol = symbols.get(i);
            if (symbol instanceof Pause) {
                Pause pause = (Pause) symbol;
                if (pause.getDuration() == 1) {
                    writer.write(" ");
                }
                else {
                    writer.write("|");
                }
            }
            if (symbol instanceof Chord) {
                writer.write("[");
                Chord chord = (Chord) symbol;
                for (Note note : chord.getNotes()) {
                    writer.write(Character.toString(note.getTextCode()));
                }
                writer.write("]");
            }
            if (symbol instanceof Note) {
                Note note = (Note) symbol;
                if (note.getDuration() == 2) {
                    writer.write(Character.toString(note.getTextCode()));
                }
                else {
                    if (firstEight) {
                        writer.write("[" + Character.toString(note.getTextCode()));
                        firstEight = false;
                    }
                    else {
                        writer.write(Character.toString(note.getTextCode()));
                    }
                    boolean isLast = true;
                    for (int j = i + 1; j < symbols.size(); j++) {
                        if (symbols.get(j) instanceof Chord || (symbols.get(j) instanceof Note && symbols.get(j).getDuration() == 2))
                            break;
                        else if (symbols.get(j) instanceof Note && symbols.get(j).getDuration() == 1)
                            isLast = false;
                    }
                    if (isLast) {
                        writer.write("]");
                        firstEight = true;
                    }
                }
            }
        }
        writer.close();
    }
    public void exportToMIDI(String path) throws IOException {
        MidiTrack tempoTrack = new MidiTrack();
        MidiTrack noteTrack = new MidiTrack();

        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(8, 8, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

        Tempo tempo = new Tempo();
        tempo.setBpm(200);
        tempoTrack.insertEvent(ts);
        tempoTrack.insertEvent(tempo);

        int channel = 0, velocity = 100, duration = 60;
        for (int i = 0; i < symbols.size(); i++) {
            MusicSymbol symbol = symbols.get(i);
            long tick = i * 480;
            if (symbol instanceof Note) {
                Note note = (Note)symbol;
                noteTrack.insertNote(channel, note.getMIDIcode(), velocity, tick, duration * note.getDuration());
            }
            else if (symbol instanceof Chord) {
                ArrayList<Note> notes = ((Chord) symbol).getNotes();
                for (Note note : notes) {
                    noteTrack.insertNote(channel, note.getMIDIcode(), velocity, tick, duration * 2);
                }
            }
        }
        //inivisible ghost note that does the job xd rofl kek lmfao kms
        noteTrack.insertNote(channel, 1, velocity, 480 * symbols.size(), duration);
        List<MidiTrack> tracks = new ArrayList<>();
        tracks.add(tempoTrack);
        tracks.add(noteTrack);
        MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);
        File output = new File(path);
        midi.writeToFile(output);
    }
}
