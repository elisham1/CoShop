package com.elisham.coshop;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OrderDeletedActivity extends AppCompatActivity {
    private String globalUserType;
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the user type from the intent and set the appropriate theme
        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }

        setContentView(R.layout.activity_order_deleted);
        menuUtils = new MenuUtils(this, globalUserType);

        // Set up the back button with appropriate background and click listener
        Button backButton = findViewById(R.id.back_button);
        if (globalUserType.equals("Supplier")) {
            backButton.setBackgroundResource(R.drawable.bg_selected_supplier);
        }
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateBack();
            }
        });
    }

    // Navigate back to the appropriate activity based on user type
    private void navigateBack() {
        Intent intent;
        if ("Consumer".equals(globalUserType)) {
            intent = new Intent(this, HomePageActivity.class);
        } else if ("Supplier".equals(globalUserType)) {
            intent = new Intent(this, MyOrdersActivity.class);
        } else {
            intent = new Intent(this, HomePageActivity.class);
        }
        intent.putExtra("userType", globalUserType);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        if ("Supplier".equals(globalUserType)) {
            MenuItem item = menu.findItem(R.id.chat_notification);
            if (item != null) {
                item.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo(); // Navigate to personal info activity
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders(); // Navigate to my orders activity
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs(); // Navigate to about us activity
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs(); // Navigate to contact us activity
                return true;
            case R.id.Log_Out:
                menuUtils.logOut(); // Log out the user
                return true;
            case R.id.home:
                menuUtils.home(); // Navigate to home activity
                return true;
            case R.id.chat_icon:
                menuUtils.allChats(); // Navigate to all chats activity
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification(); // Navigate to chat notification activity
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
