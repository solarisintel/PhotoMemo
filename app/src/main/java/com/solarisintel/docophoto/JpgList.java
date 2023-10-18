package com.solarisintel.docophoto;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// format is "datetime, file-name, fullpath(including file-name), lat:lon, address"
public class JpgList {
    public List<String>  dateList = new ArrayList<String>();
    public List<String>  nameList = new ArrayList<String>();
    public List<String>  pathList = new ArrayList<String>();
    public List<String>  addrList = new ArrayList<String>();
    public List<String>  gpsList = new ArrayList<String>();
    public List<String>  memoList = new ArrayList<String>();
    private static final String jpgExtension = "jpg";
    public final String noGpsData = "撮影場所なし";
    public final String nowProgress = "解析中";

    public void Collect( String path) {

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
                Collect( fullpath);
            }
            else {
                // 拡張子取得
                lastDotPosition = filename.lastIndexOf(".");
                if(lastDotPosition != -1){
                    fileExtension = filename.substring(lastDotPosition + 1);
                    if (fileExtension.equalsIgnoreCase(jpgExtension) ) {
                        String jpgDate = getDateJpg(fullpath);
                        String jpgGps = getGpsJpg(fullpath);
                        // no have jpg date, not display list view
                        if (jpgDate.length() > 0) {
                            if (jpgGps.length() > 0) {
                                dateList.add(jpgDate);
                                nameList.add(filename);
                                pathList.add(fullpath);
                                gpsList.add(jpgGps);
                                addrList.add(nowProgress);
                                memoList.add("");
                            } else  {
                                dateList.add(jpgDate);
                                nameList.add(filename);
                                pathList.add(fullpath);
                                gpsList.add("");
                                addrList.add(noGpsData);
                                memoList.add("");
                            }
                       }
                    }
                }
            }
        }
    }

    private double ExifHourMinSecToDegrees(String exifhourminsec) {
        String hourminsec[] = exifhourminsec.split(",");
        String hour[] = hourminsec[0].split("/");
        String min[] = hourminsec[1].split("/");
        String sec[] = hourminsec[2].split("/");
        double dhour = (double)Integer.parseInt(hour[0]) / (double)Integer.parseInt(hour[1]);
        double dmin = (double)Integer.parseInt(min[0]) / (double)Integer.parseInt(min[1]);
        double dsec = (double)Integer.parseInt(sec[0]) / (double)Integer.parseInt(sec[1]);
        double degrees = dhour + dmin / 60.0 + dsec / 3600.0;
        return degrees;
    }

    private double ExifLatitudeToDegrees(String ref, String latitude) {
        return ref.equals("S") ? -1.0 : 1.0 * ExifHourMinSecToDegrees(latitude);
    }

    private double ExifLongitudeToDegrees(String ref, String longitude) {
        return ref.equals("W") ? -1.0 : 1.0 * ExifHourMinSecToDegrees(longitude);
    }

    public String getGpsJpg(String filePath){
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if (exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null) {

                String latitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                String latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                String longitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                String longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);

                double lat = ExifLatitudeToDegrees(latitudeRef, latitude);
                double lon = ExifLatitudeToDegrees(longitudeRef, longitude);
                //Log.d("DEBUG", "TAG_GPS_LATITUDE=" + latitude);
                //Log.d("DEBUG", "TAG_GPS_LONGITUDE=" + longitude);

                String lonStr = String.format("%.8f",lon);
                String latStr = String.format("%.8f",lat);

                return lonStr + ":" + latStr;
            } else {
                return "";
            }
        } catch (IOException e){
            return "";
        }
    }

    public static String getDateJpg(String filePath){
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) != null) {
                //Log.d("DEBUG", "TAG DATETATIME=" + exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
                return  exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            } else {
                return "";
            }
        } catch (IOException e){
            return "";
        }
    }

    public static String getWidthJpg(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if (exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null) {
                return exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            } else {
                return "0";
            }
        } catch (IOException e) {
            return "0";
        }
    }

    public static String getHeightJpg(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            if (exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null) {
                return exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            } else {
                return "0";
            }
        } catch (IOException e) {
            return "0";
        }
    }

    public void SortDate(boolean ascent) {
        List<String>  sortList = new ArrayList<String>();
        String oneData;
        String[] SplitData;

        // create one line data
        for (int i =0; i < nameList.size(); i++) {
            oneData = dateList.get(i) + ",";
            oneData += nameList.get(i) + ",";
            oneData += pathList.get(i) + ",";
            oneData += gpsList.get(i) + ",";
            oneData += addrList.get(i)+ ",";
            oneData += memoList.get(i);
            sortList.add(oneData);
        }

        if (ascent) {
            Collections.sort(sortList);
        } else {
            Collections.sort(sortList, Collections.reverseOrder());
        }

        dateList.clear();
        nameList.clear();
        pathList.clear();
        gpsList.clear();
        addrList.clear();
        memoList.clear();

        // restore　List data
        for (int i =0; i < sortList.size(); i++) {
            oneData = sortList.get(i);
            // splitは最後の空文字列はないものとみなすので注意
            SplitData = oneData.split(",");
            dateList.add(SplitData[0]);
            nameList.add(SplitData[1]);
            pathList.add(SplitData[2]);
            if (SplitData.length >= 4)  {
                gpsList.add(SplitData[3]);
            } else {
                gpsList.add("");
            };
            if (SplitData.length >= 5)  {
                addrList.add(SplitData[4]);
            } else {
                addrList.add("");
            };
            if (SplitData.length >= 6)  {
                memoList.add(SplitData[5]);
            } else {
                memoList.add("");
            };
        }

    }
}
