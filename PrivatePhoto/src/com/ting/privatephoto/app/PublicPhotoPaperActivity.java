package com.ting.privatephoto.app;

import java.io.File;
import android.os.Bundle;
import com.ting.privatephoto.R;
import com.ting.privatephoto.database.PrivateItemResolver;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.FileUtil;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.view.BottomBar;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
//import com.ting.privatephoto.app.PrivatePhotoApp.PublicCacheThumbnail;

public class PublicPhotoPaperActivity extends PhotoPaperActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getSimpleName(), "[onCreate]");
        onCreate(PrivacyType.PUBLIC, this);
    }

    @Override
    public void onButtonBarButtonClicked(BottomBar.ActionCode actionCode, int viewId) {
        Log.d(getClass().getSimpleName(), "[onButtonBarButtonClicked] actionCode=" + actionCode
                + ", viewId=" + viewId);
        new ActionTasker().execute(viewId);
    }

    class ActionTasker extends PhotoPaperActivity.ActionTasker {
        @Override
        protected Result doInBackground(Integer... params) {
            publishProgress(UPDATE_CODE_SHOW_PROGRESS_DIALOG, params[0]);
            int position = photoPager.getCurrentItem();
            Log.d(getClass().getName(), "[doInBackground] position=" + position);
            MediaItem item = gridActivity.getMediaItem(position);
            FileUtil fileUtil = FileUtil.getInstance(getContext());
            PrivateItemResolver privateItemResolver = PrivateItemResolver.getInstance(getContext());
            File privateDir = getContext().getPrivateFilesDir();
            String privateAlbumName = getApp().getAdd2PrivateAlbumName().isEmpty() ?
                    album.name : getApp().getAdd2PrivateAlbumName();
            if (fileUtil.moveFile(item.path, new String[]{item.name},
                    privateDir.getPath()+"/"+privateAlbumName,
                    FileUtil.NotifyMediaStoreFileChanged.INPUT_FILE, item.type)) {
                Log.d(getClass().getName(), "[doInBackground] add2private: " + item);
                privateItemResolver.insertItem(item, privateAlbumName/*,
                        album.itemThumbnails.get(item.id)*/);
                publishProgress(UPDATE_CODE_INCREMENT_DIALOG_PROGRESS);
                notifyDataChange |= NOTIFY_DATA_CHANGE_DATA_SET_CHANGED;
                album.count--;
                if (item.id == album.thumbnail.id) {
                    Log.d(getClass().getName(), "[doInBackground] should replace album thumbnail");
                    album.thumbnail.id = 0;
                    notifyDataChange |= NOTIFY_DATA_CHANGE_REPLACE_ALBUM_THUMBNAIL;
                }
                gridActivity.removeGridThumbnail(item);
                if (album.count == 0) {
                    Log.d(getClass().getName(), "[doInBackground] remove empty album");
                    fileUtil.deleteFile(item.path, "");
                }
            }
            privateItemResolver.printAllItems("["+getClass().getName()+"][doInBackground]");
            if (album.count == 0) return new Result(-1);
            return new Result(position);
        }
    }
}