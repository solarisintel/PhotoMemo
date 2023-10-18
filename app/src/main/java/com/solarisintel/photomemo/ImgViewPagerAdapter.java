package com.solarisintel.photomemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.ortiz.touchview.TouchImageView;

import java.util.List;

public class ImgViewPagerAdapter extends PagerAdapter
{
    LayoutInflater _inflater = null;

    private int displayWidth;
    private int displayHeight;
    private TouchImageView imgView;

    private Bitmap imgBitmap;

    public ImgViewPagerAdapter(Context context )
    {
        super();
        _inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        LinearLayout layout = (LinearLayout)_inflater.inflate(R.layout.content_img, null);

        //描画する
        imgView = (TouchImageView)layout.findViewById(R.id.image_view_img);

        String[] jpgdata = MyApp.jpgList.imgList.get(position).toString().split(",");
        String jpgDate = jpgdata[0];
        String jpgname = jpgdata[1];
        String jpgPath = jpgdata[2];

        DisplayMetrics displayMetrics = container.getResources().getDisplayMetrics();
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        JpgUtil jpgUtl = new JpgUtil();
        imgBitmap = jpgUtl.decodeSampledBitmapFromFile(jpgPath, displayWidth, displayHeight);
        imgView.setImageBitmap(imgBitmap);

        // 表示されているviewを取得するためTagを利用する
        layout.setTag(position);

        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        ((ViewPager) container).removeView((View) object);
    }

    @Override
    public int getCount()
    {
        return MyApp.jpgList.imgList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

}
