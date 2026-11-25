package com.joanathanps.battlearena.scheme;

/**
 * Chosen in the main menu before a match.
 * <ul>
 *   <li>botCount: how many bots drop with you</li>
 *   <li>reactionSeconds: how often a bot re-thinks (lower reacts faster)</li>
 *   <li>accuracy: chance a bot's shot goes exactly where it aims</li>
 *   <li>aggressionMultiplier: scales how far away a bot notices and engages you</li>
 * </ul>
 */
public enum Difficulty {

    EASY("EASY", 5, 0.5f, 0.45f, 0.7f),
    MEDIUM("MEDIUM", 12, 0.25f, 0.7f, 1.0f),
    HARD("HARD", 20, 0.08f, 0.9f, 1.35f);

    private final String label;
    private final int botCount;
    private final float reactionSeconds;
    private final float accuracy;
    private final float aggressionMultiplier;

    Difficulty(String label, int botCount, float reactionSeconds, float accuracy, float aggressionMultiplier) {
        this.label = label;
        this.botCount = botCount;
        this.reactionSeconds = reactionSeconds;
        this.accuracy = accuracy;
        this.aggressionMultiplier = aggressionMultiplier;
    }

    public String getLabel() {
        return label;
    }

    public int getBotCount() {
        return botCount;
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
