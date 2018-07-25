package com.ting.privatephoto.app;

import java.util.HashSet;
import java.util.TreeSet;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import com.ting.privatephoto.R;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.media.ImageItem;
import com.ting.privatephoto.media.VideoItem;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import static com.ting.privatephoto.media.MediaItem.UNDEFINED_ID_INDEX;

public class PublicAlbumListActivity extends AlbumListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreate(PrivacyType.PUBLIC, new EnumerateAlbumTasker());
        titleBar.setText(R.string.top_bar_title_public_album_list);
    }

    private class EnumerateAlbumTasker extends AlbumListActivity.EnumerateAlbumTasker {
        @Override
        protected Void doInBackground(Void... params) {
            // enumerate bucket id
            HashSet<String> bucketIds = new HashSet<>();
            TreeSet<Album> albumSet = new TreeSet<>();
            Cursor cursor = Images.Media.query(getContext().getContentResolver(),
                    Images.Media.EXTERNAL_CONTENT_URI, new String[]{Images.Media.BUCKET_ID,
                    Images.Media.BUCKET_DISPLAY_NAME, Images.Media.DATA}, null, null,
                    Images.Media.BUCKET_DISPLAY_NAME+" ASC");
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (isCancelled()) return null;
                    String bucketId = cursor.getString(0);
                    String albumName = cursor.getString(1);
                    String itemName = cursor.getString(2);
                    if (!bucketIds.contains(bucketId)) {
                        Album album = new Album();
                        album.id = bucketId;
                        album.name = albumName;
                        album.path = itemName.substring(0, itemName.lastIndexOf("/"));
                        bucketIds.add(bucketId);
                        albumSet.add(album);
                    }
                    if (!cursor.moveToNext()) continue;
                }
                cursor.close();
            }
            cursor = getContext().getContentResolver().query(Video.Media.EXTERNAL_CONTENT_URI,
                    new String[] {Video.Media.BUCKET_ID, Video.Media.BUCKET_DISPLAY_NAME,
                    Video.Media.DATA}, null, null, Video.Media.BUCKET_DISPLAY_NAME+" ASC");
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (isCancelled()) return null;
                    String bucketId = cursor.getString(0);
                    String albumName = cursor.getString(1);
                    String itemName = cursor.getString(2);
                    if (!bucketIds.contains(bucketId)) {
                        Album album = new Album();
                        album.id = bucketId;
                        album.name = albumName;
                        album.path = itemName.substring(0, itemName.lastIndexOf("/"));
                        bucketIds.add(bucketId);
                        albumSet.add(album);
                    }
                    if (!cursor.moveToNext()) continue;
                }
                cursor.close();
            }
            if (albumSet.isEmpty()) {
                Log.d(getClass().getName(), "[doInBackground] empty buckets!");
                return null;
            }
            publishProgress(PROGRESS_CODE_ALBUM_COUNT, albumSet.size());
            checkListGridViewReady();
            // get album item count and thumbnail in each bucket id
            albums.clear();
            Object[] albumArray = albumSet.toArray();
            for (int i = 0 ; i < albumArray.length ; i++) {
                if (isCancelled()) return null;
                Album album = (Album)albumArray[i];
                albums.add(album);
                album.position = i;
                cursor = Images.Media.query(getContext().getContentResolver(),
                        Images.Media.EXTERNAL_CONTENT_URI, new String[]{Images.Media.BUCKET_ID,
                        Images.Media._ID, Images.Media.DISPLAY_NAME, Images.Media.DATA},
                        Images.Media.BUCKET_ID + " = ?", new String[]{album.id},
                        Images.Media.DISPLAY_NAME+" ASC");
                if (cursor != null && cursor.moveToFirst()) {
                    album.count = cursor.getCount();
                    if (album.count != 0) {publishProgress(PROGRESS_CODE_ALBUM_ITEM_COUNT, i, album);}
                    for (int j = 0 ; j < album.count ; j++) {
                        ImageItem image = new ImageItem();
                        image.id = cursor.getLong(1);
                        image.name = cursor.getString(2);
                        image.path = cursor.getString(3);
                        Bitmap thumbnail = Images.Thumbnails.getThumbnail(getContext().getContentResolver(),
                                    image.id, Images.Thumbnails.MICRO_KIND, null);
                        if (thumbnail != null) {
                            album.thumbnail.id = image.id;
                            album.thumbnail.self = thumbnail;
                            publishProgress(PROGRESS_CODE_ALBUM_THUMBNAIL, i, album);
                            break;
                        }
                        if (!cursor.moveToNext()) continue;
                    }
                    cursor.close();
                }
                cursor = getContext().getContentResolver().query(Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{Video.Media.BUCKET_ID, Video.Media._ID, Video.Media.DISPLAY_NAME,
                        Video.Media.DATA}, Video.Media.BUCKET_ID + " = ?", new String[]{album.id},
                        Video.Media.DISPLAY_NAME+" ASC");
                if (cursor != null && cursor.moveToFirst()) {
                    int count = cursor.getCount();
                    album.count += count;
                    if (count != 0) {publishProgress(PROGRESS_CODE_ALBUM_ITEM_COUNT, i, album);}
                    if (album.thumbnail.id == UNDEFINED_ID_INDEX) {
                        for (int j = 0; j < count; j++) {
                            VideoItem video = new VideoItem();
                            video.id = cursor.getLong(1);
                            video.name = cursor.getString(2);
                            video.path = cursor.getString(3);
                            Bitmap thumbnail = Video.Thumbnails.getThumbnail(getContext().getContentResolver(),
                                        video.id, Video.Thumbnails.MICRO_KIND, null);
                            if (thumbnail != null) {
                                album.thumbnail.id = video.id;
                                album.thumbnail.self = thumbnail;
                                publishProgress(PROGRESS_CODE_ALBUM_THUMBNAIL, i, album);
                                break;
                            }
                            if (!cursor.moveToNext()) continue;
                        }
                    }
                    cursor.close();
                }
            }
            return null;
        }
    }
}