package com.elisham.coshop;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private TextView searchAddressText;
    private ImageButton searchAddressButton;
    private ImageButton editAddressButton;
    private LinearLayout searchRow;
    private String lastAddress;
    private double lastLatitude;
    private double lastLongitude;
    private TextView fullNameTextView;
    private String email, firstName, familyName, picUrl;
    private String newFirstName, newFamilyName, googleProfilePicUrl;
    private GeoPoint address;
    private ArrayList<String> selectedCategories;
    private RadioGroup choiceRadioGroup;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;
    private Uri imageUri;
    private ImageView profileImageView;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private boolean isGoogleSignUp, changePic = false;
    private String userType;
    ProgressDialog progressDialog;

    // Error views
    private TextView fullNameError, emailError, addressError, userTypeError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            isGoogleSignUp = intent.getBooleanExtra("google_sign_up", false);
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");
            selectedCategories = intent.getStringArrayListExtra("selectedCategories");
            userType = intent.getStringExtra("userType");
        }
        if (userType == null) {
            setTheme(R.style.ConsumerTheme);
        }
        setContentView(R.layout.activity_user_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profileImageView = findViewById(R.id.profileImageView);
        TextView emailEditText = findViewById(R.id.emailText);
        fullNameTextView = findViewById(R.id.fullName);
        choiceRadioGroup = findViewById(R.id.choiceLinearLayout);
        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);

        // Initialize error views
        fullNameError = findViewById(R.id.fullNameError);
        emailError = findViewById(R.id.emailError);
        addressError = findViewById(R.id.addressError);
        userTypeError = findViewById(R.id.userTypeError);

        String fullName = firstName + " " + familyName;
        emailEditText.setText(email);
        fullNameTextView.setText(fullName);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        if (isGoogleSignUp) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                googleProfilePicUrl = account.getPhotoUrl().toString();
                Glide.with(this)
                        .load(googleProfilePicUrl)
                        .into(profileImageView);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }

        fullNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNameEditDialog();
            }
        });

        LinearLayout profilePic = findViewById(R.id.profilePic);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog(googleProfilePicUrl);
            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog(googleProfilePicUrl);
            }
        });

        showLocationWindow();

        Button doneButton = findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editUserDetails();
            }
        });

        addTextWatchers();
    }

    private void showLocationWindow() {
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

        searchRow = findViewById(R.id.search_row);
        searchRow.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("hideDistanceLayout", true);
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
                    Intent intent = new Intent(UserDetailsActivity.this, LocationWindow.class);
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
            public void afterTextChanged(Editable s) {}
        });

        editAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("hideDistanceLayout", true);
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

    private void addTextWatchers() {
        fullNameTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    fullNameError.setVisibility(View.GONE);
                } else {
                    fullNameError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchAddressText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    addressError.setVisibility(View.GONE);
                    searchRow.setBackgroundResource(R.drawable.border);
                } else {
                    addressError.setVisibility(View.VISIBLE);
                    searchRow.setBackgroundResource(R.drawable.red_border);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO_REQUEST);
        } else {
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
                Log.d("UserDetailsActivity", "Permission denied");
            }
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                googleProfilePicUrl = imageUri.toString();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
                    changePic = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUri(bitmap);
                googleProfilePicUrl = imageUri.toString();
                try {
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
                    changePic = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

    private void downloadAndUploadGoogleProfilePic(String url, OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        uploadImageToFirebase(resource, onSuccessListener, onFailureListener);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void uploadImageToFirebase(Bitmap bitmap, OnSuccessListener<String> onSuccessListener, OnFailureListener onFailureListener) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = fileReference.putBytes(data);

        uploadTask.addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            picUrl = uri.toString();
                            if (onSuccessListener != null) {
                                onSuccessListener.onSuccess(picUrl);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (onFailureListener != null) {
                                onFailureListener.onFailure(e);
                            }
                        }))
                .addOnFailureListener(e -> {
                    if (onFailureListener != null) {
                        onFailureListener.onFailure(e);
                    }
                });
    }

    public void editUserDetails() { // Collects and updates user details
        Map<String, Object> userDetails = new HashMap<>();

        choiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = findViewById(checkedId);
                if (checkedRadioButton != null) {
                    Log.d("UserDetailsActivity", "Selected: " + checkedRadioButton.getText());
                }
            }
        });

        int selectedId = choiceRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            userTypeError.setVisibility(View.VISIBLE);
            choiceRadioGroup.setBackgroundResource(R.drawable.red_border);
            return;
        } else {
            userTypeError.setVisibility(View.GONE);
            choiceRadioGroup.setBackgroundResource(R.drawable.border);
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        userDetails.put("blocked", false);
        userDetails.put("ratings", new ArrayList<Map<String, Object>>());
        userDetails.put("email", email);
        userDetails.put("favorite categories", selectedCategories);
        userDetails.put("first name", firstName);
        userDetails.put("family name", familyName);

        if (lastLatitude != 0 && lastLongitude != 0) {
            address = new GeoPoint(lastLatitude, lastLongitude);
            userDetails.put("address", address);
        }

        if (address == null) {
            addressError.setVisibility(View.VISIBLE);
            searchRow.setBackgroundResource(R.drawable.red_border);
            return;
        } else {
            addressError.setVisibility(View.GONE);
            searchRow.setBackgroundResource(R.drawable.border);
        }

        if (selectedRadioButton != null) {
            userType = selectedRadioButton.getText().toString();
            userDetails.put("type of user", userType);
        } else {
            userTypeError.setVisibility(View.VISIBLE);
            choiceRadioGroup.setBackgroundResource(R.drawable.red_border);
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                return;
            }

            String token = task.getResult();
            Log.d("FCM", "FCM registration token: " + token);
            userDetails.put("fcmToken", token);

            if (isGoogleSignUp && !changePic) {
                progressDialog.show();
                downloadAndUploadGoogleProfilePic(googleProfilePicUrl, new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String picUrl) {
                        userDetails.put("profileImageUrl", picUrl);
                        saveUserDetailsToFirestore(userDetails);
                    }
                }, e -> {
                    Log.d("UserDetailsActivity", "Error: " + e.getMessage());
                });
            } else {
                if (imageUri != null) {
                    progressDialog.show();
                    StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
                    fileReference.putFile(imageUri)
                            .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        String picUrl = uri.toString();
                                        userDetails.put("profileImageUrl", picUrl);
                                        saveUserDetailsToFirestore(userDetails);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firestore", "Error getting download URL: " + e.getMessage());
                                    }))
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error uploading image: " + e.getMessage());
                            });
                } else {
                    progressDialog.show();
                    saveUserDetailsToFirestore(userDetails);
                }
            }
        });
    }

    private void saveUserDetailsToFirestore(Map<String, Object> userDetails) { // Saves user details to Firestore
        db.collection("users").document(email)
                .set(userDetails)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Log.d("UserDetailsActivity", "User details updated successfully");
                    home();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding user details: " + e.getMessage());
                    showAlertDialog("Error adding user details: " + e.getMessage());
                    progressDialog.dismiss();
                });
    }

    private void showNameEditDialog() { // Shows dialog for editing the user's name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText firstNameEditText = dialogView.findViewById(R.id.firstNameEditText);
        EditText familyNameEditText = dialogView.findViewById(R.id.familyNameEditText);
        ImageView clearFirstNameIcon = dialogView.findViewById(R.id.clearFirstNameIcon);
        ImageView clearFamilyNameIcon = dialogView.findViewById(R.id.clearFamilyNameIcon);
        LinearLayout firstNameLayout = dialogView.findViewById(R.id.firstNameLayout);
        TextView firstNameError = dialogView.findViewById(R.id.firstNameError);

        if (firstName != null) {
            firstNameError.setVisibility(View.GONE);
            firstNameEditText.setText(firstName);
            clearFirstNameIcon.setVisibility(View.VISIBLE);
        }
        if (familyName != null && !familyName.isEmpty()) {
            familyNameEditText.setText(familyName);
            clearFamilyNameIcon.setVisibility(View.VISIBLE);
        }

        clearFirstNameIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstNameEditText.setText(""); // Clear the text in EditText
            }
        });

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
            public void afterTextChanged(Editable s) {}
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
            public void afterTextChanged(Editable s) {}
        });

        builder.setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
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

                String fullName = newFirstName + " " + newFamilyName;
                fullNameTextView.setText(fullName);

                dialog.dismiss();
            }
        });
    }

    private void showImageSourceDialog(String profileImageUrl) { // Shows dialog for choosing the image source
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
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
                                    viewPhoto(profileImageUrl);
                                }
                                break;
                            case 3:
                                if (profileImageUrl != null) {
                                    googleProfilePicUrl = null;
                                    changePic = true;
                                    profileImageView.setImageResource(R.drawable.ic_profile);
                                }
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void viewPhoto(String url) { // Displays the photo in a dialog
        ImageDialogFragment dialogFragment = ImageDialogFragment.newInstance(url);
        dialogFragment.show(getSupportFragmentManager(), "image_dialog");
    }

    private void takePhoto() { // Opens the camera to take a photo
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        }
    }

    private void showAlertDialog(String message) { // Displays an alert dialog with a message
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void home() { // Navigates to the home screen
        setResult(RESULT_OK);

        Intent homeIntent;
        if (userType.equals("Supplier")) {
            homeIntent = new Intent(UserDetailsActivity.this, MyOrdersActivity.class);
        } else {
            homeIntent = new Intent(UserDetailsActivity.this, HomePageActivity.class);
        }

        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        homeIntent.putExtra("userType", userType);
        homeIntent.putExtra("source", "user_details");
        startActivity(homeIntent);

        finish();
    }
}
