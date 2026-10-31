package com.joanathanps.battlearena.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BotNames {

    private BotNames() { }

    private static final String[] PRESET_NAMES = {
            "PotatoAim", "NoobMaster", "CaptainCrunch", "BushCamper",
            "LootGoblin", "SirDiesALot", "WaffleKnight", "TacoSniper",
            "BananaReaper", "ChickenChaser", "SneakyPanda", "GhostRider",
            "CouchPotato", "BulletMagnet", "HeadshotHarry", "ZigZagZombie",
            "DustyBones", "PixelPancake", "BoomerBob", "SaltyPretzel",
            "NinjaFrog", "ToxicToad", "DangerNoodle", "LaserLlama",
            "DrDisrespect", "MegaBite", "SniperWolf", "CyberSamurai",
            "RustBucket", "ShadowStalker", "TurboTurtle", "SneakySneak",
            "CrouchKing", "DropShotDan", "FlankFrank", "CampfireCarl",
            "TriggerHappy", "PanMan", "RecoilRick", "ScopeGod",
            "SavageSpoon", "MemeLord", "GlitchGremlin", "LagWizard"
    };

    private static List<String> shuffledPool = new ArrayList<>();
    private static int poolIndex = 0;

    public static synchronized String getNextName(int fallbackIndex) {
        if (shuffledPool.isEmpty() || poolIndex >= shuffledPool.size()) {
            shuffledPool = new ArrayList<>(Arrays.asList(PRESET_NAMES));
            Collections.shuffle(shuffledPool);
            poolIndex = 0;
        }
        if (poolIndex < shuffledPool.size()) {
            return shuffledPool.get(poolIndex++);
        }
        return "Bot_" + fallbackIndex;
    }
}