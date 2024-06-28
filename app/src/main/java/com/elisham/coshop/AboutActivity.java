package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;

public class AboutActivity extends AppCompatActivity {

    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        menuUtils = new MenuUtils(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Personal_info:
                menuUtils.personalInfo();
                return true;
            case R.id.My_Orders:
                menuUtils.myOrders();
                return true;
            case R.id.About_Us:
                menuUtils.aboutUs();
                return true;
            case R.id.Contact_Us:
                menuUtils.contactUs();
                return true;
            case R.id.Log_Out:
                menuUtils.logOut();
                return true;
            case R.id.list_icon:
                menuUtils.basket();
                return true;
            case R.id.home:
                menuUtils.home();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

