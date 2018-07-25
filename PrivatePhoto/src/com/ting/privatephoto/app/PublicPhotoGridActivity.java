package com.ting.privatephoto.app;

import java.io.File;
import java.util.ArrayList;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseBooleanArray;
import com.ting.privatephoto.R;
import com.ting.privatephoto.database.PrivateItemResolver;
import com.ting.privatephoto.media.ImageItem;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.media.VideoItem;
import com.ting.privatephoto.util.FileUtil;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import com.ting.privatephoto.view.BottomBar;

public class PublicPhotoGridActivity extends PhotoGridActivity {
    private BottomBar add2PrivateBottomBar;
    private AddToPrivateTasker add2PhotoTasker;

    private Cursor imageCursor;
    private Cursor videoCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreate(PrivacyType.PUBLIC, new EnumeratePhotoTasker());
        add2PrivateBottomBar = new BottomBar(BottomBar.Type.ITEM_ACTION_ADD2PRIVATE, this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageCursor != null)
            imageCursor.close();
        if (videoCursor != null)
            videoCursor.close();
    }

    @Override
    protected void setSelectionMode(boolean isEnabled) {
        super.setSelectionMode(isEnabled);
        Log.d(getClass().getSimpleName(), "[setSelectionMode] " + isEnabled);
        add2PrivateBottomBar.setVisible(isEnabled);
    }

    @Override
    public void onSelectionModeChanged(boolean isEnabled) {
        Log.d(getClass().getSimpleName(), "[onSelectionModeChanged] " + isEnabled);
        setSelectionMode(isEnabled);
    }

    @Override
    public void onButtonBarButtonClicked(BottomBar.ActionCode actionCode, int viewId) {
        if (add2PhotoTasker == null) {
            add2PhotoTasker = (AddToPrivateTasker)new AddToPrivateTasker().execute(
                    R.id.id_bottom_bar_add2private);
        } else {
            add2PhotoTasker.cancel(true);
            add2PhotoTasker = null;
        }
    }

    private class EnumeratePhotoTasker extends PhotoGridActivity.EnumeratePhotoTasker {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (cursorLock) {
                Log.d(getClass().getName(), "[doInBackground] " + album);
                // image
                String[] PROJECTION = new String[]{
                        MediaStore.Images.Media.BUCKET_ID,     // 0
                        MediaStore.Images.Media._ID,           // 1  // long
                        MediaStore.Images.Media.DATA,          // 2
                        MediaStore.Images.Media.DISPLAY_NAME}; // 3
                String WHERE_CLAUSE = MediaStore.Images.Media.BUCKET_ID + " = ?";
                String[] WHERE_CLAUSE_ARGS = new String[]{album.id};
                imageCursor = MediaStore.Images.Media.query(
                        getContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI/*.buildUpon()
                                .appendQueryParameter("distinct", "true").build()*/,
                        PROJECTION, WHERE_CLAUSE, WHERE_CLAUSE_ARGS,
                        MediaStore.Images.Media.DISPLAY_NAME + " ASC");
                imageCursor.moveToFirst();
                album.count = imageCursor.getCount();
                Log.d(getClass().getName(), "[doInBackground] image counts=" + album.count + ", cursor=" + imageCursor);
                // video
                videoCursor = MediaStore.Images.Media.query(
                        getContext().getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION, WHERE_CLAUSE, WHERE_CLAUSE_ARGS,
                        MediaStore.Images.Media.DISPLAY_NAME + " ASC");
                videoCursor.moveToFirst();
                int count = videoCursor.getCount();
                Log.d(getClass().getName(), "[doInBackground] video counts=" + count + ", cursor=" + videoCursor);
                album.count += count;
                publishProgress(PROGRESS_CODE_UPDATE_COUNT, album.count);
            }
            return null;
        }
    }

    private class AddToPrivateTasker extends ActionTasker {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (actionTaskerProgressDialogCancelListener == null) {
                actionTaskerProgressDialogCancelListener = new ActionTaskerCancelListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        super.onClick(dialog, which);
                        Log.d(getClass().getName(), "[onClick] cancel clicked");
                        add2PhotoTasker.cancel(true);
                    }
                };
            }
        }

        @Override
        protected Result doInBackground(Integer... params) {
            Log.d(getClass().getName(), "[doInBackground] " + this);
            publishProgress(UPDATE_CODE_SHOW_PROGRESS_DIALOG, params[0]);
            SparseBooleanArray selectedItems = getSelectedItems();
            ArrayList<MediaItem> resultItems =  new ArrayList<MediaItem>();
            FileUtil fileUtil = FileUtil.getInstance(getContext());
            PrivateItemResolver privateItemResolver = PrivateItemResolver.getInstance(getContext());
            File privateDir = getContext().getPrivateFilesDir();
            String albumPath = "";
            boolean replaceAlbumThumbnail = false;
            for (int i = 0 ; i < selectedItems.size() ; i++) {
                if (isCancelled()) {
                    Log.d(getClass().getName(), "[doInBackground] idx=" + i + ", canceled");
                    break;
                }
                int position = selectedItems.keyAt(i);
                MediaItem item = getMediaItem(position);
                Log.d(getClass().getName(), "[doInBackground] idx=" + i + ", " + item);
                String privateAlbumName = getApp().getAdd2PrivateAlbumName().isEmpty() ?
                        album.name : getApp().getAdd2PrivateAlbumName();
                if (fileUtil.moveFile(item.path, new String[]{item.name},
                        privateDir.getPath() + "/" + privateAlbumName,
                        FileUtil.NotifyMediaStoreFileChanged.INPUT_FILE, item.type)) {
                    privateItemResolver.insertItem(item, privateAlbumName);
                    resultItems.add(item);
                    publishProgress(UPDATE_CODE_INCREMENT_DIALOG_PROGRESS);
                    if (item.id == album.thumbnail.id && !replaceAlbumThumbnail) {
                        replaceAlbumThumbnail = true;
                        Log.d(getClass().getName(), "[doInBackground] should replace album thumbnail");
                    }
                }
                if (albumPath.isEmpty()) albumPath = item.path;
            }
            privateItemResolver.printAllItems("["+getClass().getName()+"][doInBackground]");
            for (MediaItem item : resultItems) {
                Log.d(getClass().getName(), "[doInBackground] add2private: " + item);
                removeGridThumbnail(item);
            }
            album.count -= resultItems.size();
            boolean isRemoveAlbum = false;
            if (album.count == 0) {
                fileUtil.deleteFile(albumPath, "");
                isRemoveAlbum = true;
                Log.d(getClass().getName(), "[doInBackground] remove empty album");
            }
            return new Result(album.count, !resultItems.isEmpty(), isRemoveAlbum, replaceAlbumThumbnail);
        }

        @Override
        protected void finishOrCancel(Result result) {
            refreshMediaCursor();
            super.finishOrCancel(result);
        }
    }

    @Override
    public MediaItem getMediaItem(int position) {
        synchronized (cursorLock) {
            if (imageCursor == null || videoCursor == null) {
                Log.e(getClass().getSimpleName(), "[getItem] null cursor, imageCursor=" + imageCursor
                        + ", videoCursor=" + videoCursor);
                return null;
            }
            if ((imageCursor.getCount() != 0 && !imageCursor.moveToFirst()) ||
                    (videoCursor.getCount() != 0 && !videoCursor.moveToFirst())) {
                Log.e(getClass().getSimpleName(), "[getItem] cursor moveToFirst() fail");
                return null;
            }
            int imageCount = imageCursor.getCount();
            if (position < imageCount) { // image
                if (!imageCursor.moveToPosition(position)) {
                    Log.e(getClass().getSimpleName(), "[getItem] imageCursor moveToPosition(" + position + ") fail");
                    return null;
                }
                ImageItem image = new ImageItem();
                image.id = imageCursor.getLong(1);
                image.name = imageCursor.getString(3);
                image.path = imageCursor.getString(2).split("/"+image.name)[0];
                image.position = position;
                Log.d(getClass().getSimpleName(), "[getItem] photo index=" + position + ", id=" + image.id + ", name="
                        + image.name + ", path=" + image.path);
                return image;
            } else { // video
                int videoIndex = position - imageCount;
                if (!videoCursor.moveToPosition(videoIndex)) {
                    Log.e(getClass().getSimpleName(), "[getItem] videoCursor moveToPosition(" + videoIndex + ") fail");
                    return null;
                }
                VideoItem video = new VideoItem();
                video.id = videoCursor.getLong(1);
                video.name = videoCursor.getString(3);
                video.path = videoCursor.getString(2).split("/"+video.name)[0];
                video.position = position;
                Log.d(getClass().getName(), "[getItem] video index=" + videoIndex + ", id=" + video.id + ", name="
                        + video.name + ", path=" + video.path);
                return video;
            }
        }
    }

    @Override
    protected Bitmap getGridThumbnail(MediaItem item) {
        Bitmap thumbnail = null;
        if (item.type == MediaItem.MEDIA_TYPE_IMAGE) {
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(),
                    item.id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
        } else if (item.type == MediaItem.MEDIA_TYPE_VIDEO){
            thumbnail = MediaStore.Video.Thumbnails.getThumbnail(getContext().getContentResolver(),
                    item.id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
        }
        Log.d(getClass().getSimpleName(), "[getGridThumbnail] " + item + ", " + thumbnail);
        return thumbnail;
    }

    @Override
    public void refreshMediaCursor() {
        enumeratePhotoTasker = (EnumeratePhotoTasker)new EnumeratePhotoTasker().execute();
    }
}