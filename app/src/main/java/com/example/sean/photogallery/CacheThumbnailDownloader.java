package com.example.sean.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;


public class CacheThumbnailDownloader extends HandlerThread {
    private static final String TAG = "CacheThumbDownloader";

    private static final int MESSAGE_CACHE_DOWNLOAD = 1;

    private Handler mHandler;

    public CacheThumbnailDownloader() {
        super(TAG);
    }

    public void queueCacheThumbnail(String url) {
        mHandler.obtainMessage(MESSAGE_CACHE_DOWNLOAD, url).sendToTarget();
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_CACHE_DOWNLOAD) {
                    String url = (String)msg.obj;
                    handleCacheRequest(url);
                }
            }
        };
    }

    private void handleCacheRequest(String url) {
        try {
            if ( (url != null) && (SingletonLruCache.getBitmapFromMemoryCache(url) == null)) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                SingletonLruCache.addBitmapToMemoryCache(url, bitmap);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }
    }

    public void clearCacheQueue() {
        mHandler.removeMessages(MESSAGE_CACHE_DOWNLOAD);
    }
}
