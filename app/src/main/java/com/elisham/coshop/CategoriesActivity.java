package com.elisham.coshop;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class CategoriesActivity extends AppCompatActivity {

    private String email, firstName, familyName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        Intent intent = getIntent();
        if(intent != null) {
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");

            String helloUser = "Hello, " + firstName;
            TextView userName = findViewById(R.id.userName);
            userName.setText(helloUser);
        }

        // Enable the back button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle the back button click
        if (id == android.R.id.home) {
            onBackPressed(); // Go back when the back arrow is clicked
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void doneCategory(View v) {
        Intent toy = new Intent(CategoriesActivity.this, UserDetailsActivity.class);
        toy.putExtra("email", email);
        toy.putExtra("firstName", firstName);
        toy.putExtra("familyName", familyName);
        startActivity(toy);
    }

}