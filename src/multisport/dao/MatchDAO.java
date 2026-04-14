package multisport.dao;

import multisport.DatabaseConnection;
import multisport.model.Match;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchDAO {

    public int createMatch(Match match) {
        String sql = "INSERT INTO matches (sport_id, team1_id, team2_id, match_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, match.getSportId());
            if (match.getTeam1Id() != null) {
                pstmt.setInt(2, match.getTeam1Id());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            if (match.getTeam2Id() != null) {
                pstmt.setInt(3, match.getTeam2Id());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setDate(4, Date.valueOf(match.getMatchDate()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Match getMatchById(int matchId) {
        String sql = "SELECT * FROM matches WHERE match_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Match(
                    rs.getInt("match_id"),
                    rs.getInt("sport_id"),
                    rs.getObject("team1_id", Integer.class),
                    rs.getObject("team2_id", Integer.class),
                    rs.getDate("match_date").toLocalDate()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateMatch(Match match) {
        String sql = "UPDATE matches SET sport_id = ?, team1_id = ?, team2_id = ?, match_date = ? WHERE match_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, match.getSportId());
            if (match.getTeam1Id() != null) {
                pstmt.setInt(2, match.getTeam1Id());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            if (match.getTeam2Id() != null) {
                pstmt.setInt(3, match.getTeam2Id());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setDate(4, Date.valueOf(match.getMatchDate()));
            pstmt.setInt(5, match.getMatchId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteMatch(int matchId) {
        String sql = "DELETE FROM matches WHERE match_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Match> getAllMatches() {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches ORDER BY match_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                matches.add(new Match(
                    rs.getInt("match_id"),
                    rs.getInt("sport_id"),
                    rs.getObject("team1_id", Integer.class),
                    rs.getObject("team2_id", Integer.class),
                    rs.getDate("match_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matches;
    }

    public List<Match> getMatchesBySport(int sportId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE sport_id = ? ORDER BY match_date";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sportId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matches.add(new Match(
                    rs.getInt("match_id"),
                    rs.getInt("sport_id"),
                    rs.getObject("team1_id", Integer.class),
                    rs.getObject("team2_id", Integer.class),
                    rs.getDate("match_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matches;
    }
}
