package com.elisham.coshop;

import com.google.firebase.Timestamp;

// Represents a date header in the chat
public class DateHeader extends ChatItem {
    private Timestamp date;

    // Constructor for DateHeader
    public DateHeader(Timestamp date) {
        this.date = date;
    }

    // Returns the date of the header
    public Timestamp getDate() {
        return date;
    }
}
