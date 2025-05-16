package com.example.skincancer.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.skincancer.R;
import com.example.skincancer.activities.Skin.Actinic;
import com.example.skincancer.activities.Skin.Basal;
import com.example.skincancer.activities.Skin.Dermatofibroma;
import com.example.skincancer.activities.Skin.Melanoma;
import com.example.skincancer.activities.Skin.Nevus;
import com.example.skincancer.activities.Skin.Pigmented;
import com.example.skincancer.activities.Skin.Seborrheic;
import com.example.skincancer.activities.Skin.Squamous;
import com.example.skincancer.activities.Skin.Vascular;
import com.makeramen.roundedimageview.RoundedImageView;
import java.io.IOException;

public class MainActivity2 extends AppCompatActivity {

    private Classifier mClassifier;
    private Bitmap mBitmap;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 2;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private static final int INPUT_SIZE = 224;
    private static final String MODEL_PATH = "skin_model.tflite";
    private static final String LABEL_PATH = "skin_labels.txt";
    private static final String SAMPLE_PATH = "skin_cancer2.png";

    private RoundedImageView mPhotoImageView;
    private TextView mResultTextView;
    private TextView b_text;
    private Button mGalleryButton;
    private Button mCameraButton;
    private Button mDetectButton;
    private LinearLayout linear;
    private TextView advice_text;
    private TextView refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        // Initialize views
        mPhotoImageView = findViewById(R.id.mPhotoImageView);
        mResultTextView = findViewById(R.id.mResultTextView);
        b_text = findViewById(R.id.b_text);
        mGalleryButton = findViewById(R.id.mGalleryButton);
        mCameraButton = findViewById(R.id.mCameraButton);
        mDetectButton = findViewById(R.id.mDetectButton);
        linear = findViewById(R.id.linear);
        advice_text = findViewById(R.id.advice_text);
        refresh = findViewById(R.id.refresh);

        try {
            mClassifier = new Classifier(getAssets(), MODEL_PATH, LABEL_PATH, INPUT_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mBitmap = BitmapFactory.decodeStream(getResources().getAssets().open(SAMPLE_PATH));
            mBitmap = Bitmap.createScaledBitmap(mBitmap, INPUT_SIZE, INPUT_SIZE, true);
            mPhotoImageView.setImageBitmap(mBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCameraButton.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        mGalleryButton.setOnClickListener(v -> {
            Intent callGalleryIntent = new Intent(Intent.ACTION_PICK);
            callGalleryIntent.setType("image/*");
            startActivityForResult(callGalleryIntent, GALLERY_REQUEST_CODE);
            mDetectButton.setVisibility(View.VISIBLE);
            b_text.setVisibility(View.GONE);
        });

        mDetectButton.setOnClickListener(v -> detectSkinDisease());

        refresh.setOnClickListener(v -> resetDetection());
    }

    public void Precaution(View view) {
        if (mBitmap == null) {
            Toast.makeText(this, "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        Classifier.Recognition results = mClassifier.recognizeImage(mBitmap).get(0);
        String detectedClass = results.getTitle();

        Intent intent = null;
        switch (detectedClass) {
            case "actinic keratosis":
                intent = new Intent(this, Actinic.class);
                break;
            case "basal cell carcinoma":
                intent = new Intent(this, Basal.class);
                break;
            case "dermatofibroma":
                intent = new Intent(this, Dermatofibroma.class);
                break;
            case "melanoma":
                intent = new Intent(this, Melanoma.class);
                break;
            case "nevus":
                intent = new Intent(this, Nevus.class);
                break;
            case "pigmented benign keratosis":
                intent = new Intent(this, Pigmented.class);
                break;
            case "seborrheic keratosis":
                intent = new Intent(this, Seborrheic.class);
                break;
            case "squamous cell carcinoma":
                intent = new Intent(this, Squamous.class);
                break;
            case "vascular lesion":
                intent = new Intent(this, Vascular.class);
                break;
            default:
                Toast.makeText(this, "No specific precautions available.", Toast.LENGTH_SHORT).show();
                return;
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                mBitmap = (Bitmap) data.getExtras().get("data");
                if (mBitmap != null) {
                    mBitmap = scaleImage(mBitmap);
                    mPhotoImageView.setImageBitmap(mBitmap);
                    mDetectButton.setVisibility(View.VISIBLE);
                    b_text.setVisibility(View.GONE);
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                mBitmap = scaleImage(mBitmap);
                mPhotoImageView.setImageBitmap(mBitmap);
                mDetectButton.setVisibility(View.VISIBLE);
                b_text.setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectSkinDisease() {
        Classifier.Recognition results = mClassifier.recognizeImage(mBitmap).get(0);
        mResultTextView.setText(results.getTitle() + "\n Confidence: " + results.getConfidence());

        advice_text.setText("Precautions for " + results.getTitle());
        linear.setVisibility(View.VISIBLE);
        refresh.setVisibility(View.VISIBLE);
    }

    private void resetDetection() {
        mPhotoImageView.setImageResource(R.drawable.skin_cancer);
        refresh.setVisibility(View.GONE);
        mResultTextView.setText("Skin Cancer result will show here..");
        mResultTextView.setTextColor(Color.BLACK);
        linear.setVisibility(View.GONE);
        mGalleryButton.setVisibility(View.VISIBLE);
        mDetectButton.setVisibility(View.INVISIBLE);
        b_text.setVisibility(View.VISIBLE);
    }

    public Bitmap scaleImage(Bitmap bitmap) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float scaleWidth = INPUT_SIZE / (float) originalWidth;
        float scaleHeight = INPUT_SIZE / (float) originalHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true);
    }
}
