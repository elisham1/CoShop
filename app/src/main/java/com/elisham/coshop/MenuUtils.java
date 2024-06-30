package com.elisham.coshop;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;


public class MenuUtils {

    private static FirebaseAuth mAuth;
    private Context context;
    GoogleSignInAccount googleSignInAccount;

    public MenuUtils(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context);

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
        if (googleSignInAccount != null) {
            // Signed in with Google
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Sign out from Firebase
                    Log.d("Logout", "Google sign-out successful");
                } else {
                    // Handle sign-out failure
                    Log.e("Logout", "Google sign-out failed");
                }
            });
        }
        // Not signed in with Google, sign out from Firebase in both cases
        mAuth.signOut();

        // Navigate to MainActivity
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