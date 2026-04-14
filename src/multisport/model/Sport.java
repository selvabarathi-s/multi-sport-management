package multisport.model;

import java.time.LocalDate;

public class Sport {
    private int sportId;
    private String sportName;

    public Sport() {}

    public Sport(String sportName) {
        this.sportName = sportName;
    }

    public Sport(int sportId, String sportName) {
        this.sportId = sportId;
        this.sportName = sportName;
    }

    public int getSportId() { return sportId; }
    public void setSportId(int sportId) { this.sportId = sportId; }

    public String getSportName() { return sportName; }
    public void setSportName(String sportName) { this.sportName = sportName; }

    @Override
    public String toString() {
        return "Sport{id=" + sportId + ", name='" + sportName + "'}";
    }
}
