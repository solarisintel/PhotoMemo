package com.solarisintel.photomemo;

import android.content.Context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;


public class MainGridAdapter extends BaseAdapter {

    static class ViewHolder {
        ImageView imageView;
        TextView dateView;
        TextView markView;
        ImageView imageMemo;
    }

    private final LayoutInflater inflater;
    private final int layoutId;

    MainGridAdapter(Context context,
                    int layoutId) {
        super();
        this.inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.layoutId = layoutId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            // main.xml の <GridView .../> に grid_items.xml を inflate して convertView とする
            convertView = inflater.inflate(layoutId, parent, false);
            // ViewHolder を生成
            holder = new ViewHolder();

            holder.imageView = convertView.findViewById(R.id.image_view);
            holder.dateView = convertView.findViewById(R.id.text_date);
            holder.markView = convertView.findViewById(R.id.text_mark);
            holder.imageMemo = convertView.findViewById(R.id.image_memo);

            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.imageView.setImageBitmap(null);

        // when position is over jpg files, do clear previous image and text
        if ((position +1) > MyApp.jpgList.imgList.size() ) {
            holder.dateView.setText("");
            return convertView;
        }

        String[] jpgdata = MyApp.jpgList.imgList.get(position).toString().split(",");
        String jpgDate = jpgdata[0];
        String jpgname = jpgdata[1];
        String jpgPath = jpgdata[2];
        //Log.d("DEBUG", "getView position =" + position + " name =" + jpgname);

        // display text is yyyy/mm/dd
        String dispDate = jpgDate.replaceAll(":", "/").substring(0,10);
        holder.dateView.setText(dispDate);

        if (jpgdata.length >= 4) {
            String jpgMemo = jpgdata[3];
            if (jpgMemo.length() > 0) {
                //holder.markView.setText("m");
                holder.imageMemo.setImageResource(R.drawable.pin);
            }
        } else {
            holder.markView.setText("");
            holder.imageMemo.setImageResource(android.R.color.transparent);
        }

        String urlPath = "file://" + jpgPath;
        Glide.with(holder.imageView.getContext())
                .load(urlPath)
                .sizeMultiplier(0.2f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imageView);

        return convertView;
    }

    @Override
    public int getCount() {
        // collected jpg files count
        return MyApp.jpgList.imgList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


}