package com.sh1r0.caffe_android_demo;


import android.hardware.Camera;

import java.util.List;

public class Util {

    public Camera.Size getBestSupportedSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = sizes.get(0);
        int largestArea = bestSize.width * bestSize.height;
        for (Camera.Size s : sizes) {
            int area = s.width * s.height;
            if (area > largestArea) {
                bestSize = s;
                largestArea = area;
            }
        }
        return bestSize;
    }

}
