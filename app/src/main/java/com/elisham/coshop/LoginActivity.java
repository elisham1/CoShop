package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    String webClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Resources res = getResources();
        int defaultWebClientId = res.getIdentifier("default_web_client_id", "string", getPackageName()); // Get the resource ID dynamically
        webClientId = res.getString(defaultWebClientId);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Call signIn method when the Google Sign-In button is clicked
        findViewById(R.id.button3).setOnClickListener(view -> signIn());
        findViewById(R.id.button1).setOnClickListener(view -> emailLogin());
    }

    // Initiates Google Sign-In process
    private void signIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish(); // Finish EmailSignupActivity if result is OK
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

    // Authenticates with Firebase using Google Sign-In account
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            Log.d("Firebase Auth", "New user needs to sign up.");
                            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                            startActivity(intent);
                            finish(); // Close this activity to prevent returning to it on back press
                        } else {
                            Log.d("Firebase Auth", "signInWithCredential:success - Existing User");
                            checkIfBlocked();
                        }
                    } else {
                        Log.w("Firebase Auth", "signInWithCredential:failure", task.getException());
                        Log.d("LoginActivity", "Authentication failed.");
                    }
                });
    }

    // Checks if the user is blocked
    private void checkIfBlocked() {
        String userEmail = mAuth.getCurrentUser().getEmail();
        DocumentReference userRef = db.collection("users").document(userEmail);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userType = documentSnapshot.getString("type of user");
                Boolean isBlocked = documentSnapshot.getBoolean("blocked");
                Timestamp blockedTimestamp = documentSnapshot.getTimestamp("blockedTimestamp");

                if (isBlocked != null && isBlocked) {
                    long blockDuration = 48 * 60 * 60 * 1000; // 48 hours in milliseconds
                    long currentTime = System.currentTimeMillis();
                    long blockedTime = blockedTimestamp != null ? blockedTimestamp.toDate().getTime() : 0;

                    if (currentTime - blockedTime < blockDuration) {
                        long remainingTime = blockDuration - (currentTime - blockedTime);
                        long hoursRemaining = remainingTime / (60 * 60 * 1000);
                        showAlertDialog("You are blocked from the app. It will be reviewed within " + hoursRemaining + " hours.");
                    } else {
                        showAlertDialog("You are blocked from the app. Please contact support at coshop.supp@gmail.com.");
                    }
                    // Signed in with Google
                    GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("Logout", "Google sign-out successful");
                        } else {
                            Log.e("Logout", "Google sign-out failed");
                        }
                    });
                    mAuth.signOut();
                } else {
                    Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
                    intent.putExtra("userType", userType);
                    startActivity(intent);
                    finish(); // Close this activity to prevent returning to it on back press
                }
            }
        }).addOnFailureListener(e -> {
            Log.d("LoginActivity", "Failed to check block status");
        });
    }

    // Shows an alert dialog with a message
    private void showAlertDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Do nothing and stay on the login screen
                    FirebaseAuth.getInstance().signOut();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Initiates email login process
    public void emailLogin() {
        Intent intent = new Intent(LoginActivity.this, EmailLoginActivity.class);
        startActivityForResult(intent, 1);
    }
}
