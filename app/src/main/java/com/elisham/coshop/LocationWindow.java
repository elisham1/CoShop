package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationWindow extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private AutoCompleteTextView addressEditText;
    private AutoCompleteTextView distanceEditText;
    private NumberPicker numberPicker;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private ArrayAdapter<String> addressAdapter;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private ImageButton clearAddressButton;
    public ActivityResultLauncher<Intent> locationSettingsLauncher;
    private String lastValidAddress;
    private LocationFunctions locationFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.location_window);

        // Hide the title in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        addressEditText = findViewById(R.id.address);

        locationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (locationFunctions.isLocationEnabled()) {
                        locationFunctions.checkLocationAndFetch();
                    } else {
                        Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        locationFunctions = new LocationFunctions(this, addressEditText, locationSettingsLauncher);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationFunctions.checkLocationAndFetch();
        }

        String apiKey = "AIzaSyCCEZAKwn0TCA-XvVpDKTOVrdiM__RfwCI"; // החלף במפתח ה-API שלך

        // Initialize Places
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        addressEditText.setAdapter(addressAdapter);

        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearAddressButton.setVisibility(View.VISIBLE);
                } else {
                    clearAddressButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    getAutocompletePredictions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        addressEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedAddress = (String) parent.getItemAtPosition(position);
            for (AutocompletePrediction prediction : predictionList) {
                if (prediction.getFullText(null).toString().equals(selectedAddress)) {
                    fetchPlace(prediction.getPlaceId());
                    break;
                }
            }
        });

        numberPicker = findViewById(R.id.number_picker);
        distanceEditText = findViewById(R.id.distance);

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(20000);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        Button closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // סגירת הדיאלוג
            }
        });

        clearAddressButton = findViewById(R.id.clear_address_button);
        clearAddressButton.setOnClickListener(v -> {
            addressEditText.setText("");
            lastValidAddress = null;
        });

        // Set onClickListener for the get location button
        LinearLayout getLocationButton = findViewById(R.id.get_location_button);
        getLocationButton.setOnClickListener(v -> locationFunctions.checkLocationPermission());

        distanceEditText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) {
                    return;
                }

                isUpdating = true;

                String input = s.toString().replace(" KM", "").trim();
                if (!input.isEmpty()) {
                    try {
                        int distance = Integer.parseInt(input);
                        if (distance > 20000) {
                            distance = 20000;
                        }
                        distanceEditText.setText(distance + " KM");
                        distanceEditText.setSelection(distanceEditText.getText().length() - 3); // Move cursor before " KM"
                        numberPicker.setValue(distance);
                    } catch (NumberFormatException e) {
                        distanceEditText.setText("");
                    }
                }

                isUpdating = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> distanceEditText.setText(newVal + " KM"));

        addressEditText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedAddress = (String) parent.getItemAtPosition(position);
            for (AutocompletePrediction prediction : predictionList) {
                if (prediction.getFullText(null).toString().equals(selectedAddress)) {
                    fetchPlace(prediction.getPlaceId());
                    break;
                }
            }
        });

        Button okButton = findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressEditText.getText().toString();
                String distanceStr = distanceEditText.getText().toString().replace(" KM", "").trim();

                if (!address.isEmpty() && !distanceStr.isEmpty()) {
                    if (lastValidAddress == null) {
                        locationFunctions.fetchAddressCoordinates(address, distanceEditText);
                    } else {
                        locationFunctions.sendResult(address, distanceStr);
                    }
                } else {
                    Toast.makeText(LocationWindow.this, "Please enter a valid address and distance", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String address = intent.getStringExtra("address");
            int distance = intent.getIntExtra("distance", 0);
            if (address != null && !address.isEmpty()) {
                addressEditText.setText(address);
                lastValidAddress = address;
            } else {
                addressEditText.setText(""); // איפוס השדה
            }
            if (distance > 0) {
                distanceEditText.setText(distance + " KM");
                numberPicker.setValue(distance);
            } else {
                distanceEditText.setText(""); // איפוס השדה
                numberPicker.setValue(0);
            }
        }
    }

    private void getAutocompletePredictions(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            predictionList.clear();
            predictionList.addAll(response.getAutocompletePredictions());

            List<String> suggestions = new ArrayList<>();
            for (AutocompletePrediction prediction : predictionList) {
                suggestions.add(prediction.getFullText(null).toString());
            }

            addressAdapter.clear();
            addressAdapter.addAll(suggestions);
            addressAdapter.notifyDataSetChanged();

            if (suggestions.isEmpty()) {
                Toast.makeText(LocationWindow.this, "Address not found in Google API", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(LocationWindow.this, "Error fetching predictions", Toast.LENGTH_LONG).show();
        });
    }

    private void fetchPlace(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            if (place.getLatLng() != null) {
                double lat = place.getLatLng().latitude;
                double lon = place.getLatLng().longitude;
                Toast.makeText(LocationWindow.this, "Latitude: " + lat + ", Longitude: " + lon, Toast.LENGTH_LONG).show();

                lastValidAddress = place.getAddress();
            } else {
                Toast.makeText(LocationWindow.this, "Address not found in Google API", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener((exception) -> {
            Toast.makeText(LocationWindow.this, "Error fetching place details", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationFunctions.checkLocationAndFetch();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationFunctions.handleLocationPermissionResult(requestCode, grantResults);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            adjustDialogSize();
        }
    }

    private void adjustDialogSize() {
        int width = (int) (getResources().getDisplayMetrics().density * 370); // גודל קבוע ב-DP
        int height = (int) (getResources().getDisplayMetrics().density * 350); // גודל קבוע ב-DP
        getWindow().setLayout(width, height);
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
        Intent toy = new Intent(LocationWindow.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(LocationWindow.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(LocationWindow.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(LocationWindow.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(LocationWindow.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(LocationWindow.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(LocationWindow.this, MainActivity.class);
        startActivity(toy);
    }
}
