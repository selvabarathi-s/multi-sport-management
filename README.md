# Multi-Sport Database Management System

A fully normalized multi-sport relational database with indexed foreign keys, constraint enforcement, DAO-based Java architecture, and analytical SQL queries for performance insights.

The main application now runs as a JDK + JDBC Swing desktop program. You do not need to start the web UI to use the project.

## Project Structure

```
Multi-sport/
├── README.md
├── sql/
│   └── schema.sql
├── src/
│   └── multisport/
│       ├── DatabaseConnection.java
│       ├── Main.java              # JDK/JDBC Swing desktop app
│       ├── WebServer.java         # Optional browser UI
│       ├── dao/
│       │   ├── SportDAO.java
│       │   ├── TeamDAO.java
│       │   ├── PlayerDAO.java
│       │   ├── MatchDAO.java
│       │   └── PlayerStatsDAO.java
│       └── model/
│           ├── Sport.java
│           ├── Team.java
│           ├── Player.java
│           ├── Match.java
│           └── PlayerStats.java
└── lib/
    └── mysql-connector-j-*.jar
```

## Database Schema

### Tables

#### `sports`
| Column     | Type        | Constraints              |
|------------|-------------|--------------------------|
| sport_id   | INT         | PK, AUTO_INCREMENT       |
| sport_name | VARCHAR(50) | UNIQUE, NOT NULL         |

#### `teams`
| Column    | Type         | Constraints              |
|-----------|--------------|--------------------------|
| team_id   | INT          | PK, AUTO_INCREMENT       |
| team_name | VARCHAR(100) | NOT NULL                 |
| sport_id  | INT          | FK → sports(sport_id)    |

#### `players`
| Column    | Type         | Constraints              |
|-----------|--------------|--------------------------|
| player_id | INT          | PK, AUTO_INCREMENT       |
| name      | VARCHAR(100) | NOT NULL                 |
| age       | INT          | CHECK (age > 0)          |
| team_id   | INT          | FK → teams(team_id)      |

#### `matches`
| Column     | Type | Constraints                 |
|------------|------|-----------------------------|
| match_id   | INT  | PK, AUTO_INCREMENT          |
| sport_id   | INT  | FK → sports(sport_id)       |
| team1_id   | INT  | FK → teams(team_id)         |
| team2_id   | INT  | FK → teams(team_id)         |
| match_date | DATE |                             |

#### `player_stats`
| Column  | Type | Constraints                    |
|---------|------|--------------------------------|
| stat_id | INT  | PK, AUTO_INCREMENT             |
| player_id | INT | FK → players(player_id)      |
| match_id  | INT | FK → matches(match_id)       |
| score   | INT  | CHECK (score >= 0)             |
| assists | INT  | CHECK (assists >= 0)           |
| wickets | INT  | CHECK (wickets >= 0)           |

### Relationships

- **One-to-Many**: `sports` → `teams` (one sport has many teams)
- **One-to-Many**: `teams` → `players` (one team has many players)
- **One-to-Many**: `sports` → `matches` (one sport has many matches)
- **Many-to-Many**: `players` ↔ `matches` via `player_stats`

### Indexes

- `idx_teams_sport` on `teams(sport_id)`
- `idx_players_team` on `players(team_id)`
- `idx_matches_sport` on `matches(sport_id)`
- `idx_stats_player` on `player_stats(player_id)`
- `idx_stats_match` on `player_stats(match_id)`

## Setup Instructions

### Prerequisites

- MySQL 8.0+
- Java 11+
- MySQL Connector/J (JDBC Driver)

### Step 1: Create Database

```bash
mysql -u root -p < sql/schema.sql
```

Or run the SQL manually in MySQL Workbench:

```bash
mysql -u root -p
source sql/schema.sql
```

### Step 2: Configure Database Credentials

Edit `src/multisport/DatabaseConnection.java`:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/multisport?useSSL=false&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASS = "1234";
```

Update `USER` and `PASS` to match your MySQL credentials.

### Step 3: Add MySQL JDBC Driver

Download MySQL Connector/J from [MySQL Downloads](https://dev.mysql.com/downloads/connector/j/) and place the JAR in the `lib/` folder.

### Step 4: Compile the JDK/JDBC Desktop App

```bash
javac -d out -cp "lib/*" src/multisport/model/*.java src/multisport/DatabaseConnection.java src/multisport/dao/*.java src/multisport/Main.java
```

On Windows PowerShell, the same command is:

```powershell
javac -d out -cp "lib/*" src\multisport\model\*.java src\multisport\DatabaseConnection.java src\multisport\dao\*.java src\multisport\Main.java
```

### Step 5: Run the Java Interface

```bash
java -cp "out;lib/*" multisport.Main
```

This opens a structured Java Swing application with login, dashboard metrics, searchable data tables, update/delete actions, and tabs for sports, teams, players, matches, player stats, and analytical queries.

Demo logins:

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Organizer | `organizer` | `org123` |
| Scorer | `scorer` | `score123` |
| Viewer | `viewer` | `view123` |

Role access:

| Role | Access |
|------|--------|
| Admin | Add, update, and delete sports, teams, players, matches, and stats |
| Organizer | Add, update, and delete teams, players, matches, and stats |
| Scorer | Add, update, and delete player stats |
| Viewer | View all data and analytical reports |

To update or delete a record, select a row in the table and click `Update Selected` or `Delete Selected`.

### Optional: Run the Web UI

The web UI is still available, but it is not required for the JDK/JDBC version.

```bash
javac -d out -cp "lib/*" src/multisport/model/*.java src/multisport/DatabaseConnection.java src/multisport/dao/*.java src/multisport/WebServer.java
java -cp "out;lib/*" multisport.WebServer
```

## DAO Pattern

Each DAO class provides:

| DAO | Operations |
|-----|------------|
| SportDAO | create, getById, update, delete, getAll |
| TeamDAO | create, getById, update, delete, getAll, getBySport |
| PlayerDAO | create, getById, update, delete, getAll, getByTeam |
| MatchDAO | create, getById, update, delete, getAll, getBySport |
| PlayerStatsDAO | create, getById, update, delete, getAll, getByPlayer |

## Analytical Queries

The Swing `Main.java` includes 6 analytical queries:

1. **Top Players by Total Score** — Aggregate scores across all matches
2. **Matches by Sport** — List all matches grouped by sport
3. **Player Performance Averages** — Average score and assists per player
4. **Team Match Count** — How many matches each team has played
5. **Highest Scoring Match** — Match with the highest combined score
6. **Players by Sport** — All players grouped by their sport

## Sample Data

The schema includes sample data for:

- **3 Sports**: Cricket, Football, Basketball
- **6 Teams**: India, Australia, Brazil, Argentina, Lakers, Warriors
- **12 Players**: Virat Kohli, Rohit Sharma, Steve Smith, David Warner, Neymar, Messi, Mbappe, Di Maria, LeBron James, Anthony Davis, Stephen Curry, Klay Thompson
- **6 Matches**: 2 per sport
- **7 Player Stats**: Performance records across matches

## Key Concepts

- Primary Keys for unique identification
- Foreign Keys with `ON DELETE CASCADE/RESTRICT/SET NULL`
- `CHECK` constraints for data validation
- Indexes on foreign keys for query performance
- Database normalization up to 3NF
- DAO pattern for clean separation of concerns
- PreparedStatements to prevent SQL injection

## Viva Point

> "This project implements a fully normalized multi-sport relational database with indexed foreign keys, constraint enforcement, DAO-based Java architecture, and analytical SQL queries for performance insights."
