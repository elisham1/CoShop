package com.elisham.coshop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Locale;

public class LocationFunctions {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final int FASTEST_LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private FusedLocationProviderClient fusedLocationClient;
    private Context context;
    private AutoCompleteTextView addressEditText;
    private String lastValidAddress;
    private ActivityResultLauncher<Intent> locationSettingsLauncher;
    private LocationCallback locationCallback;
    private CancellationTokenSource cancellationTokenSource;

    public LocationFunctions(Context context, AutoCompleteTextView addressEditText, ActivityResultLauncher<Intent> locationSettingsLauncher) {
        this.context = context;
        this.addressEditText = addressEditText;
        this.locationSettingsLauncher = locationSettingsLauncher;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        initializeLocationCallback();
    }

    private void initializeLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateAddressField(location);
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        };
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (!isLocationEnabled()) {
                Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                locationSettingsLauncher.launch(intent);
            } else {
                showLocationDialog();
            }
        }
    }

    public void handleLocationPermissionResult(int requestCode, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLocationDialog();
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                context.startActivity(intent);
            }
        }
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Use Current Location")
                .setMessage("Do you want to use your current location?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    } else {
                        resetLocationFields();
                        getLocationAndSetAddress();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void resetLocationFields() {
        addressEditText.setText("");
        lastValidAddress = null;
    }

    public void checkLocationAndFetch() {
        if (isLocationEnabled()) {
            getLocationAndSetAddress();
        } else {
            Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            locationSettingsLauncher.launch(intent);
        }
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private void getLocationAndSetAddress() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (!isLocationEnabled()) {
                Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                locationSettingsLauncher.launch(intent);
            } else {
                cancellationTokenSource = new CancellationTokenSource();

                fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Location location = task.getResult();
                                    updateAddressField(location);
                                } else {
                                    startLocationUpdates();
                                }
                            }
                        });
            }
        } else {
            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(LOCATION_UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateAddressField(Location location) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    addressEditText.setText(address);
                    lastValidAddress = address;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }}
    public void fetchAddressCoordinates(String address, String distanceStr) {
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                lastValidAddress = location.getAddressLine(0); // Use the address in English
                sendResult(lastValidAddress, distanceStr);
                hideError();
            } else {
                showError("Address not found");
                Toast.makeText(context, "Address not found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Geocoding failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendResult(String address, String distanceStr) {
        int distance = Integer.parseInt(distanceStr);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("address", address);
        resultIntent.putExtra("distance", distance);

        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                resultIntent.putExtra("latitude", lat);
                resultIntent.putExtra("longitude", lon);
                hideError();
            }
        } catch (Exception e) {
            showError("no address");
            e.printStackTrace();
        }

        ((AppCompatActivity) context).setResult(AppCompatActivity.RESULT_OK, resultIntent);
        ((AppCompatActivity) context).finish();
    }

    private void showError(String errorMessage) {
        ((AppCompatActivity) context).findViewById(R.id.address_layout).setBackgroundResource(R.drawable.red_border);
        ((AppCompatActivity) context).findViewById(R.id.addressError).setVisibility(View.VISIBLE);
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        ((TextView) ((AppCompatActivity) context).findViewById(R.id.addressError)).setText(errorMessage);
    }

    private void hideError() {
        ((AppCompatActivity) context).findViewById(R.id.address_layout).setBackgroundResource(R.drawable.border);
        ((AppCompatActivity) context).findViewById(R.id.addressError).setVisibility(View.GONE);

    }
}
