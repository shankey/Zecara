package com.app.zecara;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.zecara.adapter.FeedAdapter;
import com.app.zecara.model.ContentItem;
import com.app.zecara.service.ContentService;
import com.app.zecara.util.Html5ProjectManager;
import com.app.zecara.util.WebViewPool;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements FeedAdapter.OnFeedItemClickListener {
    
    private static final String TAG = "HomeActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ContentService contentService;
    private Html5ProjectManager projectManager;
    private WebViewPool webViewPool;
    
    private RecyclerView feedRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FeedAdapter feedAdapter;
    private List<ContentItem> feedItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        contentService = new ContentService();
        projectManager = new Html5ProjectManager(this);
        webViewPool = WebViewPool.getInstance(this);
        
        // Check if user is logged in first
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Set up feed
        setupFeed();
        
        // Load content
        loadFeedContent();
    }
    
    private void setupFeed() {
        feedRecyclerView = findViewById(R.id.feedRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Initialize feed items
        feedItems = new ArrayList<>();
        
        // Set up RecyclerView
        feedAdapter = new FeedAdapter(feedItems);
        feedAdapter.setOnFeedItemClickListener(this);
        feedAdapter.setProjectManager(projectManager); // Enable dynamic project loading
        feedAdapter.setWebViewPool(webViewPool); // Enable WebView pooling for better performance
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedRecyclerView.setAdapter(feedAdapter);
        
        // Set up pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadFeedContent);
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }
    
    private void loadFeedContent() {
        Log.d(TAG, "Loading feed content...");
        swipeRefreshLayout.setRefreshing(true);
        
        // Load HTML5 content from Firestore
        contentService.getContentByType("HTML5", new ContentService.ContentCallback() {
            @Override
            public void onSuccess(List<ContentItem> items) {
                Log.d(TAG, "Loaded " + items.size() + " feed items");
                feedItems.clear();
                feedItems.addAll(items);
                
                // Add sample content if none exists
                if (feedItems.isEmpty()) {
                    addSampleContent();
                }
                
                runOnUiThread(() -> {
                    feedAdapter.updateItems(feedItems);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading feed content: " + error);
                runOnUiThread(() -> {
                    // Add sample content on error too
                    addSampleContent();
                    feedAdapter.updateItems(feedItems);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(HomeActivity.this, "Using sample content", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void addSampleContent() {
        Log.d(TAG, "Adding sample HTML5 content");
        feedItems.clear();

        
        // Sample HTML5 content
        ContentItem item1 = new ContentItem();
        item1.setId("sample1");
        item1.setTitle("Welcome to Zecara!");
        item1.setDescription("Check out this beautiful HTML5 content with interactive elements!");
        item1.setAuthorName("Zecara Team");
        item1.setContentType("HTML5");
        item1.setHtmlContent(
            "<div style='text-align: center; padding: 20px;'>" +
            "<h2 style='color: #2196F3;'>🎉 Welcome to Zecara Feed!</h2>" +
            "<p>This is an <strong>HTML5 content tile</strong> in your Instagram-like feed!</p>" +
            "<div style='background: linear-gradient(45deg, #FF6B6B, #4ECDC4); padding: 15px; border-radius: 10px; margin: 10px 0;'>" +
            "<p style='color: white; margin: 0;'>✨ Beautiful gradients and styling!</p>" +
            "</div>" +
            "<button onclick='alert(\"Hello from HTML5!\")' style='background: #4CAF50; color: white; border: none; padding: 10px 20px; border-radius: 5px; cursor: pointer;'>Click Me!</button>" +
            "</div>"
        );
        
        ContentItem item2 = new ContentItem();
        item2.setId("sample2");
        item2.setTitle("Interactive Chart");
        item2.setDescription("Data visualization with CSS and HTML5 canvas elements");
        item2.setAuthorName("Data Scientist");
        item2.setContentType("HTML5");
        item2.setHtmlContent(
            "<div style='padding: 15px;'>" +
            "<h3 style='color: #FF9800;'>📊 Interactive Chart</h3>" +
            "<div style='display: flex; align-items: end; height: 100px; background: #f8f9fa; border-radius: 8px; padding: 10px;'>" +
            "<div style='width: 30px; background: #FF6B6B; margin-right: 5px; height: 60%;'></div>" +
            "<div style='width: 30px; background: #4ECDC4; margin-right: 5px; height: 80%;'></div>" +
            "<div style='width: 30px; background: #45B7D1; margin-right: 5px; height: 40%;'></div>" +
            "<div style='width: 30px; background: #96CEB4; margin-right: 5px; height: 90%;'></div>" +
            "<div style='width: 30px; background: #FFEAA7; height: 70%;'></div>" +
            "</div>" +
            "<p style='text-align: center; margin-top: 10px; color: #666;'>Sample data visualization</p>" +
            "</div>"
        );
        
        ContentItem item3 = new ContentItem();
        item3.setId("sample3");
        item3.setTitle("Image Gallery");
        item3.setDescription("Responsive image gallery with CSS Grid");
        item3.setAuthorName("UI Designer");
        item3.setContentType("HTML5");
        item3.setHtmlContent(
            "<div style='padding: 15px;'>" +
            "<h3 style='color: #E91E63;'>🖼️ Image Gallery</h3>" +
            "<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 15px 0;'>" +
            "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); height: 80px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white;'>Image 1</div>" +
            "<div style='background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); height: 80px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white;'>Image 2</div>" +
            "<div style='background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); height: 80px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white; grid-column: span 2;'>Image 3</div>" +
            "</div>" +
            "<p style='text-align: center; color: #666;'>Responsive CSS Grid Layout</p>" +
            "</div>"
        );
        
        feedItems.add(item1);
        feedItems.add(item2);
        feedItems.add(item3);
    }
    
    // FeedAdapter.OnFeedItemClickListener implementation
    @Override
    public void onShareClicked(ContentItem item, int position) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out: " + item.getTitle() + "\n" + item.getDescription());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onMoreClicked(ContentItem item, int position) {
        Toast.makeText(this, "More options: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Show options menu
    }

    @Override
    public void onContentClicked(ContentItem item, int position) {
        Log.d(TAG, "Content clicked: " + item.getTitle());
        // Launch full-screen HTML5 activity
        Intent intent = new Intent(this, FullScreenHtml5Activity.class);
        intent.putExtra(FullScreenHtml5Activity.EXTRA_CONTENT_ITEM, item);
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Sign out from Google
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google sign out completed");
            redirectToLogin();
        });
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(HomeActivity.this, com.app.zecara.ui.login.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is still logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up adapter
        if (feedAdapter != null) {
            feedAdapter.onDestroy();
        }
        // Clean up WebView pool - only clear if this is the last activity using it
        if (webViewPool != null && isFinishing()) {
            Log.d(TAG, "Activity finishing, clearing WebView pool");
            webViewPool.clearPool();
        }
    }
}