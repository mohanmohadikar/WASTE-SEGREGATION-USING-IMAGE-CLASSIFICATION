package com.mohanmohadikar.wastesegregation;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;





public class ViewDialog {




    public void showDialog(Activity activity, String TAG, String message, Uri uri){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);




        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(),uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView label = (TextView) dialog.findViewById(R.id.label);
        label.setText(TAG);

        TextView accuracy = (TextView) dialog.findViewById(R.id.accuracy);
        accuracy.setText(message);


        ImageView imagepredict = (ImageView) dialog.findViewById(R.id.imagepredict);
        imagepredict.setImageBitmap(bitmap);

        TextView dialogButton = (TextView) dialog.findViewById(R.id.gotIt);
        dialogButton.setOnClickListener(v-> {
            dialog.dismiss();
        });


        dialog.show();

    }
}
