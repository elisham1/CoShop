package com.elisham.coshop;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class EmailSignupActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText,firstNameEditText, familyNameEditText;

    private FirebaseAuth mAuth;
    //private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signup);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        //mDatabase = FirebaseDatabase.getInstance().getReference();

        emailEditText = findViewById(R.id.emailEditText);
        firstNameEditText = findViewById(R.id.nameEditText);
        familyNameEditText = findViewById(R.id.familyNameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button signUpButton = findViewById(R.id.signUpButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
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

    private void signUpUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String familyName = familyNameEditText.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            // You can add further actions here like navigating to another activity
                            Intent intent = new Intent(EmailSignupActivity.this, CategoriesActivity.class);
                            intent.putExtra("email", email);
                            intent.putExtra("firstName", firstName);
                            intent.putExtra("familyName", familyName);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(EmailSignupActivity.this, "Signup failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EmailSignupActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });
    }

}
