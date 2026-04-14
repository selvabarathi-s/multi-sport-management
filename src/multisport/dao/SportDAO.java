package multisport.dao;

import multisport.DatabaseConnection;
import multisport.model.Sport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SportDAO {

    public int createSport(Sport sport) {
        String sql = "INSERT INTO sports (sport_name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, sport.getSportName());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Sport getSportById(int sportId) {
        String sql = "SELECT * FROM sports WHERE sport_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sportId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Sport(rs.getInt("sport_id"), rs.getString("sport_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateSport(Sport sport) {
        String sql = "UPDATE sports SET sport_name = ? WHERE sport_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sport.getSportName());
            pstmt.setInt(2, sport.getSportId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteSport(int sportId) {
        String sql = "DELETE FROM sports WHERE sport_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sportId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Sport> getAllSports() {
        List<Sport> sports = new ArrayList<>();
        String sql = "SELECT * FROM sports ORDER BY sport_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                sports.add(new Sport(rs.getInt("sport_id"), rs.getString("sport_name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sports;
    }
}
