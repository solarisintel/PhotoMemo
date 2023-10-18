package com.solarisintel.docophoto;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AddressDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "addr.sqlitedb";
    public static final int DATABASE_VERSION = 1;
    private Context context = null;
    
    public AddressDbHelper(Context context) {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.context = context;
    	
    	try {
    		//if (! databaseExists()) {
				copyDb();
    		//}
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

	public String GetNearOneSQL(double lat, double lon ) {

		double rangeKm = 5; // 5km

		double km_cos = Math.cos(rangeKm/6371);    // 距離基準cos値
		double rad_lat = Math.toRadians(lat);
		double rad_lon = Math.toRadians(lon);

		double qsin_lat=Math.sin(rad_lat);
		double qcos_lat=Math.cos(rad_lat);
		double qsin_lon=Math.sin(rad_lon);
		double qcos_lon=Math.cos(rad_lon);

		// cos(距離/C) = sin(lat)*sin(qlat)+cos(lat)*cos(qlat)*(cos(lng)*cos(qlng)+sin(lng)*sin(qlng))
		String sql = "SELECT prefectures, city, town, lon, lat,";
		sql = sql + " (sin_lat * " + String.format("%.14f", qsin_lat) + " + cos_lat *" +  String.format("%.14f",qcos_lat) + "* ( cos_lon *" + String.format("%.14f",qcos_lon) + "+ sin_lon * " + String.format("%.14f",qsin_lon) + ")) AS distcos ";
		sql = sql + " from address ";
		sql = sql + " where distcos > " + String.format("%.14f", km_cos);
		sql = sql + " ORDER BY distcos DESC";
		sql = sql + " LIMIT 1";

		return sql;

	}

	public String GetNearTown(SQLiteDatabase db, double lat, double lon) {

		String sql = GetNearOneSQL(lat, lon);
		Cursor c = db.rawQuery(sql, null);

		String resultStr = "unknown foregin?";

		if (c.moveToFirst()) {
			resultStr = c.getString(c.getColumnIndexOrThrow("prefectures")) + "　" + c.getString(c.getColumnIndexOrThrow("city")) + "　" + c.getString(c.getColumnIndexOrThrow("town"));
		}
		c.close();
		return resultStr;
	}

}
