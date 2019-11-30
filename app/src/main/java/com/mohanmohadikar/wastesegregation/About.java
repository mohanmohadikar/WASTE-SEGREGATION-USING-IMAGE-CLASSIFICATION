package com.mohanmohadikar.wastesegregation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    }
}


/*
flashOn.setOnClickListener(v->{
        turnOnFlashLight();
        flashOn.setVisibility(v.INVISIBLE);
        flashOff.setVisibility(v.VISIBLE);
        });
        flashOff.setOnClickListener(v->{
        turnOffFlashLight();
        flashOff.setVisibility(v.INVISIBLE);
        flashOn.setVisibility(v.VISIBLE);

        });
*/