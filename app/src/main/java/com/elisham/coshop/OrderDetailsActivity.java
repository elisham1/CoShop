package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.Typeface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView descriptionTextView, categoryTextView,
            addressTextView, timeTextView, titleTextView, groupInfoTextView, closedOrderTextView,
            joinText, leaveText, shareText, chatText, waitingListText;
    private ImageView categoryImageView, joinIcon,
            waitingListButton, leaveButton, chatIcon, saveUrlButton;
    private EditText urlEditText;
    private String orderId, globalUserType, email;
    private Geocoder geocoder;
    private MenuUtils menuUtils;
    private LinearLayout userListLayout;
    private List<String> listPeopleInOrder;
    private boolean showAllUsers = false, inOrder = false,
            inWaitingList = false, orderDeleted = false;
    private ProgressDialog progressDialog;
    private ImageView cartIcon;
    private TextView cartText;

    private int[] imageResources = {
            R.drawable.one,
            R.drawable.two,
            R.drawable.three,
            R.drawable.four,
            R.drawable.five,
            R.drawable.six,
            R.drawable.seven,
            R.drawable.eight,

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the theme based on the user type

        Intent intent = getIntent();
        globalUserType = intent.getStringExtra("userType");

        if (globalUserType != null && globalUserType.equals("Consumer")) {
            setTheme(R.style.ConsumerTheme);
        }
        if (globalUserType != null && globalUserType.equals("Supplier")) {
            setTheme(R.style.SupplierTheme);
        }
        setContentView(R.layout.activity_order_details);
        menuUtils = new MenuUtils(this, globalUserType);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        geocoder = new Geocoder(this, Locale.ENGLISH);

        // Get the orderId from the intent
        orderId = intent.getStringExtra("orderId");
        email = currentUser.getEmail();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        initializeUI();
    }

    // Initialize UI elements and set up listeners
    private void initializeUI() {
        // Initialize TextViews and ImageView
        descriptionTextView = findViewById(R.id.descriptionTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        addressTextView = findViewById(R.id.addressTextView);
        timeTextView = findViewById(R.id.timeTextView);
        titleTextView = findViewById(R.id.titleTextView);
        groupInfoTextView = findViewById(R.id.groupInfoTextView);
        categoryImageView = findViewById(R.id.categoryImageView);
        chatIcon = findViewById(R.id.chatIcon);
        joinIcon = findViewById(R.id.joinIcon);
        waitingListButton = findViewById(R.id.waitingListButton);
        leaveButton = findViewById(R.id.leaveButton);
        userListLayout = findViewById(R.id.userListLayout);
        joinText = findViewById(R.id.joinText);
        leaveText = findViewById(R.id.leaveText);
        shareText = findViewById(R.id.shareText);
        chatText = findViewById(R.id.chatText);
        waitingListText = findViewById(R.id.waitingListText);
        closedOrderTextView = findViewById(R.id.closed_order);
        urlEditText = findViewById(R.id.url);
        saveUrlButton = findViewById(R.id.saveUrlButton);
        Button siteButton = findViewById(R.id.siteButton);
        ImageView tapIcon = findViewById(R.id.tap_icon);
        ConstraintLayout urlLayout = findViewById(R.id.urlLayout);
        cartIcon = findViewById(R.id.cartIcon);
        cartText = findViewById(R.id.cartText);

        if (orderId != null) {
            progressDialog.show();
            checkUserInList();
            // Fetch order details and then user details
            fetchOrderDetails(orderId, email);
        } else {
            Log.e("OrderDetailsActivity", "Order ID is null");
        }

        // Check if the activity was opened with a shared link
        SharedPreferences prefs = getSharedPreferences(MyOrdersActivity.PREFS_NAME, MODE_PRIVATE);
        String sharedLink = prefs.getString("sharedLink", null);

        if (sharedLink != null) {
            showAddToCartDialog(email, sharedLink);            // Clear the shared link from SharedPreferences after using it
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("sharedLink");
            editor.apply();
        }

        joinIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(OrderDetailsActivity.this)
                    .setTitle("Join the order?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.show();
                            addUserToOrder();
                            inOrder = true;
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        joinText.setOnClickListener(v -> {
            new AlertDialog.Builder(OrderDetailsActivity.this)
                    .setTitle("Join the order?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.show();
                            addUserToOrder();
                            inOrder = true;
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        chatIcon.setOnClickListener(v -> {
            Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("userType", globalUserType);
            chatIntent.putExtra("orderId", orderId);
            startActivity(chatIntent);
        });

        chatText.setOnClickListener(v -> {
            Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("userType", globalUserType);
            chatIntent.putExtra("orderId", orderId);
            startActivity(chatIntent);
        });

        // Initialize the tap icon
        tapIcon.setOnClickListener(v -> showImageAlertDialog());

        waitingListButton.setOnClickListener(v -> {
            if (inWaitingList) {
                new AlertDialog.Builder(OrderDetailsActivity.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to get off the waiting list?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.show();
                                removeUserFromWaitingList();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(OrderDetailsActivity.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to get on the waiting list?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.show();
                                addUserToWaitingList();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        waitingListText.setOnClickListener(v -> {
            if (inWaitingList) {
                new AlertDialog.Builder(OrderDetailsActivity.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to get off the waiting list?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.show();
                                removeUserFromWaitingList();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(OrderDetailsActivity.this)
                        .setTitle("Confirm")
                        .setMessage("Are you sure you want to get on the waiting list?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.show();
                                addUserToWaitingList();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        leaveButton.setOnClickListener(v -> {
            new AlertDialog.Builder(OrderDetailsActivity.this)
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to leave the order?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.show();
                            removeUserFromOrder();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        leaveText.setOnClickListener(v -> {
            new AlertDialog.Builder(OrderDetailsActivity.this)
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to leave the order?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog.show();
                            removeUserFromOrder();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        cartIcon.setOnClickListener(v -> showCartDialog());

        cartText.setOnClickListener(v -> showCartDialog());

        saveUrlButton.setOnClickListener(v -> saveUrl());

        ImageView shareIcon = findViewById(R.id.shareIcon);
        if (globalUserType.equals("Supplier")) {
            shareIcon.setImageResource(R.drawable.ic_share_supplier);
            leaveButton.setImageResource(R.drawable.ic_leave_supplier);
            chatIcon.setImageResource(R.drawable.ic_chat_supplier);
            cartIcon.setImageResource(R.drawable.ic_cart_supplier);
            shareText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
            joinText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
            leaveText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
            chatText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
            waitingListText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
            cartText.setTextColor(ContextCompat.getColor(this, R.color.supplierPrimary));
        }
        if (globalUserType.equals("Consumer")) {
            shareIcon.setImageResource(R.drawable.ic_share_consumer);
            shareText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
            joinText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
            leaveText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
            chatText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
            waitingListText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
            cartText.setTextColor(ContextCompat.getColor(this, R.color.consumerPrimary));
        }
        shareIcon.setOnClickListener(v -> {
            Log.d("OrderDetailsActivity", "Share icon clicked");
            progressDialog.show();
            shareOrderDetails();
        });

        shareText.setOnClickListener(v -> {
            Log.d("OrderDetailsActivity", "Share icon clicked");
            progressDialog.show();
            shareOrderDetails();
        });

        // Hide tap icon if site button is visible
        if (siteButton.getVisibility() == View.VISIBLE) {
            tapIcon.setVisibility(View.GONE);
        } else {
            tapIcon.setVisibility(View.VISIBLE);
        }
    }

    // Show image alert dialog for tutorial
    private void showImageAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Custom view for the AlertDialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_viewer, null);
        ViewPager2 viewPager = dialogView.findViewById(R.id.view_pager);
        LinearLayout dotsLayout = dialogView.findViewById(R.id.dots_layout);
        Button skipButton = dialogView.findViewById(R.id.skip_button);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        // Make the dialog non-cancelable
        alertDialog.setCancelable(false);

        String[] imageCaptions = {
                "Open the PayBox app.",
                "Select \"Box group\" and click on plus.",
                "Fill in details and click on \"Next\" and at the end \"Done\".",
                "Click \"View Group\".",
                "Click \"Invite Friends\".",
                "Click \"Share Link\".",
                "Click \"Copy\".",
                "Paste the generated link here"
        };

        ViewPagerAdapter adapter = new ViewPagerAdapter(imageResources, imageCaptions);
        viewPager.setAdapter(adapter);

        addDots(dotsLayout, imageCaptions.length, 0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDots(dotsLayout, imageCaptions.length, position);
            }
        });

        skipButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.setOnShowListener(dialog -> {
            // Get the screen width and height
            int dialogWidth = getResources().getDisplayMetrics().widthPixels;
            int dialogHeight = getResources().getDisplayMetrics().heightPixels;

            // Set the dialog to 2/3 the screen size
            alertDialog.getWindow().setLayout((int)(dialogWidth), (int)(dialogHeight * 0.66));
        });

        alertDialog.show();
    }

    // Add dots for the image pager
    private void addDots(LinearLayout dotsLayout, int size, int currentPosition) {
        dotsLayout.removeAllViews();
        TextView[] dots = new TextView[size];
        for (int i = 0; i < size; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(R.color.inactive_dot));
            dotsLayout.addView(dots[i]);
        }
        if (dots.length > 0) {
            if (globalUserType.equals("Supplier")) {
                dots[currentPosition].setTextColor(getResources().getColor(R.color.supplierPrimary));
            }
            if (globalUserType.equals("Consumer")) {
                dots[currentPosition].setTextColor(getResources().getColor(R.color.consumerPrimary));
            }
        }
    }

    // Fetch order details and update UI
    private void fetchOrderDetails(String orderId, String currentUserEmail) {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Fetch order details from the document
                String categorie = documentSnapshot.getString("categorie");
                String description = documentSnapshot.getString("description");
                GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                double latitude = geoPoint.getLatitude();
                double longitude = geoPoint.getLongitude();
                String address = getAddressFromLatLng(latitude, longitude);
                Timestamp timestamp = documentSnapshot.getTimestamp("time");
                int numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder").intValue();
                int maxPeople = documentSnapshot.getLong("max_people").intValue();
                String titleOfOrder = documentSnapshot.getString("titleOfOrder");
                String userEmail = documentSnapshot.getString("user_email");
                Timestamp openOrderTime = documentSnapshot.getTimestamp("openOrderTime");
                String siteUrl = documentSnapshot.getString("URL");

                // Convert timestamps to Date objects
                Date date = timestamp != null ? timestamp.toDate() : null;
                Date openOrderDate = openOrderTime != null ? openOrderTime.toDate() : null;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String formattedTime = date != null ? sdf.format(date) : "N/A";
                String formattedOpenOrderTime = openOrderDate != null ? sdf.format(openOrderDate) : "N/A";

                // Update TextViews and ImageView with order details
                descriptionTextView.setText(description);

                // Update Button logic
                Button siteButton = findViewById(R.id.siteButton);
                urlEditText = findViewById(R.id.url);
                saveUrlButton = findViewById(R.id.saveUrlButton);
                ImageView tapIcon = findViewById(R.id.tap_icon);
                ConstraintLayout urlLayout = findViewById(R.id.urlLayout);

                // Show/Hide URL fields based on user_email
                if (userEmail.equals(currentUserEmail)) {
                    if (siteUrl != null && !siteUrl.isEmpty()) {
                        siteButton.setVisibility(View.VISIBLE);
                        urlLayout.setVisibility(View.GONE);
                        urlEditText.setVisibility(View.GONE);
                        saveUrlButton.setVisibility(View.GONE);
                        siteButton.setOnClickListener(v -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                            startActivity(browserIntent);
                        });
                        tapIcon.setVisibility(View.GONE); // Hide tap icon if site button is visible
                    } else {
                        siteButton.setVisibility(View.GONE);
                        urlLayout.setVisibility(View.VISIBLE);
                        urlEditText.setVisibility(View.VISIBLE);
                        saveUrlButton.setVisibility(View.VISIBLE);
                        tapIcon.setVisibility(View.VISIBLE); // Show tap icon if site button is not visible
                    }
                } else {
                    if (siteUrl != null && !siteUrl.isEmpty()) {
                        siteButton.setVisibility(View.VISIBLE);
                    } else {
                        siteButton.setVisibility(View.GONE);
                    }
                    urlEditText.setVisibility(View.GONE);
                    urlLayout.setVisibility(View.GONE);
                    saveUrlButton.setVisibility(View.GONE);
                    tapIcon.setVisibility(View.GONE);
                }

                String peopleText;
                if (maxPeople == 0) {
                    peopleText = " | " + numberOfPeopleInOrder + "/∞ Members";
                } else {
                    peopleText = " | " + numberOfPeopleInOrder + "/" + maxPeople + " Members";
                }

                String category = categorie + peopleText;
                categoryTextView.setText(category);
                addressTextView.setText(address);
                timeTextView.setText(formattedTime);
                timeTextView.setTag(timestamp); // Store the timestamp in the tag
                titleTextView.setText(titleOfOrder);

                // Load icon from URL
                String iconUrl = "https://firebasestorage.googleapis.com/v0/b/coshop-6fecd.appspot.com/o/icons%2F" + categorie + ".png?alt=media";
                Glide.with(this)
                        .load(iconUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.star)
                        .error(R.drawable.star2)
                        .into(categoryImageView);

                // Fetch user details and update groupInfoTextView
                fetchUserDetails(userEmail, formattedOpenOrderTime);

                // Update the timer
                if (timestamp != null) {
                    startCountdownTimer(timestamp);
                }

                // Fetch list of users in order
                listPeopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
                if (listPeopleInOrder != null && !listPeopleInOrder.isEmpty()) {
                    fetchAndShowUsersInOrder(listPeopleInOrder, userEmail, currentUserEmail);
                }

                if (listPeopleInOrder != null) {
                    // Always show joinIcon and hide waitingListButton when max_people is 0 and user is not in the list
                    if (maxPeople == 0 && (listPeopleInOrder.isEmpty() || !listPeopleInOrder.contains(email))) {
                        joinIcon.setVisibility(View.VISIBLE);
                        joinText.setVisibility(View.VISIBLE);
                        chatIcon.setVisibility(View.GONE);
                        chatText.setVisibility(View.GONE);
                        waitingListButton.setVisibility(View.GONE);
                        waitingListText.setVisibility(View.GONE);
                        leaveButton.setVisibility(View.GONE);
                        leaveText.setVisibility(View.GONE);
                        cartIcon.setVisibility(View.GONE);
                        cartText.setVisibility(View.GONE);
                    } else {
                        // Existing logic to check if the user is in the order or waiting list
                        if (listPeopleInOrder.contains(email)) {
                            inOrder = true;
                            joinIcon.setVisibility(View.GONE); // Hide the join icon
                            joinText.setVisibility(View.GONE);
                            chatIcon.setVisibility(View.VISIBLE); // Show the chat icon
                            chatText.setVisibility(View.VISIBLE);
                            waitingListButton.setVisibility(View.GONE); // Hide the waiting list button
                            waitingListText.setVisibility(View.GONE);
                            leaveButton.setVisibility(View.VISIBLE); // Show the leave button
                            leaveText.setVisibility(View.VISIBLE);
                            cartIcon.setVisibility(View.VISIBLE); // Show cart icon
                            cartText.setVisibility(View.VISIBLE);
                        } else if (numberOfPeopleInOrder >= maxPeople) {
                            joinIcon.setVisibility(View.GONE);
                            joinText.setVisibility(View.GONE);
                            chatIcon.setVisibility(View.GONE);
                            chatText.setVisibility(View.GONE);
                            leaveButton.setVisibility(View.GONE);
                            leaveText.setVisibility(View.GONE);
                            if (inWaitingList) {
                                waitingListButton.setImageResource(R.drawable.ic_unwaitlist);
                                waitingListText.setText("Unwaitlist");
                            } else {
                                waitingListButton.setImageResource(R.drawable.ic_waitlist);
                                waitingListText.setText("Waitlist");
                            }
                            waitingListButton.setVisibility(View.VISIBLE);
                            waitingListText.setVisibility(View.VISIBLE);
                            cartIcon.setVisibility(View.GONE);
                            cartText.setVisibility(View.GONE);
                        } else {
                            joinIcon.setVisibility(View.VISIBLE);
                            joinText.setVisibility(View.VISIBLE);
                            waitingListButton.setVisibility(View.GONE);
                            waitingListText.setVisibility(View.GONE);
                            chatIcon.setVisibility(View.GONE);
                            chatText.setVisibility(View.GONE);
                            leaveButton.setVisibility(View.GONE);
                            leaveText.setVisibility(View.GONE);
                            cartIcon.setVisibility(View.GONE);
                            cartText.setVisibility(View.GONE);
                        }
                    }
                }
            }
            else {
                Log.e("OrderDetailsActivity", "Document does not exist");
                progressDialog.dismiss();
                Intent intent = new Intent(OrderDetailsActivity.this, OrderDeletedActivity.class);
                intent.putExtra("userType", globalUserType);
                startActivity(intent);
                finish();
            }
//            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            Log.e("OrderDetailsActivity", "Error fetching order details", e);
            progressDialog.dismiss();
        });
    }

    // Share order details via dynamic link
    private void shareOrderDetails() {
        createDynamicLink(orderId, globalUserType, shortLink -> {
            if (shortLink != null) {
                String shareText = buildShareText(shortLink);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                shareIntent.setType("text/plain");

                Intent chooser = Intent.createChooser(shareIntent, "Share Order Details");
                if (shareIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(chooser);
                }
            } else {
                Log.e("OrderDetailsActivity", "Error creating short link");
            }
            progressDialog.dismiss();
        });
    }

    // Build share text with order details
    private String buildShareText(String shortLink) {
        String title = titleTextView.getText().toString();
        String category = categoryTextView.getText().toString();
        Timestamp timestamp = getTimestampFromTextView(timeTextView);
        String time = timestamp != null ? convertTimestampToString(timestamp) : "N/A";
        String address = addressTextView.getText().toString();

        return "Order Details:\n\n" +
                "Title: " + title + "\n" +
                "Category: " + category + "\n" +
                "End Time: " + time + "\n" +
                "Address: " + address + "\n\n" +
                "Join the order: " + shortLink;
    }

    // Get timestamp from TextView tag
    private Timestamp getTimestampFromTextView(TextView textView) {
        // Fetch timestamp from TextView if it's stored in a tag or another way
        Object tag = textView.getTag();
        return tag instanceof Timestamp ? (Timestamp) tag : null;
    }

    // Convert Timestamp to String
    private String convertTimestampToString(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Date date = timestamp.toDate();
        return sdf.format(date);
    }

    // Create dynamic link for sharing
    private void createDynamicLink(String orderId, String userType, DynamicLinkCallback callback) {
        String domainUriPrefix = "https://coshopapp.page.link";
        String deepLink = "https://coshopapp.page.link/order?orderId=" + orderId + "&userType=" + userType;

        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(Uri.parse(domainUriPrefix + "/?" +
                        "link=" + Uri.encode(deepLink) +
                        "&apn=" + getPackageName()))
                .buildShortDynamicLink()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        callback.onLinkCreated(shortLink.toString());
                    } else {
                        Log.e("DynamicLink", "Error creating short link", task.getException());
                        callback.onLinkCreated(null);
                    }
                });
    }

    // Callback interface for dynamic link creation
    interface DynamicLinkCallback {
        void onLinkCreated(String shortLink);
    }

    // Remove user from waiting list
    private void removeUserFromWaitingList() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            List<String> waitingList = (List<String>) snapshot.get("waitingList");
            if (waitingList != null && waitingList.contains(email)) {
                transaction.update(orderRef, "waitingList", FieldValue.arrayRemove(email));
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            waitingListButton.setImageResource(R.drawable.ic_waitlist); // Update button text
            waitingListText.setText("Waitlist");
            inWaitingList = false; // Update the inWaitingList flag
            waitingListButton.setVisibility(View.VISIBLE); // Show the waiting list button
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            Log.e("OrderDetailsActivity", "Error removing from waiting list", e);
            progressDialog.dismiss();
        });
    }

    // Fetch and show users in order
    private void fetchAndShowUsersInOrder(List<String> userEmails, String orderCreatorEmail, String currentUserEmail) {
        // Reorder list current user first, then order creator, then others
        List<String> reorderedList = getReorderedList(userEmails, orderCreatorEmail, currentUserEmail);

        // Create a list of tasks for fetching user details
        List<Task<DocumentSnapshot>> userDetailTasks = new ArrayList<>();
        for (String email : reorderedList) {
            DocumentReference userRef = db.collection("users").document(email);
            userDetailTasks.add(userRef.get());
        }

        // Wait for all user detail tasks to complete
        Tasks.whenAllSuccess(userDetailTasks).addOnSuccessListener(results -> {
            List<UserDetail> userDetailList = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                DocumentSnapshot documentSnapshot = (DocumentSnapshot) results.get(i);
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("first name");
                    String familyName = documentSnapshot.getString("family name");
                    String email = documentSnapshot.getId();

                    // computing user rating
                    String userRating;
                    List<Map<String, Object>> ratings = (List<Map<String, Object>>) documentSnapshot.get("ratings");

                    double averageRating = 0.0;
                    if (ratings != null && !ratings.isEmpty()) {
                        int totalRating = 0;
                        for (Map<String, Object> rating : ratings) {
                            totalRating += ((Long) rating.get("rating")).intValue();
                        }
                        averageRating = (double) totalRating / ratings.size();
                    }

                    if (averageRating == (long) averageRating) {
                        userRating = String.format(Locale.getDefault(), "%.0f", averageRating);
                    } else {
                        userRating = String.format(Locale.getDefault(), "%.1f", averageRating);
                    }
                    String profilePicUrl = documentSnapshot.getString("profileImageUrl");
                    boolean isCurrentUser = documentSnapshot.getId().equals(currentUserEmail);
                    boolean isOrderCreator = documentSnapshot.getId().equals(orderCreatorEmail);

                    userDetailList.add(new UserDetail(email, firstName, familyName, userRating, profilePicUrl, isOrderCreator, isCurrentUser));
                }
            }
            // Show the users in order after fetching all details
            showUsersInOrder(userDetailList);
        }).addOnFailureListener(e -> {
            Log.e("OrderDetailsActivity", "Error fetching user details", e);
        });
    }

    // Reorder list current user first, then order creator, then others
    private static @NonNull List<String> getReorderedList(List<String> userEmails, String orderCreatorEmail, String currentUserEmail) {
        List<String> reorderedList = new ArrayList<>();
        if (userEmails.contains(currentUserEmail)) {
            reorderedList.add(currentUserEmail);
        }
        if (!currentUserEmail.equals(orderCreatorEmail)) {
            reorderedList.add(orderCreatorEmail); // Add order creator if not the same as current user
        }
        for (String email : userEmails) {
            if (!email.equals(currentUserEmail) && !email.equals(orderCreatorEmail)) {
                reorderedList.add(email);
            }
        }
        return reorderedList;
    }

    // Add user to waiting list
    private void addUserToWaitingList() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            List<String> waitingList = (List<String>) snapshot.get("waitingList");
            if (waitingList == null) {
                waitingList = new ArrayList<>();
            }
            if (!waitingList.contains(email)) {
                transaction.update(orderRef, "waitingList", FieldValue.arrayUnion(email));
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            inWaitingList = true; // Update the inWaitingList flag
            waitingListButton.setImageResource(R.drawable.ic_unwaitlist); // Update button text
            waitingListText.setText("Unwaitlist");
            waitingListButton.setVisibility(View.VISIBLE); // Show the waiting list button
            waitingListText.setVisibility(View.VISIBLE);
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            Log.e("OrderDetailsActivity", "Error adding to waiting list", e);
            progressDialog.dismiss();
        });
    }

    // Remove user from order list and update order info
    private Task<Void> removeUserFromOrderList(String orderId, String userEmail) {
        DocumentReference orderRef = db.collection("orders").document(orderId);

        return orderRef.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                List<String> listPeopleInOrder = (List<String>) snapshot.get("listPeopleInOrder");
                List<String> waitingList = (List<String>) snapshot.get("waitingList");
                String currentOrderCreator = snapshot.getString("user_email");

                if (listPeopleInOrder != null && listPeopleInOrder.contains(userEmail)) {
                    listPeopleInOrder.remove(userEmail);
                    inOrder = false;
                    Long numberOfPeopleInOrder = snapshot.getLong("NumberOfPeopleInOrder");
                    numberOfPeopleInOrder = (numberOfPeopleInOrder != null) ? numberOfPeopleInOrder - 1 : 0;

                    // Check if user is the order creator
                    if (userEmail.equals(currentOrderCreator) && !listPeopleInOrder.isEmpty()) {
                        currentOrderCreator = listPeopleInOrder.get(0);
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("listPeopleInOrder", listPeopleInOrder);
                    updates.put("NumberOfPeopleInOrder", numberOfPeopleInOrder);
                    updates.put("user_email", currentOrderCreator);

                    // Move user from waiting list if exists
                    if (waitingList != null && !waitingList.isEmpty()) {
                        String firstWaitingUser = waitingList.remove(0);
                        listPeopleInOrder.add(firstWaitingUser);
                        numberOfPeopleInOrder += 1;

                        updates.put("waitingList", waitingList);
                        updates.put("listPeopleInOrder", listPeopleInOrder);
                        updates.put("NumberOfPeopleInOrder", numberOfPeopleInOrder);
                    }

                    return orderRef.update(updates);
                } else {
                    return Tasks.forException(new Exception("User not part of the order"));
                }
            } else {
                return Tasks.forException(task.getException());
            }
        });
    }

    // Update order info after removal
    private Task<Void> updateOrderInfoAfterRemoval(String orderId, String userEmail) {
        DocumentReference orderRef = db.collection("orders").document(orderId);

        return orderRef.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                List<String> listPeopleInOrder = (List<String>) snapshot.get("listPeopleInOrder");

                if (listPeopleInOrder == null || listPeopleInOrder.isEmpty()) {
                    // Delete the order if no one is left
                    orderDeleted = true;
                    return orderRef.delete();
                } else {
                    // Ensure the correct order creator is set
                    String currentOrderCreator = snapshot.getString("user_email");
                    if (userEmail.equals(currentOrderCreator) && !listPeopleInOrder.isEmpty()) {
                        currentOrderCreator = listPeopleInOrder.get(0);
                        return orderRef.update("user_email", currentOrderCreator);
                    }
                    return Tasks.forResult(null);
                }
            } else {
                return Tasks.forException(task.getException());
            }
        });
    }

    // Check and delete order chat if needed
    private Task<Void> checkAndDeleteOrderChatIfNeeded(String orderId) {
        if (orderDeleted) {
            return deleteOrderChat(orderId);
        } else {
            return Tasks.forResult(null);
        }
    }

    // Delete order chat
    private Task<Void> deleteOrderChat(String orderId) {
        DocumentReference orderRef = db.collection("orders").document(orderId);

        return orderRef.collection("chat").get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (DocumentSnapshot document : documents) {
                    deleteTasks.add(document.getReference().delete());
                }
                return Tasks.whenAll(deleteTasks);
            } else {
                return Tasks.forException(task.getException());
            }
        });
    }

    // Remove user from order
    private void removeUserFromOrder() {
        String userEmail = mAuth.getCurrentUser().getEmail();
        removeUserFromOrderList(orderId, userEmail)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        // Remove items added by the user from the cart
                        return removeItemsFromCartByUser(orderId, userEmail);
                    } else {
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return updateOrderInfoAfterRemoval(orderId, userEmail);
                    } else {
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return checkAndDeleteOrderChatIfNeeded(orderId);
                    } else {
                        throw task.getException();
                    }
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (orderDeleted) {
                            Intent intent = new Intent(OrderDetailsActivity.this, MyOrdersActivity.class);
                            intent.putExtra("userType", globalUserType);
                            startActivity(intent);
                            finish();
                        } else {
                            fetchOrderDetails(orderId, userEmail);
                        }
                    } else {
                        Log.e("OrderDetailsActivity", "Error leaving the order", task.getException());
                    }
                    progressDialog.dismiss();
                });
    }

    // Remove items from cart added by the user
    private Task<Void> removeItemsFromCartByUser(String orderId, String userEmail) {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        return orderRef.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                List<Map<String, Object>> cartItems = (List<Map<String, Object>>) snapshot.get("cartItems");

                if (cartItems != null) {
                    List<Map<String, Object>> updatedCartItems = new ArrayList<>();
                    for (Map<String, Object> item : cartItems) {
                        if (!userEmail.equals(item.get("userEmail"))) {
                            updatedCartItems.add(item);
                        }
                    }
                    return orderRef.update("cartItems", updatedCartItems);
                } else {
                    return Tasks.forResult(null);
                }
            } else {
                return Tasks.forException(task.getException());
            }
        });
    }

    // Show users in order
    private void showUsersInOrder(List<UserDetail> userDetailList) {
        userListLayout.removeAllViews();
        int displayCount = showAllUsers ? userDetailList.size() : Math.min(userDetailList.size(), 3);

        for (int i = 0; i < displayCount; i++) {
            UserDetail userDetail = userDetailList.get(i);
            addUserToLayout(userDetail.email, userDetail.firstName + " " + userDetail.familyName, userDetail.userRating, userDetail.profilePicUrl, userDetail.isOrderCreator, userDetail.isCurrentUser);
        }

        if (userDetailList.size() > 3) {
            TextView toggleUsersTextView = new TextView(this);
            toggleUsersTextView.setText(showAllUsers ? "Show Less Users" : "View All Users");
            toggleUsersTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
            toggleUsersTextView.setBackgroundResource(R.drawable.border);
            toggleUsersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); // Adjust the text size to match user views
            toggleUsersTextView.setPadding(16, 16, 16, 16); // Adjust padding to match user views
            toggleUsersTextView.setGravity(Gravity.CENTER); // Center the text

            toggleUsersTextView.setOnClickListener(v -> {
                showAllUsers = !showAllUsers; // Toggle the state
                showUsersInOrder(userDetailList);
            });

            userListLayout.addView(toggleUsersTextView);
        }
        progressDialog.dismiss();
    }

    // User detail class
    private static class UserDetail {
        String email;
        String firstName;
        String familyName;
        String userRating;
        String profilePicUrl;
        boolean isOrderCreator;
        boolean isCurrentUser;

        UserDetail(String email, String firstName, String familyName, String userRating, String profilePicUrl, boolean isOrderCreator, boolean isCurrentUser) {
            this.email = email;
            this.firstName = firstName;
            this.familyName = familyName;
            this.userRating = userRating;
            this.profilePicUrl = profilePicUrl;
            this.isOrderCreator = isOrderCreator;
            this.isCurrentUser = isCurrentUser;
        }
    }

    // Add user to layout
    private void addUserToLayout(String displayedEmail, String userName, String userRating, String profilePicUrl, boolean isOrderCreator, boolean isCurrentUser) {
        View userItemView = getLayoutInflater().inflate(R.layout.user_item, userListLayout, false);

        TextView userNameTextView = userItemView.findViewById(R.id.userNameTextView);
        TextView userRatingTextView = userItemView.findViewById(R.id.userRatingTextView);
        ImageView userProfileImageView = userItemView.findViewById(R.id.userProfileImageView);
        ImageView reportImageView = userItemView.findViewById(R.id.reportImageView);
        ImageView rateImageView = userItemView.findViewById(R.id.rateImageView);
        ImageView addToCartImageView = userItemView.findViewById(R.id.addToCartImageView); // Add this line

        // Create a SpannableString that will hold the image and the text
        SpannableStringBuilder ssb = new SpannableStringBuilder(" " + " " + userRating + "/5");

        // Create an ImageSpan with the drawable
        Drawable drawable = getResources().getDrawable(R.drawable.ic_star);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_CENTER);
        // Set the ImageSpan at the beginning of the SpannableString
        ssb.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        // Set the SpannableString to the TextView
        userRatingTextView.setText(ssb);
        userNameTextView.setText(userName);
        Glide.with(this)
                .load(profilePicUrl)
                .apply(new RequestOptions().transform(new CircleCrop()))
                .error(R.drawable.ic_profile)
                .into(userProfileImageView);

        if (isOrderCreator) {
            userItemView.setBackgroundResource(R.drawable.border_creator);
        }

        if (!inOrder) {
            reportImageView.setVisibility(View.GONE);
            rateImageView.setVisibility(View.GONE);
            addToCartImageView.setVisibility(View.GONE);
        } else {
            // Hide report and rate icons for current user
            if (isCurrentUser) {
                reportImageView.setVisibility(View.GONE);
                rateImageView.setVisibility(View.GONE);
                userNameTextView.setText("You");
                addToCartImageView.setVisibility(View.VISIBLE);
                addToCartImageView.setOnClickListener(v -> {
                    showAddToCartDialog(displayedEmail, null);
                });
            } else {
                addToCartImageView.setVisibility(View.GONE);
                // Handle report and rate icons
                reportImageView.setOnClickListener(v -> {
                    // Handle report user
                    showReportDialog(displayedEmail);
                });

                rateImageView.setOnClickListener(v -> {
                    // Handle rate user
                    showRatingDialog(displayedEmail);
                });
            }
        }

        userListLayout.addView(userItemView);
    }

      // Show cart dialog with items
    private void showCartDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_cart);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(layoutParams);

        LinearLayout cartLayout = dialog.findViewById(R.id.cartLayout);
        Button closeButton = dialog.findViewById(R.id.closeButton);

        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) documentSnapshot.get("cartItems");

                        if (cartItems == null || cartItems.isEmpty()) {
                            // Display message if there are no items in the cart
                            TextView noItemsTextView = new TextView(this);
                            noItemsTextView.setText("There are no items in the cart");
                            noItemsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                            noItemsTextView.setTypeface(null, Typeface.BOLD);
                            noItemsTextView.setPadding(0, 16, 0, 8);
                            noItemsTextView.setGravity(Gravity.CENTER);
                            cartLayout.addView(noItemsTextView);
                        } else {
                            Map<String, List<Map<String, Object>>> userItemsMap = new HashMap<>();

                            // Group items by user
                            for (Map<String, Object> item : cartItems) {
                                String userEmail = (String) item.get("userEmail");
                                if (!userItemsMap.containsKey(userEmail)) {
                                    userItemsMap.put(userEmail, new ArrayList<>());
                                }
                                userItemsMap.get(userEmail).add(item);
                            }

                            // Display grouped items
                            for (Map.Entry<String, List<Map<String, Object>>> entry : userItemsMap.entrySet()) {
                                String userEmail = entry.getKey();
                                List<Map<String, Object>> items = entry.getValue();

                                TextView userNameTextView = new TextView(this);
                                userNameTextView.setText(userEmail); // Display user name instead of email
                                userNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                userNameTextView.setTypeface(null, Typeface.BOLD);
                                userNameTextView.setPadding(0, 16, 0, 8);
                                cartLayout.addView(userNameTextView);

                                for (Map<String, Object> item : items) {
                                    View itemLayout = getLayoutInflater().inflate(R.layout.item_cart_add, cartLayout, false);
                                    EditText itemUrlEditText = itemLayout.findViewById(R.id.itemUrlEditText);
                                    EditText itemPriceEditText = itemLayout.findViewById(R.id.itemPriceEditText);
                                    ImageView deleteItemIcon = itemLayout.findViewById(R.id.deleteItemIcon);
                                    ImageView copyUrlIcon = itemLayout.findViewById(R.id.copyUrlIcon);

                                    String url = (String) item.get("url");
                                    String price = (String) item.get("price");

                                    itemUrlEditText.setText(url);
                                    if (price.isEmpty()) {
                                        itemPriceEditText.setVisibility(View.GONE);
                                    } else {
                                        itemPriceEditText.setText(price);
                                        itemPriceEditText.setVisibility(View.VISIBLE);
                                    }

                                    // Check if URL is valid and set the copy icon visibility
                                    if (isValidURL(url)) {
                                        copyUrlIcon.setVisibility(View.VISIBLE);
                                        itemUrlEditText.setTextColor(ContextCompat.getColor(this, R.color.hyperlink_blue));
                                        copyUrlIcon.setOnClickListener(v -> {
                                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clip = ClipData.newPlainText("Copied URL", url);
                                            clipboard.setPrimaryClip(clip);
                                            Log.d("OrderDetailsActivity", "URL copied to clipboard");
                                        });
                                    } else {
                                        copyUrlIcon.setVisibility(View.GONE);
                                        itemUrlEditText.setTextColor(ContextCompat.getColor(this, R.color.black));
                                    }

                                    // Show the delete icon only if the current user is the one who added the item
                                    if (userEmail.equals(currentUser.getEmail())) {
                                        deleteItemIcon.setVisibility(View.VISIBLE);
                                        deleteItemIcon.setOnClickListener(v -> {
                                            cartLayout.removeView(itemLayout);
                                            removeItemFromCart(item); // Remove item from Firestore
                                        });
                                    } else {
                                        deleteItemIcon.setVisibility(View.GONE);
                                    }

                                    cartLayout.addView(itemLayout);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("OrderDetailsActivity", "Error fetching cart items", e));

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void removeItemFromCart(Map<String, Object> item) {
        db.collection("orders").document(orderId)
                .update("cartItems", FieldValue.arrayRemove(item))
                .addOnSuccessListener(aVoid -> Log.d("OrderDetailsActivity", "Item removed from cart"))
                .addOnFailureListener(e -> Log.e("OrderDetailsActivity", "Error removing item from cart", e));
    }

   // Show add to cart dialog
    private void showAddToCartDialog(String userEmail, String sharedLink) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_to_cart);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels);
        dialog.getWindow().setAttributes(layoutParams);

        LinearLayout itemsContainer = dialog.findViewById(R.id.itemsContainer);
        Button addItemButton = dialog.findViewById(R.id.addItemButton);
        ImageView addItemIcon = dialog.findViewById(R.id.addItemIcon);
        TextView addItemText = dialog.findViewById(R.id.addItemText);

        // Add initial item layout
        addItemLayout(itemsContainer, sharedLink);

        addItemIcon.setOnClickListener(v -> {
            View itemView = itemsContainer.getChildAt(itemsContainer.getChildCount() - 1);
            EditText itemUrlEditText = itemView.findViewById(R.id.itemUrlEditText);
            TextView urlError = itemView.findViewById(R.id.urlError);
            LinearLayout itemUrlLayout = itemView.findViewById(R.id.itemUrlLayout);

            String itemUrl = itemUrlEditText.getText().toString().trim();

            if (itemUrl.isEmpty()) {
                urlError.setVisibility(View.VISIBLE);
                itemUrlLayout.setBackgroundResource(R.drawable.red_border);
                return;
            } else {
                urlError.setVisibility(View.GONE);
                itemUrlLayout.setBackgroundResource(R.drawable.border);
            }

            addItemLayout(itemsContainer, null);
        });

        addItemText.setOnClickListener(v -> {
            View itemView = itemsContainer.getChildAt(itemsContainer.getChildCount() - 1);
            EditText itemUrlEditText = itemView.findViewById(R.id.itemUrlEditText);
            TextView urlError = itemView.findViewById(R.id.urlError);
            LinearLayout itemUrlLayout = itemView.findViewById(R.id.itemUrlLayout);

            String itemUrl = itemUrlEditText.getText().toString().trim();

            if (itemUrl.isEmpty()) {
                urlError.setVisibility(View.VISIBLE);
                itemUrlLayout.setBackgroundResource(R.drawable.red_border);
                return;
            } else {
                urlError.setVisibility(View.GONE);
                itemUrlLayout.setBackgroundResource(R.drawable.border);
            }

            addItemLayout(itemsContainer, null);
        });

        addItemButton.setOnClickListener(v -> {
            List<Map<String, String>> items = new ArrayList<>();

            for (int i = 0; i < itemsContainer.getChildCount(); i++) {
                View itemView = itemsContainer.getChildAt(i);
                EditText itemUrlEditText = itemView.findViewById(R.id.itemUrlEditText);
                EditText itemPriceEditText = itemView.findViewById(R.id.itemPriceEditText);
                TextView urlError = itemView.findViewById(R.id.urlError);
                LinearLayout itemUrlLayout = itemView.findViewById(R.id.itemUrlLayout);

                String itemUrl = itemUrlEditText.getText().toString().trim();
                String itemPrice = itemPriceEditText.getText().toString().trim();

                if (itemUrl.isEmpty()) {
                    urlError.setVisibility(View.VISIBLE);
                    itemUrlLayout.setBackgroundResource(R.drawable.red_border);
                    return;
                } else {
                    urlError.setVisibility(View.GONE);
                    itemUrlLayout.setBackgroundResource(R.drawable.border);
                }

                Map<String, String> item = new HashMap<>();
                item.put("url", itemUrl);
                item.put("price", itemPrice);
                items.add(item);
            }

            for (Map<String, String> item : items) {
                addItemToCart(userEmail, item.get("url"), item.get("price"));
            }

            dialog.dismiss();
        });

        dialog.show();
    }


     // Add item layout for cart
    private void addItemLayout(LinearLayout container, String sharedLink) {
        View itemLayout = getLayoutInflater().inflate(R.layout.item_cart_add, container, false);
        ImageView deleteItemIcon = itemLayout.findViewById(R.id.deleteItemIcon);
        EditText addItemText = itemLayout.findViewById(R.id.itemUrlEditText);

        if (sharedLink != null) {
            addItemText.setText(sharedLink);
            addItemText.setTextColor(ContextCompat.getColor(this, R.color.hyperlink_blue));
        }
        deleteItemIcon.setOnClickListener(v -> {
            container.removeView(itemLayout);
        });

        container.addView(itemLayout);
    }

      // Add item to cart in Firestore
    private void addItemToCart(String userEmail, String itemText, String itemPrice) {
        DocumentReference orderRef = db.collection("orders").document(orderId);

        String extractedUrl = extractUrl(itemText);
        if (extractedUrl != null && isValidURL(extractedUrl)) {
            itemText = extractedUrl;  // Save only the URL if it's valid
        }

        // Create a unique identifier for each cart item
        String uniqueId = orderRef.collection("cartItems").document().getId();

        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("id", uniqueId);  // Add the unique identifier to the cart item
        cartItem.put("userEmail", userEmail);
        cartItem.put("url", itemText);  // Save the itemText which might be a URL or the full text
        cartItem.put("price", itemPrice); // Optional: Include price if necessary

        orderRef.update("cartItems", FieldValue.arrayUnion(cartItem))
                .addOnSuccessListener(aVoid -> Log.d("OrderDetailsActivity", "Item added to cart"))
                .addOnFailureListener(e -> Log.e("OrderDetailsActivity", "Error adding item to cart", e));
    }

    // Show rating dialog
    private void showRatingDialog(String email) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rate_user);

        // Initialize the stars
        ImageView[] stars = new ImageView[5];
        stars[0] = dialog.findViewById(R.id.star1);
        stars[1] = dialog.findViewById(R.id.star2);
        stars[2] = dialog.findViewById(R.id.star3);
        stars[3] = dialog.findViewById(R.id.star4);
        stars[4] = dialog.findViewById(R.id.star5);

        final int[] selectedRating = {0};

        // Fetch existing rating if any
        DocumentReference userRef = db.collection("users").document(email);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> ratings = (List<Map<String, Object>>) documentSnapshot.get("ratings");
                if (ratings != null) {
                    for (Map<String, Object> rating : ratings) {
                        if (rating.get("email").equals(currentUser.getEmail()) && rating.get("order").equals(orderId)) {
                            selectedRating[0] = ((Long) rating.get("rating")).intValue();
                            updateStarUI(stars, selectedRating[0]);
                            break;
                        }
                    }
                }
            }
        }).addOnFailureListener(e ->
                Log.e("OrderDetailsActivity", "Error fetching existing rating", e));

        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i;
            stars[i].setOnClickListener(v -> {
                if (selectedRating[0] == starIndex + 1) {
                    selectedRating[0] = 0;
                } else {
                    selectedRating[0] = starIndex + 1;
                }
                updateStarUI(stars, selectedRating[0]);
            });
        }

        Button submitButton = dialog.findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            progressDialog.show();
            updateRating(email, orderId, selectedRating[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    // Show report dialog
    private void showReportDialog(String reportedEmail) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_report_user);

        Spinner reportReasonSpinner = dialog.findViewById(R.id.reportReasonSpinner);
        EditText reportDetailsEditText = dialog.findViewById(R.id.reportDetailsEditText);
        TextView errorTextView = dialog.findViewById(R.id.errorTextView);
        ImageView clearReportDetailsIcon = dialog.findViewById(R.id.clearReportDetailsIcon);
        LinearLayout reportDetailsContainer = dialog.findViewById(R.id.reportDetailsContainer);
        Button submitReportButton = dialog.findViewById(R.id.submitReportButton);

        reportReasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedReason = parent.getItemAtPosition(position).toString();
                if (selectedReason.equals("Other")) {
                    reportDetailsEditText.setHint("Additional details");
                } else {
                    reportDetailsEditText.setHint("Additional details (optional)");
                }
                errorTextView.setVisibility(View.GONE); // Hide error when selection changes
                reportDetailsContainer.setBackgroundResource(R.drawable.border); // Reset border
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        reportDetailsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearReportDetailsIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
                errorTextView.setVisibility(View.GONE); // Hide error when text changes
                reportDetailsContainer.setBackgroundResource(R.drawable.border); // Reset border
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearReportDetailsIcon.setOnClickListener(v -> reportDetailsEditText.setText(""));

        submitReportButton.setOnClickListener(v -> {
            String selectedReason = reportReasonSpinner.getSelectedItem().toString();
            String additionalDetails = reportDetailsEditText.getText().toString().trim();

            if (selectedReason.equals("Other") && additionalDetails.isEmpty()) {
                errorTextView.setVisibility(View.VISIBLE);
                reportDetailsContainer.setBackgroundResource(R.drawable.red_border); // Change border to red
            } else {
                errorTextView.setVisibility(View.GONE);
                reportDetailsContainer.setBackgroundResource(R.drawable.border); // Reset border
                progressDialog.show();
                submitReport(reportedEmail, orderId, selectedReason, additionalDetails);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Submit user report
    private void submitReport(String reportedUserEmail, String orderID, String reason, String details) {
        if (currentUser != null) {
            String reporterEmail = email;

            Map<String, Object> reportData = new HashMap<>();
            reportData.put("orderId", orderID);
            reportData.put("reporterEmail", reporterEmail);
            reportData.put("reason", reason);
            reportData.put("details", details);
            reportData.put("timestamp", new Timestamp(new Date()));

            DocumentReference docRef = db.collection("reports").document(reportedUserEmail);

            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<Map<String, Object>> reports = (List<Map<String, Object>>) documentSnapshot.get("reports");
                    boolean alreadyReported = false;

                    if (reports != null) {
                        for (Map<String, Object> report : reports) {
                            if (reporterEmail.equals(report.get("reporterEmail")) && orderID.equals(report.get("orderId"))) {
                                alreadyReported = true;
                                break;
                            }
                        }
                    }

                    if (alreadyReported) {
                        progressDialog.dismiss();
                        showAlertDialog("You have already reported this user in this order");
                    } else {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("reports", FieldValue.arrayUnion(reportData));

                        docRef.update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    checkAndBlockUser(reportedUserEmail, reports.size() + 1);
                                    progressDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                });
                    }
                } else {
                    Map<String, Object> newDocData = new HashMap<>();
                    newDocData.put("reports", Arrays.asList(reportData));

                    docRef.set(newDocData)
                            .addOnSuccessListener(aVoid -> {
                                checkAndBlockUser(reportedUserEmail, 1);
                                progressDialog.dismiss();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Log.e("OrderDetailsActivity", "Error creating report document", e);
                            });
                }
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e("OrderDetailsActivity", "Error fetching report document", e);
            });
        } else {
            progressDialog.dismiss();
            Log.e("OrderDetailsActivity", "Current user is null");
        }
    }

    // Check and block user if report count exceeds limit
    private void checkAndBlockUser(String userEmail, int reportCount) {
        if (reportCount == 5) {
            DocumentReference userRef = db.collection("users").document(userEmail);
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("blocked", true);
            updateData.put("blockedTimestamp", new Timestamp(new Date()));

            userRef.update(updateData).addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
            }).addOnFailureListener(e -> {
                progressDialog.dismiss();
            });
        }
    }

    // Show alert dialog with message
    private void showAlertDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Do nothing
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Update star UI for rating dialog
    private void updateStarUI(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star);
            } else {
                stars[i].setImageResource(R.drawable.star2);
            }
        }
    }

    // Update user rating in Firestore
    private void updateRating(String email, String order, int newRating) {
        DocumentReference userRef = db.collection("users").document(email);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);

            // Get the current ratings
            List<Map<String, Object>> ratings = (List<Map<String, Object>>) snapshot.get("ratings");
            if (ratings == null) {
                ratings = new ArrayList<>();
            }

            boolean ratingExists = false;
            for (Map<String, Object> rating : ratings) {
                if (rating.get("email").equals(currentUser.getEmail()) && rating.get("order").equals(order)) {
                    ratingExists = true;
                    rating.put("rating", newRating); // Update existing rating
                    break;
                }
            }

            if (!ratingExists) {
                // Add new rating if not exists
                Map<String, Object> newRatingMap = new HashMap<>();
                newRatingMap.put("email", currentUser.getEmail());
                newRatingMap.put("order", order);
                newRatingMap.put("rating", newRating);
                ratings.add(newRatingMap);
            }

            transaction.update(userRef, "ratings", ratings);
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Refresh the user list to show updated ratings
            fetchOrderDetails(orderId, currentUser.getEmail());
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
        });
    }

    // Get address from latitude and longitude
    private String getAddressFromLatLng(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    // Fetch user details and update group info
    private void fetchUserDetails(String userEmail, String formattedOpenOrderTime) {
        DocumentReference userRef = db.collection("users").document(userEmail);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("first name");
                String familyName = documentSnapshot.getString("family name");

                String fullname = null;
                if (firstName != null) {
                    fullname = firstName;
                    if (familyName != null) {
                        fullname += " " + familyName;
                    }
                }

                if (fullname != null) {
                    groupInfoTextView.setText("Order created by " + fullname + "\non " + formattedOpenOrderTime);
                } else {
                    groupInfoTextView.setText("Created by " + userEmail + " on " + formattedOpenOrderTime);
                    Log.e("OrderDetailsActivity", "First name or family name is null for user: " + userEmail);
                }
            } else {
                groupInfoTextView.setText("Order created by " + userEmail + "\non " + formattedOpenOrderTime);
                Log.e("OrderDetailsActivity", "No document found for user: " + userEmail);
            }
        }).addOnFailureListener(e -> {
            groupInfoTextView.setText("Order created by " + userEmail + "\non " + formattedOpenOrderTime);
            Log.e("OrderDetailsActivity", "Failed to fetch user details", e);
        });
    }

    // Update timer text views
    private void updateTimerTextViews(LinearLayout daysContainer, TextView daysTextView, TextView colon1, LinearLayout hoursContainer, TextView hoursTextView, TextView colon2, LinearLayout minutesContainer, TextView minutesTextView, TextView colon3, LinearLayout secondsContainer, TextView secondsTextView, long millisUntilFinished) {
        long seconds = millisUntilFinished / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds = seconds % 60;
        minutes = minutes % 60;
        hours = hours % 24;

        if (days > 0) {
            daysContainer.setVisibility(View.VISIBLE);
            colon1.setVisibility(View.VISIBLE);
            secondsContainer.setVisibility(View.GONE);
            colon3.setVisibility(View.GONE);

            daysTextView.setText(String.format(Locale.getDefault(), "%02d", days));
            hoursTextView.setText(String.format(Locale.getDefault(), "%02d", hours));
            minutesTextView.setText(String.format(Locale.getDefault(), "%02d", minutes));
        } else {
            daysContainer.setVisibility(View.GONE);
            colon1.setVisibility(View.GONE);
            secondsContainer.setVisibility(View.VISIBLE);
            colon3.setVisibility(View.VISIBLE);

            hoursTextView.setText(String.format(Locale.getDefault(), "%02d", hours));
            minutesTextView.setText(String.format(Locale.getDefault(), "%02d", minutes));
            secondsTextView.setText(String.format(Locale.getDefault(), "%02d", seconds));
        }
    }

    // Start countdown timer
    private void startCountdownTimer(Timestamp timestamp) {
        LinearLayout timerContainer = findViewById(R.id.timerContainer);
        LinearLayout daysContainer = findViewById(R.id.daysContainer);
        TextView daysTextView = findViewById(R.id.daysTextView);
        TextView colon1 = findViewById(R.id.colon1);
        LinearLayout hoursContainer = findViewById(R.id.hoursContainer);
        TextView hoursTextView = findViewById(R.id.hoursTextView);
        TextView colon2 = findViewById(R.id.colon2);
        LinearLayout minutesContainer = findViewById(R.id.minutesContainer);
        TextView minutesTextView = findViewById(R.id.minutesTextView);
        TextView colon3 = findViewById(R.id.colon3);
        LinearLayout secondsContainer = findViewById(R.id.secondsContainer);
        TextView secondsTextView = findViewById(R.id.secondsTextView);

        // Add this line to ensure the timer layout is left-to-right
        ViewCompat.setLayoutDirection(timerContainer, ViewCompat.LAYOUT_DIRECTION_LTR);

        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            Log.d("OrderDetailsActivity", "Time remaining: " + timeRemaining);
            timerContainer.setVisibility(View.VISIBLE);
            closedOrderTextView.setVisibility(View.GONE);
            new CountDownTimer(timeRemaining, 1000) {

                public void onTick(long millisUntilFinished) {
                    updateTimerTextViews(daysContainer, daysTextView, colon1, hoursContainer, hoursTextView, colon2, minutesContainer, minutesTextView, colon3, secondsContainer, secondsTextView, millisUntilFinished);
                }

                public void onFinish() {
                    daysTextView.setText("00");
                    hoursTextView.setText("00");
                    minutesTextView.setText("00");
                    secondsTextView.setText("00");
                }
            }.start();
        } else {
//            timerContainer.setVisibility(View.GONE);
            closedOrderTextView.setVisibility(View.VISIBLE);
            daysTextView.setText("00");
            hoursTextView.setText("00");
            minutesTextView.setText("00");
            secondsTextView.setText("00");
        }
    }

    // Check if user is in list
    private void checkUserInList() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> listPeopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
                List<String> waitingList = (List<String>) documentSnapshot.get("waitingList");

                if (listPeopleInOrder != null && listPeopleInOrder.contains(email)) {
                    inOrder = true;
                }
                if (waitingList != null && waitingList.contains(email)) {
                    inWaitingList = true;
                }
            }
        }).addOnFailureListener(e ->
                Log.e("OrderDetailsActivity", "Error checking user in list", e));
    }

    // Add user to order
    private void addUserToOrder() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            List<String> listPeopleInOrder = (List<String>) snapshot.get("listPeopleInOrder");
            String userEmail = snapshot.getString("user_email");
            if (listPeopleInOrder != null && !listPeopleInOrder.contains(email)) {
                transaction.update(orderRef, "listPeopleInOrder", FieldValue.arrayUnion(email));
                Long currentNumberOfPeople = snapshot.getLong("NumberOfPeopleInOrder");
                transaction.update(orderRef, "NumberOfPeopleInOrder", currentNumberOfPeople + 1);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            fetchOrderDetails(orderId, email);
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            Log.e("OrderDetailsActivity", "Error adding user to order", e);
            progressDialog.dismiss();
        });
    }

    private String extractUrl(String text) {
        Pattern urlPattern = Pattern.compile(
                "(https?://\\S+\\b)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Save URL to Firestore
    private void saveUrl() {
        String url = urlEditText.getText().toString().trim();
        if (url.isEmpty()) {
            Log.e("OrderDetailsActivity", "URL is empty");
            return;
        }

        if (!isValidURL(url)) {
            Log.e("OrderDetailsActivity", "Invalid URL");
            return;
        }

        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.update("URL", url)
                .addOnSuccessListener(aVoid -> {
                    urlEditText.setVisibility(View.GONE);
                    saveUrlButton.setVisibility(View.GONE);
                    fetchOrderDetails(orderId, email);

                    // Hide tap icon when URL is saved
                    ImageView tapIcon = findViewById(R.id.tap_icon);
                    Button siteButton = findViewById(R.id.siteButton);
                    ConstraintLayout urlLayout = findViewById(R.id.urlLayout);
                    urlLayout.setVisibility(View.GONE);
                    tapIcon.setVisibility(View.GONE);
                    siteButton.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Log.e("OrderDetailsActivity", "Error saving URL", e));
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
            case R.id.home:
                menuUtils.home();
                return true;
            case R.id.chat_icon:
                menuUtils.allChats();
                return true;
            case R.id.chat_notification:
                menuUtils.chat_notification();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
