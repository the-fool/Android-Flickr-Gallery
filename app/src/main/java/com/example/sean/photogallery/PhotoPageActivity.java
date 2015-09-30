package com.example.sean.photogallery;

import android.support.v4.app.Fragment;

/**
 * Created by Sean on 7/15/2015.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new PhotoPageFragment();
    }


}
