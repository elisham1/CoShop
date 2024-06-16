package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HomePageActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout ordersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize the orders container
        ordersContainer = findViewById(R.id.ordersContainer);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Fetch and display orders
        fetchOrders();
    }

    private void fetchOrders() {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    String orderId = documentSnapshot.getId();
                    String categorie = documentSnapshot.getString("categorie");
                    String description = documentSnapshot.getString("description");
                    GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                    double latitude = geoPoint != null ? geoPoint.getLatitude() : 0;
                    double longitude = geoPoint != null ? geoPoint.getLongitude() : 0;
                    String time = "";
                    if (documentSnapshot.contains("time") && documentSnapshot.get("time") instanceof String) {
                        time = documentSnapshot.getString("time");
                    } else if (documentSnapshot.contains("time")) {
                        Timestamp timestamp = documentSnapshot.getTimestamp("time");
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                            time = sdf.format(date);
                        }
                    }
                    String url = documentSnapshot.getString("URL");

                    // Create and add the order view to the layout
                    addOrderToLayout(orderId, categorie, description, latitude, longitude, time, url);
                }
            }
        });
    }

    private void addOrderToLayout(String orderId, String categorie, String description, double latitude, double longitude, String time, String url) {
        // Create a new RelativeLayout for the order
        RelativeLayout orderLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams orderLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        orderLayoutParams.setMargins(0, 0, 0, 16); // Add margin between orders
        orderLayout.setLayoutParams(orderLayoutParams);
        orderLayout.setPadding(16, 16, 16, 16);
        orderLayout.setBackgroundColor(Color.LTGRAY);

        // Create and add the order details
        TextView orderTextView = new TextView(this);
        orderTextView.setText("Order: " + orderId);
        orderTextView.setId(View.generateViewId());
        orderTextView.setTextSize(20);
        RelativeLayout.LayoutParams orderTextParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        orderTextView.setLayoutParams(orderTextParams);
        orderLayout.addView(orderTextView);

        TextView categoryTextView = new TextView(this);
        categoryTextView.setText("Category: " + categorie);
        categoryTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams categoryParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        categoryParams.addRule(RelativeLayout.BELOW, orderTextView.getId());
        categoryTextView.setLayoutParams(categoryParams);
        orderLayout.addView(categoryTextView);

        TextView descriptionTextView = new TextView(this);
        descriptionTextView.setText("Description: " + description);
        descriptionTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams descriptionParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        descriptionParams.addRule(RelativeLayout.BELOW, categoryTextView.getId());
        descriptionTextView.setLayoutParams(descriptionParams);
        orderLayout.addView(descriptionTextView);

        TextView locationTextView = new TextView(this);
        locationTextView.setText("Location: " + latitude + ", " + longitude);
        locationTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams locationParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        locationParams.addRule(RelativeLayout.BELOW, descriptionTextView.getId());
        locationTextView.setLayoutParams(locationParams);
        orderLayout.addView(locationTextView);

        TextView timeTextView = new TextView(this);
        if (!time.isEmpty()) {
            timeTextView.setText("Time: " + time);
        } else {
            timeTextView.setText("Time: N/A");
        }
        timeTextView.setId(View.generateViewId());
        RelativeLayout.LayoutParams timeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        timeParams.addRule(RelativeLayout.BELOW, locationTextView.getId());
        timeTextView.setLayoutParams(timeParams);
        orderLayout.addView(timeTextView);

        if (url != null && !url.isEmpty()) {
            TextView urlTextView = new TextView(this);
            urlTextView.setText("URL: " + url);
            urlTextView.setId(View.generateViewId());
            RelativeLayout.LayoutParams urlParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            urlParams.addRule(RelativeLayout.BELOW, timeTextView.getId());
            urlTextView.setLayoutParams(urlParams);
            orderLayout.addView(urlTextView);
        }

        // Create and add buttons
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        buttonLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        buttonLayout.setLayoutParams(buttonLayoutParams);
        orderLayout.addView(buttonLayout);

        Button openButton = new Button(this);
        openButton.setText("Open");
        openButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });
        buttonLayout.addView(openButton);

        Button shareButton = new Button(this);
        shareButton.setText("Share");
        shareButton.setOnClickListener(v -> {
            String shareContent = "Order: " + orderId +
                    "\nCategory: " + categorie +
                    "\nDescription: " + description +
                    "\nLocation: " + latitude + ", " + longitude +
                    "\nTime: " + time +
                    (url != null ? "\nURL: " + url : "");

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            startActivity(Intent.createChooser(shareIntent, "Share order details via"));
        });
        buttonLayout.addView(shareButton);

        Button joinButton = new Button(this);
        joinButton.setText("Join");
        joinButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomePageActivity.this, JoinOrderActivity.class);
            intent.putExtra("orderId", orderId);
            startActivity(intent);
        });
        buttonLayout.addView(joinButton);

        // Add the order layout to the container
        ordersContainer.addView(orderLayout);
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
                onBackPressed();
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

    public void personalInfo() {
        Intent toy = new Intent(HomePageActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(HomePageActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(HomePageActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(HomePageActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(HomePageActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(HomePageActivity.this, MainActivity.class);
        startActivity(toy);
    }

    public void home() {
        Intent toy = new Intent(HomePageActivity.this, HomePageActivity.class);
        startActivity(toy);
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
