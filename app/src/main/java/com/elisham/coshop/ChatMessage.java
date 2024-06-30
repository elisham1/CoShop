package com.elisham.coshop;
import com.google.firebase.Timestamp;

public class ChatMessage {
    private String sender;
    private String message;
    private Timestamp timestamp;

    // דרוש לפיירבייס
    public ChatMessage() {
    }

    // קונסטרוקטור מלא
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
