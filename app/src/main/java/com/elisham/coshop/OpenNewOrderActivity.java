package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import androidx.appcompat.app.AlertDialog;
import java.text.SimpleDateFormat;


import android.content.DialogInterface;
import android.text.InputType;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import java.util.Locale;

public class OpenNewOrderActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    FirebaseFirestore db;
    Spinner categorySpinner;
    private EditText urlEditText;
    private EditText descriptionEditText;

    private EditText addressEditText;

    private Calendar selectedTime = null;
    LocationManager locationManager;
    String saveNewCategorieName;

    private double latitude ;
    private double longitude ;

    private int max_people_in_order;
    private RadioGroup maxPeopleRadioGroup;
    private EditText maxPeopleEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_new_order);
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        // Get references to the EditText fields
        urlEditText = findViewById(R.id.url);
        descriptionEditText = findViewById(R.id.description);
        addressEditText = findViewById(R.id.address);
        // Initialize Spinner
        categorySpinner = findViewById(R.id.category);
        // Read categories from Firestore and populate Spinner
        readCategoriesFromFireStore();

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Get reference to the EditText
        addressEditText = findViewById(R.id.address);

        // Add a click listener to the EditText
        addressEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check location permission when clicked
                checkLocationPermission();
            }
        });


        // Initialize views
        maxPeopleRadioGroup = findViewById(R.id.maxPeopleRadioGroup);
        maxPeopleEditText = findViewById(R.id.maxPeopleEditText);

        // Set up RadioGroup listener
        maxPeopleRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.noLimitOption) {
                    maxPeopleEditText.setEnabled(false);
                    maxPeopleEditText.setText(""); // Clear text when disabled
                } else if (checkedId == R.id.limitOption) {
                    maxPeopleEditText.setEnabled(true);
                }
            }
        });
    }

    // Function to get the number of people
    public int maxPeople() {
        String maxPeopleStr = maxPeopleEditText.getText().toString();
        if (maxPeopleStr.isEmpty()) {
            return 0; // Return 0 if no limit or no number is entered
        }
        return Integer.parseInt(maxPeopleStr);
    }

    // Function that gets called when the EditText is clicked
    public void onMaxPeopleEditTextClick(View view) {
        // Do something when EditText is clicked
        // For demonstration, let's print the max people value
        int numberOfPeople = maxPeople();
        Log.d("gggggggggggggg", "Max People: " + numberOfPeople);
        max_people_in_order=0;
        max_people_in_order=numberOfPeople;
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Toast.makeText(this, "Latitude: " + latitude + ", Longitude: " + longitude, Toast.LENGTH_SHORT).show();
            Log.d("Location Info", "Latitude: " + latitude + ", Longitude: " + longitude);

            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    Log.d("Location Address", address);

                    // Set the address text to the address EditText
                    addressEditText.setText(address);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
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
                                int numberOfItemsInList = categoriesList.size();
                                addOtherCategory(categoriesList,numberOfItemsInList);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void addOtherCategory(List<String> categoriesList,int numberOfItemsInList) {

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
                saveNewCategorieName="";
                saveNewCategorieName=selectedCategory;
                Log.d("MyTag",saveNewCategorieName);
                if (selectedCategory.equals("Other")) {
                    // Show dialog for entering new category
                    showNewCategoryDialog(categoriesList,numberOfItemsInList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

    }

    public void showNewCategoryDialog(List<String> categoriesList,int numberOfItemsInList) {
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
                saveNewCategorieName="";
                saveNewCategorieName=newCategory;
                Log.d("MyTag",saveNewCategorieName);
                // Add new category to Firestore
                if(!newCategory.equalsIgnoreCase("Other") && !newCategory.trim().isEmpty()){
                    addCategoryToFirestore(newCategory,categoriesList,numberOfItemsInList);
                }
                else{
                    showNewCategoryDialog(categoriesList,numberOfItemsInList);
                    Toast.makeText(OpenNewOrderActivity.this, "You can not enter that! enter again.", Toast.LENGTH_SHORT).show();

                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                readCategoriesFromFireStore();
            }
        });

        builder.show();
    }

    public void addCategoryToFirestore(String newCategory,List<String> categoriesList,int numberOfItemsInList) {
        // Convert the new category to lowercase for case-insensitive comparison
        String saveCategory=newCategory;
        String lowercaseCategory = newCategory.toLowerCase();


        // Add new category to Firestore
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
//                    List<String> categoriesList = (List<String>) documentSnapshot.get("categories");
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            Toast.makeText(OpenNewOrderActivity.this, "hi", Toast.LENGTH_SHORT).show();
                            Log.d("MyTag", String.valueOf(numberOfItemsInList));
                            Log.d("MyTag", String.valueOf(categoriesList.size()));

                            if((numberOfItemsInList+1)<categoriesList.size()){//the user add categories but regret so delete the last one to add the new
                                categoriesList.remove(categoriesList.size() - 1);
                            }
                            categoriesList.add(saveCategory);
                            // Do whatever else you need to do with the new category locally
                            categorySpinner.setSelection(categoriesList.indexOf(saveCategory));
                        } else {
                            // Category already exists
                            Toast.makeText(OpenNewOrderActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                            // Update Spinner with existing category
                            // Find the correct casing of the new category in the categories list
                            String correctCasedCategory = null;
                            for (String category : categoriesList) {
                                if (category.equalsIgnoreCase(newCategory)) {
                                    correctCasedCategory = category;
                                    break;
                                }
                            }

                            categorySpinner.setSelection(categoriesList.indexOf(correctCasedCategory));

                            //categorySpinner.setSelection(categoriesList.indexOf(newCategory));

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
        addCategorieToDataBase();
        saveOrder();
        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(intent);
    }


    public void addCategorieToDataBase(){
        Log.d("MyTag", "yomadeit");
        String lowercaseCategory = saveNewCategorieName.toLowerCase();

        // Add new category to Firestore
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> categoriesList = (List<String>) documentSnapshot.get("categories");
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            // Category doesn't exist, add it
                            db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                                    .update("categories", FieldValue.arrayUnion(saveNewCategorieName))
                                    .addOnSuccessListener(aVoid -> {
                                        // Category added successfully
                                        Toast.makeText(OpenNewOrderActivity.this, "New category added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error adding category
                                        Log.e("Firestore", "Error adding category: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Error retrieving categories
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
                });
    }

    private void saveOrder() {
        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (url.isEmpty() || description.isEmpty() || saveNewCategorieName.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current user's email
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            // Handle case when user is not logged in
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new order object
        Map<String, Object> order = new HashMap<>();
        order.put("URL", url);
        order.put("description", description);
        order.put("categorie", saveNewCategorieName);
        order.put("user_email", userEmail); // Add user's email to the order
        order.put("max_people",max_people_in_order);

        // Create a GeoPoint for the location
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        // Add the GeoPoint to the order map
        order.put("location", geoPoint);

        // Check if selectedTime is not null and set
        if (selectedTime != null) {
            Date selectedDate = selectedTime.getTime();
            order.put("time", selectedDate);
        } else {
            order.put("time", null); // Set time to null if not selected
        }

        // Add a new document with a generated ID
        db.collection("orders")
                .add(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(OpenNewOrderActivity.this, "Order added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding order: " + e.getMessage());
                    Toast.makeText(OpenNewOrderActivity.this, "Failed to add order", Toast.LENGTH_SHORT).show();
                });
    }

    public void showTimePickerDialog(View view) {
        // פותחים את ה-DatePickerDialog
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(OpenNewOrderActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // בודקים אם התאריך שנבחר גדול מהתאריך הנוכחי
                Calendar currentDate = Calendar.getInstance();
                currentDate.set(Calendar.YEAR, year);
                currentDate.set(Calendar.MONTH, month);
                currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (currentDate.before(Calendar.getInstance())) {
                    // התאריך שנבחר הוא תאריך עתידי
                    Toast.makeText(OpenNewOrderActivity.this, "You can't choose last day", Toast.LENGTH_SHORT).show();
                } else {
                    // לאחר בחירת תאריך, פותחים את ה-TimePickerDialog
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(OpenNewOrderActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedDate.set(Calendar.MINUTE, minute);

                            if (selectedDate.before(Calendar.getInstance())) {
                                // השעה שנבחרה כבר עברה
                                Toast.makeText(OpenNewOrderActivity.this, "You cannot select a past time", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedTime = selectedDate; // Update the selectedTime variable
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String dateTime = sdf.format(selectedDate.getTime());

                                EditText timeEditText = findViewById(R.id.time);
                                timeEditText.setText(dateTime);
                            }
                        }
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }



}