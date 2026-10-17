package com.example.storyworthy;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.HashMap;

@IgnoreExtraProperties
public class Stats {

    private int currStreak;
    private int longestStreak;
    private int numEntries;
    private HashMap<String, Integer> tagFrequencies;

    public Stats() {
        this.currStreak = 0;
        this.longestStreak = 0;
        this.numEntries = 0;
        this.tagFrequencies = new HashMap<>();
    }

    public int getCurrStreak() { return currStreak; }
    public void setCurrStreak(int currStreak) { this.currStreak = currStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public int getNumEntries() { return numEntries; }
    public void setNumEntries(int numEntries) { this.numEntries = numEntries; }

    public HashMap<String, Integer> getTagFrequencies() { return tagFrequencies; }
    public void setTagFrequencies(HashMap<String, Integer> tagFrequencies) { this.tagFrequencies = tagFrequencies; }
}
