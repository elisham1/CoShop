package com.elisham.coshop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatMessage> chatMessages;
    private FirebaseAuth mAuth;

    private static final int VIEW_TYPE_CURRENT_USER = 1;
    private static final int VIEW_TYPE_OTHER_USER = 2;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        if (chatMessage.getSender().equals(mAuth.getCurrentUser().getEmail())) {
            return VIEW_TYPE_CURRENT_USER;
        } else {
            return VIEW_TYPE_OTHER_USER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_CURRENT_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_current_user, parent, false);
            return new CurrentUserViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_other_user, parent, false);
            return new OtherUserViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_CURRENT_USER) {
            ((CurrentUserViewHolder) holder).bind(chatMessage);
        } else {
            ((OtherUserViewHolder) holder).bind(chatMessage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    class CurrentUserViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView profileImageView;
        TextView nameTextView;
        TextView timeTextView;

        public CurrentUserViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }

        public void bind(ChatMessage chatMessage) {
            messageTextView.setText(chatMessage.getMessage());
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(chatMessage.getSender());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(profileImageView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.star)
                                .circleCrop()
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.star);
                    }

                    String firstName = documentSnapshot.getString("first name");
                    nameTextView.setText(firstName);
                } else {
                    profileImageView.setImageResource(R.drawable.star);
                }
            });

            // Update timeTextView
            Timestamp timestamp = chatMessage.getTimestamp();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String formattedTime = sdf.format(date);
                timeTextView.setText(formattedTime);
            }
        }
    }

    class OtherUserViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView profileImageView;
        TextView nameTextView;
        TextView timeTextView;

        public OtherUserViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }

        public void bind(ChatMessage chatMessage) {
            messageTextView.setText(chatMessage.getMessage());
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(chatMessage.getSender());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String imageUrl = documentSnapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(profileImageView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.star)
                                .circleCrop()
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.star);
                    }

                    String firstName = documentSnapshot.getString("first name");
                    nameTextView.setText(firstName);
                } else {
                    profileImageView.setImageResource(R.drawable.star);
                }
            });

            // Update timeTextView
            Timestamp timestamp = chatMessage.getTimestamp();
            if (timestamp != null) {
                Date date = timestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String formattedTime = sdf.format(date);
                timeTextView.setText(formattedTime);
            }
        }
    }
     }