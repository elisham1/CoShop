package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomePageActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout ordersContainer;
    private Geocoder geocoder;
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize the orders container
        ordersContainer = findViewById(R.id.ordersContainer);

        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.ENGLISH);

        menuUtils = new MenuUtils(this);

        // Retrieve filtered orders from Intent
        Intent intent = getIntent();
        String filteredOrders = intent.getStringExtra("filteredOrders");
        boolean noOrdersFound = intent.getBooleanExtra("noOrdersFound", false);
        boolean filterActive = intent.getBooleanExtra("filterActive", false);

        if (noOrdersFound) {
            showNoOrdersFoundMessage();
        } else if (filteredOrders != null && !filteredOrders.isEmpty()) {
            showFilteredOrders(filteredOrders);
        } else {
            // Fetch and display all orders as usual if no filter is applied
            getUserEmailAndFetchOrders();
        }

        // הצגת כפתור הסרת הסינון אם יש סינון פעיל
        ImageButton filterOffButton = findViewById(R.id.filterOffButton);
        if (filterActive) {
            filterOffButton.setVisibility(View.VISIBLE);
        } else {
            filterOffButton.setVisibility(View.GONE);
        }

        // Set up the filter off button
        filterOffButton.setOnClickListener(v -> {
            filterOffButton.setVisibility(View.GONE);
            fetchAllOrders2();
        });

        // Set up the star button toggle
        ImageButton starButton = findViewById(R.id.starButton);
        starButton.setOnClickListener(v -> {
            if (starButton.getTag().equals("star")) {
                starButton.setImageResource(R.drawable.star2);
                starButton.setTag("star2");
            } else {
                starButton.setImageResource(R.drawable.star);
                starButton.setTag("star");
            }
        });
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        }

        TextView filterBarText1 = findViewById(R.id.filterBarText1);

        if (userEmail != null) {
            db.collection("users").document(userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String firstName = task.getResult().getString("first name");
                            String familyName = task.getResult().getString("family name");
                            if (firstName != null && familyName != null) {
                                filterBarText1.setText("Hi " + firstName + " " + familyName);
                            }
                        }
                    });
        }

    }

    private void fetchAllOrders(GeoPoint userLocation) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                String userEmail = null;
                if (currentUser != null) {
                    userEmail = currentUser.getEmail();
                }

                long currentTime = System.currentTimeMillis();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    long timeRemaining = timestamp.toDate().getTime() - currentTime;

                    if (timeRemaining <= 0) continue; // דילוג על הזמנות שזמן ההזמנה שלהן נגמר

                    String orderOwner = documentSnapshot.getString("ownerEmail");
                    List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                    if (orderOwner != null && orderOwner.equals(userEmail)) continue; // דילוג על הזמנות שפתחתי
                    if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // דילוג על הזמנות שהצטרפתי אליהן

                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // דילוג על הזמנות מלאות

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


    private void calculateAndDisplayRatings(String orderId, String titleOfOrder, String location,
                                            long numberOfPeopleInOrder, long maxPeople, String categorie,
                                            float distance, Timestamp timestamp){
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            List<Map<String, Object>> peopleInOrder = (List<Map<String, Object>>) documentSnapshot.get("listPeopleInOrder");
            db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                List<Double> allRatings = new ArrayList<>();
                int count = 0;
                double temp_rating = 0.0;
                for (DocumentSnapshot userDoc : queryDocumentSnapshots) {
                    String email = userDoc.getId();
                    if (peopleInOrder.contains(email)) {
                        List<Map<String, Object>> userRatings = (List<Map<String, Object>>) userDoc.get("ratings");
                        if (userRatings != null) {
                            if (!userRatings.isEmpty()) {
                                for (Map<String, Object> rating : userRatings) {
                                    Object ratingValue = rating.get("rating");
                                    if (ratingValue instanceof Double) {
                                        temp_rating = (Double) ratingValue;
                                    } else if (ratingValue instanceof Long) {
                                        temp_rating = ((Long) ratingValue).doubleValue();
                                    }
                                }
                                allRatings.add(temp_rating/userRatings.size());
                            } else {
                                allRatings.add(0.0);
                            }
                            Log.d("Ratings" , count + "");
                        }
                    }

                }
                double totalRating = 0;
                for (double rating : allRatings) {
                    totalRating += rating;
                }
                double averageRating = totalRating / peopleInOrder.size();

                // Add the order to the layout
                addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople,
                        categorie, distance, timestamp, averageRating);
            });
        });
    }



    private void showFilteredOrders(String filteredOrders) {
        // Clear any existing orders
        ordersContainer.removeAllViews();

        // Parse and display the filtered orders
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

    private void showNoOrdersFoundMessage() {
        ordersContainer.removeAllViews();
        TextView noOrdersTextView = new TextView(this);
        noOrdersTextView.setText("No orders found");
        noOrdersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        noOrdersTextView.setTextColor(Color.RED);
        ordersContainer.addView(noOrdersTextView);
    }

    private float calculateDistance(GeoPoint userLocation, GeoPoint orderLocation) {
        if (userLocation == null || orderLocation == null) return 0;

        float[] results = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                orderLocation.getLatitude(), orderLocation.getLongitude(), results);
        return Math.abs(results[0] / 1000); // המרחק בקילומטרים
    }

    private GeoPoint getUserLocation() {
        // להוסיף קוד לקבלת המיקום של המשתמש הנוכחי מהפיירבייס
        // לדוגמא, ניתן להחזיר מיקום דיפולטי אם לא קיים מיקום מוגדר למשתמש
        return new GeoPoint(0, 0); // להחליף בקוד הנכון
    }

    private void getUserEmailAndFetchOrders() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void fetchAllOrders2() {
        ordersContainer.removeAllViews();
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                String userEmail = null;
                if (currentUser != null) {
                    userEmail = currentUser.getEmail();
                }

                long currentTime = System.currentTimeMillis();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    long timeRemaining = timestamp.toDate().getTime() - currentTime;

                    if (timeRemaining <= 0) continue; // דילוג על הזמנות שזמן ההזמנה שלהן נגמר

                    String orderOwner = documentSnapshot.getString("ownerEmail");
                    List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                    if (orderOwner != null && orderOwner.equals(userEmail)) continue; // דילוג על הזמנות שפתחתי
                    if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // דילוג על הזמנות שהצטרפתי אליהן

                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // דילוג על הזמנות מלאות

                    String orderId = documentSnapshot.getId();
                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                    String location = orderLocation != null ? getAddressFromLatLng(orderLocation.getLatitude(), orderLocation.getLongitude()) : "N/A";
                    String categorie = documentSnapshot.getString("categorie");

                    float distance = 0; // No need to calculate distance for fetching all orders
                    calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            }
        });
    }

    private void addOrderToLayout(String orderId, String titleOfOrder,
                                  String location, long numberOfPeopleInOrder,
                                  long maxPeople, String categorie, double distance,
                                  Timestamp timestamp, double averageRating) {
        // Create a new RelativeLayout for the order
        RelativeLayout orderLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams orderLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        orderLayoutParams.setMargins(0, 0, 0, dpToPx(8)); // Add margin between orders
        orderLayout.setLayoutParams(orderLayoutParams);
        orderLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        orderLayout.setBackgroundColor(Color.WHITE); // Set the background color to white

        // Set onClickListener to open order details
        orderLayout.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });

        TextView titleTextView = new TextView(this);
        titleTextView.setText(titleOfOrder);
        titleTextView.setId(View.generateViewId());
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        RelativeLayout.LayoutParams titleTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleTextParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        titleTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        titleTextParams.setMargins(dpToPx(20), dpToPx(10), dpToPx(10), dpToPx(5)); // Add margin from the edge
        titleTextView.setLayoutParams(titleTextParams);
        orderLayout.addView(titleTextView);

        TextView distanceTextView = new TextView(this);
        distanceTextView.setText(String.format("%.2f km", distance));
        distanceTextView.setId(View.generateViewId());
        distanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        RelativeLayout.LayoutParams distanceTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        distanceTextParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        distanceTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        distanceTextParams.setMargins(dpToPx(10), dpToPx(10), dpToPx(20), dpToPx(5)); // Add margin from the edge
        distanceTextView.setLayoutParams(distanceTextParams);
        orderLayout.addView(distanceTextView);

        // Create and add the people count
        TextView peopleTextView = new TextView(this);
        if (maxPeople == 0) {
            peopleTextView.setText("unlimit people");
        } else {
            peopleTextView.setText(numberOfPeopleInOrder + "/" + maxPeople);
        }
        peopleTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams peopleTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        peopleTextParams.addRule(RelativeLayout.CENTER_IN_PARENT); // Center horizontally and vertically
        peopleTextView.setLayoutParams(peopleTextParams);
        peopleTextParams.setMargins(0, dpToPx(10), 0, dpToPx(10)); // Add margin between title and people count
        orderLayout.addView(peopleTextView);

        // Create and add the left square with category
        LinearLayout leftSquareLayout = new LinearLayout(this);
        leftSquareLayout.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams leftSquareLayoutParams = new RelativeLayout.LayoutParams(
                dpToPx(80), ViewGroup.LayoutParams.WRAP_CONTENT); // Size of the left square
        leftSquareLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        leftSquareLayoutParams.addRule(RelativeLayout.BELOW, peopleTextView.getId());
        leftSquareLayoutParams.setMargins(dpToPx(15), dpToPx(10), dpToPx(20), dpToPx(15)); // Add margin from the left edge
        leftSquareLayout.setLayoutParams(leftSquareLayoutParams);

        ImageView leftSquare = new ImageView(this);
        leftSquare.setBackgroundColor(Color.WHITE); // Set the background color to white
        leftSquare.setId(View.generateViewId());
        LinearLayout.LayoutParams leftSquareParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(80));
        leftSquare.setLayoutParams(leftSquareParams);

        leftSquareLayout.addView(leftSquare);

        // Load the icon from URL
        String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
        Glide.with(this)
                .load(iconUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .placeholder(R.drawable.star) // Optional: Add a placeholder image
                .error(R.drawable.star2) // Optional: Add an error image
                .into(leftSquare);

        TextView categoryTextView = new TextView(this);
        categoryTextView.setText(categorie);
        categoryTextView.setId(View.generateViewId());
        categoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        categoryTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        leftSquareLayout.addView(categoryTextView);

        orderLayout.addView(leftSquareLayout);

        // Create and add the right square with timer
        LinearLayout rightSquareContainer = new LinearLayout(this);
        rightSquareContainer.setOrientation(LinearLayout.VERTICAL);
        rightSquareContainer.setGravity(Gravity.CENTER_HORIZONTAL); // Center the container horizontally
        RelativeLayout.LayoutParams rightSquareContainerParams = new RelativeLayout.LayoutParams(
                dpToPx(80), ViewGroup.LayoutParams.WRAP_CONTENT); // Size of the right square container
        rightSquareContainerParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        rightSquareContainerParams.addRule(RelativeLayout.BELOW, peopleTextView.getId());
        rightSquareContainerParams.setMargins(dpToPx(20), dpToPx(10), dpToPx(15), dpToPx(20)); // Add margin from the right edge
        rightSquareContainer.setLayoutParams(rightSquareContainerParams);

        RelativeLayout rightSquareLayout = new RelativeLayout(this);
        LinearLayout.LayoutParams rightSquareLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(80)); // Size of the right square
        rightSquareLayout.setLayoutParams(rightSquareLayoutParams);

        rightSquareContainer.addView(rightSquareLayout);

        ImageView rightSquare = new ImageView(this);
        rightSquare.setBackgroundColor(Color.WHITE); // Set the background color to white
        rightSquare.setId(View.generateViewId());
        RelativeLayout.LayoutParams rightSquareParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rightSquare.setLayoutParams(rightSquareParams);

        rightSquareLayout.addView(rightSquare);

        // Inflate custom timer layout
        View timerView = getLayoutInflater().inflate(R.layout.timer_layout, null);
        RelativeLayout.LayoutParams timerViewParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        timerView.setLayoutParams(timerViewParams);

        // Ensure the timer layout is left-to-right
        ViewCompat.setLayoutDirection(timerView, ViewCompat.LAYOUT_DIRECTION_LTR);

        rightSquareLayout.addView(timerView);

        // Get references to timer text views
        LinearLayout daysContainer = timerView.findViewById(R.id.daysContainer);
        TextView daysTextView = timerView.findViewById(R.id.daysTextView);
        TextView colon1 = timerView.findViewById(R.id.colon1);
        LinearLayout hoursContainer = timerView.findViewById(R.id.hoursContainer);
        TextView hoursTextView = timerView.findViewById(R.id.hoursTextView);
        TextView colon2 = timerView.findViewById(R.id.colon2);
        LinearLayout minutesContainer = timerView.findViewById(R.id.minutesContainer);
        TextView minutesTextView = timerView.findViewById(R.id.minutesTextView);
        TextView colon3 = timerView.findViewById(R.id.colon3);
        LinearLayout secondsContainer = timerView.findViewById(R.id.secondsContainer);
        TextView secondsTextView = timerView.findViewById(R.id.secondsTextView);

        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateTimerTextViews(daysContainer, daysTextView, colon1, hoursContainer, hoursTextView, colon2, minutesContainer, minutesTextView, colon3, secondsContainer, secondsTextView, millisUntilFinished);
                }

                public void onFinish() {
                    daysTextView.setText("00");
                    hoursTextView.setText("00");
                    minutesTextView.setText("00");
                    secondsTextView.setText("00");
                }
            }.start();
        } else {
            daysTextView.setText("00");
            hoursTextView.setText("00");
            minutesTextView.setText("00");
            secondsTextView.setText("00");
        }

        orderLayout.addView(rightSquareContainer);

        // Create and add the location
        TextView locationTextView = new TextView(this);
        locationTextView.setText(location);
        locationTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams locationParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        locationParams.addRule(RelativeLayout.BELOW, rightSquareContainer.getId()); // או כל אלמנט אחר מעל המיקום
        locationParams.addRule(RelativeLayout.BELOW, leftSquareLayout.getId());
        locationParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        locationParams.setMargins(0, dpToPx(150), 0, 0); // הוספת מרווח קטן למעלה
        locationTextView.setLayoutParams(locationParams);
        orderLayout.addView(locationTextView);

        // Fetch the type of order from Firestore and add it below the location
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            String typeOfOrder = documentSnapshot.getString("type_of_order");
            if (typeOfOrder != null) {
                TextView orderTypeTextView = new TextView(this);
                orderTypeTextView.setText(typeOfOrder);
                orderTypeTextView.setId(View.generateViewId());
                orderTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                orderTypeTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                orderTypeTextView.setTextColor(typeOfOrder.equals("Consumer") ? Color.parseColor("#00BFFF") : Color.BLUE);
                RelativeLayout.LayoutParams orderTypeParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                orderTypeParams.addRule(RelativeLayout.BELOW, locationTextView.getId());
                orderTypeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                orderTypeParams.setMargins(dpToPx(20), dpToPx(5), dpToPx(10), dpToPx(0));

                orderTypeTextView.setLayoutParams(orderTypeParams);
                orderLayout.addView(orderTypeTextView);

                if (typeOfOrder.equals("Consumer")) {
                    orderLayout.setBackgroundResource(R.drawable.border_green); // Use a drawable with green border
                } else if (typeOfOrder.equals("Supplier")) {
                    orderLayout.setBackgroundResource(R.drawable.border_blue); // Use a drawable with blue border
                }
            }

            // Check if the order is open or closed using existing variables
            boolean isOrderOpen = timestamp != null && timestamp.toDate().getTime() > currentTime;

            // Create and add the open/close text view
            TextView openCloseTextView = new TextView(this);
            openCloseTextView.setText(isOrderOpen ? "Open" : "Close");
            openCloseTextView.setId(View.generateViewId());
            openCloseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            openCloseTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            openCloseTextView.setTextColor(isOrderOpen ? Color.GREEN : Color.RED); // ירוק אם פתוח, אדום אם סגור
            RelativeLayout.LayoutParams openCloseTextParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            openCloseTextParams.addRule(RelativeLayout.BELOW, locationTextView.getId());
            openCloseTextParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            openCloseTextParams.setMargins(dpToPx(20), dpToPx(5), dpToPx(20), dpToPx(0));

            openCloseTextView.setLayoutParams(openCloseTextParams);
            orderLayout.addView(openCloseTextView);
        });

        // Create and add the star rating
        LinearLayout ratingLayout = new LinearLayout(this);
        ratingLayout.setOrientation(LinearLayout.HORIZONTAL);
        ratingLayout.setId(View.generateViewId());
        RelativeLayout.LayoutParams ratingLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ratingLayoutParams.addRule(RelativeLayout.BELOW, locationTextView.getId());
        ratingLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        ratingLayoutParams.setMargins(0, dpToPx(5), 0, 0); // Add margin from the edge
        ratingLayout.setLayoutParams(ratingLayoutParams);
        orderLayout.addView(ratingLayout);

        // Ensure the star rating layout is left-to-right
        ViewCompat.setLayoutDirection(ratingLayout, ViewCompat.LAYOUT_DIRECTION_LTR);

        // Add star images to the rating layout
        addStarsToLayout(ratingLayout, averageRating);

        // Add the order layout to the container
        ordersContainer.addView(orderLayout);
    }

    private void addStarsToLayout(LinearLayout layout, double rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = rating - fullStars >= 0.5;
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

    private void updateTimerTextViews(LinearLayout daysContainer, TextView daysTextView, TextView colon1, LinearLayout hoursContainer, TextView hoursTextView, TextView colon2, LinearLayout minutesContainer, TextView minutesTextView, TextView colon3, LinearLayout secondsContainer, TextView secondsTextView, long millisUntilFinished) {
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

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
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
            case R.id.list_icon:
                menuUtils.basket();
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

    public void gotofilter(View v) {
        Intent toy = new Intent(HomePageActivity.this, FilterActivity.class);
        startActivity(toy);
    }

    public void gotoneworder(View v) {
        Intent toy = new Intent(HomePageActivity.this, OpenNewOrderActivity.class);
        startActivity(toy);
    }

}
