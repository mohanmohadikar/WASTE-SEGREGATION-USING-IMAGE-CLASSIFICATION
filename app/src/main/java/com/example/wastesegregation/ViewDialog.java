package com.example.wastesegregation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ViewDialog {

    public void showDialog(Activity activity, String TAG, String message){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom);

        TextView label = (TextView) dialog.findViewById(R.id.label);
        label.setText(TAG);

        TextView accuracy = (TextView) dialog.findViewById(R.id.accuracy);
        accuracy.setText(message);

        TextView dialogButton = (TextView) dialog.findViewById(R.id.gotIt);
        dialogButton.setOnClickListener(v-> {
            dialog.dismiss();
        });

        dialog.show();

    }
}
