package org.opencv.samples.cameracalibration;

import android.os.Build;

public class GlassUtil {
    public static boolean isGlass() {
        return Build.MODEL.toUpperCase().contains("GLASS");
    }
}
