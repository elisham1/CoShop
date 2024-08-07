package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

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

// Handles filtering functionality for orders
public class FilterActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private TextView searchAddressText;
    private ListView categoryListView;
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private Calendar selectedDate;
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
    private TextView dateError, noFiltersError;
    private LinearLayout timeLayout, dateLayout;
    private String lastURL, globalUserType;
    private MenuUtils menuUtils;
    private boolean isDatePickerDialogOpen = false;
    private boolean isTimePickerDialogOpen = false;
    private boolean isCategoryListVisible = false;

    // Initializes the activity and its components
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

        setContentView(R.layout.activity_filter);
        menuUtils = new MenuUtils(this, globalUserType);

        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);
        editTextURL = findViewById(R.id.editTextText2);
        clearURLButton = findViewById(R.id.clear_url_button);
        rangeSlider = findViewById(R.id.rangeSliderPeople);
        unlimitEditText = findViewById(R.id.unlimit_value);
        checkBoxLimit = findViewById(R.id.checkBoxLimit);
        checkBoxUnlimited = findViewById(R.id.checkBoxUnlimited);
        dateError = findViewById(R.id.dateError);
        noFiltersError = findViewById(R.id.noFiltersError);
        timeLayout = findViewById(R.id.time_container);
        dateLayout = findViewById(R.id.date_container);

        ImageButton plusIcon = findViewById(R.id.plus_icon);
        plusIcon.setOnClickListener(this::toggleCategoryVisibility);

        TextView categoryTextView = findViewById(R.id.category_text);
        categoryTextView.setOnClickListener(v -> toggleCategoryVisibility(plusIcon));

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

        searchAddressButton.setOnClickListener(v -> {
            if (searchAddressButton.getTag() != null ) {
                if (searchAddressButton.getTag().equals("clear")) {
                    searchAddressText.setText("");
                    searchAddressButton.setTag("search");
                    searchAddressButton.setImageResource(R.drawable.baseline_search_24);
                    editAddressButton.setVisibility(View.GONE);

                    // Reset last values
                    lastAddress = null;
                    lastDistance = 0;
                    lastLatitude = 0;
                    lastLongitude = 0;
                } else {
                    Intent intentLocation = new Intent(FilterActivity.this, LocationWindow.class);
                    intentLocation.putExtra("userType", globalUserType);
                    if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                        intentLocation.putExtra("address", lastAddress);
                        intentLocation.putExtra("distance", lastDistance);
                    }
                    locationWindowLauncher.launch(intentLocation);
                }
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
            Intent intentLocation = new Intent(FilterActivity.this, LocationWindow.class);
            intentLocation.putExtra("userType", globalUserType);
            if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                intentLocation.putExtra("address", lastAddress);
                intentLocation.putExtra("distance", lastDistance);
            }
            locationWindowLauncher.launch(intentLocation);
        });

        editAddressButton.setOnClickListener(v -> {
            Intent intentLocation = new Intent(FilterActivity.this, LocationWindow.class);
            intentLocation.putExtra("userType", globalUserType);
            if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                intentLocation.putExtra("address", lastAddress);
                intentLocation.putExtra("distance", lastDistance);
            }
            locationWindowLauncher.launch(intentLocation);
        });

        clearURLButton.setOnClickListener(v -> editTextURL.setText(""));

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

        db = FirebaseFirestore.getInstance();
        categoryListView = findViewById(R.id.category_list);

        readCategoriesFromFireStore();

        ImageButton dateIcon = findViewById(R.id.date_icon);
        TextView dateText = findViewById(R.id.date_text);
        dateIcon.setOnClickListener(this::showDatePickerDialog);
        dateText.setOnClickListener(this::showDatePickerDialog);

        ImageButton timeIcon = findViewById(R.id.time_icon);
        TextView timeText = findViewById(R.id.time_text);
        timeIcon.setOnClickListener(this::showTimePickerDialog);
        timeText.setOnClickListener(this::showTimePickerDialog);

    }

    // Toggles the search/clear icon for the address input
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

    // Reads categories from Firestore and sets them in the ListView
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

    // Toggles the visibility of the category list
    public void toggleCategoryVisibility(View view) {
        ListView categoryListView = findViewById(R.id.category_list);
        ImageButton plusIcon = (ImageButton) view;

        if (isCategoryListVisible) {
            categoryListView.setVisibility(View.GONE);
            plusIcon.setImageResource(R.drawable.baseline_add_24);
        } else {
            categoryListView.setVisibility(View.VISIBLE);
            plusIcon.setImageResource(R.drawable.baseline_check_24);
        }
        isCategoryListVisible = !isCategoryListVisible;

        updateCategoryTextView(); // Update the TextView after changes in the category list
    }

    // Handles the order filtering logic
    public void OrderFiltering(View v) {
        String address = searchAddressText.getText().toString();
        String urlOrString = editTextURL.getText().toString();

        List<String> selectedCategories = getSelectedCategories();
        updateCategoryTextView();
        boolean filterByLocation = !address.isEmpty() || lastAddress != null;
        boolean filterByURLOrString = !urlOrString.isEmpty();
        boolean filterByCategory = selectedCategories != null && !selectedCategories.isEmpty();
        boolean filterByConsumer = ((CheckBox) findViewById(R.id.checkBoxConsumer)).isChecked();
        boolean filterBySupplied = ((CheckBox) findViewById(R.id.checkBoxSupplied)).isChecked();
        boolean filterByPeopleLimit = checkBoxLimit.isChecked();
        boolean filterByUnlimitedPeople = checkBoxUnlimited.isChecked();
        boolean filterByTime = selectedTime != null && selectedDate != null;
        boolean noFiltersChosen = !filterByLocation && !filterByURLOrString &&
                !filterByCategory && !filterByConsumer && !filterBySupplied &&
                !filterByPeopleLimit && !filterByUnlimitedPeople && !filterByTime;

        int peopleLimit = 0;

        if (selectedDate != null && selectedTime == null) {
            dateError.setVisibility(View.VISIBLE);
            dateError.setText("You need to select a time");
            timeLayout.setBackgroundResource(R.drawable.red_border);
            dateLayout.setBackgroundResource(R.drawable.border);
            noFiltersError.setVisibility(View.GONE);
            return;
        }

        if (selectedTime != null && selectedDate == null) {
            dateError.setVisibility(View.VISIBLE);
            dateError.setText("You need to select a date");
            timeLayout.setBackgroundResource(R.drawable.border);
            dateLayout.setBackgroundResource(R.drawable.red_border);
            noFiltersError.setVisibility(View.GONE);
            return;
        }

        if (filterByPeopleLimit) {
            String peopleLimitStr = unlimitEditText.getText().toString().trim();
            if (!peopleLimitStr.isEmpty()) {
                peopleLimit = Integer.parseInt(peopleLimitStr);
            } else {
                filterByPeopleLimit = false;
            }
        }

        if (noFiltersChosen) {
            noFiltersError.setVisibility(View.VISIBLE);
            return;
        } else {
            noFiltersError.setVisibility(View.GONE);
        }

        if (selectedDate != null && selectedTime != null) {
            Calendar selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
            selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
            selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
            selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
            selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));

            if (selectedDateTime.before(Calendar.getInstance())) {
                dateError.setVisibility(View.VISIBLE);
                dateError.setText("The selected date and time have already passed");
                timeLayout.setBackgroundResource(R.drawable.red_border);
                dateLayout.setBackgroundResource(R.drawable.red_border);
                Log.d(this.getLocalClassName(), "The selected date and time have already passed");
                return;
            }
        }

        if (filterByURLOrString) {
            if (isValidURL(urlOrString)) {
                fetchOrdersByUrl(urlOrString, address, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedDate, selectedTime);
            } else if (isValidString(urlOrString)) {
                fetchOrdersByString(urlOrString, address, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedDate, selectedTime);
            } else {
                Log.d(this.getLocalClassName(), "Invalid URL or String");
            }
        } else {
            if (filterByLocation) {
                if (address.isEmpty()) {
                    address = lastAddress;
                }

                double userLat = lastLatitude;
                double userLon = lastLongitude;

                fetchOrders(userLat, userLon, lastDistance, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedDate, selectedTime);
            } else {
                fetchOrders(0, 0, 0, selectedCategories, filterByCategory, filterByConsumer, filterBySupplied, filterByPeopleLimit, peopleLimit, filterByUnlimitedPeople, filterByTime, selectedDate, selectedTime);
            }
        }
    }

    // Resets all filters to their default state
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
        rangeSlider.setValues(2f);

        unlimitEditText.setText("1000");
        rangeSlider.setEnabled(false);
        unlimitEditText.setEnabled(false);

        // Reset category ListView
        for (int i = 0; i < categoryListView.getCount(); i++) {
            categoryListView.setItemChecked(i, false);
        }

        // Reset the saved address and distance values
        lastAddress = null;
        lastLatitude = 0;
        lastLongitude = 0;
        lastDistance = 0;

        // Reset the date and time
        TextView dateText = findViewById(R.id.date_text);
        dateText.setText("Select Date");

        TextView timeText = findViewById(R.id.time_text);
        timeText.setText("Select Time");

        selectedDate = null;
        selectedTime = null;

        //reset errors
        noFiltersError.setVisibility(View.GONE);
        dateError.setVisibility(View.GONE);
        timeLayout.setBackgroundResource(R.drawable.border);
        dateLayout.setBackgroundResource(R.drawable.border);
    }

    // Validates if a string is a valid URL
    private boolean isValidURL(String url) {
        Pattern pattern = Patterns.WEB_URL;
        return pattern.matcher(url).matches();
    }

    // Validates if a string is a valid text string
    private boolean isValidString(String str) {
        return str.matches("[a-zA-Z ]+");
    }

    // Retrieves the domain name from a URL
    private String getDomainName(String url) {
        try {
            java.net.URL netUrl = new java.net.URL(url);
            return netUrl.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    // Fetches orders based on various filters
    private void fetchOrders(double userLat, double userLon, int distance, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedDate, Calendar selectedTime) {
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
                StringBuilder orderIds = new StringBuilder();  // Collects order IDs

                long currentTimeMillis = System.currentTimeMillis();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    Timestamp timestamp = documentSnapshot.getTimestamp("time");
                    long timeRemaining = timestamp.toDate().getTime() - currentTimeMillis;

                    if (timeRemaining <= 0) continue; // Skip orders that have expired

                    String orderOwner = documentSnapshot.getString("ownerEmail");
                    List<String> joinedUsers = (List<String>) documentSnapshot.get("listPeopleInOrder");

                    if (orderOwner != null && orderOwner.equals(userEmail)) continue; // Skip orders created by the user
                    if (joinedUsers != null && joinedUsers.contains(userEmail)) continue; // Skip orders joined by the user

                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

//                    if (numberOfPeopleInOrder == maxPeople) continue; // Skip full orders

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
                    if (filterByTime && selectedDate != null && selectedTime != null) {
                        Calendar selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
                        selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
                        selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));

                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedDateTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // Check if the time has passed
                    }

                    if (matchesCategory && matchesLocation && matchesTypeOfOrder && matchesPeopleLimit && matchesTime) {
                        if (distance > 0) {
                            results.append(documentSnapshot.getId()).append(";")
                                    .append(documentSnapshot.getString("titleOfOrder")).append(";")
                                    .append(documentSnapshot.getGeoPoint("location").getLatitude()).append(",").append(documentSnapshot.getGeoPoint("location").getLongitude()).append(";")
                                    .append(documentSnapshot.getLong("NumberOfPeopleInOrder")).append(";")
                                    .append(documentSnapshot.getLong("max_people")).append(";")
                                    .append(documentSnapshot.getString("categorie")).append(";")
                                    .append(distanceInKm).append(";")
                                    .append(documentSnapshot.getTimestamp("time").getSeconds()).append("\n");
                        } else {
                            orderIds.append(documentSnapshot.getId()).append("\n");
                        }
                    }
                }

                Intent intent = new Intent(FilterActivity.this, HomePageActivity.class);
                intent.putExtra("userType", globalUserType);
                intent.putExtra("filterActive", true); // Always indicate that filtering is active

                if (distance > 0) {
                    if (results.length() == 0) {
                        intent.putExtra("noOrdersFound", true);
                    } else {
                        intent.putExtra("filteredOrders", results.toString());
                    }
                } else {
                    if (orderIds.length() == 0) {
                        intent.putExtra("noOrdersFound", true);
                    } else {
                        intent.putExtra("filteredOrderIds", orderIds.toString());
                    }
                }
                startActivity(intent);
                finish();
            }
        });
    }

    // Updates the category TextView with the selected categories
    private void updateCategoryTextView() {
        TextView categoryTextView = findViewById(R.id.category_text);
        List<String> selectedCategories = getSelectedCategories();
        if (selectedCategories.isEmpty()) {
            categoryTextView.setText("Open Category list");
        } else {
            String categoriesText = String.join(", ", selectedCategories);
            categoryTextView.setText(categoriesText);
        }
    }

    // Fetches orders by URL
    private void fetchOrdersByUrl(String url, String address, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedDate, Calendar selectedTime) {
        String domainName = getDomainName(url);
        if (domainName == null) {
            Log.d(this.getLocalClassName(), "Invalid URL");
            return;
        }

        CollectionReference ordersRef = db.collection("orders");
        ordersRef.whereEqualTo("URL", domainName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                StringBuilder results = new StringBuilder();
                long currentTimeMillis = System.currentTimeMillis(); // Current time in milliseconds
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // Skip full orders

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
                    if (filterByTime && selectedDate != null && selectedTime != null) {
                        Calendar selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
                        selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
                        selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));

                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedDateTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // Check if the time has passed
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
                intent.putExtra("userType", globalUserType);
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

    // Fetches orders by string
    private void fetchOrdersByString(String str, String address, List<String> selectedCategories, boolean filterByCategory, boolean filterByConsumer, boolean filterBySupplied, boolean filterByPeopleLimit, int peopleLimit, boolean filterByUnlimitedPeople, boolean filterByTime, Calendar selectedDate, Calendar selectedTime) {
        CollectionReference ordersRef = db.collection("orders");
        ordersRef.whereEqualTo("URL", str).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                StringBuilder results = new StringBuilder();
                long currentTimeMillis = System.currentTimeMillis(); // Current time in milliseconds
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                    long numberOfPeopleInOrder = documentSnapshot.getLong("NumberOfPeopleInOrder");
                    long maxPeople = documentSnapshot.getLong("max_people");

                    if (numberOfPeopleInOrder == maxPeople) continue; // Skip full orders

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
                    if (filterByTime && selectedDate != null && selectedTime != null) {
                        Calendar selectedDateTime = Calendar.getInstance();
                        selectedDateTime.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
                        selectedDateTime.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY));
                        selectedDateTime.set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE));

                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis <= selectedDateTime.getTimeInMillis() && orderTimeInMillis > currentTimeMillis;
                    } else {
                        long orderTimeInMillis = documentSnapshot.getTimestamp("time").toDate().getTime();
                        matchesTime = orderTimeInMillis > currentTimeMillis; // Check if the time has passed
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
                intent.putExtra("userType", globalUserType);
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

    // Gets the selected categories from the ListView
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

    // Shows the date picker dialog
    public void showDatePickerDialog(View view) {
        if (isDatePickerDialogOpen) {
            return; // Do not open a new dialog if one is already open
        }

        isDatePickerDialogOpen = true;
        Locale.setDefault(Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                FilterActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth, // Ensures the dialog is wide enough
                (view1, year, month, dayOfMonth) -> {
                    isDatePickerDialogOpen = false; // Update status after closing the dialog
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    if (this.selectedTime != null) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, this.selectedTime.get(Calendar.HOUR_OF_DAY));
                        selectedDate.set(Calendar.MINUTE, this.selectedTime.get(Calendar.MINUTE));
                    }

                    if (selectedDate.before(Calendar.getInstance())) {
                        Log.d(this.getLocalClassName(), "You can't choose a past date");
                    } else {
                        this.selectedDate = selectedDate;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String dateString = sdf.format(selectedDate.getTime());

                        TextView dateText = findViewById(R.id.date_text);
                        dateText.setText(dateString);
                        dateLayout.setBackgroundResource(R.drawable.border);
                        dateError.setVisibility(View.GONE);

                        ImageButton dateIcon = findViewById(R.id.date_icon);
                        dateIcon.setImageResource(R.drawable.baseline_calendar_month_24);
                    }
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setOnDismissListener(dialog -> isDatePickerDialogOpen = false);
        datePickerDialog.show();
    }

    // Shows the time picker dialog
    public void showTimePickerDialog(View view) {
        if (isTimePickerDialogOpen) {
            return; // Do not open a new dialog if one is already open
        }

        isTimePickerDialogOpen = true;
        Locale.setDefault(Locale.ENGLISH);
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                FilterActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth, // Ensures the dialog is wide enough
                (view1, hourOfDay, minute) -> {
                    isTimePickerDialogOpen = false; // Update status after closing the dialog
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);

                    if (this.selectedDate != null) {
                        selectedTime.set(Calendar.YEAR, this.selectedDate.get(Calendar.YEAR));
                        selectedTime.set(Calendar.MONTH, this.selectedDate.get(Calendar.MONTH));
                        selectedTime.set(Calendar.DAY_OF_MONTH, this.selectedDate.get(Calendar.DAY_OF_MONTH));
                    }

                    if (selectedTime.before(Calendar.getInstance())) {
                        Log.d(this.getLocalClassName(), "You cannot select a past time");
                    } else {
                        this.selectedTime = selectedTime;
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        String timeString = sdf.format(selectedTime.getTime());

                        TextView timeText = findViewById(R.id.time_text);
                        timeText.setText(timeString);
                        timeLayout.setBackgroundResource(R.drawable.border);
                        dateError.setVisibility(View.GONE);

                        ImageButton timeIcon = findViewById(R.id.time_icon);
                        timeIcon.setImageResource(R.drawable.ic_baseline_access_time_24);
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        );

        timePickerDialog.setOnDismissListener(dialog -> isTimePickerDialogOpen = false);
        timePickerDialog.show();
    }

    // Creates the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    // Handles menu item selections
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
