package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ContactUsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private MenuUtils menuUtils;
    private String globalUserType, email, name;
    private Spinner spinnerFeedbackTitle;
    private EditText inputFeedback;
    private Button saveButton;
    private TextView feedbackSent;

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

        setContentView(R.layout.activity_contact_us);
        menuUtils = new MenuUtils(this, globalUserType);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        spinnerFeedbackTitle = findViewById(R.id.spinnerFeedbackTitle);
        inputFeedback = findViewById(R.id.inputFeedback);
        saveButton = findViewById(R.id.save);
        feedbackSent = findViewById(R.id.feedbackSent);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.feedback_titles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeedbackTitle.setAdapter(adapter);

        inputFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (globalUserType.equals("Supplier")) {
                    saveButton.setBackgroundColor(getColor(R.color.supplierPrimary));
                } else {
                    saveButton.setBackgroundColor(getColor(R.color.consumerPrimary));
                }
                saveButton.setText("cancel");
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                       finish();
                    }
                });
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    saveButton.setText("send feedback");
                    saveButton.setBackgroundColor(getResources().getColor(R.color.unsaved_red));
                    saveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sendFeedback();
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        retrieveUserInformation();
    }

    private void retrieveUserInformation() {
        if (currentUser != null) {
            email = currentUser.getEmail();
        }
        if (email != null) {
            db.collection("users").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        name = documentSnapshot.getString("first name");
                    } else {
                        Toast.makeText(ContactUsActivity.this, "User details not found.", Toast.LENGTH_SHORT).show();
                    }

            })
                .addOnFailureListener(e -> Toast.makeText(ContactUsActivity.this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show());
        }
    }

    private void sendFeedback() {

        String feedbackTitle = spinnerFeedbackTitle.getSelectedItem().toString();
        if (feedbackTitle.equals("Select Feedback Title")) {
            Toast.makeText(this, "Please select a feedback title.", Toast.LENGTH_SHORT).show();
            return;
        }
        String feedbackContent = inputFeedback.getText().toString();
        if (feedbackContent.isEmpty()) {
            Toast.makeText(this, "Please enter your feedback.", Toast.LENGTH_SHORT).show();
            return;
        }
        String feedbackTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("name", name);
        feedback.put("email", email);
        feedback.put("title", feedbackTitle);
        feedback.put("feedback", feedbackContent);
        feedback.put("time", feedbackTime);

        db.collection("feedbacks").add(feedback)
                .addOnSuccessListener(documentReference -> {
                    // Feedback successfully added to the database
                    hideKeyboard();
                    showFeedbackReceivedMessage();
                })
                .addOnFailureListener(e -> {
                    // Failed to add feedback
                    Toast.makeText(ContactUsActivity.this, "Failed to send feedback. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showFeedbackReceivedMessage() {
        spinnerFeedbackTitle.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        inputFeedback.setVisibility(View.GONE);
        feedbackSent.setVisibility(View.VISIBLE);

        saveButton.setText("finish");
        saveButton.setVisibility(View.VISIBLE);
        if (globalUserType.equals("Supplier")) {
            saveButton.setBackgroundColor(getColor(R.color.supplierPrimary));
        } else {
            saveButton.setBackgroundColor(getColor(R.color.consumerPrimary));
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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