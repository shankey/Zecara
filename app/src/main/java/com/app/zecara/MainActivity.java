package com.app.zecara;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import com.app.zecara.ui.login.LoginActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp app = FirebaseApp.initializeApp(this);
        if (app != null) {
            FirebaseOptions options = app.getOptions();

            Log.d("FirebaseCheck", "Project ID: " + options.getProjectId());
            Log.d("FirebaseCheck", "Application ID: " + options.getApplicationId());
            Log.d("FirebaseCheck", "API Key: " + options.getApiKey());
            Log.d("FirebaseCheck", "Database URL: " + options.getDatabaseUrl());
        } else {
            Log.e("FirebaseCheck", "FirebaseApp initialization failed!");
        }

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}