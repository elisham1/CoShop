package com.elisham.coshop;import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private List<ChatOrder> chatOrders;
    private OnChatSelectedListener onChatSelectedListener;
    private FirebaseFirestore db;
    private Context context;

    public ChatListAdapter(Context context, List<ChatOrder> chatOrders, OnChatSelectedListener onChatSelectedListener) {
        this.context = context;
        this.chatOrders = chatOrders;
        this.onChatSelectedListener = onChatSelectedListener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatOrder chatOrder = chatOrders.get(position);
        holder.bind(chatOrder, onChatSelectedListener);
    }

    @Override
    public int getItemCount() {
        return chatOrders.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private ImageView orderImageView;
        private TextView orderTitleTextView;
        private TextView orderLocationTextView;
        private TextView lastMessageTimeTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            orderImageView = itemView.findViewById(R.id.orderImageView);
            orderTitleTextView = itemView.findViewById(R.id.orderTitleTextView);
            orderLocationTextView = itemView.findViewById(R.id.orderLocationTextView);
            lastMessageTimeTextView = itemView.findViewById(R.id.lastMessageTimeTextView);
        }

        public void bind(ChatOrder chatOrder, OnChatSelectedListener listener) {
            // Set default image
            orderImageView.setImageResource(R.drawable.star);

            // Fetch titleOfOrder and location from Firestore
            DocumentReference orderRef = db.collection("orders").document(chatOrder.getOrderId());
            orderRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                    GeoPoint location = documentSnapshot.getGeoPoint("location");

                    orderTitleTextView.setText(titleOfOrder);

                    if (location != null) {
                        getAddressFromGeoPoint(location, orderLocationTextView);
                    }
                }
            });

            // Set last message time
            if (chatOrder.getLastMessageTimestamp() != 0L) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                lastMessageTimeTextView.setText(sdf.format(new java.util.Date(chatOrder.getLastMessageTimestamp() * 1000)));
            } else {
                lastMessageTimeTextView.setText(""); // Set empty if no messages
            }

            itemView.setOnClickListener(v -> listener.onChatSelected(chatOrder.getOrderId()));
        }

        private void getAddressFromGeoPoint(GeoPoint geoPoint, TextView textView) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    textView.setText(address.getAddressLine(0));
                } else {
                    textView.setText("Unknown location");
                }
            } catch (IOException e) {
                e.printStackTrace();
                textView.setText("Error retrieving location");
            }
        }
    }

    public interface OnChatSelectedListener {
        void onChatSelected(String orderId);
    }
}
