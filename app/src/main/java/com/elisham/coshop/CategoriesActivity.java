package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CategoriesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String email, firstName, familyName, fullName;
    private List<String> selectedCategories;
    private boolean isGoogleSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        selectedCategories = new ArrayList<>();
        isGoogleSignUp = getIntent().getBooleanExtra("google_sign_up", true);

        //get the user mail and name
        Intent intent = getIntent();
        if (intent != null) {
            if (isGoogleSignUp) {
                email = currentUser.getEmail();
                fullName = currentUser.getDisplayName();
                // Break the full name into first name and family name
                if (fullName != null) {
                    int spaceIndex = fullName.indexOf(' ');
                    if (spaceIndex != -1) {
                        firstName = fullName.substring(0, spaceIndex);
                        familyName = fullName.substring(spaceIndex + 1);
                    } else {
                        firstName = fullName;
                        familyName = "";
                    }
                } else {
                    email = intent.getStringExtra("email");
                    firstName = intent.getStringExtra("firstName");
                    familyName = intent.getStringExtra("familyName");

                }
                String helloUser = "Hello, " + firstName;
                TextView userName = findViewById(R.id.userName);
                userName.setText(helloUser);
            }

        }
    displayCategories();
    }

    public void displayCategories() {
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> categoriesList = (List<String>) document.get("categories");

                            if (categoriesList != null) {
                                // Call function to add "Other" category and handle new category input
                                int numberOfItemsInList = categoriesList.size();

                                for (int i = 1; i <= 12; i++) {
                                    String categoryName = i < numberOfItemsInList ? categoriesList.get(i) : "Other";
                                    int buttonId = getResources().getIdentifier("button" + (i), "id", getPackageName());

                                    Button button = findViewById(buttonId);

                                    if (button != null) {
                                        button.setText(categoryName);
                                        button.setOnClickListener(this::onCategoryButtonClick);
                                    }
                                }

                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }


    public void onCategoryButtonClick(View view) {
        Button button = (Button) view;
        String category = button.getText().toString();
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category);
            button.setSelected(false);
        } else {
            selectedCategories.add(category);
            button.setSelected(true);
        }
    }

    public void doneCategory(View v) {
        Intent toy = new Intent(CategoriesActivity.this, UserDetailsActivity.class);
        toy.putExtra("google_sign_up", isGoogleSignUp);
        toy.putExtra("email", email);
        toy.putExtra("firstName", firstName);
        toy.putExtra("familyName", familyName);
        toy.putStringArrayListExtra("selectedCategories", new ArrayList<>(selectedCategories));
        startActivity(toy);
    }

}