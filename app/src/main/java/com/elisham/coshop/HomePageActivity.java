package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed(); // Go back when the back arrow is clicked
                return true;
            case R.id.Personal_info:
                Toast.makeText(this, "Personal Info is clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.My_Orders:
                Toast.makeText(this, "My Orders is clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.About_Us:
                Toast.makeText(this, "About Us is clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.Contact_Us:
                Toast.makeText(this, "Contact Us is clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.Log_Out:
                Toast.makeText(this, "Log Out is clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.list_icon:
                // Handle click on list icon
                Toast.makeText(this, "List Icon is clicked", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void gotofilter(View v) {
        Intent toy = new Intent(HomePageActivity.this, FilterActivity.class);
        startActivity(toy);
    }

    public void gotoneworder(View v) {
        Intent toy = new Intent(HomePageActivity.this, OpenNewOrderActivity.class);
        startActivity(toy);
    }
}
