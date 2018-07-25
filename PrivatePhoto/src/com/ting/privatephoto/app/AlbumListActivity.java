package com.ting.privatephoto.app;

import com.ting.privatephoto.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ting.privatephoto.util.Config;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.media.Album.AlbumList;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import static com.ting.privatephoto.media.MediaItem.UNDEFINED_ID_INDEX;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_REQUEST_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_RESULT_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_BUCKET_ID;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_COUNT;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_NAME;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_POSITION;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_THUMBNAIL;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_IS_REMOVE_ALBUM;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS;
import static com.ting.privatephoto.app.AlbumListActivity.EnumerateAlbumTasker.PROGRESS_CODE_ALBUM;
import static com.ting.privatephoto.app.AlbumListActivity.EnumerateAlbumTasker.PROGRESS_CODE_ALBUM_ITEM_COUNT;
import static com.ting.privatephoto.app.AlbumListActivity.EnumerateAlbumTasker.PROGRESS_CODE_ALBUM_THUMBNAIL;

abstract class AlbumListActivity extends ListGridActivity implements AdapterView.OnItemClickListener{
    protected TextView titleBar;
    private ListView albumList;
    private ListViewAdapter albumListAdapter;
    private ProgressBar cycleProgress;
    protected EnumerateAlbumTasker enumerateAlbumTasker;
    protected AlbumList albums;
    private PrivacyType type;

