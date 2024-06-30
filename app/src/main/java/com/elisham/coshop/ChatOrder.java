package com.elisham.coshop;
public class ChatOrder {
    private String orderId;
    private Long lastMessageTimestamp;

    public ChatOrder(String orderId, Long lastMessageTimestamp) {
        this.orderId = orderId;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }
}
