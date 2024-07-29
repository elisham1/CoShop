package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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
import android.util.Log;

public class SignupActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    String webClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Resources res = getResources();
        int defaultWebClientId = res.getIdentifier("default_web_client_id", "string", getPackageName());
        webClientId = res.getString(defaultWebClientId);

        mAuth = FirebaseAuth.getInstance();

        // Initialize sign-in button click listener
        findViewById(R.id.button3).setOnClickListener(view -> signUp());
        findViewById(R.id.button1).setOnClickListener(view -> clickToMail());
    }

    // Initiates Google sign-in process
    private void signUp() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
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

    // Authenticate with Firebase using Google account
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            Log.d("Firebase Auth", "signInWithCredential:success - New User");
                            Log.d("SignupActivity", "Sign up successful.");
                            Intent intent = new Intent(SignupActivity.this, CategoriesActivity.class);
                            intent.putExtra("google_sign_up", true);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("Firebase Auth", "signInWithCredential:success - Existing User");
                            Log.d("SignupActivity", "You already exist try log in.");
                            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.w("Firebase Auth", "signInWithCredential:failure", task.getException());
                        Log.d("SignupActivity", "Authentication failed.");
                    }
                });
    }

    // Navigate to email sign-up activity
    public void clickToMail() {
        Intent intent = new Intent(SignupActivity.this, EmailSignupActivity.class);
        startActivityForResult(intent, 1);
    }
}
