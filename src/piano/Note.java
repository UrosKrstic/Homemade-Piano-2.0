package piano;

public class Note extends MusicSymbol{
    private char textCode;
    private int MIDIcode;
    private String name;

    public Note(char _textCode, int _MIDIcode, String _name) {
        textCode = _textCode;
        MIDIcode = _MIDIcode;
        name = _name;
    }

    public Note(char _textCode, int _MIDIcode, String _name, int _duration) {
        this(_textCode, _MIDIcode, _name);
        duration = _duration;
    }

    public boolean equals(Object o) {
        if (o instanceof Chord || o instanceof Pause)
            return false;
        Note note = (Note) o;
        return textCode == note.getTextCode() && duration == note.getDuration();
    }

    public String toString() {
        return super.toString() + textCode + " " + MIDIcode + " " + name;
    }

    public char getTextCode() {
        return textCode;
    }

    public boolean isSharp() {
        return name.length() == 3;
    }

    public int getMIDIcode() {
        return MIDIcode;
    }

    public String getName() {
        return name;
    }
}

