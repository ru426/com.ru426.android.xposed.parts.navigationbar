package com.ru426.android.xposed.parts.navigationbar.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public final class ModDbProvider extends ContentProvider {
	private static final String TABLE_NAME = "mod";
	private SQLiteDatabase db;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db.insert(TABLE_NAME, null, values);
		return null;
	}

	@Override
	public boolean onCreate() {
		DatabaseHelper mDbHelper = new DatabaseHelper(getContext());
		db = mDbHelper.getWritableDatabase();
        return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return db.update(TABLE_NAME, values, selection, selectionArgs);
	}

	protected void finalize() throws Throwable {
		db.close();
		super.finalize();
	}
	
	class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "settings.db";
	    private static final int DATABASE_VERSION = 1;
	    public DatabaseHelper(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }
	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (name text unique on conflict replace, value text);");
	        db.execSQL("CREATE INDEX IF NOT EXISTS modIndex1 on mod (name);");
	    }
	    @Override
	    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
	}
}
