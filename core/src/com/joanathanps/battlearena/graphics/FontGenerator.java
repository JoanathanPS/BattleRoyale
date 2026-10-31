package com.joanathanps.battlearena.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontGenerator {

    /**
     * Generates a BitmapFont with an optional drop shadow.
     *
     * @param fontPath  the font file to use
     * @param size      point size (before any post-generation scaling)
     * @param hasShadow whether to add a drop shadow
     */
    public static BitmapFont generate(ResourceHandler.FontPath fontPath, int size, boolean hasShadow) {
        return generate(fontPath, size, hasShadow, 2);
    }

    /**
     * Generates a BitmapFont with an optional drop shadow of configurable pixel offset.
     *
     * @param fontPath      the font file to use
     * @param size          point size (before any post-generation scaling)
     * @param hasShadow     whether to add a drop shadow
     * @param shadowOffset  pixel offset for the shadow (both X and Y)
     */
    public static BitmapFont generate(ResourceHandler.FontPath fontPath, int size,
                                      boolean hasShadow, int shadowOffset) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath.path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        if (hasShadow) {
            parameter.shadowColor = new Color(0f, 0f, 0f, 0.85f);
            parameter.shadowOffsetX = shadowOffset;
            parameter.shadowOffsetY = -shadowOffset;
        }
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    /**
     * Generates a BitmapFont with an outline border and optional drop shadow.
     */
    public static BitmapFont generateWithBorder(ResourceHandler.FontPath fontPath, int size,
                                                float borderWidth, Color borderColor,
                                                int shadowOffsetX, int shadowOffsetY, Color shadowColor) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath.path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.borderWidth = borderWidth;
        parameter.borderColor = borderColor;
        parameter.shadowOffsetX = shadowOffsetX;
        parameter.shadowOffsetY = shadowOffsetY;
        parameter.shadowColor = shadowColor;
        parameter.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        parameter.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }
}
