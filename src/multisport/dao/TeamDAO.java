package multisport.dao;

import multisport.DatabaseConnection;
import multisport.model.Team;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeamDAO {

    public int createTeam(Team team) {
        String sql = "INSERT INTO teams (team_name, sport_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, team.getTeamName());
            pstmt.setInt(2, team.getSportId());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Team getTeamById(int teamId) {
        String sql = "SELECT * FROM teams WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Team(rs.getInt("team_id"), rs.getString("team_name"), rs.getInt("sport_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateTeam(Team team) {
        String sql = "UPDATE teams SET team_name = ?, sport_id = ? WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, team.getTeamName());
            pstmt.setInt(2, team.getSportId());
            pstmt.setInt(3, team.getTeamId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTeam(int teamId) {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY team_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                teams.add(new Team(rs.getInt("team_id"), rs.getString("team_name"), rs.getInt("sport_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    public List<Team> getTeamsBySport(int sportId) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams WHERE sport_id = ? ORDER BY team_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sportId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                teams.add(new Team(rs.getInt("team_id"), rs.getString("team_name"), rs.getInt("sport_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }
}
