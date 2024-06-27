package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class UserDetailsActivity extends AppCompatActivity {

    private EditText addressEditText;
    private String email, firstName, familyName;
    private ArrayList<String> selectedCategories;

    private RadioGroup choiceRadioGroup;

    private boolean firstEntry = false;


    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if(intent != null) {
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");
            String fullName = firstName + " " + familyName;
            selectedCategories = intent.getStringArrayListExtra("selectedCategories");


            if (email != null) {
                firstEntry = true;
                EditText emailTextView = findViewById(R.id.emailText);
                emailTextView.setText(email);
                TextView fullNameTextView = findViewById(R.id.fullName);
                fullNameTextView.setText(fullName);
            }
            else {
                showAlertDialog("not first entry");
            }

            addressEditText = findViewById(R.id.addressText);
            choiceRadioGroup = findViewById(R.id.choiceLinearLayout);
        }

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    public void editUserDetails(View view) {

        if (addressEditText == null) {
            showAlertDialog("Please enter your address");
            return;
        }

        String address = addressEditText.getText().toString().trim();


        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("email", email);
        userDetails.put("first name", firstName);
        userDetails.put("family name", familyName);
        userDetails.put("favorite categories", selectedCategories);
        userDetails.put("address", address);
        choiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = findViewById(checkedId);
                if (checkedRadioButton != null) {
                    Toast.makeText(UserDetailsActivity.this, "Selected: " + checkedRadioButton.getText(), Toast.LENGTH_SHORT).show();
                }
            }
        });



        int selectedId = choiceRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            // No radio button is selected, show a toast message and return
            showAlertDialog("Please select a user type");
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);

        if (selectedRadioButton != null) {
            String selectedChoice = selectedRadioButton.getText().toString();
            userDetails.put("type of user", selectedChoice);
        } else {
            Toast.makeText(UserDetailsActivity.this, "No choice selected", Toast.LENGTH_SHORT).show();
        }


        // Add a new document with a generated ID
        db.collection("users").document(email)
                .set(userDetails)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(UserDetailsActivity.this, "user details updated successfully", Toast.LENGTH_SHORT).show();
                    home();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding user details: " + e.getMessage());
                    showAlertDialog("Error adding user details: " + e.getMessage());
//                    Toast.makeText(UserDetailsActivity.this, "Failed to update user detail", Toast.LENGTH_SHORT).show();
                });
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
        Intent toy = new Intent(UserDetailsActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(UserDetailsActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(UserDetailsActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(UserDetailsActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(UserDetailsActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(UserDetailsActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(UserDetailsActivity.this, MainActivity.class);
        startActivity(toy);
    }

    public void deleteAccount(View v) {
        Intent toy = new Intent(UserDetailsActivity.this, DeleteAccountActivity.class);
        startActivity(toy);
    }

    public void changePassword(View v) {
        Intent toy = new Intent(UserDetailsActivity.this, ChangePasswordActivity.class);
        startActivity(toy);
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