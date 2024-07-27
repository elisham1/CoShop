package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenNewOrderActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private boolean isSubmitting = false;

    private Spinner categorySpinner;
    private EditText urlEditText;
    private EditText descriptionEditText;
    private EditText titleEditText;
    private TextView searchAddressText;
    private String lastAddress;
    private String getUserEmail;
    private double lastLatitude;
    private double lastLongitude;
    private ImageButton searchAddressButton;
    private ImageButton editAddressButton;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private ArrayAdapter<String> addressAdapter;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MenuUtils menuUtils;
    private boolean isDatePickerDialogOpen = false;
    private boolean isTimePickerDialogOpen = false;

    private Calendar selectedDate;
    private Calendar selectedTime;
    private EditText maxPeopleEditText;
    private int max_people_in_order;
    private String saveNewCategorieName;
    private String globalUserType;

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
        setContentView(R.layout.activity_open_new_order);
        menuUtils = new MenuUtils(this,globalUserType);

        db = FirebaseFirestore.getInstance();
        categorySpinner = findViewById(R.id.category);
        urlEditText = findViewById(R.id.url);
        descriptionEditText = findViewById(R.id.description);
        titleEditText = findViewById(R.id.title);
        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);
        maxPeopleEditText = findViewById(R.id.maxPeopleEditText);

        createNotificationChannel();
        requestNotificationPermission();

        readCategoriesFromFireStore();
        ImageButton tapIcon = findViewById(R.id.tap_icon);
        tapIcon.setOnClickListener(v -> showImageAlertDialog());
        maxPeopleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int numberOfPeople = maxPeople();
                max_people_in_order = numberOfPeople;
                Log.d("Max People", "Updated max_people_in_order: " + max_people_in_order);
            }
        });

        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 25) {
                    s.delete(25, s.length());
                    Toast.makeText(OpenNewOrderActivity.this, "Title cannot exceed 25 characters", Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationWindowLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        lastAddress = result.getData().getStringExtra("address");
                        lastLatitude = result.getData().getDoubleExtra("latitude", 0);
                        lastLongitude = result.getData().getDoubleExtra("longitude", 0);

                        if (lastAddress != null) {
                            searchAddressText.setText(lastAddress);
                            searchAddressButton.setVisibility(View.VISIBLE);
                            searchAddressButton.setTag("clear");
                            searchAddressButton.setImageResource(R.drawable.clear);
                            editAddressButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        searchAddressButton.setOnClickListener(v -> {
            if (searchAddressButton.getTag() != null && searchAddressButton.getTag().equals("clear")) {
                searchAddressText.setText("");
                searchAddressButton.setTag("search");
                searchAddressButton.setImageResource(R.drawable.baseline_search_24);
                editAddressButton.setVisibility(View.GONE);

                lastAddress = null;
                lastLatitude = 0;
                lastLongitude = 0;
            }
        });

        searchAddressText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toggleSearchClearIcon();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout searchRow = findViewById(R.id.search_row);
        searchRow.setOnClickListener(v -> {
            Intent intentLocation = new Intent(OpenNewOrderActivity.this, LocationWindow.class);
            intentLocation.putExtra("userType", globalUserType);
            intentLocation.putExtra("hideDistanceLayout", true); // העברת פרמטר להסתרת ה-KM
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intentLocation.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intentLocation);
        });

        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString().trim();
                if (url.isEmpty() || isValidUrl(url)) {
                    urlEditText.setError(null); // אין שגיאה
                    findViewById(R.id.submit_button).setEnabled(true); // לאפשר לחיצה על כפתור השליחה
                } else {
                    urlEditText.setError("Invalid URL"); // הצגת שגיאה
                    findViewById(R.id.submit_button).setEnabled(false); // לא לאפשר לחיצה על כפתור השליחה
                }
            }
        });

        ImageButton submit = findViewById(R.id.submit_button);
        if (globalUserType.equals("Supplier")) {
            submit.setImageResource(R.drawable.ic_plus_supplier);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        String apiKey = "YOUR_API_KEY"; // החלף במפתח ה-API שלך

        // Initialize Places
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
    }
    private boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches() && url.contains("://");
    }

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
    private void requestNotificationPermission() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d("Permission", "Notification permission granted");
            } else {
                Toast.makeText(this, "Notification permission is required for this app", Toast.LENGTH_SHORT).show();
            }
        });

        // Check and request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Example Channel";
            String description = "This is an example channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("example_channel_id", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private int getUniqueNotificationId() {
        return (int) System.currentTimeMillis();
    }

    private Task<Void> sendNotification(String title, String message, String orderId) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        // שמור את האימייל של המשתמש הנוכחי
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserEmail = (currentUser != null) ? currentUser.getEmail() : "";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            createDynamicLink(orderId, globalUserType, shortLink -> {
                if (shortLink != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(shortLink));
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "example_channel_id")
                            .setSmallIcon(R.drawable.coshop2)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                    // שליחת ההתראה רק אם המכשיר הנוכחי אינו המשתמש הנוכחי שיצר את ההזמנה
                    db.collection("orders").document(orderId).get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String orderUserEmail = documentSnapshot.getString("user_email");
                            if (!orderUserEmail.equals(currentUserEmail)) {
                                int notificationId = getUniqueNotificationId();
                                notificationManager.notify(notificationId, builder.build());
                            }
                        }
                        taskCompletionSource.setResult(null);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to get order details", Toast.LENGTH_SHORT).show();
                        taskCompletionSource.setException(e);
                    });

                    // שמירת הקישור במסמך ההודעה בפיירבייס
                    saveNotificationToFirestore(orderId, shortLink, title, message);
                } else {
                    Toast.makeText(this, "Failed to create notification link", Toast.LENGTH_SHORT).show();
                    Log.d("Notification", "Failed to create notification link");
                    taskCompletionSource.setException(new Exception("Failed to create notification link"));
                }
            });
        } else {
            Toast.makeText(this, "Notification permission is required to send notifications", Toast.LENGTH_SHORT).show();
            Log.d("Notification Permission", "Notification permission is not granted");
            taskCompletionSource.setException(new Exception("Notification permission is not granted"));
        }

        return taskCompletionSource.getTask();
    }

    private void saveNotificationToFirestore(String orderId, String link, String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("link", link);
        notificationData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("orders").document(orderId).collection("notifications")
                .add(notificationData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Notification saved successfully"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error saving notification", e));
    }

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

    interface DynamicLinkCallback {
        void onLinkCreated(String shortLink);
    }

    public void goToMyOrders(View v) {
        if (isSubmitting) {
            // אם הכפתור כבר נלחץ וההזמנה בעיצומה, לא נעשה כלום
            return;
        }

        // ננעל את הכפתור ונציג הודעת טעינה
        isSubmitting = true;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();
        int maxPeople = maxPeople();

        if (url.isEmpty()) {
            url = "";
        } else if (!url.contains("://")) {
            Toast.makeText(this, "Invalid URL. Please enter a valid URL", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (saveNewCategorieName.isEmpty() || saveNewCategorieName.equals("Choose Categorie")) {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (lastLatitude == 0.0 && lastLongitude == 0.0) {
            Toast.makeText(this, "Location is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (selectedDate == null) {
            Toast.makeText(this, "Date is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (selectedTime == null) {
            Toast.makeText(this, "Time is required", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }

        if (maxPeople == 0) {
            Toast.makeText(this, "Maximum people is required it's can't be zero", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }
        if (maxPeople == 1) {
            Toast.makeText(this, "Choose minimum two participants", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }
        if (maxPeople < 0) {
            Toast.makeText(this, "You cannot choose a negative number", Toast.LENGTH_SHORT).show();
            isSubmitting = false; // שחרור הכפתור אם יש שגיאה
            progressDialog.dismiss();
            return;
        }
        addCategorieToDataBase();

        saveOrder(url, description, title, maxPeople).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String orderId = task.getResult();
                checkAndNotifyUsers(saveNewCategorieName, lastLatitude, lastLongitude, orderId)
                        .addOnCompleteListener(notificationTask -> {
                            if (notificationTask.isSuccessful()) {
                                sendNotification("New Order", "There is a new order in the field that interests you!", orderId).addOnCompleteListener(notificationSendTask -> {
                                    if (notificationSendTask.isSuccessful()) {
                                        progressDialog.dismiss();
                                        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
                                        intent.putExtra("userType", globalUserType);
                                        startActivity(intent);
                                        finish(); // סיום הפעילות הנוכחית כדי למנוע חזרה
                                    } else {
                                        Toast.makeText(this, "Error in notification process", Toast.LENGTH_SHORT).show();
                                        isSubmitting = false; // שחרור הכפתור אם יש שגיאה
                                        progressDialog.dismiss();
                                    }
                                });
                            } else {
                                Toast.makeText(this, "Error in notification process", Toast.LENGTH_SHORT).show();
                                isSubmitting = false; // שחרור הכפתור אם יש שגיאה
                                progressDialog.dismiss();
                            }
                        });
            } else {
                Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
                isSubmitting = false; // שחרור הכפתור אם יש שגיאה
                progressDialog.dismiss();
            }
        });
    }

    private void toggleSearchClearIcon() {
        String address = searchAddressText.getText().toString();
        if (!address.isEmpty()) {
            searchAddressButton.setTag("clear");
            searchAddressButton.setImageResource(R.drawable.clear);
            editAddressButton.setVisibility(View.VISIBLE);
        } else {
            searchAddressButton.setTag("search");
            searchAddressButton.setImageResource(R.drawable.baseline_search_24);
            editAddressButton.setVisibility(View.GONE);
        }
    }

    public int maxPeople() {
        String maxPeopleStr = maxPeopleEditText.getText().toString();
        if (maxPeopleStr.isEmpty()) {
            return 0; // Return 0 if no limit or no number is entered
        }
        try {
            return Integer.parseInt(maxPeopleStr);
        } catch (NumberFormatException e) {
            return 0; // Return 0 אם הטקסט שהוזן אינו מספר חוקי
        }
    }

    public void readCategoriesFromFireStore() {
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> categoriesList = (List<String>) document.get("categories");
                            if (categoriesList != null) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                categorySpinner.setAdapter(adapter);

                                categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                                        saveNewCategorieName = (String) adapterView.getItemAtPosition(position);
                                        Log.d("Selected Category", saveNewCategorieName);
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> adapterView) {
                                    }
                                });
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    private boolean containsIgnoreCase(List<String> list, String str) {
        for (String s : list) {
            if (s.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private Task<Void> checkAndNotifyUsers(String category, double latitude, double longitude, String orderId) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersRef = db.collection("users");
        GeoPoint orderLocation = new GeoPoint(latitude, longitude);

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot userDocument : task.getResult()) {
                    List<String> favoriteCategories = (List<String>) userDocument.get("favorite categories");
                    Log.d("OpenNewOrderActivity", "User favorite categories: " + favoriteCategories);
                    Log.d("OpenNewOrderActivity", "Selected category: " + category);

                    if (userDocument.get("address") instanceof GeoPoint) {
                        GeoPoint userLocation = userDocument.getGeoPoint("address");
                        Log.d("OpenNewOrderActivity", "User location: " + userLocation);
                        double maxDistance = 10.0;

                        if (favoriteCategories.contains(category) && isWithinDistance(orderLocation, userLocation, maxDistance)) {
                            String userEmail = userDocument.getString("email");
                            String userType = userDocument.getString("type of user");

                            // בדיקה אם המשתמש הוא מסוג "Supplier"
                            if ("Supplier".equalsIgnoreCase(userType)) {
                                continue;
                            }

                            if (userEmail.equals(getUserEmail)) {
                                // אם זה המשתמש שיצר את ההזמנה, לא שולחים לו התראה
                                continue;
                            }
                            String notificationMessage = "There is a new order in the field that interests you!";
                            Log.d("OpenNewOrderActivity", "Sending notification to: " + userEmail);

                            createDynamicLink(orderId, globalUserType, shortLink -> {
                                if (shortLink != null) {
                                    Map<String, Object> notificationData = new HashMap<>();
                                    notificationData.put("message", notificationMessage);
                                    notificationData.put("userEmail", userEmail);
                                    notificationData.put("timestamp", FieldValue.serverTimestamp());
                                    notificationData.put("link", shortLink);

                                    CollectionReference notificationsRef = usersRef.document(userDocument.getId()).collection("notifications");
                                    notificationsRef.add(notificationData)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("Firestore", "Notification added successfully for user: " + userEmail);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w("Firestore", "Error adding notification for user: " + userEmail, e);
                                            });
                                } else {
                                    Log.d("Notification", "Failed to create notification link");
                                }
                            });
                        }
                    } else {
                        Log.w("Firestore", "User document does not contain a GeoPoint address");
                    }
                }
                taskCompletionSource.setResult(null);
            } else {
                Log.w("Firestore", "Error getting documents.", task.getException());
                taskCompletionSource.setException(task.getException());
            }
        });

        return taskCompletionSource.getTask();
    }


    private boolean isWithinDistance(GeoPoint point1, GeoPoint point2, double maxDistance) {
        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        double distance = haversine(lat1, lon1, lat2, lon2);
        Log.d("OpenNewOrderActivity", "Calculated distance: " + distance);
        return distance <= maxDistance;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void addCategorieToDataBase() {
        Log.d("Add Category", "Start");
        String lowercaseCategory = saveNewCategorieName.toLowerCase();

        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> categoriesList = (List<String>) documentSnapshot.get("categories");
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                                    .update("categories", FieldValue.arrayUnion(saveNewCategorieName))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(OpenNewOrderActivity.this, "New category added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error adding category: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
                });
    }

    private Task<String> saveOrder(String url, String description, String title, int maxPeople) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return Tasks.forException(new Exception("User not logged in"));
        }

        getUserEmail=userEmail;
        String finalUserEmail = userEmail;
        String finalUrl = url;
        String finalDescription = description;
        String finalTitle = title;

        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        // תחילה מקבלים את ה-type_of_order מפיירבייס
        db.collection("users").document(finalUserEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("type of user");

                            Map<String, Object> order = new HashMap<>();
                            order.put("URL", finalUrl);
                            order.put("description", finalDescription);
                            order.put("categorie", saveNewCategorieName);
                            order.put("user_email", finalUserEmail);
                            order.put("max_people", max_people_in_order);
                            order.put("type_of_order", userType); // שימוש ב-type_of_order מהפיירבייס
                            order.put("titleOfOrder", finalTitle);
                            order.put("time", new Timestamp(selectedTime.getTime()));
                            order.put("openOrderTime", new Timestamp(new Date()));
                            order.put("NumberOfPeopleInOrder", 1);

                            GeoPoint geoPoint = new GeoPoint(lastLatitude, lastLongitude);
                            order.put("location", geoPoint);

                            ArrayList<String> listPeopleInOrder = new ArrayList<>();
                            listPeopleInOrder.add(finalUserEmail);
                            order.put("listPeopleInOrder", listPeopleInOrder);

                            db.collection("orders")
                                    .add(order)
                                    .addOnSuccessListener(documentReference -> {
                                        taskCompletionSource.setResult(documentReference.getId());
                                        Toast.makeText(OpenNewOrderActivity.this, "Order added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        taskCompletionSource.setException(e);
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.d("Firestore", "No such document");
                            taskCompletionSource.setException(new Exception("No such document"));
                        }
                    } else {
                        Log.d("Firestore", "get failed with ", task.getException());
                        taskCompletionSource.setException(task.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public void showDatePickerDialog(View view) {
        if (isDatePickerDialogOpen) return; // בדיקה אם הדיאלוג פתוח כבר
        isDatePickerDialogOpen = true; // עדכון למצב פתוח

        Locale.setDefault(Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                OpenNewOrderActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth, // ערכת נושא שתבטיח שהדיאלוג יהיה רחב מספיק
                (view1, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    // אם כבר נבחרה שעה, מוודאים שהשעה לא בעבר
                    if (this.selectedTime != null) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, this.selectedTime.get(Calendar.HOUR_OF_DAY));
                        selectedDate.set(Calendar.MINUTE, this.selectedTime.get(Calendar.MINUTE));
                    }

                    if (selectedDate.before(Calendar.getInstance())) {
                        Toast.makeText(OpenNewOrderActivity.this, "You can't choose a past date", Toast.LENGTH_SHORT).show();
                    } else {
                        this.selectedDate = selectedDate;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String dateString = sdf.format(selectedDate.getTime());

                        TextView dateText = findViewById(R.id.date_text);
                        dateText.setText(dateString);

                        ImageButton dateIcon = findViewById(R.id.date_icon);
                        dateIcon.setImageResource(R.drawable.baseline_calendar_month_24);
                    }
                    isDatePickerDialogOpen = false; // עדכון למצב סגור לאחר סגירת הדיאלוג
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        );
        // Make the dialog non-cancelable
        datePickerDialog.setCancelable(false);
        datePickerDialog.setOnDismissListener(dialog -> isDatePickerDialogOpen = false); // עדכון למצב סגור אם הדיאלוג נסגר
        datePickerDialog.show();
    }

    public void showTimePickerDialog(View view) {
        if (isTimePickerDialogOpen) return; // בדיקה אם הדיאלוג פתוח כבר
        isTimePickerDialogOpen = true; // עדכון למצב פתוח

        Locale.setDefault(Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                OpenNewOrderActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth, // ערכת נושא שתבטיח שהדיאלוג יהיה רחב מספיק
                (view1, hourOfDay, minute) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);

                    // אם כבר נבחר תאריך, מוודאים שהתאריך והשעה לא בעבר
                    if (this.selectedDate != null) {
                        selectedTime.set(Calendar.YEAR, this.selectedDate.get(Calendar.YEAR));
                        selectedTime.set(Calendar.MONTH, this.selectedDate.get(Calendar.MONTH));
                        selectedTime.set(Calendar.DAY_OF_MONTH, this.selectedDate.get(Calendar.DAY_OF_MONTH));
                    }

                    if (selectedTime.before(Calendar.getInstance())) {
                        Toast.makeText(OpenNewOrderActivity.this, "You cannot select a past time", Toast.LENGTH_SHORT).show();
                    } else {
                        this.selectedTime = selectedTime;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        String timeString = sdf.format(selectedTime.getTime());

                        TextView timeText = findViewById(R.id.time_text);
                        timeText.setText(timeString);

                        ImageButton timeIcon = findViewById(R.id.time_icon);
                        timeIcon.setImageResource(R.drawable.ic_baseline_access_time_24);
                    }
                    isTimePickerDialogOpen = false; // עדכון למצב סגור לאחר סגירת הדיאלוג
                },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        );
        timePickerDialog.setCancelable(false);
        timePickerDialog.setOnDismissListener(dialog -> isTimePickerDialogOpen = false); // עדכון למצב סגור אם הדיאלוג נסגר
        timePickerDialog.show();
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
