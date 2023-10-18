package com.solarisintel.docophoto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class JpgUtil {
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // 画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        // 画像が横長
        if (width > height) {
            if (width > reqWidth) {
                inSampleSize = (int)Math.ceil((float) width / (float) reqWidth);
            }
        } else {// 画像が縦長,回転させるので横幅で計算させる
            //if (height > reqHeight) {
                inSampleSize = (int)Math.ceil((float) height / (float) reqWidth);
            //}
        }

        return inSampleSize;
    }
    public static Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {

        // inJustDecodeBounds=true で画像のサイズをチェック
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // inSampleSize を計算
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // inSampleSize をセットしてデコード
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

}
