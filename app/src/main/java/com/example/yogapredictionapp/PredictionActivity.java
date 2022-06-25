package com.example.yogapredictionapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yogapredictionapp.Utils.Tools;
import com.example.yogapredictionapp.ml.Final;
import com.example.yogapredictionapp.ml.Final1;
import com.example.yogapredictionapp.ml.Final2;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PredictionActivity extends AppCompatActivity {

    ImageView imageView;
    Bitmap img;
    TextView textView;
    TextView d1, d2, d3;
    Boolean isShown = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        textView = findViewById(R.id.textview);
        imageView = findViewById(R.id.imageView);


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
        showSheetDialog();
    }

    BottomSheetDialog doubtSheet;

    private void showSheetDialog() {
        if (doubtSheet == null) {
            doubtSheet = new BottomSheetDialog(this);
            doubtSheet.setContentView(R.layout.sheet_upload_image);

            Button camera = doubtSheet.findViewById(R.id.cameraBtn);
            Button galleryBtn = doubtSheet.findViewById(R.id.galleryBtn);


            camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (checkImagePermissions()) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(takePictureIntent, 101);
                        doubtSheet.hide();
                    }
                }
            });

            galleryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    predict();
                    doubtSheet.hide();
                }
            });
        }
        doubtSheet.show();
    }

    public void predict() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Add image from"), 100);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            imageView.setImageURI(data.getData());

            Uri uri = data.getData();
            try {
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                runClassification();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == 101 && resultCode == RESULT_OK){
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            img = photo;
            runClassification();
            imageView.setImageBitmap(photo);
        }
    }

    private void runClassification() {
        img = Bitmap.createScaledBitmap(img, 150, 150, true);
        try {
            Final model1 = Final.newInstance(getApplicationContext());
            Final1 model2 = Final1.newInstance(getApplicationContext());
            Final2 model3 = Final2.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 150, 150, 3}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();

            inputFeature0.loadBuffer(byteBuffer);


            int index1 = Tools.getIndexOfLargest(model1.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());
            int index2 = Tools.getIndexOfLargest(model2.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());
            int index3 = Tools.getIndexOfLargest(model3.process(inputFeature0).getOutputFeature0AsTensorBuffer().getFloatArray());

            // Releases model resources if no longer used.
            model1.close();
            model2.close();
            model3.close();

            textView.setText(Tools.getYogaPose(index1, index2, index3,
                    d1, d2, d3));


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean checkImagePermissions() {
        return true;
    }
}