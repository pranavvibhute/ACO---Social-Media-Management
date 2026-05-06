package Models;

public class TimeSlot {
    private int id;
    private String name; // Morning, Afternoon, Evening, Night

    public TimeSlot(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}