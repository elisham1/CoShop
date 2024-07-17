package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class ChatActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageView sendIcon, orderIcon;
    private TextView orderTitle;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private LinearLayout orderDetailsLayout;

    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    private MenuUtils menuUtils;
    private ListenerRegistration chatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        menuUtils = new MenuUtils(this);

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
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        Toast.makeText(this, "Order ID: " + orderId, Toast.LENGTH_SHORT).show();
        loadChatMessages(orderId);

        orderDetailsLayout.setOnClickListener(v -> {
            Intent orderDetailsIntent = new Intent(ChatActivity.this, OrderDetailsActivity.class);
            orderDetailsIntent.putExtra("orderId", orderId); // תחליף ב-ID של ההזמנה שלך
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

    private void loadChatMessages(String orderId) {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatRef.orderBy("timestamp").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading chat messages", Toast.LENGTH_SHORT).show();
                return;
            }

            chatMessages.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String message = doc.getString("message");
                    String sender = doc.getString("sender");
                    Timestamp timestamp = doc.getTimestamp("timestamp");
                    chatMessages.add(new ChatMessage(sender, message, timestamp)); // להוסיף את timestamp
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            } else {
                Toast.makeText(this, "No chat messages found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Message text is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Current user is null", Toast.LENGTH_SHORT).show();
            return;
        }
        String userEmail = currentUser.getEmail();

        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");

        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("sender", userEmail);
        chatMessage.put("message", messageText);
        chatMessage.put("timestamp", new Timestamp(new Date()));
        chatMessage.put("readBy", Collections.singletonList(userEmail)); // Add readBy field

        chatRef.add(chatMessage).addOnSuccessListener(documentReference -> {
            messageInput.setText("");
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }

    private void updateReadStatusInRealtime() {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatListener = chatRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(ChatActivity.this, "Error updating read status", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ChatActivity.this, "Failed to update read status", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateReadStatusForAllMessages();
    }

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
                        Toast.makeText(ChatActivity.this, "Failed to update read status", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateReadStatusInRealtime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatListener != null) {
            chatListener.remove();
        }
    }

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

    private interface ProfileImageCallback {
        void onCallback(String imageUrl);
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
            case R.id.chat_icon: // הוספת המקרה עבור אייקון ה-chat
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
