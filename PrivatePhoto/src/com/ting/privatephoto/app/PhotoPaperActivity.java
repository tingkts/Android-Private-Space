package com.ting.privatephoto.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.ting.privatephoto.R;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.Config;
import com.ting.privatephoto.util.Log;
import com.ting.privatephoto.view.BottomBar;
import com.ting.privatephoto.view.BottomBar.OnButtonClickListener;
import com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType;
import com.ting.privatephoto.app.PrivatePhotoApp.PaperSharedAlbum;
import static com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType.PRIVATE;
import static com.ting.privatephoto.app.PrivatePhotoApp.PrivacyType.PUBLIC;
import static com.ting.privatephoto.media.MediaItem.MEDIA_TYPE_IMAGE;
import static com.ting.privatephoto.media.MediaItem.MEDIA_TYPE_VIDEO;
import static com.ting.privatephoto.media.MediaItem.UNDEFINED_ID_INDEX;
import static com.ting.privatephoto.view.BottomBar.Type.ITEM_ACTION_ADD2PRIVATE;
import static com.ting.privatephoto.view.BottomBar.Type.ITEM_ACTIONS_DEL_MOVE_RESTORE;
import static com.ting.privatephoto.app.PrivatePhotoApp.START_ACTIVITY_FOR_RESULT_RESULT_CODE;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_IS_REMOVE_ALBUM;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ITEM_POSITION;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_REFRESH_PHOTO_GRIDS;
import static com.ting.privatephoto.app.PrivatePhotoApp.INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID;

