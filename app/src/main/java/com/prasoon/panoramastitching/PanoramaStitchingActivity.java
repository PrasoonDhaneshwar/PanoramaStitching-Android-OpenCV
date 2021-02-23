package com.prasoon.panoramastitching;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
//import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.hardware.Camera.open;

public class PanoramaStitchingActivity extends AppCompatActivity {

    static {
        System.loadLibrary("MyLibs");
        System.loadLibrary("opencv_java");
    }

    private TextView mTextViewJni;

    private Button captureBtn, saveBtn; // Capture and Save Button
    private SurfaceView mSurfaceView, mSurfaceViewOnTop; // Used to display the camera frame in UI
    private Camera mCam = null;
    private boolean isPreview; // Is the camera frame displaying?
    private boolean safeToTakePicture = true; // Is it safe to capture a picture?

    ProgressDialog ringProgressDialog;


    SurfaceHolder.Callback mSurfaceCallBack = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            try {
                mCam.setPreviewDisplay(holder);
            } catch (IOException e) {

            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            // Get the default parameters for Camera
            Camera.Parameters myParameters = mCam.getParameters();
            // Select the best preview size
            Camera.Size myBestSize = getBestPreviewSize(myParameters);
            if (myBestSize != null) {
                // Set the preview size
                myParameters.setPreviewSize(myBestSize.width, myBestSize.height);

                // Set the parameters to the camera
                mCam.setParameters(myParameters);
                // Rotate the display frame 90 degree to view in portrait mode
                mCam.setDisplayOrientation(90);
                // Start the preview
                isPreview = true;
            }
            // Extra added to avoid camera error
            mCam.startPreview();
            safeToTakePicture = true;
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        }
    };

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        Camera.Size bestSize = sizeList.get(0);

        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        return bestSize;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCam = open(0); // 0 for back camera
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPreview) {
            mCam.stopPreview();
        }
        mCam.release();
        mCam = null;
        isPreview = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // mTextViewJni = findViewById(R.id.textViewJni);
        // mTextViewJni.setText(NativePanorama.getMessageFromJni());

        isPreview = false;
        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceCallBack);

        mSurfaceViewOnTop = findViewById(R.id.surfaceViewOnTop);
        mSurfaceViewOnTop.setZOrderOnTop(true); // Always on top
        mSurfaceViewOnTop.getHolder().setFormat(PixelFormat.TRANSPARENT);

        List<Mat> listImage = new ArrayList<>();

        captureBtn = findViewById(R.id.capture);


        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // decode the byte array to a bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                // Extra maybe added to avoid camera error
                //camera.startPreview();
                // Rotate the picture to fit portrait mode
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                        bitmap.getHeight(), matrix, false);

                // TODO: Save the image to a list to pass them to OpenCV method
                Mat mat = new Mat();
                Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                Utils.bitmapToMat(bmp32, mat);
                listImage.add(mat);

                Canvas canvas = null;
                try {
                    canvas = mSurfaceViewOnTop.getHolder().lockCanvas(null);
                    synchronized (mSurfaceViewOnTop.getHolder()) {
                        // Clear canvas
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                        // Scale the image to fit the SurfaceView
                        float scale = 1.0f * mSurfaceView.getHeight() / bitmap.getHeight();
                        Bitmap scaleImage = Bitmap.createScaledBitmap(bitmap,
                                (int) (scale * bitmap.getWidth()), mSurfaceView.getHeight(), false);

                        Paint paint = new Paint();
                        // Set the opacity of the image
                        paint.setAlpha(200);

                        // Draw the image with an offset so we only see one third of the image.
                        canvas.drawBitmap(scaleImage, -scaleImage.getWidth() * 2 / 3, 0, paint);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mSurfaceViewOnTop.getHolder().unlockCanvasAndPost(canvas);
                    }
                }
                mCam.startPreview();
                safeToTakePicture = true;
            }
        };

        View.OnClickListener captureOnClickLister = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCam != null && safeToTakePicture) {
                    // set the flag to false so we don't take 2 pictures at the same time
                    safeToTakePicture = false;
                    mCam.takePicture(null, null, jpegCallback);
                }
            }
        };
        captureBtn.setOnClickListener(captureOnClickLister);


        saveBtn = findViewById(R.id.save);
        Runnable imageProcessingRunnable = new Runnable() {
            @Override
            public void run() {
                showProcessingDialog();
                // TODO: implement OpenCV parts
                try {
                    // Create a long array to store all image address
                    int elems = listImage.size();
                    Log.i("Panorama","elems " +  elems);

                    long[] tempobjadr = new long[elems];
                    for (int i = 0; i < elems; i++) {
                        tempobjadr[i] = listImage.get(i).getNativeObjAddr();
                        Log.i("Panorama","tempobjadr["+i+"] " +  tempobjadr[i]);

                    }

                    // Create a Mat to store the final panorama image
                    Mat result = new Mat();
                    // Call the OpenCV C++ code to perform stitching process
                    int ret = NativePanorama.processPanorama(tempobjadr, result.getNativeObjAddr());
                    if(ret==0){
                        Log.i("Panorama","ret " +  ret);
                    }
                    else{
                        Log.i("Panorama","ret " +  ret);
                    }
                    // Save the image to external storage
                    String directoryName = Environment.DIRECTORY_PICTURES;
                    File sdcard = Environment.getExternalStoragePublicDirectory(directoryName);

                    sdcard.mkdirs();
                    final String fileName = sdcard + "/openCV_" + System.currentTimeMillis() + ".png";
                    try {
                        boolean bool = Highgui.imwrite(fileName, result);
                        if (bool)
                            Log.i("Panorama","SUCCESS writing image to external storage" +  fileName);
                        else
                            Log.i("Panorama", "Fail writing image to external storage" + fileName);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (ret ==1 ){
                                Toast.makeText(getApplicationContext(), "File saved at: " + fileName, Toast.LENGTH_LONG).show();
                            }else
                            Toast.makeText(getApplicationContext(), "File NOT saved. ", Toast.LENGTH_LONG).show();

                        }
                    });
                    listImage.clear();
                }catch (Exception e){
                    e.printStackTrace();
                }
                closeProcessingDialog();
            }

            private void closeProcessingDialog() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCam.startPreview();
                        ringProgressDialog.dismiss();
                    }
                });
            }

            private void showProcessingDialog() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCam.stopPreview();
                        ringProgressDialog = ProgressDialog.show(PanoramaStitchingActivity.this, "", "Panorama", true);
                        ringProgressDialog.setCancelable(false);
                    }
                });
            }
        };

        View.OnClickListener saveOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Thread thread = new Thread(imageProcessingRunnable);
                thread.start();
            }
        };
        saveBtn.setOnClickListener(saveOnClickListener);
    }
}