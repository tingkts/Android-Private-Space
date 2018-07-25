package com.ting.privatephoto.app;

import java.io.File;
import android.content.DialogInterface;
import android.os.Bundle;
import com.ting.privatephoto.R;
import com.ting.privatephoto.database.PrivateItemResolver;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.FileUtil;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.view.BottomBar;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import static com.ting.privatephoto.app.PrivatePhotoGridActivity.ACTION_CODE_DEL;
import static com.ting.privatephoto.app.PrivatePhotoGridActivity.ACTION_CODE_RESTORE;
import static com.ting.privatephoto.app.PrivatePhotoGridActivity.actionCodeToString;

public class PrivatePhotoPaperActivity extends PhotoPaperActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getSimpleName(), "[onCreate]");
        onCreate(PrivacyType.PRIVATE, this);
    }

    @Override
    public void onButtonBarButtonClicked(BottomBar.ActionCode actionCode, final int viewId) {
        Log.d(getClass().getSimpleName(), "[onButtonBarButtonClicked] actionCode=" + actionCode
                + ", viewId=" + viewId);
        if (actionCode == BottomBar.ActionCode.DELETE) {
            showConfirmCancelDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new ActionTasker().execute(viewId);
                }
            });
        } else {
            new ActionTasker().execute(viewId);
        }
    }

    private class ActionTasker extends PhotoPaperActivity.ActionTasker {
        @Override
        protected Result doInBackground(Integer... params) {
            int actionCode = params[0];
            Log.d(getClass().getName(), "[doInBackground] " + actionCodeToString(actionCode));
            publishProgress(UPDATE_CODE_SHOW_PROGRESS_DIALOG, actionCode);
            int position = photoPager.getCurrentItem();
            Log.d(getClass().getName(), "[doInBackground] position=" + position);
            MediaItem item = gridActivity.getMediaItem(position);
            FileUtil fileUtil = FileUtil.getInstance(getContext());
            PrivateItemResolver privateItemResolver = PrivateItemResolver.getInstance(getContext());
            File privateDir = getContext().getPrivateFilesDir();
            if ((actionCode == ACTION_CODE_DEL &&
                    fileUtil.deleteFile(privateDir.getPath()+"/"+album.name, item.name)) ||
                    (actionCode == ACTION_CODE_RESTORE && fileUtil.moveFile(
                            privateDir.getPath()+"/"+album.name, new String[]{item.name},
                            item.path, FileUtil.NotifyMediaStoreFileChanged.OUTPUT_FILE, item.type))) {
                privateItemResolver.deleteItem(item.id);
                publishProgress(UPDATE_CODE_INCREMENT_DIALOG_PROGRESS);
                notifyDataChange |= NOTIFY_DATA_CHANGE_DATA_SET_CHANGED;
                album.count--;
                if (item.id == album.thumbnail.id) {
                    Log.d(getClass().getName(), "[doInBackground] should replace album thumbnail");
                    album.thumbnail.id = 0;
                    notifyDataChange |= NOTIFY_DATA_CHANGE_REPLACE_ALBUM_THUMBNAIL;
                }
            }
            gridActivity.removeGridThumbnail(item);
            if (album.count == 0 && fileUtil.deleteFile(privateDir+"/"+album.name, "")) {
                Log.d(getClass().getName(), "[doInBackground] remove empty album");
            }
            privateItemResolver.printAllItems("["+getClass().getName()+"][doInBackground]");
            if (album.count == 0) return new Result(-1);
            return new Result(position);
        }
    }
}