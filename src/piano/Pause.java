package piano;

public class Pause extends MusicSymbol{
    public Pause(int _duration) {
        duration = _duration;
    }

    public boolean equals(Object o) {
        if (o instanceof  Note || o instanceof Chord)
            return false;
        Pause pause = (Pause)o;
        return pause.getDuration() == duration;
    }

    public String toString() {
        return super.toString() + "pause";
    }
}
