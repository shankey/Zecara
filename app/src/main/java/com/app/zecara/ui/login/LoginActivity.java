package com.app.zecara.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.zecara.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Button googleSignInBtn;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        googleSignInBtn = findViewById(R.id.googleSignInBtn);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Activity Result Launcher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleSignInResult(result);
                    }
                });

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn() {
        // Log current configuration for debugging
        Log.d("LoginActivity", "Package name: " + getPackageName());
        Log.d("LoginActivity", "Web client ID: " + getString(R.string.default_web_client_id));
        
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(ActivityResult result) {
        Log.d("LoginActivity", "Result code: " + result.getResultCode() + ", Data: " + (result.getData() != null));
        
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                // Google Sign-In successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("LoginActivity", "Google Sign-In successful for: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("LoginActivity", "Google sign in failed with error code: " + e.getStatusCode(), e);
                String errorMessage = "Google Sign-In Failed";
                switch (e.getStatusCode()) {
                    case 7: // NETWORK_ERROR
                        errorMessage = "Network error - check your connection";
                        break;
                    case 10: // DEVELOPER_ERROR
                        errorMessage = "Configuration error - check SHA-1 and OAuth setup in Firebase Console";
                        break;
                    case 12500: // SIGN_IN_CANCELLED
                        errorMessage = "Sign-in was cancelled by user";
                        break;
                    case 12501: // SIGN_IN_CURRENTLY_IN_PROGRESS
                        errorMessage = "Sign-in already in progress";
                        break;
                    default:
                        errorMessage = "Google Sign-In Failed (Error Code: " + e.getStatusCode() + ")";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w("LoginActivity", "Google sign in canceled or failed. Result code: " + result.getResultCode());
            if (result.getResultCode() == RESULT_CANCELED) {
                Log.e("LoginActivity", "RESULT_CANCELED - This usually indicates a configuration issue:");
                Log.e("LoginActivity", "1. Check SHA-1 fingerprint in Firebase Console");
                Log.e("LoginActivity", "2. Verify Web OAuth client exists and is configured");
                Log.e("LoginActivity", "3. Ensure package name matches: " + getPackageName());
                Toast.makeText(this, "Configuration Error: Check Firebase Console setup", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Google Sign-In Failed (Result Code: " + result.getResultCode() + ")", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this,
                                    "Welcome " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();

                            // Navigate to HomeActivity
                            startActivity(new Intent(LoginActivity.this, com.app.zecara.HomeActivity.class));
                            finish();
                        } else {
                            Log.w("LoginActivity", "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
