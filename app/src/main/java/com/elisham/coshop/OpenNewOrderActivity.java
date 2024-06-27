package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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

import java.util.ArrayList;
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
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import java.util.Locale;
import android.text.TextWatcher;
import android.view.MotionEvent;

public class OpenNewOrderActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    FirebaseFirestore db;
    Spinner categorySpinner;
    private EditText urlEditText;
    private EditText descriptionEditText;
    private EditText addressEditText;
    private EditText titleEditText;
    private EditText timeEditText;
    private Calendar selectedTime = null;
    LocationManager locationManager;
    String saveNewCategorieName;
    private double latitude;
    private double longitude;
    private int max_people_in_order;
    private EditText maxPeopleEditText;
    private boolean isAddressManuallyEdited = false; // Flag to track manual edits
    private long lastClickTime = 0; // Variable to store the time of the last click

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
        titleEditText = findViewById(R.id.title);
        timeEditText = findViewById(R.id.time);
        // Initialize Spinner
        categorySpinner = findViewById(R.id.category);
        // Read categories from Firestore and populate Spinner
        readCategoriesFromFireStore();

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Add a click listener to the address EditText
        addressEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < 500) { // Double click detected
                        isAddressManuallyEdited = false;
                        checkLocationPermission();
                    } else {
                        isAddressManuallyEdited = true;
                    }
                    lastClickTime = clickTime;
                }
                return false;
            }
        });

        // Track manual edits to the address field
        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text is changed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isAddressManuallyEdited = true; // User manually edited the address
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed after text is changed
            }
        });

        // Initialize views
        maxPeopleEditText = findViewById(R.id.maxPeopleEditText);

        maxPeopleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text is changed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed on text change
            }

            @Override
            public void afterTextChanged(Editable s) {
                int numberOfPeople = maxPeople();
                max_people_in_order = numberOfPeople;
                Log.d("Max People", "Updated max_people_in_order: " + max_people_in_order);
            }
        });

        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text is changed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed on text change
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 25) {
                    s.delete(25, s.length());
                    Toast.makeText(OpenNewOrderActivity.this, "Title cannot exceed 25 characters", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public int maxPeople() {
        String maxPeopleStr = maxPeopleEditText.getText().toString();
        if (maxPeopleStr.isEmpty()) {
            return 0; // Return 0 if no limit or no number is entered
        }
        try {
            return Integer.parseInt(maxPeopleStr);
        } catch (NumberFormatException e) {
            return 0; // Return 0 if the entered text is not a valid number
        }
    }

    public void onMaxPeopleEditTextClick(View view) {
        int numberOfPeople = maxPeople();
        Log.d("Max People", "Max People: " + numberOfPeople);
        max_people_in_order = numberOfPeople;
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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && !isAddressManuallyEdited) { // Only update if not manually edited
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
                                int numberOfItemsInList = categoriesList.size();
                                addOtherCategory(categoriesList, numberOfItemsInList);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void addOtherCategory(List<String> categoriesList, int numberOfItemsInList) {
        categoriesList.add("Other");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedCategory = (String) adapterView.getItemAtPosition(position);
                saveNewCategorieName = selectedCategory;
                Log.d("Selected Category", saveNewCategorieName);
                if (selectedCategory.equals("Other")) {
                    showNewCategoryDialog(categoriesList, numberOfItemsInList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public void showNewCategoryDialog(List<String> categoriesList, int numberOfItemsInList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New Category");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newCategory = input.getText().toString();
                saveNewCategorieName = newCategory;
                Log.d("New Category", saveNewCategorieName);
                if (!newCategory.equalsIgnoreCase("Other") && !newCategory.trim().isEmpty()) {
                    addCategoryToFirestore(newCategory, categoriesList, numberOfItemsInList);
                } else {
                    showNewCategoryDialog(categoriesList, numberOfItemsInList);
                    Toast.makeText(OpenNewOrderActivity.this, "You cannot enter that! Enter again.", Toast.LENGTH_SHORT).show();
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

    public void addCategoryToFirestore(String newCategory, List<String> categoriesList, int numberOfItemsInList) {
        String saveCategory = newCategory;
        String lowercaseCategory = newCategory.toLowerCase();

        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            Log.d("Category Size", String.valueOf(numberOfItemsInList));
                            Log.d("Category List Size", String.valueOf(categoriesList.size()));

                            if ((numberOfItemsInList + 1) < categoriesList.size()) {
                                categoriesList.remove(categoriesList.size() - 1);
                            }
                            categoriesList.add(saveCategory);
                            categorySpinner.setSelection(categoriesList.indexOf(saveCategory));
                        } else {
                            Toast.makeText(OpenNewOrderActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                            String correctCasedCategory = null;
                            for (String category : categoriesList) {
                                if (category.equalsIgnoreCase(newCategory)) {
                                    correctCasedCategory = category;
                                    break;
                                }
                            }
                            categorySpinner.setSelection(categoriesList.indexOf(correctCasedCategory));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
                });
    }

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
                onBackPressed();
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
        Intent intent = new Intent(OpenNewOrderActivity.this, HomePageActivity.class);
        startActivity(intent);
    }

    public void personalInfo() {
        Intent intent = new Intent(OpenNewOrderActivity.this, UserDetailsActivity.class);
        startActivity(intent);
    }

    public void myOrders() {
        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(intent);
    }

    public void aboutUs() {
        Intent intent = new Intent(OpenNewOrderActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    public void contactUs() {
        Intent intent = new Intent(OpenNewOrderActivity.this, ContactUsActivity.class);
        startActivity(intent);
    }

    public void basket() {
        Intent intent = new Intent(OpenNewOrderActivity.this, BasketActivity.class);
        startActivity(intent);
    }

    public void logOut() {
        Intent intent = new Intent(OpenNewOrderActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void goToMyOrders(View v) {
        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        int maxPeople = maxPeople();

        if (url.isEmpty()) {
            url = "";
        } else if (!url.contains("://")) {
            Toast.makeText(this, "Invalid URL. Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saveNewCategorieName.isEmpty() || saveNewCategorieName.equals("Choose Categorie")) {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Location is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Time is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (maxPeople == 0) {
            Toast.makeText(this, "Maximum people is required", Toast.LENGTH_SHORT).show();
            return;
        }

        addCategorieToDataBase();
        saveOrder(url, description, title, time, address, maxPeople);
        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(intent);
    }

    private boolean validateFields() {
        Log.d("Validate Fields", "Start");
        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = saveNewCategorieName;
        String address = addressEditText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();
        boolean valid = true;

        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            valid = false;
        }

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            valid = false;
        }

        if (category.isEmpty() || category.equals("Other") || category.equals("Choose Categorie")) {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            valid = false;
        }

        Log.d("Validate Fields", "End with valid = " + valid);
        return valid;
    }

    public void addCategorieToDataBase() {
        Log.d("Add Category", "Start");
        String lowercaseCategory = saveNewCategorieName.toLowerCase();

        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> categoriesList = (List<String>) documentSnapshot.get("categories");
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                                    .update("categories", FieldValue.arrayUnion(saveNewCategorieName))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(OpenNewOrderActivity.this, "New category added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error adding category: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
                });
    }

    private void saveOrder(String url, String description, String title, String time, String address, int maxPeople) {
        if (url.isEmpty()) {
            url = "";
        } else if (!url.contains("://")) {
            urlEditText.setError("Invalid URL. Please enter a valid URL");
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // שמירת המשתנים כ- final או effectively final
        String finalUserEmail = userEmail;
        String finalUrl = url;
        String finalDescription = description;
        String finalTitle = title;
        String finalTime = time;
        String finalAddress = address;

        // קריאה ל-Firestore כדי לקבל את ה-type of user
        db.collection("users").document(finalUserEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("type of user");

                            // המשך הוספת ההזמנה לאחר קבלת ה-type of user
                            Map<String, Object> order = new HashMap<>();
                            order.put("URL", finalUrl);
                            order.put("description", finalDescription);
                            order.put("categorie", saveNewCategorieName);
                            order.put("user_email", finalUserEmail);
                            order.put("max_people", max_people_in_order);
                            order.put("type_of_order", userType); // הוספת ה-type of user להזמנה
                            order.put("titleOfOrder", finalTitle);
                            order.put("time", finalTime);
                            order.put("address", finalAddress);
                            order.put("NumberOfPeopleInOrder",1);

                            GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                            order.put("location", geoPoint);

                            ArrayList<String> listPeopleInOrder = new ArrayList<>();
                            listPeopleInOrder.add(finalUserEmail);
                            order.put("listPeopleInOrder", listPeopleInOrder);

                            db.collection("orders")
                                    .add(order)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(OpenNewOrderActivity.this, "Order added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error adding order: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add order", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
    }

    public void showTimePickerDialog(View view) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(OpenNewOrderActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar currentDate = Calendar.getInstance();
                currentDate.set(Calendar.YEAR, year);
                currentDate.set(Calendar.MONTH, month);
                currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (currentDate.before(Calendar.getInstance())) {
                    Toast.makeText(OpenNewOrderActivity.this, "You can't choose a past date", Toast.LENGTH_SHORT).show();
                } else {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(OpenNewOrderActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedDate.set(Calendar.MINUTE, minute);

                            if (selectedDate.before(Calendar.getInstance())) {
                                Toast.makeText(OpenNewOrderActivity.this, "You cannot select a past time", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedTime = selectedDate;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String dateTime = sdf.format(selectedDate.getTime());

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
