package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
public class OrderDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView descriptionTextView, siteTextView, categoryTextView, addressTextView, peopleTextView, timeTextView;

    public Button button5;

//    public void init() {
//        button5 = (Button) findViewById(R.id.button5);
//        button5.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                Intent toy = new Intent(OrderDetailsActivity.this, JoinOrderActivity.class);
//                startActivity(toy);
//            }
//        });
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        db = FirebaseFirestore.getInstance();

        // Initialize TextViews
        descriptionTextView = findViewById(R.id.descriptionTextView);
        siteTextView = findViewById(R.id.siteTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        addressTextView = findViewById(R.id.addressTextView);
        peopleTextView = findViewById(R.id.peopleTextView);
        timeTextView = findViewById(R.id.timeTextView);

        // Get the orderId from the intent
        Intent intent = getIntent();
        String orderId = intent.getStringExtra("orderId");
        Log.d("orderId",orderId);
        fetchOrderDetails(orderId);

//        init();
    }
    private void fetchOrderDetails(String orderId) {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Get order details
                    String categorie = documentSnapshot.getString("categorie");
                    String description = documentSnapshot.getString("description");
                    GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                    double latitude = geoPoint.getLatitude(); // Extracting latitude
                    double longitude = geoPoint.getLongitude(); // Extracting longitude
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    String address = latitude + ", " + longitude; // Use latitude and longitude as address
                    int peopleInOrder = documentSnapshot.getLong("max_people").intValue(); // Assuming people_in_order is an integer

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
                    Log.d("OrderDetailsActivity", "No such document");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("OrderDetailsActivity", "Failed to fetch document", e);
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
        Intent toy = new Intent(OrderDetailsActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(OrderDetailsActivity.this, UpdateUserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(OrderDetailsActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(OrderDetailsActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(OrderDetailsActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(OrderDetailsActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(OrderDetailsActivity.this, MainActivity.class);
        startActivity(toy);
    }

}