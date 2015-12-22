package com.sh1r0.caffe_android_demo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Scanner;


public class MainActivity extends Activity implements CNNListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    String imagePath;

    // handle camera parameter
    private Camera mCamera;

    // caffe model parameters
    private CaffeMobile caffeMobile;
    private static String[] SCENE_CLASSES;
    private static final String caffeModelPath = "/sdcard/caffe_mobile/bvlc_reference_caffenet/deploy_mobile.prototxt";
    private static final String caffeWeightPath = "/sdcard/caffe_mobile/bvlc_reference_caffenet/bvlc_reference_caffenet.caffemodel";


    private String mOutputImage;
    private final int numReturn = 3;
    private final int numClass = 40;
    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    // util library
    private Util util;

    // previous frame
    private ArrayList<Float> disp_prob; // Average of following three
    private ArrayList<Float> current_prob;
    private ArrayList<Float> prev_prob;
    private ArrayList<Float> prev_prob2;
    // index result
    private ArrayList<Integer> indexResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        util = new Util();
        initializeCaffeManager();
        initializeDataArray();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.main_surfaceView_camera);
        surfaceView.getHolder().addCallback(surfaceCallback);

        Button button = (Button) findViewById(R.id.photo);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.takePicture(null, null, jpegCallback);
            }
        });

        DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
        String date = df.format(Calendar.getInstance().getTime());
        String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String filePath = Environment.getExternalStorageDirectory().toString() + "/failure/";
        File path = new File(filePath);
        if (!path.exists()) {
            path.mkdir();
        }
        mOutputImage = filePath + date + "_" + android_id + ".jpg";
    }

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] imageData,
                                   android.hardware.Camera camera) {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmssZ");
            String date = df.format(Calendar.getInstance().getTime());
            String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String filePath = Environment.getExternalStorageDirectory().toString() + "/failure/";
            mOutputImage = filePath + date + "_" + android_id + ".jpg";
            if (imageData != null) {
                compressByteImage(imageData, 100, mOutputImage);
                mCamera.startPreview();
            }
        }
    };

    public boolean compressByteImage(byte[] imageData,
                                     int quality, String filename) {

        FileOutputStream fileOutputStream;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1; // no downsampling
            Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
                    imageData.length, options);
            fileOutputStream = new FileOutputStream(filename);

            BufferedOutputStream bos = new BufferedOutputStream(
                    fileOutputStream);

            // compress image to jpeg
            myImage.compress(Bitmap.CompressFormat.JPEG, quality, bos);

            bos.flush();
            bos.close();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException");
            e.printStackTrace();
        }

        Bitmap bmp = BitmapFactory.decodeFile(filename);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();

        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    private void initializeCaffeManager() {
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

    private void initializeDataArray() {
        disp_prob = new ArrayList<Float>();
        current_prob = new ArrayList<Float>();
        prev_prob = new ArrayList<Float>();
        prev_prob2 = new ArrayList<Float>();
        indexResult = new ArrayList<Integer>();
        for (int i=0;i<numClass; ++i) {
            prev_prob.add(0.0f);
            prev_prob2.add(0.0f);
            disp_prob.add(0.0f);
            current_prob.add(0.0f);
        }
    }

    private class CNNTask extends AsyncTask<String, Void, ArrayList<Integer>> {
        private CNNListener listener;
        private ArrayList<Integer> resultList;
        public CNNTask(CNNListener listener) {
            this.listener = listener;
            resultList = new ArrayList<Integer>();
        }

        @Override
        protected ArrayList<Integer> doInBackground(String... strings) {
            int[] ccc = caffeMobile.predictImage(strings[0]);
            for (int i = 0; i < numClass; ++i)
            {
                resultList.add(ccc[i]);
            }
            return resultList;
        }

        @Override
        protected void onPostExecute(ArrayList<Integer> integer) {
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }

    @Override
    public void onTaskCompleted(ArrayList<Integer> result) {
        updateCurrentContainer(result);
        setCurrentResult();
        setSupportingTextInfo();
        File file = new File(imagePath);
        if (file.exists()) {
            file.delete();
        }
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

    /**
     * mPreviewCallback, which get invoked every frame the camera received
     */
    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            int a = getResources().getConfiguration().orientation;
            System.out.println(a);System.out.println(a);System.out.println(a);System.out.println(a);System.out.println(a);
            imagePath = util.saveImageToPath(data, parameters, "/caffe_mobile/test.jpg");
            CNNTask cnnTask = new CNNTask(MainActivity.this);
            cnnTask.execute(imagePath);
        }
    };


    public void onResume() {
        super.onResume();
        mCamera = Camera.open(0);// 0 is back face camera, 1 is front face camera
        if (mPreviewCallback != null) {
            mCamera.setPreviewCallback(mPreviewCallback);
        }
        mCamera.setDisplayOrientation(90);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private void updateCurrentContainer(ArrayList<Integer> result) {
        prev_prob2 = prev_prob;
        prev_prob = current_prob;
        for (int i=0;i<result.size();++i) {
            current_prob.set(i, (float) result.get(i) / 1000);
        }
    }

    private void setCurrentResult() {
        for (int i=0;i<current_prob.size();++i) {
            disp_prob.set(i, current_prob.get(i) * 0.6f + prev_prob.get(i) * 0.3f + prev_prob2.get(i) * 0.1f);
        }
        ArrayList<Float> copy = new ArrayList<Float>(disp_prob);
        Collections.sort(disp_prob);
        Collections.reverse(disp_prob);
        indexResult.clear();
        for (int i=0;i<numReturn;++i) {
            indexResult.add(copy.indexOf(disp_prob.get(i)));
        }
    }

    private void setSupportingTextInfo() {
        ((TextView) findViewById(R.id.main_textView_detection)).setText(SCENE_CLASSES[indexResult.get(0)]);
        ((TextView) findViewById(R.id.main_textView_detection2)).setText(SCENE_CLASSES[indexResult.get(1)]);
        ((TextView) findViewById(R.id.main_textView_detection3)).setText(SCENE_CLASSES[indexResult.get(2)]);
        ((TextView) findViewById(R.id.main_textView_detection_prob)).setText(String.valueOf(disp_prob.get(0)));
        ((TextView) findViewById(R.id.main_textView_detection2_prob)).setText(String.valueOf(disp_prob.get(1)));
        ((TextView) findViewById(R.id.main_textView_detection3_prob)).setText(String.valueOf(disp_prob.get(2)));
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
}
