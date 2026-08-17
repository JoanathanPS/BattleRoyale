package com.joanathanps.battlearena.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/battle_royale_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private Connection connection;

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public boolean connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Database connected successfully.");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database disconnected.");
            }
        } catch (SQLException e) {
            System.err.println("Error disconnecting from database: " + e.getMessage());
        }
    }

    public void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS match_history ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "player_name VARCHAR(50) NOT NULL, "
                + "difficulty VARCHAR(10) NOT NULL, "
                + "score INT NOT NULL, "
                + "survival_time INT NOT NULL, "
                + "result VARCHAR(10) NOT NULL, "
                + "date_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableSQL);
            System.out.println("match_history table verified.");
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    public boolean saveMatchResult(String playerName, String difficulty, int score, int survivalTime, String result) {
        String sql = "INSERT INTO match_history (player_name, difficulty, score, survival_time, result) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setString(2, difficulty);
            pstmt.setInt(3, score);
            pstmt.setInt(4, survivalTime);
            pstmt.setString(5, result);
            pstmt.executeUpdate();
            System.out.println("Match result saved for player: " + playerName);
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving match result: " + e.getMessage());
            return false;
        }
    }

    public List<MatchResult> getLeaderboard(int limit) {
        List<MatchResult> results = new ArrayList<>();
        String sql = "SELECT player_name, difficulty, score, survival_time, result, date_played "
                + "FROM match_history ORDER BY score DESC, survival_time DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                MatchResult mr = new MatchResult(
                        rs.getString("player_name"),
                        rs.getString("difficulty"),
                        rs.getInt("score"),
                        rs.getInt("survival_time"),
                        rs.getString("result"),
                        rs.getTimestamp("date_played")
                );
                results.add(mr);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching leaderboard: " + e.getMessage());
        }

        return results;
    }

    public List<MatchResult> getPlayerHistory(String playerName) {
        List<MatchResult> results = new ArrayList<>();
        String sql = "SELECT player_name, difficulty, score, survival_time, result, date_played "
                + "FROM match_history WHERE player_name = ? ORDER BY date_played DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                MatchResult mr = new MatchResult(
                        rs.getString("player_name"),
                        rs.getString("difficulty"),
                        rs.getInt("score"),
                        rs.getInt("survival_time"),
                        rs.getString("result"),
                        rs.getTimestamp("date_played")
                );
                results.add(mr);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player history: " + e.getMessage());
        }

        return results;
    }

    public int getPlayerTotalKills(String playerName) {
        String sql = "SELECT COALESCE(SUM(score), 0) FROM match_history WHERE player_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player total kills: " + e.getMessage());
        }
        return 0;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
