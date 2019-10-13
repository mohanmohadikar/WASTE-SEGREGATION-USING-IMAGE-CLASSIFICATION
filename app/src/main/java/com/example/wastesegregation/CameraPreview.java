package com.example.wastesegregation;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class CameraPreview extends AppCompatActivity {

    // input image dimensions for the Inception Model
    private int DIM_IMG_SIZE_X = 224;
    private int DIM_IMG_SIZE_Y = 224;
    private int DIM_PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private int[] intValues=new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    // holds all the possible labels for model
    private List<String> labelList;
    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    private float[][] labelProbArray = null;
    private Button btnCapture;
    private Button btnSelect;
    public ImageView imageCapture;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    public static final int REQUEST_PERMISSION = 300;

    // request code for permission requests to the os for image
    public static final int REQUEST_IMAGE = 100;

    // will hold uri of image obtained from camera
    private Uri imageUri;

    // string to send to next activity that describes the chosen classifier
    private String chosen="colab_model.tflite";

    //boolean value dictating if chosen model is quantized version or not.
    private boolean quant = false;

    public String predictLabel = "";
    public String predictProbablility = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        try{
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];

        // request permission to use the camera on the user's phone
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.CAMERA}, REQUEST_PERMISSION);
        }

        // request permission to write data (aka images) to the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        // request permission to read data (aka images) from the user's external storage of their phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }


        imageCapture = (ImageView)findViewById(R.id.imageCapture);
        btnCapture = (Button)findViewById(R.id.btnCapture);
        btnSelect = (Button)findViewById(R.id.btnSelect);

        btnCapture.setOnClickListener(v->{
            openCameraIntent();


        });

        btnSelect.setOnClickListener(v->{
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
        });
    }

    // opens camera for user
    private void openCameraIntent(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        // tell camera where to store the resulting picture
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // start camera, and wait for it to finish
        startActivityForResult(intent, REQUEST_IMAGE);
    }



    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(chosen);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                if (quant) {
                    imgData.put((byte) ((val >> 16) & 0xFF));
                    imgData.put((byte) ((val >> 8) & 0xFF));
                    imgData.put((byte) (val & 0xFF));
                } else {
                    imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }

            }
        }
    }
    // loads the labels from the label txt file in assets into a string array
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(this.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        Intent intent = new Intent(this, WasteResult.class);



        if(resultCode == RESULT_OK ){
            if(requestCode == REQUEST_IMAGE){
                Uri uri = imageUri;
                System.out.println(uri);
                //System.out.println("jai hind doston");
                imageCapture.setImageURI(uri);
                //Bitmap bitmap = null;
                try {
                    Bitmap bitmap = (Bitmap) MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageCapture.setImageBitmap(bitmap);
                    bitmap = getResizedBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
                    convertBitmapToByteBuffer(bitmap);
                    tflite.run(imgData, labelProbArray);

                    if(labelProbArray[0][0]>labelProbArray[0][1]){
                        predictLabel = "ORGANIC";
                        predictProbablility = Double.toString (round(labelProbArray[0][0]*100.00,2));
                    }
                    else{
                        predictLabel = "RECYCLABLE";
                        predictProbablility = Double.toString (round(labelProbArray[0][1]*100.00,2));
                    }


                    intent.putExtra("predictLabel", predictLabel);
                    intent.putExtra("predictProbablility", predictProbablility);
                    startActivity(intent);

                } catch (IOException e) {
                    e.printStackTrace();
                }



            }
            else if(requestCode == REQUEST_IMAGE_GALLERY){

                Uri uri = data.getData();
                System.out.println(uri);
                System.out.println("jai hind doston");
                imageCapture.setImageURI(uri);
                //Bitmap bitmap = null;
                try {
                    Bitmap bitmap = (Bitmap) MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageCapture.setImageBitmap(bitmap);
                    bitmap = getResizedBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
                    convertBitmapToByteBuffer(bitmap);
                    tflite.run(imgData, labelProbArray);
                    if(labelProbArray[0][0]>labelProbArray[0][1]){
                        predictLabel = "ORGANIC";
                        predictProbablility = Double.toString (round(labelProbArray[0][0]*100.00,2));
                    }
                    else{
                        predictLabel = "RECYCLABLE";
                        predictProbablility = Double.toString (round(labelProbArray[0][1]*100.00,2));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


                intent.putExtra("predictLabel", predictLabel);
                intent.putExtra("predictProbablility", predictProbablility);
                startActivity(intent);

            }

        }




    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
