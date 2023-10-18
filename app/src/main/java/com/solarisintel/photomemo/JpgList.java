package com.solarisintel.photomemo;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// oneline data is  photo-datetime, file-name , fullpath(including file-name)"
public class JpgList {
    public ArrayList imgList = new ArrayList<>();;
    private final String jpgExtension = "jpg";

    public void JpgList(){
         imgList = new ArrayList<>();
    }
    public void walk( String path) {

        String fullpath = "";
        String filename = "";
        String fileExtension = "";
        int lastDotPosition = 0;

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            fullpath = f.getAbsoluteFile().toString();
            filename = f.getName().toString();

            if ( f.isDirectory() ) {
                // Log.d("DEBUG",  "recall Scan Dir=" + fullpath );
                walk( fullpath);
            }
            else {
                // 拡張子取得、jpgで撮影日付があるもののみリスト化する
                lastDotPosition = filename.lastIndexOf(".");
                if(lastDotPosition != -1){
                    fileExtension = filename.substring(lastDotPosition + 1);
                    if (fileExtension.equalsIgnoreCase(jpgExtension) ) {
                        String jpgDate = getDateJpg(fullpath);
                        if (jpgDate.length() > 0) {
                            String existMemo = "";
                            if (MainActivity.HelperMemoDB.ExistMemo(MainActivity.readMemoDB, jpgDate, filename)) {
                                existMemo = "M";
                            }
                            imgList.add(jpgDate + "," + filename + "," + fullpath +"," + existMemo);
                            // Log.d("DEBUG", "image　list add path= " + fullpath);
                        }
                    }
                }
            }
        }
    }

    public String getDateJpg(String filePath){
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null) {
                Log.d("DEBUG", "TAG DATETATIME=" + exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
                return  exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            } else {
                return "";
            }
        } catch (IOException e){
            return "";
        }
    }
}
