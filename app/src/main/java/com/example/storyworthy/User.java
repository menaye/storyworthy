package com.example.storyworthy;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class User implements Serializable {

    private String email;
    private String uName;
    private String fName;
    private String lName;
    private boolean seenTutorial;
    private String psswd;

    private Preferences userPrefs;
    private Stats userStats;

    private List<Entry> entries;

    public User() {
        this.userPrefs = new Preferences();
        this.userStats = new Stats();
    } // Required for Firestore

    public User(String uName, String psswd, String fName, String lName) {
        this.uName = uName;
        this.psswd = psswd;
        this.fName = fName;
        this.lName = lName;
        this.seenTutorial = false;
        this.userPrefs = new Preferences();
        this.userStats = new Stats();
        this.entries = new ArrayList<>();
    }

    public String getUName() {
        return this.uName;
    }
    public void setUName(String uName) {
        this.uName = uName;
    }

    @Exclude
    public String getFullName() {
        String first = this.fName != null ? this.fName : "";
        String last = this.lName != null ? this.lName : "";
        return (first + " " + last).trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFName() { return fName; }
    public void setFName(String fName) { this.fName = fName; }

    public String getLName() { return lName; }
    public void setLName(String lName) { this.lName = lName; }

    public String getPsswd() { return psswd; }
    public void setPsswd(String psswd) { this.psswd = psswd; }

    public boolean isSeenTutorial() { return seenTutorial; }
    public void setSeenTutorial(boolean seenTutorial) { this.seenTutorial = seenTutorial; }

    public Preferences getUserPrefs() { return userPrefs; }
    public void setUserPrefs(Preferences userPrefs) { this.userPrefs = userPrefs; }

    public Stats getUserStats() { return userStats; }
    public void setUserStats(Stats userStats) { this.userStats = userStats; }
}
