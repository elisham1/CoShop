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

public class DeepLinkHandlerActivity extends AppCompatActivity {
    private FirebaseUser currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deep_link_handler);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        handleIncomingDeepLink();
    }

    private void handleIncomingDeepLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    if (pendingDynamicLinkData != null) {
                        Uri deepLink = pendingDynamicLinkData.getLink();
                        if (deepLink != null) {
                            String orderIdFromLink = deepLink.getQueryParameter("orderId");
                            if (orderIdFromLink != null) {

                                FirebaseFirestore.getInstance().collection("users")
                                        .document(Objects.requireNonNull(currentUser.getEmail()))
                                        .get().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document.exists()) {
                                                    String userType = document.getString("type of user");
                                                    // Open OrderDetailsActivity with the orderId
                                                    Intent intent = new Intent(this, OrderDetailsActivity.class);
                                                    intent.putExtra("userType", userType);
                                                    intent.putExtra("orderId", orderIdFromLink);
                                                    startActivity(intent);
                                                    finish(); // End current activity
                                                }
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(this, e -> Log.w("DeepLinkHandlerActivity", "getDynamicLink:onFailure", e));
    }
}
