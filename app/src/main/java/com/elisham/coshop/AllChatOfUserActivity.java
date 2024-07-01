package com.elisham.coshop;

import android.content.Intent;
import android.os.Bundle;
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

public class AllChatOfUserActivity extends AppCompatActivity {
    private MenuUtils menuUtils;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private RecyclerView chatRecyclerView;
    private ChatListAdapter chatListAdapter;
    private List<ChatOrder> chatOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activityallchats);
        menuUtils = new MenuUtils(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatOrders = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(this, chatOrders, this::onChatSelected);
        chatRecyclerView.setAdapter(chatListAdapter);

        loadUserChats();
        invalidateOptionsMenu(); // עדכון התפריט
    }

    private void loadUserChats() {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = currentUser.getEmail();
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(AllChatOfUserActivity.this, "Error getting orders", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots != null) {
                    chatOrders.clear(); // ננקה את הרשימה כדי להימנע מהוספה כפולה
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

                        // הסרת ההזמנה הקיימת עם אותו orderId
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

    private void sortAndNotifyAdapter() {
        Collections.sort(chatOrders, new Comparator<ChatOrder>() {
            @Override
            public int compare(ChatOrder o1, ChatOrder o2) {
                return Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp());
            }
        });
        chatListAdapter.notifyDataSetChanged();
    }

    private void onChatSelected(String orderId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("orderId", orderId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu); // Inflate your menu XML
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
