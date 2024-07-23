package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.Intent;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    }

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
                Toast.makeText(MyOrdersActivity.this, "Failed to retrieve waiting list orders", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MyOrdersActivity.this, "Failed to retrieve orders", Toast.LENGTH_SHORT).show();
            });
        }
    }

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

    private void updateRatingOrderInFirestore(String orderId, double averageRating) {
        Log.d("MyOrdersActivity", "Updating ratingOrder in Firestore for order: " + orderId);
        db.collection("orders").document(orderId)
                .update("ratingOrder", averageRating)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "ratingOrder successfully updated"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error updating ratingOrder", e));
    }

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

    private void addOrderToLayout(String orderId, String titleOfOrder,
                                  String location, long numberOfPeopleInOrder,
                                  long maxPeople, String categorie, double distance,
                                  Timestamp timestamp, double averageRating) {
        Log.d("MyOrdersActivity", "Adding order to layout: " + orderId);
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
            Log.d("MyOrdersActivity", "Order clicked: " + orderId);
            Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
            intent.putExtra("userType", globalUserType);
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
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        titleTextView.setTypeface(null, Typeface.BOLD); // Bold text
        titleTextView.setTextColor(Color.BLACK); // Set text color to black
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
        distanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Increase text size
        distanceTextView.setTypeface(null, Typeface.BOLD); // Bold text
        distanceTextView.setTextColor(Color.BLACK); // Set text color to black
        orderLayout.addView(distanceTextView);

        // Create and add the people count
        TextView peopleTextView = new TextView(this);
        if (maxPeople == 0) {
            peopleTextView.setText("∞/" + numberOfPeopleInOrder);
            //peopleText = "People: " + numberOfPeopleInOrder + "/∞";

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
                .placeholder(R.drawable.star) // Optional: Add a placeholder image
                .error(R.drawable.star2) // Optional: Add an error image
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
                orderTypeTextView.setId(View.generateViewId());
                orderTypeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                orderTypeTextView.setTypeface(null, Typeface.BOLD); // Bold text
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
            boolean isOrderOpen = timestamp.toDate().getTime() > currentTime;

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
        ordersLayout.addView(orderLayout);
    }

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

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

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

    private float calculateDistance(GeoPoint userLocation, GeoPoint orderLocation) {
        if (userLocation == null || orderLocation == null) return 0;

        float[] results = new float[1];
        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                orderLocation.getLatitude(), orderLocation.getLongitude(), results);
        return Math.abs(results[0] / 1000); // המרחק בקילומטרים
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
