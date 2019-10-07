package com.example.wastesegregation;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class WasteResult extends AppCompatActivity {


    private Notification.Action.Builder data;


    public TextView predictlabel;
    public TextView predictProbability;
    public String label;
    public String probability;

    public String TAG;
    public String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_result);


        predictlabel = (TextView)findViewById(R.id.predictLabel);
        predictProbability = (TextView)findViewById(R.id.predictProbability);



        Intent intent = getIntent();
        label = intent.getStringExtra("predictLabel");
        TAG = label;
        probability = intent.getStringExtra("predictProbablility");
      //  predictlabel.setText(label);
        message = "PREDICTION ACCURACY : "+probability;

       // predictProbability.setText("PREDICTION ACCURACY : "+probability);





        ViewDialog alert = new ViewDialog();
        alert.showDialog(this, TAG, message);


    }
}
