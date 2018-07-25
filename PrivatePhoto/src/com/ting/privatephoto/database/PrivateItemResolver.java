package com.ting.privatephoto.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.ting.privatephoto.media.MediaItem;
import com.ting.privatephoto.util.Log;

public class PrivateItemResolver {
    private Context mContext;
    private ContentResolver mContentResolver;
    private static PrivateItemResolver mSelf;

    static public PrivateItemResolver getInstance(Context context) {
        if (mSelf == null) {
            mSelf = new PrivateItemResolver(context);
        }
        return mSelf;
    }

    public PrivateItemResolver(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }
    
    public void insertItem(MediaItem item, String privateAlbumName) {
        ContentValues values = new ContentValues();
        values.put(PrivateItemProvider.COLUMN_FILE_NAME, item.name);
        values.put(PrivateItemProvider.COLUMN_FILE_PATH, item.path);
        values.put(PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME, privateAlbumName);
        values.put(PrivateItemProvider.COLUMN_TYPE, item.type);
        mContentResolver.insert(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM, values);
    }

    public void deleteItem(long id) {
        String whereClause = PrivateItemProvider.COLUMN_ID + "=?";
        String[] whereArgs = new String[]{""+id};
        mContentResolver.delete(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM, whereClause, whereArgs);
    }

//    public Bitmap getThumbnail(Cursor cursor, int columeIndex) {
//        Bitmap thumbnail = null;
//        byte[] thumbnailBytes = cursor.getBlob(columeIndex);
//        if (thumbnailBytes != null) {
//            try {
//                thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0,
//                        thumbnailBytes.length);
//            } catch (Exception e) { e.printStackTrace(); }
//        }
//        return thumbnail;
//    }

    public void printAllItems(String prefix) {
        Cursor cursor = mContentResolver.query(PrivateItemProvider.CONTENT_URI_PRIVATE_ITEM,
                null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Log.d(getClass().getSimpleName(), prefix+"[printAllItems] id=" + cursor.getInt(cursor.getColumnIndex(PrivateItemProvider.COLUMN_ID))
                            + ", name=" + cursor.getString(cursor.getColumnIndex(PrivateItemProvider.COLUMN_FILE_NAME))
                            + ", filePath=" + cursor.getString(cursor.getColumnIndex(PrivateItemProvider.COLUMN_FILE_PATH))
                            + ", folderName=" + cursor.getString(cursor.getColumnIndex(PrivateItemProvider.COLUMN_PRIVATE_FOLDER_NAME))
                            + ", type=" + cursor.getInt(cursor.getColumnIndex(PrivateItemProvider.COLUMN_TYPE)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
}