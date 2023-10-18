package com.solarisintel.docophoto;

import static android.content.Context.STORAGE_SERVICE;

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DiskInfo {

    public List<String> volPaths =  new ArrayList<String>();
    public void getVolPaths(Context context) {
        StorageManager sm = (StorageManager) context.getSystemService(STORAGE_SERVICE);

        List<StorageVolume> volList = sm.getStorageVolumes();

        for (StorageVolume volume : volList) {
            String vpath = null;
            // Android 11 - 13
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                vpath = volume.getDirectory().toString();
                volPaths.add(vpath);
                Log.d("DEBUG",  "Android 11-13 vol Path = " + vpath);
            } else {
                // Android 7 - 10
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    try {
                        //noinspection JavaReflectionMemberAccess
                        vpath = (String) volume.getClass().getMethod("getPath").invoke(volume);
                    } catch (Exception e) {
                        vpath = null;
                    }
                    if (vpath == null || "".equals(vpath)) {
                        continue;
                    }
                    volPaths.add(vpath);
                    Log.d("DEBUG",  "Android 7-10 vol Path = " + vpath);
                }
            }
        }
     }
}
