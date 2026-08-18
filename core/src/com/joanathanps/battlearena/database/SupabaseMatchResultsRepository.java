package com.joanathanps.battlearena.database;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Single Supabase-backed implementation of {@link MatchResultsRepository} used on every platform.
 * Uses only Gdx.net (cross-platform, GWT-safe) - no java.net, no java.sql, no javax.sql.
 * Every request is wrapped in try/catch and failures are logged, never thrown.
 */
public class SupabaseMatchResultsRepository implements MatchResultsRepository {

    private static final String TAG = "SupabaseMatchResultsRepository";
    private static final String CONFIG_FILE = "supabase.properties";

    private final String url;
    private final String anonKey;
    private final boolean configured;

    public SupabaseMatchResultsRepository() {
        String loadedUrl = null;
        String loadedKey = null;
        try {
            if (Gdx.files.internal(CONFIG_FILE).exists()) {
                String content = Gdx.files.internal(CONFIG_FILE).readString();
                for (String rawLine : content.split("\\r?\\n")) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int separator = line.indexOf('=');
                    if (separator <= 0) {
                        continue;
                    }
                    String key = line.substring(0, separator).trim();
                    String value = line.substring(separator + 1).trim();
                    if ("supabase.url".equals(key)) {
                        loadedUrl = value;
                    } else if ("supabase.anonKey".equals(key)) {
                        loadedKey = value;
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.log(TAG, "Failed to load " + CONFIG_FILE + ": " + e.getMessage());
        }

        this.url = loadedUrl;
        this.anonKey = loadedKey;
        this.configured = loadedUrl != null && !loadedUrl.trim().isEmpty()
                && !loadedUrl.contains("XXXX")
                && loadedKey != null && !loadedKey.trim().isEmpty()
                && !loadedKey.contains("<your-anon-key>");

        if (configured) {
            Gdx.app.log(TAG, "Supabase leaderboard configured for " + url);
        } else {
            Gdx.app.log(TAG, "Supabase not configured - copy core/assets/supabase.properties.example "
                    + "to core/assets/supabase.properties with your real URL and anon key. "
                    + "Leaderboard features will be unavailable until then.");
        }
    }

    @Override
    public boolean isConnected() {
        return configured;
    }

    @Override
    public void saveMatchResult(String playerName, String difficulty, int score, int kills,
                                int survivalTimeSeconds, String result) {
        if (!configured) {
            Gdx.app.log(TAG, "Supabase not configured; skipping save of match result.");
            return;
        }
        try {
            JsonValue body = new JsonValue(JsonValue.ValueType.object);
            body.addChild("player_name", new JsonValue(playerName));
            body.addChild("difficulty", new JsonValue(difficulty));
            body.addChild("score", new JsonValue((long) score));
            body.addChild("kills", new JsonValue((long) kills));
            body.addChild("survival_time_seconds", new JsonValue((long) survivalTimeSeconds));
            body.addChild("result", new JsonValue(result));

            Net.HttpRequest request = new HttpRequestBuilder()
                    .newRequest()
                    .method(Net.HttpMethods.POST)
                    .url(url + "/rest/v1/match_results")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer " + anonKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .content(body.toString())
                    .build();

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    int status = httpResponse.getStatus().getStatusCode();
                    if (status == 200 || status == 201 || status == 204) {
                        Gdx.app.log(TAG, "Match result saved for " + playerName + " (HTTP " + status + ").");
                    } else {
                        Gdx.app.log(TAG, "Save match result failed with HTTP " + status + " for " + playerName + ".");
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.log(TAG, "Save match result request failed for " + playerName + ": "
                            + (t != null ? t.getMessage() : "unknown error"));
                }

                @Override
                public void cancelled() {
                    Gdx.app.log(TAG, "Save match result request cancelled for " + playerName + ".");
                }
            });
        } catch (Exception e) {
            Gdx.app.log(TAG, "Error sending save request for " + playerName + ": " + e.getMessage());
        }
    }

    @Override
    public void getLeaderboard(int limit, LeaderboardCallback callback) {
        if (!configured) {
            callback.onFailure("Supabase is not configured.");
            return;
        }
        try {
            String query = url + "/rest/v1/match_results"
                    + "?select=player_name,difficulty,score,kills,survival_time_seconds,result,created_at"
                    + "&order=score.desc&limit=" + limit;

            Net.HttpRequest request = new HttpRequestBuilder()
                    .newRequest()
                    .method(Net.HttpMethods.GET)
                    .url(query)
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer " + anonKey)
                    .header("Accept", "application/json")
                    .build();

            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    try {
                        int status = httpResponse.getStatus().getStatusCode();
                        if (status != 200 && status != 201) {
                            callback.onFailure("Leaderboard request failed with HTTP " + status + ".");
                            return;
                        }
                        callback.onSuccess(parseLeaderboard(httpResponse.getResultAsString()));
                    } catch (Exception e) {
                        Gdx.app.log(TAG, "Error handling leaderboard response: " + e.getMessage());
                        callback.onFailure("Error parsing leaderboard response.");
                    }
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.log(TAG, "Leaderboard request failed: "
                            + (t != null ? t.getMessage() : "unknown error"));
                    callback.onFailure("Could not reach the Supabase leaderboard.");
                }

                @Override
                public void cancelled() {
                    callback.onFailure("Leaderboard request cancelled.");
                }
            });
        } catch (Exception e) {
            Gdx.app.log(TAG, "Error sending leaderboard request: " + e.getMessage());
            callback.onFailure("Error sending leaderboard request.");
        }
    }

    private List<MatchResult> parseLeaderboard(String json) {
        List<MatchResult> results = new ArrayList<MatchResult>();
        try {
            JsonValue root = new JsonReader().parse(json);
            if (root.isArray()) {
                for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                    String playerName = entry.getString("player_name", "Unknown");
                    String difficulty = entry.getString("difficulty", "");
                    int score = entry.getInt("score", 0);
                    int kills = entry.getInt("kills", 0);
                    int survival = entry.getInt("survival_time_seconds", 0);
                    String result = entry.getString("result", "");
                    String createdAt = entry.getString("created_at", "");
                    results.add(new MatchResult(playerName, difficulty, score, kills, survival, result, createdAt));
                }
            }
        } catch (Exception e) {
            Gdx.app.log(TAG, "Failed to parse leaderboard JSON: " + e.getMessage());
        }
        return results;
    }
}