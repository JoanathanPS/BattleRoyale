package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

public class FreeTypeFontGenerator implements Disposable {

    public static class FreeTypeFontParameter {
        public int size = 16;
        public Color shadowColor;
        public int shadowOffsetX;
        public int shadowOffsetY;
        public float borderWidth;
        public Color borderColor;
        public Texture.TextureFilter minFilter;
        public Texture.TextureFilter magFilter;
    }

    private FileHandle file;

    public FreeTypeFontGenerator(FileHandle file) {
        this.file = file;
    }

    public BitmapFont generateFont(FreeTypeFontParameter parameter) {
        int reqSize = (parameter != null && parameter.size > 0) ? parameter.size : 16;

        int[] availableSizes = { 12, 14, 16, 18, 20, 22, 26, 28, 36, 42 };
        int bestSize = 16;
        int minDiff = Integer.MAX_VALUE;
        for (int s : availableSizes) {
            int diff = Math.abs(s - reqSize);
            if (diff < minDiff) {
                minDiff = diff;
                bestSize = s;
            }
        }

        String fntPath = "fonts/bombard-" + bestSize + ".fnt";
        FileHandle fh = Gdx.files.internal(fntPath);
        if (fh != null && fh.exists()) {
            BitmapFont font = new BitmapFont(fh);
            if (bestSize != reqSize) {
                float scale = (float) reqSize / (float) bestSize;
                font.getData().setScale(scale);
            }
            if (parameter != null && parameter.minFilter != null && parameter.magFilter != null) {
                font.getRegion().getTexture().setFilter(parameter.minFilter, parameter.magFilter);
            }
            return font;
        }

        // Fallback to internal font
        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale((float) reqSize / 15f);
        return fallback;
    }

    @Override
    public void dispose() {
    }
}
