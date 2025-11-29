package com.example.java.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

@GlideModule
public class MyGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        int memoryCacheSizeBytes = 30 * 1024 * 1024;
        int bitmapPoolSizeBytes = 30 * 1024 * 1024;
        int diskCacheSizeBytes = 50 * 1024 * 1024;

        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));
        builder.setBitmapPool(new LruBitmapPool(bitmapPoolSizeBytes));
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));

        builder.setDefaultRequestOptions(new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC));
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        okhttp3.Cache cache = new okhttp3.Cache(new java.io.File(context.getCacheDir(), "okhttp-img"), 50L * 1024L * 1024L);
        OkHttpClient client = new OkHttpClient.Builder()
                .cache(cache)
                .addNetworkInterceptor(chain -> {
                    okhttp3.Response resp = chain.proceed(chain.request());
                    return resp.newBuilder()
                            .header("Cache-Control", "public, max-age=" + (7 * 24 * 60 * 60))
                            .build();
                })
                .connectionPool(new ConnectionPool(8, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();

        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
