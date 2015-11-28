package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;


public class MainActivity extends Activity implements CNNListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    String imagePath;
    // handle camera parameter
    private Camera mCamera;

    // caffe model paramaters
    private CaffeMobile caffeMobile;
    private static String[] SCENE_CLASSES;
    private static final String caffeModelPath = "/sdcard/caffe_mobile/bvlc_reference_caffenet/deploy_mobile.prototxt";
    private static final String caffeWeightPath = "/sdcard/caffe_mobile/bvlc_reference_caffenet/bvlc_reference_caffenet.caffemodel";
    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    // util library
    private Util util;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        util = new Util();
        initializeCaffeManager();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.main_surfaceView_camera);
        surfaceView.getHolder().addCallback(surfaceCallback);
    }

    private void initializeCaffeManager()
    {
        caffeMobile = new CaffeMobile();
        caffeMobile.enableLog(true);
        caffeMobile.loadModel(caffeModelPath, caffeWeightPath);

        AssetManager am = this.getAssets();
        try {
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            ArrayList<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            SCENE_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
//            String imgPath;
//
//            if (requestCode == REQUEST_IMAGE_CAPTURE) {
//                imgPath = fileUri.getPath();
//            } else {
//                Uri selectedImage = data.getData();
//                String[] filePathColumn = { MediaStore.Images.Media.DATA };
//                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
//                cursor.moveToFirst();
//                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//                imgPath = cursor.getString(columnIndex);
//                cursor.close();
//            }
//
//            bmp = BitmapFactory.decodeFile(imgPath);
//            Log.d(LOG_TAG, imgPath);
//            Log.d(LOG_TAG, String.valueOf(bmp.getHeight()));
//            Log.d(LOG_TAG, String.valueOf(bmp.getWidth()));
//
//            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);
//
//            CNNTask cnnTask = new CNNTask(MainActivity.this);
//            cnnTask.execute(imgPath);
//        } else {
//            btnCamera.setEnabled(true);
//            btnSelect.setEnabled(true);
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    private class CNNTask extends AsyncTask<String, Void, Integer> {
        private CNNListener listener;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            return caffeMobile.predictImage(strings[0]);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }

    @Override
    public void onTaskCompleted(int result) {
        //ivCaptured.setImageBitmap(bmp);
        //tvLabel.setText(SCENE_CLASSES[result]);
        Log.d(LOG_TAG, "done !!");
        System.out.print(SCENE_CLASSES[result]);
        //btnCamera.setEnabled(true);
        //btnSelect.setEnabled(true);
        ((TextView) findViewById(R.id.main_textView_detection)).setText(SCENE_CLASSES[result]);
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
        //if (dialog != null) {
        //    dialog.dismiss();
        //}
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Override surfaceCreated(), surfaceChanged() and SurfaceDestroyed() to handle
     * camera event
     */
    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (mCamera != null) {
                    mCamera.setPreviewDisplay(holder);
                } else {
                    Log.e(LOG_TAG, "Camera cannot be open");
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null) return;
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(1920, 1080);

            Camera.Size s = util.getBestSupportedSize(parameters.getSupportedPictureSizes());
            parameters.setPictureSize(s.width, s.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(parameters);

            try {
                mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.startPreview();
                Log.d(LOG_TAG, "startPreview");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Could not start preview");
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
        }
    };

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            imagePath = util.saveImageToPath(data, parameters, "/caffe_mobile/test.jpg");
            CNNTask cnnTask = new CNNTask(MainActivity.this);
            cnnTask.execute(imagePath);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mCamera = Camera.open(0);// 0 is back face camera, 1 is front face camera
        mCamera.setDisplayOrientation(90);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

}
