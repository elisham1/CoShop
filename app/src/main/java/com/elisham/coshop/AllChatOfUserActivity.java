package com.elisham.coshop;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

// Displays all chat orders of the user
public class AllChatOfUserActivity extends AppCompatActivity {
    private MenuUtils menuUtils;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private RecyclerView chatRecyclerView;
    private ChatListAdapter chatListAdapter;
    private List<ChatOrder> chatOrders;
    private String globalUserType;
    private ProgressDialog progressDialog;
    // Initializes the activity, sets the theme based on user type, and loads user chats
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }

        setContentView(R.layout.activityallchats);
        initializeUI();
    }

    private void initializeUI() {
        menuUtils = new MenuUtils(this, globalUserType);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatOrders = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(this, chatOrders, this::onChatSelected);
        chatRecyclerView.setAdapter(chatListAdapter);

        loadUserChats();
        invalidateOptionsMenu(); // Update the menu
    }

    // Loads the user's chat orders from Firestore
    private void loadUserChats() {
        if (currentUser == null) {
            Log.d("AllChatOfUserActivity", "User not logged in");
            return;
        }

        String userEmail = currentUser.getEmail();
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d("AllChatOfUserActivity", "Error getting orders");
                    return;
                }

                if (snapshots != null) {
                    chatOrders.clear(); // Clear the list to avoid duplicate additions
                    for (DocumentSnapshot doc : snapshots) {
                        List<String> peopleInOrder = (List<String>) doc.get("listPeopleInOrder");
                        if (peopleInOrder != null && peopleInOrder.contains(userEmail)) {
                            String orderId = doc.getId();
                            loadLastMessageTime(orderId);
                        }
                    }
                }
            }
        });
    }

    // Loads the last message time for a specific order
    private void loadLastMessageTime(String orderId) {
        db.collection("orders").document(orderId).collection("chat")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        // Remove the existing order with the same orderId
                        for (int i = 0; i < chatOrders.size(); i++) {
                            if (chatOrders.get(i).getOrderId().equals(orderId)) {
                                chatOrders.remove(i);
                                break;
                            }
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            DocumentSnapshot lastMessage = snapshots.getDocuments().get(0);
                            Long lastMessageTimestamp = lastMessage.getTimestamp("timestamp").getSeconds();
                            chatOrders.add(new ChatOrder(orderId, lastMessageTimestamp));
                        } else {
                            chatOrders.add(new ChatOrder(orderId, 0L));
                        }
                        sortAndNotifyAdapter();
                    }
                });
    }

    // Sorts chat orders by last message time and notifies the adapter
    private void sortAndNotifyAdapter() {
        Collections.sort(chatOrders, new Comparator<ChatOrder>() {
            @Override
            public int compare(ChatOrder o1, ChatOrder o2) {
                return Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp());
            }
        });
        chatListAdapter.notifyDataSetChanged();
    }

    // Handles the chat selection and navigates to ChatActivity
    private void onChatSelected(String orderId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userType", globalUserType);
        intent.putExtra("orderId", orderId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeUI();
    }

    // Inflates the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);
        if ("Supplier".equals(globalUserType)) {
            MenuItem item = menu.findItem(R.id.chat_notification);
            if (item != null) {
                item.setVisible(false);
            }
        }
        return true;
    }

    // Handles item selections in the options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo(); // Navigate to Personal Info
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders(); // Navigate to My Orders
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs(); // Navigate to About Us
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs(); // Navigate to Contact Us
                return true;
            case R.id.Log_Out:
                menuUtils.logOut(); // Log out user
                return true;
            case R.id.home:
                menuUtils.home(); // Navigate to Home
                return true;
            case R.id.chat_icon:
                menuUtils.allChats(); // Navigate to All Chats
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification(); // Navigate to Chat Notification
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
