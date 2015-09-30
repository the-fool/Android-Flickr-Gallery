package com.example.sean.photogallery;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;


public class SingletonLruCache extends LruCache<String, Bitmap> {
    public static int SIZE = 60;
    private static SingletonLruCache sSingletonLruCache;

    private SingletonLruCache(int size) {
        super(size);
    }

    public static SingletonLruCache getInstance() {
        if (sSingletonLruCache == null) {
            synchronized (SingletonLruCache.class) {
                if (sSingletonLruCache == null) {
                    sSingletonLruCache = new SingletonLruCache(SIZE);
                }
            }
        }
        return sSingletonLruCache;
    }

    public static Bitmap getBitmapFromMemoryCache(String key) {
        if (key == null) return null;
        if (sSingletonLruCache.get(key) != null) {
            Log.i("Singleton", "Returning a bitmap");
        }
        return sSingletonLruCache.get(key);
    }

    public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            Log.i("Singleton", "adding to cache!");
            sSingletonLruCache.put(key, bitmap);
        }
    }
}
