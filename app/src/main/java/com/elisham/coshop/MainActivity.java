package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private String webClientId, globalUserType, userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        int defaultWebClientId = res.getIdentifier("default_web_client_id", "string", getPackageName());
        webClientId = res.getString(defaultWebClientId);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Log.d("MainActivity", "onCreate: Firebase initialized");

        if (currentUser != null) {
            Log.d("MainActivity", "onCreate: User is logged in");
            String userEmail = currentUser.getEmail();
            db.collection("users").document(userEmail).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                globalUserType = document.getString("type of user");
                                Log.d("MainActivity", "onCreate: User type is " + globalUserType);
                                if ("Consumer".equals(globalUserType)) {
                                    Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
                                    intent.putExtra("userType", "Consumer");
                                    startActivity(intent);
                                    finish();
                                } else if ("Supplier".equals(globalUserType)) {
                                    Intent intent = new Intent(MainActivity.this, MyOrdersActivity.class);
                                    intent.putExtra("userType", "Supplier");
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                Log.d("MainActivity", "onCreate: User document does not exist");
                                googleLogin();
                            }
                        } else {
                            Log.d("MainActivity", "onCreate: Firebase failed", task.getException());
                            Toast.makeText(MainActivity.this, "firebase failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.d("MainActivity", "onCreate: User is not logged in");
            setContentView(R.layout.activity_main);
            findViewById(R.id.googleButton).setOnClickListener(view -> signUp());
        }


    }

    public void loginclick(View v) {
        Log.d("MainActivity", "loginclick: Login button clicked");
        Intent intent = new Intent(MainActivity.this, EmailLoginActivity.class);
        startActivityForResult(intent, 1);
    }

    public void signupclick(View v) {
        Log.d("MainActivity", "signupclick: Signup button clicked");
        Intent intent = new Intent(MainActivity.this, EmailSignupActivity.class);
        startActivityForResult(intent, 1);
    }

    public void googleLogin() {
        currentUser = mAuth.getCurrentUser();
        userEmail = currentUser.getEmail();
        Log.d("MainActivity", "googleLogin: Logging in with Google for " + userEmail);
        db.collection("users").document(userEmail).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            globalUserType = document.getString("type of user");
                            Log.d("MainActivity", "googleLogin: User type is " + globalUserType);
                            Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
                            intent.putExtra("userType", globalUserType);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            Log.d("MainActivity", "googleLogin: User document does not exist");
                            Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                            intent.putExtra("google_sign_up", true);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private void signUp() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Log.d("MainActivity", "signUp: Starting Google sign-in intent");
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("MainActivity", "firebaseAuthWithGoogle: Authenticating with Google for " + acct.getEmail());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        Log.d("MainActivity", "firebaseAuthWithGoogle: Credential created");
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    Log.d("MainActivity", "firebaseAuthWithGoogle: Sign-in complete");
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        Log.d("MainActivity", "firebaseAuthWithGoogle: Sign-in successful, new user: " + isNewUser);
                        if (isNewUser) {
                            Toast.makeText(MainActivity.this, "Sign up successful.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                            intent.putExtra("google_sign_up", true);
                            startActivity(intent);
                        } else {
                            googleLogin();
                        }
                    } else {
                        Log.w("MainActivity", "firebaseAuthWithGoogle: Authentication failed", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.w("MainActivity", "firebaseAuthWithGoogle: Authentication failed", e);
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("MainActivity", "onActivityResult: Google sign-in successful for " + account.getEmail());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("MainActivity", "onActivityResult: Google sign-in failed with code " + e.getStatusCode());
            }
        }

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Log.d("MainActivity", "onActivityResult: Result OK");
            setResult(RESULT_OK);
            finish();
        }
    }
}
