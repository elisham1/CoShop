package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.slider.RangeSlider;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FilterActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private TextView searchAddressText;
    private ListView categoryListView;
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private EditText timeEditText;
    private Calendar selectedTime;
    private RangeSlider rangeSlider;
    private EditText unlimitEditText;
    private CheckBox checkBoxLimit;
    private CheckBox checkBoxUnlimited;
    private String lastAddress;
    private double lastLatitude;
    private double lastLongitude;
    private int lastDistance;
    private ImageButton searchAddressButton;
    private ImageButton editAddressButton;
    private EditText editTextURL;
    private ImageButton clearURLButton;
    private String lastURL;
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        menuUtils = new MenuUtils(this);

        timeEditText = findViewById(R.id.time);
        timeEditText.setOnClickListener(this::showTimePickerDialog);
        ImageButton clearTimeButton = findViewById(R.id.clear_time_button);

        LinearLayout timeRow = findViewById(R.id.time_row);
        timeRow.setOnClickListener(this::showTimePickerDialog);

        clearTimeButton.setOnClickListener(v -> {
            timeEditText.setText("");
            clearTimeButton.setVisibility(View.GONE);
        });

        editTextURL = findViewById(R.id.editTextText2);
        clearURLButton = findViewById(R.id.clear_url_button);
        clearURLButton.setOnClickListener(v -> editTextURL.setText(""));

        editTextURL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearURLButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                lastURL = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout searchRow = findViewById(R.id.search_row);
        searchRow.setOnClickListener(v -> {
            Intent intent = new Intent(FilterActivity.this, LocationWindow.class);
            if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                intent.putExtra("address", lastAddress);
                intent.putExtra("distance", lastDistance);
            }
            locationWindowLauncher.launch(intent);
        });

        locationWindowLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        lastAddress = result.getData().getStringExtra("address");
                        lastDistance = result.getData().getIntExtra("distance", 0);
                        lastLatitude = result.getData().getDoubleExtra("latitude", 0);
                        lastLongitude = result.getData().getDoubleExtra("longitude", 0);

                        if (lastAddress != null) {
                            String displayText = String.format(Locale.getDefault(), "%s, %d KM", lastAddress, lastDistance);
                            searchAddressText.setText(displayText);
                            searchAddressButton.setVisibility(View.VISIBLE);
                            searchAddressButton.setTag("clear");
                            searchAddressButton.setImageResource(R.drawable.clear);
                            editAddressButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        ImageButton closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());

        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);

        searchAddressButton.setOnClickListener(v -> {
            if (searchAddressButton.getTag() != null && searchAddressButton.getTag().equals("clear")) {
                searchAddressText.setText("");
                searchAddressButton.setTag("search");
                searchAddressButton.setImageResource(R.drawable.baseline_search_24);
                editAddressButton.setVisibility(View.GONE);

                // איפוס הערכים האחרונים
                lastAddress = null;
                lastDistance = 0;
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

        db = FirebaseFirestore.getInstance();
        categoryListView = findViewById(R.id.category_list);

        rangeSlider = findViewById(R.id.rangeSliderPeople);
        unlimitEditText = findViewById(R.id.unlimit_value);

        checkBoxLimit = findViewById(R.id.checkBoxLimit);
        checkBoxUnlimited = findViewById(R.id.checkBoxUnlimited);

        checkBoxLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxUnlimited.setChecked(false);
                rangeSlider.setEnabled(true);
                unlimitEditText.setEnabled(true);
            } else {
                rangeSlider.setEnabled(false);
                unlimitEditText.setEnabled(false);
            }
        });

        checkBoxUnlimited.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxLimit.setChecked(false);
                rangeSlider.setEnabled(false);
                unlimitEditText.setEnabled(false);
            }
        });

        rangeSlider.setValueFrom(2);
        rangeSlider.setValueTo(1000);

        rangeSlider.setLabelFormatter(value -> String.valueOf((int) value));

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                unlimitEditText.setText(String.valueOf((int) value));
            }
        });

        readCategoriesFromFireStore();

        locationWindowLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        lastAddress = result.getData().getStringExtra("address");
                        lastDistance = result.getData().getIntExtra("distance", 0);
                        lastLatitude = result.getData().getDoubleExtra("latitude", 0);
                        lastLongitude = result.getData().getDoubleExtra("longitude", 0);

                        if (lastAddress != null) {
                            String displayText = String.format(Locale.getDefault(), "%s, %d KM", lastAddress, lastDistance);
                            searchAddressText.setText(displayText);
                            searchAddressButton.setVisibility(View.VISIBLE);
                            searchAddressButton.setTag("clear");
                            searchAddressButton.setImageResource(R.drawable.clear);
                            editAddressButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        editAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(FilterActivity.this, LocationWindow.class);
            if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                intent.putExtra("address", lastAddress);
                intent.putExtra("distance", lastDistance);
            }
            locationWindowLauncher.launch(intent);
        });
    }

    private void toggleSearchClearIcon() {
        String address = searchAddressText.getText().toString();
        if (!address.isEmpty() && lastDistance > 0) {
            searchAddressButton.setTag("clear");
            searchAddressButton.setImageResource(R.drawable.clear);
            editAddressButton.setVisibility(View.VISIBLE);
        } else {
            searchAddressButton.setTag("search");
            searchAddressButton.setImageResource(R.drawable.baseline_search_24);
            editAddressButton.setVisibility(View.GONE);
        }
    }

    private void readCategoriesFromFireStore() {
        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> categoriesList = (List<String>) document.get("categories");
                            if (categoriesList != null && categoriesList.size() > 1) {
                                List<String> subList = categoriesList.subList(1, categoriesList.size());
                                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, subList);
                                categoryListView.setAdapter(adapter);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void OrderFiltering(View v) {
        String address = searchAddressText.getText().toString();
        String urlOrString = editTextURL.getText().toString();
        List<String> selectedCategories = getSelectedCategories();

        boolean filterByLocation = !address.isEmpty() || lastAddress != null;
        boolean filterByURLOrString = !urlOrString.isEmpty();
        boolean filterByCategory = selectedCategories != null && !selectedCategories.isEmpty();
        boolean filterByConsumer = ((CheckBox) findViewById(R.id.checkBoxConsumer)).isChecked();
        boolean filterBySupplied = ((CheckBox) findViewById(R.id.checkBoxSupplied)).isChecked();
        boolean filterByPeopleLimit = checkBoxLimit.isChecked();
        boolean filterByUnlimitedPeople = checkBoxUnlimited.isChecked();
        boolean filterByTime = selectedTime != null;
        int peopleLimit = 0;

        if (filterByPeopleLimit) {
            String peopleLimitStr = unlimitEditText.getText().toString().trim();
            if (!peopleLimitStr.isEmpty()) {
                peopleLimit = Integer.parseInt(peopleLimitStr);
            } else {
                filterByPeopleLimit = false;
            }
        }

        if (!filterByLocation && !filterByURLOrString && !filterByCategory && !filterByConsumer && !filterBySupplied && !filterByPeopleLimit && !filterByUnlimitedPeople && !filterByTime) {
            Toast.makeText(this, "Select minimum in one filter", Toast.LENGTH_SHORT).show();
            return;
        }

        if (filterByURLOrString) {
            if (isValidURL(urlOrString)) {
                fetchOrdersByUrl(urlOrString, address, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedTime);
            } else if (isValidString(urlOrString)) {
                fetchOrdersByString(urlOrString, address, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedTime);
            } else {
                Toast.makeText(this, "Invalid URL or String", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (filterByLocation) {
                if (address.isEmpty()) {
                    address = lastAddress;
                }

                double userLat = lastLatitude;
                double userLon = lastLongitude;

                fetchOrders(userLat, userLon, lastDistance, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedTime);
            } else {
                fetchOrders(0, 0, 0, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedTime);
            }
        }
    }

    public void resetFilters(View v) {
        // Reset address field
        searchAddressText.setText(""); // Reset the TextView to its initial state

        // Reset URL field
        editTextURL.setText(""); // Reset the EditText to its initial state

        // Reset CheckBoxes
        CheckBox checkBoxSupplied = findViewById(R.id.checkBoxSupplied);
        checkBoxSupplied.setChecked(false);

        CheckBox checkBoxConsumer = findViewById(R.id.checkBoxConsumer);
        checkBoxConsumer.setChecked(false);

        CheckBox checkBoxLimit = findViewById(R.id.checkBoxLimit);
        checkBoxLimit.setChecked(false);

        CheckBox checkBoxUnlimited = findViewById(R.id.checkBoxUnlimited);
        checkBoxUnlimited.setChecked(false);

        // Reset RangeSlider and EditText for people limit
        RangeSlider rangeSlider = findViewById(R.id.rangeSliderPeople);
        rangeSlider.setValues(2f);

        EditText unlimitEditText = findViewById(R.id.unlimit_value);
        unlimitEditText.setText("1000");
        rangeSlider.setEnabled(false);
        unlimitEditText.setEnabled(false);

        // Reset category ListView
        ListView categoryListView = findViewById(R.id.category_list);
        for (int i = 0; i < categoryListView.getCount(); i++) {
            categoryListView.setItemChecked(i, false);
        }

        // Reset the saved address and distance values
        lastAddress = null;
        lastLatitude = 0;
        lastLongitude = 0;
        lastDistance = 0;

        // Reset the time and date
        timeEditText.setText("");
        findViewById(R.id.clear_time_button).setVisibility(View.GONE);
    }

    private boolean isValidURL(String url) {
        Pattern pattern = Patterns.WEB_URL;
        return pattern.matcher(url).matches();
    }

    private boolean isValidString(String str) {
        return str.matches("[a-zA-Z ]+");
    }
    private String getDomainName(String url) {
        try {
            java.net.URL netUrl = new java.net.URL(url);
            return netUrl.getHost();
        } catch (Exception e) {
            return null;
        }
    }


    private void fetchOrders(double userLat, double userLon, int distance, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedTime) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();
                String userEmail = null;
                if (currentUser != null) {
                    userEmail = currentUser.getEmail();
                }

                StringBuilder results = new StringBuilder();
                long currentTimeMillis = System.currentTimeMillis();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    long timeRemaining = timestamp.toDate().getTime() - currentTimeMillis;

                    if (timeRemaining <= 0) continue; // דילוג על הזמנות שזמן ההזמנה שלהן נגמר

                    String orderOwner = documentSnapshot.getString("ownerEmail");
                    List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                    if (orderOwner != null && orderOwner.equals(userEmail)) continue; // דילוג על הזמנות שפתחתי
                    if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // דילוג על הזמנות שהצטרפתי אליהן

                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // דילוג על הזמנות מלאות

                    boolean matchesCategory = true;
                    if (filterByCategory) {
                        String categorie = documentSnapshot.getString("categorie");
                        matchesCategory = selectedCategories.contains(categorie);
                    }

                    boolean matchesLocation = true;
                    double distanceInKm = 0;
                    if (distance > 0) {
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        if (geoPoint != null) {
                            double orderLat = geoPoint.getLatitude();
                            double orderLon = geoPoint.getLongitude();

                            float[] resultsArray = new float[1];
                            android.location.Location.distanceBetween(userLat, userLon, orderLat, orderLon, resultsArray);
                            float distanceInMeters = resultsArray[0];
                            distanceInKm = distanceInMeters / 1000;

                            matchesLocation = distanceInKm <= distance;
                        } else {
                            matchesLocation = false;
                        }
                    }

                    boolean matchesTypeOfOrder = true;
                    if (filterByConsumer || filterBySupplied) {
                        String typeOfOrder = documentSnapshot.getString("type_of_order");

                        matchesTypeOfOrder = (filterByConsumer && "Consumer".equals(typeOfOrder)) ||
                                (filterBySupplied && "Supplier".equals(typeOfOrder));
                    }

                    boolean matchesPeopleLimit = true;
                    if (filterByPeopleLimit || filterByUnlimitedPeople) {
                        Long maxPeopleDoc = documentSnapshot.getLong("max_people");
                        if (filterByUnlimitedPeople) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc == 0;
                        } else if (filterByPeopleLimit) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc >= 2 && maxPeopleDoc <= peopleLimit && maxPeopleDoc != 0;
                        }
                    }

                    boolean matchesTime = true;
                    if (filterByTime) {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // בדיקה האם הזמן עבר
                    }

                    if (matchesCategory && matchesLocation && matchesTypeOfOrder && matchesPeopleLimit && matchesTime) {
                        results.append(documentSnapshot.getId()).append(";")
                                .append(documentSnapshot.getString("titleOfOrder")).append(";")
                                .append(documentSnapshot.getGeoPoint("location").getLatitude()).append(",").append(documentSnapshot.getGeoPoint("location").getLongitude()).append(";")
                                .append(documentSnapshot.getLong("NumberOfPeopleInOrder")).append(";")
                                .append(documentSnapshot.getLong("max_people")).append(";")
                                .append(documentSnapshot.getString("categorie")).append(";")
                                .append(distanceInKm).append(";")
                                .append(documentSnapshot.getTimestamp("time").getSeconds()).append("\n");
                    }
                }

                Intent intent = new Intent(FilterActivity.this, HomePageActivity.class);
                intent.putExtra("filterActive", true); // תמיד מציינים שסינון פעיל
                if (results.length() == 0) {
                    intent.putExtra("noOrdersFound", true);
                } else {
                    intent.putExtra("filteredOrders", results.toString());
                }
                startActivity(intent);
            }
        });
    }


    private void fetchOrdersByUrl(String url, String address, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedTime) {
        String domainName = getDomainName(url);
        if (domainName == null) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference ordersRef = db.collection("orders");
        ordersRef.whereEqualTo("URL", domainName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                StringBuilder results = new StringBuilder();
                long currentTimeMillis = System.currentTimeMillis(); // זמן נוכחי במילישניות
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // דילוג על הזמנות מלאות

                    boolean matchesCategory = true;
                    if (filterByCategory) {
                        String categorie = documentSnapshot.getString("categorie");
                        matchesCategory = selectedCategories.contains(categorie);
                    }

                    boolean matchesLocation = true;
                    double distanceInKm = 0;
                    if (!address.isEmpty()) {
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        if (geoPoint != null) {
                            double orderLat = geoPoint.getLatitude();
                            double orderLon = geoPoint.getLongitude();

                            float[] resultsArray = new float[1];
                            android.location.Location.distanceBetween(lastLatitude, lastLongitude, orderLat, orderLon, resultsArray);
                            float distanceInMeters = resultsArray[0];
                            distanceInKm = distanceInMeters / 1000;

                            matchesLocation = distanceInKm <= lastDistance;
                        } else {
                            matchesLocation = false;
                        }
                    }

                    boolean matchesTypeOfOrder = true;
                    if (filterByConsumer || filterBySupplied) {
                        String typeOfOrder = documentSnapshot.getString("type_of_order");

                        matchesTypeOfOrder = (filterByConsumer && "Consumer".equals(typeOfOrder)) ||
                                (filterBySupplied && "Supplier".equals(typeOfOrder));
                    }

                    boolean matchesPeopleLimit = true;
                    if (filterByPeopleLimit || filterByUnlimitedPeople) {
                        Long maxPeopleDoc = documentSnapshot.getLong("max_people");
                        if (filterByUnlimitedPeople) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc == 0;
                        } else if (filterByPeopleLimit) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc >= 2 && maxPeopleDoc <= peopleLimit && maxPeopleDoc != 0;
                        }
                    }

                    boolean matchesTime = true;
                    if (filterByTime) {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // בדיקה האם הזמן עבר
                    }

                    if (matchesCategory && matchesLocation && matchesTypeOfOrder && matchesPeopleLimit && matchesTime) {
                        results.append(documentSnapshot.getId()).append(";")
                                .append(documentSnapshot.getString("titleOfOrder")).append(";")
                                .append(documentSnapshot.getGeoPoint("location").getLatitude()).append(",").append(documentSnapshot.getGeoPoint("location").getLongitude()).append(";")
                                .append(documentSnapshot.getLong("NumberOfPeopleInOrder")).append(";")
                                .append(documentSnapshot.getLong("max_people")).append(";")
                                .append(documentSnapshot.getString("categorie")).append(";")
                                .append(distanceInKm).append(";")
                                .append(documentSnapshot.getTimestamp("time").getSeconds()).append("\n");
                    }
                }

                Intent intent = new Intent(FilterActivity.this, HomePageActivity.class);
                if (results.length() == 0) {
                    intent.putExtra("noOrdersFound", true);
                } else {
                    intent.putExtra("filteredOrders", results.toString());
                    intent.putExtra("filterActive", true);
                }
                startActivity(intent);
            }
        });
    }


    private void fetchOrdersByString(String str, String address, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedTime) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.whereEqualTo("URL", str).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                StringBuilder results = new StringBuilder();
                long currentTimeMillis = System.currentTimeMillis(); // זמן נוכחי במילישניות
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // דילוג על הזמנות מלאות

                    boolean matchesCategory = true;
                    if (filterByCategory) {
                        String categorie = documentSnapshot.getString("categorie");
                        matchesCategory = selectedCategories.contains(categorie);
                    }

                    boolean matchesLocation = true;
                    double distanceInKm = 0;
                    if (!address.isEmpty()) {
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        if (geoPoint != null) {
                            double orderLat = geoPoint.getLatitude();
                            double orderLon = geoPoint.getLongitude();

                            float[] resultsArray = new float[1];
                            android.location.Location.distanceBetween(lastLatitude, lastLongitude, orderLat, orderLon, resultsArray);
                            float distanceInMeters = resultsArray[0];
                            distanceInKm = distanceInMeters / 1000;

                            matchesLocation = distanceInKm <= lastDistance;
                        } else {
                            matchesLocation = false;
                        }
                    }

                    boolean matchesTypeOfOrder = true;
                    if (filterByConsumer || filterBySupplied) {
                        String typeOfOrder = documentSnapshot.getString("type_of_order");

                        matchesTypeOfOrder = (filterByConsumer && "Consumer".equals(typeOfOrder)) ||
                                (filterBySupplied && "Supplier".equals(typeOfOrder));
                    }

                    boolean matchesPeopleLimit = true;
                    if (filterByPeopleLimit || filterByUnlimitedPeople) {
                        Long maxPeopleDoc = documentSnapshot.getLong("max_people");
                        if (filterByUnlimitedPeople) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc == 0;
                        } else if (filterByPeopleLimit) {
                            matchesPeopleLimit = maxPeopleDoc != null && maxPeopleDoc >= 2 && maxPeopleDoc <= peopleLimit && maxPeopleDoc != 0;
                        }
                    }

                    boolean matchesTime = true;
                    if (filterByTime) {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // בדיקה האם הזמן עבר
                    }

                    if (matchesCategory && matchesLocation && matchesTypeOfOrder && matchesPeopleLimit && matchesTime) {
                        results.append(documentSnapshot.getId()).append(";")
                                .append(documentSnapshot.getString("titleOfOrder")).append(";")
                                .append(documentSnapshot.getGeoPoint("location").getLatitude()).append(",").append(documentSnapshot.getGeoPoint("location").getLongitude()).append(";")
                                .append(documentSnapshot.getLong("NumberOfPeopleInOrder")).append(";")
                                .append(documentSnapshot.getLong("max_people")).append(";")
                                .append(documentSnapshot.getString("categorie")).append(";")
                                .append(distanceInKm).append(";")
                                .append(documentSnapshot.getTimestamp("time").getSeconds()).append("\n");
                    }
                }

                Intent intent = new Intent(FilterActivity.this, HomePageActivity.class);
                if (results.length() == 0) {
                    intent.putExtra("noOrdersFound", true);
                } else {
                    intent.putExtra("filteredOrders", results.toString());
                    intent.putExtra("filterActive", true);
                }
                startActivity(intent);
            }
        });
    }

    private List<String> getSelectedCategories() {
        SparseBooleanArray checkedItems = categoryListView.getCheckedItemPositions();
        List<String> selectedCategories = new ArrayList<>();
        for (int i = 0; i < checkedItems.size(); i++) {
            int position = checkedItems.keyAt(i);
            if (checkedItems.valueAt(i)) {
                selectedCategories.add(adapter.getItem(position));
            }
        }
        return selectedCategories;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    public void showTimePickerDialog(View view) {
        Locale.setDefault(Locale.ENGLISH); // הגדרת השפה לאנגלית
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(FilterActivity.this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar currentDate = Calendar.getInstance();
                currentDate.set(Calendar.YEAR, year);
                currentDate.set(Calendar.MONTH, month);
                currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (currentDate.before(Calendar.getInstance())) {
                    Toast.makeText(FilterActivity.this, "You can't choose a past date", Toast.LENGTH_SHORT).show();
                } else {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(FilterActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedDate.set(Calendar.MINUTE, minute);

                            if (selectedDate.before(Calendar.getInstance())) {
                                Toast.makeText(FilterActivity.this, "You cannot select a past time", Toast.LENGTH_SHORT).show();
                            } else {
                                selectedTime = selectedDate;
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String dateTime = sdf.format(selectedDate.getTime());

                                timeEditText.setText(dateTime);
                                findViewById(R.id.clear_time_button).setVisibility(View.VISIBLE);
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
