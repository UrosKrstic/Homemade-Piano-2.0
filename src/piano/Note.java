package piano;

public class Note {
    private char textCode;
    private int MIDIcode;
    private String name;

    public Note(char _textCode, int _MIDIcode, String _name) {
        textCode = _textCode;
        MIDIcode = _MIDIcode;
        name = _name;
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

