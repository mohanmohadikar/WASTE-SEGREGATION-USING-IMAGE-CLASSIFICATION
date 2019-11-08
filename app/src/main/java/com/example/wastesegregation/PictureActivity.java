package com.example.wastesegregation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PictureActivity extends AppCompatActivity {


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



    // will hold uri of image obtained from camera
    private Uri imageUri;

    // string to send to next activity that describes the chosen classifier
    private String chosen="colab_model.tflite";



    public String predictLabel = "";
    public String predictProbablility = "";


    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    String storageNode;

    Bitmap b1,b3,b4;
    Button btnPredict;

    Uri u1,u2,u3;

    String cll = "";





    private ImageView imageView;
    private static final String IMAGE_DIRECTORY = "/CustomImage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);


        storageNode = TakeImage.userAccount;


        mStorageRef = FirebaseStorage.getInstance().getReference(storageNode);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference(storageNode);


        Intent intent = getIntent();

        u3 = intent.getParcelableExtra("uri");


        if(u3!=null){
            try {
                b3 = (Bitmap) MediaStore.Images.Media.getBitmap(this.getContentResolver(),u3);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




        imageView = findViewById(R.id.img);
        btnPredict = (Button)findViewById(R.id.btnPredict);



        if(b3!=null){
            imageUri = getImageUri(this, b3);
            imageView.setImageBitmap(b3);
            b3 = getResizedBitmap(b3, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
            saveImage(b3);
            b4 = b3;
        }
        else{

            b1 = getResizedBitmap(TakeImage.bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
            imageUri = getImageUri(this, b1);
            imageView.setImageBitmap(TakeImage.bitmap);

            saveImage(TakeImage.bitmap);
            b4 = b1;
        }





        btnPredict.setOnClickListener(v->{

            Uri uri = imageUri;
            convertBitmapToByteBuffer(b4);
            tflite.run(imgData, labelProbArray);

            if(labelProbArray[0][0]>labelProbArray[0][1]){
                predictLabel = "ORGANIC";
                predictProbablility = Double.toString (round(labelProbArray[0][0]*100.00,2));
            }
            else{
                predictLabel = "RECYCLABLE";
                predictProbablility = Double.toString (round(labelProbArray[0][1]*100.00,2));
            }


            uploadFile(uri);

            Intent predictIntent = new Intent(this, WasteResult.class);
            predictIntent.putExtra("predictLabel", predictLabel);
            predictIntent.putExtra("predictProbablility", predictProbablility);
            startActivity(predictIntent);

            //Toast.makeText(this, predictLabel, Toast.LENGTH_LONG).show();
        });




        try{
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception e){
            e.printStackTrace();
        }
        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];

    }

    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();   //give read write permission
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
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
                if (true) {
                    imgData.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
    }


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


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    private void uploadFile(Uri uri){


        Uri file = uri;
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }




        StorageReference ref = mStorageRef.child("/" + predictLabel.charAt(0)+ Calendar.getInstance().getTimeInMillis() +".jpeg");
        System.out.println(predictLabel);



        ref.putFile(getImageUri(this,getResizedBitmap(bitmap,480,480)))
                .addOnSuccessListener(taskSnapshot ->
                        Toast.makeText(this, "Your file is uploaded for further analysis", Toast.LENGTH_LONG).show())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                        Toast.makeText(PictureActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }



}

