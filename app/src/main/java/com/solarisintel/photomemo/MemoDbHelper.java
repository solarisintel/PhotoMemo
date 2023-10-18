package com.solarisintel.photomemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MemoDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "memo.sqlitedb";
	public static final String TABLE_NAME = "memo";
    public static final int DATABASE_VERSION = 1;
    private Context context = null;
    public MemoDbHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.context = context;
    	
    	try {
    		if (! databaseExists()) {
				copyDb();
    		}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	try {
			copyDb();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private boolean databaseExists() {
    	File dbFile = new File(this.context.getFilesDir().getParent() + "/databases/" + DATABASE_NAME);
    	return dbFile.exists();
    }
    
    private void copyDb() throws IOException {
    	//
    	String copyFolder = this.context.getFilesDir().getParent() + "/databases/";
    	
    	// 
    	File checkDirectory = new File(copyFolder);
    	if (! checkDirectory.exists()) { 
    		checkDirectory.mkdir();
    	}

    	// 
    	InputStream in = this.context.getAssets().open(DATABASE_NAME);
    	OutputStream out = new FileOutputStream(copyFolder + DATABASE_NAME);

    	// 
    	byte[] buf = new byte[1024];
    	int len = 0;
    	while ((len = in.read(buf)) > 0) {
    		out.write(buf, 0, len);
    	}
    	
    	// 
    	in.close(); 
    	out.close();
    }

	public void Delete(SQLiteDatabase db, String argDate, String argFileName) {

		String[] args = new String[]{argDate, argFileName};

		db.delete(TABLE_NAME, "fdate=? and fname=?", args);
		db.close();
	}

	public void Save(SQLiteDatabase db, String argDate, String argFileName, String argMemo) {

		if (ExistMemo(db, argDate, argFileName)) {

			ContentValues cv = new ContentValues();
			cv.put("memotext", argMemo);

			String[] args = new String[]{argDate, argFileName};

			db.update(TABLE_NAME, cv, "fdate=? and fname=?", args);
		} else {
			ContentValues cv = new ContentValues();
			cv.put("fdate", argDate);
			cv.put("fname", argFileName);
			cv.put("memotext", argMemo);
			db.insert(TABLE_NAME, null, cv);
		}
	}

	public boolean ExistMemo(SQLiteDatabase db, String argDate, String argFileName ) {

		Cursor cursor = db.rawQuery(
				"SELECT * FROM " + TABLE_NAME + " WHERE fdate=? and fname=?", new String[]{argDate,argFileName });

		boolean result = false;
		if (cursor.moveToFirst()) {
			result = true;
		}
		cursor.close();
		return result;
	}

	public String getMemo(SQLiteDatabase db, String argDate, String argFileName ) {


		Cursor cursor = db.rawQuery(
				"SELECT memotext FROM " + TABLE_NAME + " WHERE fdate=? and fname=?", new String[]{argDate,argFileName });

		String result = "";
		if (cursor.moveToFirst()) {
			result =  cursor.getString(cursor.getColumnIndexOrThrow("memotext"));
		}
		cursor.close();
		return result;
	}

	public void DebugDump(SQLiteDatabase db ) {

		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME,null );

		int count = 0;
		while (cursor.moveToNext()) {
			String result1 =  cursor.getString(cursor.getColumnIndexOrThrow("fdate"));
			String result2 =  cursor.getString(cursor.getColumnIndexOrThrow("fname"));
			String result3 =  cursor.getString(cursor.getColumnIndexOrThrow("memotext"));
			count += 1;
			Log.d("DEBUG", "SQL Result = " + count + " " + result1 + "  " + result2 + "  " + result3);
		}
		cursor.close();
	}

}
