package com.example.sean.photogallery;

/**
 * Created by Sean on 6/17/2015.
 */
public class GalleryItem {
    private String mCaption;
    private String mId;
    private String mOwner;
    private String mUrl;

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getCaption() {

        return mCaption;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getOwner() {
        return mOwner;
    }
    public void setOwner(String owner) {
        mOwner = owner;
    }
    public String getPhotoPageUrl() {
        return "http://www.flickr.com/photos/" + mOwner + "/" + mId;
    }

    @Override
    public String toString() {
        return mCaption;
    }
}
