package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyOrdersActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        db = FirebaseFirestore.getInstance();

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        readUserOrders();
    }

    private void readUserOrders() {
        // Get a reference to the collection of orders
        CollectionReference ordersRef = db.collection("orders");

        // Get the current user's email
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = currentUser.getEmail();

        // Query orders where the user_email field equals the user's email (Orders I Open)
        Query userOrdersQuery = ordersRef.whereEqualTo("user_email", userEmail);

        // Execute the query for orders I open
        userOrdersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Get the context
                Context context = MyOrdersActivity.this;

                // Get the openedOrdersLayout
                LinearLayout openedOrdersLayout = findViewById(R.id.openedOrdersLayout);

                // Iterate through each document snapshot
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    // Get order details from each document snapshot
                    String orderId = documentSnapshot.getId();
                    String categorie = documentSnapshot.getString("categorie");
                    String description = documentSnapshot.getString("description");
                    GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                    double latitude = geoPoint.getLatitude(); // Extracting latitude
                    double longitude = geoPoint.getLongitude(); // Extracting longitude
                    String time = "";
                    // Check if "time" field exists and is of type string
                    if (documentSnapshot.contains("time") && documentSnapshot.get("time") instanceof String) {
                        // Get the time string
                        time = documentSnapshot.getString("time");

                        // Perform any further operations with the time string
                    } else {
                        // Handle the case where the "time" field either doesn't exist or is not of type string
                        if (documentSnapshot.contains("time")) {
                            Timestamp timestamp = documentSnapshot.getTimestamp("time");
                            if (timestamp != null) {
                                Date date = timestamp.toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                time = sdf.format(date);
                            }
                        }
                    }

                    String url = documentSnapshot.getString("URL");

                    // Create a new LinearLayout for order details
                    LinearLayout orderLayout = new LinearLayout(context);
                    orderLayout.setOrientation(LinearLayout.VERTICAL);

                    // Add order details to the layout


                    TextView categoryTextView = new TextView(context);
                    categoryTextView.setText("Categorie: " + categorie);
                    orderLayout.addView(categoryTextView);

                    TextView descriptionTextView = new TextView(context);
                    descriptionTextView.setText("Description: " + description);
                    orderLayout.addView(descriptionTextView);

                    TextView locationTextView = new TextView(context);
                    locationTextView.setText("Location: " + latitude + ", " + longitude);
                    orderLayout.addView(locationTextView);

                    // Add time if it's not empty
                    if (!time.isEmpty()) {
                        TextView timeTextView = new TextView(context);
                        timeTextView.setText("Time: " + time);
                        orderLayout.addView(timeTextView);
                    }

                    // Add URL if it's not empty
                    if (url != null && !url.isEmpty()) {
                        TextView urlTextView = new TextView(context);
                        urlTextView.setText("URL: " + url);
                        orderLayout.addView(urlTextView);
                    }

                    // Create a new CardView
                    CardView cardView = new CardView(context);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.setMargins(0, 0, 0, 16); // Add margin between CardViews
                    cardView.setLayoutParams(layoutParams);
                    cardView.setRadius(10); // Rounded corners for the CardView
                    cardView.setCardElevation(8); // Shadow for the CardView
                    cardView.setCardBackgroundColor(Color.WHITE); // Background color of the CardView

                    // Set padding for views inside the CardView
                    cardView.setPadding(16, 16, 16, 16);

                    // Add order details layout to the CardView
                    cardView.addView(orderLayout);

                    // Add an OnClickListener to the CardView
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
                            intent.putExtra("orderId", orderId);
                            startActivity(intent);
                        }
                    });

                    // Add the CardView to the openedOrdersLayout
                    openedOrdersLayout.addView(cardView);
                }
            }
        });

        // Query all orders where the user's email is in the listPeopleInOrder array (Orders I Joined)
        Query joinedOrdersQuery = ordersRef.whereArrayContains("listPeopleInOrder", userEmail);

        // Execute the query for orders I joined
        joinedOrdersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Get the context
                Context context = MyOrdersActivity.this;

                // Get the joinedOrdersLayout
                LinearLayout joinedOrdersLayout = findViewById(R.id.joinedOrdersLayout);

                // Iterate through each document snapshot
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    // Get the user_email of the order
                    String orderUserEmail = documentSnapshot.getString("user_email");

                    // Skip orders where the current user's email is the same as the order user_email
                    if (userEmail.equals(orderUserEmail)) {
                        continue;
                    }

                    // Get order details from each document snapshot
                    String orderId = documentSnapshot.getId();
                    String categorie = documentSnapshot.getString("categorie");
                    String description = documentSnapshot.getString("description");
                    GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                    double latitude = geoPoint.getLatitude(); // Extracting latitude
                    double longitude = geoPoint.getLongitude(); // Extracting longitude
                    String time = "";
                    if (documentSnapshot.contains("time")) {
                        Timestamp timestamp = documentSnapshot.getTimestamp("time");
                        if (timestamp != null) {
                            Date date = timestamp.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                            time = sdf.format(date);
                        }
                    }
                    String url = documentSnapshot.getString("URL");

                    // Create a new LinearLayout for order details
                    LinearLayout orderLayout = new LinearLayout(context);
                    orderLayout.setOrientation(LinearLayout.VERTICAL);

                    // Add order details to the layout


                    TextView categoryTextView = new TextView(context);
                    categoryTextView.setText("Categorie: " + categorie);
                    orderLayout.addView(categoryTextView);

                    TextView descriptionTextView = new TextView(context);
                    descriptionTextView.setText("Description: " + description);
                    orderLayout.addView(descriptionTextView);

                    TextView locationTextView = new TextView(context);
                    locationTextView.setText("Location: " + latitude + ", " + longitude);
                    orderLayout.addView(locationTextView);

                    // Add time if it's not empty
                    if (!time.isEmpty()) {
                        TextView timeTextView = new TextView(context);
                        timeTextView.setText("Time: " + time);
                        orderLayout.addView(timeTextView);
                    }

                    // Add URL if it's not empty
                    if (url != null && !url.isEmpty()) {
                        TextView urlTextView = new TextView(context);
                        urlTextView.setText("URL: " + url);
                        orderLayout.addView(urlTextView);
                    }

                    // Create a new CardView
                    CardView cardView = new CardView(context);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    layoutParams.setMargins(0, 0, 0, 16); // Add margin between CardViews
                    cardView.setLayoutParams(layoutParams);
                    cardView.setRadius(10); // Rounded corners for the CardView
                    cardView.setCardElevation(8); // Shadow for the CardView
                    cardView.setCardBackgroundColor(Color.WHITE); // Background color of the CardView

                    // Set padding for views inside the CardView
                    cardView.setPadding(16, 16, 16, 16);

                    // Add order details layout to the CardView
                    cardView.addView(orderLayout);

                    // Add an OnClickListener to the CardView
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
                            intent.putExtra("orderId", orderId);
                            startActivity(intent);
                        }
                    });

                    // Add the CardView to the joinedOrdersLayout
                    joinedOrdersLayout.addView(cardView);
                }
            }
        });
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
        Intent toy = new Intent(MyOrdersActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(MyOrdersActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(MyOrdersActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(MyOrdersActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(MyOrdersActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(MyOrdersActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(MyOrdersActivity.this, MainActivity.class);
        startActivity(toy);
    }

    public void openOrder(View v) {
        Intent toy = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
        startActivity(toy);
    }
}
