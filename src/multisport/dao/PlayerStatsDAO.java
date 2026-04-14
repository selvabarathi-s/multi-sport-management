package multisport.dao;

import multisport.DatabaseConnection;
import multisport.model.PlayerStats;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerStatsDAO {

    public int createStats(PlayerStats stats) {
        String sql = "INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (stats.getPlayerId() != null) pstmt.setInt(1, stats.getPlayerId());
            else pstmt.setNull(1, Types.INTEGER);

            if (stats.getMatchId() != null) pstmt.setInt(2, stats.getMatchId());
            else pstmt.setNull(2, Types.INTEGER);

            if (stats.getScore() != null) pstmt.setInt(3, stats.getScore());
            else pstmt.setNull(3, Types.INTEGER);

            if (stats.getAssists() != null) pstmt.setInt(4, stats.getAssists());
            else pstmt.setNull(4, Types.INTEGER);

            if (stats.getWickets() != null) pstmt.setInt(5, stats.getWickets());
            else pstmt.setNull(5, Types.INTEGER);

            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public PlayerStats getStatsById(int statId) {
        String sql = "SELECT * FROM player_stats WHERE stat_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, statId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                    rs.getInt("stat_id"),
                    rs.getObject("player_id", Integer.class),
                    rs.getObject("match_id", Integer.class),
                    rs.getObject("score", Integer.class),
                    rs.getObject("assists", Integer.class),
                    rs.getObject("wickets", Integer.class)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStats(PlayerStats stats) {
        String sql = "UPDATE player_stats SET player_id = ?, match_id = ?, score = ?, assists = ?, wickets = ? WHERE stat_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (stats.getPlayerId() != null) pstmt.setInt(1, stats.getPlayerId());
            else pstmt.setNull(1, Types.INTEGER);

            if (stats.getMatchId() != null) pstmt.setInt(2, stats.getMatchId());
            else pstmt.setNull(2, Types.INTEGER);

            if (stats.getScore() != null) pstmt.setInt(3, stats.getScore());
            else pstmt.setNull(3, Types.INTEGER);

            if (stats.getAssists() != null) pstmt.setInt(4, stats.getAssists());
            else pstmt.setNull(4, Types.INTEGER);

            if (stats.getWickets() != null) pstmt.setInt(5, stats.getWickets());
            else pstmt.setNull(5, Types.INTEGER);

            pstmt.setInt(6, stats.getStatId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteStats(int statId) {
        String sql = "DELETE FROM player_stats WHERE stat_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, statId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<PlayerStats> getAllStats() {
        List<PlayerStats> statsList = new ArrayList<>();
        String sql = "SELECT * FROM player_stats ORDER BY stat_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                statsList.add(new PlayerStats(
                    rs.getInt("stat_id"),
                    rs.getObject("player_id", Integer.class),
                    rs.getObject("match_id", Integer.class),
                    rs.getObject("score", Integer.class),
                    rs.getObject("assists", Integer.class),
                    rs.getObject("wickets", Integer.class)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statsList;
    }

    public List<PlayerStats> getStatsByPlayer(int playerId) {
        List<PlayerStats> statsList = new ArrayList<>();
        String sql = "SELECT * FROM player_stats WHERE player_id = ? ORDER BY match_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                statsList.add(new PlayerStats(
                    rs.getInt("stat_id"),
                    rs.getObject("player_id", Integer.class),
                    rs.getObject("match_id", Integer.class),
                    rs.getObject("score", Integer.class),
                    rs.getObject("assists", Integer.class),
                    rs.getObject("wickets", Integer.class)
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statsList;
    }
}
