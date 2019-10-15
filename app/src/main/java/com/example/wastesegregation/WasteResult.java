package com.example.wastesegregation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class WasteResult extends AppCompatActivity {


    private Notification.Action.Builder data;



    public String label;
    public String probability;

    public String TAG;
    public String message;
    public String url1;
    public String url2;

    public String urlo = "file:///android_asset/organic.html";
    public String urlr = "file:///android_asset/recycle.html";


    WebView ourBrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_result);


        Intent intent = getIntent();
        label = intent.getStringExtra("predictLabel");
        TAG = label;
        probability = intent.getStringExtra("predictProbablility");
        //  predictlabel.setText(label);
        message = "PREDICTION ACCURACY : "+probability+"%";



        if(label.equals("RECYCLABLE")){
            url2 = urlo;
            url1 = urlr;
        }
        else{
            url2 = urlr;
            url1 = urlo;
        }

        ourBrow = (WebView)findViewById(R.id.webview);
        ourBrow.setWebViewClient(new MyBrowser());
        ourBrow.getSettings().setJavaScriptEnabled(true);
        ourBrow.loadUrl(url1);


        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        ViewDialog alert = new ViewDialog();
        alert.showDialog(this, TAG, message);


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
            intent.putExtra("url2",url2);
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


    private class MyBrowser extends WebViewClient{

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }



}
