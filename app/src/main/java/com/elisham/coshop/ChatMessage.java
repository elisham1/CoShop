package com.elisham.coshop;

import com.google.firebase.Timestamp;

// Represents a chat message in the app
public class ChatMessage extends ChatItem {
    private String sender;
    private String message;
    private Timestamp timestamp;

    // Default constructor required for Firestore
    public ChatMessage() {
    }

    // Full constructor for ChatMessage
    public ChatMessage(String sender, String message, Timestamp timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Returns the sender of the message
    public String getSender() {
        return sender;
    }

    // Returns the message content
    public String getMessage() {
        return message;
    }

    // Returns the timestamp of the message
    public Timestamp getTimestamp() {
        return timestamp;
    }
}
