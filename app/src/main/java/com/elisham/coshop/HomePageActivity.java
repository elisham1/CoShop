package com.elisham.coshop;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HomePageActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseUser currentUser;
    private boolean isLocation = true;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Boolean clickOnStar = false;
    private List<OrderData> orderDataList = new ArrayList<>();
    private List<String> displayedOrderIds;
    private FirebaseFirestore db;
    private LinearLayout ordersContainer;
    private Geocoder geocoder;
    private MenuUtils menuUtils;
    private String userEmail, globalUserType;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_FIRST_TIME = "firstTime";
    private RelativeLayout explanationLayout;
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        displayedOrderIds = new ArrayList<>();
        requestNotificationPermission();
        createNotificationChannel();

        // Set the theme based on the user type
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }
        setContentView(R.layout.activity_home_page);

        ImageButton locationButton = findViewById(R.id.locationButton);

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton searchButton = findViewById(R.id.searchButton);
                ImageButton filterOffButton = findViewById(R.id.filterOffButton);
                ImageButton starButton = findViewById(R.id.starButton);
                FrameLayout mapContainer = findViewById(R.id.mapContainer);

                if (isLocation) {
                    locationButton.setImageResource(R.drawable.ic_location_off);
                    // הסתרת רשימת ההזמנות והצגת המפה
                    ordersContainer.setVisibility(View.GONE);
                    mapContainer.setVisibility(View.VISIBLE);

                    // Hide other icons
                    searchButton.setVisibility(View.GONE);
                    filterOffButton.setVisibility(View.GONE);
                    starButton.setVisibility(View.GONE);
                } else {
                    locationButton.setImageResource(R.drawable.location);
                    // הצגת רשימת ההזמנות והסתרת המפה
                    ordersContainer.setVisibility(View.VISIBLE);
                    mapContainer.setVisibility(View.GONE);

                    // Show other icons
                    searchButton.setVisibility(View.VISIBLE);
                    searchButton.setVisibility(View.VISIBLE);
                    starButton.setVisibility(View.VISIBLE);
                }
                isLocation = !isLocation;
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("You")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        }
                    });

            long currentTime = System.currentTimeMillis();
            db.collection("orders").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        Timestamp timestamp = documentSnapshot.getTimestamp("time");
                        long timeRemaining = timestamp.toDate().getTime() - currentTime;
                        String orderOwner = documentSnapshot.getString("ownerEmail");
                        List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                        if (orderOwner != null && orderOwner.equals(userEmail)) continue; // Skip orders that I created
                        if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // Skip orders that I joined
                        if (timeRemaining <= 0) continue; // Skip orders that have expired

                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        String categorie = documentSnapshot.getString("categorie");
                        if (geoPoint != null && categorie != null) {
                            LatLng orderLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                            String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                            String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";

                            Glide.with(this)
                                    .asBitmap()
                                    .load(iconUrl)
                                    .into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            int markerSize = 150;
                                            int innerCircleSize = 80;
                                            int borderSize = 10;
                                            int radius = (int) ((innerCircleSize / 2f) + borderSize);
                                            Bitmap baseMarker = Bitmap.createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888);
                                            Canvas canvas = new Canvas(baseMarker);

                                            Paint borderPaint = new Paint();
                                            borderPaint.setColor(Color.RED);
                                            borderPaint.setStyle(Paint.Style.FILL);
                                            canvas.drawCircle(markerSize / 2f, markerSize / 3f, radius, borderPaint);

                                            Paint whiteCirclePaint = new Paint();
                                            whiteCirclePaint.setColor(Color.WHITE);
                                            whiteCirclePaint.setStyle(Paint.Style.FILL);
                                            canvas.drawCircle(markerSize / 2f, markerSize / 3f, innerCircleSize / 2f, whiteCirclePaint);

                                            Paint trianglePaint = new Paint();
                                            trianglePaint.setColor(Color.RED);
                                            trianglePaint.setStyle(Paint.Style.FILL);
                                            Path path = new Path();
                                            path.moveTo(markerSize / 2f, markerSize);
                                            path.lineTo((markerSize / 2f) - radius, markerSize / 3f);
                                            path.lineTo((markerSize / 2f) + radius, markerSize / 3f);
                                            path.close();
                                            canvas.drawPath(path, trianglePaint);

                                            Bitmap scaledResource = Bitmap.createScaledBitmap(resource, innerCircleSize, innerCircleSize, false);
                                            canvas.drawBitmap(scaledResource, (markerSize - innerCircleSize) / 2f, (markerSize / 3f) - (innerCircleSize / 2f), null);

                                            mMap.addMarker(new MarkerOptions()
                                                    .position(orderLocation)
                                                    .title(titleOfOrder)
                                                    .icon(BitmapDescriptorFactory.fromBitmap(baseMarker))
                                                    .snippet(documentSnapshot.getId()));
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                                    });
                        }
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        mMap.setOnMarkerClickListener(marker -> {
            String orderId = marker.getSnippet();
            if (orderId != null) {
                // מציאת ה-OrderData עבור orderId
                OrderData orderData = findOrderDataById(orderId);
                if (orderData != null) {


                    // הצגת פרטי ההזמנה ב-FrameLayout
                    FrameLayout orderDetailContainer = findViewById(R.id.orderDetailContainer);
                    orderDetailContainer.removeAllViews();
                    orderDetailContainer.setVisibility(View.VISIBLE);

                    View orderBackground = findViewById(R.id.translucentBackground);
                    orderBackground.setVisibility(View.VISIBLE);
                    orderBackground.setOnClickListener(v -> {
                        orderBackground.setVisibility(View.GONE);
                        orderDetailContainer.setVisibility(View.GONE);
                    });

                    // Inflate order item layout and add it to the container
                    View orderView = getLayoutInflater().inflate(R.layout.order_item, orderDetailContainer, false);

                    orderDetailContainer.addView(orderView);

                    // Populate the order view with existing method
                    populateOrderDetail(orderView, orderData);
                }
            }
            return true;
        });

        LatLng defaultLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
    }

    // פונקציה מעודכנת להצגת פרטי ההזמנה באמצעות אובייקט OrderData
    private void populateOrderDetail(View orderView, OrderData orderData) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) orderView.getLayoutParams();
        layoutParams.bottomMargin = 0;
        TextView titleTextView = orderView.findViewById(R.id.titleTextView);
        TextView distanceTextView = orderView.findViewById(R.id.distanceTextView);
        TextView peopleTextView = orderView.findViewById(R.id.peopleTextView);
        ImageView leftSquare = orderView.findViewById(R.id.leftSquare);
        TextView categoryTextView = orderView.findViewById(R.id.categoryTextView);
        TextView locationTextView = orderView.findViewById(R.id.locationTextView);
        TextView typeTextView = orderView.findViewById(R.id.typeTextView);
        LinearLayout ratingLayout = orderView.findViewById(R.id.ratingLayout);
        TextView statusTextView = orderView.findViewById(R.id.statusTextView);
        View scoresLayout = orderView.findViewById(R.id.scoresLayout);

        // Set values to the views
        titleTextView.setText(orderData.titleOfOrder);
        distanceTextView.setText(String.format("%.2f km", orderData.distance));
        peopleTextView.setText(orderData.maxPeople == 0 ? orderData.numberOfPeopleInOrder + "/∞" : orderData.numberOfPeopleInOrder + "/" + orderData.maxPeople);
        categoryTextView.setText(orderData.categorie);
        locationTextView.setText(orderData.location);

        // Load the icon from URL
        String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + orderData.categorie + ".png?alt=media";
        Glide.with(this)
                .load(iconUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.other)
                .error(R.drawable.other)
                .into(leftSquare);

        // Calculate and set the timer
        long currentTime = System.currentTimeMillis();
        Date date = orderData.timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateTimerTextViews(orderView, millisUntilFinished);
                }

                public void onFinish() {
                    updateTimerTextViews(orderView, 0);
                }
            }.start();
        } else {
            updateTimerTextViews(orderView, 0);
        }

        // Check if the order is open or closed
        boolean isOrderOpen = orderData.timestamp.toDate().getTime() > currentTime;
        statusTextView.setText(isOrderOpen ? "Open" : "Closed");
        statusTextView.setTextColor(isOrderOpen ? Color.GREEN : Color.RED);

        // Set the star rating
        addStarsToLayout(ratingLayout, orderData.averageRating);

        // קבלת סוג ההזמנה והגדרת הצבעים בהתאם
        typeTextView.setText(orderData.typeOfOrder);
        if ("Consumer".equals(orderData.typeOfOrder)) {
            orderView.setBackgroundResource(R.drawable.border_green);
            typeTextView.setTextColor(getResources().getColor(R.color.consumerPrimary));
        } else if ("Supplier".equals(orderData.typeOfOrder)) {
            orderView.setBackgroundResource(R.drawable.border_blue);
            typeTextView.setTextColor(getResources().getColor(R.color.supplierPrimary));
        }

        // If clickOnStar is true, add dynamic scores
        if (clickOnStar) {
            addScoresToLayout(scoresLayout, orderData.orderId);
        }

        // הוספת קוד להוספת OnClickListener ל-orderView כדי לפתוח את OrderDetailsActivity
        orderView.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, OrderDetailsActivity.class);
            intent.putExtra("userType", globalUserType);
            intent.putExtra("orderId", orderData.orderId);
            startActivity(intent);
        });
    }

    private void populateOrderDetail(View orderView, String orderId, String titleOfOrder, String location,
                                     long numberOfPeopleInOrder, long maxPeople, String categorie,
                                     double distance, Timestamp timestamp, double averageRating) {
        TextView titleTextView = orderView.findViewById(R.id.titleTextView);
        TextView distanceTextView = orderView.findViewById(R.id.distanceTextView);
        TextView peopleTextView = orderView.findViewById(R.id.peopleTextView);
        ImageView leftSquare = orderView.findViewById(R.id.leftSquare);
        TextView categoryTextView = orderView.findViewById(R.id.categoryTextView);
        TextView locationTextView = orderView.findViewById(R.id.locationTextView);
        TextView typeTextView = orderView.findViewById(R.id.typeTextView);
        LinearLayout ratingLayout = orderView.findViewById(R.id.ratingLayout);
        TextView statusTextView = orderView.findViewById(R.id.statusTextView);
        View scoresLayout = orderView.findViewById(R.id.scoresLayout);

        // Set values to the views
        titleTextView.setText(titleOfOrder);
        distanceTextView.setText(String.format("%.2f km", distance));
        peopleTextView.setText(maxPeople == 0 ? numberOfPeopleInOrder + "/∞" : numberOfPeopleInOrder + "/" + maxPeople);
        categoryTextView.setText(categorie);
        locationTextView.setText(location);

        // Load the icon from URL
        String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
        Glide.with(this)
                .load(iconUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.other)
                .error(R.drawable.other)
                .into(leftSquare);

        // Calculate and set the timer
        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateTimerTextViews(orderView, millisUntilFinished);
                }

                public void onFinish() {
                    updateTimerTextViews(orderView, 0);
                }
            }.start();
        } else {
            updateTimerTextViews(orderView, 0);
        }

        // Check if the order is open or closed
        boolean isOrderOpen = timestamp.toDate().getTime() > currentTime;
        statusTextView.setText(isOrderOpen ? "Open" : "Closed");
        statusTextView.setTextColor(isOrderOpen ? Color.GREEN : Color.RED);

        // Set the star rating
        addStarsToLayout(ratingLayout, averageRating);

        //get type from db
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            String typeOfOrder = documentSnapshot.getString("type_of_order");
            if (typeOfOrder != null) {
                typeTextView.setText(typeOfOrder);
                if (typeOfOrder.equals("Consumer")) {
                    orderView.setBackgroundResource(R.drawable.border_green);
                    typeTextView.setTextColor(getResources().getColor(R.color.consumerPrimary));
                } else if (typeOfOrder.equals("Supplier")) {
                    orderView.setBackgroundResource(R.drawable.border_blue);
                    typeTextView.setTextColor(getResources().getColor(R.color.supplierPrimary));
                }
            }
        });

        // If clickOnStar is true, add dynamic scores
        if (clickOnStar) {
            addScoresToLayout(scoresLayout, orderId);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // קריאה לשיטה הבסיסית

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    // Shows explanations if needed
    private void showExplanationsIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstTime = prefs.getBoolean(KEY_FIRST_TIME, true);

        if (firstTime) {
            explanationLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.explanation_layout, null);
            addContentView(explanationLayout, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

            explanationLayout.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showNextExplanationStep();
                    return true;
                }
                return false;
            });

            Button btnDismiss = explanationLayout.findViewById(R.id.btnDismiss);
            btnDismiss.setOnClickListener(v -> {
                // Dismiss the explanations
                explanationLayout.setVisibility(View.GONE);
                // Update the shared preferences to mark that the user has seen the explanations
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_FIRST_TIME, false);
                editor.apply();
            });

            // Show the first explanation step
            showNextExplanationStep();
        }
    }

    // Shows the next explanation step
    private void showNextExplanationStep() {
        ImageView arrowToPlus = explanationLayout.findViewById(R.id.arrow_to_plus);
        TextView textPlus = explanationLayout.findViewById(R.id.text_plus);
        ImageView arrowToMenu = explanationLayout.findViewById(R.id.arrow_to_menu);
        TextView textMenu = explanationLayout.findViewById(R.id.text_menu);
        ImageView arrowToFilter = explanationLayout.findViewById(R.id.arrow_to_filter);
        TextView textFilter = explanationLayout.findViewById(R.id.text_filter);
        Button btnDismiss = explanationLayout.findViewById(R.id.btnDismiss);
        TextView textEnjoy = explanationLayout.findViewById(R.id.text_enjoy);

        // Hide all elements initially
        arrowToPlus.setVisibility(View.GONE);
        textPlus.setVisibility(View.GONE);
        arrowToMenu.setVisibility(View.GONE);
        textMenu.setVisibility(View.GONE);
        arrowToFilter.setVisibility(View.GONE);
        textFilter.setVisibility(View.GONE);
        btnDismiss.setVisibility(View.GONE);
        textEnjoy.setVisibility(View.GONE);

        // Show elements based on the current step
        switch (currentStep) {
            case 0:
                currentStep++;
                arrowToPlus.setVisibility(View.VISIBLE);
                textPlus.setVisibility(View.VISIBLE);
                break;
            case 1:
                currentStep++;
                arrowToMenu.setVisibility(View.VISIBLE);
                textMenu.setVisibility(View.VISIBLE);
                break;
            case 2:
                currentStep++;
                arrowToFilter.setVisibility(View.VISIBLE);
                textFilter.setVisibility(View.VISIBLE);
                break;
            case 3:
                textEnjoy.setVisibility(View.VISIBLE);
                btnDismiss.setVisibility(View.VISIBLE);
                break;
        }
    }

    // Initializes the UI components
    private void initializeUI() {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        }
        // Initialize the orders container
        ordersContainer = findViewById(R.id.ordersContainer);
        // Clear the orders container at the start
        if (ordersContainer != null) {
            ordersContainer.removeAllViews();
        }

        ImageButton plus = findViewById(R.id.newOrderButton);
        if (globalUserType.equals("Supplier")) {
            plus.setImageResource(R.drawable.ic_plus_supplier);
        }

        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.ENGLISH);
        menuUtils = new MenuUtils(this, globalUserType);
        Intent intent = getIntent();
        // Retrieve filtered orders from Intent
        String filteredOrders = intent.getStringExtra("filteredOrders");
        String filteredOrderIds = intent.getStringExtra("filteredOrderIds");
        boolean noOrdersFound = intent.getBooleanExtra("noOrdersFound", false);
        AtomicBoolean filterActive = new AtomicBoolean(intent.getBooleanExtra("filterActive", false));

        if (noOrdersFound) {
            showNoOrdersFoundMessage();
        } else if (filteredOrders != null && !filteredOrders.isEmpty()) {
            showFilteredOrders(filteredOrders);
        } else if (filteredOrderIds != null && !filteredOrderIds.isEmpty()) {
            getUserLocationAndShowFilteredOrderIds(filteredOrderIds);
        } else {
            // Fetch and display all orders as usual if no filter is applied
            getUserEmailAndFetchOrders();
        }

        // Show or hide filter off button based on filter state
        ImageButton filterButton = findViewById(R.id.searchButton);
        ImageButton filterOffButton = findViewById(R.id.filterOffButton);
        if (filterActive.get()) {
            filterButton.setVisibility(View.GONE);
            filterOffButton.setVisibility(View.VISIBLE);
        } else {
            filterButton.setVisibility(View.VISIBLE);
            filterOffButton.setVisibility(View.GONE);
        }

        // Set up the filter off button
        filterOffButton.setOnClickListener(v -> {
            filterOffButton.setVisibility(View.GONE);
            filterButton.setVisibility(View.VISIBLE);
            // Fetch and display all orders without reloading the activity
            getUserEmailAndFetchOrders();
        });

        // Set up the star button toggle
        ImageButton starButton = findViewById(R.id.starButton);
        starButton.setOnClickListener(v -> {
            if (starButton.getTag().equals("star")) {
                starButton.setImageResource(R.drawable.star);
                starButton.setTag("star2");
                clickOnStar = true;
                // Get the user location from Firebase
                db.collection("users").document(userEmail)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                GeoPoint userLocation = task.getResult().getGeoPoint("address");

                                if (userLocation != null) {
                                    // Call the new function with the user's location
                                    calculateDisplayedOrdersRecommendationRatings(currentUser, userLocation);
                                } else {
                                    Log.d(this.getLocalClassName(), "User location not found");
                                }
                            } else {
                                Log.d(this.getLocalClassName(), "Failed to get user location");
                            }
                        });
            } else {
                starButton.setImageResource(R.drawable.star2);
                clickOnStar = false;
                starButton.setTag("star");
                getUserEmailAndFetchOrders();
            }
        });

        TextView filterBarText1 = findViewById(R.id.filterBarText1);

        if (userEmail != null) {
            db.collection("users").document(userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String firstName = task.getResult().getString("first name");
                            String familyName = task.getResult().getString("family name");
                            if (firstName != null && familyName != null) {
                                String greeting = "Hi " + firstName + " " + familyName;
                                SpannableString spannableString = new SpannableString(greeting);

                                // Set the style to bold
                                spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, greeting.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                // Set the color to black
                                spannableString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, greeting.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                filterBarText1.setText(spannableString);
                            }
                        }
                    });
        }
        // Show explanations if it's the user's first time
        showExplanationsIfNeeded();
    }

    // Gets user location and shows filtered order IDs
    private void getUserLocationAndShowFilteredOrderIds(String filteredOrderIds) {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Log.d(this.getLocalClassName(), "User not logged in");
            return;
        }

        db.collection("users").document(userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        GeoPoint userLocation = task.getResult().getGeoPoint("address");
                        if (userLocation != null) {
                            showFilteredOrderIds(filteredOrderIds, userLocation);
                        }
                    }
                });
    }

    // Shows filtered order IDs
    private void showFilteredOrderIds(String filteredOrderIds, GeoPoint userLocation) {
        // Clear any existing orders
        ordersContainer.removeAllViews();

        String[] orderIds = filteredOrderIds.split("\n");
        for (String orderId : orderIds) {
            db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult();

                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                    String location = orderLocation != null ? getAddressFromLatLng(orderLocation.getLatitude(), orderLocation.getLongitude()) : "N/A";
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");
                    String categorie = documentSnapshot.getString("categorie");
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");

                    float distance = calculateDistance(userLocation, orderLocation);
                    Log.d("HomePageActivity", "Calculated distance: " + distance);

                    calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            });
        }
    }

    // Shows filtered order IDs without user location
    private void showFilteredOrderIds(String filteredOrderIds) {
        // Clear any existing orders
        ordersContainer.removeAllViews();

        String[] orderIds = filteredOrderIds.split("\n");
        for (String orderId : orderIds) {
            db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult();

                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                    String location = orderLocation != null ? getAddressFromLatLng(orderLocation.getLatitude(), orderLocation.getLongitude()) : "N/A";
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");
                    String categorie = documentSnapshot.getString("categorie");
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");

                    float distance = calculateDistance(getUserLocation(), orderLocation);
                    Log.d("HomePageActivity", "Calculated distance: " + distance);
                    Log.d("HomePageActivity", "User Location: " + getUserLocation());
                    Log.d("HomePageActivity", "Order Location: " + orderLocation);

                    calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            });
        }
    }

    // Fetches all orders
    private void fetchAllOrders(GeoPoint userLocation) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                if (currentUser != null) {
                    userEmail = currentUser.getEmail();
                }

                long currentTime = System.currentTimeMillis();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    //todo
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    long timeRemaining = timestamp.toDate().getTime() - currentTime;

                    if (timeRemaining <= 0) continue; // Skip orders that have expired

                    String orderOwner = documentSnapshot.getString("ownerEmail");
                    List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                    if (orderOwner != null && orderOwner.equals(userEmail)) continue; // Skip orders that I created
                    if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // Skip orders that I joined

                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    String orderId = documentSnapshot.getId();
                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                    String location = orderLocation != null ? getAddressFromLatLng(orderLocation.getLatitude(), orderLocation.getLongitude()) : "N/A";
                    String categorie = documentSnapshot.getString("categorie");

                    float distance = calculateDistance(userLocation, orderLocation);
                    calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            }
        });
    }

    class OrderData {
        String orderId;
        String titleOfOrder;
        String location;
        String typeOfOrder;
        long numberOfPeopleInOrder;
        long maxPeople;
        String categorie;
        double distance;
        Timestamp timestamp;
        double averageRating;
        double totalScore;
        double categoryScore;
        double ratingScore;
        double distanceScore;

        public OrderData(String orderId, String titleOfOrder, String location, String typeOfOrder,
                         long numberOfPeopleInOrder, long maxPeople, String categorie,
                         double distance, Timestamp timestamp, double averageRating) {
            this.orderId = orderId;
            this.titleOfOrder = titleOfOrder;
            this.location = location;
            this.typeOfOrder = typeOfOrder;
            this.numberOfPeopleInOrder = numberOfPeopleInOrder;
            this.maxPeople = maxPeople;
            this.categorie = categorie;
            this.distance = distance;
            this.timestamp = timestamp;
            this.averageRating = averageRating;
        }
    }

    // Calculates and displays ratings
    private void calculateAndDisplayRatings(String orderId, String titleOfOrder, String location,
                                            long numberOfPeopleInOrder, long maxPeople, String categorie,
                                            float distance, Timestamp timestamp) {
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            List<String> peopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
            if (peopleInOrder == null || peopleInOrder.isEmpty()) {
                addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople,
                        categorie, distance, timestamp, 0.0);
                return;
            }

            db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                List<CompletableFuture<Double>> ratingFutures = queryDocumentSnapshots.getDocuments().stream()
                        .filter(userDoc -> peopleInOrder.contains(userDoc.getId()))
                        .map(userDoc -> CompletableFuture.supplyAsync(() -> calculateUserRating(userDoc)))
                        .collect(Collectors.toList());

                CompletableFuture<Void> allOf = CompletableFuture.allOf(ratingFutures.toArray(new CompletableFuture[0]));
                CompletableFuture<List<Double>> allRatingsFuture = allOf.thenApply(v ->
                        ratingFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));

                try {
                    List<Double> allRatings = allRatingsFuture.get();
                    double totalRating = allRatings.stream().mapToDouble(Double::doubleValue).sum();
                    double averageRating = totalRating / peopleInOrder.size();

                    // Round the rating
                    averageRating = Math.round(averageRating * 2) / 2.0;

                    updateRatingOrderInFirestore(orderId, averageRating);

                    addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople,
                            categorie, distance, timestamp, averageRating);

                    String typeOfOrder = documentSnapshot.getString("type_of_order");
                    orderDataList.add(new OrderData(orderId, titleOfOrder, location, typeOfOrder, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp, averageRating));

                    // Add order ID to the displayed order list
                    displayedOrderIds.add(orderId);

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    // Calculates displayed orders recommendation ratings
    private void calculateDisplayedOrdersRecommendationRatings(FirebaseUser currentUser, GeoPoint userLocation) {
        ordersContainer.removeAllViews();
        getUserPreferredCategories(currentUser, userPreferredCategories -> {
            db.collection("orders").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    double minDistance = Double.MAX_VALUE;
                    double maxDistance = Double.MIN_VALUE;
                    List<DocumentSnapshot> orders = new ArrayList<>();
                    List<Float> distances = new ArrayList<>();

                    for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                        String orderId = documentSnapshot.getId();
                        if (!displayedOrderIds.contains(orderId)) continue;

                        GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                        if (orderLocation != null) {
                            float distance = calculateDistance(userLocation, orderLocation);
                            distances.add(distance);
                            if (distance < minDistance) {
                                minDistance = distance;
                            }
                            if (distance > maxDistance) {
                                maxDistance = distance;
                            }
                        }
                        orders.add(documentSnapshot);
                    }

                    List<OrderData> sortedOrderDataList = new ArrayList<>();

                    for (int i = 0; i < orders.size(); i++) {
                        DocumentSnapshot documentSnapshot = orders.get(i);
                        float distance = distances.get(i);

                        String orderId = documentSnapshot.getId();
                        String categorie = documentSnapshot.getString("categorie");
                        GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                        Double rating = documentSnapshot.getDouble("ratingOrder");

                        double categoryScore = calculateCategoryScore(categorie, userPreferredCategories);
                        double ratingScore = calculateRatingScore(rating);
                        Log.d("RecommendationRating", "Order distance: " + distance);

                        float distanceScore = calculateDistanceScore(distance, minDistance, maxDistance);

                        double totalScore = categoryScore + ratingScore + distanceScore;

                        Log.d("RecommendationRating", "Order ID: " + orderId);
                        Log.d("RecommendationRating", "Category Score: " + categoryScore);
                        Log.d("RecommendationRating", "Rating Score: " + ratingScore);
                        Log.d("RecommendationRating", "Distance Score: " + distanceScore);
                        Log.d("RecommendationRating", "Total Score: " + totalScore);

                        OrderData orderData = findOrderDataById(orderId);
                        if (orderData != null) {
                            orderData.totalScore = totalScore;
                            orderData.categoryScore = categoryScore;
                            orderData.ratingScore = ratingScore;
                            orderData.distanceScore = distanceScore;
                            sortedOrderDataList.add(orderData);
                        }
                    }

                    sortedOrderDataList.sort((o1, o2) -> Double.compare(o2.totalScore, o1.totalScore));

                    ordersContainer.removeAllViews();

                    for (OrderData orderData : sortedOrderDataList) {
                        addOrderToLayout(orderData.orderId, orderData.titleOfOrder, orderData.location, orderData.numberOfPeopleInOrder, orderData.maxPeople, orderData.categorie, orderData.distance, orderData.timestamp, orderData.averageRating);
                    }
                }
            });
        });
    }

    // Finds OrderData by order ID
    private OrderData findOrderDataById(String orderId) {
        for (OrderData orderData : orderDataList) {
            if (orderData.orderId.equals(orderId)) {
                return orderData;
            }
        }
        return null;
    }

    // Updates rating order in Firestore
    private void updateRatingOrderInFirestore(String orderId, double averageRating) {
        db.collection("orders").document(orderId)
                .update("ratingOrder", averageRating)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Rating order successfully updated"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error updating rating order", e));
    }

    // Calculates user rating
    private Double calculateUserRating(DocumentSnapshot userDoc) {
        List<Map<String, Object>> userRatings = (List<Map<String, Object>>) userDoc.get("ratings");
        if (userRatings == null || userRatings.isEmpty()) {
            return 0.0;
        }
        double totalRating = 0.0;
        for (Map<String, Object> rating : userRatings) {
            Object ratingValue = rating.get("rating");
            if (ratingValue instanceof Double) {
                totalRating += (Double) ratingValue;
            } else if (ratingValue instanceof Long) {
                totalRating += ((Long) ratingValue).doubleValue();
            }
        }
        return totalRating / userRatings.size();
    }

    // Shows filtered orders
    private void showFilteredOrders(String filteredOrders) {
        ordersContainer.removeAllViews();

        String[] orders = filteredOrders.split("\n");
        for (String orderInfo : orders) {
            String[] orderDetails = orderInfo.split(";");
            if (orderDetails.length == 8) {
                String orderId = orderDetails[0];
                String titleOfOrder = orderDetails[1];
                String[] locationParts = orderDetails[2].split(",");
                double lat = Double.parseDouble(locationParts[0]);
                double lon = Double.parseDouble(locationParts[1]);
                String location = getAddressFromLatLng(lat, lon);
                long numberOfPeopleInOrder = Long.parseLong(orderDetails[3]);
                long maxPeople = Long.parseLong(orderDetails[4]);
                String categorie = orderDetails[5];
                double distance = Double.parseDouble(orderDetails[6]);
                Timestamp timestamp = new Timestamp(new Date(Long.parseLong(orderDetails[7]) * 1000));
                calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, (float) distance, timestamp);
            }
        }
    }

    // Shows no orders found message
    private void showNoOrdersFoundMessage() {
        ordersContainer.removeAllViews();
        TextView noOrdersTextView = new TextView(this);
        noOrdersTextView.setText("No orders found");
        noOrdersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        noOrdersTextView.setTextColor(Color.RED);
        ordersContainer.addView(noOrdersTextView);
    }

    // Calculates distance between two GeoPoints
    private float calculateDistance(GeoPoint userLocation, GeoPoint orderLocation) {
        if (userLocation == null || orderLocation == null) return 0;

        float[] results = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                orderLocation.getLatitude(), orderLocation.getLongitude(), results);
        return Math.abs(results[0] / 1000); // Distance in kilometers
    }

    // Gets the user location
    private GeoPoint getUserLocation() {
        // Add code to get the current user's location from Firebase
        // For example, you can return a default location if the user's location is not defined
        return new GeoPoint(0, 0); // Replace with actual code
    }

    // Gets user email and fetches orders
    private void getUserEmailAndFetchOrders() {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Log.d(this.getLocalClassName(), "User not logged in");
            return;
        }
        // Clear any existing orders
        ordersContainer.removeAllViews();

        db.collection("users").document(userEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        GeoPoint userLocation = task.getResult().getGeoPoint("address");
                        if (userLocation != null) {
                            fetchAllOrders(userLocation);
                        }
                    }
                });
    }

    // Gets address from latitude and longitude
    private String getAddressFromLatLng(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    // Adds order to layout
    private void addOrderToLayout(String orderId, String titleOfOrder, String location,
                                  long numberOfPeopleInOrder, long maxPeople, String categorie,
                                  double distance, Timestamp timestamp, double averageRating) {
        View orderLayout = getLayoutInflater().inflate(R.layout.order_item, ordersContainer, false);
        if (globalUserType.equals("Supplier")) {
            orderLayout.setBackgroundResource(R.drawable.border_blue);
        } else {
            orderLayout.setBackgroundResource(R.drawable.border_green);
        }
        // Set onClickListener to open order details
        orderLayout.setOnClickListener(v -> {
            Log.d("MyOrdersActivity", "Order clicked: " + orderId);
            Intent intent = new Intent(HomePageActivity.this, OrderDetailsActivity.class);
            intent.putExtra("userType", globalUserType);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });
        TextView titleTextView = orderLayout.findViewById(R.id.titleTextView);
        TextView distanceTextView = orderLayout.findViewById(R.id.distanceTextView);
        TextView peopleTextView = orderLayout.findViewById(R.id.peopleTextView);
        ImageView leftSquare = orderLayout.findViewById(R.id.leftSquare);
        TextView categoryTextView = orderLayout.findViewById(R.id.categoryTextView);
        TextView locationTextView = orderLayout.findViewById(R.id.locationTextView);
        TextView typeTextView = orderLayout.findViewById(R.id.typeTextView);
        LinearLayout ratingLayout = orderLayout.findViewById(R.id.ratingLayout);
        TextView statusTextView = orderLayout.findViewById(R.id.statusTextView);
        View scoresLayout = orderLayout.findViewById(R.id.scoresLayout);

        // Find the timer layout inside the inflated order layout
        View timerView = orderLayout.findViewById(R.id.timerView);

        // Set values to the views
        titleTextView.setText(titleOfOrder);
        distanceTextView.setText(String.format("%.2f km", distance));
        if (maxPeople == 0) {
            peopleTextView.setText(numberOfPeopleInOrder + "/∞");
        } else {
            peopleTextView.setText(numberOfPeopleInOrder + "/" + maxPeople);
        }
        categoryTextView.setText(categorie);
        locationTextView.setText(location);

        // Load the icon from URL
        String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
        Glide.with(this)
                .load(iconUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .placeholder(R.drawable.other) // Optional: Add a placeholder image
                .error(R.drawable.other) // Optional: Add an error image
                .into(leftSquare);

        // Calculate and set the timer
        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateTimerTextViews(timerView, millisUntilFinished);
                }

                public void onFinish() {
                    updateTimerTextViews(timerView, 0);
                }
            }.start();
        } else {
            updateTimerTextViews(timerView, 0);
        }

        // Check if the order is open or closed using existing variables
        boolean isOrderOpen = timestamp.toDate().getTime() > currentTime;
        statusTextView.setText(isOrderOpen ? "Open" : "Closed");
        statusTextView.setTextColor(isOrderOpen ? Color.GREEN : Color.RED);

        // Set the star rating
        addStarsToLayout(ratingLayout, averageRating);

        //get type from db
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            String typeOfOrder = documentSnapshot.getString("type_of_order");
            if (typeOfOrder != null) {
                if (typeOfOrder.equals("Consumer")) {
                    orderLayout.setBackgroundResource(R.drawable.border_green);
                    typeTextView.setTextColor(getResources().getColor(R.color.consumerPrimary));
                } else if (typeOfOrder.equals("Supplier")) {
                    orderLayout.setBackgroundResource(R.drawable.border_blue);
                    typeTextView.setTextColor(getResources().getColor(R.color.supplierPrimary));
                }
                typeTextView.setText(typeOfOrder);
            }
        });

        // Add dynamic scores if clickOnStar is true
        if (clickOnStar) {
            addScoresToLayout(scoresLayout, orderId);
        }

        // Add the order layout to the container
        ordersContainer.addView(orderLayout);
    }

    // Adds scores to layout
    private void addScoresToLayout(View scoresLayout, String orderId) {
        // Set scores layout visibility to visible
        scoresLayout.setVisibility(View.VISIBLE);

        // Find the score views inside the included layout
        TextView totalScoreTextView = scoresLayout.findViewById(R.id.totalScoreTextView);
        ImageView categoryIcon = scoresLayout.findViewById(R.id.categoryIcon);
        TextView categoryScoreTextView = scoresLayout.findViewById(R.id.categoryScoreTextView);
        ImageView locationIcon = scoresLayout.findViewById(R.id.locationIcon);
        TextView distanceScoreTextView = scoresLayout.findViewById(R.id.distanceScoreTextView);
        ImageView ratingIcon = scoresLayout.findViewById(R.id.ratingIcon);
        TextView ratingScoreTextView = scoresLayout.findViewById(R.id.ratingScoreTextView);

        // Find the OrderData for the given orderId
        OrderData orderData = findOrderDataById(orderId);
        if (orderData != null) {
            // Update the score views with the appropriate data
            totalScoreTextView.setText(String.format("Matches: %s", formatScore(orderData.totalScore)));
            categoryScoreTextView.setText(formatScore(orderData.categoryScore));
            distanceScoreTextView.setText(formatScore(orderData.distanceScore));
            ratingScoreTextView.setText(formatScore(orderData.ratingScore));
        }
    }

    // Formats score
    private String formatScore(double score) {
        // Round the number to two decimal places
        score = Math.round(score * 10) / 10.0;

        // Check if the number is an integer
        if (score % 1 == 0) {
            return String.format("%d%%", (int) score);
        } else {
            return String.format("%.1f%%", score);
        }
    }

    // Updates timer text views
    private void updateTimerTextViews(View timerView, long millisUntilFinished) {
        TextView daysTextView = timerView.findViewById(R.id.daysTextView);
        TextView hoursTextView = timerView.findViewById(R.id.hoursTextView);
        TextView minutesTextView = timerView.findViewById(R.id.minutesTextView);
        TextView secondsTextView = timerView.findViewById(R.id.secondsTextView);
        LinearLayout daysContainer = timerView.findViewById(R.id.daysContainer);
        TextView colon1 = timerView.findViewById(R.id.colon1);
        LinearLayout secondsContainer = timerView.findViewById(R.id.secondsContainer);
        TextView colon3 = timerView.findViewById(R.id.colon3);

        long seconds = millisUntilFinished / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds = seconds % 60;
        minutes = minutes % 60;
        hours = hours % 24;

        if (days > 0) {
            daysContainer.setVisibility(View.VISIBLE);
            colon1.setVisibility(View.VISIBLE);
            secondsContainer.setVisibility(View.GONE);
            colon3.setVisibility(View.GONE);

            daysTextView.setText(String.format(Locale.getDefault(), "%02d", days));
            hoursTextView.setText(String.format(Locale.getDefault(), "%02d", hours));
            minutesTextView.setText(String.format(Locale.getDefault(), "%02d", minutes));
        } else {
            daysContainer.setVisibility(View.GONE);
            colon1.setVisibility(View.GONE);
            secondsContainer.setVisibility(View.VISIBLE);
            colon3.setVisibility(View.VISIBLE);

            hoursTextView.setText(String.format(Locale.getDefault(), "%02d", hours));
            minutesTextView.setText(String.format(Locale.getDefault(), "%02d", minutes));
            secondsTextView.setText(String.format(Locale.getDefault(), "%02d", seconds));
        }
    }

    // Converts dp to pixels
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
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
                initializeUI();
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

    // Navigates to the filter activity
    public void gotofilter(View v) {
        Intent toy = new Intent(HomePageActivity.this, FilterActivity.class);
        toy.putExtra("userType", globalUserType);
        startActivity(toy);
    }

    // Navigates to the new order activity
    public void gotoneworder(View v) {
        Intent toy = new Intent(HomePageActivity.this, OpenNewOrderActivity.class);
        toy.putExtra("userType", globalUserType);
        startActivity(toy);
    }

    // Calculates category score
    private double calculateCategoryScore(String orderCategory, List<String> userPreferredCategories) {
        if (userPreferredCategories != null && userPreferredCategories.contains(orderCategory)) {
            return (100 / 3);
        }
        return 0;
    }

    // Gets user preferred categories
    private void getUserPreferredCategories(FirebaseUser currentUser, OnCategoriesRetrievedListener listener) {
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            db.collection("users").document(userEmail).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    List<String> userPreferredCategories = (List<String>) task.getResult().get("favorite categories");
                    listener.onCategoriesRetrieved(userPreferredCategories);
                } else {
                    listener.onCategoriesRetrieved(new ArrayList<>());
                }
            });
        } else {
            listener.onCategoriesRetrieved(new ArrayList<>());
        }
    }

    private interface OnCategoriesRetrievedListener {
        void onCategoriesRetrieved(List<String> categories);
    }

    // Calculates rating score
    private double calculateRatingScore(Double rating) {
        if (rating == null) {
            return 0;
        }
        return Math.round((rating / 5.0) * (100 / 3) * 2) / 2.0;
    }

    // Calculates distance score
    private float calculateDistanceScore(float distanceInKm, double minDistance, double maxDistance) {
        distanceInKm = Math.round(distanceInKm * 100) / 100.0f;
        Log.d("RecommendationRating", "Distance: " + distanceInKm);

        if (distanceInKm <= 20) {
            return (float) ((100.0 / 3.0) - (((100.0 / 3.0) / 20.0) * distanceInKm));
        } else {
            return 0;
        }
    }

    // Requests notification permission
    private void requestNotificationPermission() {
        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d("Permission", "Notification permission granted");
            } else {
                Log.d(this.getLocalClassName(), "Notification permission is required for this app");
            }
        });

        // Check and request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // Creates notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "example_channel";
            String description = "Channel for example notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("example_channel_id", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Adds stars to layout
    private void addStarsToLayout(LinearLayout layout, double rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = rating - fullStars > 0;
        int emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

        int starSize = dpToPx(16); // Adjust the size of the stars here

        for (int i = 0; i < fullStars; i++) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(starSize, starSize);
            star.setLayoutParams(params);
            layout.addView(star);
        }

        if (hasHalfStar) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star_half_yellow_icon);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(starSize, starSize);
            star.setLayoutParams(params);
            layout.addView(star);
        }

        for (int i = 0; i < emptyStars; i++) {
            ImageView star = new ImageView(this);
            star.setImageResource(R.drawable.star_line_yellow_icon);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(starSize, starSize);
            star.setLayoutParams(params);
            layout.addView(star);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeUI();
    }
}
