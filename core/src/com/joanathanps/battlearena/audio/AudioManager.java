package com.joanathanps.battlearena.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.joanathanps.battlearena.entities.objects.weapons.*;
import com.joanathanps.battlearena.entities.objects.weapons.srs.*;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {

    private static final Map<String, Sound> sounds = new HashMap<String, Sound>();
    private static boolean enabled = true;
    private static float masterVolume = 0.85f;
    private static AudioBridge audioBridge = null;
    private static boolean initialized = false;

    public static void setAudioBridge(AudioBridge bridge) {
        audioBridge = bridge;
    }

    public static void init() {
        if (initialized) return;
        loadSound("shoot", "sounds/shoot.wav");
        loadSound("sniper", "sounds/sniper.wav");
        loadSound("shotgun", "sounds/shotgun.wav");
        loadSound("loot", "sounds/loot.wav");
        loadSound("hit", "sounds/hit.wav");
        loadSound("kill", "sounds/kill.wav");
        loadSound("click", "sounds/click.wav");
        loadSound("zone", "sounds/zone.wav");
        loadSound("reload", "sounds/reload.wav");
        initialized = true;
    }

    private static void loadSound(String key, String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Sound s = Gdx.audio.newSound(Gdx.files.internal(path));
                sounds.put(key, s);
            }
        } catch (Exception e) {
            Gdx.app.log("AudioManager", "Could not load sound " + path + ": " + e.getMessage());
        }
    }

    public static void play(String key, float volume) {
        if (!enabled) return;
        float vol = Math.max(0f, Math.min(1f, volume * masterVolume));

        Sound sound = sounds.get(key);
        if (sound != null) {
            try {
                sound.play(vol);
            } catch (Exception ignored) {}
        }

        if (audioBridge != null) {
            try {
                audioBridge.play(key, vol);
            } catch (Exception ignored) {}
        }
    }

    public static void play(String key) {
        play(key, 1f);
    }

    public static void playShoot(Weapon weapon, boolean isPlayer, Vector2 soundPos, Vector2 playerPos) {
        float vol = 1f;
        if (!isPlayer && soundPos != null && playerPos != null) {
            float dist = soundPos.dst(playerPos);
            vol = Math.max(0.1f, 1f - (dist / 1400f));
        }

        String soundKey = "shoot";
        if (weapon instanceof AW3) {
            soundKey = "sniper";
        } else if (weapon instanceof M20) {
            soundKey = "shotgun";
        }
        play(soundKey, vol);
    }

    public static void playLoot() {
        play("loot", 0.9f);
    }

    public static void playHit() {
        play("hit", 0.85f);
    }

    public static void playKill() {
        play("kill", 1f);
    }

    public static void playClick() {
        play("click", 0.7f);
    }

    public static void playZoneWarning() {
        play("zone", 0.95f);
    }

    public static void playReload() {
        play("reload", 0.85f);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static void dispose() {
        for (Sound s : sounds.values()) {
            if (s != null) {
                try {
                    s.dispose();
                } catch (Exception ignored) {}
            }
        }
        sounds.clear();
        initialized = false;
    }
}
