package com.example.wastesegregation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class TakeImage extends AppCompatActivity {


    String[] prediction = new String[2];

    Intent intw;


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

    Bitmap b3,b4,b5;

    Matrix matrix = new Matrix();


    private static final String IMAGE_DIRECTORY = "/CustomImage";
    ImageView menudots;



    public String urlo = "file:///android_asset/organic.html";
    public String urlr = "file:///android_asset/recycle.html";



    // The request code used in ActivityCompat.requestPermissions()
    // and returned in the Activity's onRequestPermissionsResult()
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.INTERNET
    };



    private Camera mCamera;
    private ImagePreview mPreview;
    private Camera.PictureCallback mPicture;
    private ImageView capture, switchCamera, selGal;
    private Context myContext;
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;
    public static Bitmap bitmap;
    private static final int REQUEST_IMAGE_GALLERY = 2;
   // String storageNode;
    public static String userAccount;

    Bitmap b2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_image);





        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
//        toolbar.inflateMenu(R.menu.example_menu);
        setSupportActionBar(toolbar);


        intw = new Intent(TakeImage.this, WasteResult.class);
        //intw.putExtra("predictLabel", prediction[0]);



        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

//




        matrix.postRotate(90);

        try{
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
            labelList = loadLabelList();
        } catch (Exception e){
            e.printStackTrace();
        }
        imgData = ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];




        Intent i = getIntent();
        storageNode = i.getStringExtra("KEY");
        userAccount = storageNode;

        mStorageRef = FirebaseStorage.getInstance().getReference(storageNode);
        mDatabaseRef = FirebaseDatabase.getInstance().getReference(storageNode);


        //ViewDialog alert = new ViewDialog();
       // alert.showDialog(this, TAG, message);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;

        mCamera =  Camera.open();
        mCamera.setDisplayOrientation(90);
        cameraPreview = (LinearLayout) findViewById(R.id.cPreview);
        capture = (ImageView) findViewById(R.id.btnCam);
        switchCamera = (ImageView) findViewById(R.id.btnSwitch);
        menudots = (ImageView) findViewById(R.id.menudots);
        selGal = (ImageView) findViewById(R.id.selGal);
        mPreview = new ImagePreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        CameraSwitch();
        CameraSwitch();


        capture.setOnClickListener(v->{
            mCamera.takePicture(null, null, mPicture);
        });


        switchCamera.setOnClickListener(v->{
            CameraSwitch();
        });


        selGal.setOnClickListener(v->{
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
        });



        mCamera.startPreview();

    }








    private int findFrontFacingCamera() {

        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;

    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;

            }

        }
        return cameraId;
    }

    private void CameraSwitch() {
        //get the number of cameras
        int camerasNumber = Camera.getNumberOfCameras();
        if (camerasNumber > 1) {
            //release the old camera instance
            //switch camera, from the front and the back and vice versa

            releaseCamera();
            chooseCamera();
        } else {

        }
    }

    public void onResume() {

        super.onResume();
        if(mCamera == null) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
            Log.d("nu", "null");
        }else {
            Log.d("nu","no null");
        }

    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                Uri uri = getImageUri(TakeImage.this, bitmap);


                b5 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

//                imageView.setImageBitmap(b3);
                b5 = getResizedBitmap(b5, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
                saveImage(b5);
                b4 = b5;
                prediction = predict(uri);

                ViewDialog alert = new ViewDialog();
                alert.showDialog(TakeImage.this, prediction[0], prediction[1], uri);
                //releaseCamera();
                mCamera.startPreview();
               // mCamera = Camera.open();
              //  CameraSwitch();
              //  CameraSwitch();
               // Intent intent = new Intent(TakeImage.this,PictureActivity.class);
               // startActivity(intent);
            }
        };
        return picture;
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK ){
            if(requestCode == REQUEST_IMAGE_GALLERY){

                Uri uri = data.getData();

                try {
                    b3 = (Bitmap) MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                b5 = Bitmap.createBitmap(b3, 0, 0, b3.getWidth(), b3.getHeight(), matrix, true);

//                imageView.setImageBitmap(b3);
                b5 = getResizedBitmap(b5, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y);
                saveImage(b5);
                b4 = b5;
                prediction = predict(uri);

                ViewDialog alert = new ViewDialog();
                alert.showDialog(this, prediction[0], prediction[1], uri);



                //Intent intent = new Intent(this, PictureActivity.class);
               // intent.putExtra("uri", uri);
               // intent.putExtra("KEY", storageNode);
                //startActivity(intent);
            }
        }
    }



    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private String[] predict(Uri ImgUri){

        String[] result = new String[2];

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

        result[0] = predictLabel;
        result[1] = predictProbablility;

        uploadFile(ImgUri);

        return result;




       // Intent predictIntent = new Intent(this, WasteResult.class);
      //  predictIntent.putExtra("predictLabel", predictLabel);
      //  predictIntent.putExtra("predictProbablility", predictProbablility);
     //   predictIntent.putExtra("uri", uri);
       // startActivity(predictIntent);

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

                        Toast.makeText(TakeImage.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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


    public   void goToSuggestion(Intent inten){
      //  Intent intent = new Intent(TakeImage.this, WasteResult.class);

        startActivity(inten);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.example_menu, menu);
        return true;
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.get_more_info) {

            Intent intent = new Intent(this, GetMoreInfo.class);
            intent.putExtra("urlo",urlo);
            startActivity(intent);
            return true;
        }

        if (id == R.id.recyclable) {

            Intent intent = new Intent(this, WasteResult.class);
            intent.putExtra("urlr",urlr);
            startActivity(intent);
            return true;
        }

        if (id == R.id.feedback) {
            Intent intent = new Intent(this, Feedback.class);
            startActivity(intent);
            return true;
        }



        return super.onOptionsItemSelected(item);
    }




}
