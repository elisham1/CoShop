package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

// Handles chat functionality for the app
public class ChatActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageView sendIcon, orderIcon;
    private TextView orderTitle;
    private String orderId, globalUserType;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private LinearLayout orderDetailsLayout;

    private ChatAdapter chatAdapter;
    private ArrayList<ChatItem> chatMessages; // Change to ChatItem

    private MenuUtils menuUtils;
    private ListenerRegistration chatListener;

    // Initializes the activity and sets the theme based on user type
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
        setContentView(R.layout.activity_chat);
        // Get the orderId from the intent
        orderId = intent.getStringExtra("orderId");
        Log.d("ChatActivity", "Order ID: " + orderId);
        initializeUI();
    }

    private void initializeUI() {
        menuUtils = new MenuUtils(this, globalUserType);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize chat components
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendIcon = findViewById(R.id.sendIcon);
        orderDetailsLayout = findViewById(R.id.orderDetailsLayout);
        orderIcon = findViewById(R.id.orderIcon);
        orderTitle = findViewById(R.id.orderTitle);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages); // Pass the correct type
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        loadChatMessages(orderId);

        orderDetailsLayout.setOnClickListener(v -> {
            Intent orderDetailsIntent = new Intent(ChatActivity.this, OrderDetailsActivity.class);
            orderDetailsIntent.putExtra("userType", globalUserType);
            orderDetailsIntent.putExtra("orderId", orderId);
            startActivity(orderDetailsIntent);
        });

        db.collection("orders").document(orderId).get().
                addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        orderTitle.setText(documentSnapshot.getString("titleOfOrder"));
                        String categorie = documentSnapshot.getString("categorie");
                        String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
                        Glide.with(this)
                                .load(iconUrl)
                                .error(R.drawable.star2)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .override(100, 100) // Adjust according to your ImageView size
                                .into(orderIcon);
                    }
                });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    sendIcon.setVisibility(View.VISIBLE);
                } else {
                    sendIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        sendIcon.setOnClickListener(v -> sendMessage());
    }

    // Loads chat messages from Firestore for the given order ID
    private void loadChatMessages(String orderId) {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatRef.orderBy("timestamp").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.d("ChatActivity", "Error loading chat messages");
                return;
            }

            chatMessages.clear();
            if (snapshots != null) {
                Timestamp lastTimestamp = null;
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String message = doc.getString("message");
                    String sender = doc.getString("sender");
                    Timestamp timestamp = doc.getTimestamp("timestamp");

                    if (lastTimestamp == null || !isSameDay(lastTimestamp, timestamp)) {
                        chatMessages.add(new DateHeader(timestamp));
                    }

                    chatMessages.add(new ChatMessage(sender, message, timestamp));
                    lastTimestamp = timestamp;
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            } else {
                Log.d("ChatActivity", "No chat messages found");
            }
        });
    }

    // Checks if two timestamps are on the same day
    private boolean isSameDay(Timestamp t1, Timestamp t2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(t1.toDate()).equals(fmt.format(t2.toDate()));
    }

    // Sends a chat message and updates Firestore
    private void sendMessage() {
        String messageText = messageInput.getText().toString();
        if (messageText.isEmpty()) {
            Log.d("ChatActivity", "Message text is empty");
            return;
        }
        if (currentUser == null) {
            Log.d("ChatActivity", "Current user is null");
            return;
        }
        String userEmail = currentUser.getEmail();

        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");

        chatRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean addDateHeader = true;
                for (DocumentSnapshot document : task.getResult()) {
                    Timestamp lastTimestamp = document.getTimestamp("timestamp");
                    if (lastTimestamp != null) {
                        Date lastDate = lastTimestamp.toDate();
                        Date currentDate = new Date();
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
                        addDateHeader = !fmt.format(lastDate).equals(fmt.format(currentDate));
                    }
                }

                WriteBatch batch = db.batch();
                if (addDateHeader) {
                    Map<String, Object> dateHeader = new HashMap<>();
                    dateHeader.put("type", "dateHeader");
                    dateHeader.put("date", new Timestamp(new Date()));
                    chatRef.add(dateHeader);
                }

                Map<String, Object> chatMessage = new HashMap<>();
                chatMessage.put("sender", userEmail);
                chatMessage.put("message", messageText);
                chatMessage.put("timestamp", new Timestamp(new Date()));
                chatMessage.put("readBy", Collections.singletonList(userEmail));

                chatRef.add(chatMessage).addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }).addOnFailureListener(e -> Log.d("ChatActivity", "Failed to send message"));
            }
        });
    }

    // Updates the read status of chat messages in real-time
    private void updateReadStatusInRealtime() {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatListener = chatRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.d("ChatActivity", "Error updating read status");
                    return;
                }

                if (snapshots != null) {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        List<String> readBy = (List<String>) document.get("readBy");
                        if (readBy == null) {
                            readBy = new ArrayList<>();
                        }
                        if (!readBy.contains(currentUser.getEmail())) {
                            readBy.add(currentUser.getEmail());
                            batch.update(document.getReference(), "readBy", readBy);
                        }
                    }
                    batch.commit().addOnCompleteListener(batchTask -> {
                        if (!batchTask.isSuccessful()) {
                            Log.d("ChatActivity", "Failed to update read status");
                        }
                    });
                }
            }
        });
    }

    // Starts the activity and updates the read status for all messages
    @Override
    protected void onStart() {
        super.onStart();
        updateReadStatusForAllMessages();
    }

    // Updates the read status for all chat messages
    private void updateReadStatusForAllMessages() {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot document : task.getResult()) {
                    if (document.exists()) {
                        List<String> readBy = (List<String>) document.get("readBy");
                        if (readBy == null) {
                            readBy = new ArrayList<>();
                        }
                        if (!readBy.contains(currentUser.getEmail())) {
                            readBy.add(currentUser.getEmail());
                            batch.update(document.getReference(), "readBy", readBy);
                        }
                    }
                }
                batch.commit().addOnCompleteListener(batchTask -> {
                    if (!batchTask.isSuccessful()) {
                        Log.d("ChatActivity", "Failed to update read status");
                    }
                });
            }
        });
    }

    // Resumes the activity and updates the read status in real-time
    @Override
    protected void onResume() {
        super.onResume();
        updateReadStatusInRealtime();
    }

    // Pauses the activity and removes the chat listener
    @Override
    protected void onPause() {
        super.onPause();
        if (chatListener != null) {
            chatListener.remove();
        }
    }

    // Gets the profile image URL for a given sender email
    private void getProfileImageUrl(String senderEmail, ProfileImageCallback callback) {
        DocumentReference userRef = db.collection("users").document(senderEmail);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageUrl = documentSnapshot.getString("profileImageUrl");
                callback.onCallback(imageUrl);
            } else {
                callback.onCallback(null);
            }
        }).addOnFailureListener(e -> callback.onCallback(null));
    }

    // Callback interface for profile image URL
    private interface ProfileImageCallback {
        void onCallback(String imageUrl);
    }

    // Inflates the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
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
