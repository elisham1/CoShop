package com.elisham.coshop;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuUtils {

    private static FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private final Context context;
    private final GoogleSignInAccount googleSignInAccount;
    private final String userType;

    public MenuUtils(Context context, String userType) {
        this.context = context;
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(context);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        this.userType = userType;
    }

    // Opens the personal information update activity
    public void personalInfo() {
        Intent intent = new Intent(context, UpdateUserDetailsActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Opens the user's orders activity
    public void myOrders() {
        Intent intent = new Intent(context, MyOrdersActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Opens the about us activity
    public void aboutUs() {
        Intent intent = new Intent(context, AboutActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Opens the contact us activity
    public void contactUs() {
        Intent intent = new Intent(context, ContactUsActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Logs out the user
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

    // Navigates to the home activity based on user type
    public void home() {
        Intent intent;
        if (userType.equals("Consumer")) {
            intent = new Intent(context, HomePageActivity.class);
        } else {
            intent = new Intent(context, MyOrdersActivity.class);
        }
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Finishes the current activity if it's not HomePageActivity
    private void finishActivity() {
        if (context instanceof AppCompatActivity && !(context instanceof HomePageActivity)) {
            ((AppCompatActivity) context).finish();
        }
    }

    // Opens the all chats activity
    public void allChats() {
        Intent intent = new Intent(context, AllChatOfUserActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }

    // Opens the chat notifications activity
    public void chat_notification() {
        Intent intent = new Intent(context, notificationActivity.class);
        intent.putExtra("userType", userType);
        context.startActivity(intent);
        finishActivity();
    }
}
