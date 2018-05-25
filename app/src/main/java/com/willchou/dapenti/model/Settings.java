package com.willchou.dapenti.model;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.willchou.dapenti.R;

public class Settings {
    static private Resources resources;
    static private SharedPreferences prefs;

    static public final int FontSizeSmall = 0;
    static public final int FontSizeMedia = 1;
    static public final int FontSizeBig = 2;
    static public final int FontSizeSuperBig = 3;

    static public void initiate(SharedPreferences p, Resources r) {
        prefs = p;
        resources = r;
    }

    static public int getFontSize() {
        String s = prefs.getString(resources.getString(R.string.pref_key_font_size), "");

        int fontSize = FontSizeMedia;
        switch (s) {
            case "small":
                fontSize = FontSizeSmall;
                break;
            case "big":
                fontSize = FontSizeBig;
                break;
            case "super":
                fontSize = FontSizeSuperBig;
                break;
        }

        return fontSize;
    }

    static public boolean isImageEnabled() {
        return prefs.getBoolean(resources.getString(R.string.pref_key_display_image), true);
    }
}
