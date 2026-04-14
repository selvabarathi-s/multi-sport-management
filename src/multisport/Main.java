package multisport;

import multisport.dao.*;
import multisport.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class Main extends JFrame {
    private static final Color NAVY = new Color(18, 35, 54);
    private static final Color GREEN = new Color(24, 116, 82);
    private static final Color ORANGE = new Color(210, 91, 45);
    private static final Color BACKGROUND = new Color(244, 247, 250);
    private static final Color TEXT_MUTED = new Color(92, 105, 117);

    private final SportDAO sportDAO = new SportDAO();
    private final TeamDAO teamDAO = new TeamDAO();
    private final PlayerDAO playerDAO = new PlayerDAO();
    private final MatchDAO matchDAO = new MatchDAO();
    private final PlayerStatsDAO statsDAO = new PlayerStatsDAO();
    private final Map<String, User> users = createUsers();

    private User currentUser;
    private JLabel statusLabel;
    private JLabel sportsCount;
    private JLabel teamsCount;
    private JLabel playersCount;
    private JLabel matchesCount;
    private JLabel statsCount;
    private final DefaultTableModel sportsModel = tableModel("ID", "Sport Name");
    private final DefaultTableModel teamsModel = tableModel("ID", "Team Name", "Sport ID");
    private final DefaultTableModel playersModel = tableModel("ID", "Player Name", "Age", "Team ID");
    private final DefaultTableModel matchesModel = tableModel("ID", "Sport ID", "Team 1 ID", "Team 2 ID", "Match Date");
    private final DefaultTableModel statsModel = tableModel("ID", "Player ID", "Match ID", "Score", "Assists", "Wickets");
    private final DefaultTableModel analyticsModel = tableModel("Report", "Value 1", "Value 2", "Value 3");

    private record User(String username, String password, String role, String displayName) {}

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Use the default Swing look and feel when Nimbus is unavailable.
        }
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.loginAndOpen();
        });
    }

    public Main() {
        super("Multi-Sport Event Manager - Java JDBC Interface");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
    }

    private void loginAndOpen() {
        if (!databaseAvailable()) {
            JOptionPane.showMessageDialog(this,
                "Database connection failed.\nCheck MySQL, import sql/schema.sql, and verify DatabaseConnection.java.",
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }

        currentUser = showLoginDialog();
        if (currentUser == null) {
            dispose();
            return;
        }

        buildInterface();
        setVisible(true);
    }

    private User showLoginDialog() {
        JTextField username = new JTextField(16);
        JPasswordField password = new JPasswordField(16);
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel title = new JLabel("Multi-Sport Event Manager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Login to manage fixtures, rosters, and player performance.");
        subtitle.setForeground(TEXT_MUTED);
        panel.add(title);
        panel.add(subtitle);
        panel.add(new JLabel("Username"));
        panel.add(username);
        panel.add(new JLabel("Password"));
        panel.add(password);
        panel.add(new JLabel("Demo: admin/admin123, organizer/org123, scorer/score123, viewer/view123"));

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return null;
            }
            User user = users.get(username.getText().trim().toLowerCase());
            if (user != null && user.password().equals(new String(password.getPassword()))) {
                return user;
            }
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void buildInterface() {
        getContentPane().removeAll();
        JLabel title = new JLabel("<html><span style='font-size:22px'>Multi-Sport Event Manager</span><br><span style='font-size:12px'>Java Swing + JDBC desktop application</span></html>");
        title.setForeground(Color.WHITE);

        JLabel identity = new JLabel(currentUser.displayName() + " - " + roleLabel(currentUser.role()));
        identity.setOpaque(true);
        identity.setBackground(GREEN);
        identity.setForeground(Color.WHITE);
        identity.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton refresh = button("Refresh", this::refreshAll);
        JButton logout = button("Logout", () -> {});
        logout.addActionListener(event -> {
            setVisible(false);
            loginAndOpen();
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(NAVY);
        top.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(identity);
        right.add(refresh);
        right.add(logout);
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 14f));
        tabs.addTab("Dashboard", buildDashboardPanel());
        tabs.addTab("Sports", buildSportsPanel());
        tabs.addTab("Teams", buildTeamsPanel());
        tabs.addTab("Players", buildPlayersPanel());
        tabs.addTab("Matches", buildMatchesPanel());
        tabs.addTab("Stats", buildStatsPanel());
        tabs.addTab("Analytics", buildAnalyticsPanel());

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        statusLabel.setForeground(TEXT_MUTED);
        add(statusLabel, BorderLayout.SOUTH);
        refreshAll();
        revalidate();
        repaint();
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Quick database summary and usage guide.");
        subtitle.setForeground(TEXT_MUTED);
        JPanel intro = new JPanel(new GridLayout(0, 1, 2, 2));
        intro.setOpaque(false);
        intro.add(title);
        intro.add(subtitle);

        sportsCount = metricValue();
        teamsCount = metricValue();
        playersCount = metricValue();
        matchesCount = metricValue();
        statsCount = metricValue();
        JPanel metrics = new JPanel(new GridLayout(1, 5, 14, 14));
        metrics.setOpaque(false);
        metrics.add(metricCard("Sports", sportsCount));
        metrics.add(metricCard("Teams", teamsCount));
        metrics.add(metricCard("Players", playersCount));
        metrics.add(metricCard("Matches", matchesCount));
        metrics.add(metricCard("Stat Lines", statsCount));

        JPanel guide = cardPanel(new BorderLayout(8, 8));
        JLabel guideTitle = new JLabel("How to use it");
        guideTitle.setFont(guideTitle.getFont().deriveFont(Font.BOLD, 18f));
        JLabel guideText = new JLabel("<html>Select a module tab, choose a row, then use Add, Update Selected, or Delete Selected. Tables are sortable. Role permissions match the web UI: Admin manages all, Organizer manages events, Scorer manages stats, Viewer reads only.</html>");
        guideText.setForeground(TEXT_MUTED);
        guide.add(guideTitle, BorderLayout.NORTH);
        guide.add(guideText, BorderLayout.CENTER);

        panel.add(intro, BorderLayout.NORTH);
        panel.add(metrics, BorderLayout.CENTER);
        panel.add(guide, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSportsPanel() {
        JTable table = new JTable(sportsModel);
        return tablePanel(table, actions(
            button("Add", () -> requireAdmin(() -> {
                String name = askText("Sport name", "");
                sportDAO.createSport(new Sport(name));
                refreshAll();
            })),
            button("Update Selected", () -> requireAdmin(() -> {
                int id = selectedId(table);
                Sport sport = sportDAO.getSportById(id);
                String name = askText("Sport name", sport.getSportName());
                sport.setSportName(name);
                sportDAO.updateSport(sport);
                refreshAll();
            })),
            button("Delete Selected", () -> requireAdmin(() -> {
                int id = selectedId(table);
                if (confirmDelete()) {
                    sportDAO.deleteSport(id);
                    refreshAll();
                }
            }))
        ));
    }

    private JPanel buildTeamsPanel() {
        JTable table = new JTable(teamsModel);
        return tablePanel(table, actions(
            button("Add", () -> requireManageEvents(() -> {
                Team team = teamDialog(null);
                teamDAO.createTeam(team);
                refreshAll();
            })),
            button("Update Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                Team existing = teamDAO.getTeamById(id);
                Team team = teamDialog(existing);
                team.setTeamId(id);
                teamDAO.updateTeam(team);
                refreshAll();
            })),
            button("Delete Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                if (confirmDelete()) {
                    teamDAO.deleteTeam(id);
                    refreshAll();
                }
            }))
        ));
    }

    private JPanel buildPlayersPanel() {
        JTable table = new JTable(playersModel);
        return tablePanel(table, actions(
            button("Add", () -> requireManageEvents(() -> {
                playerDAO.createPlayer(playerDialog(null));
                refreshAll();
            })),
            button("Update Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                Player player = playerDialog(playerDAO.getPlayerById(id));
                player.setPlayerId(id);
                playerDAO.updatePlayer(player);
                refreshAll();
            })),
            button("Delete Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                if (confirmDelete()) {
                    playerDAO.deletePlayer(id);
                    refreshAll();
                }
            }))
        ));
    }

    private JPanel buildMatchesPanel() {
        JTable table = new JTable(matchesModel);
        return tablePanel(table, actions(
            button("Add", () -> requireManageEvents(() -> {
                matchDAO.createMatch(matchDialog(null));
                refreshAll();
            })),
            button("Update Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                Match match = matchDialog(matchDAO.getMatchById(id));
                match.setMatchId(id);
                matchDAO.updateMatch(match);
                refreshAll();
            })),
            button("Delete Selected", () -> requireManageEvents(() -> {
                int id = selectedId(table);
                if (confirmDelete()) {
                    matchDAO.deleteMatch(id);
                    refreshAll();
                }
            }))
        ));
    }

    private JPanel buildStatsPanel() {
        JTable table = new JTable(statsModel);
        return tablePanel(table, actions(
            button("Add", () -> requireRecordStats(() -> {
                statsDAO.createStats(statsDialog(null));
                refreshAll();
            })),
            button("Update Selected", () -> requireRecordStats(() -> {
                int id = selectedId(table);
                PlayerStats stats = statsDialog(statsDAO.getStatsById(id));
                stats.setStatId(id);
                statsDAO.updateStats(stats);
                refreshAll();
            })),
            button("Delete Selected", () -> requireRecordStats(() -> {
                int id = selectedId(table);
                if (confirmDelete()) {
                    statsDAO.deleteStats(id);
                    refreshAll();
                }
            }))
        ));
    }

    private JPanel buildAnalyticsPanel() {
        JTable table = new JTable(analyticsModel);
        return tablePanel(table, actions(button("Run Analytical Queries", this::refreshAnalytics)));
    }

    private Team teamDialog(Team existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getTeamName(), 18);
        JTextField sportId = new JTextField(existing == null ? "" : String.valueOf(existing.getSportId()), 8);
        showForm("Team", new String[] {"Team name", "Sport ID"}, new JComponent[] {name, sportId});
        return new Team(required(name, "Team name"), intValue(sportId, "Sport ID"));
    }

    private Player playerDialog(Player existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName(), 18);
        JTextField age = new JTextField(existing == null ? "" : String.valueOf(existing.getAge()), 8);
        JTextField teamId = new JTextField(existing == null || existing.getTeamId() == null ? "" : String.valueOf(existing.getTeamId()), 8);
        showForm("Player", new String[] {"Player name", "Age", "Team ID"}, new JComponent[] {name, age, teamId});
        return new Player(required(name, "Player name"), intValue(age, "Age"), intValue(teamId, "Team ID"));
    }

    private Match matchDialog(Match existing) {
        JTextField sportId = new JTextField(existing == null ? "" : String.valueOf(existing.getSportId()), 8);
        JTextField team1Id = new JTextField(existing == null || existing.getTeam1Id() == null ? "" : String.valueOf(existing.getTeam1Id()), 8);
        JTextField team2Id = new JTextField(existing == null || existing.getTeam2Id() == null ? "" : String.valueOf(existing.getTeam2Id()), 8);
        JTextField date = new JTextField(existing == null ? "" : String.valueOf(existing.getMatchDate()), 10);
        showForm("Match", new String[] {"Sport ID", "Team 1 ID", "Team 2 ID", "Date YYYY-MM-DD"}, new JComponent[] {sportId, team1Id, team2Id, date});
        return new Match(intValue(sportId, "Sport ID"), intValue(team1Id, "Team 1 ID"), intValue(team2Id, "Team 2 ID"), dateValue(date, "Match date"));
    }

    private PlayerStats statsDialog(PlayerStats existing) {
        JTextField playerId = new JTextField(existing == null || existing.getPlayerId() == null ? "" : String.valueOf(existing.getPlayerId()), 8);
        JTextField matchId = new JTextField(existing == null || existing.getMatchId() == null ? "" : String.valueOf(existing.getMatchId()), 8);
        JTextField score = new JTextField(existing == null || existing.getScore() == null ? "" : String.valueOf(existing.getScore()), 8);
        JTextField assists = new JTextField(existing == null || existing.getAssists() == null ? "" : String.valueOf(existing.getAssists()), 8);
        JTextField wickets = new JTextField(existing == null || existing.getWickets() == null ? "" : String.valueOf(existing.getWickets()), 8);
        showForm("Player Stats", new String[] {"Player ID", "Match ID", "Score", "Assists", "Wickets"}, new JComponent[] {playerId, matchId, score, assists, wickets});
        return new PlayerStats(intValue(playerId, "Player ID"), intValue(matchId, "Match ID"), intValue(score, "Score"), intValue(assists, "Assists"), intValue(wickets, "Wickets"));
    }

    private void showForm(String title, String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }
        int result = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            throw new IllegalArgumentException("Action cancelled.");
        }
    }

    private JPanel tablePanel(JTable table, JPanel actions) {
        styleTable(table);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = cardPanel(new BorderLayout(10, 10));
        JTextField search = new JTextField(22);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) table.getModel());
        table.setRowSorter(sorter);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String value = search.getText().trim();
                sorter.setRowFilter(value.isEmpty() ? null : RowFilter.regexFilter("(?i)" + value));
            }
        });
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search"));
        searchPanel.add(search);
        top.add(searchPanel, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(224, 230, 235)));
        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel actions(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setOpaque(false);
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    private JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        if (label.toLowerCase().contains("delete")) {
            button.setBackground(ORANGE);
            button.setForeground(Color.WHITE);
        } else if (label.equals("Add") || label.contains("Add") || label.contains("Update") || label.equals("Refresh") || label.contains("Run")) {
            button.setBackground(GREEN);
            button.setForeground(Color.WHITE);
        }
        button.addActionListener(event -> {
            try {
                setStatus("Working...");
                action.run();
                setStatus("Done");
            } catch (IllegalArgumentException e) {
                if (!"Action cancelled.".equals(e.getMessage())) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Input", JOptionPane.WARNING_MESSAGE);
                }
                setStatus("Ready");
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Ready");
            }
        });
        return button;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(216, 238, 229));
        table.setSelectionForeground(NAVY);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(new Color(232, 237, 242));
        table.getTableHeader().setForeground(NAVY);
    }

    private JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 231, 237)),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        return panel;
    }

    private JLabel metricValue() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 30f));
        label.setForeground(GREEN);
        return label;
    }

    private JPanel metricCard(String label, JLabel valueLabel) {
        JPanel card = cardPanel(new BorderLayout(4, 4));
        JLabel name = new JLabel(label, SwingConstants.CENTER);
        name.setForeground(TEXT_MUTED);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(name, BorderLayout.SOUTH);
        return card;
    }

    private DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void refreshAll() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setStatus("Refreshing data...");
        clear(sportsModel);
        java.util.List<Sport> sports = sportDAO.getAllSports();
        java.util.List<Team> teams = teamDAO.getAllTeams();
        java.util.List<Player> players = playerDAO.getAllPlayers();
        java.util.List<Match> matches = matchDAO.getAllMatches();
        java.util.List<PlayerStats> stats = statsDAO.getAllStats();

        for (Sport sport : sports) {
            sportsModel.addRow(new Object[] {sport.getSportId(), sport.getSportName()});
        }
        clear(teamsModel);
        for (Team team : teams) {
            teamsModel.addRow(new Object[] {team.getTeamId(), team.getTeamName(), team.getSportId()});
        }
        clear(playersModel);
        for (Player player : players) {
            playersModel.addRow(new Object[] {player.getPlayerId(), player.getName(), player.getAge(), player.getTeamId()});
        }
        clear(matchesModel);
        for (Match match : matches) {
            matchesModel.addRow(new Object[] {match.getMatchId(), match.getSportId(), match.getTeam1Id(), match.getTeam2Id(), match.getMatchDate()});
        }
        clear(statsModel);
        for (PlayerStats stat : stats) {
            statsModel.addRow(new Object[] {stat.getStatId(), stat.getPlayerId(), stat.getMatchId(), stat.getScore(), stat.getAssists(), stat.getWickets()});
        }
        updateMetric(sportsCount, sports.size());
        updateMetric(teamsCount, teams.size());
        updateMetric(playersCount, players.size());
        updateMetric(matchesCount, matches.size());
        updateMetric(statsCount, stats.size());
        refreshAnalytics();
        setStatus("Loaded " + sports.size() + " sports, " + teams.size() + " teams, " + players.size() + " players.");
        setCursor(Cursor.getDefaultCursor());
    }

    private void refreshAnalytics() {
        clear(analyticsModel);
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            addAnalyticsRows(stmt, "Top Players by Total Score", "SELECT p.name, SUM(ps.score) AS total_score FROM players p JOIN player_stats ps ON p.player_id = ps.player_id GROUP BY p.player_id, p.name ORDER BY total_score DESC", "name", "total_score");
            addAnalyticsRows(stmt, "Matches by Sport", "SELECT s.sport_name, m.match_id, m.match_date FROM matches m JOIN sports s ON m.sport_id = s.sport_id ORDER BY s.sport_name, m.match_date", "sport_name", "match_id", "match_date");
            addAnalyticsRows(stmt, "Player Performance Averages", "SELECT p.name, AVG(ps.score) AS avg_score, AVG(ps.assists) AS avg_assists FROM players p JOIN player_stats ps ON p.player_id = ps.player_id GROUP BY p.player_id, p.name", "name", "avg_score", "avg_assists");
            addAnalyticsRows(stmt, "Team Match Count", "SELECT t.team_name, COUNT(m.match_id) AS matches_played FROM teams t JOIN matches m ON t.team_id = m.team1_id OR t.team_id = m.team2_id GROUP BY t.team_id, t.team_name", "team_name", "matches_played");
            addAnalyticsRows(stmt, "Highest Scoring Match", "SELECT m.match_id, SUM(ps.score) AS total_score FROM matches m JOIN player_stats ps ON m.match_id = ps.match_id GROUP BY m.match_id ORDER BY total_score DESC LIMIT 1", "match_id", "total_score");
            addAnalyticsRows(stmt, "Players by Sport", "SELECT s.sport_name, p.name FROM players p JOIN teams t ON p.team_id = t.team_id JOIN sports s ON t.sport_id = s.sport_id ORDER BY s.sport_name, p.name", "sport_name", "name");
        } catch (SQLException e) {
            analyticsModel.addRow(new Object[] {"Query failed", e.getMessage(), "", ""});
        }
    }

    private void addAnalyticsRows(Statement stmt, String title, String sql, String... columns) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                Object[] row = new Object[] {title, "", "", ""};
                for (int i = 0; i < columns.length && i < 3; i++) {
                    row[i + 1] = rs.getString(columns[i]);
                }
                analyticsModel.addRow(row);
            }
            if (!found) {
                analyticsModel.addRow(new Object[] {title, "No rows found", "", ""});
            }
        }
    }

    private void requireAdmin(Runnable action) {
        if (!isAdmin()) {
            deny("Only Admin can manage sports.");
            return;
        }
        action.run();
    }

    private void requireManageEvents(Runnable action) {
        if (!(isAdmin() || "organizer".equals(currentUser.role()))) {
            deny("Only Admin and Organizer can manage this data.");
            return;
        }
        action.run();
    }

    private void requireRecordStats(Runnable action) {
        if (!(isAdmin() || "organizer".equals(currentUser.role()) || "scorer".equals(currentUser.role()))) {
            deny("Only Admin, Organizer, and Scorer can manage stats.");
            return;
        }
        action.run();
    }

    private boolean isAdmin() {
        return currentUser != null && "admin".equals(currentUser.role());
    }

    private void deny(String message) {
        JOptionPane.showMessageDialog(this, message, "Access Denied", JOptionPane.WARNING_MESSAGE);
    }

    private int selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("Select a row first.");
        }
        return Integer.parseInt(table.getValueAt(row, 0).toString());
    }

    private String askText(String label, String currentValue) {
        String value = JOptionPane.showInputDialog(this, label, currentValue);
        if (value == null) {
            throw new IllegalArgumentException("Action cancelled.");
        }
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private boolean confirmDelete() {
        return JOptionPane.showConfirmDialog(this, "Delete selected record?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private String required(JTextField field, String label) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private int intValue(JTextField field, String label) {
        try {
            return Integer.parseInt(required(field, label));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private LocalDate dateValue(JTextField field, String label) {
        try {
            return LocalDate.parse(required(field, label));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + " must use YYYY-MM-DD format.");
        }
    }

    private boolean databaseAvailable() {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void clear(DefaultTableModel model) {
        model.setRowCount(0);
    }

    private void updateMetric(JLabel label, int value) {
        if (label != null) {
            label.setText(String.valueOf(value));
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private Map<String, User> createUsers() {
        Map<String, User> demoUsers = new HashMap<>();
        demoUsers.put("admin", new User("admin", "admin123", "admin", "Asha Admin"));
        demoUsers.put("organizer", new User("organizer", "org123", "organizer", "Omar Organizer"));
        demoUsers.put("scorer", new User("scorer", "score123", "scorer", "Sara Scorer"));
        demoUsers.put("viewer", new User("viewer", "view123", "viewer", "Vikram Viewer"));
        return demoUsers;
    }

    private String roleLabel(String role) {
        if ("admin".equals(role)) {
            return "Admin";
        }
        if ("organizer".equals(role)) {
            return "Organizer";
        }
        if ("scorer".equals(role)) {
            return "Scorer";
        }
        return "Viewer";
    }
}
