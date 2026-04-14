package multisport;

import multisport.dao.*;
import multisport.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  COMPREHENSIVE TEST SUITE");
        System.out.println("========================================\n");

        testSportDAO();
        testTeamDAO();
        testPlayerDAO();
        testMatchDAO();
        testPlayerStatsDAO();
        testAnalyticalQueries();
        testForeignKeyConstraints();
        testCheckConstraints();
        testEdgeCases();

        System.out.println("\n========================================");
        System.out.println("  RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
    }

    /* ==================== SPORT DAO ==================== */

    private static void testSportDAO() {
        section("SportDAO");

        SportDAO dao = new SportDAO();

        // CREATE
        int id = dao.createSport(new Sport("Tennis"));
        assertPass("createSport", id > 0);

        // READ
        Sport s = dao.getSportById(id);
        assertPass("getSportById", s != null && s.getSportName().equals("Tennis"));

        // UPDATE
        s.setSportName("Table Tennis");
        boolean updated = dao.updateSport(s);
        assertPass("updateSport", updated);
        Sport updatedSport = dao.getSportById(id);
        assertPass("updateSport verified", updatedSport != null && updatedSport.getSportName().equals("Table Tennis"));

        // GET ALL
        List<Sport> all = dao.getAllSports();
        assertPass("getAllSports", all.size() >= 3);

        // DELETE
        boolean deleted = dao.deleteSport(id);
        assertPass("deleteSport", deleted);
        Sport gone = dao.getSportById(id);
        assertPass("deleteSport verified", gone == null);

        // DELETE non-existent
        boolean delFail = dao.deleteSport(99999);
        assertPass("deleteSport non-existent returns false", !delFail);

        // GET non-existent
        Sport gone2 = dao.getSportById(99999);
        assertPass("getSportById non-existent returns null", gone2 == null);
    }

    /* ==================== TEAM DAO ==================== */

    private static void testTeamDAO() {
        section("TeamDAO");

        TeamDAO dao = new TeamDAO();
        SportDAO sportDAO = new SportDAO();

        // CREATE
        int sportId = sportDAO.createSport(new Sport("Volleyball"));
        int teamId = dao.createTeam(new Team("Beach Boys", sportId));
        assertPass("createTeam", teamId > 0);

        // READ
        Team t = dao.getTeamById(teamId);
        assertPass("getTeamById", t != null && t.getTeamName().equals("Beach Boys"));

        // UPDATE
        t.setTeamName("Beach Stars");
        boolean updated = dao.updateTeam(t);
        assertPass("updateTeam", updated);
        Team updatedTeam = dao.getTeamById(teamId);
        assertPass("updateTeam verified", updatedTeam != null && updatedTeam.getTeamName().equals("Beach Stars"));

        // GET ALL
        List<Team> all = dao.getAllTeams();
        assertPass("getAllTeams", all.size() >= 6);

        // GET BY SPORT
        List<Team> cricket = dao.getTeamsBySport(1);
        assertPass("getTeamsBySport (Cricket)", cricket.size() == 2);

        List<Team> football = dao.getTeamsBySport(2);
        assertPass("getTeamsBySport (Football)", football.size() == 2);

        List<Team> basketball = dao.getTeamsBySport(3);
        assertPass("getTeamsBySport (Basketball)", basketball.size() == 2);

        List<Team> empty = dao.getTeamsBySport(99999);
        assertPass("getTeamsBySport non-existent returns empty", empty.isEmpty());

        // DELETE
        boolean deleted = dao.deleteTeam(teamId);
        assertPass("deleteTeam", deleted);
        boolean delSport = sportDAO.deleteSport(sportId);
        assertPass("cleanup sport", delSport);

        // DELETE non-existent
        assertPass("deleteTeam non-existent returns false", !dao.deleteTeam(99999));
    }

    /* ==================== PLAYER DAO ==================== */

    private static void testPlayerDAO() {
        section("PlayerDAO");

        PlayerDAO dao = new PlayerDAO();

        // CREATE
        int playerId = dao.createPlayer(new Player("Test Player", 25, 1));
        assertPass("createPlayer", playerId > 0);

        // CREATE with null team
        int freeAgentId = dao.createPlayer(new Player("Free Agent", 22, null));
        assertPass("createPlayer with null team", freeAgentId > 0);

        // READ
        Player p = dao.getPlayerById(playerId);
        assertPass("getPlayerById", p != null && p.getName().equals("Test Player"));

        // UPDATE
        p.setName("Updated Player");
        p.setAge(26);
        boolean updated = dao.updatePlayer(p);
        assertPass("updatePlayer", updated);
        Player updatedPlayer = dao.getPlayerById(playerId);
        assertPass("updatePlayer verified", updatedPlayer != null && updatedPlayer.getName().equals("Updated Player") && updatedPlayer.getAge() == 26);

        // GET ALL
        List<Player> all = dao.getAllPlayers();
        assertPass("getAllPlayers", all.size() >= 12);

        // GET BY TEAM
        List<Player> india = dao.getPlayersByTeam(1);
        assertPass("getPlayersByTeam (India)", india.size() >= 2);

        List<Player> empty = dao.getPlayersByTeam(99999);
        assertPass("getPlayersByTeam non-existent returns empty", empty.isEmpty());

        // DELETE
        boolean deleted = dao.deletePlayer(playerId);
        assertPass("deletePlayer", deleted);
        boolean deleted2 = dao.deletePlayer(freeAgentId);
        assertPass("deletePlayer (free agent)", deleted2);

        // DELETE non-existent
        assertPass("deletePlayer non-existent returns false", !dao.deletePlayer(99999));

        // GET non-existent
        assertPass("getPlayerById non-existent returns null", dao.getPlayerById(99999) == null);
    }

    /* ==================== MATCH DAO ==================== */

    private static void testMatchDAO() {
        section("MatchDAO");

        MatchDAO dao = new MatchDAO();

        // CREATE
        int matchId = dao.createMatch(new Match(1, 1, 2, LocalDate.of(2025, 6, 15)));
        assertPass("createMatch", matchId > 0);

        // READ
        Match m = dao.getMatchById(matchId);
        assertPass("getMatchById", m != null && m.getSportId() == 1);

        // UPDATE
        m.setMatchDate(LocalDate.of(2025, 7, 20));
        boolean updated = dao.updateMatch(m);
        assertPass("updateMatch", updated);
        Match updatedMatch = dao.getMatchById(matchId);
        assertPass("updateMatch verified", updatedMatch != null && updatedMatch.getMatchDate().equals(LocalDate.of(2025, 7, 20)));

        // GET ALL
        List<Match> all = dao.getAllMatches();
        assertPass("getAllMatches", all.size() >= 6);

        // GET BY SPORT
        List<Match> cricket = dao.getMatchesBySport(1);
        assertPass("getMatchesBySport (Cricket)", cricket.size() >= 2);

        List<Match> empty = dao.getMatchesBySport(99999);
        assertPass("getMatchesBySport non-existent returns empty", empty.isEmpty());

        // DELETE
        boolean deleted = dao.deleteMatch(matchId);
        assertPass("deleteMatch", deleted);

        // DELETE non-existent
        assertPass("deleteMatch non-existent returns false", !dao.deleteMatch(99999));

        // GET non-existent
        assertPass("getMatchById non-existent returns null", dao.getMatchById(99999) == null);
    }

    /* ==================== PLAYER STATS DAO ==================== */

    private static void testPlayerStatsDAO() {
        section("PlayerStatsDAO");

        PlayerStatsDAO dao = new PlayerStatsDAO();

        // CREATE
        int statId = dao.createStats(new PlayerStats(1, 2, 50, 3, 1));
        assertPass("createStats", statId > 0);

        // READ
        PlayerStats ps = dao.getStatsById(statId);
        assertPass("getStatsById", ps != null && ps.getScore() == 50);

        // UPDATE
        ps.setScore(55);
        ps.setAssists(4);
        boolean updated = dao.updateStats(ps);
        assertPass("updateStats", updated);
        PlayerStats updatedStats = dao.getStatsById(statId);
        assertPass("updateStats verified", updatedStats != null && updatedStats.getScore() == 55 && updatedStats.getAssists() == 4);

        // GET ALL
        List<PlayerStats> all = dao.getAllStats();
        assertPass("getAllStats", all.size() >= 7);

        // GET BY PLAYER
        List<PlayerStats> kohli = dao.getStatsByPlayer(1);
        assertPass("getStatsByPlayer (Kohli)", kohli.size() >= 1);

        List<PlayerStats> empty = dao.getStatsByPlayer(99999);
        assertPass("getStatsByPlayer non-existent returns empty", empty.isEmpty());

        // DELETE
        boolean deleted = dao.deleteStats(statId);
        assertPass("deleteStats", deleted);

        // DELETE non-existent
        assertPass("deleteStats non-existent returns false", !dao.deleteStats(99999));

        // GET non-existent
        assertPass("getStatsById non-existent returns null", dao.getStatsById(99999) == null);
    }

    /* ==================== ANALYTICAL QUERIES ==================== */

    private static void testAnalyticalQueries() {
        section("Analytical Queries");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Query 1: Top Players by Total Score
            ResultSet rs1 = stmt.executeQuery(
                "SELECT p.name, SUM(ps.score) AS total_score " +
                "FROM players p JOIN player_stats ps ON p.player_id = ps.player_id " +
                "GROUP BY p.player_id ORDER BY total_score DESC");
            boolean hasResults = rs1.next();
            assertPass("Query 1: Top Players by Total Score returns results", hasResults);
            if (hasResults) {
                String topPlayer = rs1.getString("name");
                assertPass("Query 1: Virat Kohli is top scorer", topPlayer.equals("Virat Kohli"));
                int topScore = rs1.getInt("total_score");
                assertPass("Query 1: Top score is 85", topScore == 85);
            }

            // Query 2: Matches by Sport
            ResultSet rs2 = stmt.executeQuery(
                "SELECT s.sport_name, m.match_id, m.match_date " +
                "FROM matches m JOIN sports s ON m.sport_id = s.sport_id");
            int matchCount = 0;
            while (rs2.next()) matchCount++;
            assertPass("Query 2: Matches by Sport returns 6 matches", matchCount == 6);

            // Query 3: Player Performance Averages
            ResultSet rs3 = stmt.executeQuery(
                "SELECT p.name, AVG(ps.score) AS avg_score, AVG(ps.assists) AS avg_assists " +
                "FROM players p JOIN player_stats ps ON p.player_id = ps.player_id " +
                "GROUP BY p.player_id");
            int playerCount = 0;
            while (rs3.next()) playerCount++;
            assertPass("Query 3: Player Averages returns 7 players", playerCount == 7);

            // Query 4: Team Match Count
            ResultSet rs4 = stmt.executeQuery(
                "SELECT t.team_name, COUNT(m.match_id) AS matches_played " +
                "FROM teams t JOIN matches m ON t.team_id = m.team1_id OR t.team_id = m.team2_id " +
                "GROUP BY t.team_id");
            int teamCount = 0;
            while (rs4.next()) teamCount++;
            assertPass("Query 4: Team Match Count returns 6 teams", teamCount == 6);

            // Query 5: Highest Scoring Match
            ResultSet rs5 = stmt.executeQuery(
                "SELECT m.match_id, SUM(ps.score) AS total_score " +
                "FROM matches m JOIN player_stats ps ON m.match_id = ps.match_id " +
                "GROUP BY m.match_id ORDER BY total_score DESC LIMIT 1");
            assertPass("Query 5: Highest Scoring Match returns result", rs5.next());
            if (rs5.isBeforeFirst() || rs5.getRow() == 0) {
                rs5.next();
            }
            int highestMatchId = rs5.getInt("match_id");
            int highestScore = rs5.getInt("total_score");
            assertPass("Query 5: Match #1 is highest scoring", highestMatchId == 1);
            assertPass("Query 5: Highest score is 215", highestScore == 215);

            // Query 6: Players by Sport
            ResultSet rs6 = stmt.executeQuery(
                "SELECT s.sport_name, p.name " +
                "FROM players p JOIN teams t ON p.team_id = t.team_id " +
                "JOIN sports s ON t.sport_id = s.sport_id " +
                "ORDER BY s.sport_name");
            int totalPlayers = 0;
            while (rs6.next()) totalPlayers++;
            assertPass("Query 6: Players by Sport returns 12 players", totalPlayers == 12);

        } catch (SQLException e) {
            fail("Analytical Queries", e.getMessage());
        }
    }

    /* ==================== FOREIGN KEY CONSTRAINTS ==================== */

    private static void testForeignKeyConstraints() {
        section("Foreign Key Constraints");

        // FK: teams.sport_id -> sports.sport_id (ON DELETE CASCADE)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create a sport with a team
            stmt.executeUpdate("INSERT INTO sports (sport_name) VALUES ('Test Sport FK')");
            ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs.next();
            int sportId = rs.getInt(1);

            stmt.executeUpdate("INSERT INTO teams (team_name, sport_id) VALUES ('Test Team FK', " + sportId + ")");
            ResultSet rs2 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs2.next();
            int teamId = rs2.getInt(1);

            // Delete sport -> team should be cascade deleted
            stmt.executeUpdate("DELETE FROM sports WHERE sport_id = " + sportId);

            ResultSet check = stmt.executeQuery("SELECT COUNT(*) FROM teams WHERE team_id = " + teamId);
            check.next();
            assertPass("FK CASCADE: Deleting sport deletes team", check.getInt(1) == 0);

        } catch (SQLException e) {
            fail("FK CASCADE test", e.getMessage());
        }

        // FK: players.team_id -> teams.team_id (ON DELETE SET NULL)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("INSERT INTO sports (sport_name) VALUES ('Test Sport FK2')");
            ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs.next();
            int sportId = rs.getInt(1);

            stmt.executeUpdate("INSERT INTO teams (team_name, sport_id) VALUES ('Test Team FK2', " + sportId + ")");
            ResultSet rs2 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs2.next();
            int teamId = rs2.getInt(1);

            stmt.executeUpdate("INSERT INTO players (name, age, team_id) VALUES ('Test Player FK', 25, " + teamId + ")");
            ResultSet rs3 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs3.next();
            int playerId = rs3.getInt(1);

            // Delete team -> player's team_id should be SET NULL
            stmt.executeUpdate("DELETE FROM teams WHERE team_id = " + teamId);

            ResultSet check = stmt.executeQuery("SELECT team_id FROM players WHERE player_id = " + playerId);
            check.next();
            Object teamIdVal = check.getObject("team_id");
            assertPass("FK SET NULL: Deleting team sets player.team_id to NULL", teamIdVal == null);

            // Cleanup
            stmt.executeUpdate("DELETE FROM players WHERE player_id = " + playerId);
            stmt.executeUpdate("DELETE FROM sports WHERE sport_id = " + sportId);

        } catch (SQLException e) {
            fail("FK SET NULL test", e.getMessage());
        }

        // FK: matches.team_id -> teams.team_id (ON DELETE RESTRICT)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Try to delete a team that is referenced in matches (India, team_id=1)
            boolean threw = false;
            try {
                stmt.executeUpdate("DELETE FROM teams WHERE team_id = 1");
            } catch (SQLException e) {
                threw = true;
            }
            assertPass("FK RESTRICT: Cannot delete team referenced in matches", threw);

        } catch (SQLException e) {
            fail("FK RESTRICT test", e.getMessage());
        }

        // FK: player_stats -> players (ON DELETE CASCADE)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("INSERT INTO players (name, age, team_id) VALUES ('Stats Test Player', 28, 1)");
            ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs.next();
            int playerId = rs.getInt(1);

            stmt.executeUpdate("INSERT INTO matches (sport_id, team1_id, team2_id, match_date) VALUES (1, 1, 2, '2025-01-01')");
            ResultSet rs2 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs2.next();
            int matchId = rs2.getInt(1);

            stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (" + playerId + ", " + matchId + ", 40, 2, 0)");
            ResultSet rs3 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs3.next();
            int statId = rs3.getInt(1);

            // Delete player -> stats should cascade
            stmt.executeUpdate("DELETE FROM players WHERE player_id = " + playerId);

            ResultSet check = stmt.executeQuery("SELECT COUNT(*) FROM player_stats WHERE stat_id = " + statId);
            check.next();
            assertPass("FK CASCADE: Deleting player deletes player_stats", check.getInt(1) == 0);

            // Cleanup match
            stmt.executeUpdate("DELETE FROM matches WHERE match_id = " + matchId);

        } catch (SQLException e) {
            fail("FK CASCADE player_stats test", e.getMessage());
        }

        // FK: player_stats -> matches (ON DELETE CASCADE)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("INSERT INTO players (name, age, team_id) VALUES ('Stats Test Player 2', 29, 1)");
            ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs.next();
            int playerId = rs.getInt(1);

            stmt.executeUpdate("INSERT INTO matches (sport_id, team1_id, team2_id, match_date) VALUES (1, 1, 2, '2025-02-01')");
            ResultSet rs2 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs2.next();
            int matchId = rs2.getInt(1);

            stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (" + playerId + ", " + matchId + ", 35, 1, 0)");
            ResultSet rs3 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs3.next();
            int statId = rs3.getInt(1);

            // Delete match -> stats should cascade
            stmt.executeUpdate("DELETE FROM matches WHERE match_id = " + matchId);

            ResultSet check = stmt.executeQuery("SELECT COUNT(*) FROM player_stats WHERE stat_id = " + statId);
            check.next();
            assertPass("FK CASCADE: Deleting match deletes player_stats", check.getInt(1) == 0);

            // Cleanup
            stmt.executeUpdate("DELETE FROM players WHERE player_id = " + playerId);

        } catch (SQLException e) {
            fail("FK CASCADE match test", e.getMessage());
        }
    }

    /* ==================== CHECK CONSTRAINTS ==================== */

    private static void testCheckConstraints() {
        section("Check Constraints");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // CHECK (age > 0)
            boolean ageFail = false;
            try {
                stmt.executeUpdate("INSERT INTO players (name, age, team_id) VALUES ('Bad Age', -5, 1)");
            } catch (SQLException e) {
                ageFail = true;
            }
            assertPass("CHECK age > 0 rejects negative age", ageFail);

            // CHECK (score >= 0)
            boolean scoreFail = false;
            try {
                stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (1, 1, -10, 0, 0)");
            } catch (SQLException e) {
                scoreFail = true;
            }
            assertPass("CHECK score >= 0 rejects negative score", scoreFail);

            // CHECK (assists >= 0)
            boolean assistsFail = false;
            try {
                stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (1, 1, 0, -5, 0)");
            } catch (SQLException e) {
                assistsFail = true;
            }
            assertPass("CHECK assists >= 0 rejects negative assists", assistsFail);

            // CHECK (wickets >= 0)
            boolean wicketsFail = false;
            try {
                stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (1, 1, 0, 0, -3)");
            } catch (SQLException e) {
                wicketsFail = true;
            }
            assertPass("CHECK wickets >= 0 rejects negative wickets", wicketsFail);

            // Valid values should work
            stmt.executeUpdate("INSERT INTO players (name, age, team_id) VALUES ('Valid Age', 25, 1)");
            ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs.next();
            int playerId = rs.getInt(1);

            stmt.executeUpdate("INSERT INTO matches (sport_id, team1_id, team2_id, match_date) VALUES (1, 1, 2, '2025-03-01')");
            ResultSet rs2 = stmt.executeQuery("SELECT LAST_INSERT_ID()");
            rs2.next();
            int matchId = rs2.getInt(1);

            stmt.executeUpdate("INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES (" + playerId + ", " + matchId + ", 0, 0, 0)");
            assertPass("CHECK allows zero values for score/assists/wickets", true);

            // Cleanup
            stmt.executeUpdate("DELETE FROM player_stats WHERE match_id = " + matchId);
            stmt.executeUpdate("DELETE FROM matches WHERE match_id = " + matchId);
            stmt.executeUpdate("DELETE FROM players WHERE player_id = " + playerId);

        } catch (SQLException e) {
            fail("Check Constraints test", e.getMessage());
        }
    }

    /* ==================== EDGE CASES ==================== */

    private static void testEdgeCases() {
        section("Edge Cases");

        SportDAO sportDAO = new SportDAO();
        TeamDAO teamDAO = new TeamDAO();
        PlayerDAO playerDAO = new PlayerDAO();
        MatchDAO matchDAO = new MatchDAO();
        PlayerStatsDAO statsDAO = new PlayerStatsDAO();

        // UNIQUE constraint on sport_name
        boolean dupFail = false;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO sports (sport_name) VALUES ('Cricket')");
        } catch (SQLException e) {
            dupFail = true;
        }
        assertPass("UNIQUE sport_name rejects duplicate", dupFail);

        // getSportById with id=0
        assertPass("getSportById(0) returns null", sportDAO.getSportById(0) == null);

        // getTeamById with id=0
        assertPass("getTeamById(0) returns null", teamDAO.getTeamById(0) == null);

        // getPlayerById with id=0
        assertPass("getPlayerById(0) returns null", playerDAO.getPlayerById(0) == null);

        // getMatchById with id=0
        assertPass("getMatchById(0) returns null", matchDAO.getMatchById(0) == null);

        // getStatsById with id=0
        assertPass("getStatsById(0) returns null", statsDAO.getStatsById(0) == null);

        // createSport with empty string
        int emptySportId = sportDAO.createSport(new Sport(""));
        assertPass("createSport with empty name succeeds (no NOT NULL violation)", emptySportId > 0);
        sportDAO.deleteSport(emptySportId);

        // createSport with very long name
        String longName = "A".repeat(50);
        int longSportId = sportDAO.createSport(new Sport(longName));
        assertPass("createSport with 50-char name succeeds", longSportId > 0);
        sportDAO.deleteSport(longSportId);

        // getAllSports returns non-null list
        assertPass("getAllSports returns non-null list", sportDAO.getAllSports() != null);

        // getAllTeams returns non-null list
        assertPass("getAllTeams returns non-null list", teamDAO.getAllTeams() != null);

        // getAllPlayers returns non-null list
        assertPass("getAllPlayers returns non-null list", playerDAO.getAllPlayers() != null);

        // getAllMatches returns non-null list
        assertPass("getAllMatches returns non-null list", matchDAO.getAllMatches() != null);

        // getAllStats returns non-null list
        assertPass("getAllStats returns non-null list", statsDAO.getAllStats() != null);

        // Player with null team_id (free agent)
        int freeAgent = playerDAO.createPlayer(new Player("Free Agent Edge", 30, null));
        assertPass("createPlayer with null team_id", freeAgent > 0);
        Player retrieved = playerDAO.getPlayerById(freeAgent);
        assertPass("getPlayerById returns player with null team_id", retrieved != null && retrieved.getTeamId() == null);
        playerDAO.deletePlayer(freeAgent);

        // Match with null teams
        int nullTeamMatch = matchDAO.createMatch(new Match(1, null, null, LocalDate.of(2025, 12, 25)));
        assertPass("createMatch with null team1_id and team2_id", nullTeamMatch > 0);
        matchDAO.deleteMatch(nullTeamMatch);

        // PlayerStats with null values
        int nullStats = statsDAO.createStats(new PlayerStats(1, 1, null, null, null));
        assertPass("createStats with null score/assists/wickets", nullStats > 0);
        statsDAO.deleteStats(nullStats);
    }

    /* ==================== UTILITY METHODS ==================== */

    private static void section(String name) {
        System.out.println("\n--- " + name + " ---");
    }

    private static void assertPass(String testName, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName);
            failed++;
        }
    }

    private static void fail(String testName, String reason) {
        System.out.println("  FAIL: " + testName + " - " + reason);
        failed++;
    }
}
