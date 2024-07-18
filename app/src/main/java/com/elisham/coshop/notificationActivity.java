package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class notificationActivity extends AppCompatActivity {

    private MenuUtils menuUtils;
    private ListView notificationsListView;
    private ArrayAdapter<String> adapter;
    private List<String> notificationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        menuUtils = new MenuUtils(this);
        notificationsListView = findViewById(R.id.notificationsListView);
        notificationsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notificationsList);
        notificationsListView.setAdapter(adapter);

        // קבלת נתונים מההזמנה החדשה
        String category = getIntent().getStringExtra("category");
        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);

        if (category != null && latitude != 0 && longitude != 0) {
            checkAndNotifyUsers(category, latitude, longitude);
        }

        // שליפת הודעות מה-collection ב-Firestore
        loadNotifications();
    }

    private void checkAndNotifyUsers(String category, double latitude, double longitude) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersRef = db.collection("users");
        GeoPoint orderLocation = new GeoPoint(latitude, longitude);

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot userDocument : task.getResult()) {
                    List<String> favoriteCategories = (List<String>) userDocument.get("favorite categories");
                    Log.d("NotificationActivity", "User favorite categories: " + favoriteCategories);

                    // בדיקת סוג השדה 'address' לפני השליפה
                    if (userDocument.get("address") instanceof GeoPoint) {
                        GeoPoint userLocation = userDocument.getGeoPoint("address");
                        Log.d("NotificationActivity", "User location: " + userLocation);
                        double maxDistance = 10.0; // נניח טווח של 10 קילומטרים, ניתן לשנות לפי הצורך

                        if (favoriteCategories.contains(category) && isWithinDistance(orderLocation, userLocation, maxDistance)) {
                            String userEmail = userDocument.getString("email");
                            String notificationMessage = "הזמנה חדשה בתחום שמעניין אותך!";
                            Log.d("NotificationActivity", "Sending notification to: " + userEmail);

                            // שמירת הודעה ב-Firestore בתת-collection של המשתמש
                            CollectionReference notificationsRef = usersRef.document(userDocument.getId()).collection("notifications");
                            Map<String, Object> notificationData = new HashMap<>();
                            notificationData.put("message", notificationMessage);
                            notificationData.put("userEmail", userEmail);
                            notificationData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                            notificationsRef.add(notificationData)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d("Firestore", "Notification added successfully for user: " + userEmail);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error adding notification for user: " + userEmail, e);
                                    });
                        }
                    } else {
                        Log.w("Firestore", "User document does not contain a GeoPoint address");
                    }
                }
            } else {
                Log.w("Firestore", "Error getting documents.", task.getException());
            }
        });
    }

    private boolean isWithinDistance(GeoPoint point1, GeoPoint point2, double maxDistance) {
        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        double distance = haversine(lat1, lon1, lat2, lon2);
        Log.d("NotificationActivity", "Calculated distance: " + distance);
        return distance <= maxDistance;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // רדיוס כדור הארץ בקילומטרים
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void loadNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference notificationsRef = db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("notifications");

        notificationsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String message = document.getString("message");
                    notificationsList.add(message);
                    Log.d("NotificationActivity", "Loaded notification: " + message);
                }
                adapter.notifyDataSetChanged();
            } else {
                Log.w("Firestore", "Error getting documents.", task.getException());
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
                loadNotifications(); // קריאה ל-loadNotifications כדי לעדכן את ההתראות
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
