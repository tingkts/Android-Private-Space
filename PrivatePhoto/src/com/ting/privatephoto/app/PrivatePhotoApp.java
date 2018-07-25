package com.ting.privatephoto.app;

import android.app.Application;
import com.ting.privatephoto.media.Album;
import com.ting.privatephoto.util.Log;

public class PrivatePhotoApp extends Application {
    enum PrivacyType {PRIVATE, PUBLIC}

    static final int START_ACTIVITY_FOR_RESULT_REQUEST_CODE = 1;
    static final int START_ACTIVITY_FOR_RESULT_RESULT_CODE = START_ACTIVITY_FOR_RESULT_REQUEST_CODE+1;
    static final String INTENT_EXTRA_KEY_ALBUM_POSITION = "intent_extra_key_album_position";
    static final String INTENT_EXTRA_KEY_ALBUM_BUCKET_ID = "intent_extra_key_album_bucket_id";
    static final String INTENT_EXTRA_KEY_ALBUM_NAME = "intent_extra_key_album_name";
    static final String INTENT_EXTRA_KEY_ALBUM_COUNT = "intent_extra_key_album_count";
    static final String INTENT_EXTRA_KEY_IS_REMOVE_ALBUM = "intent_extra_key_is_album_count";
    static final String INTENT_EXTRA_KEY_ALBUM_THUMBNAIL_ID = "intent_extra_key_album_thumbnail_id";
    static final String INTENT_EXTRA_KEY_ALBUM_THUMBNAIL = "intent_extra_key_album_thumbnail";
    static final String INTENT_EXTRA_KEY_ITEM_POSITION = "intent_extra_key_item_position";
    static final String INTENT_EXTRA_KEY_REFRESH_PRIVATE_ALBUMS = "intent_extra_key_refresh_private_albums";
    static final String INTENT_EXTRA_KEY_REFRESH_PHOTO_GRIDS = "intent_extra_key_refresh_photo_grids";

    private String add2PrivateAlbumName = "";
    void setAdd2PrivateAlbumName(String albumName) {add2PrivateAlbumName = albumName;}
    String getAdd2PrivateAlbumName() {return add2PrivateAlbumName;}

    static class PaperSharedAlbum {
        static private final PaperSharedAlbum self = new PaperSharedAlbum();
        static PaperSharedAlbum getInstance() {return self;}

        private Album album;
        void setAlbum(Album album) {this.album = album;}
        Album getAlbum() {return album;}

        private PhotoGridActivity gridActivity;
        void setGridActivity(PhotoGridActivity gridActivity) {
            this.gridActivity = gridActivity;
        }
        PhotoGridActivity getGridActivity() {
            return gridActivity;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.e(getClass().getSimpleName(), "[onLowMemory]");
    }
}