package com.ting.privatephoto.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Message;
import android.util.DisplayMetrics;
import android.widget.AbsListView;
import android.widget.Toast;
import com.ting.privatephoto.util.Log;

abstract class ListGridActivity extends BaseActivity {
    protected static final int MESSAGE_CHECK_IF_LIST_VIEW_READY = 1;
    private static int REQUESTED_ITEM_BITMAP_SIZE = 0;
    private Handler handler;
    private Object lock;

    protected void onCreate(Handler _handler) {
        handler = _handler;
        lock = new Object();
        calculateRequestItemBitmapSize();
    }

    protected void checkListGridViewReady(AbsListView listView, long msgDelayMillis, long objWaitTimeout) {
        synchronized(lock) {
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_CHECK_IF_LIST_VIEW_READY,
                    1, (int)msgDelayMillis, listView), msgDelayMillis);
            try {lock.wait(objWaitTimeout);} catch (InterruptedException e) {Log.printStackTrace(e);}
        }
    }

    protected class Handler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MESSAGE_CHECK_IF_LIST_VIEW_READY) {
                int retryCount = msg.arg1;
                int msgDelayMillis = msg.arg2;
                AbsListView listView = (AbsListView)msg.obj;
                int firstPosition = listView.getFirstVisiblePosition();
                int lastPosition = listView.getLastVisiblePosition();
                int childCount = listView.getChildCount();
                if (firstPosition >= 0 && lastPosition >=0 && childCount == (lastPosition-firstPosition+1)) {
                    synchronized(lock) {lock.notifyAll();}
                } else {
                    if (retryCount < 3) {handler.sendMessageDelayed(handler.obtainMessage(
                            MESSAGE_CHECK_IF_LIST_VIEW_READY, retryCount++, msgDelayMillis,
                            listView), msgDelayMillis); }
                    else {synchronized(lock) {lock.notifyAll();}}
                }
                Log.d(getClass().getSimpleName(), "[MESSAGE_CHECK_IF_LIST_VIEW_READY] retryCount= "
                        + retryCount + ", firstPosition=" + firstPosition + ", lastPosition="
                        + lastPosition + ", childCount=" + childCount);
            }
        }
    }

    private Toast toastWaitDataReadyOnListGridScroll;
    protected void showToastWaitDataReadyOnListGridScroll(AbsListView listView, final AsyncTask tasker) {
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.d(getClass().getName(), "[onScrollStateChanged] scrollState=" + scrollState);
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING) {
                    boolean isTaskEnded = checkIfTaskEnded(tasker, false);
                    if (isTaskEnded && toastWaitDataReadyOnListGridScroll != null) {
                        toastWaitDataReadyOnListGridScroll.cancel();}
                    else if (!isTaskEnded) {
                        if (toastWaitDataReadyOnListGridScroll == null) {
                            toastWaitDataReadyOnListGridScroll = Toast.makeText(getContext(),
                                "Wait a moment for data ready", Toast.LENGTH_SHORT);}
                        toastWaitDataReadyOnListGridScroll.show();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
    }

    private int calculateRequestItemBitmapSize() {
        if (REQUESTED_ITEM_BITMAP_SIZE == 0) {
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            REQUESTED_ITEM_BITMAP_SIZE = (int) (80*displayMetrics.scaledDensity);
            Log.d(getClass().getSimpleName(), "[calculateRequestItemBitmapSize] "
                    + "REQUESTED_ITEM_BITMAP_SIZE=" + REQUESTED_ITEM_BITMAP_SIZE);
        }
        return REQUESTED_ITEM_BITMAP_SIZE;
    }

    protected Bitmap decodeBitmapFromImage(String filePathName) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePathName, options);
        Log.d(getClass().getSimpleName(), "[decodeBitmapFromImage] " + filePathName
                + ", bound size= " + options.outWidth + " x " + options.outHeight);
        // Calculate inSampleSize
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        int targetSide = options.outWidth <= options.outHeight ? options.outWidth : options.outWidth;
        inSampleSize = targetSide/REQUESTED_ITEM_BITMAP_SIZE;
        options.inSampleSize = inSampleSize;
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap returnBitmap = BitmapFactory.decodeFile(filePathName, options);
        Log.d(getClass().getSimpleName(), "[decodeBitmapFromImage] " + filePathName
                + ", sample size= " + options.inSampleSize
                + ", bitmap size= " + returnBitmap.getWidth() + " x " + returnBitmap.getHeight());
        return returnBitmap;
    }

    protected Bitmap decodeBitmapFromVideo(String filePathName) {
        Bitmap videoFrame = null;
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(filePathName);
            byte[] data = mediaMetadataRetriever.getEmbeddedPicture();
            if (data != null) {
                videoFrame = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            if (videoFrame == null) {
                videoFrame = mediaMetadataRetriever.getFrameAtTime();
            }
        } catch (IllegalArgumentException e) {
            Log.e(getClass().getName(), "[decodeBitmapFromVideo] " + filePathName, e);
        }
        // createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter)
        videoFrame = Bitmap.createScaledBitmap(videoFrame, REQUESTED_ITEM_BITMAP_SIZE,
                REQUESTED_ITEM_BITMAP_SIZE, false);
        return videoFrame;
    }
}