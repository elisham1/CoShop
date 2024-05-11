package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
public class OpenNewOrderActivity extends AppCompatActivity {
    FirebaseFirestore db;
    Spinner categorySpinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_new_order);
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        // Initialize Spinner
        categorySpinner = findViewById(R.id.category);
        // Read categories from Firestore and populate Spinner
        readCategoriesFromFireStore();

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


    }

    public void readCategoriesFromFireStore() {
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> categoriesList = (List<String>) document.get("categories");
                            if (categoriesList != null) {
                                // Call function to add "Other" category and handle new category input
                                addOtherCategory(categoriesList);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void addOtherCategory(List<String> categoriesList) {
        // Check if "Other" category already exists
        if (!categoriesList.contains("Other")) {
            // Add "Other" category to the list
            categoriesList.add("Other");

            // Create adapter and set it to the Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);

            // Handle selection of "Other" category
            categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    String selectedCategory = (String) adapterView.getItemAtPosition(position);
                    if (selectedCategory.equals("Other")) {
                        // Show dialog for entering new category
                        showNewCategoryDialog();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // Do nothing
                }
            });
        }
    }

    public void showNewCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New Category");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newCategory = input.getText().toString();
                // Add new category to Firestore
                addCategoryToFirestore(newCategory);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void addCategoryToFirestore(String newCategory) {
        // Convert the new category to lowercase for case-insensitive comparison
        String lowercaseCategory = newCategory.toLowerCase();

        // Add new category to Firestore
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> categoriesList = (List<String>) documentSnapshot.get("categories");
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            // Category doesn't exist, add it
                            db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                                    .update("categories", FieldValue.arrayUnion(newCategory))
                                    .addOnSuccessListener(aVoid -> {
                                        // Category added successfully
                                        Toast.makeText(OpenNewOrderActivity.this, "New category added successfully", Toast.LENGTH_SHORT).show();

                                        // Update categoriesList with new category
                                        categoriesList.add(newCategory);

                                        // Update Spinner with new category
                                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(OpenNewOrderActivity.this, android.R.layout.simple_spinner_item, categoriesList);
                                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        categorySpinner.setAdapter(spinnerAdapter);
                                        categorySpinner.setSelection(categoriesList.indexOf(newCategory));
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error adding category
                                        Log.e("Firestore", "Error adding category: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Category already exists
                            Toast.makeText(OpenNewOrderActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                            // Update Spinner with existing category
                            categorySpinner.setSelection(categoriesList.indexOf(lowercaseCategory));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Error retrieving categories
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
                });
    }

    // Helper function to check if a list contains a string (case-insensitive)
    private boolean containsIgnoreCase(List<String> list, String str) {
        for (String s : list) {
            if (s.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
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
        Intent toy = new Intent(OpenNewOrderActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(OpenNewOrderActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(OpenNewOrderActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(OpenNewOrderActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(OpenNewOrderActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(OpenNewOrderActivity.this, MainActivity.class);
        startActivity(toy);
    }

    public void goToMyOrders(View v) {
        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(intent);
    }

}