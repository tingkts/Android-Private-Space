package com.ting.privatephoto.app;

import java.io.File;
import java.util.ArrayList;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import com.ting.privatephoto.R;
import com.ting.privatephoto.database.PrivateItemProvider;
import com.ting.privatephoto.database.PrivateItemResolver;
import com.ting.privatephoto.media.ImageItem;
import com.ting.privatephoto.media.VideoItem;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.FileUtil;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.view.BottomBar;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_RESULT_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_REQUEST_CODE;

public class PrivatePhotoGridActivity extends PhotoGridActivity {
    private BottomBar bottomBarAdd;
    private BottomBar bottomBarItemActions;
    private ItemActionsTasker itemActionsTasker;

    private Cursor mediaCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreate(PrivacyType.PRIVATE, new EnumeratePhotoTasker());
        bottomBarAdd = new BottomBar(BottomBar.Type.ADD, this, this);
        bottomBarItemActions = new BottomBar(BottomBar.Type.ITEM_ACTIONS_DEL_MOVE_RESTORE, this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaCursor != null) {
            mediaCursor.close();
        }
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
            enumeratePhotoTasker = (EnumeratePhotoTasker)new EnumeratePhotoTasker().execute();
            setResult(START_ACTIVITY_FOR_RESULT_RESULT_CODE, new Intent().putExtra(
                    INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS, true));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSelectionModeChanged(boolean isEnabled) {
        Log.d(getClass().getSimpleName(), "[onSelectionModeChanged] " + isEnabled);
        setSelectionMode(isEnabled);
    }

    @Override
    protected void setSelectionMode(boolean isEnabled) {
        super.setSelectionMode(isEnabled);
        Log.d(getClass().getSimpleName(), "[setSelectionMode] " + isEnabled);
        bottomBarAdd.setVisible(!isEnabled);
        bottomBarItemActions.setVisible(isEnabled);
    }

