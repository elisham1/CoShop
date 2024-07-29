package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

// Handles the categories selection and updates for the user
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
    private ProgressDialog progressDialog;

    // Initializes the activity, sets the theme based on user type, and handles signup methods
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

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        isGoogleSignUp = intent.getBooleanExtra("google_sign_up", false);
        isEmailSignUp = intent.getBooleanExtra("email_sign_up", false);
        isCategoriesUpdate = intent.getBooleanExtra("categories_update", false);

        if (isGoogleSignUp) {
            Log.d("CategoriesActivity", "google signup");
            email = currentUser.getEmail();
            fullName = currentUser.getDisplayName();
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
            Log.d("CategoriesActivity", "email signup");
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");
            updateHelloUser();
            displayCategories();
        }
        else if (isCategoriesUpdate) {
            Log.d("CategoriesActivity", "category update");
            progressDialog.show();
            Log.d("CategoriesActivity", "update categories");
            email = currentUser.getEmail();
            db.collection("users").document(email).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    firstName = document.getString("first name");
                                    familyName = document.getString("family name");
                                    selectedCategories = (ArrayList<String>) document.get("favorite categories");
                                    Log.d("firebase", "Name: " + firstName + ", Family Name: " + familyName);
                                    updateHelloUser();
                                    displayCategories();
                                    progressDialog.dismiss();
                                } else {
                                    Log.d("firebase", "No such document");
                                    progressDialog.dismiss();
                                }
                            } else {
                                Log.d("firebase", "get failed with ", task.getException());
                                progressDialog.dismiss();
                            }
                        }
                    });
        } else {
            email = currentUser.getEmail();
            firstName = "User";
            updateHelloUser();
            displayCategories();
        }
    }

    // Updates the greeting message with the user's name
    private void updateHelloUser() {
        String helloUser = "Hello, " + firstName;
        TextView userName = findViewById(R.id.userName);
        userName.setText(helloUser);
    }

    // Displays the categories for the user to select from
    public void displayCategories() {
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> categoriesList = (List<String>) document.get("categories");

                            if (categoriesList != null) {
                                LinearLayout categoryContainer = findViewById(R.id.categoryContainer);
                                categoryContainer.removeAllViews();

                                int numberOfItemsInList = categoriesList.size();
                                Log.d("displayCategories", "Number of categories: " + numberOfItemsInList);

                                LinearLayout rowLayout = null;

                                for (int i = 1; i < numberOfItemsInList; i++) {
                                    if (i % 3 == 1) {
                                        rowLayout = new LinearLayout(this);
                                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                                        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT));
                                        categoryContainer.addView(rowLayout);
                                    }

                                    String categoryName = categoriesList.get(i);
                                    String categoryImage = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categoryName + ".png?alt=media";
                                    Log.d("displayCategories", "Category: " + categoryName + ", Image URL: " + categoryImage);

                                    View categoryView = LayoutInflater.from(this).inflate(R.layout.category_item, rowLayout, false);
                                    ImageView imageView = categoryView.findViewById(R.id.categoryImage);
                                    TextView textView = categoryView.findViewById(R.id.categoryName);

                                    Glide.with(this).load(categoryImage).into(imageView);
                                    imageView.setTag(categoryName);
                                    imageView.setContentDescription("Category image for " + categoryName);

                                    textView.setText(categoryName);
                                    textView.setContentDescription("Category: " + categoryName);

                                    categoryView.setTag(categoryName);
                                    categoryView.setOnClickListener(view -> onCategoryImageClick(categoryView, textView));

                                    if (selectedCategories != null && selectedCategories.contains(categoryName)) {
                                        textView.setTextColor(getResources().getColor(R.color.white));
                                        if (globalUserType.equals("Supplier"))
                                            categoryView.setBackgroundResource(R.drawable.bg_category_supplier);
                                        else
                                            categoryView.setBackgroundResource(R.drawable.bg_category_consumer);
                                    }

                                    rowLayout.addView(categoryView);
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

    // Handles category image click event
    public void onCategoryImageClick(View categoryView, TextView textView) {
        String category = (String) categoryView.getTag();
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category);
            textView.setTextColor(getResources().getColor(R.color.black));
            categoryView.setBackgroundResource(R.drawable.border);
        } else {
            selectedCategories.add(category);
            textView.setTextColor(getResources().getColor(R.color.white));
            if (globalUserType.equals("Supplier"))
                categoryView.setBackgroundResource(R.drawable.bg_category_supplier);
            else
                categoryView.setBackgroundResource(R.drawable.bg_category_consumer);
        }
    }

    // Handles the done button click event and updates user details or navigates to UserDetailsActivity
    public void doneCategory(View v) {
        if (selectedCategories.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("No Categories Selected");
            builder.setMessage("Please select at least one category.");
            builder.setPositiveButton("OK", (dialog, which) -> {});
            builder.show();
            return;
        }

        if (isCategoriesUpdate) {
            Map<String, Object> userDetails = new HashMap<>();

            userDetails.put("favorite categories", selectedCategories);
            db.collection("users").document(email)
                    .update(userDetails)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("CategoriesActivity", "User details updated.");
                            Intent toy = new Intent(CategoriesActivity.this, UpdateUserDetailsActivity.class);
                            toy.putExtra("userType", globalUserType);
                            startActivity(toy);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("CategoriesActivity", "Error updating user details", e);
                        }
                    });
        } else {
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
