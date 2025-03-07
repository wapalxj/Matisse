/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.model;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.zhihu.matisse.internal.loader.AlbumLoader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AlbumCollection implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID =0;



    private static final String STATE_CURRENT_SELECTION = "state_current_selection";
    private WeakReference<Context> mContext;
    private LoaderManager mLoaderManager;
    private AlbumCallbacks mCallbacks;
    private int mCurrentSelection;
    private boolean mLoadFinished;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Context context = mContext.get();
        if (context == null) {
            return null;
        }
        mLoadFinished = false;
        return AlbumLoader.newInstance(context);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        if (data != null && !data.isAfterLast()) {
            ArrayList<String> allImages = new ArrayList<>();
            while (data.moveToNext()) {
                //查询数据
                String imageName = data.getString(data.getColumnIndexOrThrow("bucket_display_name"));
                String imagePath = data.getString(data.getColumnIndexOrThrow("uri"));
//                String imageName = data.getString(data.getColumnIndexOrThrow("_display_name"));
//                String imagePath = data.getString(data.getColumnIndexOrThrow("_id"));
                long count = data.getLong(data.getColumnIndex(AlbumLoader.COLUMN_COUNT));
                allImages.add(imagePath);

                Log.e("fire---AlbumCollection", "imageName==" + imageName + ",imagePath===" + imagePath+",count=="+count);

            }
        }
        if (!mLoadFinished) {
            mLoadFinished = true;
            mCallbacks.onAlbumLoad(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        mCallbacks.onAlbumReset();
    }

    public void onCreate(FragmentActivity activity, AlbumCallbacks callbacks) {
        mContext = new WeakReference<Context>(activity);
        mLoaderManager = activity.getSupportLoaderManager();
        mCallbacks = callbacks;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        mCurrentSelection = savedInstanceState.getInt(STATE_CURRENT_SELECTION);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SELECTION, mCurrentSelection);
    }

    public void onDestroy() {
        if (mLoaderManager != null) {
            mLoaderManager.destroyLoader(LOADER_ID);
        }
        mCallbacks = null;
    }

    public void loadAlbums() {
        mLoaderManager.initLoader(LOADER_ID, null, this);
    }

    public int getCurrentSelection() {
        return mCurrentSelection;
    }

    public void setStateCurrentSelection(int currentSelection) {
        mCurrentSelection = currentSelection;
    }

    public interface AlbumCallbacks {
        void onAlbumLoad(Cursor cursor);

        void onAlbumReset();
    }
}
