package com.tondz.phathiengianlanthicu;

import android.content.res.AssetManager;
import android.view.Surface;

public class CelebSDK {
    public native boolean loadModel(AssetManager assetManager);

    public native boolean openCamera(int facing);

    public native boolean closeCamera();

    public native boolean setOutputWindow(Surface surface);
    public native String getPose();

    static {
        System.loadLibrary("celebsdk");
    }
}
