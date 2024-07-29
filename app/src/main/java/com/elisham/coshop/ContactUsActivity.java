package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// Activity for the Contact Us page
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

    // Initializes the activity and sets up the UI elements
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            // Sets the cancel button when text changes
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (globalUserType.equals("Supplier")) {
                    saveButton.setBackgroundResource(R.drawable.bg_selected_supplier);
                } else {
                    saveButton.setBackgroundResource(R.drawable.bg_selected_consumer);
                }
                saveButton.setText("cancel");
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }

            // Sets the send feedback button when text changes
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    saveButton.setText("SEND FEEDBACK");
                    saveButton.setBackgroundResource(R.drawable.bg_send_feedback);
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

    // Retrieves user information from Firestore
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
                            Log.d("ContactUsActivity", "User details not found.");
                        }

                    })
                    .addOnFailureListener(e -> Log.d("ContactUsActivity", "Failed to fetch user details.", e));
        }
    }

    // Sends feedback to Firestore
    private void sendFeedback() {
        String feedbackTitle = spinnerFeedbackTitle.getSelectedItem().toString();
        if (feedbackTitle.equals("Select Feedback Title")) {
            Log.d("ContactUsActivity", "Please select a feedback title.");
            return;
        }
        String feedbackContent = inputFeedback.getText().toString();
        if (feedbackContent.isEmpty()) {
            Log.d("ContactUsActivity", "Please enter your feedback.");
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
                    Log.d("ContactUsActivity", "Failed to send feedback. Please try again.", e);
                });
    }

    // Displays a message indicating feedback was received
    private void showFeedbackReceivedMessage() {
        spinnerFeedbackTitle.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        inputFeedback.setVisibility(View.GONE);
        feedbackSent.setVisibility(View.VISIBLE);

        saveButton.setText("GO TO HOME PAGE");
        saveButton.setVisibility(View.VISIBLE);
        if (globalUserType.equals("Supplier")) {
            saveButton.setBackgroundResource(R.drawable.bg_selected_supplier);
        } else {
            saveButton.setBackgroundResource(R.drawable.bg_selected_consumer);
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Hides the keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Inflates the options menu
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

    // Handles item selections in the options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo(); // Navigate to Personal Info
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders(); // Navigate to My Orders
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs(); // Navigate to About Us
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs(); // Navigate to Contact Us
                return true;
            case R.id.Log_Out:
                menuUtils.logOut(); // Log out user
                return true;
            case R.id.home:
                menuUtils.home(); // Navigate to Home
                return true;
            case R.id.chat_icon:
                menuUtils.allChats(); // Navigate to All Chats
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification(); // Navigate to Chat Notification
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
