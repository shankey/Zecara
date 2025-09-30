package com.app.zecara.util;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebView Pool Manager for efficient WebView reuse in RecyclerView
 * Reduces WebView creation/destruction overhead and memory usage
 */
public class WebViewPool {
    private static final String TAG = "WebViewPool";
    private static final int POOL_SIZE = 4; // Maximum number of WebViews to keep in pool
    private static final int MAX_POOL_SIZE = 6; // Hard limit to prevent memory issues
    
    private static WebViewPool instance;
    private final BlockingQueue<WebView> pool;
    private final Context context;
    private int createdWebViews = 0;
    
    private WebViewPool(Context context) {
        this.context = context.getApplicationContext();
        this.pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        initializePool();
    }
    
    public static synchronized WebViewPool getInstance(Context context) {
        if (instance == null) {
            instance = new WebViewPool(context);
        }
        return instance;
    }
    
    /**
     * Pre-create WebViews for the pool
     */
    private void initializePool() {
        Log.d(TAG, "Initializing WebView pool with " + POOL_SIZE + " WebViews");
        
        for (int i = 0; i < POOL_SIZE; i++) {
            WebView webView = createConfiguredWebView();
            pool.offer(webView);
        }
    }
    
    /**
     * Create a new WebView with standard configuration for HTML5 content
     */
    private WebView createConfiguredWebView() {
        WebView webView = new WebView(context);
        createdWebViews++;
        
        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Disable touch interactions for feed items
        webView.setClickable(false);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        
        // Set WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // Allow navigation within WebView
            }
        });
        
        Log.d(TAG, "Created WebView #" + createdWebViews);
        return webView;
    }
    
    /**
     * Acquire a WebView from the pool
     * @return A configured WebView ready for use
     */
    public WebView acquireWebView() {
        WebView webView = pool.poll();
        
        if (webView == null) {
            // Pool is empty, create a new WebView if under limit
            if (createdWebViews < MAX_POOL_SIZE) {
                webView = createConfiguredWebView();
                Log.d(TAG, "Pool empty, created new WebView. Total: " + createdWebViews);
            } else {
                Log.w(TAG, "Pool exhausted and at max limit (" + MAX_POOL_SIZE + "). Waiting for WebView...");
                try {
                    webView = pool.take(); // Block until one is available
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return createConfiguredWebView(); // Fallback
                }
            }
        }
        
        // Clear previous content
        clearWebView(webView);
        
        Log.d(TAG, "WebView acquired. Pool size: " + pool.size());
        return webView;
    }
    
    /**
     * Release a WebView back to the pool
     * @param webView The WebView to return to the pool
     */
    public void releaseWebView(WebView webView) {
        if (webView == null) {
            return;
        }
        
        // Clear content and reset state
        clearWebView(webView);
        
        // Return to pool if there's space
        boolean added = pool.offer(webView);
        
        if (added) {
            Log.d(TAG, "WebView returned to pool. Pool size: " + pool.size());
        } else {
            Log.d(TAG, "Pool full, destroying WebView");
            destroyWebView(webView);
        }
    }
    
    /**
     * Clear WebView content and reset state for reuse
     */
    private void clearWebView(WebView webView) {
        if (webView != null) {
            // Clear content
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            
            // Remove from parent if attached (layout parameters will be set by new parent)
            if (webView.getParent() != null) {
                try {
                    ((android.view.ViewGroup) webView.getParent()).removeView(webView);
                } catch (Exception e) {
                    Log.w(TAG, "Could not remove WebView from parent: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Properly destroy a WebView to prevent memory leaks
     */
    private void destroyWebView(WebView webView) {
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
            createdWebViews--;
            Log.d(TAG, "WebView destroyed. Total: " + createdWebViews);
        }
    }
    
    /**
     * Get current pool statistics
     */
    public PoolStats getStats() {
        return new PoolStats(pool.size(), createdWebViews, MAX_POOL_SIZE);
    }
    
    /**
     * Clear and destroy all WebViews in the pool
     * Call this when the app is being destroyed
     */
    public void clearPool() {
        Log.d(TAG, "Clearing WebView pool...");
        
        WebView webView;
        while ((webView = pool.poll()) != null) {
            destroyWebView(webView);
        }
        
        Log.d(TAG, "WebView pool cleared. Remaining WebViews: " + createdWebViews);
    }
    
    /**
     * Pool statistics for monitoring
     */
    public static class PoolStats {
        public final int availableWebViews;
        public final int totalWebViews;
        public final int maxPoolSize;
        
        PoolStats(int available, int total, int max) {
            this.availableWebViews = available;
            this.totalWebViews = total;
            this.maxPoolSize = max;
        }
        
        @Override
        public String toString() {
            return String.format("WebView Pool: %d/%d available, %d total created", 
                    availableWebViews, maxPoolSize, totalWebViews);
        }
    }
}
