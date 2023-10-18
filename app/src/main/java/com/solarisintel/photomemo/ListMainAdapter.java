package com.solarisintel.photomemo;

import static android.os.Looper.getMainLooper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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


public class ListMainAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final int layoutID;

    static class ViewHolder {
        TextView TvDate;
        ImageView img;
        TextView TvMemoText;
    }


    ListMainAdapter(Context context) {

        inflater = LayoutInflater.from(context);
        layoutID = R.layout.list_item;

    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(layoutID, null);
            holder = new ViewHolder();
            holder.img = convertView.findViewById(R.id.img_item);
            holder.TvDate = convertView.findViewById(R.id.text_date);
            holder.TvMemoText = convertView.findViewById(R.id.text_memo);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String[] jpgdata = MyApp.jpgList.imgList.get(position).toString().split(",");
        String jpgDate = jpgdata[0];
        String jpgname = jpgdata[1];
        String jpgPath = jpgdata[2];
        String displayJpgDate;

        displayJpgDate = "";
        if (jpgDate.length() >= 10 ) {
            displayJpgDate = jpgDate.substring(0, 10);
            displayJpgDate = displayJpgDate.replace(":", "/");
        }

        holder.TvDate.setText(displayJpgDate);

        // メモ内容の表示
        String DisplayMemo = MainActivity.HelperMemoDB.getMemo(MainActivity.readMemoDB, jpgDate, jpgname);

        if (DisplayMemo.length() > 0) {
            holder.TvMemoText.setBackground(convertView.getContext().getDrawable(R.drawable.round_outline));
        } else {
            holder.TvMemoText.setBackground(convertView.getContext().getDrawable(R.drawable.round_outline_gray));
        }
        holder.TvMemoText.setText(DisplayMemo);

        String urlPath = "file://" + jpgPath;
        Glide.with(holder.img.getContext())
                .load(urlPath)
                .sizeMultiplier(0.2f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.img);

        return convertView;
    }

    @Override
    public int getCount() {
        return MyApp.jpgList.imgList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}