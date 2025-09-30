package com.app.zecara.model;

import java.io.Serializable;

public class ContentItem implements Serializable {
    private String id;
    private String title;
    private String description;
    private String htmlContent; // HTML5 content to display in WebView
    private String projectPath; // Path to HTML5 project folder (for multi-page apps)
    private String projectUrl;  // Firebase Storage URL for downloadable HTML5 projects
    private String contentType; // "HTML5"
    private String category;
    private String authorName;
    private String authorAvatar;

    // Default constructor (required for Firestore)
    public ContentItem() {
    }

    public ContentItem(String id, String title, String description, String htmlContent, String contentType, String authorName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.htmlContent = htmlContent;
        this.contentType = contentType;
        this.authorName = authorName;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public String getProjectUrl() { return projectUrl; }
    public void setProjectUrl(String projectUrl) { this.projectUrl = projectUrl; }
}