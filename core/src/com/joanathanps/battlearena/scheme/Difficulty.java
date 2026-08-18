package com.joanathanps.battlearena.scheme;

/**
 * Difficulty tiers chosen before a match (V6 Section 6).
 * Each tier meaningfully changes bot behavior:
 * - reactionSeconds  : decision/decision frequency (lower = faster reactions)
 * - accuracy         : chance a bot shot lands on target
 * - aggressionMulti  : acquisition range and how far a bot keeps pursuing a target
 */
public enum Difficulty {

    EASY("EASY", 0.5f, 0.45f, 0.7f),
    MEDIUM("MEDIUM", 0.25f, 0.7f, 1.0f),
    HARD("HARD", 0.08f, 0.9f, 1.35f);

    private final String label;
    private final float reactionSeconds;
    private final float accuracy;
    private final float aggressionMultiplier;

    Difficulty(String label, float reactionSeconds, float accuracy, float aggressionMultiplier) {
        this.label = label;
        this.reactionSeconds = reactionSeconds;
        this.accuracy = accuracy;
        this.aggressionMultiplier = aggressionMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public float getReactionSeconds() {
        return reactionSeconds;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getAggressionMultiplier() {
        return aggressionMultiplier;
    }
}