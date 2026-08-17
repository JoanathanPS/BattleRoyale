package com.joanathanps.battlearena.database;

import java.sql.Timestamp;

public class MatchResult {

    private String playerName;
    private String difficulty;
    private int score;
    private int survivalTime;
    private String result;
    private Timestamp datePlayed;

    public MatchResult(String playerName, String difficulty, int score, int survivalTime,
                       String result, Timestamp datePlayed) {
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.score = score;
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

    public int getSurvivalTime() {
        return survivalTime;
    }

    public String getResult() {
        return result;
    }

    public Timestamp getDatePlayed() {
        return datePlayed;
    }

    public String getFormattedTime() {
        int minutes = survivalTime / 60;
        int seconds = survivalTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return playerName + " | " + difficulty + " | Score: " + score
                + " | Time: " + getFormattedTime() + " | " + result;
    }
}
