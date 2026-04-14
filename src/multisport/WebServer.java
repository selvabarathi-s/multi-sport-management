package multisport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import multisport.dao.MatchDAO;
import multisport.dao.PlayerDAO;
import multisport.dao.PlayerStatsDAO;
import multisport.dao.SportDAO;
import multisport.dao.TeamDAO;
import multisport.model.Match;
import multisport.model.Player;
import multisport.model.PlayerStats;
import multisport.model.Sport;
import multisport.model.Team;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {
    private static final int PORT = 8080;

    private final SportDAO sportDAO = new SportDAO();
    private final TeamDAO teamDAO = new TeamDAO();
    private final PlayerDAO playerDAO = new PlayerDAO();
    private final MatchDAO matchDAO = new MatchDAO();
    private final PlayerStatsDAO statsDAO = new PlayerStatsDAO();
    private final Map<String, User> users = createUsers();
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    private record User(String username, String password, String role, String displayName) {}

    public static void main(String[] args) throws IOException {
        new WebServer().start();
    }

    private void start() throws IOException {
        seedExampleData();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handleHome);
        server.createContext("/login", this::handleLogin);
        server.createContext("/logout", this::handleLogout);
        server.createContext("/sports", this::handleCreateSport);
        server.createContext("/teams", this::handleCreateTeam);
        server.createContext("/players", this::handleCreatePlayer);
        server.createContext("/matches", this::handleCreateMatch);
        server.createContext("/stats", this::handleCreateStats);
        server.setExecutor(null);
        server.start();
        System.out.println("Multi-Sport event manager is running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        User user = currentUser(exchange);
        if (user == null) {
            send(exchange, 200, "text/html; charset=utf-8", buildLoginPage(null));
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", buildPage(user));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 200, "text/html; charset=utf-8", buildLoginPage(null));
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        Map<String, String> form = readForm(exchange);
        User user = users.get(form.getOrDefault("username", "").trim().toLowerCase());
        if (user == null || !user.password().equals(form.getOrDefault("password", ""))) {
            send(exchange, 401, "text/html; charset=utf-8", buildLoginPage("Invalid username or password."));
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        exchange.getResponseHeaders().add("Set-Cookie", "MULTISPORT_SESSION=" + sessionId + "; HttpOnly; SameSite=Lax; Path=/");
        redirectHome(exchange);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String sessionId = sessionId(exchange);
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "MULTISPORT_SESSION=; Max-Age=0; HttpOnly; SameSite=Lax; Path=/");
        redirectHome(exchange);
    }

    private void handleCreateSport(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        if (!requireRole(exchange, "admin")) {
            return;
        }
        Map<String, String> form = readForm(exchange);
        String sportName = form.getOrDefault("sportName", "").trim();
        if (!sportName.isEmpty()) {
            sportDAO.createSport(new Sport(sportName));
        }
        redirectHome(exchange);
    }

    private void handleCreateTeam(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        if (!requireRole(exchange, "admin", "organizer")) {
            return;
        }
        Map<String, String> form = readForm(exchange);
        String teamName = form.getOrDefault("teamName", "").trim();
        int sportId = parseInt(form.get("sportId"), -1);
        if (!teamName.isEmpty() && sportId > 0) {
            teamDAO.createTeam(new Team(teamName, sportId));
        }
        redirectHome(exchange);
    }

    private void handleCreatePlayer(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        if (!requireRole(exchange, "admin", "organizer")) {
            return;
        }
        Map<String, String> form = readForm(exchange);
        String playerName = form.getOrDefault("playerName", "").trim();
        int age = parseInt(form.get("age"), -1);
        int teamId = parseInt(form.get("teamId"), -1);
        if (!playerName.isEmpty() && age > 0 && teamId > 0) {
            playerDAO.createPlayer(new Player(playerName, age, teamId));
        }
        redirectHome(exchange);
    }

    private void handleCreateMatch(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        if (!requireRole(exchange, "admin", "organizer")) {
            return;
        }
        Map<String, String> form = readForm(exchange);
        int sportId = parseInt(form.get("sportId"), -1);
        int team1Id = parseInt(form.get("team1Id"), -1);
        int team2Id = parseInt(form.get("team2Id"), -1);
        LocalDate matchDate = parseDate(form.get("matchDate"));
        if (sportId > 0 && team1Id > 0 && team2Id > 0 && team1Id != team2Id && matchDate != null) {
            matchDAO.createMatch(new Match(sportId, team1Id, team2Id, matchDate));
        }
        redirectHome(exchange);
    }

    private void handleCreateStats(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        if (!requireRole(exchange, "admin", "organizer", "scorer")) {
            return;
        }
        Map<String, String> form = readForm(exchange);
        int playerId = parseInt(form.get("playerId"), -1);
        int matchId = parseInt(form.get("matchId"), -1);
        int score = parseInt(form.get("score"), 0);
        int assists = parseInt(form.get("assists"), 0);
        int wickets = parseInt(form.get("wickets"), 0);
        if (playerId > 0 && matchId > 0 && score >= 0 && assists >= 0 && wickets >= 0) {
            statsDAO.createStats(new PlayerStats(playerId, matchId, score, assists, wickets));
        }
        redirectHome(exchange);
    }

    private String buildPage(User user) {
        List<Sport> sports = sportDAO.getAllSports();
        List<Team> teams = teamDAO.getAllTeams();
        List<Player> players = playerDAO.getAllPlayers();
        List<Match> matches = matchDAO.getAllMatches();
        List<PlayerStats> stats = statsDAO.getAllStats();
        Map<Integer, String> sportNames = sportNames(sports);
        Map<Integer, Team> teamMap = teamMap(teams);
        Map<Integer, String> teamNames = teamNames(teams);
        Map<Integer, String> playerNames = playerNames(players);

        StringBuilder html = new StringBuilder();
        html.append("""
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Sport Event Manager</title>
              <style>
                :root {
                  --ink: #101820;
                  --muted: #66736d;
                  --paper: #fffaf0;
                  --panel: rgba(255, 250, 240, 0.9);
                  --line: rgba(16, 24, 32, 0.13);
                  --grass: #116149;
                  --clay: #d95d39;
                  --gold: #f2b84b;
                  --sky: #d7edf2;
                }
                * { box-sizing: border-box; }
                html { scroll-behavior: smooth; }
                body {
                  margin: 0;
                  color: var(--ink);
                  font-family: 'Trebuchet MS', Verdana, sans-serif;
                  background:
                    linear-gradient(90deg, rgba(255,255,255,.08) 49%, rgba(16,24,32,.035) 50%, rgba(255,255,255,.08) 51%) 0 0 / 84px 84px,
                    radial-gradient(circle at 10% 10%, rgba(217,93,57,.3), transparent 26rem),
                    radial-gradient(circle at 88% 8%, rgba(17,97,73,.28), transparent 28rem),
                    linear-gradient(135deg, #fbf2d5 0%, #dff0e8 100%);
                  min-height: 100vh;
                }
                .shell { width: min(1240px, calc(100% - 32px)); margin: 0 auto; padding: 28px 0 52px; }
                .hero {
                  min-height: 360px;
                  border-radius: 38px;
                  padding: clamp(28px, 6vw, 76px);
                  color: white;
                  background:
                    linear-gradient(120deg, rgba(16,24,32,.92), rgba(17,97,73,.88)),
                    radial-gradient(circle at 80% 20%, rgba(242,184,75,.8), transparent 16rem);
                  position: relative;
                  overflow: hidden;
                  box-shadow: 0 30px 90px rgba(16,24,32,.24);
                }
                .hero::before {
                  content: '';
                  position: absolute;
                  inset: auto -80px -180px auto;
                  width: 420px;
                  height: 420px;
                  border: 42px solid rgba(255,255,255,.12);
                  border-radius: 50%;
                }
                .hero-grid { display: grid; grid-template-columns: 1.2fr .8fr; gap: 28px; position: relative; z-index: 1; }
                .eyebrow { color: var(--gold); font-weight: 800; letter-spacing: .16em; text-transform: uppercase; }
                h1 { font-family: Georgia, 'Times New Roman', serif; font-size: clamp(2.7rem, 7vw, 6.4rem); line-height: .9; margin: 14px 0 18px; }
                .hero p { color: rgba(255,255,255,.78); font-size: 1.08rem; line-height: 1.75; max-width: 680px; }
                .quick { display: grid; gap: 12px; align-content: end; }
                .pill {
                  border: 1px solid rgba(255,255,255,.18);
                  border-radius: 20px;
                  padding: 18px;
                  background: rgba(255,255,255,.1);
                  backdrop-filter: blur(12px);
                }
                .pill strong { display: block; font-size: 1.6rem; }
                .nav { display: flex; flex-wrap: wrap; gap: 10px; margin: 20px 0 0; }
                .nav a {
                  color: white;
                  text-decoration: none;
                  border: 1px solid rgba(255,255,255,.22);
                  border-radius: 999px;
                  padding: 10px 14px;
                  background: rgba(255,255,255,.1);
                }
                .stats { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin: 20px 0; }
                .stat, .card, .event {
                  background: var(--panel);
                  border: 1px solid var(--line);
                  box-shadow: 0 20px 56px rgba(16,24,32,.1);
                }
                .stat { border-radius: 24px; padding: 20px; }
                .stat strong { display: block; font-size: 2rem; }
                .stat span, .muted { color: var(--muted); }
                .layout { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; align-items: start; }
                .wide { grid-column: 1 / -1; }
                .card { border-radius: 28px; padding: 22px; overflow: hidden; }
                .card-head { display: flex; justify-content: space-between; gap: 12px; align-items: start; margin-bottom: 16px; }
                h2 { margin: 0; font-family: Georgia, 'Times New Roman', serif; font-size: 1.7rem; }
                form { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 18px; }
                .compact-form { grid-template-columns: repeat(3, minmax(0, 1fr)); }
                label.field { display: grid; gap: 6px; color: var(--muted); font-size: .78rem; font-weight: 900; letter-spacing: .08em; text-transform: uppercase; }
                label.field input, label.field select { margin: 0; }
                .field-hint { color: var(--muted); font-size: .9rem; line-height: 1.5; margin: -8px 0 16px; }
                .span-2 { grid-column: span 2; }
                input, select, button {
                  width: 100%;
                  border-radius: 14px;
                  border: 1px solid var(--line);
                  padding: 12px 13px;
                  font: inherit;
                  background: #fffdf7;
                  color: var(--ink);
                }
                button {
                  cursor: pointer;
                  border-color: var(--clay);
                  background: var(--clay);
                  color: white;
                  font-weight: 800;
                }
                .events { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
                .event { border-radius: 24px; padding: 18px; background: linear-gradient(145deg, rgba(255,250,240,.98), rgba(215,237,242,.82)); }
                .event .date { color: var(--clay); font-weight: 800; letter-spacing: .08em; text-transform: uppercase; font-size: .78rem; }
                .versus { display: grid; grid-template-columns: 1fr auto 1fr; gap: 10px; align-items: center; margin: 14px 0; font-weight: 900; }
                .vs { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 50%; background: var(--ink); color: white; font-size: .8rem; }
                table { width: 100%; border-collapse: collapse; font-size: .94rem; }
                th, td { text-align: left; padding: 11px 8px; border-bottom: 1px solid var(--line); vertical-align: top; }
                th { color: var(--grass); font-size: .76rem; letter-spacing: .1em; text-transform: uppercase; }
                .leader { display: grid; gap: 10px; }
                .leader-row { display: grid; grid-template-columns: 32px 1fr auto; gap: 12px; align-items: center; padding: 12px; border-radius: 16px; background: rgba(255,255,255,.62); }
                .rank { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 50%; background: var(--gold); font-weight: 900; }
                .empty { color: var(--muted); font-style: italic; }
                .topbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
                .identity { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
                .badge { border-radius: 999px; padding: 9px 13px; background: rgba(255,250,240,.82); border: 1px solid var(--line); font-weight: 800; }
                .logout { color: var(--ink); text-decoration: none; border-radius: 999px; padding: 9px 13px; background: var(--gold); font-weight: 900; }
                .locked { padding: 14px; border-radius: 16px; background: rgba(16,24,32,.06); border: 1px dashed var(--line); color: var(--muted); }
                @media (max-width: 980px) { .hero-grid, .layout, .stats, .events, form, .compact-form { grid-template-columns: 1fr; } }
              </style>
            </head>
            <body>
              <main class="shell">
            """);
        html.append("<div class=\"topbar\"><div class=\"identity\"><span class=\"badge\">")
            .append(escape(user.displayName())).append("</span><span class=\"badge\">")
            .append(roleLabel(user.role())).append("</span></div><a class=\"logout\" href=\"/logout\">Logout</a></div>");
        html.append("""
                <section class="hero">
                  <div class="hero-grid">
                    <div>
                      <div class="eyebrow">Sport Event Management</div>
                      <h1>Plan fixtures. Track squads. Record performances.</h1>
                      <p>A browser-based control room for your multi-sport database: schedule matches, register teams and players, record match stats, and review leaderboard momentum.</p>
                      <nav class="nav">
                        <a href="#events">Events</a>
                        <a href="#stats">Record Stats</a>
                        <a href="#teams">Teams</a>
                        <a href="#leaderboard">Leaderboard</a>
                      </nav>
                    </div>
                    <div class="quick">
            """);
        html.append("<div class=\"pill\"><strong>").append(nextMatch(matches)).append("</strong><span>Next scheduled event</span></div>");
        html.append("<div class=\"pill\"><strong>").append(topScorer(stats, playerNames)).append("</strong><span>Current scoring leader</span></div>");
        html.append("""
                    </div>
                  </div>
                </section>
            """);

        html.append("<section class=\"stats\">")
            .append(statCard("Sports", sports.size()))
            .append(statCard("Teams", teams.size()))
            .append(statCard("Players", players.size()))
            .append(statCard("Events", matches.size()))
            .append(statCard("Stat Lines", stats.size()))
            .append("</section>");

        html.append("<section class=\"layout\">")
            .append(eventsCard(sports, teams, matches, sportNames, teamNames, user))
            .append(statsCard(players, matches, teamNames, user))
            .append(leaderboardCard(stats, playerNames))
            .append(rosterCard(sports, teams, players, sportNames, teamMap))
            .append(setupCard(sports, teams, user))
            .append("</section>");

        html.append("""
              </main>
            </body>
            </html>
            """);
        return html.toString();
    }

    private String eventsCard(List<Sport> sports, List<Team> teams, List<Match> matches, Map<Integer, String> sportNames, Map<Integer, String> teamNames, User user) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <article class="card wide" id="events">
              <div class="card-head">
                <div><h2>Event Schedule</h2><div class="muted">Create fixtures and review upcoming matchups.</div></div>
              </div>
            """);
        if (canManageEvents(user)) {
            html.append("<p class=\"field-hint\">Fill this as: choose the sport, select both teams, then pick the match date.</p>");
            html.append("<form method=\"post\" action=\"/matches\">");
            html.append(wrapField("Sport", select("sportId", sports, "Select sport")));
            html.append(wrapField("Team 1", teamSelect("team1Id", teams, "Select first team")));
            html.append(wrapField("Team 2", teamSelect("team2Id", teams, "Select opponent")));
            html.append("<label class=\"field\">Match date<input type=\"date\" name=\"matchDate\" required></label><button type=\"submit\">Schedule Match</button></form>");
        } else {
            html.append("<div class=\"locked\">Your role can view events, but scheduling is available only to Admins and Organizers.</div>");
        }
        if (matches.isEmpty()) {
            html.append("<p class=\"empty\">No events scheduled yet.</p>");
        } else {
            html.append("<div class=\"events\">");
            for (Match match : matches) {
                html.append("<div class=\"event\"><div class=\"date\">").append(match.getMatchDate()).append(" · ")
                    .append(escape(sportNames.getOrDefault(match.getSportId(), "Sport"))).append("</div>")
                    .append("<div class=\"versus\"><span>").append(escape(teamNames.getOrDefault(match.getTeam1Id(), "TBD")))
                    .append("</span><span class=\"vs\">VS</span><span>").append(escape(teamNames.getOrDefault(match.getTeam2Id(), "TBD")))
                    .append("</span></div><div class=\"muted\">Match #").append(match.getMatchId()).append("</div></div>");
            }
            html.append("</div>");
        }
        return html.append("</article>").toString();
    }

    private String statsCard(List<Player> players, List<Match> matches, Map<Integer, String> teamNames, User user) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <article class="card" id="stats">
              <div class="card-head"><div><h2>Record Performance</h2><div class="muted">Add score, assists, and wickets for a match.</div></div></div>
            """);
        if (canRecordStats(user)) {
            html.append("<p class=\"field-hint\">Fill this as: pick the player, pick the match, then enter score/points, assists, and wickets. Use 0 when a stat does not apply.</p>");
            html.append("<form class=\"compact-form\" method=\"post\" action=\"/stats\">");
            html.append(wrapField("Player", playerSelect(players, teamNames)));
            html.append(wrapField("Match", matchSelect(matches, teamNames)));
            html.append("""
                <label class="field">Score / Points<input type="number" min="0" name="score" placeholder="e.g. 25" value="0" required></label>
                <label class="field">Assists<input type="number" min="0" name="assists" placeholder="e.g. 4" value="0" required></label>
                <label class="field">Wickets<input type="number" min="0" name="wickets" placeholder="e.g. 2" value="0" required></label>
                <button type="submit">Save Stats</button>
              </form>
            """);
        } else {
            html.append("<div class=\"locked\">Viewer access is read-only. Login as Scorer, Organizer, or Admin to record performance.</div>");
        }
        html.append("<p class=\"muted\">Tip: use this after each fixture to keep the leaderboard and event history fresh.</p></article>");
        return html.toString();
    }

    private String leaderboardCard(List<PlayerStats> stats, Map<Integer, String> playerNames) {
        Map<Integer, Integer> totals = new HashMap<>();
        for (PlayerStats stat : stats) {
            if (stat.getPlayerId() != null) {
                totals.merge(stat.getPlayerId(), stat.getScore() == null ? 0 : stat.getScore(), Integer::sum);
            }
        }
        List<Map.Entry<Integer, Integer>> sorted = totals.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(6)
            .toList();

        StringBuilder html = new StringBuilder();
        html.append("<article class=\"card\" id=\"leaderboard\"><div class=\"card-head\"><div><h2>Leaderboard</h2><div class=\"muted\">Top performers by total score.</div></div></div>");
        if (sorted.isEmpty()) {
            html.append("<p class=\"empty\">No player stats recorded yet.</p>");
        } else {
            html.append("<div class=\"leader\">");
            int rank = 1;
            for (Map.Entry<Integer, Integer> entry : sorted) {
                html.append("<div class=\"leader-row\"><span class=\"rank\">").append(rank++).append("</span><strong>")
                    .append(escape(playerNames.getOrDefault(entry.getKey(), "Player #" + entry.getKey())))
                    .append("</strong><span>").append(entry.getValue()).append(" pts</span></div>");
            }
            html.append("</div>");
        }
        return html.append("</article>").toString();
    }

    private String rosterCard(List<Sport> sports, List<Team> teams, List<Player> players, Map<Integer, String> sportNames, Map<Integer, Team> teamMap) {
        StringBuilder html = new StringBuilder();
        html.append("<article class=\"card wide\" id=\"teams\"><div class=\"card-head\"><div><h2>Clubs & Roster</h2><div class=\"muted\">A sport-wise view of teams and registered athletes.</div></div></div>");
        if (teams.isEmpty()) {
            html.append("<p class=\"empty\">No teams found.</p>");
        } else {
            html.append("<table><thead><tr><th>Sport</th><th>Team</th><th>Players</th></tr></thead><tbody>");
            for (Team team : teams) {
                html.append("<tr><td>").append(escape(sportNames.getOrDefault(team.getSportId(), "Sport #" + team.getSportId())))
                    .append("</td><td><strong>").append(escape(team.getTeamName())).append("</strong></td><td>")
                    .append(playersForTeam(players, team.getTeamId())).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("<p class=\"muted\">Registered sports: ").append(sports.size()).append(". Active teams: ").append(teamMap.size()).append(".</p>");
        return html.append("</article>").toString();
    }

    private String setupCard(List<Sport> sports, List<Team> teams, User user) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <article class="card wide">
              <div class="card-head"><div><h2>Registration Desk</h2><div class="muted">Add sports, teams, and players without leaving the dashboard.</div></div></div>
            """);
        if (isAdmin(user)) {
            html.append("""
                <p class="field-hint">Add a sport category first, for example Hockey, Kabaddi, Badminton, or Table Tennis.</p>
                <form method="post" action="/sports">
                  <label class="field span-2">Sport name<input name="sportName" placeholder="e.g. Hockey" required></label>
                  <button type="submit">Add Sport</button>
                </form>
                """);
        } else {
            html.append("<div class=\"locked\">Only Admins can create new sport categories.</div>");
        }
        if (canManageEvents(user)) {
            html.append("<p class=\"field-hint\">After adding a sport, create teams for it, then register players under a team.</p>");
            html.append("<form method=\"post\" action=\"/teams\">");
            html.append("<label class=\"field\">Team name<input name=\"teamName\" placeholder=\"e.g. City Strikers\" required></label>")
                .append(wrapField("Sport", select("sportId", sports, "Select sport")))
                .append("<button type=\"submit\">Add Team</button></form>");
            html.append("<form method=\"post\" action=\"/players\"><label class=\"field\">Player name<input name=\"playerName\" placeholder=\"e.g. Priya Sharma\" required></label><label class=\"field\">Age<input type=\"number\" min=\"1\" name=\"age\" placeholder=\"e.g. 24\" required></label>");
            html.append(wrapField("Team", teamSelect("teamId", teams, "Select team"))).append("<button type=\"submit\">Add Player</button></form>");
        } else {
            html.append("<div class=\"locked\">Team and player registration is available to Admins and Organizers.</div>");
        }
        html.append("</article>");
        return html.toString();
    }

    private String statCard(String label, int value) {
        return "<div class=\"stat\"><strong>" + value + "</strong><span>" + escape(label) + "</span></div>";
    }

    private String wrapField(String label, String controlHtml) {
        return "<label class=\"field\">" + escape(label) + controlHtml + "</label>";
    }

    private String select(String name, List<Sport> sports, String label) {
        StringBuilder html = new StringBuilder("<select name=\"" + name + "\" required><option value=\"\">" + escape(label) + "</option>");
        for (Sport sport : sports) {
            html.append("<option value=\"").append(sport.getSportId()).append("\">").append(escape(sport.getSportName())).append("</option>");
        }
        return html.append("</select>").toString();
    }

    private String teamSelect(String name, List<Team> teams, String label) {
        StringBuilder html = new StringBuilder("<select name=\"" + name + "\" required><option value=\"\">" + escape(label) + "</option>");
        for (Team team : teams) {
            html.append("<option value=\"").append(team.getTeamId()).append("\">").append(escape(team.getTeamName())).append("</option>");
        }
        return html.append("</select>").toString();
    }

    private String playerSelect(List<Player> players, Map<Integer, String> teamNames) {
        StringBuilder html = new StringBuilder("<select name=\"playerId\" required><option value=\"\">Player</option>");
        for (Player player : players) {
            html.append("<option value=\"").append(player.getPlayerId()).append("\">").append(escape(player.getName()))
                .append(" · ").append(escape(teamNames.getOrDefault(player.getTeamId(), "Free agent"))).append("</option>");
        }
        return html.append("</select>").toString();
    }

    private String matchSelect(List<Match> matches, Map<Integer, String> teamNames) {
        StringBuilder html = new StringBuilder("<select name=\"matchId\" required><option value=\"\">Match</option>");
        for (Match match : matches) {
            html.append("<option value=\"").append(match.getMatchId()).append("\">#").append(match.getMatchId()).append(" · ")
                .append(match.getMatchDate()).append(" · ").append(escape(teamNames.getOrDefault(match.getTeam1Id(), "TBD")))
                .append(" vs ").append(escape(teamNames.getOrDefault(match.getTeam2Id(), "TBD"))).append("</option>");
        }
        return html.append("</select>").toString();
    }

    private String playersForTeam(List<Player> players, int teamId) {
        StringBuilder names = new StringBuilder();
        for (Player player : players) {
            if (player.getTeamId() != null && player.getTeamId() == teamId) {
                if (!names.isEmpty()) {
                    names.append(", ");
                }
                names.append(escape(player.getName()));
            }
        }
        return names.isEmpty() ? "<span class=\"empty\">No players yet</span>" : names.toString();
    }

    private String nextMatch(List<Match> matches) {
        LocalDate today = LocalDate.now();
        return matches.stream()
            .filter(match -> !match.getMatchDate().isBefore(today))
            .sorted((a, b) -> a.getMatchDate().compareTo(b.getMatchDate()))
            .findFirst()
            .map(match -> match.getMatchDate().toString())
            .orElse(matches.isEmpty() ? "No events" : matches.get(matches.size() - 1).getMatchDate().toString());
    }

    private String topScorer(List<PlayerStats> stats, Map<Integer, String> playerNames) {
        Map<Integer, Integer> totals = new HashMap<>();
        for (PlayerStats stat : stats) {
            if (stat.getPlayerId() != null) {
                totals.merge(stat.getPlayerId(), stat.getScore() == null ? 0 : stat.getScore(), Integer::sum);
            }
        }
        return totals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> playerNames.getOrDefault(entry.getKey(), "Player #" + entry.getKey()) + " · " + entry.getValue() + " pts")
            .orElse("No stats yet");
    }

    private Map<Integer, String> sportNames(List<Sport> sports) {
        Map<Integer, String> names = new HashMap<>();
        for (Sport sport : sports) {
            names.put(sport.getSportId(), sport.getSportName());
        }
        return names;
    }

    private Map<Integer, String> teamNames(List<Team> teams) {
        Map<Integer, String> names = new HashMap<>();
        for (Team team : teams) {
            names.put(team.getTeamId(), team.getTeamName());
        }
        return names;
    }

    private Map<Integer, Team> teamMap(List<Team> teams) {
        Map<Integer, Team> map = new HashMap<>();
        for (Team team : teams) {
            map.put(team.getTeamId(), team);
        }
        return map;
    }

    private Map<Integer, String> playerNames(List<Player> players) {
        Map<Integer, String> names = new HashMap<>();
        for (Player player : players) {
            names.put(player.getPlayerId(), player.getName());
        }
        return names;
    }

    private void seedExampleData() {
        int tennisId = ensureSport("Tennis");
        int volleyballId = ensureSport("Volleyball");
        int rugbyId = ensureSport("Rugby");

        int acesId = ensureTeam("Baseline Aces", tennisId);
        int smashersId = ensureTeam("Court Smashers", tennisId);
        int spikersId = ensureTeam("Chennai Spikers", volleyballId);
        int blockersId = ensureTeam("Mumbai Blockers", volleyballId);
        int rangersId = ensureTeam("Delhi Rangers", rugbyId);
        int hawksId = ensureTeam("Bengal Hawks", rugbyId);

        int saniaId = ensurePlayer("Sania Mirza", 39, acesId);
        int bopannaId = ensurePlayer("Rohan Bopanna", 46, smashersId);
        int karthikId = ensurePlayer("Karthik Serve", 24, spikersId);
        int arjunId = ensurePlayer("Arjun Block", 26, blockersId);
        int veerId = ensurePlayer("Veer Tackle", 27, rangersId);
        int kabirId = ensurePlayer("Kabir Sprint", 25, hawksId);

        int tennisMatchId = ensureMatch(tennisId, acesId, smashersId, LocalDate.of(2026, 4, 18));
        int volleyballMatchId = ensureMatch(volleyballId, spikersId, blockersId, LocalDate.of(2026, 4, 21));
        int rugbyMatchId = ensureMatch(rugbyId, rangersId, hawksId, LocalDate.of(2026, 4, 24));

        ensureStats(saniaId, tennisMatchId, 42, 6, 0);
        ensureStats(bopannaId, tennisMatchId, 38, 4, 0);
        ensureStats(karthikId, volleyballMatchId, 26, 8, 0);
        ensureStats(arjunId, volleyballMatchId, 22, 10, 0);
        ensureStats(veerId, rugbyMatchId, 18, 3, 1);
        ensureStats(kabirId, rugbyMatchId, 24, 2, 0);
    }

    private int ensureSport(String name) {
        for (Sport sport : sportDAO.getAllSports()) {
            if (sport.getSportName().equalsIgnoreCase(name)) {
                return sport.getSportId();
            }
        }
        return sportDAO.createSport(new Sport(name));
    }

    private int ensureTeam(String name, int sportId) {
        for (Team team : teamDAO.getAllTeams()) {
            if (team.getTeamName().equalsIgnoreCase(name)) {
                return team.getTeamId();
            }
        }
        return teamDAO.createTeam(new Team(name, sportId));
    }

    private int ensurePlayer(String name, int age, int teamId) {
        for (Player player : playerDAO.getAllPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player.getPlayerId();
            }
        }
        return playerDAO.createPlayer(new Player(name, age, teamId));
    }

    private int ensureMatch(int sportId, int team1Id, int team2Id, LocalDate matchDate) {
        for (Match match : matchDAO.getAllMatches()) {
            if (match.getSportId() == sportId
                && match.getTeam1Id() != null && match.getTeam1Id() == team1Id
                && match.getTeam2Id() != null && match.getTeam2Id() == team2Id
                && match.getMatchDate().equals(matchDate)) {
                return match.getMatchId();
            }
        }
        return matchDAO.createMatch(new Match(sportId, team1Id, team2Id, matchDate));
    }

    private void ensureStats(int playerId, int matchId, int score, int assists, int wickets) {
        for (PlayerStats stat : statsDAO.getAllStats()) {
            if (stat.getPlayerId() != null && stat.getPlayerId() == playerId
                && stat.getMatchId() != null && stat.getMatchId() == matchId) {
                return;
            }
        }
        statsDAO.createStats(new PlayerStats(playerId, matchId, score, assists, wickets));
    }

    private String buildLoginPage(String error) {
        String errorHtml = error == null ? "" : "<div class=\"error\">" + escape(error) + "</div>";
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Login · Sport Event Manager</title>
              <style>
                :root { --ink: #101820; --muted: #64736c; --clay: #d95d39; --grass: #116149; --gold: #f2b84b; }
                * { box-sizing: border-box; }
                body {
                  min-height: 100vh;
                  margin: 0;
                  display: grid;
                  place-items: center;
                  padding: 24px;
                  color: var(--ink);
                  font-family: 'Trebuchet MS', Verdana, sans-serif;
                  background:
                    radial-gradient(circle at 12% 18%, rgba(217,93,57,.32), transparent 26rem),
                    radial-gradient(circle at 88% 10%, rgba(17,97,73,.3), transparent 28rem),
                    linear-gradient(135deg, #fbf2d5 0%, #dff0e8 100%);
                }
                .login {
                  width: min(1040px, 100%);
                  display: grid;
                  grid-template-columns: 1.1fr .9fr;
                  border-radius: 36px;
                  overflow: hidden;
                  background: rgba(255,250,240,.9);
                  box-shadow: 0 30px 90px rgba(16,24,32,.22);
                  border: 1px solid rgba(16,24,32,.12);
                }
                .story { padding: clamp(28px, 6vw, 70px); color: white; background: linear-gradient(135deg, rgba(16,24,32,.95), rgba(17,97,73,.9)); }
                .story h1 { font-family: Georgia, 'Times New Roman', serif; font-size: clamp(2.5rem, 6vw, 5rem); line-height: .92; margin: 16px 0; }
                .story p { color: rgba(255,255,255,.76); line-height: 1.7; }
                .panel { padding: clamp(26px, 5vw, 56px); }
                .eyebrow { color: var(--gold); font-weight: 900; letter-spacing: .16em; text-transform: uppercase; }
                form { display: grid; gap: 14px; margin: 22px 0; }
                input, button { border: 1px solid rgba(16,24,32,.14); border-radius: 16px; padding: 14px 15px; font: inherit; }
                button { border-color: var(--clay); background: var(--clay); color: white; cursor: pointer; font-weight: 900; }
                .demo { display: grid; gap: 10px; color: var(--muted); font-size: .94rem; }
                .demo code { color: var(--ink); background: rgba(242,184,75,.32); border-radius: 999px; padding: 3px 8px; }
                .error { padding: 12px 14px; border-radius: 14px; background: rgba(217,93,57,.14); color: #8a2d19; font-weight: 800; }
                @media (max-width: 820px) { .login { grid-template-columns: 1fr; } }
              </style>
            </head>
            <body>
              <main class="login">
                <section class="story">
                  <div class="eyebrow">Secure Access</div>
                  <h1>Sport Event Manager</h1>
                  <p>Login by role to manage fixtures, registrations, scoring, or read-only event insights.</p>
                </section>
                <section class="panel">
                  <h2>Login</h2>
                  """ + errorHtml + """
                  <form method="post" action="/login">
                    <input name="username" placeholder="Username" autocomplete="username" required>
                    <input name="password" type="password" placeholder="Password" autocomplete="current-password" required>
                    <button type="submit">Enter Dashboard</button>
                  </form>
                  <div class="demo">
                    <strong>Demo logins</strong>
                    <span>Admin: <code>admin / admin123</code></span>
                    <span>Organizer: <code>organizer / org123</code></span>
                    <span>Scorer: <code>scorer / score123</code></span>
                    <span>Viewer: <code>viewer / view123</code></span>
                  </div>
                </section>
              </main>
            </body>
            </html>
            """;
    }

    private Map<String, User> createUsers() {
        Map<String, User> demoUsers = new HashMap<>();
        demoUsers.put("admin", new User("admin", "admin123", "admin", "Asha Admin"));
        demoUsers.put("organizer", new User("organizer", "org123", "organizer", "Omar Organizer"));
        demoUsers.put("scorer", new User("scorer", "score123", "scorer", "Sara Scorer"));
        demoUsers.put("viewer", new User("viewer", "view123", "viewer", "Vikram Viewer"));
        return demoUsers;
    }

    private boolean requireRole(HttpExchange exchange, String... allowedRoles) throws IOException {
        User user = currentUser(exchange);
        if (user == null) {
            redirectHome(exchange);
            return false;
        }
        for (String role : allowedRoles) {
            if (user.role().equals(role)) {
                return true;
            }
        }
        send(exchange, 403, "text/html; charset=utf-8", buildForbiddenPage(user));
        return false;
    }

    private String buildForbiddenPage(User user) {
        return new StringBuilder()
            .append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Access denied</title></head>")
            .append("<body style=\"font-family: Verdana, sans-serif; min-height: 100vh; display: grid; place-items: center; background: #fbf2d5; color: #101820;\">")
            .append("<main style=\"max-width: 560px; padding: 32px; border-radius: 24px; background: #fffaf0; box-shadow: 0 20px 60px rgba(16,24,32,.16);\">")
            .append("<h1>Access denied</h1><p>")
            .append(escape(roleLabel(user.role())))
            .append(" access cannot perform this action.</p>")
            .append("<a href=\"/\" style=\"color: #116149; font-weight: 800;\">Back to dashboard</a>")
            .append("</main></body></html>")
            .toString();
    }

    private User currentUser(HttpExchange exchange) {
        String sessionId = sessionId(exchange);
        return sessionId == null ? null : sessions.get(sessionId);
    }

    private String sessionId(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (String cookie : header.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "MULTISPORT_SESSION".equals(parts[0])) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private boolean canManageEvents(User user) {
        return user != null && ("admin".equals(user.role()) || "organizer".equals(user.role()));
    }

    private boolean canRecordStats(User user) {
        return user != null && ("admin".equals(user.role()) || "organizer".equals(user.role()) || "scorer".equals(user.role()));
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equals(user.role());
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

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<>();
        if (body.isBlank()) {
            return form;
        }
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            form.put(key, value);
        }
        return form;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void redirectHome(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
    }

    private void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
