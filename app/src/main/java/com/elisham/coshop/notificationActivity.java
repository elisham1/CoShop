package com.elisham.coshop;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class notificationActivity extends AppCompatActivity {

    private MenuUtils menuUtils;
    private ListView notificationsListView;

    private ArrayAdapter<String> adapter;
    private List<String> notificationsList;
    private String globalUserType;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the theme based on the user type
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }

        setContentView(R.layout.activity_notification);

        // Initialize MenuUtils
         menuUtils = new MenuUtils(this,globalUserType);

        // Initialize ListView
        notificationsListView = findViewById(R.id.notificationsListView);
        notificationsList = new ArrayList<>();
        adapter = new SimpleAdapter(this, notificationsList, R.layout.notification_item,
                new String[]{"newOrder", "message", "linkButton", "timestamp"},
                new int[]{R.id.newOrder, R.id.message, R.id.linkButton, R.id.timestamp});
        adapter.setViewBinder((view, data, textRepresentation) -> {
            if (view.getId() == R.id.linkButton) {
                Button button = (Button) view;
                button.setTag(data.toString());
                button.setText("click to see order");
                button.setOnClickListener(this::openLink);
                return true;
            }
            return false;
        });
        notificationsListView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            fetchNotifications(currentUser.getEmail());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNotifications(String userEmail) {
        CollectionReference notificationsRef = db.collection("users").document(userEmail).collection("notifications");
        notificationsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        notificationsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String message = document.getString("message");
                            String link = document.getString("link");
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(document.getTimestamp("timestamp").toDate());

                            Map<String, Object> notification = new HashMap<>();
                            notification.put("newOrder", "New order");
                            notification.put("message", message);
                            notification.put("linkButton", link);
                            notification.put("timestamp", timestamp);

                            notificationsList.add(notification);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                    }
                });
    }

    public void openLink(View view) {
        String link = (String) view.getTag();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                menuUtils.chat_notification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
