package com.joanathanps.battlearena.database;

/**
 * Immutable value object for a single match result. GWT-safe: no java.sql types, no String.format.
 */
public class MatchResult {

    private final String playerName;
    private final String difficulty;
    private final int score;
    private final int kills;
    private final int survivalTime;
    private final String result;
    private final String datePlayed;

    public MatchResult(String playerName, String difficulty, int score, int kills, int survivalTime,
                       String result) {
        this(playerName, difficulty, score, kills, survivalTime, result, "");
    }

    public MatchResult(String playerName, String difficulty, int score, int kills, int survivalTime,
                       String result, String datePlayed) {
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.score = score;
        this.kills = kills;
        this.survivalTime = survivalTime;
        this.result = result;
        this.datePlayed = datePlayed;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getScore() {
        return score;
    }

    public int getKills() {
        return kills;
    }

    public int getSurvivalTime() {
        return survivalTime;
    }

    public String getResult() {
        return result;
    }

    public String getDatePlayed() {
        return datePlayed;
    }

    public String getFormattedTime() {
        int minutes = survivalTime / 60;
        int seconds = survivalTime % 60;
        return padTwo(minutes) + ":" + padTwo(seconds);
    }

    private String padTwo(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    @Override
    public String toString() {
        return playerName + " | " + difficulty + " | Score: " + score
                + " | Time: " + getFormattedTime() + " | " + result;
    }
}