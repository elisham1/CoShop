package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
                    addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            }
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

                addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
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
                    addOrderToLayout(orderId, titleOfOrder, location, numberOfPeopleInOrder, maxPeople, categorie, distance, timestamp);
                }
            }
        });
    }
    private void addOrderToLayout(String orderId, String titleOfOrder, String location, long numberOfPeopleInOrder, long maxPeople, String categorie, double distance, Timestamp timestamp) {
        // Create a new RelativeLayout for the order
        RelativeLayout orderLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams orderLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        orderLayoutParams.setMargins(0, 0, 0, dpToPx(8)); // Add margin between orders
        orderLayout.setLayoutParams(orderLayoutParams);
        orderLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        orderLayout.setBackgroundColor(Color.LTGRAY);

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
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
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
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
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
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        peopleTextParams.addRule(RelativeLayout.CENTER_IN_PARENT); // Center horizontally and vertically
        peopleTextView.setLayoutParams(peopleTextParams);
        peopleTextParams.setMargins(0, dpToPx(10), 0, dpToPx(10)); // Add margin between title and people count
        orderLayout.addView(peopleTextView);

        // Create and add the left square with category
        RelativeLayout leftSquareLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams leftSquareLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT); // Set WRAP_CONTENT for the layout
        leftSquareLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        leftSquareLayoutParams.addRule(RelativeLayout.BELOW, peopleTextView.getId());
        leftSquareLayoutParams.setMargins(dpToPx(15), dpToPx(10), dpToPx(20), dpToPx(15)); // Add margin from the left edge
        leftSquareLayout.setLayoutParams(leftSquareLayoutParams);

        ImageView leftSquare = new ImageView(this);
        leftSquare.setBackgroundColor(Color.GRAY);
        leftSquare.setId(View.generateViewId());
        RelativeLayout.LayoutParams leftSquareParams = new RelativeLayout.LayoutParams(dpToPx(80), dpToPx(80)); // Size of the left square
        leftSquare.setLayoutParams(leftSquareParams);

        TextView categoryTextView = new TextView(this);
        categoryTextView.setText(categorie);
        categoryTextView.setId(View.generateViewId());
        categoryTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        RelativeLayout.LayoutParams categoryTextParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        categoryTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL); // Center the category text horizontally
        categoryTextParams.addRule(RelativeLayout.BELOW, leftSquare.getId());
        categoryTextParams.setMargins(0, dpToPx(5), 0, dpToPx(20)); // Add margin between square and text
        categoryTextView.setLayoutParams(categoryTextParams);

        leftSquareLayout.addView(leftSquare);
        leftSquareLayout.addView(categoryTextView);

        orderLayout.addView(leftSquareLayout);

        // Create and add the right square with timer
        RelativeLayout rightSquareLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams rightSquareLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT); // Set WRAP_CONTENT for the layout
        rightSquareLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        rightSquareLayoutParams.addRule(RelativeLayout.BELOW, peopleTextView.getId());
        rightSquareLayoutParams.setMargins(dpToPx(20), dpToPx(10), dpToPx(15), dpToPx(20)); // Add margin from the right edge
        rightSquareLayout.setLayoutParams(rightSquareLayoutParams);

        ImageView rightSquare = new ImageView(this);
        rightSquare.setBackgroundColor(Color.GRAY);
        rightSquare.setId(View.generateViewId());
        RelativeLayout.LayoutParams rightSquareParams = new RelativeLayout.LayoutParams(dpToPx(80), dpToPx(80)); // Size of the right square
        rightSquare.setLayoutParams(rightSquareParams);

        TextView timerTextView = new TextView(this);
        timerTextView.setId(View.generateViewId());
        timerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        RelativeLayout.LayoutParams timerTextParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        timerTextParams.addRule(RelativeLayout.CENTER_IN_PARENT); // Center the timer text inside the square
        timerTextView.setLayoutParams(timerTextParams);

        rightSquareLayout.addView(rightSquare);
        rightSquareLayout.addView(timerTextView);

        TextView timeLabelTextView = new TextView(this);
        timeLabelTextView.setText("time");
        timeLabelTextView.setId(View.generateViewId());
        timeLabelTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        RelativeLayout.LayoutParams timeLabelTextParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        timeLabelTextParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        timeLabelTextParams.addRule(RelativeLayout.BELOW, rightSquare.getId()); // שינוי זה מוודא שהטקסט יופיע מתחת לריבוע של הטיימר
        timeLabelTextParams.setMargins(0, dpToPx(5), 0, dpToPx(15)); // הוספת מרווח בין הריבוע לטקסט
        timeLabelTextView.setLayoutParams(timeLabelTextParams);

        rightSquareLayout.addView(timeLabelTextView);

        orderLayout.addView(rightSquareLayout);

        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000;
                    long minutes = seconds / 60;
                    long hours = minutes / 60;
                    long days = hours / 24;

                    seconds = seconds % 60;
                    minutes = minutes % 60;
                    hours = hours % 24;

                    timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d:%02d", days, hours, minutes, seconds));
                }

                public void onFinish() {
                    timerTextView.setText("זמן נגמר");
                }
            }.start();
        } else {
            timerTextView.setText("זמן נגמר");
        }

        // Create and add the location
        TextView locationTextView = new TextView(this);
        locationTextView.setText(location);
        locationTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams locationParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        locationParams.addRule(RelativeLayout.BELOW, rightSquareLayout.getId());
        locationParams.addRule(RelativeLayout.BELOW, leftSquareLayout.getId());
        locationParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        locationParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        locationParams.setMargins(0, dpToPx(20), 0, 0); // Add margin to prevent touching the squares
        locationTextView.setLayoutParams(locationParams);
        orderLayout.addView(locationTextView);

        // Add the order layout to the container
        ordersContainer.addView(orderLayout);
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
