package com.elisham.coshop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;


public class EmailSignupActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText,firstNameEditText, familyNameEditText, confirmPasswordEditText;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private ImageView togglePasswordVisibility;
    private ImageView toggleConfirmPasswordVisibility;
    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signup);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        firstNameEditText = findViewById(R.id.nameEditText);
        familyNameEditText = findViewById(R.id.familyNameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility);

        togglePassword();
        Button signUpButton = findViewById(R.id.signUpButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
            }
        });
    }

    private void togglePassword() {
        togglePasswordVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    togglePasswordVisibility.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    togglePasswordVisibility.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                isPasswordVisible = !isPasswordVisible;
                // Move cursor to end of text
                passwordEditText.setSelection(passwordEditText.getText().length());
            }
        });

        toggleConfirmPasswordVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfirmPasswordVisible) {
                    confirmPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    toggleConfirmPasswordVisibility.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    confirmPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    toggleConfirmPasswordVisibility.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                // Move cursor to end of text
                confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
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
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (email.equals("") || password.equals("") || firstName.equals("") || familyName.equals("")) {
            showAlertDialog("Please enter all details");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlertDialog("Signup failed: passwords are not equal");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("FirebaseAuth", "createUserWithEmail:success");
                            user = mAuth.getCurrentUser();
                            Toast.makeText(EmailSignupActivity.this, "Authentication Successful.", Toast.LENGTH_SHORT).show();
                            // Update UI or redirect to another activity
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(EmailSignupActivity.this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(EmailSignupActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                    }
                });
        // Set the result to OK to indicate success
        setResult(RESULT_OK);
        Intent intent = new Intent(EmailSignupActivity.this, CategoriesActivity.class);
        intent.putExtra("currUser", user);
        intent.putExtra("email", email);
        intent.putExtra("firstName", firstName);
        intent.putExtra("familyName", familyName);
        // Clear the activity stack and start HomePageActivity as a new task
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        // Finish UserDetailsActivity
        finish();
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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
