-- =============================================
-- MULTI-SPORT DATABASE MANAGEMENT SYSTEM
-- Database: MySQL
-- =============================================

-- Create Database
CREATE DATABASE IF NOT EXISTS multisport;
USE multisport;

-- =============================================
-- TABLE DEFINITIONS
-- =============================================

-- Sports Table
CREATE TABLE sports (
    sport_id INT AUTO_INCREMENT PRIMARY KEY,
    sport_name VARCHAR(50) UNIQUE NOT NULL
);

-- Teams Table
CREATE TABLE teams (
    team_id INT AUTO_INCREMENT PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    sport_id INT NOT NULL,
    FOREIGN KEY (sport_id) REFERENCES sports(sport_id) ON DELETE CASCADE
);

CREATE INDEX idx_teams_sport ON teams(sport_id);

-- Players Table
CREATE TABLE players (
    player_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT CHECK (age > 0),
    team_id INT,
    FOREIGN KEY (team_id) REFERENCES teams(team_id) ON DELETE SET NULL
);

CREATE INDEX idx_players_team ON players(team_id);

-- Matches Table
CREATE TABLE matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    sport_id INT NOT NULL,
    team1_id INT,
    team2_id INT,
    match_date DATE,
    FOREIGN KEY (sport_id) REFERENCES sports(sport_id) ON DELETE CASCADE,
    FOREIGN KEY (team1_id) REFERENCES teams(team_id) ON DELETE RESTRICT,
    FOREIGN KEY (team2_id) REFERENCES teams(team_id) ON DELETE RESTRICT
);

CREATE INDEX idx_matches_sport ON matches(sport_id);

-- Player Stats Table
CREATE TABLE player_stats (
    stat_id INT AUTO_INCREMENT PRIMARY KEY,
    player_id INT,
    match_id INT,
    score INT CHECK (score >= 0),
    assists INT CHECK (assists >= 0),
    wickets INT CHECK (wickets >= 0),
    FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE,
    FOREIGN KEY (match_id) REFERENCES matches(match_id) ON DELETE CASCADE
);

CREATE INDEX idx_stats_player ON player_stats(player_id);
CREATE INDEX idx_stats_match ON player_stats(match_id);

-- =============================================
-- SAMPLE DATA
-- =============================================

-- Sports
INSERT INTO sports (sport_name) VALUES
('Cricket'),
('Football'),
('Basketball');

-- Teams
INSERT INTO teams (team_name, sport_id) VALUES
('India', 1),
('Australia', 1),
('Brazil', 2),
('Argentina', 2),
('Lakers', 3),
('Warriors', 3);

-- Players
INSERT INTO players (name, age, team_id) VALUES
('Virat Kohli', 34, 1),
('Rohit Sharma', 36, 1),
('Steve Smith', 35, 2),
('David Warner', 37, 2),
('Neymar', 31, 3),
('Messi', 36, 4),
('Mbappe', 25, 3),
('Di Maria', 35, 4),
('LeBron James', 39, 5),
('Anthony Davis', 31, 5),
('Stephen Curry', 36, 6),
('Klay Thompson', 34, 6);

-- Matches
INSERT INTO matches (sport_id, team1_id, team2_id, match_date) VALUES
(1, 1, 2, '2024-01-10'),
(1, 2, 1, '2024-01-15'),
(2, 3, 4, '2024-02-10'),
(2, 4, 3, '2024-02-15'),
(3, 5, 6, '2024-03-10'),
(3, 6, 5, '2024-03-15');

-- Player Stats
INSERT INTO player_stats (player_id, match_id, score, assists, wickets) VALUES
(1, 1, 85, 2, 0),
(2, 1, 60, 1, 0),
(3, 1, 70, 0, 0),
(5, 3, 2, 1, 0),
(6, 3, 3, 2, 0),
(9, 5, 30, 5, 0),
(11, 5, 28, 6, 0);
