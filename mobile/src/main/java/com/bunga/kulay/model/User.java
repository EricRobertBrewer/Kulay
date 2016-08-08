package com.bunga.kulay.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ebrewer on 7/27/16.
 */
public class User {

    public int favoriteColor;
    public long timestamp;

    public User() {
    }

    public User(int favoriteColor, long timestamp) {
        this.favoriteColor = favoriteColor;
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("favoriteColor", favoriteColor);
        map.put("timestamp", timestamp);
        return map;
    }
}
