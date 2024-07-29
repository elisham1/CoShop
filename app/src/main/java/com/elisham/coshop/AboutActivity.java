package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;

// Handles the About Us page functionality
public class AboutActivity extends AppCompatActivity {

    private MenuUtils menuUtils;
    private String globalUserType;

    // Initializes the activity and sets the theme based on user type
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        } else if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }

        setContentView(R.layout.activity_about);
        menuUtils = new MenuUtils(this, globalUserType);
    }

    // Inflates the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    // Handles item selections in the options menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo(); // Navigate to Personal Info
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders(); // Navigate to My Orders
                return true;
            case R.id.About_Us:
                return true; // Stay on About Us
            case R.id.Contact_Us:
                menuUtils.contactUs(); // Navigate to Contact Us
                return true;
            case R.id.Log_Out:
                menuUtils.logOut(); // Log out user
                return true;
            case R.id.home:
                menuUtils.home(); // Navigate to Home
                return true;
            case R.id.chat_icon:
                menuUtils.allChats(); // Navigate to All Chats
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification(); // Navigate to Chat Notification
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
