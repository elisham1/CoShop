package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FilterActivity extends AppCompatActivity implements LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    ListView categoryListView;
    FirebaseFirestore db;
    ArrayAdapter<String> adapter;
    private EditText addressEditText;
    private LocationManager locationManager;
    private boolean isAddressManuallyEdited = false; // Flag to track manual edits
    private long lastClickTime = 0; // Variable to store the time of the last click
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize ListView
        categoryListView = findViewById(R.id.category_list);

        // Get references to the EditText fields
        addressEditText = findViewById(R.id.address);

        // Read categories from Firestore and populate ListView
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

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text is changed
            }


            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isAddressManuallyEdited = true; // User manually edited the address
            }


            public void afterTextChanged(Editable s) {
                // No action needed after text is changed
            }
        });
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
                            if (categoriesList != null && categoriesList.size() > 1) {
                                // Skip the first category
                                List<String> subList = categoriesList.subList(1, categoriesList.size());
                                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, subList);
                                categoryListView.setAdapter(adapter);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
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
        Intent toy = new Intent(FilterActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(FilterActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(FilterActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(FilterActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(FilterActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(FilterActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(FilterActivity.this, MainActivity.class);
        startActivity(toy);
    }
}
