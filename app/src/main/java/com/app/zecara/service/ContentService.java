package com.app.zecara.service;

import android.util.Log;

import com.app.zecara.model.ContentItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ContentService {
    private static final String TAG = "ContentService";
    private static final String CONTENT_COLLECTION = "content";
    
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    public ContentService() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public interface ContentCallback {
        void onSuccess(List<ContentItem> contentItems);
        void onError(String error);
    }

    public interface DownloadUrlCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
    }

    // Get all content items
    public void getAllContent(ContentCallback callback) {
        db.collection(CONTENT_COLLECTION)
                .orderBy("uploadTime")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<ContentItem> contentItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ContentItem item = document.toObject(ContentItem.class);
                                contentItems.add(item);
                            }
                            callback.onSuccess(contentItems);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                            callback.onError("Failed to load content: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // Get content by type (PDF or VIDEO)
    public void getContentByType(String contentType, ContentCallback callback) {
        db.collection(CONTENT_COLLECTION)
                .whereEqualTo("contentType", contentType)
                .orderBy("uploadTime")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<ContentItem> contentItems = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ContentItem item = document.toObject(ContentItem.class);
                                contentItems.add(item);
                                Log.i("pdf metadata", item.toString());
                            }
                            callback.onSuccess(contentItems);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                            callback.onError("Failed to load " + contentType + " content: " + task.getException().getMessage());
                        }
                    }
                });
    }

    // Get download URL for a storage path
    public void getDownloadUrl(String storagePath, DownloadUrlCallback callback) {
        StorageReference storageRef = storage.getReference().child(storagePath);
        storageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Log.d(TAG, "Download URL retrieved: " + uri.toString());
                    callback.onSuccess(uri.toString());
                })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Error getting download URL", exception);
                    callback.onError("Failed to get download URL: " + exception.getMessage());
                });
    }

    // Add HTML5 content to Firestore
    public interface AddContentCallback {
        void onSuccess(String documentId);
        void onError(String error);
    }

    public void addHtml5Content(ContentItem contentItem, AddContentCallback callback) {
        // Set content type to HTML5
        contentItem.setContentType("HTML5");
        
        // Add current timestamp if not set
        if (contentItem.getId() == null || contentItem.getId().isEmpty()) {
            contentItem.setId(String.valueOf(System.currentTimeMillis()));
        }
        
        Log.d(TAG, "Adding HTML5 content: " + contentItem.getTitle());
        
        db.collection(CONTENT_COLLECTION)
                .add(contentItem)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId();
                    Log.d(TAG, "HTML5 content added with ID: " + documentId);
                    callback.onSuccess(documentId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding HTML5 content", e);
                    callback.onError("Failed to add content: " + e.getMessage());
                });
    }
}
