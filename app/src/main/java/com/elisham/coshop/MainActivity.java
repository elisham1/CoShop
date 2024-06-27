package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Check if the user is logged in
        if (currentUser != null) {
            Toast.makeText(MainActivity.this, "logged in", Toast.LENGTH_SHORT).show();

            //check if user completed registration
            String userEmail = currentUser.getEmail();
            db.collection("users").document(userEmail).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Toast.makeText(MainActivity.this, "user exists", Toast.LENGTH_SHORT).show();

                                // User document exists, redirect to HomePageActivity
                                Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(MainActivity.this, "user dont exists", Toast.LENGTH_SHORT).show();

                                // User document does not exist, redirect to CategoriesActivity
                                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                                intent.putExtra("email", userEmail);
                                intent.putExtra("firstName", currentUser.getDisplayName());
                                intent.putExtra("familyName", currentUser.getDisplayName()); // You may need to adjust how you get familyName
                                startActivity(intent);
                            }
                        } else {
                            // Handle the error
                            Toast.makeText(MainActivity.this, "firebase failed", Toast.LENGTH_SHORT).show();

                            Log.d("firebase", "get failed with ", task.getException());
                        }
                        finish(); // Close the current activity in both cases
                    });
        } else {
            // User is not signed in, stay on MainActivity
            setContentView(R.layout.activity_main);
            // Initialize your views and other logic for MainActivity
        }
    }


    public void loginclick(View v) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(intent, 1);
    }

    public void signupclick(View v) {
        Intent intent = new Intent(MainActivity.this, SignupActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

}