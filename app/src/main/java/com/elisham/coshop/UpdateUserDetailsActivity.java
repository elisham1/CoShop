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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private MenuUtils menuUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_user_details);
        menuUtils = new MenuUtils(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        emailTextView = findViewById(R.id.emailText);
        fullNameTextView = findViewById(R.id.fullName);
//        addressEditText = findViewById(R.id.addressText);
        typeOfUserTextView = findViewById(R.id.type_of_user);
        profileImageView = findViewById(R.id.profileImage);

        searchAddressText = findViewById(R.id.search_address_text);
        searchAddressButton = findViewById(R.id.search_address_button);
        editAddressButton = findViewById(R.id.edit_address_button);

        getUserInformation();
        showLocationWindow();

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
                showImageSourceDialog();
            }
        });

    }

    private void showLocationWindow() {
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

        LinearLayout searchRow = findViewById(R.id.search_row);
        searchRow.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDetailsActivity.this, LocationWindow.class);
            if (lastAddress != null && !lastAddress.isEmpty() && lastDistance > 0) {
                intent.putExtra("address", lastAddress);
                intent.putExtra("distance", lastDistance);
            }
            locationWindowLauncher.launch(intent);
        });

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
            public void afterTextChanged(Editable s) {
                changeLocation = true;
            }
        });

        editAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(UpdateUserDetailsActivity.this, LocationWindow.class);
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

    private void showImageSourceDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                checkCameraPermissionAndTakePhoto();
                                break;
                            case 1:
                                openFileChooser();
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, TAKE_PHOTO_REQUEST);
        }
        else {
            takePhoto();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
                    uploadImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUri(bitmap);
                try {
                    bitmap = rotateImageIfRequired(bitmap, imageUri);
                    profileImageView.setImageBitmap(bitmap);
                    uploadImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadImage() {

        if (imageUri != null) {
            // Delete previous profile picture if exists
            if (picUrl != null && !picUrl.isEmpty()) {
                deleteUserProfileImage(picUrl);
            }

            StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    picUrl = uri.toString();
                                    changePic = true;
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UpdateUserDetailsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
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
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }


    private void showNameEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void getUserInformation() {

        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
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

                                    String fullName = firstName + " " + familyName;
                                    String typeText = "User Type: " + userType;
                                    emailTextView.setText(email);
                                    fullNameTextView.setText(fullName);
                                    searchAddressText.setText(geoPointToString(address));
                                    typeOfUserTextView.setText(typeText);
                                    if (picUrl != null && !picUrl.isEmpty()) {
                                        Glide.with(UpdateUserDetailsActivity.this)
                                                .load(picUrl)
                                                .into(profileImageView);
                                    }
                                    // Log or use the retrieved information
                                    Log.d("firebase", "Name: " + firstName + ", Family Name: " + familyName);
                                }
                                else {
                                    Log.d("firebase", "No such document");
                                }
                            }
                            else {
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

        return String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", lat, lon);
    }


    public void editUserDetails(View view) {
        Map<String, Object> userDetails = new HashMap<>();
        //check if change name is true and update map based on this.
        if (changeName)
        {
            userDetails.put("first name", firstName);
            userDetails.put("family name", familyName);
        }

        //check if change pic is true and update based on this.
        if (changePic) {
            userDetails.put("profileImageUrl", picUrl);
        }

        // Update Firestore with the new details
        db.collection("users").document(email)
                .update(userDetails)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(UpdateUserDetailsActivity.this, "User details updated successfully", Toast.LENGTH_SHORT).show();
                        Log.d("EditUserDetails", "User details updated.");
                        // Optionally, navigate to another activity or perform further actions upon success
                        Intent toy = new Intent(UpdateUserDetailsActivity.this, HomePageActivity.class);
                        startActivity(toy);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UpdateUserDetailsActivity.this, "Failed to update user details", Toast.LENGTH_SHORT).show();
                        Log.e("EditUserDetails", "Error updating user details", e);
                    }
                });

    }

    public void deleteAccount(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete your account?")
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(currentUser != null)
                        {
                            if (picUrl != null && !picUrl.isEmpty()) {
                                deleteUserProfileImage(picUrl);
                            }
                            deleteUserFromAllOrders(db, email);
                            deleteUserDetails(db, email);
                            deleteUser(currentUser);
                            dialog.dismiss();
                        }
                        else {
                            Toast.makeText(UpdateUserDetailsActivity.this,
                                    "no user is signed in", Toast.LENGTH_SHORT).show();
                            Log.d("UpdateUserActivity", "No user is currently signed in.");
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void deleteUserProfileImage(String imageUrl) {
        StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
        photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(UpdateUserDetailsActivity.this,
                        "Profile image deleted successfully", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Profile image deleted.");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(UpdateUserDetailsActivity.this,
                        "Failed to delete profile image", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Error deleting profile image", exception);
            }
        });
    }

    private void deleteUserFromAllOrders(@NonNull FirebaseFirestore db, String emailToRemove) {

        db.collection("orders").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            for (DocumentSnapshot document : documents) {
                                List<String> mails = (List<String>) document.get("listPeopleInOrder");
                                if (mails != null && mails.contains(emailToRemove)) {
                                    // Mail exists, remove it
                                    mails.remove(emailToRemove);
                                    document.getReference().update("listPeopleInOrder", mails)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(UpdateUserDetailsActivity.this,
                                                            "user removed successfully from all orders", Toast.LENGTH_SHORT).show();
                                                    Log.d("MainActivity", "Mail removed successfully from " + document.getId());
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(UpdateUserDetailsActivity.this,
                                                            "failed to remove user from all orders", Toast.LENGTH_SHORT).show();
                                                    Log.e("MainActivity", "Error updating document", e);
                                                }
                                            });
                                    Log.d("MainActivity", document.getId() + " => " + document.getData());
                                }
                            }
                        }

                        else {
                            Log.w("MainActivity", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void deleteUserDetails(@NonNull FirebaseFirestore db, String emailToRemove) {
        //delete all user connected details
        db.collection("users").document(emailToRemove).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(UpdateUserDetailsActivity.this,
                                "user details removed successfully", Toast.LENGTH_SHORT).show();
                        Log.d("MainActivity", "Document successfully deleted!");
                        Intent toy = new Intent(UpdateUserDetailsActivity.this, MainActivity.class);
                        startActivity(toy);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UpdateUserDetailsActivity.this,
                                "failed to remove user details", Toast.LENGTH_SHORT).show();
                        Log.w("MainActivity", "Error deleting document", e);
                    }
                });
    }

    private void deleteUser(FirebaseUser curr){
        currentUser.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(UpdateUserDetailsActivity.this,
                                "Account successfully deleted! 111", Toast.LENGTH_SHORT).show();
                        Log.d("MainActivity", "User account deleted.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("MainActivity", "Error deleting user", e);
                    }
                });
    }

    public void changePassword(View v) {
        Intent toy = new Intent(UpdateUserDetailsActivity.this, ChangePasswordActivity.class);
        startActivity(toy);
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
