package com.parentplatform.dto;

import org.springframework.web.multipart.MultipartFile;

public class ResourceCreateDTO {
    private String title;
    private String description;
    private String type;
    private String age;
    private String fullContent;
    private MultipartFile file;
    private String videoUrl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}