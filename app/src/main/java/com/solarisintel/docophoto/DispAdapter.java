package com.solarisintel.docophoto;

import static android.os.Looper.getMainLooper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.os.HandlerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.concurrent.Executors;


public class DispAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final int layoutID;

    private static boolean splashMode = true;

    private String[] splashDispList = null;
    private Bitmap[] splashPhotolist = null;

    private static JpgList photoList;

    private SearchAddressTask asyncSearchAddressTask;

    public static SQLiteDatabase AddrDB;
    public static AddressDbHelper HelperAddrDB;
    public static SQLiteDatabase MemoDB;
    public static MemoDbHelper HelperMemoDB;

    public static final String EXIST_MEMO_MARK = "メモ";

    static class ViewHolder {
        TextView TvName;
        TextView TvDate;
        TextView TvWhere;
        ImageView img;
        TextView TvMemoMark;
    }

    DispAdapter(Context context, int itemLayoutId,
                String[] infos, int[] photos) {

        inflater = LayoutInflater.from(context);
        layoutID = itemLayoutId;

        splashMode = true;

        splashDispList = infos;
        // bitmapの配列
        splashPhotolist = new Bitmap[photos.length];
        // drawableのIDからbitmapに変換
        for (int i = 0; i < photos.length; i++) {
            splashPhotolist[i] = BitmapFactory.decodeResource(context.getResources(), photos[i]);
        }
    }

    DispAdapter(Context context, int itemLayoutId, JpgList photos) {

        inflater = LayoutInflater.from(context);
        layoutID = itemLayoutId;

        splashMode = false;
        photoList = photos;

        AddrDB = new AddressDbHelper(context).getReadableDatabase();
        HelperAddrDB = new AddressDbHelper(context);

        MemoDB = new MemoDbHelper(context).getReadableDatabase();
        HelperMemoDB = new MemoDbHelper(context);

        // address search thread run start
        asyncSearchAddressTask = new SearchAddressTask();
        asyncSearchAddressTask.execute();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(layoutID, null);
            holder = new ViewHolder();
            holder.img = convertView.findViewById(R.id.img_item);
            holder.TvName = convertView.findViewById(R.id.text_name);
            holder.TvDate = convertView.findViewById(R.id.text_date);
            holder.TvWhere = convertView.findViewById(R.id.text_where);
            holder.TvMemoMark = convertView.findViewById(R.id.text_memo_mark);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String str;
        String strPath;
        String strDisp[];


        if (splashMode == true) {
            holder.img.setImageBitmap(splashPhotolist[position]);

            str = splashDispList[position];
            strDisp = str.split(",");

            holder.TvName.setText(strDisp[0]);
            holder.TvDate.setText(strDisp[1]);
            holder.TvWhere.setText(strDisp[2]);
            holder.TvMemoMark.setText("");
        } else {
            String jpgDate = photoList.dateList.get(position).toString();
            if (jpgDate.length() >= 10 ) {
                jpgDate = jpgDate.substring(0, 10);
                jpgDate = jpgDate.replace(":", "/");
            }
            holder.TvDate.setText(jpgDate);
            holder.TvName.setText(photoList.nameList.get(position).toString());
            holder.TvWhere.setText(photoList.addrList.get(position).toString());

            // メモがある場合
            if (photoList.memoList.get(position).trim().length() > 0) {
                holder.TvMemoMark.setText(EXIST_MEMO_MARK);
            } else {
                holder.TvMemoMark.setText("");
            }

            strPath = photoList.pathList.get(position).toString();
            String urlPath = "file://" + strPath;
            Glide.with(holder.img.getContext())
                    .load(urlPath)
                    .sizeMultiplier(0.2f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.img);

        }

        return convertView;
    }

    @Override
    public int getCount() {
        if (splashMode) {
            return splashDispList.length;
        } else {
            return photoList.nameList.size();
        }
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class SearchAddressTask {

        void execute() {
            Executors.newSingleThreadExecutor().execute(() -> {

                Log.d("DEBUG", "Collecting address files count =" + photoList.gpsList.size());

                for (int i = 0 ; i < photoList.gpsList.size() ; i++){
                    String fileName = photoList.nameList.get(i);
                    String dateValue = photoList.dateList.get(i);

                    String value = photoList.gpsList.get(i);
                    if (! value.equals("")) {
                        String[] strDisp = value.split(":");
                        if (strDisp.length >= 2) {
                            String jpgLat = strDisp[0];
                            String jpgLon = strDisp[1];
                            String TownName = HelperAddrDB.GetNearTown(AddrDB, Double.parseDouble(jpgLat), Double.parseDouble(jpgLon));
                            photoList.addrList.set(i, TownName);
                            //Log.d("DEBUG", "get addr =" + fileName + "," + TownName);
                        }
                    }
                    String memoData = HelperMemoDB.getMemo(MemoDB, dateValue, fileName);
                    photoList.memoList.set(i, memoData);
                   // Log.d("DEBUG", "get memo =" +memoData);
                }

                // waiting code for debug
                try{
                    Thread.sleep(1000); // Sleep seconds
                }catch(InterruptedException e){}

                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    MainActivity.StopActionbarProgressAndStartMain();
                });
            });
        }
    }
}