package com.elisham.coshop;

import com.google.firebase.Timestamp;

public class DateHeader extends ChatItem {
    private Timestamp date;

    public DateHeader(Timestamp date) {
        this.date = date;
    }

    public Timestamp getDate() {
        return date;
    }
}
