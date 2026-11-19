package com.idrees.emberhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

/**
 * Runtime capability checks and responsive sizing for Android TV and Fire TV.
 * Keep this class free of APIs newer than minSdk 19 unless guarded at runtime.
 */
final class DeviceProfile {
    private DeviceProfile() { }

    static boolean isAmazonFireTv() {
        return contains(Build.MANUFACTURER, "amazon")
                || contains(Build.BRAND, "amazon")
                || contains(Build.MODEL, "aft")
                || contains(Build.DEVICE, "aft");
    }

    static boolean isLowRamDevice(Context context) {
        try {
            ActivityManager manager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            return manager == null || manager.isLowRamDevice();
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Choose a grid density from available logical width, not TV diagonal size.
     * This avoids assuming every television reports the same pixel density.
     */
    static int gridColumns(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density > 0f ? metrics.density : 1f;
        float widthDp = metrics.widthPixels / density;

        if (widthDp >= 1200f) return 7;
        if (widthDp >= 980f) return 6;
        if (widthDp >= 780f) return 5;
        if (widthDp >= 560f) return 4;
        return 3;
    }

    static boolean supportsImmersiveMode() {
        return Build.VERSION.SDK_INT >= 19;
    }

    private static boolean contains(String value, String fragment) {
        return value != null && value.toLowerCase().contains(fragment);
    }
}
