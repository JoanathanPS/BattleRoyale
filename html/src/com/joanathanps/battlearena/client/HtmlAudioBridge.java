package com.joanathanps.battlearena.client;

import com.joanathanps.battlearena.audio.AudioBridge;

public class HtmlAudioBridge implements AudioBridge {
    @Override
    public native void play(String name, float volume) /*-{
        if ($wnd.playGameSound) {
            $wnd.playGameSound(name, volume);
        }
    }-*/;
}

