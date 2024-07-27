package com.elisham.coshop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import android.app.ProgressDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText oldPasswordEditText, newPasswordEditText, confirmNewPasswordEditText;
    private boolean isOldPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmNewPasswordVisible = false;
    private ImageView toggleOldPassword, toggleNewPassword, toggleConfirmNewPassword;
    private TextView oldPasswordError, newPasswordError, confirmNewPasswordError,forgotPasswordTextView;
    private LinearLayout oldPasswordLayout, newPasswordLayout, confirmNewPasswordLayout;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private MenuUtils menuUtils;
    private String globalUserType;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the theme based on the user type
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }
        setContentView(R.layout.activity_change_password);
        menuUtils = new MenuUtils(this, globalUserType);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        oldPasswordLayout = findViewById(R.id.oldPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmNewPasswordLayout = findViewById(R.id.confirmNewPasswordLayout);
        oldPasswordEditText = findViewById(R.id.old_password);
        newPasswordEditText = findViewById(R.id.new_password);
        confirmNewPasswordEditText = findViewById(R.id.confirm_new_password);
        toggleOldPassword = findViewById(R.id.toggle_old_password);
        toggleNewPassword = findViewById(R.id.toggle_new_password);
        toggleConfirmNewPassword = findViewById(R.id.toggle_confirm_new_password);
        oldPasswordError = findViewById(R.id.oldPasswordError);
        newPasswordError = findViewById(R.id.newPasswordError);
        confirmNewPasswordError = findViewById(R.id.confirmNewPasswordError);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        Button doneButton = findViewById(R.id.done_button);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        togglePasswordVisibility();
        addTextWatchers();
        setupForgotPassword();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                changePassword();
            }
        });
    }

    private void addTextWatchers() {
        oldPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    oldPasswordError.setVisibility(View.GONE);
                    oldPasswordLayout.setBackgroundResource(R.drawable.border);
                } else {
                    oldPasswordError.setVisibility(View.VISIBLE);
                    oldPasswordError.setText(getString(R.string.field_required));
                    oldPasswordLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        newPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 6) {
                    newPasswordError.setVisibility(View.GONE);
                    newPasswordLayout.setBackgroundResource(R.drawable.border);
                } else {
                    newPasswordError.setVisibility(View.VISIBLE);
                    newPasswordError.setText(getString(R.string.field_required));
                    newPasswordLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        confirmNewPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    confirmNewPasswordError.setVisibility(View.GONE);
                    confirmNewPasswordLayout.setBackgroundResource(R.drawable.border);
                } else {
                    confirmNewPasswordError.setVisibility(View.VISIBLE);
                    confirmNewPasswordError.setText(getString(R.string.field_required));
                    confirmNewPasswordLayout.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void togglePasswordVisibility() {
        toggleOldPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOldPasswordVisible) {
                    oldPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    toggleOldPassword.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    oldPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    toggleOldPassword.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                isOldPasswordVisible = !isOldPasswordVisible;
                oldPasswordEditText.setSelection(oldPasswordEditText.getText().length());
            }
        });

        toggleNewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNewPasswordVisible) {
                    newPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    toggleNewPassword.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    newPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    toggleNewPassword.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                isNewPasswordVisible = !isNewPasswordVisible;
                newPasswordEditText.setSelection(newPasswordEditText.getText().length());
            }
        });

        toggleConfirmNewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfirmNewPasswordVisible) {
                    confirmNewPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    toggleConfirmNewPassword.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    confirmNewPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    toggleConfirmNewPassword.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible;
                confirmNewPasswordEditText.setSelection(confirmNewPasswordEditText.getText().length());
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
        LinearLayout emailForgotPasswordLayout = dialogView.findViewById(R.id.emailForgotPasswordLayout);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null) {
                emailForgotPasswordEditText.setText(userEmail);
                emailForgotPasswordEditText.setEnabled(false);
                emailForgotPasswordEditText.setTextColor(getResources().getColor(R.color.default_color));
                clearEmailIcon.setVisibility(View.GONE);
            }
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
                emailForgotPasswordLayout.setBackgroundResource(R.drawable.border);
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
                    emailForgotPasswordLayout.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter your email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailForgotPassword).matches()) {
                    emailForgotPasswordLayout.setBackgroundResource(R.drawable.red_border);
                    errorTextView.setText("Please enter a valid email address");
                    errorTextView.setVisibility(View.VISIBLE);
                } else {
                    emailForgotPasswordLayout.setBackgroundResource(R.drawable.border);
                    errorTextView.setVisibility(View.GONE);

                    mAuth.sendPasswordResetEmail(emailForgotPassword)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ChangePasswordActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ChangePasswordActivity.this, "Failed to send password reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    dialog.dismiss();
                                }
                            });
                }
            }
        });
    }


    private void changePassword() {
        String oldPassword = oldPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

        if (oldPassword.isEmpty()) {
            oldPasswordError.setVisibility(View.VISIBLE);
            oldPasswordError.setText(getString(R.string.field_required));
            oldPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        if (newPassword.isEmpty()) {
            newPasswordError.setVisibility(View.VISIBLE);
            newPasswordError.setText(getString(R.string.field_required));
            newPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        if (confirmNewPassword.isEmpty()) {
            confirmNewPasswordError.setVisibility(View.VISIBLE);
            confirmNewPasswordError.setText(getString(R.string.field_required));
            confirmNewPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        if (newPassword.length() < 6) {
            newPasswordError.setVisibility(View.VISIBLE);
            newPasswordError.setText(getString(R.string.password_length_error));
            newPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        if (oldPassword.equals(newPassword)) {
            newPasswordError.setVisibility(View.VISIBLE);
            newPasswordError.setText(getString(R.string.passwords_must_be_different));
            newPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            confirmNewPasswordError.setVisibility(View.VISIBLE);
            confirmNewPasswordError.setText(getString(R.string.passwords_do_not_match));
            confirmNewPasswordLayout.setBackgroundResource(R.drawable.red_border);
            progressDialog.dismiss();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

            // Re-authenticate the user
            user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        // Update the password
                        user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> updateTask) {
                                if (updateTask.isSuccessful()) {
                                    Log.d("FirebaseAuth", "Password updated successfully");
                                    progressDialog.dismiss();
                                    finish();
                                } else {
                                    progressDialog.dismiss();
                                    Log.w("FirebaseAuth", "Error updating password", updateTask.getException());
                                }
                            }
                        });
                    } else {
                        oldPasswordError.setVisibility(View.VISIBLE);
                        oldPasswordError.setText(getString(R.string.wrong_password));
                        oldPasswordLayout.setBackgroundResource(R.drawable.red_border);
                        progressDialog.dismiss();
                        Log.w("FirebaseAuth", "Authentication failed", task.getException());
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        if ("Supplier".equals(globalUserType)) {
            MenuItem item = menu.findItem(R.id.chat_notification);
            if (item != null) {
                item.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo();
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders();
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs();
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs();
                return true;
            case R.id.Log_Out:
                menuUtils.logOut();
                return true;
            case R.id.home:
                menuUtils.home();
                return true;
            case R.id.chat_icon:
                menuUtils.allChats();
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
