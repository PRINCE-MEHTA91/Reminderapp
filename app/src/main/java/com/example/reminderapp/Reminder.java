package com.example.reminderapp;
public class Reminder {
    public String id, title, description;
    public long timeInMillis;

    public Reminder() {}

    public Reminder(String id, String title, String description, long timeInMillis) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.timeInMillis = timeInMillis;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }
}
