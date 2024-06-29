package com.elisham.coshop;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class MenuUtils {

    private static FirebaseAuth mAuth;
    private Context context;

    public MenuUtils(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
    }

    public void personalInfo() {
        Intent intent = new Intent(context, UpdateUserDetailsActivity.class);
        context.startActivity(intent);
    }

    public void myOrders() {
        Intent intent = new Intent(context, MyOrdersActivity.class);
        context.startActivity(intent);
    }

    public void aboutUs() {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    public void contactUs() {
        Intent intent = new Intent(context, ContactUsActivity.class);
        context.startActivity(intent);
    }

    public void basket() {
        Intent intent = new Intent(context, BasketActivity.class);
        context.startActivity(intent);
    }

    public void logOut() {
        mAuth.signOut();
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
        finishActivity();
    }

    public void home() {
        Intent intent = new Intent(context, HomePageActivity.class);
        context.startActivity(intent);
        finishActivity();
    }

    private void finishActivity() {
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).finish();
        }
    }
}