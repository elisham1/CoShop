package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class HomePageActivity extends AppCompatActivity {

    private FirebaseUser currentUser;
    private Boolean clickOnStar=false;
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
        initializeUI();
    }

    private void showExplanationsIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstTime = prefs.getBoolean(KEY_FIRST_TIME, true);
//        firstTime = true;

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

    private void initializeUI() {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        }
        // Initialize the orders container
        ordersContainer = findViewById(R.id.ordersContainer);
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
        boolean filterActive = intent.getBooleanExtra("filterActive", false);

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
            // Create a new intent to refresh the activity
            Intent refreshIntent = new Intent(HomePageActivity.this, HomePageActivity.class);
            refreshIntent.putExtra("userType", globalUserType);
            startActivity(refreshIntent);
            finish();
        });

        // Set up the star button toggle
        ImageButton starButton = findViewById(R.id.starButton);
        starButton.setOnClickListener(v -> {
            if (starButton.getTag().equals("star")) {
                starButton.setImageResource(R.drawable.star);
                starButton.setTag("star2");
                clickOnStar=true;
                // קבל את המיקום של המשתמש מהפיירבייס
                db.collection("users").document(userEmail)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                GeoPoint userLocation = task.getResult().getGeoPoint("address");

                                if (userLocation != null) {
                                    // קריאה לפונקציה החדשה עם המיקום של המשתמש
                                    calculateDisplayedOrdersRecommendationRatings(currentUser, userLocation);
                                } else {
                                    Toast.makeText(this, "User location not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Failed to get user location", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                starButton.setImageResource(R.drawable.star2);
                clickOnStar=false;
                starButton.setTag("star");
                Intent refreshIntent = new Intent(HomePageActivity.this, HomePageActivity.class);
                refreshIntent.putExtra("userType", globalUserType);
                startActivity(refreshIntent);
                finish();
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

    private void getUserLocationAndShowFilteredOrderIds(String filteredOrderIds) {
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
                            showFilteredOrderIds(filteredOrderIds, userLocation);
                        }
                    }
                });
    }

    private void showFilteredOrderIds(String filteredOrderIds, GeoPoint userLocation) {
        // Clear any existing orders
        ordersContainer.removeAllViews();

        String[] orderIds = filteredOrderIds.split("\n");
        for (String orderId : orderIds) {
            db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult(); // שינוי ל-DocumentSnapshot

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

    private void showFilteredOrderIds(String filteredOrderIds) {
        // Clear any existing orders
        ordersContainer.removeAllViews();

        String[] orderIds = filteredOrderIds.split("\n");
        for (String orderId : orderIds) {
            db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot documentSnapshot = task.getResult(); // שינוי ל-DocumentSnapshot

                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
                    String location = orderLocation != null ? getAddressFromLatLng(orderLocation.getLatitude(), orderLocation.getLongitude()) : "N/A";
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");
                    String categorie = documentSnapshot.getString("categorie");
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");

                    float distance = calculateDistance(getUserLocation(), orderLocation);
                    Log.d("HomePageActivity", "Calculated distance: " + distance);
                    Log.d("HomePageActivity", "Calculated distance: " + getUserLocation());
                    Log.d("HomePageActivity", "Calculated distance: " + orderLocation);


                    calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            });
        }
    }

    private void fetchAllOrders(GeoPoint userLocation) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

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
        long numberOfPeopleInOrder;
        long maxPeople;
        String categorie;
        double distance;
        Timestamp timestamp;
        double averageRating;
        double totalScore;
        double categoryScore; // שדה חדש עבור categoryScore
        double ratingScore;   // שדה חדש עבור ratingScore
        double distanceScore; // שדה חדש עבור distanceScore

        public OrderData(String orderId, String titleOfOrder, String location, long numberOfPeopleInOrder, long maxPeople, String categorie, double distance, Timestamp timestamp, double averageRating) {
            this.orderId = orderId;
            this.titleOfOrder = titleOfOrder;
            this.location = location;
            this.numberOfPeopleInOrder = numberOfPeopleInOrder;
            this.maxPeople = maxPeople;
            this.categorie = categorie;
            this.distance = distance;
            this.timestamp = timestamp;
            this.averageRating = averageRating;
        }
    }

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

                    // עיגול הדירוג
                    averageRating = Math.round(averageRating * 2) / 2.0;

                    updateRatingOrderInFirestore(orderId, averageRating);

                    addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople,
                            categorie, distance, timestamp, averageRating);

                    orderDataList.add(new OrderData(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp, averageRating));

                    // הוספת מזהה ההזמנה לרשימה
                    displayedOrderIds.add(orderId);

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    // Handle exception
                }
            });
        });
    }

    // פונקציה חדשה שתחשב את הציונים על סמך ההזמנות שהוצגו בדף הבית
    private void calculateDisplayedOrdersRecommendationRatings(FirebaseUser currentUser, GeoPoint userLocation) {
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
                        Log.d("RecommendationRating", "distance: " + distance);

                        double categoryScore = calculateCategoryScore(categorie, userPreferredCategories);
                        double ratingScore = calculateRatingScore(rating);
                        double distanceScore = calculateDistanceScore(distance, minDistance, maxDistance);

                        double totalScore = categoryScore + ratingScore + distanceScore;

                        Log.d("RecommendationRating", "Order ID: " + orderId);
                        Log.d("RecommendationRating", "Category Score: " + categoryScore);
                        Log.d("RecommendationRating", "Rating Score: " + ratingScore);
                        Log.d("RecommendationRating", "Distance Score: " + distanceScore);
                        Log.d("RecommendationRating", "Total Score: " + totalScore);

                        // מצרף את הנתונים לרשימה הממוינת
                        OrderData orderData = findOrderDataById(orderId);
                        if (orderData != null) {
                            orderData.totalScore = totalScore;
                            orderData.categoryScore = categoryScore; // הוספת categoryScore
                            orderData.ratingScore = ratingScore;     // הוספת ratingScore
                            orderData.distanceScore = distanceScore; // הוספת distanceScore
                            sortedOrderDataList.add(orderData);
                        }
                    }

                    // ממיין את הרשימה לפי totalScore מהגבוה לנמוך
                    sortedOrderDataList.sort((o1, o2) -> Double.compare(o2.totalScore, o1.totalScore));

                    // מנקה את המסך מהזמנות קיימות
                    ordersContainer.removeAllViews();

                    // מציג את ההזמנות לפי הסדר הממוין
                    for (OrderData orderData : sortedOrderDataList) {
                        addOrderToLayout(orderData.orderId, orderData.titleOfOrder, orderData.location, orderData.numberOfPeopleInOrder, orderData.maxPeople, orderData.categorie, orderData.distance, orderData.timestamp, orderData.averageRating);
                    }
                }
            });
        });
    }

    // מחפש את OrderData לפי orderId
    private OrderData findOrderDataById(String orderId) {
        for (OrderData orderData : orderDataList) {
            if (orderData.orderId.equals(orderId)) {
                return orderData;
            }
        }
        return null;
    }

    private void updateRatingOrderInFirestore(String orderId, double averageRating) {
        db.collection("orders").document(orderId)
                .update("ratingOrder", averageRating)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "ratingOrder successfully updated"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error updating ratingOrder", e));
    }

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

    private void addOrderToLayout(String orderId, String titleOfOrder, String location, long numberOfPeopleInOrder, long maxPeople, String categorie, double distance, Timestamp timestamp, double averageRating) {
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
            intent.putExtra("userType", globalUserType);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });

        TextView titleTextView = new TextView(this);
        titleTextView.setText(titleOfOrder);
        titleTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams titleTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleTextParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        titleTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        titleTextParams.setMargins(dpToPx(20), dpToPx(10), dpToPx(10), dpToPx(5)); // Add margin from the edge
        titleTextView.setLayoutParams(titleTextParams);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        titleTextView.setTypeface(null, Typeface.BOLD); // Bold text
        titleTextView.setTextColor(Color.BLACK); // Set text color to black
        orderLayout.addView(titleTextView);

        TextView distanceTextView = new TextView(this);
        distanceTextView.setText(String.format("%.2f km", distance));
        distanceTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams distanceTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        distanceTextParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        distanceTextParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        distanceTextParams.setMargins(dpToPx(10), dpToPx(10), dpToPx(20), dpToPx(5)); // Add margin from the edge
        distanceTextView.setLayoutParams(distanceTextParams);
        distanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        distanceTextView.setTypeface(null, Typeface.BOLD); // Bold text
        distanceTextView.setTextColor(Color.BLACK); // Set text color to black
        orderLayout.addView(distanceTextView);

        // Create and add the people count
        TextView peopleTextView = new TextView(this);
        if (maxPeople == 0) {
            peopleTextView.setText("∞/"+numberOfPeopleInOrder);
        } else {
            peopleTextView.setText(numberOfPeopleInOrder + "/" + maxPeople);
        }
        peopleTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams peopleTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        peopleTextParams.addRule(RelativeLayout.CENTER_IN_PARENT); // Center horizontally and vertically
        peopleTextView.setLayoutParams(peopleTextParams);
        peopleTextParams.setMargins(0, dpToPx(10), 0, dpToPx(10)); // Add margin between title and people count
        peopleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Increase text size
        peopleTextView.setTypeface(null, Typeface.BOLD); // Bold text
        peopleTextView.setTextColor(Color.BLACK); // Set text color to black
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
                .placeholder(R.drawable.other) // Optional: Add a placeholder image
                .error(R.drawable.other) // Optional: Add an error image
                .into(leftSquare);

        TextView categoryTextView = new TextView(this);
        categoryTextView.setText(categorie);
        categoryTextView.setId(View.generateViewId());
        categoryTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        categoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        categoryTextView.setTypeface(null, Typeface.BOLD); // Bold text
        categoryTextView.setTextColor(Color.BLACK); // Set text color to black
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
        locationTextView.setId(View.generateViewId());
        locationTextView.setSingleLine(true);
        locationTextView.setEllipsize(TextUtils.TruncateAt.END);
        locationTextView.setText(location);
        RelativeLayout.LayoutParams locationParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        locationParams.addRule(RelativeLayout.BELOW, rightSquareContainer.getId()); // או כל אלמנט אחר מעל המיקום
        locationParams.addRule(RelativeLayout.BELOW, leftSquareLayout.getId());
        locationParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        locationParams.setMargins(0, dpToPx(170), 0, dpToPx(10)); // הוספת מרווח קטן למעלה
        locationTextView.setLayoutParams(locationParams);
        locationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        locationTextView.setTypeface(null, Typeface.BOLD); // Bold text
        locationTextView.setTextColor(Color.BLACK); // Set text color to black
        orderLayout.addView(locationTextView);


        // Fetch the type of order from Firestore and add it below the location
        db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
            String typeOfOrder = documentSnapshot.getString("type_of_order");
            if (typeOfOrder != null) {
                TextView orderTypeTextView = new TextView(this);
                orderTypeTextView.setText(typeOfOrder);
                orderTypeTextView.setTypeface(null, Typeface.BOLD);
                orderTypeTextView.setId(View.generateViewId());
                orderTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                orderTypeTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                orderTypeTextView.setTextColor(typeOfOrder.equals("Consumer") ?
                        Color.parseColor("#1679AB") :
                        Color.parseColor("#E98654"));
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

            // Create a SpannableString to apply different styles and colors
            SpannableString spannableString;
            if (isOrderOpen) {
                spannableString = new SpannableString("Open");
                spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), 0, spannableString.length(), 0); // Set color to green
                spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableString.length(), 0); // Set style to bold
            } else {
                spannableString = new SpannableString("Close");
                spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannableString.length(), 0); // Set color to red
                spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableString.length(), 0); // Set style to bold
            }

            openCloseTextView.setText(spannableString);
            openCloseTextView.setId(View.generateViewId());
            openCloseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            openCloseTextView.setGravity(Gravity.CENTER_HORIZONTAL);

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

        if (clickOnStar == true) {
            // מציאת ה-OrderData המתאים לפי orderId
            OrderData orderData = findOrderDataById(orderId);
            if (orderData != null) {
                // יצירת RelativeLayout עבור האייקונים והציונים
                RelativeLayout scoresLayout = new RelativeLayout(this);
                RelativeLayout.LayoutParams scoresLayoutParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                scoresLayoutParams.addRule(RelativeLayout.BELOW, locationTextView.getId());
                scoresLayoutParams.setMargins(0, dpToPx(40), 0, dpToPx(5));
                scoresLayout.setLayoutParams(scoresLayoutParams);

                // יצירת TextView עבור totalScore בצד START של השורה
                TextView totalScoreTextView = new TextView(this);
                totalScoreTextView.setText(String.format(" matches for you %s", formatScore(orderData.totalScore)));
                totalScoreTextView.setId(View.generateViewId());
                totalScoreTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                totalScoreTextView.setTypeface(null, Typeface.BOLD);
                totalScoreTextView.setTextColor(Color.BLACK);

                RelativeLayout.LayoutParams totalScoreParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                totalScoreParams.addRule(RelativeLayout.ALIGN_PARENT_START);
                totalScoreTextView.setLayoutParams(totalScoreParams);
                scoresLayout.addView(totalScoreTextView);

                // יצירת LinearLayout עבור האייקונים והציונים בצד END של השורה
                LinearLayout iconsLayout = new LinearLayout(this);
                iconsLayout.setOrientation(LinearLayout.HORIZONTAL);
                RelativeLayout.LayoutParams iconsLayoutParams = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                iconsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                iconsLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                iconsLayout.setLayoutParams(iconsLayoutParams);

                // יצירת ImageView ו-TextView עבור קטגוריה
                ImageView categoryIcon = new ImageView(this);
                categoryIcon.setImageResource(R.drawable.category);
                LinearLayout.LayoutParams categoryIconParams = new LinearLayout.LayoutParams(
                        dpToPx(16), dpToPx(16));
                categoryIconParams.setMargins(dpToPx(5), 0, dpToPx(5), 0);
                categoryIcon.setLayoutParams(categoryIconParams);
                iconsLayout.addView(categoryIcon);

                TextView categoryScoreTextView = new TextView(this);
                categoryScoreTextView.setText(formatScore(orderData.categoryScore));
                categoryScoreTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                categoryScoreTextView.setTypeface(null, Typeface.BOLD);
                categoryScoreTextView.setTextColor(Color.BLACK);
                LinearLayout.LayoutParams categoryScoreParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                categoryScoreTextView.setLayoutParams(categoryScoreParams);
                iconsLayout.addView(categoryScoreTextView);

                // יצירת ImageView ו-TextView עבור מיקום
                ImageView locationIcon = new ImageView(this);
                locationIcon.setImageResource(R.drawable.location);
                LinearLayout.LayoutParams locationIconParams = new LinearLayout.LayoutParams(
                        dpToPx(16), dpToPx(16));
                locationIconParams.setMargins(dpToPx(5), 0, dpToPx(5), 0);
                locationIcon.setLayoutParams(locationIconParams);
                iconsLayout.addView(locationIcon);

                TextView distanceScoreTextView = new TextView(this);
                distanceScoreTextView.setText(formatScore(orderData.distanceScore));
                distanceScoreTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                distanceScoreTextView.setTypeface(null, Typeface.BOLD);
                distanceScoreTextView.setTextColor(Color.BLACK);
                LinearLayout.LayoutParams distanceScoreParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                distanceScoreTextView.setLayoutParams(distanceScoreParams);
                iconsLayout.addView(distanceScoreTextView);

                // יצירת ImageView ו-TextView עבור דירוג
                ImageView ratingIcon = new ImageView(this);
                ratingIcon.setImageResource(R.drawable.star);
                LinearLayout.LayoutParams ratingIconParams = new LinearLayout.LayoutParams(
                        dpToPx(16), dpToPx(16));
                ratingIconParams.setMargins(dpToPx(5), 0, dpToPx(5), 0);
                ratingIcon.setLayoutParams(ratingIconParams);
                iconsLayout.addView(ratingIcon);

                TextView ratingScoreTextView = new TextView(this);
                ratingScoreTextView.setText(formatScore(orderData.ratingScore));
                ratingScoreTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                ratingScoreTextView.setTypeface(null, Typeface.BOLD);
                ratingScoreTextView.setTextColor(Color.BLACK);
                LinearLayout.LayoutParams ratingScoreParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ratingScoreTextView.setLayoutParams(ratingScoreParams);
                iconsLayout.addView(ratingScoreTextView);

                // הוספת iconsLayout ל-scoresLayout
                scoresLayout.addView(iconsLayout);

                // הוספת scoresLayout ל-orderLayout
                orderLayout.addView(scoresLayout);
            }
        }
    }

    private String formatScore(double score) {
        if (score % 1 == 0) {
            return String.format(Locale.getDefault(), "%.0f%%", score);
        } else if (score * 10 % 1 == 0) {
            return String.format(Locale.getDefault(), "%.1f%%", score);
        } else {
            return String.format(Locale.getDefault(), "%.2f%%", score);
        }
    }
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

    public void gotofilter(View v) {
        Intent toy = new Intent(HomePageActivity.this, FilterActivity.class);
        toy.putExtra("userType", globalUserType);
        startActivity(toy);
    }

    public void gotoneworder(View v) {
        Intent toy = new Intent(HomePageActivity.this, OpenNewOrderActivity.class);
        toy.putExtra("userType", globalUserType);
        startActivity(toy);
    }


    private double calculateCategoryScore(String orderCategory, List<String> userPreferredCategories) {
        if (userPreferredCategories != null && userPreferredCategories.contains(orderCategory)) {
            return (100/3);
        }
        return 0;
    }



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



    private double calculateRatingScore(Double rating) {
        if (rating == null) {
            return 0;
        }
        return Math.round((rating / 5.0) * (100/3) * 2) / 2.0;
    }

    private double calculateDistanceScore(float distanceInKm, double minDistance, double maxDistance) {
        if (distanceInKm <= minDistance) {
            return (100/3);
        } else if (distanceInKm >= maxDistance) {
            return 0;
        } else {
            return (1 - (distanceInKm - minDistance) / (maxDistance - minDistance)) * (100/3);
        }
    }


}
