package piano;

public abstract class MusicSymbol {
    protected int duration = 1;
    public void exportToMidi() {}
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    @Override
    public boolean equals (Object o) { return true; }

    public String toString() {
        return duration + " : ";
    }
}
