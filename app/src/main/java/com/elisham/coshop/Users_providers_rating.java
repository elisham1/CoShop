package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class Users_providers_rating extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_providers_rating);

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
        Intent toy = new Intent(Users_providers_rating.this, HomePageActivity.class);
        startActivity(toy);
    }

    public void personalInfo() {
        Intent toy = new Intent(Users_providers_rating.this, UpdateUserDetailsActivity.class);
        startActivity(toy);
    }

    public void myOrders() {
        Intent toy = new Intent(Users_providers_rating.this, MyOrdersActivity.class);
        startActivity(toy);
    }

    public void aboutUs() {
        Intent toy = new Intent(Users_providers_rating.this, AboutActivity.class);
        startActivity(toy);
    }

    public void contactUs() {
        Intent toy = new Intent(Users_providers_rating.this, ContactUsActivity.class);
        startActivity(toy);
    }

    public void basket() {
        Intent toy = new Intent(Users_providers_rating.this, BasketActivity.class);
        startActivity(toy);
    }

    public void logOut() {
        Intent toy = new Intent(Users_providers_rating.this, MainActivity.class);
        startActivity(toy);
    }
}