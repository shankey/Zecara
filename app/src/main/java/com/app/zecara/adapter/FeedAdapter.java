package com.app.zecara.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zecara.R;
import com.app.zecara.model.ContentItem;
import com.app.zecara.util.Html5ProjectManager;
import com.app.zecara.util.WebViewPool;

import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    
    private List<ContentItem> feedItems;
    private OnFeedItemClickListener listener;
    private Html5ProjectManager projectManager;
    private WebViewPool webViewPool;

    public interface OnFeedItemClickListener {
        void onShareClicked(ContentItem item, int position);
        void onMoreClicked(ContentItem item, int position);
        void onContentClicked(ContentItem item, int position);
    }

    public FeedAdapter(List<ContentItem> feedItems) {
        this.feedItems = feedItems;
    }

    public void setProjectManager(Html5ProjectManager projectManager) {
        this.projectManager = projectManager;
    }
    
    public void setWebViewPool(WebViewPool webViewPool) {
        this.webViewPool = webViewPool;
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        ContentItem item = feedItems.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void updateItems(List<ContentItem> newItems) {
        this.feedItems = newItems;
        notifyDataSetChanged();
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
        private TextView authorName;
        private WebView contentWebView;
        private ImageView shareButton;
        private TextView postDescription;
        private ImageView moreOptions;
        private ImageView authorAvatar;
        private FrameLayout contentContainer;
        private TextView fullScreenHint;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            authorName = itemView.findViewById(R.id.authorName);
            shareButton = itemView.findViewById(R.id.shareButton);
            postDescription = itemView.findViewById(R.id.postDescription);
            moreOptions = itemView.findViewById(R.id.moreOptions);
            authorAvatar = itemView.findViewById(R.id.authorAvatar);
            fullScreenHint = itemView.findViewById(R.id.fullScreenHint);
            
            // Get the FrameLayout container for pooled WebViews
            contentContainer = itemView.findViewById(R.id.contentWebView).getParent() instanceof FrameLayout ?
                    (FrameLayout) itemView.findViewById(R.id.contentWebView).getParent() : null;
            
            // Remove the placeholder WebView from layout - we'll add pooled ones dynamically
            WebView placeholderWebView = itemView.findViewById(R.id.contentWebView);
            if (contentContainer != null && placeholderWebView != null) {
                contentContainer.removeView(placeholderWebView);
            }
        }

        /**
         * Acquire a WebView from the pool and attach it to the container
         */
        private void acquireWebView() {
            if (webViewPool != null && contentContainer != null) {
                // Release any existing WebView first
                releaseWebView();
                
                // Get a fresh WebView from the pool
                contentWebView = webViewPool.acquireWebView();
                
                if (contentWebView != null) {
                    try {
                        // Ensure WebView is not attached to any parent
                        if (contentWebView.getParent() != null) {
                            ((android.view.ViewGroup) contentWebView.getParent()).removeView(contentWebView);
                        }
                        
                        // Set proper layout parameters for the container
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                        );
                        contentWebView.setLayoutParams(layoutParams);
                        
                        // Add to container
                        contentContainer.addView(contentWebView, 0); // Add below the hint overlay
                        
                        Log.d("FeedAdapter", "WebView acquired and attached to ViewHolder");
                    } catch (Exception e) {
                        Log.e("FeedAdapter", "Error attaching WebView: " + e.getMessage());
                        // Release the WebView back to pool if attachment failed
                        if (webViewPool != null && contentWebView != null) {
                            webViewPool.releaseWebView(contentWebView);
                            contentWebView = null;
                        }
                    }
                }
            }
        }
        
        /**
         * Release the current WebView back to the pool
         */
        private void releaseWebView() {
            if (contentWebView != null && webViewPool != null) {
                try {
                    // Remove from container safely
                    if (contentContainer != null && contentWebView.getParent() == contentContainer) {
                        contentContainer.removeView(contentWebView);
                    }
                    
                    // Return to pool
                    webViewPool.releaseWebView(contentWebView);
                    contentWebView = null;
                    
                    Log.d("FeedAdapter", "WebView released back to pool");
                } catch (Exception e) {
                    Log.e("FeedAdapter", "Error releasing WebView: " + e.getMessage());
                    // Still try to return to pool even if removal failed
                    if (webViewPool != null && contentWebView != null) {
                        webViewPool.releaseWebView(contentWebView);
                        contentWebView = null;
                    }
                }
            }
        }

        public void bind(ContentItem item, int position) {
            // Acquire a WebView from the pool for this item
            acquireWebView();
            
            // Set author info
            authorName.setText(item.getAuthorName() != null ? item.getAuthorName() : "Anonymous");

            // Only load content if we successfully acquired a WebView
            if (contentWebView != null) {
                // Load HTML5 content - prioritize downloadable projects
                if (item.getProjectUrl() != null && !item.getProjectUrl().isEmpty()) {
                    loadDownloadableProject(item);
                } else if (item.getProjectPath() != null && !item.getProjectPath().isEmpty()) {
                    // Load multi-page HTML5 project from assets
                    String projectUrl = "file:///android_asset/html5_projects/" + item.getProjectPath() + "/index.html";
                    contentWebView.loadUrl(projectUrl);
                } else if (item.getHtmlContent() != null && !item.getHtmlContent().isEmpty()) {
                    // Load inline HTML content (legacy support)
                    String htmlContent = wrapHtmlContent(item.getHtmlContent());
                    contentWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                } else {
                    contentWebView.loadData("<html><body><p>No content available</p></body></html>", "text/html", "UTF-8");
                }
            } else {
                Log.e("FeedAdapter", "Failed to acquire WebView for item: " + item.getTitle());
                showError("WebView not available");
            }

            // Set description
            postDescription.setText(item.getDescription() != null ? item.getDescription() : "");

            // Set click listeners
            if (listener != null) {
                shareButton.setOnClickListener(v -> listener.onShareClicked(item, position));
                moreOptions.setOnClickListener(v -> listener.onMoreClicked(item, position));
                
                // Make multiple areas clickable for better UX
                itemView.setOnClickListener(v -> {
                    Log.d("FeedAdapter", "Feed item clicked: " + item.getTitle());
                    listener.onContentClicked(item, position);
                });
                contentContainer.setOnClickListener(v -> {
                    Log.d("FeedAdapter", "Content container clicked: " + item.getTitle());
                    listener.onContentClicked(item, position);
                });
                fullScreenHint.setOnClickListener(v -> {
                    Log.d("FeedAdapter", "Full screen hint clicked: " + item.getTitle());
                    listener.onContentClicked(item, position);
                });
            }
        }

        private void loadDownloadableProject(ContentItem item) {
            if (projectManager == null) {
                // Fallback to inline content or error
                showError("Project manager not available");
                return;
            }

            if (contentWebView == null) {
                Log.e("FeedAdapter", "Cannot load downloadable project - WebView is null");
                return;
            }

            // Check if project is already downloaded
            String localPath = projectManager.getLocalProjectPath(item.getId());
            if (localPath != null) {
                // Load from local cache
                contentWebView.loadUrl(localPath);
                return;
            }

            // Show loading state
            showLoadingState("Downloading project...");

            // Download project from Firebase Storage
            projectManager.downloadProject(
                item.getProjectUrl(), 
                item.getId(), 
                new Html5ProjectManager.ProjectDownloadCallback() {
                    @Override
                    public void onSuccess(String localProjectPath) {
                        // Load the downloaded project
                        String indexUrl = "file://" + localProjectPath + "/index.html";
                        if (contentWebView != null) {
                            contentWebView.post(() -> {
                                if (contentWebView != null) {
                                    contentWebView.loadUrl(indexUrl);
                                }
                            });
                        }
                    }

                    @Override
                    public void onProgress(int percentage) {
                        if (contentWebView != null) {
                            contentWebView.post(() -> 
                                showLoadingState("Downloading... " + percentage + "%")
                            );
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (contentWebView != null) {
                            contentWebView.post(() -> showError("Download failed: " + error));
                        }
                    }
                }
            );
        }

        private void showLoadingState(String message) {
            if (contentWebView != null) {
                String loadingHtml = "<html><body style='text-align: center; padding: 50px;'>" +
                    "<div style='color: #666;'>" +
                    "<div style='font-size: 18px; margin-bottom: 10px;'>📦</div>" +
                    "<div>" + message + "</div>" +
                    "</div></body></html>";
                contentWebView.loadDataWithBaseURL(null, loadingHtml, "text/html", "UTF-8", null);
            }
        }

        private void showError(String error) {
            if (contentWebView != null) {
                String errorHtml = "<html><body style='text-align: center; padding: 50px;'>" +
                    "<div style='color: #f44336;'>" +
                    "<div style='font-size: 18px; margin-bottom: 10px;'>⚠️</div>" +
                    "<div>Error: " + error + "</div>" +
                    "</div></body></html>";
                contentWebView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null);
            } else {
                Log.e("FeedAdapter", "Error (WebView null): " + error);
            }
        }

        private String wrapHtmlContent(String htmlContent) {
            return "<html>" +
                   "<head>" +
                   "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                   "<style>" +
                   "body { margin: 0; padding: 8px; font-family: Arial, sans-serif; background-color: #f5f5f5; }" +
                   "img { max-width: 100%; height: auto; border-radius: 8px; }" +
                   "video { max-width: 100%; height: auto; border-radius: 8px; }" +
                   "h1, h2, h3 { margin-top: 0; color: #333; }" +
                   "p { line-height: 1.4; color: #666; }" +
                   ".card { background: white; border-radius: 8px; padding: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }" +
                   "</style>" +
                   "</head>" +
                   "<body>" +
                   "<div class='card'>" + htmlContent + "</div>" +
                   "</body>" +
                   "</html>";
        }
        
        /**
         * Cleanup method to call when ViewHolder is being recycled
         */
        public void cleanup() {
            releaseWebView();
        }
    }
    
    @Override
    public void onViewRecycled(@NonNull FeedViewHolder holder) {
        super.onViewRecycled(holder);
        // Release WebView back to pool when ViewHolder is recycled
        holder.cleanup();
        Log.d("FeedAdapter", "ViewHolder recycled, WebView returned to pool");
    }
    
    @Override
    public void onViewDetachedFromWindow(@NonNull FeedViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // Release WebView when ViewHolder goes off-screen
        holder.cleanup();
    }
    
    /**
     * Call this method when the adapter is being destroyed
     */
    public void onDestroy() {
        if (webViewPool != null) {
            Log.d("FeedAdapter", "Adapter destroyed, WebView pool stats: " + webViewPool.getStats());
            // Note: Pool cleanup should be handled by the Activity/Fragment
            // since the pool might be shared across multiple components
        }
    }
}
