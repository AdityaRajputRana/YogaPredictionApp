package com.example.yogapredictionapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import com.example.yogapredictionapp.CameraDetection.CameraConnectionFragment;
import com.example.yogapredictionapp.CameraDetection.ImageUtils;
import com.example.yogapredictionapp.Utils.Constants;
import com.example.yogapredictionapp.Utils.Tools;
import com.example.yogapredictionapp.ml.Final;
import com.example.yogapredictionapp.ml.Final1;
import com.example.yogapredictionapp.ml.Final2;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LiveDetection extends AppCompatActivity implements ImageReader.OnImageAvailableListener {

    Bitmap img;
    TextView textView;
    TextView d1, d2, d3;
    Boolean isShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detection);

        setUpUI();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA}, 121);
            }else{
                setFragment();
            }
        } else {
            setFragment();

        }
    }

    private void setUpUI() {
        textView = findViewById(R.id.textview);


        findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        d1 = findViewById(R.id.txt1);
        d2 = findViewById(R.id.txt2);
        d3 = findViewById(R.id.txt3);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isShown) {
                    d1.setVisibility(View.GONE);
                    d2.setVisibility(View.GONE);
                    d3.setVisibility(View.GONE);
                } else {
                    d1.setVisibility(View.VISIBLE);
                    d2.setVisibility(View.VISIBLE);
                    d3.setVisibility(View.VISIBLE);
                }
                isShown = !isShown;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            finish();
        }
    }

    int previewHeight = 0,previewWidth = 0;
    int sensorOrientation;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void setFragment() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = null;
        try {
            cameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        CameraConnectionFragment fragment;
        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        new CameraConnectionFragment.ConnectionCallback() {
                            @Override
                            public void onPreviewSizeChosen(final Size size, final int rotation) {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                Log.d("tryOrientation","rotation: "+rotation+"   orientation: "+getScreenOrientation()+"  "+previewWidth+"   "+previewHeight);
                                sensorOrientation = rotation - getScreenOrientation();
                            }
                        },
                        this,
                        R.layout.camera_fragment,
                        new Size(640, 480));

        camera2Fragment.setCamera(cameraId);
        fragment = camera2Fragment;
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Bitmap rgbFrameBitmap;

    @Override
    public void onImageAvailable(ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();

        } catch (final Exception e) {
            Log.d("tryError",e.getMessage());
            return;
        }
    }

    private void processImage() {
        imageConverter.run();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        //Do your work here
        img = rgbFrameBitmap;
        runClassification();
        new Handler().postDelayed(postInferenceCallback, 1000);
//        postInferenceCallback.run();
    }

    Final model1;
    Final1 model2;
    Final2 model3;


    private void runClassification() {
        Log.i("Info", "runningClassification");
        img = Bitmap.createScaledBitmap(img, 150, 150, true);
        try {
            if (model1 == null) {
                 model1 = Final.newInstance(getApplicationContext());
                 model2 = Final1.newInstance(getApplicationContext());
                 model3 = Final2.newInstance(getApplicationContext());
            }

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();

            inputFeature0.loadBuffer(byteBuffer);


            int index1 = Tools.getIndexOfLargest(model1.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());
            int index2 = Tools.getIndexOfLargest(model2.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());
            int index3 = Tools.getIndexOfLargest(model3.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());



            textView.setText(Tools.getYogaPose(index1, index2, index3,
                    d1, d2, d3));


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }
}