package piano;

import java.util.ArrayList;

public class Chord extends MusicSymbol {
    private ArrayList<Note> notes = new ArrayList<>();

    public ArrayList<Note> getNotes() { return notes; }
    public void addNote(Note n) {
        notes.add(n);
    }
    public Chord() {
        duration = 2;
    }
    public Chord(ArrayList<Note> _notes) {
        this();
        notes = _notes;
    }
    public boolean equals(Object o) {
        if (o instanceof Note || o instanceof Pause)
            return false;
        Chord chord = (Chord)o;
        if (notes.size() != chord.getNotes().size())
            return false;
        boolean matched;
        for (Note note1 : notes) {
            matched = false;
            for (Note note2 : chord.getNotes()) {
                if (note1.equals(note2)) {
                    matched = true;
                    break;
                }
            }
            if (!matched)
                return false;
        }
        return true;
    }
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Note note : notes) {
            str.append(note.toString());
        }
        return str.toString();
    }
}
