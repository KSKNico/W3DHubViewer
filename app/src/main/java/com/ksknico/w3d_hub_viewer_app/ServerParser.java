package com.ksknico.w3d_hub_viewer_app;

import android.util.ArrayMap;

import java.lang.reflect.Array;

public class ServerParser {

    private ArrayMap<String, String> parsed = new ArrayMap<String, String>();
    private String key = "";
    private String value = "";

    public ServerParser() { }

    public ArrayMap<String, String> parse(String input) {
        int i = 0;


        for (String element : input.split("\\\\")) {
            if (element.equals("")) {
                continue;
            } else if (i % 2 == 0) {
                key = element;
            } else if (i % 2 == 1) {
                value = element;
                parsed.put(key, value);
            }
            i++;
        }
        return parsed;
    }
}
