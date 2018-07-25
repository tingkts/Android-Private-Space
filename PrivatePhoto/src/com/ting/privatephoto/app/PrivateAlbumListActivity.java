package com.ting.privatephoto.app;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import java.io.File;
import java.util.HashSet;
import com.ting.privatephoto.R;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import com.ting.privatephoto.database.PrivateItemProvider;
import com.ting.privatephoto.database.PrivateItemResolver;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.Log;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_REQUEST_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_RESULT_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS;

public class PrivateAlbumListActivity extends AlbumListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreate(PrivacyType.PRIVATE, new EnumerateAlbumTasker());
        titleBar.setText(R.string.top_bar_title_private_album_list);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != START_ACTIVITY_FOR_RESULT_REQUEST_CODE ||
                resultCode != START_ACTIVITY_FOR_RESULT_RESULT_CODE || data == null) {
            return;
        }
        boolean refreshPrivateAlbums = data.getBooleanExtra(INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS, false);
        Log.d(getClass().getSimpleName(), "[onActivityResult] refreshPrivateAlbums=" + refreshPrivateAlbums);
        if (refreshPrivateAlbums) {
            enumerateAlbumTasker = (EnumerateAlbumTasker)new EnumerateAlbumTasker().execute();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class EnumerateAlbumTasker extends AlbumListActivity.EnumerateAlbumTasker {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... params) {
            PrivateItemResolver.getInstance(getContext()).printAllItems(
                    "[PrivateAlbumListActivity][EnumerateAlbumTasker][doInBackground]");
            ContentResolver contentResolver = getContext().getContentResolver();
            Cursor cursor = contentResolver.query(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM,
                    new String[] {PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME}, null, null,
                    PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME +" ASC");
            if (cursor == null || !cursor.moveToFirst()) return null;
            albums.clear();
            HashSet<String> albumNames = new HashSet<>();
            for (int i = 0 ; i < cursor.getCount() ; i++) {
                if (isCancelled()) break;
                String albumName = cursor.getString(0);
                if (!albumNames.contains(albumName)) {
                    albumNames.add(albumName);
                    Album album = new Album();
                    album.name = albumName;
                    albums.add(album);
                }
                Log.d(getClass().getName(), "[doInBackground] " + albumName);
                if (!cursor.moveToNext()) continue;
            }
            cursor.close();
            if (albums.isEmpty()) return null;
            publishProgress(PROGRESS_CODE_ALBUM_COUNT, albums.size());
            checkListGridViewReady();
            for (int i = 0 ; i < albums.size() ; i++) {
                if (isCancelled()) break;
                Album album = albums.get(i);
                cursor = contentResolver.query(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM,
                        new String[] {
                                PrivateItemProvider.COLUMN_ID,                  // 0
                                PrivateItemProvider.COLUMN_MICRO_THUMBNAIL,     // 1
                                PrivateItemProvider.COLUMN_TYPE,                // 2
                                PrivateItemProvider.COLUMN_FILE_NAME},          // 3
                        PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME + " = ?", new String[] {album.name},
                        PrivateItemProvider.COLUMN_FILE_NAME+" ASC");
                if (cursor == null || !cursor.moveToFirst()) continue;
                album.count = cursor.getCount();
                publishProgress(PROGRESS_CODE_ALBUM_ITEM_COUNT, i, album);
                do {
                    Bitmap microThumbnail/* = PrivateItemResolver.getInstance(getContext()).getThumbnail(cursor, 1)*/ = null;
                    int type = cursor.getInt(2);
                    String fileName = cursor.getString(3);
                    File privateDir = getContext().getPrivateFilesDir();
                    String filePathName = privateDir.getPath() + "/" + album.name + "/" + fileName;
                    if (type == MediaItem.MEDIA_TYPE_IMAGE) {
                        microThumbnail = decodeBitmapFromImage(filePathName);
                    } else if (type == MediaItem.MEDIA_TYPE_VIDEO) {
                        microThumbnail = decodeBitmapFromVideo(filePathName);
                    }
                    if (microThumbnail != null) {
                        album.thumbnail.id = cursor.getLong(0);
                        album.thumbnail.self = microThumbnail;
                        publishProgress(PROGRESS_CODE_ALBUM_THUMBNAIL, i, album);
                        break;
                    }
                } while (cursor.moveToNext() && !isCancelled());
                cursor.close();
            }
            printAlbums();
            return null;
        }
    }
}