package com.elisham.coshop;
import com.google.firebase.Timestamp;

public class ChatMessage extends ChatItem {
    private String sender;
    private String message;
    private Timestamp timestamp;

    // Required for Firestore
    public ChatMessage() {
    }

    // Full constructor
    public ChatMessage(String sender, String message, Timestamp timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}

