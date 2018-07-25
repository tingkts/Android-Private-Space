package com.ting.privatephoto.util;

import android.os.Build;
import android.os.SystemProperties;

public class Config {
    private static final String PROPERTY_KEY_DEBUG_LOG = "persist.pp.debug_log";
    private static final String PROPERTY_KEY_DEBUG_UI = "persist.pp.debug.ui";
    private static final String PROPERTY_KEY_SHOW_CYCLE_PROGRESS = "persist.pp.show_cycle_progress";

    public static final boolean DEBUG_LOG = SystemProperties.getBoolean(PROPERTY_KEY_DEBUG_LOG, true);
    public static final boolean DEBUG_UI = SystemProperties.getBoolean(PROPERTY_KEY_DEBUG_UI, false);
    public static final boolean DEBUG = /*Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug")*/DEBUG_LOG;

    public static final boolean SHOW_CYCLE_PROGRESS = SystemProperties.getBoolean(
            PROPERTY_KEY_SHOW_CYCLE_PROGRESS, false);

    public static void print() {
        android.util.Log.d(Config.class.getSimpleName(), "DEBUG_LOG=" + DEBUG_LOG + ", DEBUG_UI="
                + DEBUG_UI + ", SHOW_CYCLE_PROGRESS=" + SHOW_CYCLE_PROGRESS);
    }
}