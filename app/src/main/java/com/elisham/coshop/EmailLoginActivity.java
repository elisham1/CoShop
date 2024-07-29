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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

// Handles email login functionality
public class EmailLoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private boolean isPasswordVisible = false;
    private ImageView togglePasswordVisibility, clearEmailIcon;
    private TextView emailError, passwordError, forgotPasswordTextView;
    private LinearLayout emailLayout, passwordLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean firstEntry = false;

    // Initializes the activity and its components
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        clearEmailIcon = findViewById(R.id.clearEmailIcon);
        emailError = findViewById(R.id.emailError);
        passwordError = findViewById(R.id.passwordError);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        togglePassword();
        addTextWatchers();
        setupForgotPassword();

        Intent intent = getIntent();
        if (intent != null) {
            String source = intent.getStringExtra("source");
            if (source != null && source.equals("EmailSignupActivity")) {
                emailEditText.setText(intent.getStringExtra("email"));
                firstEntry = intent.getBooleanExtra("isFirstEntry", false);
            }
        }

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        clearEmailIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailEditText.setText("");
                emailError.setVisibility(View.VISIBLE);
                emailError.setText("Email is required");
                emailLayout.setBackgroundResource(R.drawable.red_border);
                clearEmailIcon.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Adds text watchers to email and password fields
    private void addTextWatchers() {
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                        emailLayout.setBackgroundResource(R.drawable.red_border);
                        emailError.setVisibility(View.VISIBLE);
                        emailError.setText("Invalid email format");
                        clearEmailIcon.setVisibility(View.VISIBLE);
                    } else {
                        emailError.setVisibility(View.GONE);
                        emailLayout.setBackgroundResource(R.drawable.border);
                        clearEmailIcon.setVisibility(View.VISIBLE);
                    }
                } else {
                    clearEmailIcon.setVisibility(View.INVISIBLE);
                    emailLayout.setBackgroundResource(R.drawable.red_border);
                    emailError.setVisibility(View.VISIBLE);
                    emailError.setText("Email is required");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 6) {
                    passwordError.setVisibility(View.GONE);
                    passwordLayout.setBackgroundResource(R.drawable.border);
                } else {
                    passwordError.setVisibility(View.VISIBLE);
                    passwordError.setText("Password must be at least 6 characters");
                    passwordLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Toggles password visibility in the password field
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
                passwordEditText.setSelection(passwordEditText.getText().length());
            }
        });
    }

    // Sets up the forgot password functionality
    private void setupForgotPassword() {
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    // Shows a dialog for forgot password functionality
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText emailForgotPasswordEditText = dialogView.findViewById(R.id.emailForgotPasswordEditText);
        ImageView clearEmailIcon = dialogView.findViewById(R.id.clearEmailIcon);
        TextView errorTextView = dialogView.findViewById(R.id.errorTextView);

        if (emailEditText.getText().toString().length() > 0) {
            emailForgotPasswordEditText.setText(emailEditText.getText().toString());
        }

        emailForgotPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearEmailIcon.setVisibility(View.VISIBLE);
                } else {
                    clearEmailIcon.setVisibility(View.INVISIBLE);
                }
                emailForgotPasswordEditText.setBackgroundResource(android.R.color.transparent);
                errorTextView.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearEmailIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailForgotPasswordEditText.setText("");
            }
        });

        builder.setView(dialogView)
                .setPositiveButton("Send Reset Email", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailForgotPassword = emailForgotPasswordEditText.getText().toString().trim();

                if (emailForgotPassword.isEmpty()) {
                    emailForgotPasswordEditText.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter your email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailForgotPassword).matches()) {
                    emailForgotPasswordEditText.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter a valid email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else {
                    emailForgotPasswordEditText.setBackgroundResource(android.R.color.transparent);
                    errorTextView.setVisibility(View.GONE);

                    mAuth.sendPasswordResetEmail(emailForgotPassword)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("EmailLoginActivity", "Password reset email sent");
                                    } else {
                                        Log.d("EmailLoginActivity", "Failed to send password reset email: " + task.getException().getMessage());
                                    }
                                    dialog.dismiss();
                                }
                            });
                }
            }
        });
    }

    // Logs in the user with email and password
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailError.setVisibility(View.VISIBLE);
            emailError.setText("Email is required");
            emailLayout.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (password.isEmpty()) {
            passwordError.setVisibility(View.VISIBLE);
            passwordError.setText("Password is required");
            passwordLayout.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (password.length() < 6) {
            passwordError.setVisibility(View.VISIBLE);
            passwordError.setText("Password must be at least 6 characters");
            passwordLayout.setBackgroundResource(R.drawable.red_border);
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (!user.isEmailVerified()) {
                                    showAlertDialog("Please verify your email before logging in");
                                } else {
                                    if (firstEntry) {
                                        firstEntry();
                                    } else {
                                        checkIfBlocked();
                                    }
                                }
                            }
                        } else {
                            showAlertDialog("Login failed: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // Redirects the user to CategoriesActivity on first entry
    private void firstEntry() {
        Intent intent = new Intent(EmailLoginActivity.this, CategoriesActivity.class);
        intent.putExtra("email", emailEditText.getText().toString().trim());
        startActivity(intent);
        finish();
    }

    // Checks if the user is blocked and redirects accordingly
    private void checkIfBlocked() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null) {
                DocumentReference userRef = db.collection("users").document(userEmail);
                userRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("type of user");
                        Boolean isBlocked = documentSnapshot.getBoolean("blocked");
                        Timestamp blockedTimestamp = documentSnapshot.getTimestamp("blockedTimestamp");

                        if (isBlocked != null && isBlocked) {
                            long blockDuration = 48 * 60 * 60 * 1000;
                            long currentTime = System.currentTimeMillis();
                            long blockedTime = blockedTimestamp != null ? blockedTimestamp.toDate().getTime() : 0;

                            if (currentTime - blockedTime < blockDuration) {
                                long remainingTime = blockDuration - (currentTime - blockedTime);
                                long hoursRemaining = remainingTime / (60 * 60 * 1000);
                                showAlertDialog("You are blocked from the app. It will be reviewed within " + hoursRemaining + " hours.");
                                Intent intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                showAlertDialog("You are blocked from the app. Please contact support at coshop.supp@gmail.com.");
                                Intent intent = new Intent(EmailLoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Intent intent = new Intent(EmailLoginActivity.this, HomePageActivity.class);
                            intent.putExtra("userType", userType);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        firstEntry();
                    }
                }).addOnFailureListener(e -> {
                    Log.d("EmailLoginActivity", "Failed to check block status");
                });
            }
        }
    }

    // Shows an alert dialog with the given message
    private void showAlertDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mAuth.signOut();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
