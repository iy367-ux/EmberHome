package com.idrees.emberhome;

import android.os.Build;

/**
 * Hardware profile for the Wisdom Share / CVTE MStar PB801 target.
 *
 * Target observed on the owner's television:
 * CVTE, SMART_TV_PB801, BD_TP_MS338_PB801, Android 4.4.4 / API 19,
 * approximately 480 MB reported RAM.
 *
 * This class intentionally uses only public Android APIs available at API 19.
 * It does not attempt privileged CVTE/MStar operations.
 */
final class Msd338Profile {
    private Msd338Profile() { }

    static boolean isTargetDevice() {
        return contains(Build.MANUFACTURER, "cvte")
                && (contains(Build.DEVICE, "pb801")
                    || contains(Build.PRODUCT, "pb801")
                    || contains(Build.HARDWARE, "ms338"));
    }

    static boolean isKitKat() {
        return Build.VERSION.SDK_INT == 19;
    }

    /** Keep animation and artwork budgets conservative on the 512 MB class board. */
    static int maxHomeArtworkPixels() {
        return 1280 * 720;
    }

    static int maxPosterWidthPx() {
        return 320;
    }

    static int maxPosterHeightPx() {
        return 180;
    }

    static int recommendedHomeColumns() {
        return 5;
    }

    static boolean allowBackgroundVideo() {
        return false;
    }

    static boolean allowParallax() {
        return false;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
