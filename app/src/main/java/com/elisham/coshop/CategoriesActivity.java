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
    private String email, firstName, familyName;
    private List<String> selectedCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        selectedCategories = new ArrayList<>();

        Intent intent = getIntent();
        if (intent != null) {
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");

            String helloUser = "Hello, " + firstName;
            TextView userName = findViewById(R.id.userName);
            userName.setText(helloUser);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle the back button click
        if (id == android.R.id.home) {
            onBackPressed(); // Go back when the back arrow is clicked
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doneCategory(View v) {
        Intent toy = new Intent(CategoriesActivity.this, UserDetailsActivity.class);
        toy.putExtra("email", email);
        toy.putExtra("firstName", firstName);
        toy.putExtra("familyName", familyName);
        toy.putStringArrayListExtra("selectedCategories", new ArrayList<>(selectedCategories));
        startActivityForResult(toy, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }

}