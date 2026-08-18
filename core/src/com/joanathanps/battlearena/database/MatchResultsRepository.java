package com.joanathanps.battlearena.database;

import java.util.List;

/** Cross-platform persistence contract for match results (desktop + web). */
public interface MatchResultsRepository {

    boolean isConnected();

    /** Fire-and-forget save. Implementations must never throw out of this method. */
    void saveMatchResult(String playerName, String difficulty, int score, int kills,
                         int survivalTimeSeconds, String result);

    /** Asynchronous leaderboard fetch. Callbacks are invoked off the render thread on desktop. */
    void getLeaderboard(int limit, LeaderboardCallback callback);

    interface LeaderboardCallback {
        void onSuccess(List<MatchResult> results);

        void onFailure(String errorMessage);
    }
}