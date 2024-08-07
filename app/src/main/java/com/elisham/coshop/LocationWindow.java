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
import android.widget.TextView;

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
    private LinearLayout distanceLayout;
    private NumberPicker numberPicker;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private ArrayAdapter<String> addressAdapter;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private ImageButton clearAddressButton;
    public ActivityResultLauncher<Intent> locationSettingsLauncher;
    private String lastValidAddress;
    private LocationFunctions locationFunctions;
    private String globalUserType;

    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the theme based on the user type
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        } else {
            setTheme(R.style.ConsumerTheme);
        }

        setContentView(R.layout.location_window);
        menuUtils = new MenuUtils(this, globalUserType);

        // Hide the title in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        addressEditText = findViewById(R.id.address);
        distanceLayout = findViewById(R.id.distance_layout);

        locationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (locationFunctions.isLocationEnabled()) {
                        locationFunctions.checkLocationAndFetch();
                    } else {
                        Log.d("LocationWindow", "Please enable location services");
                    }
                }
        );

        locationFunctions = new LocationFunctions(this, addressEditText, locationSettingsLauncher);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationFunctions.checkLocationAndFetch();
        }

        String apiKey = "AIzaSyCCEZAKwn0TCA-XvVpDKTOVrdiM__RfwCI"; // Replace with your API key

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
                finish(); // Close the dialog
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
                boolean hideDistanceLayout = getIntent().getBooleanExtra("hideDistanceLayout", false);
                String distanceStr = hideDistanceLayout ? "0" : distanceEditText.getText().toString().replace(" KM", "").trim();

                boolean isValid = true;

                if (address.isEmpty()) {
                    findViewById(R.id.address_layout).setBackgroundResource(R.drawable.red_border);
                    findViewById(R.id.addressError).setVisibility(View.VISIBLE);
                    ((TextView) (findViewById(R.id.addressError))).setText("Field is required");
                    isValid = false;
                } else {
                    findViewById(R.id.address_layout).setBackgroundResource(R.drawable.border);
                    findViewById(R.id.addressError).setVisibility(View.GONE);
                }

                if (!hideDistanceLayout && distanceStr.isEmpty()) {
                    findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.red_border);
                    findViewById(R.id.distanceError).setVisibility(View.VISIBLE);
                    isValid = false;
                } else {
                    findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.border);
                    findViewById(R.id.distanceError).setVisibility(View.GONE);
                }

                if (isValid) {
                    findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.border);
                    findViewById(R.id.distanceError).setVisibility(View.GONE);
                    if (lastValidAddress == null) {
                        locationFunctions.fetchAddressCoordinates(address, distanceStr);
                    } else {
                        locationFunctions.sendResult(address, distanceStr);
                    }
                } else {
                    findViewById(R.id.address_layout).setBackgroundResource(R.drawable.red_border);
                    findViewById(R.id.addressError).setVisibility(View.VISIBLE);
                    ((TextView) (findViewById(R.id.addressError))).setText("Please enter a valid address");
                    Log.d("LocationWindow", "Please enter a valid address" + (hideDistanceLayout ? "" : " and distance"));
                }
            }
        });

        boolean hideDistanceLayout = intent.getBooleanExtra("hideDistanceLayout", false);
        String address = intent.getStringExtra("address");
        int distance = intent.getIntExtra("distance", 0);

        if (hideDistanceLayout) {
            distanceEditText.setVisibility(View.INVISIBLE);
            numberPicker.setVisibility(View.INVISIBLE);
            distanceLayout.setVisibility(View.INVISIBLE);
        }

        if (address != null && !address.isEmpty()) {
            addressEditText.setText(address);
            lastValidAddress = address;
        } else {
            addressEditText.setText(""); // Reset the field
        }
        if (distance > 0) {
            distanceEditText.setText(distance + " KM");
            numberPicker.setValue(distance);
        } else {
            distanceEditText.setText(""); // Reset the field
            numberPicker.setValue(0);
        }
    }

    // Fetches autocomplete predictions for the entered query.
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
                findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.red_border);
                findViewById(R.id.distanceError).setVisibility(View.VISIBLE);
                findViewById(R.id.address_layout).setBackgroundResource(R.drawable.red_border);
                findViewById(R.id.addressError).setVisibility(View.VISIBLE);
                ((TextView) (findViewById(R.id.addressError))).setText("Address not found");
            } else {
                findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.border);
                findViewById(R.id.distanceError).setVisibility(View.GONE);
                findViewById(R.id.address_layout).setBackgroundResource(R.drawable.border);
                findViewById(R.id.addressError).setVisibility(View.GONE);
            }
        }).addOnFailureListener(exception -> {
            Log.d("LocationWindow", "Error fetching predictions");
        });
    }

    // Sets the error state for distance layout.
    public void setError() {
        findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.red_border);
        findViewById(R.id.distanceError).setVisibility(View.VISIBLE);
    }

    // Clears the error state for distance layout.
    public void setNotError() {
        findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.border);
        findViewById(R.id.distanceError).setVisibility(View.GONE);
    }

    // Fetches the place details using placeId.
    private void fetchPlace(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            if (place.getLatLng() != null) {
                findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.border);
                findViewById(R.id.distanceError).setVisibility(View.GONE);
                double lat = place.getLatLng().latitude;
                double lon = place.getLatLng().longitude;
                Log.d("LocationWindow", "Latitude: " + lat + ", Longitude: " + lon);

                lastValidAddress = place.getAddress();
            } else {
                findViewById(R.id.distance_layout).setBackgroundResource(R.drawable.red_border);
                findViewById(R.id.distanceError).setVisibility(View.VISIBLE);
                Log.d("LocationWindow", "Address not found in Google API");
            }
        }).addOnFailureListener((exception) -> {
            Log.d("LocationWindow", "Error fetching place details");
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

    // Adjusts the dialog size.
    private void adjustDialogSize() {
        int width = (int) (getResources().getDisplayMetrics().density * 370); // Fixed size in DP
        int height = (int) (getResources().getDisplayMetrics().density * 350); // Fixed size in DP
        getWindow().setLayout(width, height);
    }

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo();
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders();
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs();
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs();
                return true;
            case R.id.Log_Out:
                menuUtils.logOut();
                return true;
            case R.id.home:
                menuUtils.home();
                return true;
            case R.id.chat_icon:
                menuUtils.allChats();
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
