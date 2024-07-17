package com.elisham.coshop;

import android.Manifest;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> locationWindowLauncher;
    private TextView searchAddressText;
    private ImageButton searchAddressButton;
    private ImageButton editAddressButton;
    private String lastAddress;
    private double lastLatitude;
    private double lastLongitude;
    private int lastDistance;
    private TextView fullNameTextView, emailEditText;
    private String email, firstName, familyName, picUrl;
    private String newFirstName, newFamilyName, googleProfilePicUrl;
    private boolean changeName = false;
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
    boolean isGoogleSignUp, changePic = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profileImageView = findViewById(R.id.profileImageView);

        emailEditText = findViewById(R.id.emailText);
        fullNameTextView = findViewById(R.id.fullName);
        choiceRadioGroup = findViewById(R.id.choiceLinearLayout);

        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);

        Intent intent = getIntent();
        if (intent != null) {
            isGoogleSignUp = intent.getBooleanExtra("google_sign_up", false);
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");
            selectedCategories = intent.getStringArrayListExtra("selectedCategories");
        }

        String fullName = firstName + " " + familyName;
        emailEditText.setText(email);
        fullNameTextView.setText(fullName);

        if (isGoogleSignUp)
        {
            Toast.makeText(UserDetailsActivity.this, "google signup", Toast.LENGTH_SHORT).show();
            // Inside onCreate method
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                googleProfilePicUrl = account.getPhotoUrl().toString();
//                downloadAndUploadGoogleProfilePic(googleProfilePicUrl);
                Glide.with(this)
                        .load(googleProfilePicUrl)
                        .into(profileImageView);
            }
        }


        fullNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNameEditDialog();
            }
        });

        CardView profilePic = findViewById(R.id.profilePic);
        profilePic.setOnClickListener(new View.OnClickListener() {
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

        LinearLayout searchRow = findViewById(R.id.search_row);
        searchRow.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("hideDistanceLayout", true); // העברת פרמטר להסתרת ה-KM
            if (lastAddress != null && !lastAddress.isEmpty()) {
                intent.putExtra("address", lastAddress);
            }
            locationWindowLauncher.launch(intent);
        });

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

        editAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailsActivity.this, LocationWindow.class);
            intent.putExtra("hideDistanceLayout", true); // העברת פרמטר להסתרת ה-KM
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


    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
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
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
//                    uploadImage();
                    changePic = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUri(bitmap);
                try {
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
//                    uploadImage();
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
        // Use Glide to download the image
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
                        // Handle cleanup if necessary
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


    public void editUserDetails() {
        Map<String, Object> userDetails = new HashMap<>();

        choiceRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = findViewById(checkedId);
                if (checkedRadioButton != null) {
                    Toast.makeText(UserDetailsActivity.this, "Selected: " + checkedRadioButton.getText(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        int selectedId = choiceRadioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            // No radio button is selected, show a toast message and return
            showAlertDialog("Please select a user type");
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedId);
        userDetails.put("blocked", false);
        userDetails.put("ratings", new ArrayList<Map<String, Object>>());
        userDetails.put("email", email);
        userDetails.put("favorite categories", selectedCategories);
        userDetails.put("first name", firstName);
        userDetails.put("family name", familyName);
        // Create a GeoPoint object from the latitude and longitude
        if (lastLatitude != 0 && lastLongitude != 0) {
            address = new GeoPoint(lastLatitude, lastLongitude);
            userDetails.put("address", address);
        }

        if (address == null) {
            showAlertDialog("Please enter your address");
            return;
        }

        if (selectedRadioButton != null) {
            String selectedChoice = selectedRadioButton.getText().toString();
            userDetails.put("type of user", selectedChoice);
        } else {
            showAlertDialog("No choice selected");
            Toast.makeText(UserDetailsActivity.this, "No choice selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGoogleSignUp && !changePic) {
            Toast.makeText(UserDetailsActivity.this, "1", Toast.LENGTH_SHORT).show();

            // If Google Sign Up, download the Google profile picture and upload it to Firebase
            downloadAndUploadGoogleProfilePic(googleProfilePicUrl, new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String picUrl) {
                    userDetails.put("profileImageUrl", picUrl);
                    saveUserDetailsToFirestore(userDetails);
                }
            }, e -> {
                Toast.makeText(UserDetailsActivity.this, "2: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // If not Google Sign Up, upload the selected image
            if (imageUri != null) {
                StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
                fileReference.putFile(imageUri)
                        .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String picUrl = uri.toString();
                                    userDetails.put("profileImageUrl", picUrl);
                                    saveUserDetailsToFirestore(userDetails);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(UserDetailsActivity.this, "3 Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }))
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserDetailsActivity.this, "4 Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(UserDetailsActivity.this, "5 No file selected", Toast.LENGTH_SHORT).show();
                saveUserDetailsToFirestore(userDetails);
            }
        }
    }

    private void saveUserDetailsToFirestore(Map<String, Object> userDetails) {
        db.collection("users").document(email)
                .set(userDetails)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(UserDetailsActivity.this, "User details updated successfully", Toast.LENGTH_SHORT).show();
                    home();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding user details: " + e.getMessage());
                    showAlertDialog("Error adding user details: " + e.getMessage());
                });
    }


    private void showNameEditDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);
        EditText firstNameEditText = dialogView.findViewById(R.id.firstNameEditText);
        EditText familyNameEditText = dialogView.findViewById(R.id.familyNameEditText);
        ImageView clearFirstNameIcon = dialogView.findViewById(R.id.clearFirstNameIcon); // Icon view
        ImageView clearFamilyNameIcon = dialogView.findViewById(R.id.clearFamilyNameIcon); // Icon view

        // Set current values
        firstNameEditText.setText(firstName);
        familyNameEditText.setText(familyName);

        // Set onClickListener for the  first name clear icon
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

        builder.setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newFirstName = firstNameEditText.getText().toString().trim();
                        firstName = newFirstName;
                        newFamilyName = familyNameEditText.getText().toString().trim();
                        familyName = newFamilyName;

                        // Update UI with new names
                        String fullName = newFirstName + " " + newFamilyName;
                        fullNameTextView.setText(fullName);
                        changeName = true;

                        // Dismiss dialog
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showImageSourceDialog(String profileImageUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        // Create a list of options
        List<CharSequence> options = new ArrayList<>();
        if (profileImageUrl != null) {
            options.add("View Photo");
        }
        options.add("Take Photo");
        options.add("Choose from Gallery");

        CharSequence[] items = options.toArray(new CharSequence[0]);
        builder.setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                if (profileImageUrl != null) {
                                    // Handle viewing the photo
                                    viewPhoto(profileImageUrl);
                                } else {
                                    checkCameraPermissionAndTakePhoto();
                                }
                                break;
                            case 1:
                                checkCameraPermissionAndTakePhoto();
                                break;
                            case 2:
                                openFileChooser();
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

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        }
    }

    private void showAlertDialog(String message) {
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

    public void home() {
        // Set the result to OK to indicate success
        setResult(RESULT_OK);

        // Create an intent to go to HomePageActivity
        Intent homeIntent = new Intent(UserDetailsActivity.this, HomePageActivity.class);

        // Clear the activity stack and start HomePageActivity as a new task
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(homeIntent);

        // Finish UserDetailsActivity
        finish();
    }


    public void deleteAccount(View v) {
        MenuUtils logout = new MenuUtils(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete your account?")
                .setCancelable(true)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (currentUser != null) {
                            currentUser.delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            showAlertDialog("Account successfully deleted! 1");
                                            Toast.makeText(UserDetailsActivity.this, "Account successfully deleted! 111", Toast.LENGTH_SHORT).show();
                                            Log.d("MainActivity", "User account deleted.");
                                            logout.logOut();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(UserDetailsActivity.this, "Error deleting user" + e, Toast.LENGTH_SHORT).show();
                                            Log.e("MainActivity", "Error deleting user", e);
                                        }
                                    });
//
                        } else {
                            Log.d("MainActivity", "No user is currently signed in.");
                        }
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void changePassword(View v) {
        Intent toy = new Intent(UserDetailsActivity.this, ChangePasswordActivity.class);
        startActivity(toy);
    }
}