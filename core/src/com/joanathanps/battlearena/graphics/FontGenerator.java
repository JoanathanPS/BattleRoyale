package com.joanathanps.battlearena.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Rasterizes TTF fonts on first use and hands out the same BitmapFont afterwards.
 * Fonts are shared, so callers must never dispose them; the game disposes all of them on exit.
 */
public final class FontGenerator {

    private static final ObjectMap<String, BitmapFont> cache = new ObjectMap<String, BitmapFont>();

    private FontGenerator() { }

    public static BitmapFont generate(ResourceHandler.FontPath fontPath, int size, boolean hasShadow) {
        String key = fontPath.name() + "|" + size + "|" + hasShadow;
        BitmapFont font = cache.get(key);
        if (font == null) {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            if (hasShadow) {
                parameter.shadowColor = new Color(0f, 0f, 0f, 0.85f);
                parameter.shadowOffsetX = 2;
                parameter.shadowOffsetY = -2;
            }
            font = build(fontPath, parameter);
            cache.put(key, font);
        }
        return font;
    }

    /** A font with a dark outline, for text drawn over the game world. */
    public static BitmapFont generateOutlined(ResourceHandler.FontPath fontPath, int size, float borderWidth) {
        String key = fontPath.name() + "|" + size + "|outline" + borderWidth;
        BitmapFont font = cache.get(key);
        if (font == null) {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            parameter.borderWidth = borderWidth;
            parameter.borderColor = new Color(0f, 0f, 0f, 0.9f);
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            font = build(fontPath, parameter);
            cache.put(key, font);
        }
        return font;
    }

    private static BitmapFont build(ResourceHandler.FontPath fontPath,
                                    FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath.path));
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    public static void disposeAll() {
        for (BitmapFont font : cache.values()) {
            font.dispose();
        }
        cache.clear();
    }
}
