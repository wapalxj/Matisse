/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.SelectionSpec;

import java.io.OutputStream;

import androidx.annotation.Nullable;

public class AlbumsAdapter extends CursorAdapter {

    private final Drawable mPlaceholder;

    public AlbumsAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);

        TypedArray ta = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.album_thumbnail_placeholder});
        mPlaceholder = ta.getDrawable(0);
        ta.recycle();
    }

    public AlbumsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        TypedArray ta = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.album_thumbnail_placeholder});
        mPlaceholder = ta.getDrawable(0);
        ta.recycle();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.album_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Album album = Album.valueOf(cursor);
        ((TextView) view.findViewById(R.id.album_name)).setText(album.getDisplayName(context));
        ((TextView) view.findViewById(R.id.album_media_count)).setText(String.valueOf(album.getCount()));

        // do not need to load animated Gif
        SelectionSpec.getInstance().imageEngine.loadThumbnail(context, context.getResources().getDimensionPixelSize(R
                        .dimen.media_grid_size), mPlaceholder,
                (ImageView) view.findViewById(R.id.album_cover), album.getCoverUri());
//                (ImageView) view.findViewById(R.id.album_cover), Uri.parse(getRealPathFromUri(context,album.getCoverUri())));
//                (ImageView) view.findViewById(R.id.album_cover), Uri.parse("content://media/external/images/media/77953"));

//        Glide.with(context)
//                .asBitmap() // some .jpeg files are actually gif
//                .listener(new RequestListener<Bitmap>() {
//                    @Override
//                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
//                        if (resource.getWidth()>100 && resource.getHeight()>=70) {
//                            Log.e("fire---ppppponResourceReady","resource.getWidth()==="+resource.getWidth()+"====resource.getHeight()"+resource.getHeight());
//                            insertImage(context,resource);
//                        }
//                        return false;
//                    }
//                })
//                .load(album.getCoverUri())
//                .into((ImageView) view.findViewById(R.id.album_cover));


    }


    //MediaStore api 插入图片到DCIM文件夹
    private void insertImage(Context context,Bitmap bitmap) {
        // 拿到 MediaStore.Images 表的uri
        Uri tableUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // 创建图片索引
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,"test");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/test");
        values.put(MediaStore.Images.Media.DATE_ADDED, 0);//无效，切无法修改
        values.put(MediaStore.Images.Media.DATE_MODIFIED, 0);//无效，切无法修改
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());//无效，切无法修改

        // 将该索引信息插入数据表，获得图片的Uri
        Uri imageUri = context.getContentResolver().insert(tableUri,values);
        try {
            // 通过图片uri获得输出流
            OutputStream os = context.getContentResolver().openOutputStream(imageUri);

            // 图片压缩保存
            bitmap.compress(Bitmap.CompressFormat.PNG,100,os);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        String url="";
        int count=0;
        try {
//            String[] proj = {MediaStore.Images.Media.DATA,"_display_name","_id"};
            String[] proj = {"_id", "_display_name", "mime_type", "_size", "duration"};
            proj = null;
            //根据type查询全部
            cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                    proj,
                    MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                            + " AND " + MediaStore.MediaColumns.SIZE + ">0",
                    new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)},
                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");

            //根据coverUri获取封面图片
//            cursor = context.getContentResolver().query(Uri.parse("content://media/external/images/media/77953"),
//                    proj,
//                    null,null,
//                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");

            //根据bucket_id查
//            cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
//                    proj,
//                    "media_type=? AND  bucket_id=? AND _size>0",
////                    new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),"-2079080312"},
//                    new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),"97354131"},
//                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");

            //根据某张id查找图片
//            cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
//                    proj,
//                    "media_type=? AND _id=? ",
//                    new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),"101232"},
//                    MediaStore.Images.Media.DATE_MODIFIED + " DESC");


            while (cursor.moveToNext()) {
                //查询数据
                count++;
                if (count<=4) {
                    String imageName = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                    String imageDateAdded = cursor.getString(cursor.getColumnIndexOrThrow("date_added"));
                    String imagedate_modified = cursor.getString(cursor.getColumnIndexOrThrow("date_modified"));
                    String imagedatetaken = cursor.getString(cursor.getColumnIndexOrThrow("datetaken"));
                    Log.e("fire---ppppp", "_display_name==" + imageName + ",_id===" + imagePath + ",imageDateAdded===" + imageDateAdded + ",date_modified===" + imagedate_modified + ",datetaken===" + imagedatetaken);
                }
            }
//            ContentValues values=new ContentValues();
//            for (String columnName : cursor.getColumnNames()) {
//                Log.e("fire---ppppp222", "columnName===" + columnName + ",_value===" + cursor.getString(cursor.getColumnIndexOrThrow(columnName)));
//            }
//            values.put("_display_name","-xxxxxxx.png");
////            values.put("datetaken",10);
//            int row=context.getContentResolver().update(MediaStore.Files.getContentUri("external"),values,"media_type=? AND _id=? ", new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),"101232"});
//
//            Log.e("fire---ppppp8888", "update==row==" + row);

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            url= cursor.getString(column_index);
            return url;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }
}
