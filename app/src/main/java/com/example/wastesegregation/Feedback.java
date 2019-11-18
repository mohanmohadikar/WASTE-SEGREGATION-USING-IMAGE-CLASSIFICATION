package com.example.wastesegregation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

//

public class Feedback extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private String TAG = "nothing...";
    private int RC_SIGN_IN = 1;

    GoogleSignInAccount acct;
    FirebaseUser user;

    WebView ourBrow;
    private static final String url = "https://docs.google.com/forms/d/e/1FAIpQLSfQHJ6Q1U2dqB6YJNdd_nIkFxjN2CBfYcAuiakDvV4R1YFwGA/viewform?usp=sf_link";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();



        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

      //  if(mAuth.getCurrentUser()!=null){
            //FirebaseUser user = mAuth.getCurrentUser();
          //  updateUI(mAuth.getCurrentUser());
       // }


        signIn();





        ourBrow = (WebView)findViewById(R.id.webview);
        ourBrow.getSettings().setUserAgentString("WasteSegregation");
        WebChromeClient client = new WebChromeClient();
        ourBrow.setWebChromeClient(client);
       // ourBrow.setWebViewClient(new MyBrowser());
        ourBrow.getSettings().setJavaScriptEnabled(true);
        ourBrow.getSettings().setUserAgentString("Waste Segregation");
        ourBrow.loadUrl(url);

        signIn();
    }


    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }



    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            //updateUI(user);
                            Toast.makeText(Feedback.this, "logged in chutiya", Toast.LENGTH_LONG).show();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(Feedback.this, "you are not able to log in to Google", Toast.LENGTH_LONG).show();
                            //updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private class MyBrowser extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    private class MyChromeBrowser extends WebChromeClient {



    }
}
