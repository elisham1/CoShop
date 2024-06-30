package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class OrderDetailsActivity extends AppCompatActivity {

    private MenuUtils menuUtils;
    private FirebaseFirestore db;
    private TextView descriptionTextView, siteTextView, categoryTextView, addressTextView, peopleTextView, timeTextView;
    private Button joinButton, closeButton;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        menuUtils = new MenuUtils(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize TextViews
        descriptionTextView = findViewById(R.id.descriptionTextView);
        siteTextView = findViewById(R.id.siteTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        addressTextView = findViewById(R.id.addressTextView);
        peopleTextView = findViewById(R.id.peopleTextView);
        timeTextView = findViewById(R.id.timeTextView);
        joinButton = findViewById(R.id.joinButton);
        closeButton = findViewById(R.id.closeButton);

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        Toast.makeText(this, "Order ID: " + orderId, Toast.LENGTH_SHORT).show();
        fetchOrderDetails(orderId);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Check if the user is already in the list
        checkUserInList();

        joinButton.setOnClickListener(v -> {
            if (joinButton.getText().toString().equals("Join")) {
                addUserToOrder();
            } else {
                Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
                chatIntent.putExtra("orderId", orderId);
                startActivity(chatIntent);
            }
        });

        closeButton.setOnClickListener(v -> finish());
    }

    private void fetchOrderDetails(String orderId) {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get order details
                String categorie = documentSnapshot.getString("categorie");
                String description = documentSnapshot.getString("description");
                GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                double latitude = geoPoint.getLatitude();
                double longitude = geoPoint.getLongitude();
                Timestamp timestamp = documentSnapshot.getTimestamp("time");
                String address = latitude + ", " + longitude;
                int peopleInOrder = documentSnapshot.getLong("max_people").intValue();

                // Convert timestamp to Date object
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String formattedTime = sdf.format(date);

                // Update the TextViews with order details
                descriptionTextView.setText("Description: " + description);
                siteTextView.setText("Site: " + address);
                categoryTextView.setText("Category: " + categorie);
                addressTextView.setText("Address: " + address);
                peopleTextView.setText("People in Order: " + peopleInOrder);
                timeTextView.setText("Time: " + formattedTime);
            } else {
                Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch document", Toast.LENGTH_SHORT).show());
    }

    private void checkUserInList() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> listPeopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
                if (listPeopleInOrder != null && listPeopleInOrder.contains(currentUser.getEmail())) {
                    joinButton.setText("Chat");
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to check user in list", Toast.LENGTH_SHORT).show());
    }

    private void addUserToOrder() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.update("listPeopleInOrder", FieldValue.arrayUnion(currentUser.getEmail()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Added to order", Toast.LENGTH_SHORT).show();
                    joinButton.setText("Chat");
                    Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
                    chatIntent.putExtra("orderId", orderId);
                    startActivity(chatIntent);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add user to order", Toast.LENGTH_SHORT).show());
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

}
