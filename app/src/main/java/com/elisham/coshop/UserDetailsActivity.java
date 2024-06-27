package com.elisham.coshop;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    private EditText addressEditText, emailEditText;
    private TextView fullNameTextView;
    private String email, firstName, familyName, picUrl;
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
        addressEditText = findViewById(R.id.addressText);
        choiceRadioGroup = findViewById(R.id.choiceLinearLayout);

        CardView profilePic = findViewById(R.id.profilePic);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            email = intent.getStringExtra("email");
            firstName = intent.getStringExtra("firstName");
            familyName = intent.getStringExtra("familyName");
            selectedCategories = intent.getStringArrayListExtra("selectedCategories");
        }

        String fullName = firstName + " " + familyName;
        emailEditText.setText(email);
        fullNameTextView.setText(fullName);
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

    private Uri getImageUri(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
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

    private void uploadImage() {
        if (imageUri != null) {
            StorageReference fileReference = storageReference.child("profile_images/" + System.currentTimeMillis() + ".jpg");
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    picUrl = uri.toString();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UserDetailsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    public void editUserDetails(View view) {
        Map<String, Object> userDetails = new HashMap<>();

//        address = addressEditText.getText().toString().trim();
//        if (address.equals("")) {
//            showAlertDialog("Please enter your address");
//            return;
//        }

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

        userDetails.put("email", email);
        userDetails.put("favorite categories", selectedCategories);
        userDetails.put("first name", firstName);
        userDetails.put("family name", familyName);
        userDetails.put("address", address);
        userDetails.put("profileImageUrl", picUrl);
        if (selectedRadioButton != null) {
            String selectedChoice = selectedRadioButton.getText().toString();
            userDetails.put("type of user", selectedChoice);
        } else {
            showAlertDialog("No choice selected");
            Toast.makeText(UserDetailsActivity.this, "No choice selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add a new document with a generated ID
        db.collection("users").document(email)
                .set(userDetails)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(UserDetailsActivity.this, "user details updated successfully", Toast.LENGTH_SHORT).show();
                    home();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error adding user details: " + e.getMessage());
                    showAlertDialog("Error adding user details: " + e.getMessage());
                });
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
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
                                            Intent toy = new Intent(UserDetailsActivity.this, MainActivity.class);
                                            startActivity(toy);
                                            finish();
                                            Log.d("MainActivity", "User account deleted.");
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