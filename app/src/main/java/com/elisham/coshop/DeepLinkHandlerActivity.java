package com.elisham.coshop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

// Handles incoming deep links and redirects to appropriate activities
public class DeepLinkHandlerActivity extends AppCompatActivity {
    private static final String TAG = "DeepLinkHandlerActivity";
    private static final String APP_DOMAIN = "coshopapp.page.link";
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    // Initializes the activity, Firebase Auth, and Firestore
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deep_link_handler);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "onCreate: Initialized Firebase Auth and Firestore.");
        handleIncomingDeepLink();
    }

    // Handles the incoming deep link and extracts order ID
    private void handleIncomingDeepLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    if (pendingDynamicLinkData != null) {
                        Uri deepLink = pendingDynamicLinkData.getLink();
                        if (deepLink != null) {
                            String host = deepLink.getHost();
                            if (APP_DOMAIN.equals(host)) {
                                String orderIdFromLink = deepLink.getQueryParameter("orderId");
                                Log.d(TAG, "handleIncomingDeepLink: Deep link received with orderId: " + orderIdFromLink);
                                if (orderIdFromLink != null) {
                                    getUserInfoAndCheckOrder(orderIdFromLink);
                                } else {
                                    Log.w(TAG, "handleIncomingDeepLink: No orderId found in the deep link.");
                                }
                            } else {
                                Log.w(TAG, "handleIncomingDeepLink: Deep link is null.");
                            }
                        }
                        else {
                            Log.w(TAG, "handleIncomingDeepLink: Deep link is null.");
                        }
                    } else {
                        Log.w(TAG, "handleIncomingDeepLink: No pending dynamic link data.");
                        // Get the intent that started this activity
                        Intent intent = getIntent();
                        String action = intent.getAction();
                        String type = intent.getType();
                        if (Intent.ACTION_SEND.equals(action) && type != null) {
                            Log.d(TAG, "handleIncomingDeepLink: Received intent with action: " + action + " and type: " + type);
                            if ("text/plain".equals(type)) {
                                Log.d(TAG, "handleIncomingDeepLink: Received text/plain intent.");
                                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                                if (sharedText != null) {
                                    Log.d(TAG, "handleIncomingDeepLink: Shared text: " + sharedText);
                                    handleSharedLink(sharedText);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(this, e -> Log.e(TAG, "handleIncomingDeepLink: getDynamicLink failed.", e));
    }

    // Retrieves user information from Firestore and checks the order
    private void getUserInfoAndCheckOrder(String orderIdFromLink) {
        String userEmail = Objects.requireNonNull(currentUser.getEmail());
        Log.d(TAG, "getUserInfoAndCheckOrder: Fetching user info for email: " + userEmail);

        db.collection("users")
                .document(userEmail)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("type of user");
                            Log.d(TAG, "getUserInfoAndCheckOrder: User type: " + userType);
                            checkOrderExistence(orderIdFromLink, userType);
                        } else {
                            Log.w(TAG, "getUserInfoAndCheckOrder: No document found for user.");
                        }
                    } else {
                        Log.e(TAG, "getUserInfoAndCheckOrder: Failed to fetch user info.", task.getException());
                    }
                });
    }

    // Checks if the order exists and redirects to the appropriate activity
    private void checkOrderExistence(String orderId, String userType) {
        Log.d(TAG, "checkOrderExistence: Checking existence of order with ID: " + orderId);

        db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot orderDocument = task.getResult();
                Intent intent;
                if (orderDocument.exists()) {
                    Log.d(TAG, "checkOrderExistence: Order exists. Navigating to OrderDetailsActivity.");
                    intent = new Intent(this, OrderDetailsActivity.class);
                } else {
                    Log.d(TAG, "checkOrderExistence: Order does not exist. Navigating to OrderDeletedActivity.");
                    intent = new Intent(this, OrderDeletedActivity.class);
                }
                intent.putExtra("userType", userType);
                intent.putExtra("orderId", orderId);
                startActivity(intent);
                finish(); // End current activity
            } else {
                Log.e(TAG, "checkOrderExistence: Failed to check order existence.", task.getException());
            }
        });
    }

    private void handleSharedLink(String sharedLink) {
        String userEmail = Objects.requireNonNull(currentUser.getEmail());
        Log.d(TAG, "handleSharedLink: Fetching user info for email: " + userEmail);

        db.collection("users")
                .document(userEmail)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("type of user");
                            Log.d(TAG, "handleSharedLink: User type: " + userType);
                            openMyOrdersActivityWithSharedLink(userType, sharedLink);
                        } else {
                            Log.w(TAG, "handleSharedLink: No document found for user.");
                        }
                    } else {
                        Log.e(TAG, "handleSharedLink: Failed to fetch user info.", task.getException());
                    }
                });
    }

    private void openMyOrdersActivityWithSharedLink(String userType, String sharedLink) {
        Intent intent = new Intent(this, MyOrdersActivity.class);
        intent.putExtra("userType", userType);
        intent.putExtra("sharedLink", sharedLink);
        startActivity(intent);
        finish();
    }
}
