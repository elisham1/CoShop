package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

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
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intent.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intent);
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        String apiKey = "AIzaSyCCEZAKwn0TCA-XvVpDKTOVrdiM__RfwCI"; // החלף במפתח ה-API שלך

        // Initialize Places
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
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
                                int numberOfItemsInList = categoriesList.size();
                                addOtherCategory(categoriesList, numberOfItemsInList);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error getting categories: ", task.getException());
                    }
                });
    }

    public void addOtherCategory(List<String> categoriesList, int numberOfItemsInList) {
        categoriesList.add("Other");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoriesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedCategory = (String) adapterView.getItemAtPosition(position);
                saveNewCategorieName = selectedCategory;
                Log.d("Selected Category", saveNewCategorieName);
                if (selectedCategory.equals("Other")) {
                    showNewCategoryDialog(categoriesList, numberOfItemsInList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public void showNewCategoryDialog(List<String> categoriesList, int numberOfItemsInList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter New Category");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newCategory = input.getText().toString();
            saveNewCategorieName = newCategory;
            Log.d("New Category", saveNewCategorieName);
            if (!newCategory.equalsIgnoreCase("Other") && !newCategory.trim().isEmpty()) {
                addCategoryToFirestore(newCategory, categoriesList, numberOfItemsInList);
            } else {
                showNewCategoryDialog(categoriesList, numberOfItemsInList);
                Toast.makeText(OpenNewOrderActivity.this, "You cannot enter that! Enter again.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            readCategoriesFromFireStore();
        });

        builder.show();
    }

    public void addCategoryToFirestore(String newCategory, List<String> categoriesList, int numberOfItemsInList) {
        String saveCategory = newCategory;
        String lowercaseCategory = newCategory.toLowerCase();

        db.collection("categories").document("jQ4hXL6kr1AbKwPvEdXl")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (categoriesList != null) {
                        if (!containsIgnoreCase(categoriesList, lowercaseCategory)) {
                            Log.d("Category Size", String.valueOf(numberOfItemsInList));
                            Log.d("Category List Size", String.valueOf(categoriesList.size()));

                            if ((numberOfItemsInList + 1) < categoriesList.size()) {
                                categoriesList.remove(categoriesList.size() - 1);
                            }
                            categoriesList.add(saveCategory);
                            categorySpinner.setSelection(categoriesList.indexOf(saveCategory));
                        } else {
                            Toast.makeText(OpenNewOrderActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                            String correctCasedCategory = null;
                            for (String category : categoriesList) {
                                if (category.equalsIgnoreCase(newCategory)) {
                                    correctCasedCategory = category;
                                    break;
                                }
                            }
                            categorySpinner.setSelection(categoriesList.indexOf(correctCasedCategory));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error getting categories: " + e.getMessage());
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
        saveOrder(url, description, title, maxPeople);
        Intent intent = new Intent(OpenNewOrderActivity.this, MyOrdersActivity.class);
        startActivity(intent);
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

    private void saveOrder(String url, String description, String title, int maxPeople) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = null;
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // שמירת המשתנים כ- final או effectively final
        String finalUserEmail = userEmail;
        String finalUrl = url;
        String finalDescription = description;
        String finalTitle = title;

        // קריאה ל-Firestore כדי לקבל את ה-type of user
        db.collection("users").document(finalUserEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String userType = document.getString("type of user");

                            // המשך הוספת ההזמנה לאחר קבלת ה-type of user
                            Map<String, Object> order = new HashMap<>();
                            order.put("URL", finalUrl);
                            order.put("description", finalDescription);
                            order.put("categorie", saveNewCategorieName);
                            order.put("user_email", finalUserEmail);
                            order.put("max_people", max_people_in_order);
                            order.put("type_of_order", userType); // הוספת ה-type of user להזמנה
                            order.put("titleOfOrder", finalTitle);
                            order.put("time", new Timestamp(selectedTime.getTime()));
                            order.put("openOrderTime", new Timestamp(new Date())); // הוספת הזמן הנוכחי כ-openOrderTime
                            order.put("NumberOfPeopleInOrder",1);

                            GeoPoint geoPoint = new GeoPoint(lastLatitude, lastLongitude);
                            order.put("location", geoPoint);

                            ArrayList<String> listPeopleInOrder = new ArrayList<>();
                            listPeopleInOrder.add(finalUserEmail);
                            order.put("listPeopleInOrder", listPeopleInOrder);

                            db.collection("orders")
                                    .add(order)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(OpenNewOrderActivity.this, "Order added successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error adding order: " + e.getMessage());
                                        Toast.makeText(OpenNewOrderActivity.this, "Failed to add order", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
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
