package com.badlogic.gdx.graphics.g2d.freetype;

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

    public FreeTypeFontGenerator(FileHandle file) {
    }

    public BitmapFont generateFont(FreeTypeFontParameter parameter) {
        BitmapFont font = new BitmapFont();
        if (parameter != null && parameter.size > 0) {
            font.getData().setScale(Math.max(0.6f, (float) parameter.size / 16f));
        }
        return font;
    }

    @Override
    public void dispose() {
    }
}
