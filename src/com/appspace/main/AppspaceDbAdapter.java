package com.appspace.main;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

// Adapter class to interface with database
public class AppspaceDbAdapter {

	public static final String TABLE_NAME = "main";
	public static final String FIELD_PACKAGE = "package";
	public static final String FIELD_CATEGORY = "category";
	private Context context;
	private SQLiteDatabase database;
	private AppspaceDbHelper dbHelper;
	
	public AppspaceDbAdapter(Context m) {
		this.context = m;
	}
	
	// Open the database
	public synchronized AppspaceDbAdapter open() throws SQLException {
		dbHelper = new AppspaceDbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}

	// Close the database
	public synchronized void close() {
		dbHelper.close();
	}
	
	// Fetch the category corressponding to the app package
	public int fetchPackageCategory(String pname) {
		Cursor c = database.query(TABLE_NAME, new String[]{FIELD_CATEGORY}, FIELD_PACKAGE+" = '"+pname+"'", null, null, null, null);
		if(c.moveToFirst()) {
			// Package entry found
			return c.getInt(c.getColumnIndex(FIELD_CATEGORY));
		}
		else {
			// Package entry not in database
			return -1;
		}
	}
}
