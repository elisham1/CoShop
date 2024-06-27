package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText oldPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmNewPasswordEditText;
    private ImageButton toggleOldPassword;
    private ImageButton toggleNewPassword;
    private ImageButton toggleConfirmNewPassword;
    private Button doneButton;
    private boolean isOldPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmNewPasswordVisible = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        oldPasswordEditText = findViewById(R.id.old_password);
        newPasswordEditText = findViewById(R.id.new_password);
        confirmNewPasswordEditText = findViewById(R.id.confirm_new_password);
        toggleOldPassword = findViewById(R.id.toggle_old_password);
        toggleNewPassword = findViewById(R.id.toggle_new_password);
        toggleConfirmNewPassword = findViewById(R.id.toggle_confirm_new_password);

        doneButton = findViewById(R.id.done_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        togglePassword();
        doneButton.setOnClickListener(view -> changePassword());
    }

    private void togglePassword() {
        toggleOldPassword.setOnClickListener(v -> {
            if (isOldPasswordVisible) {
                oldPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleOldPassword.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                oldPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleOldPassword.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            isOldPasswordVisible = !isOldPasswordVisible;
            oldPasswordEditText.setSelection(oldPasswordEditText.getText().length());
        });

        toggleNewPassword.setOnClickListener(v -> {
            if (isNewPasswordVisible) {
                newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleNewPassword.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                newPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleNewPassword.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            isNewPasswordVisible = !isNewPasswordVisible;
            newPasswordEditText.setSelection(newPasswordEditText.getText().length());
        });

        toggleConfirmNewPassword.setOnClickListener(v -> {
            if (isConfirmNewPasswordVisible) {
                confirmNewPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleConfirmNewPassword.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                confirmNewPasswordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleConfirmNewPassword.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible;
            confirmNewPasswordEditText.setSelection(confirmNewPasswordEditText.getText().length());
        });
    }

    private void changePassword() {
        String oldPassword = oldPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

        if (newPassword.isEmpty() || confirmNewPassword.isEmpty() || oldPassword.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

            // Re-authenticate the user
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Update the password
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error updating password.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Authentication failed. Incorrect old password.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); // Go back when the back arrow is clicked
                return true;
            case R.id.Personal_info:
                personalInfo();
                return true;
            case R.id.My_Orders:
                myOrders();
                return true;
            case R.id.About_Us:
                aboutUs();
                return true;
            case R.id.Contact_Us:
                contactUs();
                return true;
            case R.id.Log_Out:
                logOut();
                return true;
            case R.id.list_icon:
                basket();
                return true;
            case R.id.home:
                home();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void home() {
        Intent toy = new Intent(ChangePasswordActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(ChangePasswordActivity.this, UpdateUserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(ChangePasswordActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(ChangePasswordActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(ChangePasswordActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(ChangePasswordActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(ChangePasswordActivity.this, MainActivity.class);
        startActivity(toy);
    }
}
