package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignupActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Resources res = getResources();
        int defaultWebClientId = res.getIdentifier("default_web_client_id", "string", getPackageName()); // Get the resource ID dynamically
        String webClientId = res.getString(defaultWebClientId);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Call signIn method when the Google Sign-In button is clicked
        findViewById(R.id.button3).setOnClickListener(view -> signUp());
        findViewById(R.id.button1).setOnClickListener(view -> clickToMail());

    }

    private void signUp() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("Google Sign In Error", "signInResult:failed code=" + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check if the user is new
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            // If user is new, navigate to CategoriesActivity
                            Log.d("Firebase Auth", "signInWithCredential:success - New User");
                            Toast.makeText(SignupActivity.this, "Sign up successful.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SignupActivity.this, CategoriesActivity.class);
                            startActivity(intent);
                            finish(); // Close this activity to prevent returning to it on back press
                        } else {
                            // If user is not new, proceed with normal sign-in flow
                            Log.d("Firebase Auth", "signInWithCredential:success - Existing User");
                            Toast.makeText(SignupActivity.this, "You already exist try log in.", Toast.LENGTH_SHORT).show();
                            // Navigate to SignupActivity
                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish(); // Close this activity to prevent returning to it on back press
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Firebase Auth", "signInWithCredential:failure", task.getException());
                        Toast.makeText(SignupActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handle the back button click
        if (id == android.R.id.home) {
            onBackPressed(); // Go back when the back arrow is clicked
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void emailLogin(View v) {
        Intent intent = new Intent(SignupActivity.this, EmailLoginActivity.class);
        startActivity(intent);
    }

    public void clickToHome(View v) {
        Intent intent = new Intent(SignupActivity.this, HomePageActivity.class);
        startActivity(intent);
    }
    public void clicktocategories(View v) {
        Intent intent = new Intent(SignupActivity.this, CategoriesActivity.class);
        startActivity(intent);
    }

    public void clickToMail() {
        Intent intent = new Intent(SignupActivity.this, EmailSignupActivity.class);
        startActivityForResult(intent, 1);    }
}
