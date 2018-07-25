package com.ting.privatephoto.util;

public class Log {
    public static final boolean DEBUG_LOG = Config.DEBUG_LOG;

    public static void d(String tag, String msg, Throwable tr) {
        if (DEBUG_LOG) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    public static void d(String tag, String msg) {
        if (DEBUG_LOG) {
            android.util.Log.d(tag, msg, null);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG_LOG) {
            android.util.Log.e(tag, msg, null);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (DEBUG_LOG) {
            android.util.Log.e(tag, msg, tr);
        }
    }

    public static void printStackTrace(Throwable e) {
        if (DEBUG_LOG) {
            e.printStackTrace();
        }
    }
}