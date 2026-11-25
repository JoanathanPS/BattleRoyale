package com.joanathanps.battlearena.entities;

import com.badlogic.gdx.math.MathUtils;

/** Hands out bot names for one match, each name at most once. */
public final class BotNames {

    private static final String[] NAMES = {
            "PotatoAim", "NoobMaster", "CaptainCrunch", "BushCamper",
            "LootGoblin", "SirDiesALot", "WaffleKnight", "TacoSniper",
            "BananaReaper", "ChickenChaser", "SneakyPanda", "GhostRider",
            "CouchPotato", "BulletMagnet", "HeadshotHarry", "ZigZagZombie",
            "DustyBones", "PixelPancake", "BoomerBob", "SaltyPretzel",
            "NinjaFrog", "ToxicToad", "DangerNoodle", "LaserLlama",
            "MegaBite", "CyberSamurai", "RustBucket", "ShadowStalker",
            "TurboTurtle", "CrouchKing", "DropShotDan", "FlankFrank",
            "CampfireCarl", "TriggerHappy", "PanMan", "RecoilRick",
            "ScopeGod", "SavageSpoon", "GlitchGremlin", "LagWizard"
    };

    private final String[] pool = NAMES.clone();
    private int next;

    public BotNames() {
        for (int i = pool.length - 1; i > 0; i--) {
            int j = MathUtils.random(i);
            String swap = pool[i];
            pool[i] = pool[j];
            pool[j] = swap;
        }
    }

    public String next() {
        if (next < pool.length) {
            return pool[next++];
        }
        return "Bot" + (++next);
    }
}
