package com.bunga.kulay.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ebrewer on 7/27/16.
 */
public class Color {

    public String userId;
    public long timestamp;

    public Color() {
    }

    public Color(String userId, long timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("timestamp", timestamp);
        return map;
    }
}
