package com.example.storyworthy;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@IgnoreExtraProperties
public class Preferences {
    private boolean takesAISuggestion;
    private boolean lightMode;
    private boolean sfx;
    private boolean music;
    private boolean notifs;
    private int pageLayout; // sets layout on an integer; 0 is standard line, 1 is nonstandard, 2 is no lines, etc.
    private List<String> desiredKeywords;

    public Preferences() {
        this.takesAISuggestion = true;
        this.lightMode = true;
        this.sfx = true;
        this.music = true;
        this.notifs = true;
        this.pageLayout = 0;
        this.desiredKeywords = new ArrayList<>(Arrays.asList(
                "Anxiety", "Change", "Choice", "Confidence", "Courage",
                "Death", "Dreams", "Excellence", "Failure", "Fairness", "Fear", "Forgiveness",
                "Freedom", "Future", "Happiness", "Inspiration", "Kindness", "Leadership", "Life",
                "Living", "Love", "Pain", "Past", "Success", "Time", "Today", "Truth", "Work"));
    }

    public boolean isTakesAISuggestion() { return takesAISuggestion; }
    public void setTakesAISuggestion(boolean takesAISuggestion) { this.takesAISuggestion = takesAISuggestion; }

    public boolean isLightMode() { return lightMode; }
    public void setLightMode(boolean lightMode) { this.lightMode = lightMode; }

    public boolean isSfx() { return sfx; }
    public void setSfx(boolean sfx) { this.sfx = sfx; }

    public boolean isMusic() { return music; }
    public void setMusic(boolean music) { this.music = music; }

    public boolean isNotifs() { return notifs; }
    public void setNotifs(boolean notifs) { this.notifs = notifs; }

    public int getPageLayout() { return pageLayout; }
    public void setPageLayout(int pageLayout) { this.pageLayout = pageLayout; }

    public List<String> getDesiredKeywords() { return desiredKeywords; }
    public void setDesiredKeywords(List<String> desiredKeywords) { this.desiredKeywords = desiredKeywords; }

    public void toggleSuggestion() {
        this.takesAISuggestion = !this.takesAISuggestion;
    }

    public void toggleLightMode() {
        this.lightMode = !this.lightMode;
    }

    public void toggleSfx() {
        this.sfx = !this.sfx;
    }

    public void toggleMusic() {
        this.music = !this.music;
    }

    public void toggleNotifs() { this.notifs = !this.notifs; }

    @Exclude
    public boolean getMode() { return this.lightMode; }

    @Exclude
    public boolean getAIAssistStatus() { return this.takesAISuggestion; }

    @Exclude
    public void setAIAssistStatus(boolean status) { this.takesAISuggestion = status; }

    public void adjustLayout(int layoutPref) { this.pageLayout = layoutPref; }
}
