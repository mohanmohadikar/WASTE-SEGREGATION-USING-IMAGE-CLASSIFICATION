package com.example.wastesegregation;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CameraPreview extends AppCompatActivity {

    private Button btnCapture;
    private Button btnSelect;
    public ImageView imageCapture;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        imageCapture = (ImageView)findViewById(R.id.imageCapture);
        btnCapture = (Button)findViewById(R.id.btnCapture);
        btnSelect = (Button)findViewById(R.id.btnSelect);

        btnCapture.setOnClickListener(v->{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getPackageManager()) != null){

                startActivityForResult(intent,REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect.setOnClickListener(v->{
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(resultCode == RESULT_OK ){
            if(requestCode == REQUEST_IMAGE_CAPTURE){
                Bitmap bitmap = (Bitmap)data.getExtras().get("data");
                imageCapture.setImageBitmap(bitmap);

            }
            else if(requestCode == REQUEST_IMAGE_GALLERY){

                Uri uri = data.getData();
                try {
                    Bitmap bitmap = (Bitmap) MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageCapture.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
