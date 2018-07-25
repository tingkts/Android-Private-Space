package com.ting.privatephoto.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ting.privatephoto.R;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.media.Album.ThumbnailEntry;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.BitmapCache;
import com.ting.privatephoto.util.Config;
import com.ting.privatephoto.util.ImageWorker;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import com.ting.privatephoto.app.PrivatePhotoApp.PaperSharedAlbum;
import com.ting.privatephoto.view.BottomBar;
import static com.ting.privatephoto.media.MediaItem.MEDIA_TYPE_IMAGE;
import static com.ting.privatephoto.media.MediaItem.MEDIA_TYPE_VIDEO;
import static com.ting.privatephoto.media.MediaItem.UNDEFINED_ID_INDEX;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_REQUEST_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_RESULT_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_BUCKET_ID;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_COUNT;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_NAME;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_POSITION;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_IS_REMOVE_ALBUM;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ITEM_POSITION;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_THUMBNAIL;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_REFRESH_PHOTO_GRIDS;

abstract class PhotoGridActivity extends ListGridActivity
        implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener,
        BottomBar.OnButtonClickListener{
    private PrivacyType privacyType;
    private TopBar topBar;
    private GridView photoGrids;
    private PhotoGridViewAdapter photoGridsAdapter;
    private ProgressBar progressCycle;
    private Handler handler;
    private SelectionMode selectionMode;
    protected EnumeratePhotoTasker enumeratePhotoTasker;
    protected Album album;

    private ImageWorker imageWorker;
    protected final Object cursorLock = new Object();

    protected void onCreate(PrivacyType type, EnumeratePhotoTasker enumPhotoTasker) {
        onCreate(handler = new Handler());
        setContentView(R.layout.layout_activity_photogrid);
        privacyType = type;
        topBar = new TopBar();
        setTopBarBackIconOnClickResponse(true);
        photoGrids = (GridView) findViewById(R.id.id_gridview_photo);
        photoGrids.setAdapter(photoGridsAdapter = new PhotoGridViewAdapter());
        photoGrids.setOnItemLongClickListener(this);
        photoGrids.setOnItemClickListener(this);
        progressCycle = (ProgressBar) findViewById(R.id.id_progress_cycle);
        selectionMode = new SelectionMode();
        album = new Album();
        album.position = getIntent().getIntExtra(INTENT_EXTRA_KEY_ALBUM_POSITION, UNDEFINED_ID_INDEX);;
        album.id = getIntent().getStringExtra(INTENT_EXTRA_KEY_ALBUM_BUCKET_ID);
        album.name = getIntent().getStringExtra(INTENT_EXTRA_KEY_ALBUM_NAME);
        album.count = getIntent().getIntExtra(INTENT_EXTRA_KEY_ALBUM_COUNT, 0);
        album.thumbnail.id = getIntent().getLongExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, UNDEFINED_ID_INDEX);
        PaperSharedAlbum.getInstance().setAlbum(album);
        PaperSharedAlbum.getInstance().setGridActivity(this);
        enumeratePhotoTasker = (EnumeratePhotoTasker)enumPhotoTasker.execute();
        topBar.setMode(false);
        photoGridsAdapter.setCount(album.count);
        showToastWaitDataReadyOnListGridScroll(photoGrids, enumPhotoTasker);

        imageWorker = new ImageWorker(this) {
            @Override
            protected Bitmap processBitmap(MediaItem data) {
                return getGridThumbnail(data);
            }
        };
        imageWorker.addImageCache(BitmapCache.newInstance(0.25f));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setSelectionMode(false);
        if (PaperSharedAlbum.getInstance().getAlbum() == null) {
            PaperSharedAlbum.getInstance().setAlbum(album);
        }
        if (PaperSharedAlbum.getInstance().getGridActivity() == null) {
            PaperSharedAlbum.getInstance().setGridActivity(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        imageWorker.setExitTasksEarly(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        imageWorker.setPauseWork(false);
        imageWorker.setExitTasksEarly(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enumeratePhotoTasker.cancel(true);
        PaperSharedAlbum.getInstance().setAlbum(null);
        PaperSharedAlbum.getInstance().setGridActivity(null);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        PhotoGridViewAdapter.ViewHolder viewHolder = (PhotoGridViewAdapter.ViewHolder) view.getTag();
        Log.d(getClass().getSimpleName(), "[onItemLongClick] position=" + position
                + ", isSelectionMode=" + selectionMode.isEnabled() + " ("
                + selectionMode.getItemCount() + ")" + ", isItemSelected="
                + selectionMode.isItemSelected(position));
        if (!checkIfTaskEnded(enumeratePhotoTasker, true)) return true;
        if (!selectionMode.isEnabled()) {
            selectionMode.enter();
        }
        if (!selectionMode.isItemSelected(position)) {
            selectionMode.addItem(position);
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PhotoGridViewAdapter.ViewHolder viewHolder = (PhotoGridViewAdapter.ViewHolder) view.getTag();
        Log.d(getClass().getSimpleName(), "[onItemClick] position=" + position
                + ", isSelectionMode=" + selectionMode.isEnabled() + " ("
                + selectionMode.getItemCount() + ")" + ", isItemSelected="
                + selectionMode.isItemSelected(position));
        if (!checkIfTaskEnded(enumeratePhotoTasker, true)) return;
        if (!selectionMode.isEnabled()) { // enter single photo preview activity
            MediaItem item = getMediaItem(position);
            if (item.type == MEDIA_TYPE_IMAGE) {
                onItemClickedLaunchPhotoPaperActivity(position);
            } else if (item.type == MEDIA_TYPE_VIDEO) {
                String filePathName = null;
                if (privacyType == PrivacyType.PRIVATE) {
                    filePathName = getContext().getPrivateFilesDir().getPath()+"/"+album.name+"/"+item.name;
                } else if (privacyType == PrivacyType.PUBLIC) {
                    filePathName = item.path+"/"+item.name;
                }
                playVideo(privacyType, filePathName);
            }
        } else { // in selection mode
            if (selectionMode.isItemSelected(position) && selectionMode.getItemCount() == 1) {
                selectionMode.removeItem(position);
                selectionMode.exit();
            } else {
                selectionMode.toggle(position);
            }
        }
    }

    private void onItemClickedLaunchPhotoPaperActivity(int position) {
        Intent intent = new Intent();
        if (privacyType == PrivacyType.PRIVATE) {
            intent.setClass(this, PrivatePhotoPaperActivity.class);}
        else if (privacyType == PrivacyType.PUBLIC) {
            intent.setClass(this, PublicPhotoPaperActivity.class);}
        intent.putExtra(INTENT_EXTRA_KEY_ITEM_POSITION, position);
        startActivityForResult(intent, START_ACTIVITY_FOR_RESULT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != START_ACTIVITY_FOR_RESULT_REQUEST_CODE ||
                resultCode != START_ACTIVITY_FOR_RESULT_RESULT_CODE || data == null) {
            return;
        }
        boolean isRemoveAlbum = data.getBooleanExtra(INTENT_EXTRA_KEY_IS_REMOVE_ALBUM, false);
        boolean isDataSetChanged = data.getBooleanExtra(INTENT_EXTRA_KEY_REFRESH_PHOTO_GRIDS, false);
        long replacedAlbumThumbnailId = data.getLongExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, UNDEFINED_ID_INDEX);
        Log.d(getClass().getSimpleName(), "[onActivityResult] isRemoveAlbum=" + isRemoveAlbum +
                ", isDataSetChanged=" + isDataSetChanged + ", replacedAlbumThumbnailId="
                + replacedAlbumThumbnailId);
        photoGridsAdapter.setCount(album.count);
        topBar.textTitle.setText(album.name+" ("+album.count+")");
        if (isRemoveAlbum) {
            setReusltToAlbumListActivity(album, true, false);
            if (privacyType == PrivacyType.PUBLIC) sendBackKeyDelayed();
        } else if (isDataSetChanged) {
            setReusltToAlbumListActivity(album, false, replacedAlbumThumbnailId != UNDEFINED_ID_INDEX);
        }
    }

    @Override
    public void onBackPressed() {
        if (selectionMode.isEnabled()) {
            selectionMode.exit();
        } else {
            if (album.count == 0) { setReusltToAlbumListActivity(album, true, false); }
            super.onBackPressed();
        }
    }

    abstract protected void onSelectionModeChanged(boolean isEnabled);

    protected void setSelectionMode(boolean isEnabled) {
        Log.d(getClass().getSimpleName(), "[setSelectionMode] " + isEnabled);
        if (isEnabled) {
            selectionMode.enter();
        } else {
            selectionMode.exit();
        }
    }

    protected SparseBooleanArray getSelectedItems() {
        return selectionMode.getItems();
    }

    protected void setReusltToAlbumListActivity(Album album, boolean removeAlbum,
            boolean replaceAlbumThumbnail) {
        ThumbnailEntry thumbnailEntry = new ThumbnailEntry();
        if (replaceAlbumThumbnail) {
            thumbnailEntry = getAlbumThumbnail();
        }
        Log.d(getClass().getSimpleName(), "[setReusltToAlbumListActivity] " + album + ", "
                + (removeAlbum ? "remove album" : "") + ", thumbnail=" + thumbnailEntry);
        getActivity().setResult(
            START_ACTIVITY_FOR_RESULT_RESULT_CODE,
            new Intent().putExtra(INTENT_EXTRA_KEY_ALBUM_POSITION, album.position).
                putExtra(INTENT_EXTRA_KEY_ALBUM_BUCKET_ID, album.id).
                putExtra(INTENT_EXTRA_KEY_ALBUM_NAME, album.name).
                putExtra(INTENT_EXTRA_KEY_ALBUM_COUNT, album.count).
                putExtra(INTENT_EXTRA_KEY_IS_REMOVE_ALBUM, removeAlbum).
                putExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, thumbnailEntry.id).
                putExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL, thumbnailEntry.self));
    }

    protected void checkListGridViewReady() {
        checkListGridViewReady(photoGrids, 150, 500);
    }

    private void updateGirdViewByPosition(int position, Bitmap thumbnail) {
        if (thumbnail == null)  return;
        int firstVisiblePosition = photoGrids.getFirstVisiblePosition();
        int lastVisiblePosition = photoGrids.getLastVisiblePosition();
        int visibleIndex = position-firstVisiblePosition;
        View v = photoGrids.getChildAt(visibleIndex);
        if (v != null) {
            PhotoGridViewAdapter.ViewHolder vHolder = (PhotoGridViewAdapter.ViewHolder) v.getTag();
            vHolder.image.setImageBitmap(thumbnail);
        }
        Log.d(getClass().getSimpleName(), "[updateGirdViewByPosition] position=" + position
                + ", thumbnail=" + thumbnail + ", visiblePosition="+ visibleIndex
                + ", firstVisiblePosition=" + firstVisiblePosition + ", lastVisiblePosition="
                + lastVisiblePosition + "view=" + v);
    }

    protected class TopBar {
        TextView textCancel;
        TextView textTitle;
        TextView textSelectAll;

        public TopBar() {
            textCancel = (TextView) PhotoGridActivity.this.findViewById(R.id.id_topbar_cancel);
            textTitle = (TextView) PhotoGridActivity.this.findViewById(R.id.id_topbar_title);
            textSelectAll = (TextView) PhotoGridActivity.this.findViewById(R.id.id_topbar_select_all);
            textCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getSimpleName(), "top bar \"cancel\" clicked");
                    selectionMode.selectAllOrCancel(false);
                }
            });
            textSelectAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getSimpleName(), "top bar \"select all\" clicked");
                    selectionMode.selectAllOrCancel(true);
                }
            });
        }

        void setMode(boolean isSelectionMode) {
            if (isSelectionMode) {
                textCancel.setVisibility(View.VISIBLE);
                textSelectAll.setVisibility(View.VISIBLE);
                textTitle.setText(String.format(getString(R.string.top_bar_select_n), 0));
            } else {
                textCancel.setVisibility(View.INVISIBLE);
                textSelectAll.setVisibility(View.INVISIBLE);
                textTitle.setText(album.name + " (" + album.count + ")");
            }
        }
    }

    protected class SelectionMode {
        private boolean enabled;
        private SparseBooleanArray items = new SparseBooleanArray();
        private Vibrator vibrator;
        private final long[] LONG_PRESS_VIBE_PATTERN = {0, 1, 20, 21};

        public SelectionMode() {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        boolean isEnabled() {
            return enabled;
        }

        void enter() {
            if (!enabled) {
                enabled = true;
                vibrator.vibrate(LONG_PRESS_VIBE_PATTERN, -1);
                topBar.setMode(true);
                items.clear();
                onSelectionModeChanged(true);
            }
        }

        void exit() {
            if (enabled) {
                enabled = false;
                topBar.setMode(false);
                items.clear();
                photoGridsAdapter.notifyDataSetChanged();
                onSelectionModeChanged(false);
            }
        }

        void addItem(int position) {
            items.put(position, true);
            topBar.textTitle.setText(String.format(getString(R.string.top_bar_select_n), items.size()));
            photoGridsAdapter.notifyDataSetChanged();
        }

        void removeItem(int position) {
            items.delete(position);
            topBar.textTitle.setText(String.format(getString(R.string.top_bar_select_n), items.size()));
            photoGridsAdapter.notifyDataSetChanged();
        }

        int getItemCount() {
            return items.size();
        }

        SparseBooleanArray getItems() { return items; }

        boolean isItemSelected(int position) {
            return items.get(position);
        }

        void toggle(int position) {
            if (isItemSelected(position)) {
                removeItem(position);
            } else {
                addItem(position);
            }
        }

        void selectAllOrCancel(boolean isSelectedAll) {
            if (isSelectedAll) {
                for (int i = 0 ; i < photoGridsAdapter.getCount() ; i++) {
                    items.put(i, true);
                }
            } else {
                items.clear();
            }
            topBar.textTitle.setText(String.format(getString(R.string.top_bar_select_n), items.size()));
            photoGridsAdapter.notifyDataSetChanged();
        }
    }

    protected class PhotoGridViewAdapter extends BaseAdapter {
        private int count;

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(PhotoGridActivity.this).inflate(R.layout.layout_gridview_item_photo, null);
                viewHolder.root = (FrameLayout) convertView.findViewById(R.id.id_gridview_item_root);
                viewHolder.image = (ImageView) convertView.findViewById(R.id.id_gridview_item_photo);
                viewHolder.hook = (ImageView) convertView.findViewById(R.id.id_gridview_item_selected_hook);
                viewHolder.videoIcon = (ImageView) convertView.findViewById(R.id.id_gridview_item_video_icon);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Log.d(getClass().getName(), "[getView] position=" + position + ", imageView=" + viewHolder.image);
            if (!Config.DEBUG_UI) viewHolder.image.setImageBitmap(null);
            viewHolder.hook.setVisibility(selectionMode.isItemSelected(position) ? View.VISIBLE : View.INVISIBLE);
            MediaItem item = getMediaItem(position);
            if (item.type == MEDIA_TYPE_IMAGE) { viewHolder.videoIcon.setVisibility(View.INVISIBLE); }
            else if (item.type == MEDIA_TYPE_VIDEO) { viewHolder.videoIcon.setVisibility(View.VISIBLE); }
            imageWorker.loadImage(item, viewHolder.image);
            return convertView;
        }

        void setCount(int count) {
            this.count = count;
            notifyDataSetChanged();
        }

        class ViewHolder {
            FrameLayout root;
            ImageView image;
            ImageView hook;
            ImageView videoIcon;
        }
    }

    protected abstract class EnumeratePhotoTasker extends AsyncTask<Void, Object, Void> {
        protected final int PROGRESS_CODE_UPDATE_COUNT = 1;
        protected final int PROGRESS_CODE_UPDATE_ITEM = 2;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(getClass().getName(), "[onPreExecute]");
            if (Config.SHOW_CYCLE_PROGRESS) {
                progressCycle.setVisibility(View.VISIBLE);
                //photoGrids.setEnabled(false);
            }
        }

        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int progressCode = (int)values[0];
            if (progressCode == PROGRESS_CODE_UPDATE_COUNT) {
                int count = (int)values[1];
                Log.d(getClass().getName(), "[onProgressUpdate] progressCode=" + progressCode
                        + ", count=" + count);
                photoGridsAdapter.setCount(count);
                topBar.setMode(false);
            } else if (progressCode == PROGRESS_CODE_UPDATE_ITEM) {
                Log.d(getClass().getName(), "[onProgressUpdate] progressCode=" + progressCode
                        + ", position=" + (int)values[1]);
                updateGirdViewByPosition((int) values[1], (Bitmap) values[2]);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(getClass().getName(), "[onPostExecute]");
            taskEnded();
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            Log.d(getClass().getName(), "[onCancelled]");
            taskEnded();
        }

        private void taskEnded() {
            if (Config.SHOW_CYCLE_PROGRESS) {
                progressCycle.setVisibility(View.INVISIBLE);
                //photoGrids.setEnabled(true);
            }
            photoGridsAdapter.notifyDataSetChanged();
        }
    }

    protected abstract class ActionTasker extends BaseActivity.ActionTasker {
        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int updateCode = (int)values[0];
            if (updateCode == UPDATE_CODE_SHOW_PROGRESS_DIALOG) {
                if (actionTaskerProgressDialog != null) {
                    actionTaskerProgressDialog.setMax(getSelectedItems().size());
                    actionTaskerProgressDialog.setProgress(0);
                    actionTaskerProgressDialog.show();
                }
            }

        }

        protected void finishOrCancel(final Result result) {
            if (result.doesSetResultToAlbumList) {
                if (result.doesReplaceAlbumThumbnail) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean isTaskerEnded = false;
                            int retryCount = 0;
                            do {
                                isTaskerEnded = checkIfTaskEnded(enumeratePhotoTasker, false);
                                retryCount++;
                                if (!isTaskerEnded) {
                                    try {
                                        Thread.sleep(150);
                                    } catch (InterruptedException e) {
                                        Log.d(getClass().getName(), "[finishOrCancel_thread] retryCount=" + retryCount
                                                + ", sleep interrupted, " + e);
                                    }
                                }
                            } while (!isTaskerEnded && retryCount <= 100);
                            Log.d(getClass().getName(), "[finishOrCancel_thread] retryCount = " + retryCount
                                    + ", isTaskerEnded=" + isTaskerEnded);
                            if (isTaskerEnded) {
                                setReusltToAlbumListActivity(album, result.doesRemoveAlbum,
                                        result.doesReplaceAlbumThumbnail);
                            }
                        }
                    }).start();
                } else {
                    setReusltToAlbumListActivity(album, result.doesRemoveAlbum, false);
                }
            }

            super.finishOrCancel(result);
            setSelectionMode(false);
            int albumItemCount = result.albumItemCount;
            photoGridsAdapter.setCount(albumItemCount);
            if (albumItemCount == 0 && privacyType == PrivacyType.PUBLIC) {
                sendBackKeyDelayed();
            }
            progressCycle.setVisibility(View.INVISIBLE);
            photoGrids.setEnabled(true);
        }
    }

    class ActionTaskerCancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.d(getClass().getName(), "[onClick] cancel clicked");
            progressCycle.setVisibility(View.VISIBLE);
            photoGrids.setEnabled(false);
        }
    }    
    
    private void sendBackKeyDelayed() {
        handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendBackKey();
                }}, 100);
    }

    public abstract MediaItem getMediaItem(int position);
    protected abstract Bitmap getGridThumbnail(MediaItem item);
    public abstract void refreshMediaCursor();

    protected ThumbnailEntry getAlbumThumbnail() {
        MediaItem item = getMediaItem(0);
        if (item == null) {
            return new ThumbnailEntry();
        }
        Bitmap thumbnail = imageWorker.getImage(item.id);
        if (thumbnail == null) {
            int retryCount = 0;
            do {
                thumbnail = getGridThumbnail(item);
                if (thumbnail != null) break;
                else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.d(getClass().getSimpleName(), "[getAlbumThumbnail] retryCount=" + retryCount
                                + ", " + item + "sleep interrupted, " + e);
                    }
                }
                retryCount++;
                Log.d(getClass().getSimpleName(), "[getAlbumThumbnail] retryCount=" + retryCount
                        + ", " + item);
            } while (retryCount <= 3);
        }
        ThumbnailEntry thumbnailEntry = new ThumbnailEntry(item.id, thumbnail);
        Log.d(getClass().getSimpleName(), "[getAlbumThumbnail] " + item + ", " + thumbnailEntry);
        return thumbnailEntry;
    }

    final public boolean removeGridThumbnail(MediaItem item) {
        return imageWorker.removeImage(item.id);
    }
}