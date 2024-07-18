package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
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
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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
    private Spinner categorySpinner;
    private EditText urlEditText;
    private EditText descriptionEditText;
    private EditText titleEditText;
    private EditText timeEditText;
    private Calendar selectedTime = null;
    private int max_people_in_order;
    private EditText maxPeopleEditText;
    private String saveNewCategorieName;
    private TextView searchAddressText;
    private String lastAddress;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_new_order);
        menuUtils = new MenuUtils(this);

        db = FirebaseFirestore.getInstance();
        categorySpinner = findViewById(R.id.category);
        urlEditText = findViewById(R.id.url);
        descriptionEditText = findViewById(R.id.description);
        titleEditText = findViewById(R.id.title);
        timeEditText = findViewById(R.id.time);
        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);

        createNotificationChannel();
        requestNotificationPermission();

        readCategoriesFromFireStore();

        maxPeopleEditText = findViewById(R.id.maxPeopleEditText);
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
            Intent intent = new Intent(OpenNewOrderActivity.this, LocationWindow.class);
            intent.putExtra("hideDistanceLayout", true); // העברת פרמטר להסתרת ה-KM
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intent.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intent);
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        String apiKey = "YOUR_API_KEY"; // החלף במפתח ה-API שלך

        // Initialize Places
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
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

    private void sendNotification(String title, String message, String orderId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            createDynamicLink(orderId, shortLink -> {
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
                    int notificationId = getUniqueNotificationId(); // שימוש בפונקציה ליצירת id ייחודי להודעה
                    notificationManager.notify(notificationId, builder.build());
                } else {
                    Toast.makeText(this, "Failed to create notification link", Toast.LENGTH_SHORT).show();
                    Log.d("Notification", "Failed to create notification link");
                }
            });
        } else {
            Toast.makeText(this, "Notification permission is required to send notifications", Toast.LENGTH_SHORT).show();
            Log.d("Notification Permission", "Notification permission is not granted");
        }
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



    public void goToMyOrders(View v) {
        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();
        String time = timeEditText.getText().toString().trim();
        int maxPeople = maxPeople();

        if (url.isEmpty()) {
            url = "";
        } else if (!url.contains("://")) {
            Toast.makeText(this, "Invalid URL. Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (saveNewCategorieName.isEmpty() || saveNewCategorieName.equals("Choose Categorie")) {
            Toast.makeText(this, "Category is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lastLatitude == 0.0 && lastLongitude == 0.0) {
            Toast.makeText(this, "Location is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty()) {
            Toast.makeText(this, "Time is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (maxPeople == 0) {
            Toast.makeText(this, "Maximum people is required", Toast.LENGTH_SHORT).show();
            return;
        }

        addCategorieToDataBase();

        saveOrder(url, description, title, maxPeople).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String orderId = task.getResult();
                checkAndNotifyUsers(saveNewCategorieName, lastLatitude, lastLongitude)
                        .addOnCompleteListener(notificationTask -> {
                            if (notificationTask.isSuccessful()) {
                                sendNotification("New Order", "There is a new order in the field that interests you!", orderId);
                                Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Error in notification process", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
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
            return 0; // Return 0 if the entered text is not a valid number
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


    private Task<Void> checkAndNotifyUsers(String category, double latitude, double longitude) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersRef = db.collection("users");
        GeoPoint orderLocation = new GeoPoint(latitude, longitude);

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot userDocument : task.getResult()) {
                    List<String> favoriteCategories = (List<String>) userDocument.get("favorite categories");
                    Log.d("OpenNewOrderActivity", "User favorite categories: " + favoriteCategories);

                    if (userDocument.get("address") instanceof GeoPoint) {
                        GeoPoint userLocation = userDocument.getGeoPoint("address");
                        Log.d("OpenNewOrderActivity", "User location: " + userLocation);
                        double maxDistance = 10.0;

                        if (favoriteCategories.contains(category) && isWithinDistance(orderLocation, userLocation, maxDistance)) {
                            String userEmail = userDocument.getString("email");
                            String notificationMessage = "There is a new order in the field that interests you!";
                            Log.d("OpenNewOrderActivity", "Sending notification to: " + userEmail);

                            CollectionReference notificationsRef = usersRef.document(userDocument.getId()).collection("notifications");
                            notificationsRef.get().addOnCompleteListener(notificationTask -> {
                                if (notificationTask.isSuccessful() && notificationTask.getResult().isEmpty()) {
                                    // אוסף התראות לא קיים, צור אותו ולאחר מכן הוסף את ההתראה
                                    Map<String, Object> notificationData = new HashMap<>();
                                    notificationData.put("message", notificationMessage);
                                    notificationData.put("userEmail", userEmail);
                                    notificationData.put("timestamp", FieldValue.serverTimestamp());

                                    notificationsRef.add(notificationData)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("Firestore", "Notification added successfully for user: " + userEmail);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w("Firestore", "Error adding notification for user: " + userEmail, e);
                                            });
                                } else {
                                    // אוסף התראות קיים, הוסף את ההתראה
                                    Map<String, Object> notificationData = new HashMap<>();
                                    notificationData.put("message", notificationMessage);
                                    notificationData.put("userEmail", userEmail);
                                    notificationData.put("timestamp", FieldValue.serverTimestamp());

                                    notificationsRef.add(notificationData)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("Firestore", "Notification added successfully for user: " + userEmail);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w("Firestore", "Error adding notification for user: " + userEmail, e);
                                            });
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
    private void sendPushNotification(String fcmToken, String message) {
        String serverKey = "YOUR_SERVER_KEY"; // החלף במפתח השרת שלך מ-Firebase Console
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();

        try {
            notificationBody.put("title", "New Order");
            notificationBody.put("message", message);

            notification.put("to", fcmToken);
            notification.put("data", notificationBody);

        } catch (JSONException e) {
            Log.e("FCM", "Error creating notification JSON", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, "https://fcm.googleapis.com/fcm/send", notification,
                response -> Log.d("FCM", "Notification sent successfully"),
                error -> Log.e("FCM", "Error sending notification", error)) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "key=" + serverKey);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
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

    private boolean validateFields() {
        Log.d("Validate Fields", "Start");
        String url = urlEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String category = saveNewCategorieName;
        String address = searchAddressText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();
        boolean valid = true;

        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            valid = false;
        }

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            valid = false;
        }

        if (category.isEmpty() || category.equals("Other") || category.equals("Choose Categorie")) {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (address.isEmpty()) {
            searchAddressText.setError("Address is required");
            valid = false;
        }

        Log.d("Validate Fields", "End with valid = " + valid);
        return valid;
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

    public void showTimePickerDialog(View view) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(OpenNewOrderActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar currentDate = Calendar.getInstance();
                currentDate.set(Calendar.YEAR, year);
                currentDate.set(Calendar.MONTH, month);
                currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (currentDate.before(Calendar.getInstance())) {
                    Toast.makeText(OpenNewOrderActivity.this, "You can't choose a past date", Toast.LENGTH_SHORT).show();
                } else {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(OpenNewOrderActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedDate.set(Calendar.MINUTE, minute);

                            if (selectedDate.before(Calendar.getInstance())) {
                                Toast.makeText(OpenNewOrderActivity.this, "You cannot select a past time", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedTime = selectedDate;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String dateTime = sdf.format(selectedDate.getTime());

                                timeEditText.setText(dateTime);
                            }
                        }
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
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
