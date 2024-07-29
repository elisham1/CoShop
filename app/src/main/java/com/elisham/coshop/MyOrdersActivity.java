package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MyOrdersActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private MenuUtils menuUtils;
    private LinearLayout ordersLayout;
    private String userEmail, globalUserType;
    private GeoPoint userLocation;
    private Geocoder geocoder;
    private TextView tvAllOrders, tvOpenedOrders, tvClosedOrders, tvSupplierOrders, tvConsumerOrders, tvWaitList;
    private static final String ALL_ORDERS = "All My Orders";
    private static final String OPENED_ORDERS = "Opened Orders";
    private static final String CLOSED_ORDERS = "Closed Orders";
    private static final String SUPPLIER_ORDERS = "Supplier Orders";
    private static final String CONSUMER_ORDERS = "Customer Orders";
    private static final String WAIT_LIST = "Wait List";
    private List<String> selectedOptions = new ArrayList<>();
    public static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_FIRST_TIME = "firstTime";
    private RelativeLayout explanationLayout;
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MyOrdersActivity", "onCreate called");
        // Set the theme based on the user type
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }
        setContentView(R.layout.activity_my_orders);
        db = FirebaseFirestore.getInstance();
        menuUtils = new MenuUtils(this, globalUserType);
        ordersLayout = findViewById(R.id.ordersLayout);

        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.ENGLISH);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        userEmail = currentUser.getEmail();

        Log.d("MyOrdersActivity", "Fetching user location");
        db.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userLocation = documentSnapshot.getGeoPoint("address");
                        Log.d("MyOrdersActivity", "User location fetched: " + userLocation);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("MyOrdersActivity", "Error getting user location", e);
                });

        tvWaitList = findViewById(R.id.tvWaitList);
        tvAllOrders = findViewById(R.id.tvAllOrders);
        tvOpenedOrders = findViewById(R.id.tvOpenedOrders);
        tvClosedOrders = findViewById(R.id.tvClosedOrders);
        tvSupplierOrders = findViewById(R.id.tvSupplierOrders);
        tvConsumerOrders = findViewById(R.id.tvConsumerOrders);

        if (globalUserType.equals("Supplier")) {
            tvSupplierOrders.setVisibility(View.GONE);
            tvConsumerOrders.setVisibility(View.GONE);
            tvWaitList.setVisibility(View.GONE);
        }

        String sharedLink = intent.getStringExtra("sharedLink");

        if (sharedLink != null) {
            TextView sharedLinkTextView = findViewById(R.id.sharedLinkTextView);
            sharedLinkTextView.setVisibility(View.VISIBLE);
            // Save the shared link in SharedPreferences to pass it to OrderDetailsActivity
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("sharedLink", sharedLink);
            editor.apply();
        }

        // Set default view to All Orders
        selectedOptions.add(ALL_ORDERS);
        readUserOrders(selectedOptions);

        tvWaitList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "Wait List clicked");
                selectedOptions.clear();
                selectedOptions.add(WAIT_LIST);
                readUserOrders(selectedOptions);
            }
        });

        tvAllOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "All Orders clicked");
                selectedOptions.clear();
                selectedOptions.add(ALL_ORDERS);
                readUserOrders(selectedOptions);
            }
        });

        tvOpenedOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "Opened Orders clicked");
                selectedOptions.remove(ALL_ORDERS);
                selectedOptions.remove(WAIT_LIST);
                if (selectedOptions.contains(OPENED_ORDERS)) {
                    selectedOptions.remove(OPENED_ORDERS);
                    if (selectedOptions.isEmpty()) {
                        selectedOptions.add(ALL_ORDERS);
                    }
                } else {
                    selectedOptions.remove(CLOSED_ORDERS);
                    selectedOptions.add(OPENED_ORDERS);
                }
                readUserOrders(selectedOptions);
            }
        });

        tvClosedOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "Closed Orders clicked");
                selectedOptions.remove(ALL_ORDERS);
                selectedOptions.remove(WAIT_LIST);
                if (selectedOptions.contains(CLOSED_ORDERS)) {
                    selectedOptions.remove(CLOSED_ORDERS);
                    if (selectedOptions.isEmpty()) {
                        selectedOptions.add(ALL_ORDERS);
                    }
                } else {
                    selectedOptions.remove(OPENED_ORDERS);
                    selectedOptions.add(CLOSED_ORDERS);
                }
                readUserOrders(selectedOptions);
            }
        });

        tvSupplierOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "Supplier Orders clicked");
                if (selectedOptions.contains(ALL_ORDERS) || selectedOptions.contains(WAIT_LIST)) {
                    selectedOptions.remove(ALL_ORDERS);
                    selectedOptions.remove(WAIT_LIST);
                }
                if (selectedOptions.contains(SUPPLIER_ORDERS)) {
                    selectedOptions.remove(SUPPLIER_ORDERS);
                    if (selectedOptions.isEmpty()) {
                        selectedOptions.add(ALL_ORDERS);
                    }
                } else {
                    selectedOptions.remove(CONSUMER_ORDERS);
                    selectedOptions.add(SUPPLIER_ORDERS);
                }
                readUserOrders(selectedOptions);
            }
        });

        tvConsumerOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "Consumer Orders clicked");
                if (selectedOptions.contains(ALL_ORDERS) || selectedOptions.contains(WAIT_LIST)) {
                    selectedOptions.remove(ALL_ORDERS);
                    selectedOptions.remove(WAIT_LIST);
                }
                if (selectedOptions.contains(CONSUMER_ORDERS)) {
                    selectedOptions.remove(CONSUMER_ORDERS);
                    if (selectedOptions.isEmpty()) {
                        selectedOptions.add(ALL_ORDERS);
                    }
                } else {
                    selectedOptions.remove(SUPPLIER_ORDERS);
                    selectedOptions.add(CONSUMER_ORDERS);
                }
                readUserOrders(selectedOptions);
            }
        });

        ImageButton newOrderButton = findViewById(R.id.newOrderButton);
        if (globalUserType.equals("Supplier")) {
            newOrderButton.setVisibility(View.VISIBLE);
            newOrderButton.setImageResource(R.drawable.ic_plus_supplier);
        }
        newOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MyOrdersActivity", "New Order button clicked");
                Intent intent = new Intent(MyOrdersActivity.this, OpenNewOrderActivity.class);
                intent.putExtra("userType", globalUserType);
                startActivity(intent);
            }
        });

        if (globalUserType.equals("Supplier")) {
            showExplanationsIfNeeded();
        }
    }

    // Displays explanations if needed
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
                textEnjoy.setVisibility(View.VISIBLE);
                btnDismiss.setVisibility(View.VISIBLE);
                break;
        }
    }

    // Reads user orders based on selected options
    private void readUserOrders(List<String> options) {
        Log.d("MyOrdersActivity", "readUserOrders called with options: " + options);

        // Clear the existing orders
        ordersLayout.removeAllViews();

        updateButtonColors(options);

        CollectionReference ordersRef = db.collection("orders");

        if (selectedOptions.contains(WAIT_LIST)) {
            Query allWaitingOrdersQuery = ordersRef.whereArrayContains("waitingList", userEmail);
            allWaitingOrdersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    Log.d("MyOrdersActivity", "All Waiting Orders query successful");
                    List<DocumentSnapshot> allWaitingOrdersList = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        allWaitingOrdersList.add(documentSnapshot);
                    }

                    displayOrders(allWaitingOrdersList);
                }
            }).addOnFailureListener(e -> {
                Log.e("MyOrdersActivity", "Failed to retrieve waiting list orders", e);
                Log.d("MyOrdersActivity", "Failed to retrieve waiting list orders");
            });
        } else {
            // Query orders where the user's email is in the listPeopleInOrder array
            Query allOrdersQuery = ordersRef.whereArrayContains("listPeopleInOrder", userEmail);
            allOrdersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    Log.d("MyOrdersActivity", "All Orders query successful");
                    List<DocumentSnapshot> allOrdersList = new ArrayList<>();
                    List<DocumentSnapshot> openedOrdersList = new ArrayList<>();
                    List<DocumentSnapshot> closedOrdersList = new ArrayList<>();
                    List<DocumentSnapshot> supplierOrdersList = new ArrayList<>();
                    List<DocumentSnapshot> consumerOrdersList = new ArrayList<>();

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                        // Add to the list of all orders
                        allOrdersList.add(documentSnapshot);

                        // Check if the order is open or closed based on the "time" field
                        Timestamp orderTime = documentSnapshot.getTimestamp("time");
                        Timestamp currentTime = Timestamp.now();
                        if (orderTime != null && currentTime.compareTo(orderTime) >= 0) {
                            closedOrdersList.add(documentSnapshot);
                        } else {
                            openedOrdersList.add(documentSnapshot);
                        }

                        // Check if the order type is supplier or consumer
                        String orderType = documentSnapshot.getString("type_of_order");
                        if ("supplier".equalsIgnoreCase(orderType)) {
                            supplierOrdersList.add(documentSnapshot);
                        } else {
                            consumerOrdersList.add(documentSnapshot);
                        }
                    }

                    List<DocumentSnapshot> ordersToDisplay = new ArrayList<>();
                    if (options.contains(ALL_ORDERS))
                    {
                        ordersToDisplay.addAll(allOrdersList);
                    } else {
                        if (options.contains(OPENED_ORDERS))
                        {
                            if (options.contains(SUPPLIER_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : supplierOrdersList) {
                                    if (openedOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else if (options.contains(CONSUMER_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : consumerOrdersList) {
                                    if (openedOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else {
                                ordersToDisplay.addAll(openedOrdersList);
                            }
                        } else if (options.contains(CLOSED_ORDERS)){
                            if (options.contains(SUPPLIER_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : supplierOrdersList) {
                                    if (closedOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else if (options.contains(CONSUMER_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : consumerOrdersList) {
                                    if (closedOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else {
                                ordersToDisplay.addAll(closedOrdersList);
                            }
                        } else if (options.contains(SUPPLIER_ORDERS)){
                            if (options.contains(OPENED_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : openedOrdersList) {
                                    if (supplierOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else if (options.contains(CLOSED_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : closedOrdersList) {
                                    if (supplierOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else {
                                ordersToDisplay.addAll(supplierOrdersList);
                            }
                        } else {
                            if (options.contains(OPENED_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : openedOrdersList) {
                                    if (consumerOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else if (options.contains(CLOSED_ORDERS))
                            {
                                for (DocumentSnapshot documentSnapshot : closedOrdersList) {
                                    if (consumerOrdersList.contains(documentSnapshot))
                                        ordersToDisplay.add(documentSnapshot);
                                }
                            } else {
                                ordersToDisplay.addAll(consumerOrdersList);
                            }
                        }
                    }

                    // Sort the orders by open time
                    ordersToDisplay.sort((o1, o2) -> {
                        Timestamp time1 = o1.getTimestamp("openOrderTime");
                        Timestamp time2 = o2.getTimestamp("openOrderTime");
                        return time2 != null && time1 != null ? time2.compareTo(time1) : 0;
                    });
                    displayOrders(ordersToDisplay);
                }
            }).addOnFailureListener(e -> {
                Log.e("MyOrdersActivity", "Failed to retrieve orders", e);
                Log.d("MyOrdersActivity", "Failed to retrieve orders");
            });
        }
    }

    // Displays orders in the layout
    private void displayOrders(List<DocumentSnapshot> orders) {
        Log.d("MyOrdersActivity", "displayOrders called with " + orders.size() + " orders");

        if (orders.isEmpty()) {
            showNoOrdersFoundMessage();
            return;
        }

        // Clear the existing orders
        ordersLayout.removeAllViews();

        for (DocumentSnapshot documentSnapshot : orders) {
            String orderId = documentSnapshot.getId();
            String titleOfOrder = documentSnapshot.getString("titleOfOrder");
            GeoPoint orderLocation = documentSnapshot.getGeoPoint("location");
            double latitude = orderLocation.getLatitude();
            double longitude = orderLocation.getLongitude();
            String location = getAddressFromLatLng(latitude, longitude);
            long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
            long maxPeople = documentSnapshot.getLong("max_people");
            String categorie = documentSnapshot.getString("categorie");
            Log.d("HomePageActivity", "Calculated distance: " + userLocation);
            Log.d("HomePageActivity", "Calculated distance: " + orderLocation);
            float distance = calculateDistance(userLocation, orderLocation);
            Timestamp timestamp = documentSnapshot.getTimestamp("time");

            // Calculate the average rating
            calculateAndDisplayRatings(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);

        }
    }

    // Calculates and displays ratings for an order
    private void calculateAndDisplayRatings(String orderId, String titleOfOrder, String location,
                                            long numberOfPeopleInOrder, long maxPeople, String categorie,
                                            float distance, Timestamp timestamp) {
        Log.d("MyOrdersActivity", "calculateAndDisplayRatings called for order: " + orderId);
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
                    updateRatingOrderInFirestore(orderId, averageRating);

                    addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople,
                            categorie, distance, timestamp, averageRating);
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("MyOrdersActivity", "Error calculating ratings", e);
                    e.printStackTrace();
                    // Handle exception
                }
            });
        });
    }

    // Updates the rating of an order in Firestore
    private void updateRatingOrderInFirestore(String orderId, double averageRating) {
        Log.d("MyOrdersActivity", "Updating ratingOrder in Firestore for order: " + orderId);
        db.collection("orders").document(orderId)
                .update("ratingOrder", averageRating)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "ratingOrder successfully updated"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error updating ratingOrder", e));
    }

    // Calculates the rating of a user
    private Double calculateUserRating(DocumentSnapshot userDoc) {
        Log.d("MyOrdersActivity", "Calculating user rating for user: " + userDoc.getId());
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

    // Adds an order to the layout
    private void addOrderToLayout(String orderId, String titleOfOrder, String location,
                                  long numberOfPeopleInOrder, long maxPeople, String categorie,
                                  double distance, Timestamp timestamp, double averageRating) {
        View orderLayout = getLayoutInflater().inflate(R.layout.order_item, ordersLayout, false);
        if (globalUserType.equals("Supplier")) {
            orderLayout.setBackgroundResource(R.drawable.border_blue);
        } else {
            orderLayout.setBackgroundResource(R.drawable.border_green);
        }

        // Set onClickListener to open order details
        orderLayout.setOnClickListener(v -> {
            Log.d("MyOrdersActivity", "Order clicked: " + orderId);
            Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
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

        // Get type from db
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

        // Add the order layout to the container
        ordersLayout.addView(orderLayout);
    }

    // Adds stars to the rating layout
    private void addStarsToLayout(LinearLayout layout, double rating) {
        Log.d("MyOrdersActivityRating", "Rating: " + rating);
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

    // Converts dp to pixels
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // Gets address from latitude and longitude
    private String getAddressFromLatLng(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                Log.d("MyOrdersActivity", "Address fetched: " + address.getAddressLine(0));
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            Log.e("MyOrdersActivity", "Error fetching address", e);
            e.printStackTrace();
        }
        return "N/A";
    }

    // Calculates distance between two GeoPoints
    private float calculateDistance(GeoPoint userLocation, GeoPoint orderLocation) {
        if (userLocation == null || orderLocation == null) return 0;

        float[] results = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                orderLocation.getLatitude(), orderLocation.getLongitude(), results);
        return Math.abs(results[0] / 1000); // distance in kilometers
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

    // Updates button colors based on selected options
    private void updateButtonColors(List<String> selectedOptions) {
        Log.d("MyOrdersActivity", "Updating button colors for options: " + selectedOptions);

        if (globalUserType.equals("Supplier")) {
            tvAllOrders.setBackgroundResource(R.drawable.bg_selected_supplier);
        } else {
            if (selectedOptions.size() == 4) {
                selectedOptions.clear();
                selectedOptions.add(ALL_ORDERS);
            }
            tvAllOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
        }

        tvOpenedOrders.setBackgroundResource(R.drawable.bg_unselected);
        tvClosedOrders.setBackgroundResource(R.drawable.bg_unselected);
        tvSupplierOrders.setBackgroundResource(R.drawable.bg_unselected);
        tvConsumerOrders.setBackgroundResource(R.drawable.bg_unselected);

        for (String option : selectedOptions) {
            if (option.equals(ALL_ORDERS)) {
                if (globalUserType.equals("Supplier")) {
                    tvAllOrders.setBackgroundResource(R.drawable.bg_selected_supplier);
                } else {
                    tvAllOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
                }
                tvOpenedOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvClosedOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvSupplierOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvConsumerOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvWaitList.setBackgroundResource(R.drawable.bg_unselected);
                break;
            } else if (option.equals(WAIT_LIST)) {
                tvAllOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvOpenedOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvClosedOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvSupplierOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvConsumerOrders.setBackgroundResource(R.drawable.bg_unselected);
                if (globalUserType.equals("Supplier")) {
                    tvWaitList.setBackgroundResource(R.drawable.bg_selected_supplier);
                } else {
                    tvWaitList.setBackgroundResource(R.drawable.bg_selected_consumer);
                }
            } else {
                tvAllOrders.setBackgroundResource(R.drawable.bg_unselected);
                tvWaitList.setBackgroundResource(R.drawable.bg_unselected);

                if (option.equals(OPENED_ORDERS)) {
                    if (globalUserType.equals("Supplier")) {
                        tvOpenedOrders.setBackgroundResource(R.drawable.bg_selected_supplier);
                    } else {
                        tvOpenedOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
                    }
                }
                if (option.equals(CLOSED_ORDERS)) {
                    if (globalUserType.equals("Supplier")) {
                        tvClosedOrders.setBackgroundResource(R.drawable.bg_selected_supplier);
                    } else {
                        tvClosedOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
                    }
                }
                if (option.equals(SUPPLIER_ORDERS)) {
                    tvSupplierOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
                }
                if (option.equals(CONSUMER_ORDERS)) {
                    tvConsumerOrders.setBackgroundResource(R.drawable.bg_selected_consumer);
                }
            }
        }
    }

    // Shows a message when no orders are found
    private void showNoOrdersFoundMessage() {
        Log.d("MyOrdersActivity", "No orders found");
        ordersLayout.removeAllViews();
        TextView noOrdersTextView = new TextView(this);
        noOrdersTextView.setText("No orders found for this filter.");
        noOrdersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        noOrdersTextView.setTextColor(Color.RED);
        ordersLayout.addView(noOrdersTextView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MyOrdersActivity", "onCreateOptionsMenu called");
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
        Log.d("MyOrdersActivity", "onOptionsItemSelected called with itemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.Personal_info:
                Log.d("MyOrdersActivity", "Personal info selected");
                menuUtils.personalInfo();
                return true;
            case R.id.My_Orders:
                Log.d("MyOrdersActivity", "My Orders selected");
                menuUtils.myOrders();
                return true;
            case R.id.About_Us:
                Log.d("MyOrdersActivity", "About Us selected");
                menuUtils.aboutUs();
                return true;
            case R.id.Contact_Us:
                Log.d("MyOrdersActivity", "Contact Us selected");
                menuUtils.contactUs();
                return true;
            case R.id.Log_Out:
                Log.d("MyOrdersActivity", "Log Out selected");
                menuUtils.logOut();
                return true;
            case R.id.home:
                Log.d("MyOrdersActivity", "Home selected");
                menuUtils.home();
                return true;
            case R.id.chat_icon:
                Log.d("MyOrdersActivity", "Chat icon selected");
                menuUtils.allChats();
                return true;
            case R.id.chat_notification:
                Log.d("MyOrdersActivity", "Chat notification selected");
                menuUtils.chat_notification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
