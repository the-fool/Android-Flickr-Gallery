package com.example.sean.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_CACHE = 1;

    Handler mHandler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
    Map<String, Boolean> cacheMap = Collections.synchronizedMap(new HashMap<String, Boolean>());
    Handler mResponseHandler;
    Listener<Token> mListener;
    SingletonLruCache mLruCache;


    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listner) {
        mListener = listner;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        mLruCache = SingletonLruCache.getInstance();
    }

    @Override
    protected void onLooperPrepared() {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a download request: " + requestMap.get(token));
                    handleRequest(token);
                }
                else if (msg.what == MESSAGE_CACHE) {
                    String url = (String)msg.obj;
                    Log.i(TAG, "Got a cache request: " + url);
                    handleCacheRequest(url);
                }
            }
        };
    }
    private void handleCacheRequest(String url) {
        if (url == null) return;
       /* if (cacheMap.size() >= mLruCache.SIZE) {
            cacheMap.remove(url);
            return;
        }*/
        try {
            if(mLruCache.get(url) == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                final Bitmap bitmap;
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mLruCache.addBitmapToMemoryCache(url, bitmap);
                Log.i(TAG, "Adding to LruCache");
            } else {
                Log.i(TAG, "Not adding to LruCache -- it was already there");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error caching image", e);
        } finally {
            cacheMap.remove(url);
        }
    }

    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            Log.i(TAG, "Handling download request for url: " + url);
            if (url == null) {
                return;
            }
            final Bitmap bitmap;
            if (mLruCache.get(url) == null) {
                Log.i(TAG, "Did not pull from cache");
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                mLruCache.addBitmapToMemoryCache(url, bitmap);
            } else {
                bitmap = mLruCache.getBitmapFromMemoryCache(url);
                Log.i(TAG, "Pulled from cache");
            }

            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (requestMap.get(token) != url) {
                        Log.i(TAG, "RequestMap got screwed up");
                        return;

                    }
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });

        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }

    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got URL: " + url);
        requestMap.put(token, url);
        Log.i(TAG, "Length of requestMap: " + requestMap.size());
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }

    public void queueCache( String url) {
        if (cacheMap.containsKey(url) || (cacheMap.size() >= mLruCache.SIZE)) {
            Log.i(TAG, "Blocked duplicate cache call");
            return;
        }

        cacheMap.put(url, Boolean.TRUE);
        Log.i(TAG, "Length of cacheMap: " + cacheMap.size());
        mHandler.obtainMessage(MESSAGE_CACHE, url).sendToTarget();
    }


}
