package com.elisham.coshop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatItem> chatItems; // Change type to ChatItem to accommodate different item types
    private FirebaseAuth mAuth;

    private static final int VIEW_TYPE_CURRENT_USER = 1;
    private static final int VIEW_TYPE_OTHER_USER = 2;
    private static final int VIEW_TYPE_DATE_HEADER = 3;

    public ChatAdapter(List<ChatItem> chatItems) {
        this.chatItems = chatItems;
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        ChatItem item = chatItems.get(position);
        if (item instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) item;
            if (chatMessage.getSender().equals(mAuth.getCurrentUser().getEmail())) {
                return VIEW_TYPE_CURRENT_USER;
            } else {
                return VIEW_TYPE_OTHER_USER;
            }
        } else {
            return VIEW_TYPE_DATE_HEADER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_CURRENT_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_current_user, parent, false);
            return new CurrentUserViewHolder(view);
        } else if (viewType == VIEW_TYPE_OTHER_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_message_other_user, parent, false);
            return new OtherUserViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_CURRENT_USER) {
            ((CurrentUserViewHolder) holder).bind((ChatMessage) chatItems.get(position));
        } else if (holder.getItemViewType() == VIEW_TYPE_OTHER_USER) {
            ((OtherUserViewHolder) holder).bind((ChatMessage) chatItems.get(position));
        } else {
            ((DateHeaderViewHolder) holder).bind((DateHeader) chatItems.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }

        public void bind(DateHeader dateHeader) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            dateTextView.setText(sdf.format(dateHeader.getDate().toDate()));
        }
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

            profileImageView.setOnClickListener(v -> {
                String imageUrl = (String) profileImageView.getTag();
                showImageDialog(imageUrl);
            });
        }

        private void showImageDialog(String imageUrl) {
            ImageDialogFragment dialogFragment = ImageDialogFragment.newInstance(imageUrl);
            dialogFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "image_dialog");
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
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(profileImageView);
                        profileImageView.setTag(imageUrl);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_profile);
                        profileImageView.setTag(null);
                    }

                    String firstName = documentSnapshot.getString("first name");
                    nameTextView.setText(firstName);
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile);
                    profileImageView.setTag(null);
                }
            });

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

            profileImageView.setOnClickListener(v -> {
                String imageUrl = (String) profileImageView.getTag();
                showImageDialog(imageUrl);
            });
        }

        private void showImageDialog(String imageUrl) {
            ImageDialogFragment dialogFragment = ImageDialogFragment.newInstance(imageUrl);
            dialogFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), "image_dialog");
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
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(profileImageView);
                        profileImageView.setTag(imageUrl);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_profile);
                        profileImageView.setTag(null);
                    }

                    String firstName = documentSnapshot.getString("first name");
                    nameTextView.setText(firstName);
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile);
                    profileImageView.setTag(null);
                }
            });

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