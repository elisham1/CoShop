package com.elisham.coshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView descriptionTextView, siteTextView, categoryTextView, addressTextView, timeTextView, titleTextView, groupInfoTextView;
    private ImageView categoryImageView;
    private ImageView joinIcon;
    private String orderId;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Geocoder geocoder;
    private MenuUtils menuUtils;

    private LinearLayout userListLayout;
    private List<String> listPeopleInOrder;
    private boolean showAllUsers = false;
    private boolean inOrder = false;
    private ImageView chatIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        menuUtils = new MenuUtils(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        geocoder = new Geocoder(this, Locale.ENGLISH);

        // Initialize TextViews and ImageView
        descriptionTextView = findViewById(R.id.descriptionTextView);
        siteTextView = findViewById(R.id.siteTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        addressTextView = findViewById(R.id.addressTextView);
        timeTextView = findViewById(R.id.timeTextView);
        titleTextView = findViewById(R.id.titleTextView);
        groupInfoTextView = findViewById(R.id.groupInfoTextView);
        categoryImageView = findViewById(R.id.categoryImageView);
        chatIcon = findViewById(R.id.chatIcon);
        joinIcon = findViewById(R.id.joinIcon);

        // Initialize userListLayout
        userListLayout = findViewById(R.id.userListLayout);

        // Handle incoming deep link
        handleIncomingDeepLink();

        // Get the orderId from the intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");

        if (orderId != null) {
            checkUserInList();
            // Fetch order details and then user details
            fetchOrderDetails(orderId, currentUser.getEmail());
        } else {
            Toast.makeText(this, "Order ID is null", Toast.LENGTH_SHORT).show();
        }

        joinIcon.setOnClickListener(v -> {
            if (joinIcon.getContentDescription().toString().equals("Join Icon")) {
                addUserToOrder();
                inOrder = true;
            } else {
                Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
                chatIntent.putExtra("orderId", orderId);
                startActivity(chatIntent);
            }
        });

        chatIcon.setOnClickListener(v -> {
            Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("orderId", orderId);
            startActivity(chatIntent);
        });

        ImageView shareIcon = findViewById(R.id.shareIcon);
        shareIcon.setOnClickListener(v -> {
            Log.d("OrderDetailsActivity", "Share icon clicked");
            shareOrderDetails();
        });
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
                                // Open OrderDetailsActivity with the orderId
                                Intent intent = new Intent(this, OrderDetailsActivity.class);
                                intent.putExtra("orderId", orderIdFromLink);
                                startActivity(intent);
                                finish(); // End current activity
                            }
                        }
                    }
                })
                .addOnFailureListener(this, e -> Log.w("OrderDetailsActivity", "getDynamicLink:onFailure", e));
    }

    private void shareOrderDetails() {
        createDynamicLink(orderId, shortLink -> {
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
                Toast.makeText(OrderDetailsActivity.this, "Failed to create shareable link", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private Timestamp getTimestampFromTextView(TextView textView) {
        // Fetch timestamp from TextView if it's stored in a tag or another way
        Object tag = textView.getTag();
        return tag instanceof Timestamp ? (Timestamp) tag : null;
    }

    private String convertTimestampToString(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Date date = timestamp.toDate();
        return sdf.format(date);
    }

    private void createDynamicLink(String orderId, DynamicLinkCallback callback) {
        String domainUriPrefix = "https://coshopapp.page.link";
        String deepLink = "https://coshopapp.page.link/order?orderId=" + orderId;

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

    interface DynamicLinkCallback {
        void onLinkCreated(String shortLink);
    }

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
                descriptionTextView.setText("Group description:\n" + description);
                if (siteUrl != null && !siteUrl.isEmpty()) {
                    siteTextView.setText("Url/Site: " + siteUrl);
                    siteTextView.setVisibility(View.VISIBLE);
                } else {
                    siteTextView.setVisibility(View.GONE);
                }
                categoryTextView.setText(categorie);
                addressTextView.setText("Address: " + address);
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
                fetchUserDetails(userEmail, numberOfPeopleInOrder, maxPeople, formattedOpenOrderTime);

                // Update the timer
                if (timestamp != null) {
                    startCountdownTimer(timestamp);
                }

                // Fetch list of users in order
                listPeopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
                if (listPeopleInOrder != null && !listPeopleInOrder.isEmpty()) {
                    fetchAndShowUsersInOrder(listPeopleInOrder, userEmail, currentUserEmail);
                }
            } else {
                Toast.makeText(this, "No such document", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch document", Toast.LENGTH_SHORT).show());
    }

    private void fetchAndShowUsersInOrder(List<String> userEmails, String orderCreatorEmail, String currentUserEmail) {
        //Reorder list current user first, then order creator, then others
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

                    //computing user rating
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
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
        });
    }

    //Reorder list current user first, then order creator, then others
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
    }

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

    private void addUserToLayout(String email, String userName, String userRating, String profilePicUrl, boolean isOrderCreator, boolean isCurrentUser) {
        View userItemView = getLayoutInflater().inflate(R.layout.user_item, userListLayout, false);

        TextView userNameTextView = userItemView.findViewById(R.id.userNameTextView);
        TextView userRatingTextView = userItemView.findViewById(R.id.userRatingTextView);
        ImageView userProfileImageView = userItemView.findViewById(R.id.userProfileImageView);
        ImageView reportImageView = userItemView.findViewById(R.id.reportImageView);
        ImageView rateImageView = userItemView.findViewById(R.id.rateImageView);

        // Create a SpannableString that will hold the image and the text
        SpannableStringBuilder ssb = new SpannableStringBuilder(" " + " " + userRating + "/5");

        // Create an ImageSpan with the drawable
        Drawable drawable = getResources().getDrawable(R.drawable.ic_rating_face);
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
            userItemView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        }

        if (!inOrder) {
            reportImageView.setVisibility(View.GONE);
            rateImageView.setVisibility(View.GONE);
        } else {
            // Hide report and rate icons for current user
            if (isCurrentUser) {
                reportImageView.setVisibility(View.GONE);
                rateImageView.setVisibility(View.GONE);
                userNameTextView.setText("You");
            } else {
                // Handle report and rate icons
                reportImageView.setOnClickListener(v -> {
                    // Handle report user
                    showReportDialog(email);
                });

                rateImageView.setOnClickListener(v -> {
                    // Handle rate user
                    showRatingDialog(email);
                });
            }
        }

        userListLayout.addView(userItemView);
    }

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
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch existing rating", Toast.LENGTH_SHORT).show());

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
            updateRating(email, orderId, selectedRating[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showReportDialog(String email) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_report_user);

        Spinner reportReasonSpinner = dialog.findViewById(R.id.reportReasonSpinner);
        EditText reportDetailsEditText = dialog.findViewById(R.id.reportDetailsEditText);
        Button submitReportButton = dialog.findViewById(R.id.submitReportButton);

        submitReportButton.setOnClickListener(v -> {
            String selectedReason = reportReasonSpinner.getSelectedItem().toString();
            String additionalDetails = reportDetailsEditText.getText().toString().trim();
            if (selectedReason.equals("Other") && additionalDetails.isEmpty()) {
                showAlertDialog("Please provide additional details for the report");
                return;
            }

            submitReport(email, orderId, selectedReason, additionalDetails);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReport(String reportedUserEmail, String orderID, String reason, String details) {
        if (currentUser != null) {
            String reporterEmail = currentUser.getEmail();

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
                        showAlertDialog("You have already reported this user in this order");
                    } else {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("reports", FieldValue.arrayUnion(reportData));

                        docRef.update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                                    checkAndBlockUser(reportedUserEmail, reports.size() + 1);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Map<String, Object> newDocData = new HashMap<>();
                    newDocData.put("reports", Arrays.asList(reportData));

                    docRef.set(newDocData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                                checkAndBlockUser(reportedUserEmail, 1);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "You need to be logged in to report a user", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndBlockUser(String userEmail, int reportCount) {
        if (reportCount == 5) {
            DocumentReference userRef = db.collection("users").document(userEmail);
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("blocked", true);
            updateData.put("blockedTimestamp", new Timestamp(new Date()));

            userRef.update(updateData).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User has been blocked due to multiple reports", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to block user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }


    private void showAlertDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


                private void updateStarUI(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.star);
            } else {
                stars[i].setImageResource(R.drawable.star2);
            }
        }
    }

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
            Toast.makeText(this, "User rated successfully", Toast.LENGTH_SHORT).show();
            // Refresh the user list to show updated ratings
            fetchOrderDetails(orderId, currentUser.getEmail());
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to rate user", Toast.LENGTH_SHORT).show());
    }

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


    private void fetchUserDetails(String userEmail, int numberOfPeopleInOrder, int maxPeople, String formattedOpenOrderTime) {
        DocumentReference userRef = db.collection("users").document(userEmail);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("first name");
                String familyName = documentSnapshot.getString("family name");

                if (firstName != null && familyName != null) {
                    groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + firstName + " " + familyName + "\non " + formattedOpenOrderTime);
                } else {
                    groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
                    Log.e("OrderDetailsActivity", "First name or family name is null for user: " + userEmail);
                }
            } else {
                groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
                Log.e("OrderDetailsActivity", "No document found for user: " + userEmail);
            }
        }).addOnFailureListener(e -> {
            groupInfoTextView.setText("People: " + numberOfPeopleInOrder + "/" + maxPeople + "\nThe group was created by " + userEmail + "\non " + formattedOpenOrderTime);
            Log.e("OrderDetailsActivity", "Failed to fetch user details", e);
            Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
        });
    }
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

        long currentTime = System.currentTimeMillis();
        Date date = timestamp.toDate();
        long timeRemaining = date.getTime() - currentTime;

        if (timeRemaining > 0) {
            timerContainer.setVisibility(View.VISIBLE);
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
            timerContainer.setVisibility(View.GONE);
            daysTextView.setText("00");
            hoursTextView.setText("00");
            minutesTextView.setText("00");
            secondsTextView.setText("00");
        }
    }

    private void checkUserInList() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> listPeopleInOrder = (List<String>) documentSnapshot.get("listPeopleInOrder");
                if (listPeopleInOrder != null && listPeopleInOrder.contains(currentUser.getEmail())) {
                    inOrder = true;
                    joinIcon.setVisibility(View.GONE); // Hide the join icon
                    chatIcon.setVisibility(View.VISIBLE); // Show the chat icon
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to check user in list", Toast.LENGTH_SHORT).show());
    }

    private void addUserToOrder() {
        DocumentReference orderRef = db.collection("orders").document(orderId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(orderRef);
            List<String> listPeopleInOrder = (List<String>) snapshot.get("listPeopleInOrder");
            if (listPeopleInOrder != null && !listPeopleInOrder.contains(currentUser.getEmail())) {
                transaction.update(orderRef, "listPeopleInOrder", FieldValue.arrayUnion(currentUser.getEmail()));
                Long currentNumberOfPeople = snapshot.getLong("NumberOfPeopleInOrder");
                transaction.update(orderRef, "NumberOfPeopleInOrder", currentNumberOfPeople + 1);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Added to order", Toast.LENGTH_SHORT).show();
            joinIcon.setVisibility(View.GONE); // Hide the join icon
            chatIcon.setVisibility(View.VISIBLE); // Show the chat icon
            Intent chatIntent = new Intent(OrderDetailsActivity.this, ChatActivity.class);
            chatIntent.putExtra("orderId", orderId);
            startActivity(chatIntent);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to add user to order", Toast.LENGTH_SHORT).show());
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
            case R.id.chat_icon:
                menuUtils.allChats();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}


