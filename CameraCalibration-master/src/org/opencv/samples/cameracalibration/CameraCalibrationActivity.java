// This sample is based on "Camera calibration With OpenCV" tutorial:
// http://docs.opencv.org/doc/tutorials/calib3d/camera_calibration/camera_calibration.html
//
// It uses standard OpenCV asymmetric circles grid pattern 11x4:
// https://github.com/Itseez/opencv/blob/2.4/doc/acircles_pattern.png.
// The results are the camera matrix and 5 distortion coefficients.
//
// Tap on highlighted pattern to capture pattern corners for calibration.
// Move pattern along the whole screen and capture data.
//
// When you've captured necessary amount of pattern corners (usually ~20 are enough),
// press "Calibrate" button for performing camera calibration.

package org.opencv.samples.cameracalibration;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class CameraCalibrationActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender mOnCameraFrameRender;
    private int mWidth;
    private int mHeight;
    private GestureDetector mGestureDetector;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
                mOpenCvCameraView.setOnTouchListener(CameraCalibrationActivity.this);
            }
                break;
            default: {
                super.onManagerConnected(status);
            }
                break;
            }
        }
    };

    public CameraCalibrationActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.camera_calibration_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openOptionsMenu();
                return true;
            }
        });

        if (GlassUtil.isGlass()) {
            mGestureDetector = createGestureDetector(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.calibration, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.preview_mode).setEnabled(true);
        if (!mCalibrator.isCalibrated())
            menu.findItem(R.id.preview_mode).setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.calibration:
            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
            item.setChecked(true);
            return true;
        case R.id.undistortion:
            mOnCameraFrameRender = new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
            item.setChecked(true);
            return true;
        case R.id.comparison:
            mOnCameraFrameRender = new OnCameraFrameRender(new ComparisonFrameRender(mCalibrator, mWidth, mHeight,
                    getResources()));
            item.setChecked(true);
            return true;
        case R.id.calibrate:
            final Resources res = getResources();
            if (mCalibrator.getCornersBufferSize() < 2) {
                (Toast.makeText(this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
                return true;
            }

            mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender());
            new AsyncTask<Void, Void, Void>() {
                private ProgressDialog calibrationProgress;

                @Override
                protected void onPreExecute() {
                    calibrationProgress = new ProgressDialog(CameraCalibrationActivity.this);
                    calibrationProgress.setTitle(res.getString(R.string.calibrating));
                    calibrationProgress.setMessage(res.getString(R.string.please_wait));
                    calibrationProgress.setCancelable(false);
                    calibrationProgress.setIndeterminate(true);
                    calibrationProgress.show();
                }

                @Override
                protected Void doInBackground(Void... arg0) {
                    mCalibrator.calibrate();
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    calibrationProgress.dismiss();
                    mCalibrator.clearCorners();
                    mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
                    String resultMessage = (mCalibrator.isCalibrated()) ? res
                            .getString(R.string.calibration_successful) + " " + mCalibrator.getAvgReprojectionError()
                            : res.getString(R.string.calibration_unsuccessful);
                    (Toast.makeText(CameraCalibrationActivity.this, resultMessage, Toast.LENGTH_SHORT)).show();

                    if (mCalibrator.isCalibrated()) {
                        CalibrationResult.save(CameraCalibrationActivity.this, mCalibrator.getCameraMatrix(),
                                mCalibrator.getDistortionCoefficients());
                    }
                }
            }.execute();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients())) {
                mCalibrator.setCalibrated();
            }

            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
        }
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return mOnCameraFrameRender.render(inputFrame);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, "onTouch invoked");

        mCalibrator.addCorners();
        return false;
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        // Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    openOptionsMenu();
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    mCalibrator.addCorners();
                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
}
