package com.elisham.coshop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class EmailLoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private String email;
    private Button loginButton;
    private boolean isPasswordVisible = false;
    ImageButton togglePasswordVisibility;
    private TextView forgotPasswordTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        togglePassword();
        setupForgotPassword();

        Intent intent = getIntent();
        if (intent != null) {
            String source = getIntent().getStringExtra("source");
            if (source != null) {
                if (source.equals("EmailSignupActivity")) {
                    email = getIntent().getStringExtra("email");
                    emailEditText.setText(email);
                }
            }
        }


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
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
    }

    private void setupForgotPassword() {
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText emailForgotPasswordEditText = dialogView.findViewById(R.id.emailForgotPasswordEditText);
        ImageView clearEmailIcon = dialogView.findViewById(R.id.clearEmailIcon);
        TextView errorTextView = dialogView.findViewById(R.id.errorTextView);

        // Pre-fill the email field if the email is not empty
        if (email != null && !email.isEmpty()) {
            emailForgotPasswordEditText.setText(email);
        }

        // Set TextWatcher to handle the visibility of the clear icon
        emailForgotPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearEmailIcon.setVisibility(View.VISIBLE);
                } else {
                    clearEmailIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set onClickListener for the clear email icon
        clearEmailIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailForgotPasswordEditText.setText(""); // Clear the text in EditText
            }
        });

        builder.setView(dialogView)
                .setPositiveButton("Send Reset Email", null) // Set null listener to override later
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to perform validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailForgotPassword = emailForgotPasswordEditText.getText().toString().trim();

                if (emailForgotPassword.isEmpty()) {
                    emailForgotPasswordEditText.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter your email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else if (!isValidEmail(emailForgotPassword)) {
                    emailForgotPasswordEditText.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter a valid email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else {
                    emailForgotPasswordEditText.setBackgroundResource(android.R.color.transparent); // Reset to default
                    errorTextView.setVisibility(View.GONE);

                    mAuth.sendPasswordResetEmail(emailForgotPassword)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(EmailLoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(EmailLoginActivity.this, "Failed to send password reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    dialog.dismiss();
                                }
                            });
                }
            }
        });
    }

    // Utility method to validate email
    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


    private void loginUser() {
        email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login success
                            // Set the result to OK to indicate success
                            setResult(RESULT_OK);
                            Intent intent = new Intent(EmailLoginActivity.this, HomePageActivity.class);
                            // Clear the activity stack and start HomePageActivity as a new task
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            // Finish UserDetailsActivity
                            finish();
                        } else {
                            // Login failed
                            Toast.makeText(EmailLoginActivity.this, "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }
}
