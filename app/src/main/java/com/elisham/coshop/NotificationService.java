package com.elisham.coshop;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class NotificationService extends Service {
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        startNotificationListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNotificationListener();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startNotificationListener() {
        String userEmail = getUserEmail();
        if (userEmail != null) {
            Log.d("NotificationService", "Starting listener for user: " + userEmail);
            notificationListener = db.collection("users")
                    .document(userEmail)
                    .collection("notifications")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w("NotificationService", "Listen failed.", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                if (doc.exists() && !doc.contains("isSend")) {
                                    String title = doc.getString("title");
                                    String message = doc.getString("message");
                                    String link = doc.getString("link");
                                    String notificationId = doc.getId();

                                    Log.d("NotificationService", "Notification received: " + title + ", " + message);

                                    // בדיקה ועדכון המצב בתוך טרנזקציה
                                    updateNotificationAsSent(notificationId, success -> {
                                        if (success) {
                                            // שליחת ההודעה למשתמש
                                            sendNotification(title, message, link, notificationId);
                                        } else {
                                            Log.w("NotificationService", "Failed to mark notification as sent");
                                        }
                                    });
                                }
                            }
                        } else {
                            Log.d("NotificationService", "No notifications found.");
                        }
                    });
        } else {
            Log.w("NotificationService", "User email is null, cannot start listener");
        }
    }

    private void stopNotificationListener() {
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void updateNotificationAsSent(String notificationId, UpdateCallback callback) {
        String userEmail = getUserEmail();
        if (userEmail != null) {
            DocumentReference notificationRef = db.collection("users")
                    .document(userEmail)
                    .collection("notifications")
                    .document(notificationId);

            db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(notificationRef);
                if (snapshot.exists() && !snapshot.contains("isSend")) {
                    transaction.update(notificationRef, "isSend", true);
                    return null;
                } else {
                    throw new FirebaseFirestoreException("Notification already marked as sent or does not exist",
                            FirebaseFirestoreException.Code.ABORTED);
                }
            }).addOnSuccessListener(aVoid -> {
                Log.d("NotificationService", "Notification marked as sent");
                callback.onComplete(true);
            }).addOnFailureListener(e -> {
                Log.w("NotificationService", "Error updating notification", e);
                callback.onComplete(false);
            });
        } else {
            callback.onComplete(false);
        }
    }

    private void sendNotification(String title, String message, String link, String notificationId) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "example_channel_id")
                    .setSmallIcon(R.drawable.coshop2)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            int notificationIdInt = getUniqueNotificationId();
            notificationManager.notify(notificationIdInt, builder.build());

            Log.d("NotificationService", "Notification sent: " + title + ", " + message);
        } else {
            Log.d("NotificationService", "Notification permission is not granted");
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
                .addOnCompleteListener(new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(Task<ShortDynamicLink> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Uri shortLink = task.getResult().getShortLink();
                            callback.onLinkCreated(shortLink.toString());
                        } else {
                            Log.e("NotificationService", "Error creating short link", task.getException());
                            callback.onLinkCreated(null);
                        }
                    }
                });
    }

    private int getUniqueNotificationId() {
        return (int) System.currentTimeMillis();
    }

    interface DynamicLinkCallback {
        void onLinkCreated(String shortLink);
    }

    interface UpdateCallback {
        void onComplete(boolean success);
    }

    private String getUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
}
