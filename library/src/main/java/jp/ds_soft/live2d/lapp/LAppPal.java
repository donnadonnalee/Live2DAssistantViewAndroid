package jp.ds_soft.live2d.lapp;

import android.app.Activity;
import android.util.Log;
import com.live2d.sdk.cubism.core.ICubismLogger;
import java.io.IOException;
import java.io.InputStream;

public class LAppPal {
    public static class PrintLogFunction implements ICubismLogger {
        @Override
        public void print(String message) {
            Log.d(TAG, message);
        }
    }

    public static void moveTaskToBack() {
        Activity activity = LAppDelegate.getInstance().getActivity();
        if (activity != null) {
            activity.moveTaskToBack(true);
        }
    }

    public static void updateTime() {
        s_currentFrame = getSystemNanoTime();
        if (_lastNanoTime == 0) {
            _lastNanoTime = s_currentFrame;
        }
        _deltaNanoTime = s_currentFrame - _lastNanoTime;
        _lastNanoTime = s_currentFrame;
    }

    public static void resetTime() {
        _lastNanoTime = 0;
    }

    public static byte[] loadFileAsBytes(final String path) {
        InputStream fileData = null;
        try {
            fileData = LAppDelegate.getInstance().getContext().getAssets().open(path);

            int fileSize = fileData.available();
            byte[] fileBuffer = new byte[fileSize];
            fileData.read(fileBuffer, 0, fileSize);

            return fileBuffer;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static float getDeltaTime() {
        return (float) (_deltaNanoTime / 1000000000.0f);
    }

    public static void printLog(String message) {
        Log.d(TAG, message);
    }

    private static long getSystemNanoTime() {
        return System.nanoTime();
    }

    private static double s_currentFrame;
    private static double _lastNanoTime;
    private static double _deltaNanoTime;

    private static final String TAG = "[APP]";

    private LAppPal() {}
}
