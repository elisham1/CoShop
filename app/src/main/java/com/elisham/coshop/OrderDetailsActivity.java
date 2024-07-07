package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView descriptionTextView, siteTextView, categoryTextView, addressTextView, timeTextView, titleTextView, groupInfoTextView;
    private ImageView categoryImageView;
    private Button joinButton;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Geocoder geocoder;
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        menuUtils = new MenuUtils(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        geocoder = new Geocoder(this, Locale.ENGLISH);

        // Initialize TextViews and ImageView
        descriptionTextView = findViewById(R.id.descriptionTextView);
        siteTextView = findViewById(R.id.siteTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        addressTextView = findViewById(R.id.addressTextView);
        timeTextView = findViewById(R.id.timeTextView);
        titleTextView = findViewById(R.id.titleTextView);
        groupInfoTextView = findViewById(R.id.groupInfoTextView);
        categoryImageView = findViewById(R.id.categoryImageView);
        joinButton = findViewById(R.id.joinButton);

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        Toast.makeText(this, "Order ID: " + orderId, Toast.LENGTH_SHORT).show();
        fetchOrderDetails(orderId);

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
    }

    private void fetchOrderDetails(String orderId) {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Fetch order details from the document
                String categorie = documentSnapshot.getString("categorie");
                String description = documentSnapshot.getString("description");
                GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                double latitude = geoPoint.getLatitude();
                double longitude = geoPoint.getLongitude();
                String address = getAddressFromLatLng(latitude, longitude);
                Timestamp timestamp = documentSnapshot.getTimestamp("time");
                int numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder").intValue();
                int maxPeople = documentSnapshot.getLong("max_people").intValue();
                String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                String userEmail = documentSnapshot.getString("user_email");
                Timestamp openOrderTime = documentSnapshot.getTimestamp("openOrderTime");
                String siteUrl = documentSnapshot.getString("URL");

                // Convert timestamps to Date objects
                Date date = timestamp.toDate();
                Date openOrderDate = openOrderTime.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String formattedTime = sdf.format(date);
                String formattedOpenOrderTime = sdf.format(openOrderDate);

                // Update TextViews and ImageView with order details
                descriptionTextView.setText("Group description:\n" + description);
                siteTextView.setText("Url/Site: " + siteUrl);
                categoryTextView.setText(categorie);
                addressTextView.setText("Address: " + address);
                // Hide timeTextView and peopleTextView by not setting text
                timeTextView.setText("");
                titleTextView.setText(titleOfOrder);

                // Fetch user details and update groupInfoTextView
                fetchUserDetails(userEmail, numberOfPeopleInOrder, maxPeople, formattedOpenOrderTime);

                // Load icon from URL
                String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
                Glide.with(this)
                        .load(iconUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.star)
                        .error(R.drawable.star2)
                        .into(categoryImageView);
            } else {
                Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch document", Toast.LENGTH_SHORT).show());
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

    private void fetchUserDetails(String userEmail, int numberOfPeopleInOrder, int maxPeople, String formattedOpenOrderTime) {
        DocumentReference userRef = db.collection("users").document(userEmail);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("first name");
                String familyName = documentSnapshot.getString("family name");

                if (firstName != null && familyName != null) {
                    groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + firstName + " " + familyName + "\non " + formattedOpenOrderTime);
                } else {
                    groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
                    Log.e("OrderDetailsActivity", "First name or family name is null for user: " + userEmail);
                }
            } else {
                groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
                Log.e("OrderDetailsActivity", "No document found for user: " + userEmail);
            }
        }).addOnFailureListener(e -> {
            groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
            Log.e("OrderDetailsActivity", "Failed to fetch user details", e);
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
        });
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
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            List<String> listPeopleInOrder = (List<String>) snapshot.get("listPeopleInOrder");
            if (listPeopleInOrder != null && !listPeopleInOrder.contains(currentUser.getEmail())) {
                transaction.update(orderRef, "listPeopleInOrder", FieldValue.arrayUnion(currentUser.getEmail()));
                Long currentNumberOfPeople = snapshot.getLong("NumberOfPeopleInOrder");
                transaction.update(orderRef, "NumberOfPeopleInOrder", currentNumberOfPeople + 1);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Added to order", Toast.LENGTH_SHORT).show();
            joinButton.setText("Chat");
            Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("orderId", orderId);
            startActivity(chatIntent);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to add user to order", Toast.LENGTH_SHORT).show());
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