    protected void onCreate(PrivacyType type, EnumerateAlbumTasker enumerateAlbumTasker) {
        onCreate(new Handler());
        setContentView(R.layout.layout_activity_albumlist);
        titleBar = (TextView) findViewById(R.id.id_topbar_title);
        if (type == PrivacyType.PRIVATE) setTopBarBackIconOnClickResponse(false).
                setImageDrawable(getResources().getDrawable(R.drawable.ic_settings_tpv_privacy));
        else if (type == PrivacyType.PUBLIC) setTopBarBackIconOnClickResponse(true);
        albumList = (ListView) findViewById(R.id.id_listview_album);
        cycleProgress = (ProgressBar) findViewById(R.id.id_progress_cycle);
        albums = new AlbumList();
        albumList.setAdapter(albumListAdapter = new ListViewAdapter());
        albumList.setOnItemClickListener(this);
        this.type = type;
        this.enumerateAlbumTasker = (EnumerateAlbumTasker)enumerateAlbumTasker.execute();
        showToastWaitDataReadyOnListGridScroll(albumList, enumerateAlbumTasker);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != START_ACTIVITY_FOR_RESULT_REQUEST_CODE ||
                resultCode != START_ACTIVITY_FOR_RESULT_RESULT_CODE || data == null) {
            return;
        }
        int albumPosition = data.getIntExtra(INTENT_EXTRA_KEY_ALBUM_POSITION, UNDEFINED_ID_INDEX);
        String bucketId = data.getStringExtra(INTENT_EXTRA_KEY_ALBUM_BUCKET_ID);
        String albumName = data.getStringExtra(INTENT_EXTRA_KEY_ALBUM_NAME);
        int albumItemCount = data.getIntExtra(INTENT_EXTRA_KEY_ALBUM_COUNT, 0);
        boolean isRemoveAlbum = data.getBooleanExtra(INTENT_EXTRA_KEY_IS_REMOVE_ALBUM, false);
        long albumThumbnailId = data.getLongExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, UNDEFINED_ID_INDEX);
        Log.d(getClass().getSimpleName(), "[onActivityResult] albumPosition=" + albumPosition
                + ", bucketId=" + bucketId + ", albumName=" + albumName + ", albumItemCount="
                + albumItemCount + ", isRemoveAlbum=" + isRemoveAlbum + ", albumThumbnailId="
                + albumThumbnailId);
        if (isRemoveAlbum) {
            albums.remove(albumPosition);
            albumListAdapter.setCount(albums.size());
        } else {
            Album album = albums.get(albumPosition);
            album.count = albumItemCount;
            if (albumThumbnailId != UNDEFINED_ID_INDEX) {
                album.thumbnail.id = albumThumbnailId;
                album.thumbnail.self = data.getParcelableExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL);
                updateListViewByPosition(PROGRESS_CODE_ALBUM, albumPosition,
                        album.name, album.count, album.thumbnail.self);
            } else {
                updateListViewByPosition(PROGRESS_CODE_ALBUM_ITEM_COUNT, albumPosition,
                        album.name, album.count, null);
            }
        }
        if (type == PrivacyType.PUBLIC) {
            setResult(START_ACTIVITY_FOR_RESULT_RESULT_CODE, new Intent().putExtra(
                    INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS, true));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(getClass().getSimpleName(), "[onItemClick] item " + position + " clicked");
        if (!checkIfTaskEnded(enumerateAlbumTasker, true)) return;
        if (type == PrivacyType.PRIVATE && position == albumListAdapter.getCount()-1) {
            getApp().setAdd2PrivateAlbumName("");
            Intent intent = new Intent();
            intent.setClass(this, PublicAlbumListActivity.class);
            startActivityForResult(intent, START_ACTIVITY_FOR_RESULT_REQUEST_CODE);
        } else {
            Album album = albums.get(position);
            Intent intent = new Intent();
            if (type == PrivacyType.PRIVATE) intent.setClass(this, PrivatePhotoGridActivity.class);
            else if (type == PrivacyType.PUBLIC) intent.setClass(this, PublicPhotoGridActivity.class);
            intent.putExtra(INTENT_EXTRA_KEY_ALBUM_POSITION, position);
            intent.putExtra(INTENT_EXTRA_KEY_ALBUM_BUCKET_ID, album.id);
            intent.putExtra(INTENT_EXTRA_KEY_ALBUM_NAME, album.name);
            intent.putExtra(INTENT_EXTRA_KEY_ALBUM_COUNT, album.count);
            intent.putExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, album.thumbnail.id);
            startActivityForResult(intent, START_ACTIVITY_FOR_RESULT_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        enumerateAlbumTasker.cancel(true);
    }

    protected void checkListGridViewReady() {
        checkListGridViewReady(albumList, 100, 350);
    }

    protected boolean updateListViewByPosition(int progressCode, int position, String albumName,
                                             int albumItemCount, Bitmap albumThumbnail) {
        Log.d(getClass().getSimpleName(), "[updateListViewByPosition] progressCode="+progressCode+
                ", position=" + position + ", albumName=" + albumName + ", albumPhotoCount"
                + albumItemCount + ", albumThumbnail=" + albumThumbnail);
        int visibleIndex = position-albumList.getFirstVisiblePosition();
        Log.d(getClass().getSimpleName(), "[updateListViewByPosition] position=" +position
                + ", visiblePosition=" + visibleIndex);
        View v = albumList.getChildAt(visibleIndex);
        if (v == null) {
            Log.d(getClass().getSimpleName(), "[updateListViewByPosition] skip for null view, " +
                    "position=" + position + ", visiblePosition=" + visibleIndex);
            return false;
        }
        ListViewAdapter.ViewHolder viewHolder = (ListViewAdapter.ViewHolder) v.getTag();
        if (progressCode == PROGRESS_CODE_ALBUM_ITEM_COUNT) {
            viewHolder.albumName.setText(albumName);
            viewHolder.albumCount.setText(""+albumItemCount);
        } else if (progressCode == PROGRESS_CODE_ALBUM_THUMBNAIL) {
            viewHolder.albumIcon.setImageBitmap(albumThumbnail);
        } else if (progressCode == PROGRESS_CODE_ALBUM) {
            viewHolder.albumName.setText(albumName);
            viewHolder.albumCount.setText(""+albumItemCount);
            viewHolder.albumIcon.setImageBitmap(albumThumbnail);
        }
        return true;
    }

    protected void printAlbums() {
        for (Album album : albums) {
            Log.d(getClass().getSimpleName(), "[printAlbums] " + album);
        }
    }

    protected class ListViewAdapter extends BaseAdapter {
        protected int itemCount;

        @Override
        public int getCount() {
            return itemCount;
        }

        @Override
        public Object getItem(int position) {
            return position;
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
            Log.d(getClass().getName(), "[getView] position=" + position);
            ViewHolder viewHolder;
            if (convertView == null) {
                Object[] outObjects = inflaterConvertView();
                convertView = (View)outObjects[0];
                viewHolder = (ViewHolder)outObjects[1];
            }  else { viewHolder = (ViewHolder)convertView.getTag(); }
            if (type == PrivacyType.PRIVATE && position == getCount()-1) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_listview_item_album_2, null);
            } else {
                if (viewHolder == null) {
                    Object[] outObjects = inflaterConvertView();
                    convertView = (View)outObjects[0];
                    viewHolder = (ViewHolder)outObjects[1];
                }
                try {
                    Album album = albums.get(position);
                    viewHolder.albumName.setText(album.name);
                    viewHolder.albumCount.setText(""+album.count);
                    if (album.thumbnail.self != null) viewHolder.albumIcon.setImageBitmap(album.thumbnail.self);
                } catch (Exception e) {Log.d(getClass().getName(), "[getView] index mismatch for data is not ready");}
            }
            return convertView;
        }

        public void setCount(int count) {
            Log.d(getClass().getName(), "[setCount] count=" + count);
            itemCount = count;
            if (type == PrivacyType.PRIVATE) itemCount++;
            notifyDataSetChanged();
        }

        private Object[] inflaterConvertView() {
            View convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_listview_item_album, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.albumIcon = (ImageView)convertView.findViewById(R.id.id_listview_item_album_icon);
            viewHolder.albumName = (TextView)convertView.findViewById(R.id.id_listview_item_album_name);
            viewHolder.albumCount = (TextView)convertView.findViewById(R.id.id_listview_item_album_count);
            viewHolder.rightArrow = (ImageView)convertView.findViewById(com.tpv.R.id.preference_image_next);
            convertView.setTag(viewHolder);
            Object[] outObjects = new Object[2];
            outObjects[0] = convertView;
            outObjects[1] = viewHolder;
            return outObjects;
        }

        class ViewHolder {
            ImageView albumIcon;
            TextView albumName;
            TextView albumCount;
            ImageView rightArrow;
        }
    }

    protected abstract class EnumerateAlbumTasker extends AsyncTask<Void, Object, Void> {
        static public final int PROGRESS_CODE_ALBUM_COUNT = 1;
        static public final int PROGRESS_CODE_ALBUM_ITEM_COUNT = 2;
        static public final int PROGRESS_CODE_ALBUM_THUMBNAIL = 3;
        static public final int PROGRESS_CODE_ALBUM = 4;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (Config.SHOW_CYCLE_PROGRESS) {
                cycleProgress.setVisibility(View.VISIBLE);
                //albumList.setEnabled(false);
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int progressCode = (int)values[0];
            Log.d(getClass().getName(), "[onProgressUpdate] progressCode=" + progressCode);
            if (progressCode == PROGRESS_CODE_ALBUM_COUNT) {
                albumListAdapter.setCount((int)values[1]);
            } else if (progressCode == PROGRESS_CODE_ALBUM_ITEM_COUNT ||
                    progressCode == PROGRESS_CODE_ALBUM_THUMBNAIL) {
                int position = (int)values[1];
                Album album = (Album)values[2];
                updateListViewByPosition(progressCode, position, album.name, album.count, album.thumbnail.self);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(getClass().getName(), "[onPostExecute]");
            taskEnded();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(getClass().getName(), "[onCancelled]");
            taskEnded();
        }

        private void taskEnded() {
            if (Config.SHOW_CYCLE_PROGRESS) {
                cycleProgress.setVisibility(View.INVISIBLE);
                //albumList.setEnabled(true);
            }
            if (albums.isEmpty()) { albumListAdapter.setCount(0); }
            else { albumListAdapter.notifyDataSetChanged(); }
        }
    }
}