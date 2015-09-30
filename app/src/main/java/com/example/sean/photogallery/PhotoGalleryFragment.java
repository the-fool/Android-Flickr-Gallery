package com.example.sean.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;


public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";

    GridView mGridView;
    boolean mNewSearch;
    ArrayList<GalleryItem> mItems;
    private int mTotalCount;
    private int mPages;
    ThumbnailDownloader<ImageView> mThumbnailThread;
    SingletonLruCache mLruCache;

    @Override
    public void onCreate(Bundle b) {
        mNewSearch = true;
        mTotalCount = 0;
        super.onCreate(b);
        mPages = 0;
        mItems = new ArrayList<GalleryItem>();
        mLruCache = SingletonLruCache.getInstance();

        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread begun");
    }

    public void updateItems() {
        new FetchItemsTask().execute(++mPages);
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup p, Bundle b) {
        View v = i.inflate(R.layout.fragment_photo_gallery, p, false);

        mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if ((visibleItemCount > 0) && ((firstVisibleItem + visibleItemCount) >= totalItemCount) && (totalItemCount > mTotalCount)) {
                    mTotalCount = totalItemCount;
                    Log.i(TAG, "OnScroll triggered");
                    new FetchItemsTask().execute(++mPages);
                }
            }
        });

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GalleryItem item = mItems.get(position);

                Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
                Intent i = new Intent(getActivity(), PhotoPageActivity.class);
                i.setData(photoPageUri);

                startActivity(i);
            }
        });
        resumeAdapter();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            //searchView.setIconified(true);
            /*searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    SearchManager sm = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
                    ComponentName name = getActivity().getComponentName();
                    SearchableInfo searchInfo = sm.getSearchableInfo(name);

                    searchView.setSearchableInfo(searchInfo);
                    searchItem.collapseActionView();

                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });*/
            SearchManager sm = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            SearchableInfo searchInfo = sm.getSearchableInfo(name);
            searchView.setSearchableInfo(searchInfo);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;*/
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                setNewSearchFlag(true);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);

                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    public void setNewSearchFlag(boolean newSearch) {
        mNewSearch = newSearch;
        mPages = 0;
        mTotalCount = 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {

        @Override
        protected ArrayList<GalleryItem> doInBackground(Integer... params) {
            Activity activity = getActivity();
            if (activity == null)
                return new ArrayList<GalleryItem>();

            String query = PreferenceManager.getDefaultSharedPreferences(activity).getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
            if (query != null) {
                return new FlickrFetchr().search(query, params[0]);
            } else {
                return new FlickrFetchr().fetchItems(params[0]);
            }
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            if (mItems != null && !mNewSearch) {
                Log.i(TAG, "Adding a new page");
                mItems.addAll(items);
            } else {
                Log.i(TAG, "Starting fresh");
                mNewSearch = false;
                mItems.clear();
                mItems.addAll(items);
                mGridView.smoothScrollToPosition(0);
            }
            resumeAdapter();
        }
    }

    private class CacheThumbnailTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            int pos = params[0];
            for (int i = pos - 10; i <= (pos + 25); i++) {
                if (i >= 0 && i < mItems.size() && i != pos) {
                    // This guard used to make a String instance first . . .
                    if (mItems.get(i) == null) continue;
                    if (mItems.get(i).getUrl() == null) continue;
                    if (mLruCache.get(mItems.get(i).getUrl()) == null) {
                        mThumbnailThread.queueCache(mItems.get(i).getUrl());
                    }
                }
            }
            return null;
        }
    }

    void resumeAdapter() {
        if (getActivity() == null || mGridView == null) return;
        if (!mItems.isEmpty()) {
            if (mGridView.getAdapter() == null) {
                mGridView.setAdapter(new GalleryItemAdapter(mItems));
            } else {
                ArrayAdapter<GalleryItem> aa = (ArrayAdapter<GalleryItem>) mGridView.getAdapter();
                aa.notifyDataSetChanged();
            }
        } else {
            mGridView.setAdapter(null);
        }
    }

    static class ViewHolder {
        ImageView mImageView;
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }


        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
                holder = new ViewHolder();
                holder.mImageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            GalleryItem item = getItem(pos);
            String key = item.getUrl();
            holder.mImageView.setImageResource(R.drawable.brian_up_close);

            Bitmap bitmap = mLruCache.getBitmapFromMemoryCache(key);

            if (bitmap == null) {
                mThumbnailThread.queueThumbnail(holder.mImageView, key);
            } else {
                holder.mImageView.setImageBitmap(bitmap);
            }
            if (pos != 0) {
                new CacheThumbnailTask().execute(pos);
            }

            return convertView;
        }

    }
}

