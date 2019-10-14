package com.example.wastesegregation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Feedback extends AppCompatActivity {


    WebView ourBrow;
    String url = "https://docs.google.com/forms/d/e/1FAIpQLSfQHJ6Q1U2dqB6YJNdd_nIkFxjN2CBfYcAuiakDvV4R1YFwGA/viewform?usp=sf_link";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);



        ourBrow = (WebView)findViewById(R.id.webview);
        ourBrow.setWebViewClient(new MyBrowser());
        ourBrow.getSettings().setJavaScriptEnabled(true);
        ourBrow.loadUrl(url);
    }


    private class MyBrowser extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
