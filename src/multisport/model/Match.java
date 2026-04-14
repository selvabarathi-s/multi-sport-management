package multisport.model;

import java.time.LocalDate;

public class Match {
    private int matchId;
    private int sportId;
    private Integer team1Id;
    private Integer team2Id;
    private LocalDate matchDate;

    public Match() {}

    public Match(int sportId, Integer team1Id, Integer team2Id, LocalDate matchDate) {
        this.sportId = sportId;
        this.team1Id = team1Id;
        this.team2Id = team2Id;
        this.matchDate = matchDate;
    }

    public Match(int matchId, int sportId, Integer team1Id, Integer team2Id, LocalDate matchDate) {
        this.matchId = matchId;
        this.sportId = sportId;
        this.team1Id = team1Id;
        this.team2Id = team2Id;
        this.matchDate = matchDate;
    }

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getSportId() { return sportId; }
    public void setSportId(int sportId) { this.sportId = sportId; }

    public Integer getTeam1Id() { return team1Id; }
    public void setTeam1Id(Integer team1Id) { this.team1Id = team1Id; }

    public Integer getTeam2Id() { return team2Id; }
    public void setTeam2Id(Integer team2Id) { this.team2Id = team2Id; }

    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    @Override
    public String toString() {
        return "Match{id=" + matchId + ", sportId=" + sportId + ", team1Id=" + team1Id + ", team2Id=" + team2Id + ", date=" + matchDate + "}";
    }
}