    @Override
    public void onButtonBarButtonClicked(BottomBar.ActionCode actionCode, final int viewId) {
        if (actionCode == BottomBar.ActionCode.ADD_OR_ADD2PRIVATE) {
            getApp().setAdd2PrivateAlbumName(album.name);
            Intent intent = new Intent();
            intent.setClass(PrivatePhotoGridActivity.this, PublicAlbumListActivity.class);
            startActivityForResult(intent, START_ACTIVITY_FOR_RESULT_REQUEST_CODE);
        }
        else if (actionCode == BottomBar.ActionCode.DELETE || actionCode == BottomBar.ActionCode.MOVE ||
                actionCode == BottomBar.ActionCode.RESTORE) {
            if (actionCode == BottomBar.ActionCode.DELETE) {
                showConfirmCancelDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeItemActionsTasker(viewId);
                    }
                });
            } else {
                executeItemActionsTasker(viewId);
            }
        }
    }

    private void executeItemActionsTasker(int viewId) {
        if (itemActionsTasker == null) {
            (itemActionsTasker = new ItemActionsTasker()).execute(viewId);
        } else {
            itemActionsTasker.cancel(true);
            (itemActionsTasker = new ItemActionsTasker()).execute(viewId);
        }
    }

    private class EnumeratePhotoTasker extends PhotoGridActivity.EnumeratePhotoTasker {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (cursorLock) {
                ContentResolver contentResolver = getContext().getContentResolver();
                mediaCursor = contentResolver.query(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM, null,
                        PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME + " = ?", new String[]{album.name},
                        PrivateItemProvider.COLUMN_FILE_NAME + " ASC");
                if (mediaCursor == null || !mediaCursor.moveToFirst()) {
                    Log.e(getClass().getName(), "[doInBackground] cursor error!");
                    return null;
                }
                int count = mediaCursor.getCount();
                if (count != album.count) {
                    Log.d(getClass().getName(), "[doInBackground] count=" + count);
                    album.count = count;
                    publishProgress(PROGRESS_CODE_UPDATE_COUNT, count);
                }
                return null;
            }
        }
    }

    public static final int ACTION_CODE_DEL = R.id.id_bottom_bar_item_actions_del;
    public static final int ACTION_CODE_MOVE = R.id.id_bottom_bar_item_actions_move;
    public static final int ACTION_CODE_RESTORE = R.id.id_bottom_bar_item_actions_restore;
    public static String actionCodeToString(int actionCode) {
        if (actionCode == ACTION_CODE_DEL)  return "action_del";
        else if (actionCode == ACTION_CODE_MOVE)  return "action_move";
        else if (actionCode == ACTION_CODE_RESTORE)  return "action_restore";
        return "action_none";
    }
    private class ItemActionsTasker extends ActionTasker {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (actionTaskerProgressDialogCancelListener == null) {
                actionTaskerProgressDialogCancelListener = new ActionTaskerCancelListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        super.onClick(dialog, which);
                        Log.d(getClass().getName(), "[onClick] cancel clicked");
                        itemActionsTasker.cancel(true);
                    }
                };
            }
        }

        @Override
        protected Result doInBackground(Integer... params) {
            int actionCode = params[0];
            Log.d(getClass().getName(), "[doInBackground] " +this + ", " + actionCodeToString(actionCode));
            publishProgress(UPDATE_CODE_SHOW_PROGRESS_DIALOG, actionCode);
            SparseBooleanArray selectedItems = getSelectedItems();
            ArrayList<MediaItem> doneActionItems =  new ArrayList<MediaItem>();
            FileUtil fileUtil = FileUtil.getInstance(getContext());
            PrivateItemResolver privateItemResolver = PrivateItemResolver.getInstance(getContext());
            File privateDir = getContext().getPrivateFilesDir();
            boolean replaceAlbumThumbnail = false;
            for (int i = 0 ; i < selectedItems.size() ; i++) {
                if (isCancelled()) {
                    Log.d(getClass().getName(), "[doInBackground] idx=" + i + ", canceled");
                    break;
                }
                int position = selectedItems.keyAt(i);
                MediaItem item = getMediaItem(position);
                Log.d(getClass().getName(), "[doInBackground] idx=" + i + ", " + item);
                if ((actionCode == ACTION_CODE_DEL &&
                        fileUtil.deleteFile(privateDir.getPath()+"/"+album.name, item.name)) ||
                        (actionCode == ACTION_CODE_RESTORE && fileUtil.moveFile(
                                privateDir.getPath()+"/"+album.name, new String[]{item.name},
                                item.path, FileUtil.NotifyMediaStoreFileChanged.OUTPUT_FILE, item.type))) {
                    publishProgress(UPDATE_CODE_INCREMENT_DIALOG_PROGRESS);
                    privateItemResolver.deleteItem(item.id);
                    doneActionItems.add(item);
                    if (item.id == album.thumbnail.id && !replaceAlbumThumbnail) {
                        replaceAlbumThumbnail = true;
                        Log.d(getClass().getName(), "[doInBackground] should replace album thumbnail");
                    }
                }
            }
            privateItemResolver.printAllItems("["+getClass().getName()+"][doInBackground]");
            for (MediaItem item : doneActionItems) {
                Log.d(getClass().getName(), "[doInBackground] remove " + item);
                removeGridThumbnail(item);
            }
            album.count -= doneActionItems.size();
            boolean isRemoveAlbum = false;
            if (album.count == 0 && fileUtil.deleteFile(privateDir+"/"+album.name, "")) {
                isRemoveAlbum = true;
                Log.d(getClass().getName(), "[doInBackground] remove empty album");
            }
            return new Result(album.count, !doneActionItems.isEmpty(), isRemoveAlbum, replaceAlbumThumbnail);
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
            MediaItem item = null;
            if (!mediaCursor.moveToFirst()) {
                Log.d(getClass().getSimpleName(), "[getMediaItem] position=" + position
                        + "move cursor to first fail");
                return null;
            }
            if (!mediaCursor.moveToPosition(position)) {
                Log.e(getClass().getSimpleName(), "[getMediaItem] mediaCursor moveToPosition("
                        + position + ") fail");
                return null;
            }
            int type = mediaCursor.getInt(mediaCursor.getColumnIndex(PrivateItemProvider.COLUMN_TYPE));
            if (type == MediaItem.MEDIA_TYPE_IMAGE) {
                item = new ImageItem();
            } else if (type == MediaItem.MEDIA_TYPE_VIDEO) {
                item = new VideoItem();
            }
            item.position = position;
            item.id = mediaCursor.getLong(mediaCursor.getColumnIndex(PrivateItemProvider.COLUMN_ID));
            item.name = mediaCursor.getString(mediaCursor.getColumnIndex(PrivateItemProvider.COLUMN_FILE_NAME));
            item.path = mediaCursor.getString(mediaCursor.getColumnIndex(PrivateItemProvider.COLUMN_FILE_PATH));
//        Bitmap microThumbnail = PrivateItemResolver.getInstance(getContext()).getThumbnail(mediaCursor,
//                mediaCursor.getColumnIndex(PrivateItemProvider.COLUMN_MICRO_THUMBNAIL));
//        if (microThumbnail != null) {
//            album.itemThumbnails.put(item.id, microThumbnail);
//            publishProgress(PROGRESS_CODE_UPDATE_ITEM, i, microThumbnail);
//        }
//        album.items.aadd(item);
            Log.d(getClass().getName(), "[getMediaItem] position=" + position + ", " + item);
            return item;
        }
    }

    @Override
    protected Bitmap getGridThumbnail(MediaItem item) {
        Bitmap thumbnail = null;
        try {
            File privateDir = getContext().getPrivateFilesDir();
            String filePathName = privateDir.getPath() + "/" + album.name + "/" + item.name;
            if (item.type == MediaItem.MEDIA_TYPE_IMAGE) {
                thumbnail = decodeBitmapFromImage(filePathName);
            } else if (item.type == MediaItem.MEDIA_TYPE_VIDEO) {
                thumbnail = decodeBitmapFromVideo(filePathName);
            }
        } catch (Exception e) { // catch IO Exception
            Log.d(getClass().getSimpleName(), "[getGridThumbnail]");
        }
        return thumbnail;
    }

    @Override
    public void refreshMediaCursor() {
        enumeratePhotoTasker = (EnumeratePhotoTasker)new EnumeratePhotoTasker().execute();
    }
}