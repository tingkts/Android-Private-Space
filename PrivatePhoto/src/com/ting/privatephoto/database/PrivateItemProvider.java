package com.ting.privatephoto.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class PrivateItemProvider extends ContentProvider {
    public static final String AUTHORITY = "com.ting.privatephoto.database.PrivateItemProvider";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_FILE_NAME = "file_name";
    public static final String COLUMN_FILE_PATH = "file_path";
    public static final String COLUMN_PRIVATE_FOLDER_NAME = "private_folder_name";
    public static final String COLUMN_MICRO_THUMBNAIL = "micro_thumbnail";
    public static final String COLUMN_TYPE = "type";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "privateitem.db";

    private static final String DB_TABLE_PRIVATE_ITEM = "private_item";
    private static final String SQL_CREATE_TABLE_PRIVATE_ITEM = "CREATE TABLE " +
            DB_TABLE_PRIVATE_ITEM + " (" + "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_FILE_NAME + " TEXT NOT NULL," +
            COLUMN_FILE_PATH + " TEXT NOT NULL," +
            COLUMN_PRIVATE_FOLDER_NAME + " TEXT NOT NULL," +
            COLUMN_MICRO_THUMBNAIL + " BOOB," +
            COLUMN_TYPE + " INTEGER" + ");";

    public static final Uri CONTENT_URI_PRIVATE_ITEM =
            Uri.parse("content://" + AUTHORITY + "/private_item");

    public static final String TYPE_CONTENT = "vnd.android.cursor.dir/vnd.tpv.privatephoto";
    public static final String TYPE_CONTENT_ITEM = "vnd.android.cursor.item/vnd.tpv.privatephoto";

    private static final int URI_PRIVATE_ITEM = 0;
    private static final int URI_PRIVATE_ITEM_ID = 1;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "private_item", URI_PRIVATE_ITEM);
        uriMatcher.addURI(AUTHORITY, "private_item/#", URI_PRIVATE_ITEM_ID);
    }

    private DatabaseHelper mDatabaseHelper = null;
    private ContentResolver mContentResolver = null;

    @Override
    public boolean onCreate() {
        mContentResolver = getContext().getContentResolver();
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case URI_PRIVATE_ITEM:
                return TYPE_CONTENT;
            case URI_PRIVATE_ITEM_ID:
                return TYPE_CONTENT_ITEM;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        String table = null;
        switch (uriMatcher.match(uri)) {
            case URI_PRIVATE_ITEM_ID:
                selection = "_id=" + uri.getPathSegments().get(1)
                        + (!TextUtils.isEmpty(selection) ? " and (" + selection + ')' : "");
            case URI_PRIVATE_ITEM:
                table = DB_TABLE_PRIVATE_ITEM;
                break;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
        Cursor cursor = db.query(table, projection, selection, selectionArgs,
                null, null, sortOrder);
        cursor.setNotificationUri(mContentResolver, uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long id = -1L;
        switch (uriMatcher.match(uri)) {
            case URI_PRIVATE_ITEM:
                id = db.insert(DB_TABLE_PRIVATE_ITEM, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
        if (id < 0) {
            throw new SQLiteException("Unable to insert " + contentValues + " for " + uri);
        }
        Uri newUri = ContentUris.withAppendedId(uri, id);
        mContentResolver.notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count = 0;
        String table = null;
        switch (uriMatcher.match(uri)) {
            case URI_PRIVATE_ITEM_ID:
                selection = "_id=" + uri.getPathSegments().get(1)
                        + (!TextUtils.isEmpty(selection) ? " and (" + selection + ')' : "");
            case URI_PRIVATE_ITEM:
                table = DB_TABLE_PRIVATE_ITEM;
                break;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
        count = db.delete(table, selection, selectionArgs);
        mContentResolver.notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count = 0;
        String table = null;
        switch (uriMatcher.match(uri)) {
            case URI_PRIVATE_ITEM_ID:
                selection = "_id=" + uri.getPathSegments().get(1)
                        + (!TextUtils.isEmpty(selection) ? " and (" + selection + ')' : "");
            case URI_PRIVATE_ITEM:
                table = DB_TABLE_PRIVATE_ITEM;
                break;
            default:
                throw new IllegalArgumentException("Error Uri: " + uri);
        }
        count = db.update(table, values, selection, selectionArgs);
        mContentResolver.notifyChange(uri, null);
        return count;
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
            super(context, name, factory, version, errorHandler);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TABLE_PRIVATE_ITEM);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}