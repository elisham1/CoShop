package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.drawable.ColorDrawable;

public class MyOrdersActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private MenuUtils menuUtils;
    private LinearLayout ordersLayout;
    private String userEmail;
    private Button btnAllOrders;
    private Button btnOpenedOrders;
    private Button btnJoinedOrders;
    private static final String ALL_ORDERS = "All My Orders";
    private static final String OPENED_ORDERS = "Orders I Opened";
    private static final String JOINED_ORDERS = "Orders I Joined";
    private String currentOption = ALL_ORDERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        db = FirebaseFirestore.getInstance();
        menuUtils = new MenuUtils(this);
        ordersLayout = findViewById(R.id.ordersLayout);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        userEmail = currentUser.getEmail();

        btnAllOrders = findViewById(R.id.btnAllOrders);
        btnOpenedOrders = findViewById(R.id.btnOpenedOrders);
        btnJoinedOrders = findViewById(R.id.btnJoinedOrders);

        // Set default view to All Orders
        readUserOrders(ALL_ORDERS);
        updateButtonColors(ALL_ORDERS);

        btnAllOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentOption = ALL_ORDERS;
                readUserOrders(ALL_ORDERS);
            }
        });

        btnOpenedOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentOption = OPENED_ORDERS;
                readUserOrders(OPENED_ORDERS);
            }
        });

        btnJoinedOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentOption = JOINED_ORDERS;
                readUserOrders(JOINED_ORDERS);
            }
        });
    }

    private void readUserOrders(String option) {

        // Clear the existing orders
        ordersLayout.removeAllViews();

        updateButtonColors(option);

        CollectionReference ordersRef = db.collection("orders");

        // Query orders where the user's email is in the listPeopleInOrder array
        Query allOrdersQuery = ordersRef.whereArrayContains("listPeopleInOrder", userEmail);
        allOrdersQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<DocumentSnapshot> allOrdersList = new ArrayList<>();
                List<DocumentSnapshot> openedOrdersList = new ArrayList<>();
                List<DocumentSnapshot> joinedOrdersList = new ArrayList<>();

                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    allOrdersList.add(documentSnapshot);
                    String orderUserEmail = documentSnapshot.getString("user_email");
                    if (userEmail.equals(orderUserEmail)) {
                        openedOrdersList.add(documentSnapshot);
                    } else {
                        joinedOrdersList.add(documentSnapshot);
                    }
                }

                switch (option) {
                    case ALL_ORDERS:
                        displayOrders(allOrdersList);
                        break;
                    case OPENED_ORDERS:
                        displayOrders(openedOrdersList);
                        break;
                    case JOINED_ORDERS:
                        displayOrders(joinedOrdersList);
                        break;
                }
            }
        });
    }

    private void displayOrders(List<DocumentSnapshot> orders) {
        Context context = MyOrdersActivity.this;

        for (DocumentSnapshot documentSnapshot : orders) {
            String orderId = documentSnapshot.getId();
            String categorie = documentSnapshot.getString("categorie");
            String description = documentSnapshot.getString("description");
            GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
            double latitude = geoPoint.getLatitude();
            double longitude = geoPoint.getLongitude();
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

            LinearLayout orderLayout = new LinearLayout(context);
            orderLayout.setOrientation(LinearLayout.VERTICAL);

            TextView categoryTextView = new TextView(context);
            categoryTextView.setText("Categorie: " + categorie);
            orderLayout.addView(categoryTextView);

            TextView descriptionTextView = new TextView(context);
            descriptionTextView.setText("Description: " + description);
            orderLayout.addView(descriptionTextView);

            TextView locationTextView = new TextView(context);
            locationTextView.setText("Location: " + latitude + ", " + longitude);
            orderLayout.addView(locationTextView);

            if (!time.isEmpty()) {
                TextView timeTextView = new TextView(context);
                timeTextView.setText("Time: " + time);
                orderLayout.addView(timeTextView);
            }

            if (url != null && !url.isEmpty()) {
                TextView urlTextView = new TextView(context);
                urlTextView.setText("URL: " + url);
                orderLayout.addView(urlTextView);
            }

            CardView cardView = new CardView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(layoutParams);
            cardView.setRadius(10);
            cardView.setCardElevation(8);
            cardView.setCardBackgroundColor(Color.WHITE);
            cardView.setPadding(16, 16, 16, 16);

            cardView.addView(orderLayout);

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
                    intent.putExtra("orderId", orderId);
                    startActivity(intent);
                }
            });

            ordersLayout.addView(cardView);
        }
    }

    private void updateButtonColors(String selectedOption) {
        int selectedColor = getResources().getColor(R.color.colorSelected);
        int defaultColor = getResources().getColor(R.color.colorDefault);

        btnAllOrders.setBackgroundColor(defaultColor);
        btnOpenedOrders.setBackgroundColor(defaultColor);
        btnJoinedOrders.setBackgroundColor(defaultColor);

        switch (selectedOption) {
            case ALL_ORDERS:
                btnAllOrders.setBackgroundColor(selectedColor);
                break;
            case OPENED_ORDERS:
                btnOpenedOrders.setBackgroundColor(selectedColor);
                break;
            case JOINED_ORDERS:
                btnJoinedOrders.setBackgroundColor(selectedColor);
                break;
        }
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

    public void openOrder(View v) {
        Intent toy = new Intent(MyOrdersActivity.this, OrderDetailsActivity.class);
        startActivity(toy);
    }

}
