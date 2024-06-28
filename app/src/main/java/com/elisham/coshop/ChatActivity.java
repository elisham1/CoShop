package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private LinearLayout chatContainer;
    private EditText messageInput;
    private Button sendButton;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        menuUtils = new MenuUtils(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize chat components
        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        Toast.makeText(this, "Order ID: " + orderId, Toast.LENGTH_SHORT).show();
        loadChatMessages(orderId);

        sendButton.setOnClickListener(v -> {
            Toast.makeText(this, "Send button clicked", Toast.LENGTH_SHORT).show();
            sendMessage();
        });
    }

    private void loadChatMessages(String orderId) {
        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");
        chatRef.orderBy("timestamp").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading chat messages", Toast.LENGTH_SHORT).show();
                return;
            }

            chatContainer.removeAllViews();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String message = doc.getString("message");
                    String sender = doc.getString("sender");
                    addMessageToLayout(sender, message);
                }
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
        Toast.makeText(this, "User Email: " + userEmail, Toast.LENGTH_SHORT).show();

        CollectionReference chatRef = db.collection("orders").document(orderId).collection("chat");

        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("sender", userEmail);
        chatMessage.put("message", messageText);
        chatMessage.put("timestamp", new Timestamp(new Date()));

        chatRef.add(chatMessage).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show();
            messageInput.setText("");
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }

    private void addMessageToLayout(String sender, String message) {
        TextView messageView = new TextView(this);
        messageView.setText(sender + ": " + message);
        chatContainer.addView(messageView);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
