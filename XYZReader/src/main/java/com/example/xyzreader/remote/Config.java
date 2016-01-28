package com.example.xyzreader.remote;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {

    public static final String KEY_POSITION = "position";

    public static final URL BASE_URL;

    static {

        URL url;

        try {
            url = new URL("https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json" );
        } catch (MalformedURLException ignored) {
            url = null;
        }

        BASE_URL = url;
    }
}
