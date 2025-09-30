package com.app.zecara;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.zecara.model.ContentItem;
import com.app.zecara.util.Html5ProjectManager;

public class FullScreenHtml5Activity extends AppCompatActivity {
    private static final String TAG = "FullScreenHtml5Activity";
    public static final String EXTRA_CONTENT_ITEM = "content_item";
    
    private WebView webView;
    private ContentItem contentItem;
    private Html5ProjectManager projectManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_html5);

        // Get content item from intent
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CONTENT_ITEM)) {
            contentItem = (ContentItem) intent.getSerializableExtra(EXTRA_CONTENT_ITEM);
        }

        if (contentItem == null) {
            Toast.makeText(this, "Content not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize project manager
        projectManager = new Html5ProjectManager(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(contentItem.getTitle() != null ? contentItem.getTitle() : "HTML5 Content");

        // Set up WebView
        webView = findViewById(R.id.webView);
        setupWebView();
        
        // Set up back navigation
        setupBackNavigation();
        
        // Load HTML5 content
        loadContent();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        
        // Enable HTML5 features
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep navigation within the WebView
                view.loadUrl(url);
                return true;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);
            }
        });
    }

    private void loadContent() {
        // Load HTML5 content - prioritize downloadable projects
        if (contentItem.getProjectUrl() != null && !contentItem.getProjectUrl().isEmpty()) {
            loadDownloadableProject();
        } else if (contentItem.getProjectPath() != null && !contentItem.getProjectPath().isEmpty()) {
            // Load multi-page HTML5 project from assets
            String projectUrl = "file:///android_asset/html5_projects/" + contentItem.getProjectPath() + "/index.html";
            webView.loadUrl(projectUrl);
        } else if (contentItem.getHtmlContent() != null && !contentItem.getHtmlContent().isEmpty()) {
            // Load inline HTML content
            String htmlContent = wrapHtmlContent(contentItem.getHtmlContent());
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        } else {
            webView.loadData("<html><body><p>No content available</p></body></html>", "text/html", "UTF-8");
        }
    }

    private void loadDownloadableProject() {
        // Check if project is already downloaded
        String localPath = projectManager.getLocalProjectPath(contentItem.getId());
        if (localPath != null) {
            // Load from local cache
            webView.loadUrl(localPath);
            return;
        }

        // Show loading state
        showLoadingState("Downloading project...");

        // Download project from Firebase Storage
        projectManager.downloadProject(
            contentItem.getProjectUrl(), 
            contentItem.getId(), 
            new Html5ProjectManager.ProjectDownloadCallback() {
                @Override
                public void onSuccess(String localProjectPath) {
                    // Load the downloaded project
                    String indexUrl = "file://" + localProjectPath + "/index.html";
                    runOnUiThread(() -> webView.loadUrl(indexUrl));
                }

                @Override
                public void onProgress(int percentage) {
                    runOnUiThread(() -> 
                        showLoadingState("Downloading... " + percentage + "%")
                    );
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError("Download failed: " + error));
                }
            }
        );
    }

    private void showLoadingState(String message) {
        String loadingHtml = "<html><body style='text-align: center; padding: 100px; font-family: Arial;'>" +
            "<div style='color: #666; font-size: 18px;'>" +
            "<div style='font-size: 48px; margin-bottom: 20px;'>📦</div>" +
            "<div>" + message + "</div>" +
            "</div></body></html>";
        webView.loadDataWithBaseURL(null, loadingHtml, "text/html", "UTF-8", null);
    }

    private void showError(String error) {
        String errorHtml = "<html><body style='text-align: center; padding: 100px; font-family: Arial;'>" +
            "<div style='color: #f44336; font-size: 18px;'>" +
            "<div style='font-size: 48px; margin-bottom: 20px;'>⚠️</div>" +
            "<div>Error: " + error + "</div>" +
            "<div style='margin-top: 20px; font-size: 14px; color: #999;'>Tap back to return to feed</div>" +
            "</div></body></html>";
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
    }

    private String wrapHtmlContent(String htmlContent) {
        return "<html>" +
               "<head>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<style>" +
               "body { margin: 0; padding: 16px; font-family: Arial, sans-serif; background-color: #f5f5f5; }" +
               "img { max-width: 100%; height: auto; border-radius: 8px; }" +
               "video { max-width: 100%; height: auto; border-radius: 8px; }" +
               "h1, h2, h3 { margin-top: 0; color: #333; }" +
               "p { line-height: 1.6; color: #666; }" +
               ".card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); }" +
               "button { background: #2196F3; color: white; border: none; padding: 12px 24px; border-radius: 8px; cursor: pointer; font-size: 16px; }" +
               "button:hover { background: #1976D2; }" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='card'>" + htmlContent + "</div>" +
               "</body>" +
               "</html>";
    }

    private void setupBackNavigation() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
