package com.elisham.coshop;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpdateUserDetailsActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private TextView searchAddressText;
    private ImageButton searchAddressButton;
    private ImageButton editAddressButton;
    private String lastAddress;
    private double lastLatitude;
    private double lastLongitude;
    private int lastDistance;
    private TextView fullNameTextView;
    private TextView emailTextView;
    private EditText addressEditText;
    private TextView typeOfUserTextView;
    private ImageView profileImageView;

    private String email, firstName, familyName, userType, picUrl;
    private GeoPoint address;
    private String newFirstName, newFamilyName;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private boolean changeName = false;
    private boolean changePic = false;
    private boolean changeLocation = false;
    private Uri imageUri;
    private static final int TAKE_PHOTO_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int UPDATE_CATEGORIES_REQUEST = 3;
    private static final int GOOGLE_SIGN_IN_REQUEST_CODE = 1001;
    private MenuUtils menuUtils;
    private ArrayList<String> currentCategories;
    private String globalUserType, webClientId;
    private ProgressDialog progressDialog;

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
        setContentView(R.layout.activity_update_user_details);
        menuUtils = new MenuUtils(this,globalUserType);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        Resources res = getResources();
        int defaultWebClientId = res.getIdentifier("default_web_client_id", "string", getPackageName()); // Get the resource ID dynamically
        webClientId = res.getString(defaultWebClientId);

        emailTextView = findViewById(R.id.emailText);
        fullNameTextView = findViewById(R.id.fullName);
        typeOfUserTextView = findViewById(R.id.type_of_user);
        profileImageView = findViewById(R.id.profileImage);

        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);
        currentCategories = new ArrayList<String>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        getUserInformation();

        showLocationWindow();

        fullNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNameEditDialog();
            }
        });

        ImageButton nameEditButton = findViewById(R.id.edit_name_button);
        nameEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNameEditDialog();
            }
        });

        LinearLayout profilePic = findViewById(R.id.profilePic);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picUrl == null)
                {
                    profileImageView.setImageResource(R.drawable.ic_profile);
                }
                showImageSourceDialog(picUrl);
            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picUrl == null)
                {
                    profileImageView.setImageResource(R.drawable.ic_profile);
                }
                showImageSourceDialog(picUrl);
            }
        });

        ImageButton updateCategoriesButton = findViewById(R.id.update_categories_button);
        updateCategoriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UpdateUserDetailsActivity.this, CategoriesActivity.class);
                intent.putExtra("userType", globalUserType);
                intent.putExtra("categories_update", true);
                startActivityForResult(intent, UPDATE_CATEGORIES_REQUEST);
            }
        });

        TextView categoriesText = findViewById(R.id.update_categories_text);
        categoriesText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UpdateUserDetailsActivity.this, CategoriesActivity.class);
                intent.putExtra("userType", globalUserType);
                intent.putExtra("categories_update", true);
                startActivityForResult(intent, UPDATE_CATEGORIES_REQUEST);
            }
        });


        ImageButton changePasswordButton = findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        TextView passwordText = findViewById(R.id.change_password_text);
        passwordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        TextView deleteAccountButton = findViewById(R.id.deleteButton);
        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAccount();
            }
        });

    }

    private void showLocationWindow() {
        LinearLayout searchRow = findViewById(R.id.search_row);
        // Update the height of the layout
        ViewGroup.LayoutParams params = searchRow.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        locationWindowLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        lastAddress = result.getData().getStringExtra("address");
                        lastLatitude = result.getData().getDoubleExtra("latitude", 0);
                        lastLongitude = result.getData().getDoubleExtra("longitude", 0);
                        if (lastLatitude != 0 && lastLongitude != 0) {
                            address = new GeoPoint(lastLatitude, lastLongitude);
                            Map<String, Object> updateAddress = new HashMap<>();
                            updateAddress.put("address", address);
                            progressDialog.show();
                            updateDB(updateAddress);
                        }

                        if (lastAddress != null) {
                            searchAddressText.setText(lastAddress);
                            searchRow.setLayoutParams(params);// Display address only without KM
                            searchAddressButton.setVisibility(View.VISIBLE);
                            searchAddressButton.setTag("clear");
                            searchAddressButton.setImageResource(R.drawable.clear);
                            editAddressButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        searchRow.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("userType", globalUserType);
            intent.putExtra("hideDistanceLayout", true); // Hide KM
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intent.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intent);
        });

        searchAddressButton.setOnClickListener(v -> {
            if (searchAddressButton.getTag() != null) {
                if (searchAddressButton.getTag().equals("clear")) {
                    searchAddressText.setText("");
                    searchAddressButton.setTag("search");
                    searchAddressButton.setImageResource(R.drawable.baseline_search_24);
                    editAddressButton.setVisibility(View.GONE);

                    lastAddress = null;
                    lastLatitude = 0;
                    lastLongitude = 0;
                } else {
                    Intent intent = new Intent(UpdateUserDetailsActivity.this, LocationWindow.class);
                    intent.putExtra("hideDistanceLayout", true);
                    if (lastAddress != null && !lastAddress.isEmpty()) {
                        intent.putExtra("address", lastAddress);
                    }
                    locationWindowLauncher.launch(intent);
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
            public void afterTextChanged(Editable s) {
            }
        });

        editAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("userType", globalUserType);
            intent.putExtra("hideDistanceLayout", true); // Hide KM
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intent.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intent);
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

    private void showImageSourceDialog(String profileImageUrl) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        // Create a list of options
        List<CharSequence> options = new ArrayList<>();
        options.add("Take Photo");
        options.add("Choose from Gallery");
        if (profileImageUrl != null) {
            options.add("View Photo");
            options.add("Delete Photo");
        }

        CharSequence[] items = options.toArray(new CharSequence[0]);
        builder.setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("ImageSourceDialog", "Selected option: " + which);
                        switch (which) {
                            case 0:
                                Log.d("ImageSourceDialog", "Take Photo");
                                checkCameraPermissionAndTakePhoto();
                                break;
                            case 1:
                                Log.d("ImageSourceDialog", "Choose from Gallery");
                                openFileChooser();
                                break;
                            case 2:
                                if (profileImageUrl != null) {
                                    // Handle viewing the photo
                                    viewPhoto(profileImageUrl);
                                }
                                break;
                            case 3:
                                if (profileImageUrl != null) {
                                    // Handle deleting the photo
                                    progressDialog.show();
                                    deleteUserProfileImage(profileImageUrl);
                                    picUrl = null;
                                    Map<String, Object> updatePicUrl = new HashMap<>();
                                    updatePicUrl.put("profileImageUrl", picUrl);
                                    updateDB(updatePicUrl);
                                    changePic = true;
                                    profileImageView.setImageResource(R.drawable.ic_profile);
                                }
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void viewPhoto(String url) {
        // Implement the logic to view the photo
        // For example, you can start an activity that shows the image
        ImageDialogFragment dialogFragment = ImageDialogFragment.newInstance(url);
        dialogFragment.show(getSupportFragmentManager(), "image_dialog");
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO_REQUEST);
        }
        else {
            takePhoto();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TAKE_PHOTO_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Log.d("UpdateUserDetailsActivity", "Permission denied");
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    reauthenticateUser(currentUser, credential);
                }
            } catch (ApiException e) {
                Log.e("UpdateUserDetailsActivity", "Google sign in failed", e);
            }
        } else if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                bitmap = rotateImageIfRequired(bitmap, imageUri);
                profileImageView.setImageBitmap(bitmap);
                progressDialog.show();
                Map<String, Object> updatePicUrl = new HashMap<>();
                uploadImage(updatePicUrl);
                changePic = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_OK && requestCode == TAKE_PHOTO_REQUEST && data != null && data.getExtras() != null) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imageUri = getImageUri(bitmap);
            try {
                bitmap = rotateImageIfRequired(bitmap, imageUri);
                profileImageView.setImageBitmap(bitmap);
                Map<String, Object> updatePicUrl = new HashMap<>();
                progressDialog.show();
                uploadImage(updatePicUrl);
                changePic = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private Uri getImageUri(Bitmap bitmap) {
        // Create a file for the image
        File imagesFolder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ProfilePic");
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }
        File imageFile = new File(imagesFolder, "ProfilePic_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Use FileProvider to get the content URI
        return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", imageFile);
    }

    private void showNameEditDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText firstNameEditText = dialogView.findViewById(R.id.firstNameEditText);
        EditText familyNameEditText = dialogView.findViewById(R.id.familyNameEditText);
        ImageView clearFirstNameIcon = dialogView.findViewById(R.id.clearFirstNameIcon);
        ImageView clearFamilyNameIcon = dialogView.findViewById(R.id.clearFamilyNameIcon);
        LinearLayout firstNameLayout = dialogView.findViewById(R.id.firstNameLayout);
        TextView firstNameError = dialogView.findViewById(R.id.firstNameError);

        // Set current values
        if (firstName != null) {
            firstNameError.setVisibility(View.GONE);
            firstNameEditText.setText(firstName);
            clearFirstNameIcon.setVisibility(View.VISIBLE);
        }
        if (familyName != null && !familyName.isEmpty()) {
            familyNameEditText.setText(familyName);
            clearFamilyNameIcon.setVisibility(View.VISIBLE);
        }

        // Set onClickListener for the first name clear icon
        clearFirstNameIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstNameEditText.setText(""); // Clear the text in EditText
            }
        });

        // Set onClickListener for the family name clear icon
        clearFamilyNameIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                familyNameEditText.setText(""); // Clear the text in EditText
            }
        });

        firstNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    firstNameError.setVisibility(View.GONE);
                    firstNameLayout.setBackgroundResource(R.drawable.border);
                    clearFirstNameIcon.setVisibility(View.VISIBLE);
                } else {
                    firstNameLayout.setBackgroundResource(R.drawable.red_border);
                    firstNameError.setVisibility(View.VISIBLE);
                    clearFirstNameIcon.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        familyNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearFamilyNameIcon.setVisibility(View.VISIBLE);
                } else {
                    clearFamilyNameIcon.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        builder.setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();

        // Set the onClickListener for the positive button after showing the dialog
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newFirstName = firstNameEditText.getText().toString().trim();
                if (newFirstName.isEmpty()) {
                    firstNameError.setVisibility(View.VISIBLE);
                    firstNameLayout.setBackgroundResource(R.drawable.red_border);
                    return;
                }
                firstName = newFirstName;
                newFamilyName = familyNameEditText.getText().toString().trim();
                if (newFamilyName.isEmpty()) {
                    newFamilyName = "";
                }
                familyName = newFamilyName;

                // Update UI with new names
                String fullName = newFirstName + " " + newFamilyName;
                fullNameTextView.setText(fullName);

                // Update Firestore with the new details
                Map<String, Object> updateName = new HashMap<>();
                updateName.put("first name", firstName);
                updateName.put("family name", familyName);
                progressDialog.show();
                updateDB(updateName);
                // Dismiss dialog
                dialog.dismiss();
            }
        });
    }

    public void getUserInformation() {
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            progressDialog.show();
            email = currentUser.getEmail();
            db.collection("users").document(email).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Get the name and family name fields
                                    firstName = document.getString("first name");
                                    familyName = document.getString("family name");
                                    address = document.getGeoPoint("address");
                                    userType = document.getString("type of user");
                                    picUrl = document.getString("profileImageUrl");
                                    currentCategories = (ArrayList<String>) document.get("favorite categories");

                                    String fullName = firstName + " " + familyName;
                                    emailTextView.setText(email);
                                    fullNameTextView.setText(fullName);
                                    searchAddressText.setText(geoPointToString(address));
                                    typeOfUserTextView.setText(userType);
                                    if (picUrl != null && !picUrl.isEmpty()) {
                                        Glide.with(UpdateUserDetailsActivity.this)
                                                .load(picUrl)
                                                .error(R.drawable.ic_profile)
                                                .into(profileImageView);
                                    }
                                    progressDialog.dismiss();
                                    // Log or use the retrieved information
                                    Log.d("firebase", "Name: " + firstName + ", Family Name: " + familyName);
                                }
                                else {
                                    progressDialog.dismiss();
                                    Log.d("firebase", "No such document");
                                }
                            }
                            else {
                                progressDialog.dismiss();
                                Log.d("firebase", "get failed with ", task.getException());
                            }
                        }
                    });
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please Log in or Sign up")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent toy = new Intent(UpdateUserDetailsActivity.this, MainActivity.class);
                            startActivity(toy);
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

    }

    private String geoPointToString(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return "";
        }
        double lat = geoPoint.getLatitude();
        double lon = geoPoint.getLongitude();
        String address = "";
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                address = addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    private void updateDB(Map<String, Object> userDetails){
        // Update Firestore with the new details
        db.collection("users").document(email)
                .update(userDetails)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("EditUserDetails", "User details updated.");
                        // Navigate to another activity or perform further actions upon success
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("EditUserDetails", "Error updating user details", e);
                        progressDialog.dismiss();
                    }
                });

    }

    private void uploadImage(Map<String, Object> userDetails) {

        if (imageUri != null) {
            // Delete previous profile picture if exists
            if (picUrl != null && !picUrl.isEmpty()) {
                deleteUserProfileImage(picUrl);
            }

            Log.d("UpdateUserDetailsActivity", "Uploading...");
            StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    picUrl = uri.toString();
                                    userDetails.put("profileImageUrl", picUrl);
                                    // Update Firestore with the new details
                                    updateDB(userDetails);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("UpdateUserDetailsActivity", "Upload failed: " + e.getMessage());
                        }
                    });
        } else {
            Log.d("UpdateUserDetailsActivity", "No file selected");
        }
    }

    public void deleteAccount() {
        Log.d("UpdateUserDetailsActivity", "deleteAccount called.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account?")
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (currentUser != null) {
                            //todo progress
                            Log.d("UpdateUserDetailsActivity", "User is signed in, proceeding to reauthenticateAndDeleteUser.");
                            reauthenticateAndDeleteUser();
                        } else {
                            Log.d("UpdateUserDetailsActivity", "No user is currently signed in.");
                        }
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void reauthenticateAndDeleteUser() {
        Log.d("UpdateUserDetailsActivity", "reauthenticateAndDeleteUser called.");
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleSignInAccount != null) {
            Log.d("UpdateUserDetailsActivity", "GoogleSignInAccount found, using GoogleAuthProvider credential.");
            promptForGoogleSignIn();
        } else {
            Log.d("UpdateUserDetailsActivity", "No GoogleSignInAccount found, prompting for password.");
            promptForPassword();
        }
    }

    private void promptForGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
    }

    private void promptForPassword() {
        Log.d("UpdateUserDetailsActivity", "promptForPassword called.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authentication Required");
        builder.setMessage("Please enter your password to continue:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                Log.d("UpdateUserDetailsActivity", "Password entered: " + password);
                if (!password.isEmpty()) {
                    reauthenticateWithPassword(password);
                } else {
                    Log.d("UpdateUserDetailsActivity", "Empty password entered.");
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Log.d("UpdateUserDetailsActivity", "Password prompt cancelled.");
            }
        });

        builder.show();
    }

    private void reauthenticateWithPassword(String password) {
        Log.d("UpdateUserDetailsActivity", "reauthenticateWithPassword called.");
        String email = currentUser.getEmail();
        if (email != null) {
            Log.d("UpdateUserDetailsActivity", "User email: " + email);
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            reauthenticateUser(currentUser, credential);
        } else {
            Log.d("UpdateUserDetailsActivity", "No email associated with the current user.");
        }
    }

    private void reauthenticateUser(FirebaseUser user, AuthCredential credential) {
        Log.d("UpdateUserDetailsActivity", "reauthenticateUser called.");
        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("UpdateUserDetailsActivity", "Re-authentication successful.");
                    performDeleteTasks();
                } else {
                    Log.d("UpdateUserDetailsActivity", "Re-authentication failed.", task.getException());
                }
            }
        });
    }

    private void performDeleteTasks() {
        Log.d("UpdateUserDetailsActivity", "performDeleteTasks called.");
        progressDialog.show();
        Task<Void> deleteProfileImageTask = Tasks.forResult(null);
        if (picUrl != null && !picUrl.isEmpty()) {
            Log.d("UpdateUserDetailsActivity", "Adding deleteUserProfileImage task.");
            deleteProfileImageTask = deleteUserProfileImage(picUrl);
        }

        deleteProfileImageTask
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UpdateUserDetailsActivity", "UserProfileImage deleted successfully.");
                        return deleteUserFromAllOrders(db, email);
                    } else {
                        Log.d("UpdateUserDetailsActivity", "Failed to delete UserProfileImage.", task.getException());
                        progressDialog.dismiss();
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UpdateUserDetailsActivity", "User removed from all orders successfully.");
                        return deleteUserNotifications(db, email);
                    } else {
                        Log.d("UpdateUserDetailsActivity", "Failed to remove user from all orders.", task.getException());
                        progressDialog.dismiss();
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UpdateUserDetailsActivity", "User notifications deleted successfully.");
                        return deleteUserDetails(db, email);
                    } else {
                        Log.d("UpdateUserDetailsActivity", "Failed to delete user notifications.", task.getException());
                        progressDialog.dismiss();
                        throw task.getException();
                    }
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UpdateUserDetailsActivity", "User details deleted successfully.");
                        return deleteUser(currentUser);
                    } else {
                        Log.d("UpdateUserDetailsActivity", "Failed to delete user details.", task.getException());
                        progressDialog.dismiss();
                        throw task.getException();
                    }
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        Log.d("UpdateUserDetailsActivity", "All deletion tasks completed successfully.");
                        progressDialog.dismiss();
                        menuUtils.logOut();
                        Log.d("UpdateUserDetailsActivity", "Account and associated data deleted successfully.");
                    } else {
                        Log.d("UpdateUserDetailsActivity", "Error in deletion tasks.", task.getException());
                    }
                });
    }


    private Task<Void> deleteUserProfileImage(String imageUrl) {
        Log.d("UpdateUserDetailsActivity", "deleteUserProfileImage called with URL: " + imageUrl);
        StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
        return photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("UpdateUserDetailsActivity", "Profile image deleted.");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d("UpdateUserDetailsActivity", "Failed to delete profile image", exception);
                progressDialog.dismiss();
            }
        });
    }

    private Task<Void> deleteUserFromAllOrders(@NonNull FirebaseFirestore db, String emailToRemove) {
        Log.d("UpdateUserDetailsActivity", "deleteUserFromAllOrders called with email: " + emailToRemove);
        return db.collection("orders").get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                List<Task<Void>> updateTasks = new ArrayList<>();
                for (DocumentSnapshot document : documents) {
                    List<String> mails = (List<String>) document.get("listPeopleInOrder");
                    List<String> waitingList = (List<String>) document.get("waitingList");
                    String userEmail = document.getString("user_email");
                    Long numberOfPeopleInOrder = document.getLong("NumberOfPeopleInOrder");
                    Log.d("UpdateUserDetailsActivity", "Processing order document ID: " + document.getId());

                    // Check and remove user's items from the cart
                    List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get("cartItems");
                    if (cartItems != null) {
                        List<Map<String, Object>> itemsToRemove = new ArrayList<>();
                        for (Map<String, Object> item : cartItems) {
                            String itemUserEmail = (String) item.get("userEmail");
                            if (emailToRemove.equals(itemUserEmail)) {
                                itemsToRemove.add(item);
                            }
                        }

                        if (!itemsToRemove.isEmpty()) {
                            cartItems.removeAll(itemsToRemove);
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("cartItems", cartItems);
                            updateTasks.add(document.getReference().update(updates));
                        }
                    }

                    // Delete chat collection if globalUserType is "Supplier" and user email matches
                    if (globalUserType.equals("Supplier") && userEmail != null && userEmail.equals(emailToRemove)) {
                        Log.d("UpdateUserDetailsActivity", "Deleting chat collection for order.");
                        updateTasks.add(deleteOrderChat(document.getReference()));
                    }

                    // Delete order document if globalUserType is "Supplier" and user email matches
                    if (globalUserType.equals("Supplier") && userEmail != null && userEmail.equals(emailToRemove)) {
                        Log.d("UpdateUserDetailsActivity", "Deleting order document for supplier.");
                        updateTasks.add(document.getReference().delete());
                    } else {
                        if (mails != null && mails.contains(emailToRemove)) {
                            if (mails.size() == 1) {
                                Log.d("UpdateUserDetailsActivity", "Email is the only one in the list, deleting the whole order.");
                                updateTasks.add(deleteOrderChat(document.getReference()));
                                updateTasks.add(document.getReference().delete());
                            } else {
                                Log.d("UpdateUserDetailsActivity", "Email exists, removing it from the order.");
                                mails.remove(emailToRemove);
                                long updatedNumberOfPeopleInOrder = numberOfPeopleInOrder != null ? numberOfPeopleInOrder - 1 : 0;
                                if (emailToRemove.equals(userEmail)) {
                                    userEmail = mails.get(0); // Assuming we set it to the next email in the list
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("listPeopleInOrder", mails);
                                updates.put("NumberOfPeopleInOrder", updatedNumberOfPeopleInOrder);
                                updates.put("user_email", userEmail);

                                updateTasks.add(document.getReference().update(updates));
                            }
                        }
                        if (waitingList != null && waitingList.contains(emailToRemove)) {
                            Log.d("UpdateUserDetailsActivity", "Email exists, removing it from the waiting list.");
                            waitingList.remove(emailToRemove);
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("waitingList", waitingList);
                            updateTasks.add(document.getReference().update(updates));
                        }
                    }
                }
                return Tasks.whenAll(updateTasks);
            } else {
                Log.d("UpdateUserDetailsActivity", "Error getting orders collection.", task.getException());
                progressDialog.dismiss();
                throw task.getException();
            }
        });
    }

    private Task<Void> deleteOrderChat(DocumentReference orderRef) {
        Log.d("UpdateUserDetailsActivity", "deleteOrderChat called for order: " + orderRef.getId());
        return orderRef.collection("chat").get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (DocumentSnapshot document : documents) {
                    Log.d("UpdateUserDetailsActivity", "Deleting chat document ID: " + document.getId());
                    deleteTasks.add(document.getReference().delete());
                }
                return Tasks.whenAll(deleteTasks);
            } else {
                Log.d("UpdateUserDetailsActivity", "Error getting chat collection.", task.getException());
                progressDialog.dismiss();
                throw task.getException();
            }
        });
    }

    private Task<Void> deleteUserNotifications(@NonNull FirebaseFirestore db, String emailToRemove) {
        Log.d("UpdateUserDetailsActivity", "deleteUserNotifications called with email: " + emailToRemove);
        return db.collection("users").document(emailToRemove).collection("notifications").get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (DocumentSnapshot document : documents) {
                    Log.d("UpdateUserDetailsActivity", "Deleting notification document ID: " + document.getId());
                    deleteTasks.add(document.getReference().delete());
                }
                return Tasks.whenAll(deleteTasks);
            } else {
                Log.d("UpdateUserDetailsActivity", "Error getting notifications collection.", task.getException());
                progressDialog.dismiss();
                throw task.getException();
            }
        });
    }

    private Task<Void> deleteUserDetails(@NonNull FirebaseFirestore db, String emailToRemove) {
        Log.d("UpdateUserDetailsActivity", "deleteUserDetails called with email: " + emailToRemove);
        return db.collection("users").document(emailToRemove).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("UpdateUserDetailsActivity", "User document successfully deleted.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("UpdateUserDetailsActivity", "Failed to remove user details", e);
                        progressDialog.dismiss();
                    }
                });
    }

    private Task<Void> deleteUser(FirebaseUser curr) {
        Log.d("UpdateUserDetailsActivity", "deleteUser called.");
        return curr.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("UpdateUserDetailsActivity", "User account deleted.");
                progressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("UpdateUserDetailsActivity", "Error deleting user account.", e);
                progressDialog.dismiss();
            }
        });
    }

    public void changePassword() {
        Intent toy = new Intent(UpdateUserDetailsActivity.this, ChangePasswordActivity.class);
        toy.putExtra("userType", globalUserType);
        startActivity(toy);
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
