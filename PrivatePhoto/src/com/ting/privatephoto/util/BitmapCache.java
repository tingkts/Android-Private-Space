package com.ting.privatephoto.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;

public class BitmapCache extends LruCache<Long, Bitmap> {
    public static BitmapCache newInstance(float maxMemoryPercentage) {
        return new BitmapCache(Math.round(Runtime.getRuntime().maxMemory()*maxMemoryPercentage));
    }

    public BitmapCache(int maxSize) {
        super(maxSize);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected int sizeOf(Long key, Bitmap value) {
        return value.getAllocationByteCount();
    }
}