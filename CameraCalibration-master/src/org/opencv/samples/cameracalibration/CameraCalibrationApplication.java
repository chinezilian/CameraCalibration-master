package org.opencv.samples.cameracalibration;

import org.opencv.android.OpenCVLoader;

import android.app.Application;

public class CameraCalibrationApplication extends Application {
    static {
        OpenCVLoader.initDebug();
    }
}
