package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class FilterActivity extends AppCompatActivity {

    public Button ok_id;

    public void init() {
        ok_id = (Button) findViewById(R.id.ok_id);
        ok_id.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent toy = new Intent(FilterActivity.this, HomePageActivity.class);
                startActivity(toy);
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        init();
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
                personalInfo();
                return true;
            case R.id.My_Orders:
                myOrders();
                return true;
            case R.id.About_Us:
                aboutUs();
                return true;
            case R.id.Contact_Us:
                contactUs();
                return true;
            case R.id.Log_Out:
                logOut();
                return true;
            case R.id.list_icon:
                basket();
                return true;
            case R.id.home:
                home();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void home() {
        Intent toy = new Intent(FilterActivity.this, HomePageActivity.class);
        startActivity(toy);
    }
    public void personalInfo() {
        Intent toy = new Intent(FilterActivity.this, UserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(FilterActivity.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(FilterActivity.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(FilterActivity.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(FilterActivity.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(FilterActivity.this, MainActivity.class);
        startActivity(toy);
    }

}