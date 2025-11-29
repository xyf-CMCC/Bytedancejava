package com.example.java.glide;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.java.User;
import com.example.java.UsersAdapter;

import java.util.Collections;
import java.util.List;

public class AvatarPreloadProvider implements ListPreloader.PreloadModelProvider<String> {
    private final UsersAdapter adapter;
    private final RequestManager rm;
    private final RequestOptions opts = new RequestOptions()
            .override(80, 80)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .circleCrop();

    public AvatarPreloadProvider(RequestManager rm, UsersAdapter adapter) {
        this.rm = rm;
        this.adapter = adapter;
    }

    @Override
    public List<String> getPreloadItems(int position) {
        if (position < 0 || position >= adapter.getItemCount()) return Collections.emptyList();
        User u = adapter.getItemAt(position);
        if (u == null || u.getAvatarUrl() == null || u.getAvatarUrl().isEmpty()) return Collections.emptyList();
        return Collections.singletonList(u.getAvatarUrl());
    }

    @Override
    public RequestBuilder<?> getPreloadRequestBuilder(String url) {
        return rm.load(url).apply(opts);
    }
}
