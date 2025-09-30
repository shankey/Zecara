package com.app.zecara.util;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Html5ProjectManager {
    private static final String TAG = "Html5ProjectManager";
    private static final String PROJECTS_DIR = "html5_projects";
    
    private Context context;
    private FirebaseStorage storage;

    public interface ProjectDownloadCallback {
        void onSuccess(String localProjectPath);
        void onProgress(int percentage);
        void onError(String error);
    }

    public Html5ProjectManager(Context context) {
        this.context = context;
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Download and extract HTML5 project from Firebase Storage
     * @param storageUrl Firebase Storage download URL
     * @param projectId Unique project identifier
     * @param callback Download progress callback
     */
    public void downloadProject(String storageUrl, String projectId, ProjectDownloadCallback callback) {
        Log.d(TAG, "Starting download for project: " + projectId);
        
        // Check if project already exists locally
        File projectDir = new File(context.getFilesDir(), PROJECTS_DIR + "/" + projectId);
        if (projectDir.exists() && projectDir.isDirectory()) {
            File indexFile = new File(projectDir, "index.html");
            if (indexFile.exists()) {
                Log.d(TAG, "Project already exists locally: " + projectId);
                callback.onSuccess(projectDir.getAbsolutePath());
                return;
            }
        }

        // Create storage reference from URL
        StorageReference ref = storage.getReferenceFromUrl(storageUrl);
        
        // Create temporary file for download
        File tempZip = new File(context.getCacheDir(), projectId + ".zip");
        
        ref.getFile(tempZip)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    callback.onProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "ZIP downloaded successfully for: " + projectId);
                    // Extract ZIP file
                    extractProject(tempZip, projectId, callback);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to download project: " + projectId, exception);
                    callback.onError("Download failed: " + exception.getMessage());
                    // Clean up temp file
                    if (tempZip.exists()) {
                        tempZip.delete();
                    }
                });
    }

    /**
     * Extract ZIP file to internal storage
     */
    private void extractProject(File zipFile, String projectId, ProjectDownloadCallback callback) {
        try {
            File projectDir = new File(context.getFilesDir(), PROJECTS_DIR + "/" + projectId);
            
            // Create project directory
            if (!projectDir.exists()) {
                projectDir.mkdirs();
            }

            // Extract ZIP
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                
                // Security check: prevent directory traversal
                if (fileName.contains("..") || fileName.startsWith("/")) {
                    Log.w(TAG, "Skipping suspicious entry: " + fileName);
                    continue;
                }

                File outputFile = new File(projectDir, fileName);
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    // Create parent directories
                    outputFile.getParentFile().mkdirs();
                    
                    // Write file content
                    FileOutputStream fos = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    fos.close();
                }
                zis.closeEntry();
            }

            zis.close();
            fis.close();

            // Clean up ZIP file
            zipFile.delete();

            // Verify index.html exists
            File indexFile = new File(projectDir, "index.html");
            if (indexFile.exists()) {
                Log.d(TAG, "Project extracted successfully: " + projectId);
                callback.onSuccess(projectDir.getAbsolutePath());
            } else {
                Log.e(TAG, "index.html not found in extracted project: " + projectId);
                callback.onError("Invalid project: index.html not found");
                // Clean up extracted files
                deleteProjectDir(projectDir);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to extract project: " + projectId, e);
            callback.onError("Extraction failed: " + e.getMessage());
            // Clean up
            zipFile.delete();
        }
    }

    /**
     * Get local path for a project (if it exists)
     */
    public String getLocalProjectPath(String projectId) {
        File projectDir = new File(context.getFilesDir(), PROJECTS_DIR + "/" + projectId);
        File indexFile = new File(projectDir, "index.html");
        
        if (indexFile.exists()) {
            return "file://" + indexFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * Clear cached projects to free up space
     */
    public void clearCache() {
        File projectsDir = new File(context.getFilesDir(), PROJECTS_DIR);
        if (projectsDir.exists()) {
            deleteProjectDir(projectsDir);
            Log.d(TAG, "Cleared HTML5 projects cache");
        }
    }

    /**
     * Delete specific project
     */
    public void deleteProject(String projectId) {
        File projectDir = new File(context.getFilesDir(), PROJECTS_DIR + "/" + projectId);
        if (projectDir.exists()) {
            deleteProjectDir(projectDir);
            Log.d(TAG, "Deleted project: " + projectId);
        }
    }

    private void deleteProjectDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteProjectDir(child);
                }
            }
        }
        dir.delete();
    }

    /**
     * Get total size of cached projects in MB
     */
    public long getCacheSizeMB() {
        File projectsDir = new File(context.getFilesDir(), PROJECTS_DIR);
        return getDirSizeBytes(projectsDir) / (1024 * 1024);
    }

    private long getDirSizeBytes(File dir) {
        long size = 0;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirSizeBytes(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
}
