package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoriesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String email, firstName, familyName, fullName;
    private List<String> selectedCategories;
    private boolean isGoogleSignUp;
    private boolean isEmailSignUp;
    private boolean isCategoriesUpdate;
    private String globalUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType == null) {
            globalUserType = "Consumer";
        }
        if (globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }
        setContentView(R.layout.activity_categories);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        selectedCategories = new ArrayList<>();

        //get the user mail and name

            isGoogleSignUp = intent.getBooleanExtra("google_sign_up", false);
            isEmailSignUp = intent.getBooleanExtra("email_sign_up", false);
            isCategoriesUpdate = intent.getBooleanExtra("categories_update", false);

            if (isGoogleSignUp) {
                Toast.makeText(CategoriesActivity.this, "google signup", Toast.LENGTH_SHORT).show();
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
                }
                updateHelloUser();
                displayCategories();
            }
            else if (isEmailSignUp) {
                Toast.makeText(CategoriesActivity.this, "email signup", Toast.LENGTH_SHORT).show();
                email = intent.getStringExtra("email");
                firstName = intent.getStringExtra("firstName");
                familyName = intent.getStringExtra("familyName");
                updateHelloUser();
                displayCategories();
            }
            else if (isCategoriesUpdate) {
                Toast.makeText(CategoriesActivity.this, "update categories", Toast.LENGTH_SHORT).show();
                email = currentUser.getEmail();
                db.collection("users").document(email).get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        // Get the name and family name fields
                                        firstName = document.getString("first name");
                                        familyName = document.getString("family name");
                                        selectedCategories = (ArrayList<String>) document.get("favorite categories");
                                        // Log or use the retrieved information
                                        Log.d("firebase", "Name: " + firstName + ", Family Name: " + familyName);
                                        updateHelloUser();
                                        displayCategories();
                                    }
                                    else {
                                        Log.d("firebase", "No such document");
                                    }
                                }
                                else {
                                    Log.d("firebase", "get failed with ", task.getException());
                                }
                            }
                        });
            }
            else {
                email = currentUser.getEmail();
                firstName = "User";
                updateHelloUser();
                displayCategories();
            }


    }

    private void updateHelloUser() {
        String helloUser = "Hello, " + firstName;
        TextView userName = findViewById(R.id.userName);
        userName.setText(helloUser);
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
                                GridLayout categoryGrid = findViewById(R.id.categoryGrid);
                                categoryGrid.removeAllViews();  // Clear any existing views

                                int numberOfItemsInList = categoriesList.size();
                                Log.d("displayCategories", "Number of categories: " + numberOfItemsInList);

                                for (int i = 1; i < numberOfItemsInList; i++) {  // Start from index 1
                                    String categoryName = categoriesList.get(i);
                                    String categoryImage = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categoryName + ".png?alt=media";
                                    Log.d("displayCategories", "Category: " + categoryName + ", Image URL: " + categoryImage);

                                    // Dynamically create ImageView and TextView for each category
                                    ImageView imageView = new ImageView(this);
                                    TextView textView = new TextView(this);

                                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                                    params.width = getResources().getDimensionPixelSize(R.dimen.image_size);
                                    params.height = getResources().getDimensionPixelSize(R.dimen.image_size);
                                    params.setMargins(8, 8, 8, 8);
                                    imageView.setLayoutParams(params);
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    imageView.setBackgroundResource(R.drawable.image_background);
                                    Glide.with(this).load(categoryImage).into(imageView);
                                    imageView.setTag(categoryName);

                                    // Set content description for accessibility
                                    imageView.setContentDescription("Category image for " + categoryName);

                                    if (selectedCategories != null && selectedCategories.contains(categoryName)) {
                                        imageView.setBackgroundResource(R.drawable.image_background);
                                    }

                                    textView.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    ));
                                    textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    textView.setPadding(4, 4, 4, 4);
                                    textView.setText(categoryName);
                                    textView.setTextColor(getResources().getColor(R.color.black));
                                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                                    // Set content description for the text view
                                    textView.setContentDescription("Category: " + categoryName);

                                    LinearLayout categoryLayout = new LinearLayout(this);
                                    categoryLayout.setOrientation(LinearLayout.VERTICAL);
                                    categoryLayout.setBackgroundResource(R.drawable.border);
                                    categoryLayout.setGravity(Gravity.CENTER);
                                    categoryLayout.setTag(categoryName);
                                    categoryLayout.setLayoutParams(new GridLayout.LayoutParams());
                                    categoryLayout.addView(imageView);
                                    categoryLayout.addView(textView);
                                    categoryLayout.setOnClickListener(view -> onCategoryImageClick(categoryLayout, textView));
                                    categoryGrid.addView(categoryLayout);
                                }

                            } else {
                                Log.d("displayCategories", "Categories list is null");
                            }
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void onCategoryImageClick(LinearLayout categoryLayout, TextView textView) {
        String category = (String) categoryLayout.getTag();
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category);
            textView.setTextColor(getResources().getColor(R.color.black));
            categoryLayout.setBackgroundResource(R.drawable.border);  // Set default background
        } else {
            selectedCategories.add(category);
            textView.setTextColor(getResources().getColor(R.color.white));
            if (globalUserType.equals("Supplier"))
                categoryLayout.setBackgroundResource(R.drawable.bg_category_supplier);
            else
                categoryLayout.setBackgroundResource(R.drawable.bg_category_consumer);  // Set selected background
        }
    }

    public void doneCategory(View v) {
        if (selectedCategories.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Categories Selected");
            builder.setMessage("Please select at least one category.");
            builder.setPositiveButton("OK", (dialog, which) -> {
                // Do nothing, just close the dialog
            });
            builder.show();
            return;
        }

        if (isCategoriesUpdate)
        {
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("favorite categories", selectedCategories);
            db.collection("users").document(email)
                    .update(userDetails)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(CategoriesActivity.this, "User details updated successfully", Toast.LENGTH_SHORT).show();
                            Log.d("CategoriesActivity", "User details updated.");
                            // Optionally, navigate to another activity or perform further actions upon success
                            Intent toy = new Intent(CategoriesActivity.this, UpdateUserDetailsActivity.class);
                            toy.putExtra("userType", globalUserType);
                            startActivity(toy);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CategoriesActivity.this, "Failed to update user details", Toast.LENGTH_SHORT).show();
                            Log.e("CategoriesActivity", "Error updating user details", e);
                        }
                    });
        }
        else {
            Intent toy = new Intent(CategoriesActivity.this, UserDetailsActivity.class);
            toy.putExtra("google_sign_up", isGoogleSignUp);
            toy.putExtra("email", email);
            toy.putExtra("firstName", firstName);
            toy.putExtra("familyName", familyName);
            toy.putStringArrayListExtra("selectedCategories", new ArrayList<>(selectedCategories));
            toy.putExtra("userType", globalUserType);
            startActivity(toy);
            finish();
        }
    }
}