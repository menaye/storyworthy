package com.example.storyworthy;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Entry implements Serializable {
    @DocumentId
    private String id;

    private String title;
    private String text;
    private String date;
    private String time;
    private String mood;
    private List<String> tags;
    private List<String> imageUris;
    private String strokesJson;
    private String audioPath;

    @PropertyName("isStarred")
    private boolean isStarred;

    // Default no-argument constructor required for Firebase Firestore deserialization
    public Entry() {
        this.title = "";
        this.text = "";
        this.date = "";
        this.time = "";
        this.mood = "";
        this.tags = new ArrayList<>();
        this.imageUris = new ArrayList<>();
        this.strokesJson = "";
        this.audioPath = "";
        this.isStarred = false;
    }

    public Entry(
            String title,
            String text,
            String date,
            String time,
            String mood,
            List<String> tags,
            List<String> imageUris,
            String strokesJson,
            String audioPath
    ) {
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.date = date != null ? date : "";
        this.time = time != null ? time : "";
        this.mood = mood != null ? mood : "";
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.imageUris = imageUris != null ? new ArrayList<>(imageUris) : new ArrayList<>();
        this.strokesJson = strokesJson != null ? strokesJson : "";
        this.audioPath = audioPath != null ? audioPath : "";
        this.isStarred = false;
    }

    // Getters and Setters required by Firestore
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getImageUris() { return imageUris; }
    public void setImageUris(List<String> imageUris) { this.imageUris = imageUris; }

    public String getStrokesJson() { return strokesJson; }
    public void setStrokesJson(String strokesJson) { this.strokesJson = strokesJson; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    @PropertyName("isStarred")
    public boolean isStarred() { return isStarred; }

    @PropertyName("isStarred")
    public void setStarred(boolean starred) { isStarred = starred; }
}
