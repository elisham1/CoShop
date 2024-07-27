package com.elisham.coshop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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

public class EmailSignupActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, firstNameEditText, familyNameEditText, confirmPasswordEditText;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private ImageView togglePasswordVisibility, toggleConfirmPasswordVisibility;
    private ImageView clearFirstNameIcon, clearFamilyNameIcon, clearEmailIcon;
    private TextView firstNameError, emailError, passwordError, confirmPasswordError;
    private LinearLayout passwordLayout, confirmPasswordLayout, emailLayout, firstNameLayout, familyNameLayout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_signup);

        mAuth = FirebaseAuth.getInstance();

        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        emailLayout = findViewById(R.id.emailLayout);
        firstNameLayout = findViewById(R.id.firstNameLayout);
        familyNameLayout = findViewById(R.id.familyNameLayout);
        emailEditText = findViewById(R.id.emailEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        familyNameEditText = findViewById(R.id.familyNameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility);
        clearFirstNameIcon = findViewById(R.id.clearFirstNameIcon);
        clearFamilyNameIcon = findViewById(R.id.clearFamilyNameIcon);
        clearEmailIcon = findViewById(R.id.clearEmailIcon);
        firstNameError = findViewById(R.id.firstNameError);
        emailError = findViewById(R.id.emailError);
        passwordError = findViewById(R.id.passwordError);
        confirmPasswordError = findViewById(R.id.confirmPasswordError);

        togglePassword();
        addTextWatchers();
        Button signUpButton = findViewById(R.id.signUpButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
            }
        });

        clearFirstNameIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstNameEditText.setText("");
                firstNameError.setVisibility(View.VISIBLE);
                firstNameError.setText("First Name is required");
                firstNameLayout.setBackgroundResource(R.drawable.red_border);
                clearFirstNameIcon.setVisibility(View.INVISIBLE);
            }
        });

        clearFamilyNameIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                familyNameEditText.setText("");
                clearFamilyNameIcon.setVisibility(View.INVISIBLE);
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

    private void addTextWatchers() {
        firstNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    firstNameError.setVisibility(View.GONE);
                    firstNameLayout.setBackgroundResource(R.drawable.border);
                    clearFirstNameIcon.setVisibility(View.VISIBLE);
                } else {
                    clearFirstNameIcon.setVisibility(View.INVISIBLE);
                    firstNameError.setVisibility(View.VISIBLE);
                    firstNameError.setText("First Name is required");
                    firstNameLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        familyNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearFamilyNameIcon.setVisibility(View.VISIBLE);
                } else {
                    clearFamilyNameIcon.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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

        confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    confirmPasswordError.setVisibility(View.GONE);
                    confirmPasswordLayout.setBackgroundResource(R.drawable.border);
                } else {
                    confirmPasswordError.setVisibility(View.VISIBLE);
                    confirmPasswordError.setText("Confirm Password");
                    confirmPasswordLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
                confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
            }
        });
    }

    private void signUpUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String familyName = familyNameEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (firstName.isEmpty()) {
            firstNameError.setVisibility(View.VISIBLE);
            firstNameError.setText("First Name is required");
            firstNameEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (email.isEmpty()) {
            emailError.setVisibility(View.VISIBLE);
            emailEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setVisibility(View.VISIBLE);
            emailError.setText("Invalid email format");
            emailEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (password.isEmpty()) {
            passwordError.setVisibility(View.VISIBLE);
            passwordError.setText("Password is required");
            passwordEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError.setVisibility(View.VISIBLE);
            confirmPasswordError.setText("Confirm Password");
            confirmPasswordEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (password.length() < 6) {
            passwordError.setVisibility(View.VISIBLE);
            passwordError.setText(R.string.password_length_error);
            passwordEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordError.setVisibility(View.VISIBLE);
            confirmPasswordError.setText(R.string.passwords_do_not_match);
            confirmPasswordEditText.setBackgroundResource(R.drawable.red_border);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            sendVerificationEmail();
                            showVerificationDialog(email, firstName, familyName);
                        } else {
                            handleSignUpFailure(task);
                        }
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("FirebaseAuth", "Verification email sent to " + user.getEmail());
                            } else {
                                Log.w("FirebaseAuth", "Failed to send verification email", task.getException());
                            }
                        }
                    });
        }
    }

    private void showVerificationDialog(String email, String firstName, String familyName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(EmailSignupActivity.this);
        builder.setMessage("A verification email has been sent to " + email + ". Please verify your email before logging in.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        navigateToLogin(email, firstName, familyName);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void navigateToLogin(String email, String firstName, String familyName) {
        Intent intent = new Intent(EmailSignupActivity.this, EmailLoginActivity.class);
        intent.putExtra("source", "EmailSignupActivity");
        intent.putExtra("isFirstEntry", true);
        intent.putExtra("email", email);
        intent.putExtra("firstName", firstName);
        intent.putExtra("familyName", familyName);
        startActivity(intent);
        finish();
    }

    private void handleSignUpFailure(Task<AuthResult> task) {
        Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
        showAlertDialog("Signup failed: " + task.getException().getMessage());
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
}
