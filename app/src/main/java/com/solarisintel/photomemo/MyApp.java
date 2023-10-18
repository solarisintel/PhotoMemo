package com.solarisintel.photomemo;

import android.app.Application;

import java.util.ArrayList;

// アプリケーション内で保持されるグローバル変数を持たせるクラス
// AndroidManifestの  <application android:name="MyApp" .. > と連動する
public class MyApp extends Application {

    // jpg file list
    public static JpgList jpgList = new JpgList();

    public static boolean firstStart = true;

}
