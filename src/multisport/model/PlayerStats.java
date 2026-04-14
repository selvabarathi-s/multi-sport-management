package multisport.model;

public class PlayerStats {
    private int statId;
    private Integer playerId;
    private Integer matchId;
    private Integer score;
    private Integer assists;
    private Integer wickets;

    public PlayerStats() {}

    public PlayerStats(Integer playerId, Integer matchId, Integer score, Integer assists, Integer wickets) {
        this.playerId = playerId;
        this.matchId = matchId;
        this.score = score;
        this.assists = assists;
        this.wickets = wickets;
    }

    public PlayerStats(int statId, Integer playerId, Integer matchId, Integer score, Integer assists, Integer wickets) {
        this.statId = statId;
        this.playerId = playerId;
        this.matchId = matchId;
        this.score = score;
        this.assists = assists;
        this.wickets = wickets;
    }

    public int getStatId() { return statId; }
    public void setStatId(int statId) { this.statId = statId; }

    public Integer getPlayerId() { return playerId; }
    public void setPlayerId(Integer playerId) { this.playerId = playerId; }

    public Integer getMatchId() { return matchId; }
    public void setMatchId(Integer matchId) { this.matchId = matchId; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getAssists() { return assists; }
    public void setAssists(Integer assists) { this.assists = assists; }

    public Integer getWickets() { return wickets; }
    public void setWickets(Integer wickets) { this.wickets = wickets; }

    @Override
    public String toString() {
        return "PlayerStats{id=" + statId + ", playerId=" + playerId + ", matchId=" + matchId +
               ", score=" + score + ", assists=" + assists + ", wickets=" + wickets + "}";
    }
}
