package com.elisham.coshop;

// Represents a chat order with the order ID and last message timestamp
public class ChatOrder {
    private String orderId;
    private Long lastMessageTimestamp;

    // Constructor for ChatOrder
    public ChatOrder(String orderId, Long lastMessageTimestamp) {
        this.orderId = orderId;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    // Returns the order ID
    public String getOrderId() {
        return orderId;
    }

    // Returns the timestamp of the last message
    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }
}
