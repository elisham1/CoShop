package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize chat components
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        Toast.makeText(this, "Order ID: " + orderId, Toast.LENGTH_SHORT).show();
        loadChatMessages(orderId);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sendButton.setOnClickListener(v -> sendMessage());
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
                    chatMessages.add(new ChatMessage(sender, message));
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

        chatRef.add(chatMessage).addOnSuccessListener(documentReference -> {
            messageInput.setText("");
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show());
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
            case android.R.id.home:
                onBackPressed();
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
        Intent toy = new Intent(ChatActivity.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(ChatActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(ChatActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(ChatActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(ChatActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(ChatActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(ChatActivity.this, MainActivity.class);
        startActivity(toy);
    }
}