abstract class PhotoPaperActivity extends BaseActivity
        implements BottomBar.OnButtonClickListener, ViewPager.OnPageChangeListener {
    private PrivacyType privacyType;
    private View topBarRoot;
    private TextView topBarText;
    private BottomBar bottomBarActions;
    protected ViewPager photoPager;
    private PhotoPagerAdapter photoPagerAdapter;
    protected Album album;
    protected PhotoGridActivity gridActivity;
    protected int notifyDataChange;

    protected void onCreate(PrivacyType type, OnButtonClickListener onButtonClickListener) {
        Log.d(getClass().getSimpleName(), "[onCreate]");
        setContentView(R.layout.layout_activity_photopaper);
        int systemUiVis = getWindow().getDecorView().getSystemUiVisibility();
        systemUiVis |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(systemUiVis);
        privacyType = type;
        topBarRoot = findViewById(R.id.id_topbar_title_root);
        topBarText = (TextView)findViewById(R.id.id_topbar_title);
        topBarText.setText("* title *");
        setTopBarBackIconOnClickResponse(true);
        if (privacyType == PRIVATE)
            bottomBarActions = new BottomBar(ITEM_ACTIONS_DEL_MOVE_RESTORE, this, onButtonClickListener);
        else if (privacyType == PUBLIC)
            bottomBarActions = new BottomBar(ITEM_ACTION_ADD2PRIVATE, this, onButtonClickListener);
        bottomBarActions.setVisible(true);
        photoPager = (ViewPager) findViewById(R.id.id_photo_pager);
        photoPager.setAdapter(photoPagerAdapter = new PhotoPagerAdapter(getFragmentManager()));
        photoPager.addOnPageChangeListener(this);
        album = PaperSharedAlbum.getInstance().getAlbum();
        gridActivity = PaperSharedAlbum.getInstance().getGridActivity();
        if (isValidAlbum()) {
            REQUESTED_ITEM_BITMAP_SIZE = calculateRequestItemBitmapSize();
            photoPagerAdapter.setCount(album.count);
            int position = getIntent().getIntExtra(INTENT_EXTRA_KEY_ITEM_POSITION, UNDEFINED_ID_INDEX);
            setCurrentItem(position);
        }
        notifyDataChange = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
    @Override
    public void onPageScrollStateChanged(int state) {}
    @Override
    public void onPageSelected(int position) {
        topBarText.setText(gridActivity.getMediaItem(position).name);
    }

    private PhotoPagerFragment.Callbacks photoPagerFragmentCallbacks = new PhotoPagerFragment.Callbacks() {
        @Override
        public void onClick(View v) {
            Log.d(getClass().getName(), "[onClick] " + v);
            boolean isNavHidden = (getWindow().getDecorView().getSystemUiVisibility() &
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;
            showHideSystemUIAndActivityUiBar(isNavHidden);
        }

        @Override
        public Object[] getItemInfo(int position) {
            MediaItem item = gridActivity.getMediaItem(position);
            Object[] itemInfo = new Object[3]; // id, type, file path name
            itemInfo[0] = item.id;
            itemInfo[1] = item.type;
            if (privacyType == PUBLIC) {
                itemInfo[2] = item.path+"/"+item.name;
            } else if (privacyType == PRIVATE) {
                itemInfo[2] = getContext().getPrivateFilesDir().getPath()+"/"
                        +album.name+"/"+item.name;
            }
            return itemInfo;
        }

        @Override
        public void playVideo(int position) {
            PhotoPaperActivity.this.playVideo(privacyType, (String)getItemInfo(position)[2]);
        }
    };

    boolean isValidAlbum() { return (album != null && gridActivity != null); }

    private void showHideSystemUIAndActivityUiBar(boolean show) {
        int systemUiVis = getWindow().getDecorView().getSystemUiVisibility();
        if (show) {
            topBarRoot.setVisibility(View.VISIBLE);
            bottomBarActions.setVisible(true);
            systemUiVis &= ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else { // hide
            topBarRoot.setVisibility(View.INVISIBLE);
            bottomBarActions.setVisible(false);
            systemUiVis |= (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        getWindow().getDecorView().setSystemUiVisibility(systemUiVis);
    }

    static int REQUESTED_ITEM_BITMAP_SIZE;
    private int calculateRequestItemBitmapSize() {
        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;
        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        final int longest = (height > width ? height : width) / 2;
        return longest;
    }

    void setCurrentItem(int position) {
        photoPager.setCurrentItem(position, true);
        onPageSelected(position);
    }

    private class PhotoPagerAdapter extends FragmentStatePagerAdapter {
        private int count;

        public PhotoPagerAdapter(FragmentManager fm) { super(fm); }

        @Override
        public int getCount() { return count; }

        @Override
        public Fragment getItem(int position) {
            return PhotoPagerFragment.newInstance(position).
                    setCallbacks(photoPagerFragmentCallbacks);
        }

        void setCount(int count) {
            this.count = count;
            notifyDataSetChanged();
        }
    }

    public static class PhotoPagerFragment extends Fragment {
        static private final String BUNDLE_KEY_POSITION = "position";
        private int position;
        private ImageView imagePhoto;
        private ImageView videoIcon;
        private ProgressBar progressCycle;
        private LoadingBitmapTasker loadingBitmapTasker;
        private Callbacks callbacks;

        public PhotoPagerFragment() {}

        static PhotoPagerFragment newInstance(int position) {
            PhotoPagerFragment f = new PhotoPagerFragment();
            Bundle args = new Bundle();
            args.putInt(BUNDLE_KEY_POSITION, position);
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            position = getArguments() != null ? getArguments().getInt(BUNDLE_KEY_POSITION) : 0;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.layout_viewpaper_item_photo, container, false);
            imagePhoto = (ImageView) v.findViewById(R.id.id_viewpager_item_photo);
            videoIcon = (ImageView) v.findViewById(R.id.id_viewpager_item_video_icon);
            progressCycle = (ProgressBar) v.findViewById(R.id.id_progress_cycle);
            TextView textPosition = (TextView) v.findViewById(R.id.id_viewpager_item_test_text);
            if (Config.DEBUG_UI) {textPosition.setText(""+position);}
            else {imagePhoto.setImageBitmap(null);}
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            imagePhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getName(), "[onClick] " + v);
                    callbacks.onClick(v);
                }
            });
            videoIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getName(), "[onClick] " + v);
                    callbacks.playVideo(position);
                }
            });
            Object[] itemInfo = callbacks.getItemInfo(position);
            long itemId = (long) itemInfo[0];
            int itemType = (int) itemInfo[1];
            String itemFilePathName = (String) itemInfo[2];
            loadingBitmapTasker = (LoadingBitmapTasker) new LoadingBitmapTasker().
                    execute(imagePhoto, itemId, itemFilePathName, itemType, videoIcon);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            loadingBitmapTasker.cancel(true);
        }

        public Fragment setCallbacks(Callbacks callback) { callbacks = callback; return this; }

        interface Callbacks {
            void onClick(View v);
            Object[] getItemInfo(int position);
            void playVideo(int position);
        }

        private class LoadingBitmapTasker extends AsyncTask<Object, Object, Void> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressCycle.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Object... params) {
                ImageView imageView = (ImageView)params[0];
                long itemId = (long)params[1];
                String itemFilePathName = (String)params[2];
                int itemType = (int)params[3];
                ImageView videoIcon = (ImageView)params[4];
                Bitmap itemBitmap = null;
                if (itemType == MEDIA_TYPE_IMAGE) {
                    itemBitmap = decodeBitmapFromImage(itemFilePathName);
                } else if (itemType == MEDIA_TYPE_VIDEO) {
                    itemBitmap = decodeBitmapFromVideo(itemFilePathName);
                }
                if (itemBitmap != null) { publishProgress(true, imageView, itemBitmap, itemType, videoIcon); }
                else { publishProgress(false); }
                Log.d(getClass().getName(), "[doInBackground] item id = " + itemId + ", "
                        + itemFilePathName + ", itemBitmap=" + itemBitmap);
                return null;
            }

            @Override
            protected void onProgressUpdate(Object... values) {
                super.onProgressUpdate(values);
                boolean hasBitmap = (boolean)values[0];
                if (hasBitmap) {
                    ImageView imageView = (ImageView) values[1];
                    Bitmap photoBitmap = (Bitmap) values[2];
                    int itemType = (int)values[3];
                    ImageView videoIcon = (ImageView) values[4];
                    imageView.setImageBitmap(photoBitmap);
                    if (itemType == MEDIA_TYPE_VIDEO) {
                        videoIcon.setVisibility(View.VISIBLE);
                    } else {
                        videoIcon.setVisibility(View.GONE);
                    }
                }
                progressCycle.setVisibility(View.INVISIBLE);
            }

            private Bitmap decodeBitmapFromImage(String filePathName) {
                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(filePathName, options);
                // Calculate inSampleSize
                final int height = options.outHeight;
                final int width = options.outWidth;
                int inSampleSize = 1;
                if (height > REQUESTED_ITEM_BITMAP_SIZE || width > REQUESTED_ITEM_BITMAP_SIZE) {
                    final int halfHeight = height / 2;
                    final int halfWidth = width / 2;
                    // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                    // height and width larger than the requested height and width.
                    while ((halfHeight / inSampleSize) > REQUESTED_ITEM_BITMAP_SIZE
                            && (halfWidth / inSampleSize) > REQUESTED_ITEM_BITMAP_SIZE) {
                        inSampleSize *= 2;
                    }
                    // This offers some additional logic in case the image has a strange
                    // aspect ratio. For example, a panorama may have a much larger
                    // width than height. In these cases the total pixels might still
                    // end up being too large to fit comfortably in memory, so we should
                    // be more aggressive with sample down the image (=larger inSampleSize).
                    long totalPixels = width * height / inSampleSize;
                    // Anything more than 2x the requested pixels we'll sample down further
                    final long totalReqPixelsCap = REQUESTED_ITEM_BITMAP_SIZE *
                            REQUESTED_ITEM_BITMAP_SIZE * 2;
                    while (totalPixels > totalReqPixelsCap) {
                        inSampleSize *= 2;
                        totalPixels /= 2;
                    }
                }
                options.inSampleSize = inSampleSize;
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                return BitmapFactory.decodeFile(filePathName, options);
            }

            private Bitmap decodeBitmapFromVideo(String filePathName) {
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
                return videoFrame;
            }
        }
    }

    static protected final int NOTIFY_DATA_CHANGE_DATA_SET_CHANGED = 0x01;
    static protected final int NOTIFY_DATA_CHANGE_REPLACE_ALBUM_THUMBNAIL = 0x02;
    private Handler handler = new Handler();
    protected abstract class ActionTasker extends BaseActivity.ActionTasker {
        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int updateCode = (int)values[0];
            if (updateCode == UPDATE_CODE_SHOW_PROGRESS_DIALOG) {
                if (actionTaskerProgressDialog != null) {
                    actionTaskerProgressDialog.setMax(1);
                    actionTaskerProgressDialog.setProgress(0);
                    actionTaskerProgressDialog.show();
                }
            }
        }

        @Override
        protected void finishOrCancel(/*int index*/Result result) {
            gridActivity.refreshMediaCursor();
            super.finishOrCancel(/*index*/result);
            int index = result.albumItemCount;
            if (index == -1) {
                handler.postDelayed(new Runnable() {
                       @Override
                       public void run() { sendBackKey(); }}, 100);
            } else {
                photoPager.setAdapter(photoPagerAdapter = new PhotoPagerAdapter(getFragmentManager()));
                photoPagerAdapter.setCount(album.count);
                if (index == album.count) setCurrentItem(0);
                else setCurrentItem(index);
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (notifyDataChange != 0) {
            boolean removeAlbum = album.count == 0;
            if (removeAlbum) {
                setResult(START_ACTIVITY_FOR_RESULT_RESULT_CODE, new Intent().
                        putExtra(INTENT_EXTRA_KEY_IS_REMOVE_ALBUM, true));
            } else {
                boolean refreshPhotoGrids = (notifyDataChange & NOTIFY_DATA_CHANGE_DATA_SET_CHANGED)
                        == NOTIFY_DATA_CHANGE_DATA_SET_CHANGED;
                long replaceAlbumThumbnailId = (notifyDataChange & NOTIFY_DATA_CHANGE_REPLACE_ALBUM_THUMBNAIL)
                        == NOTIFY_DATA_CHANGE_REPLACE_ALBUM_THUMBNAIL ? album.thumbnail.id : UNDEFINED_ID_INDEX;
                setResult(START_ACTIVITY_FOR_RESULT_RESULT_CODE, new Intent().
                        putExtra(INTENT_EXTRA_KEY_REFRESH_PHOTO_GRIDS, refreshPhotoGrids).
                        putExtra(INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID, replaceAlbumThumbnailId));
            }
        }
        super.onBackPressed();
    }
}