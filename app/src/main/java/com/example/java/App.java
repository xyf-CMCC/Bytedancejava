package com.example.java;

import android.app.Application;
import android.content.ComponentCallbacks2;

import com.bumptech.glide.Glide;

public class App extends Application implements ComponentCallbacks2 {
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }
}
