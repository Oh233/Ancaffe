package com.sh1r0.caffe_android_demo;


import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    public String saveImageToPath(byte[] data, Camera.Parameters parameters, String desPath) {
        Camera.Size size = parameters.getPreviewSize();
        String filePath = Environment.getExternalStorageDirectory().getPath() + desPath;
        try {
            data = rotateYUV90(data, size.width, size.height);
            int rotatedHeight = size.width;
            int rotatedWidth = size.height;
            YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                    rotatedWidth, rotatedHeight, null);
            File file = new File(filePath);
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, fos);
            }

        } catch (FileNotFoundException e) {

        }
        return filePath;
    }

    private byte[] rotateYUV90(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y color component
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight-1; y >= 0; y--) {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for (int x = imageWidth-1;x > 0;x=x-2) {
            for (int y = 0;y < imageHeight/2;y++) {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

}
