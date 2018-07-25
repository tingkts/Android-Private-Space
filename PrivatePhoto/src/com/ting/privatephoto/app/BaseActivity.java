package com.ting.privatephoto.app;

import java.io.File;
import java.util.ArrayList;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.ting.privatephoto.R;
import com.ting.privatephoto.util.Log;

abstract class BaseActivity extends FragmentActivity {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkAndRequestForGallery()) {
            Log.d(getClass().getSimpleName(), "[onCreate] checkAndRequestForGallery() fail!");
        }
    }

    Context getContext() {return this;}
    Activity getActivity() {return this;}
    PrivatePhotoApp getApp() {return (PrivatePhotoApp)getApplication();}

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkAndRequestForGallery() {
        // get permissions needed in current scenario
        ArrayList<String> permissionsNeeded = new ArrayList<String>();
        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsNeeded.add("android.permission.WRITE_MEDIA_STORAGE");
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith("content://mms")) {
            permissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        ArrayList<String> permissionsNeedRequest = new ArrayList<String>();
        for (String permission : permissionsNeeded) {
            if (checkSelfPermission(permission)
                    == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            permissionsNeedRequest.add(permission);
        }
        // request permissions
        if (permissionsNeedRequest.size() == 0) {
            Log.d(getClass().getSimpleName(), "<checkAndRequestForGallery> all permissions are granted");
            return true;
        } else {
            Log.d(getClass().getSimpleName(), "<checkAndRequestForGallery> not all permissions are granted, reuqest");
            String[] permissions = new String[permissionsNeedRequest.size()];
            permissions = permissionsNeedRequest.toArray(permissions);
            requestPermissions(permissions, 0);
            return false;
        }
    }

    boolean checkIfTaskEnded(AsyncTask task, boolean showToast) {
        boolean isTaskEnded = (task.getStatus() == AsyncTask.Status.FINISHED || task.isCancelled());
        if (showToast && !isTaskEnded) {
            Toast.makeText(this, "Wait a moment for data ready", Toast.LENGTH_SHORT).show();
        }
        return isTaskEnded;
    }

    ImageView setTopBarBackIconOnClickResponse(boolean performBackKeyBehavior) {
        ImageView backIcon = (ImageView)findViewById(R.id.id_topbar_title_back_icon);
        if (performBackKeyBehavior) backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "[onClick][titleBarBackIcon]");
                sendBackKey();
            }
        });
        return backIcon;
    }

    void sendBackKey() {
        getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

    ProgressDialog actionTaskerProgressDialog;
    DialogInterface.OnClickListener actionTaskerProgressDialogCancelListener;
    @Override
    protected Dialog onCreateDialog(int id) {
        CharSequence title = "";
        if (id == R.id.id_bottom_bar_add2private) title = getText(R.string.bottom_bar_add2private);
        else if (id == R.id.id_bottom_bar_item_actions_del) title = getText(R.string.bottom_bar_delete);
        else if (id == R.id.id_bottom_bar_item_actions_move) title = getText(R.string.bottom_bar_move);
        else if (id == R.id.id_bottom_bar_item_actions_restore) title = getText(R.string.bottom_bar_restore);
        else return null;
        actionTaskerProgressDialog = new ProgressDialog(this, R.style.AlertDialogTheme);
        actionTaskerProgressDialog.setTitle(title);
        actionTaskerProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        actionTaskerProgressDialog.setCancelable(false);
        if (actionTaskerProgressDialogCancelListener != null) {
            actionTaskerProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getText(R.string.top_bar_cancel), actionTaskerProgressDialogCancelListener);
        }
        return actionTaskerProgressDialog;
    }

    protected abstract class ActionTasker extends AsyncTask<Integer, Object, ActionTasker.Result> {
        final int UPDATE_CODE_SHOW_PROGRESS_DIALOG = 1;
        final int UPDATE_CODE_INCREMENT_DIALOG_PROGRESS = 2;

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int updateCode = (int)values[0];
            Log.d(getClass().getName(), "[onProgressUpdate] updateCode=" + updateCode);
            if (updateCode == UPDATE_CODE_SHOW_PROGRESS_DIALOG) {
                getActivity().showDialog((int)values[1]);
            } else if (updateCode == UPDATE_CODE_INCREMENT_DIALOG_PROGRESS) {
                actionTaskerProgressDialog.incrementProgressBy(1);
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            Log.d(getClass().getName(), "[onPostExecute] albumItemCount=" + result);
            finishOrCancel(result);
        }

        @Override
        protected void onCancelled(Result result) {
            super.onCancelled(result);
            Log.d(getClass().getName(), "[onCancelled] albumItemCount=" + result);
            finishOrCancel(result);
        }

        protected void finishOrCancel(Result result) {
            actionTaskerProgressDialog.dismiss();
        }

        class Result {
            int albumItemCount = 0;
            boolean doesSetResultToAlbumList = false;
            boolean doesRemoveAlbum = false;
            boolean doesReplaceAlbumThumbnail = false;

            public Result(int albumItemCount) {
                this.albumItemCount = albumItemCount;
            }

            public Result(int albumItemCount, boolean doesSetResultToAlbumList,
                          boolean doesRemoveAlbum, boolean doesReplaceAlbumThumbnail) {
                this.albumItemCount = albumItemCount;
                this.doesSetResultToAlbumList = doesSetResultToAlbumList;
                this.doesRemoveAlbum = doesRemoveAlbum;
                this.doesReplaceAlbumThumbnail = doesReplaceAlbumThumbnail;
            }
        }
    }

    // use in-house video player to play private video
    private static final String IN_HOUSE_VIDEO_PLAYER_PACKAGE_NAME = "com.videoplayer";
    private static final String IN_HOUSE_VIDEO_PLAYER_CLASS_NAME = "com.videoplayer.MoviePlayerActivity";
    void playVideo(PrivatePhotoApp.PrivacyType privacyType, String filePathName) {
        if (privacyType == PrivatePhotoApp.PrivacyType.PRIVATE) {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setClassName(IN_HOUSE_VIDEO_PLAYER_PACKAGE_NAME, IN_HOUSE_VIDEO_PLAYER_CLASS_NAME)
                    .setDataAndType(Uri.fromFile(new File(filePathName)), "video/*");
            startActivity(intent);
        } else if (privacyType == PrivatePhotoApp.PrivacyType.PUBLIC) {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(filePathName), "video/*");
            startActivity(intent);
        }
    }

    void showConfirmCancelDialog(DialogInterface.OnClickListener okClickListener) {
        AlertDialog.Builder theBuilder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        theBuilder.setMessage(R.string.confirm_delete);
        theBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
        theBuilder.setPositiveButton(getString(R.string.ok).toUpperCase(), okClickListener);
        theBuilder.setNegativeButton(getString(R.string.top_bar_cancel).toUpperCase(), null);
        theBuilder.create();
        theBuilder.show();
    }
}