package com.solarisintel.photomemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import com.ortiz.touchview.TouchImageView;

// https://blog.csdn.net/JMW1407/article/details/114273649

public class SubActivity extends AppCompatActivity {

    private Context context;
    ProgressBar progressBar;
    public static Menu toolbarMenu;

    private ViewPager imgViewPager;
    private ImgViewPagerAdapter imgViewPagerAdapter;
    TextView memoView;
    TextView dateView;
    LinearLayout layoutImage;
    LinearLayout layoutContainter;
    CardView cardInfo;

    // ダブルタップ連続操作でダイアログが２重に出る現象を回避
    private boolean processingMemoDialog = false;

    private float rotateDegress = 0.0f;
    private int rotatePosition = -1;

    private int displayWidth;
    private int displayHeight;

    Bitmap imgBitmapRotated0;
    Bitmap imgBitmapRotated90;
    Bitmap imgBitmapRotated180;
    Bitmap imgBitmapRotated270;
    private boolean doingImageTurn = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sub, menu);
        toolbarMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        if (itemId == R.id.menu_info_edit) {
            DoInputMemo();
            return true;
        }
        if (itemId == R.id.menu_turn) {
            if (doingImageTurn == true)
                return true;

            doingImageTurn = true;

            rotatePosition =  imgViewPager.getCurrentItem(); // 今表示されている場所
            String[] jpgdata = MyApp.jpgList.imgList.get(rotatePosition).toString().split(",");
            String jpgDate = jpgdata[0];
            String jpgname = jpgdata[1];
            String jpgPath = jpgdata[2];

            // 回転マトリックス作成（90度回転）
            Matrix mat = new Matrix();

            JpgUtil jpgUtl = new JpgUtil();
            imgBitmapRotated0 = jpgUtl.decodeSampledBitmapFromFile(jpgPath, displayWidth, displayHeight);

            int bitWidth = imgBitmapRotated0.getWidth();
            int bitHeight = imgBitmapRotated0.getHeight();

            // 回転したビットマップを作成
            mat.postRotate(90);
            imgBitmapRotated90 = Bitmap.createBitmap(imgBitmapRotated0, 0, 0, bitWidth, bitHeight, mat, true);
            mat.postRotate(90);
            imgBitmapRotated180 = Bitmap.createBitmap(imgBitmapRotated0, 0, 0, bitWidth, bitHeight, mat, true);
            mat.postRotate(90);
            imgBitmapRotated270 = Bitmap.createBitmap(imgBitmapRotated0, 0, 0, bitWidth, bitHeight, mat, true);

            // 回転を加算
            rotateDegress = rotateDegress + 90.0f;
            if (rotateDegress == 360.0f) {
                rotateDegress = 0.0f;
            }

            // PagerViewのviewを取得、あらかじめTagに保存しているView(layout)を取得
            View currentView = imgViewPager.findViewWithTag(rotatePosition);

            ImageView currentImg = currentView.findViewById(R.id.image_view_img);
            RotateAnimation rotate = new RotateAnimation(-90.0f, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(750);  // animation duration

            rotate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    doingImageTurn = false;
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            if (rotateDegress == 90.0f) {
                currentImg.setImageBitmap(imgBitmapRotated90);
            }
            if (rotateDegress == 180.0f) {
                currentImg.setImageBitmap(imgBitmapRotated180);
            }
            if (rotateDegress == 270.0f) {
                currentImg.setImageBitmap(imgBitmapRotated270);
            }
            if (rotateDegress == 0.0f) {
                currentImg.setImageBitmap(imgBitmapRotated0);
            }
            // currentImg.setRotation(rotateDegress);
            currentImg.startAnimation(rotate);


            return true;
        }

        if (itemId == R.id.menu_info_hide) {
            HideInfoCard();
            return true;
        }
        if (itemId == R.id.menu_info_show) {
            ShowInfoCard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void HideInfoCard() {
        // show jpg info, memo and action bar menu change
        MenuItem item = toolbarMenu.findItem(R.id.menu_info_hide);
        item.setVisible(false);

        MenuItem hide_item = toolbarMenu.findItem(R.id.menu_info_show);
        hide_item.setVisible(true);

        CardView cardInfo = findViewById(R.id.carview_info);
        cardInfo.setVisibility(View.INVISIBLE);

        LinearLayout layoutImage =findViewById(R.id.linear_image);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT,
                0.0f
        );
        layoutImage.setLayoutParams(param);
        layoutContainter.requestLayout();

        imgViewPagerAdapter.notifyDataSetChanged(); // これで表示の更新が行われる

    }
    private void ShowInfoCard() {
        // show jpg info, memo and action bar menu change
        MenuItem otherItem = toolbarMenu.findItem(R.id.menu_info_hide);
        otherItem.setVisible(true);

        MenuItem hide_item = toolbarMenu.findItem(R.id.menu_info_show);
        hide_item.setVisible(false);


        CardView cardInfo = findViewById(R.id.carview_info);
        cardInfo.setVisibility(View.VISIBLE);

        LinearLayout layoutImage =findViewById(R.id.linear_image);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT,
                1.0f
        );
        layoutImage.setLayoutParams(param);
        layoutContainter.requestLayout();

        imgViewPagerAdapter.notifyDataSetChanged(); // これで表示の更新が行われる

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        context = getApplicationContext(); // for Toast()

        layoutContainter = findViewById(R.id.container);
        layoutImage = findViewById(R.id.linear_image);
        dateView = findViewById(R.id.text_date);
        memoView = findViewById(R.id.text_memo);
        cardInfo = findViewById(R.id.carview_info);

        // Mainから番号をもらう
        Intent intent = getIntent();
        int dispPosition  = Integer.parseInt(intent.getStringExtra("POSITION"));
        Log.d("DEBUG", "Request get SubActivity position=" + String.valueOf(dispPosition));


        // 画面の表示サイズを得る
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        // ViewPagerの作成
        imgViewPager = (ViewPager) findViewById(R.id.viewpager);
        imgViewPagerAdapter = new ImgViewPagerAdapter(this);

        imgViewPager.addOnPageChangeListener(new PageChangeListener());

        imgViewPager.setAdapter(imgViewPagerAdapter);

        imgViewPager.setCurrentItem(dispPosition, false);
        imgViewPagerAdapter.notifyDataSetChanged(); // これで写真表示が行われる

        // Back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#808000")));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // memo double click
        memoView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Log.d("DEBUG", "memo SINGLE click");
            }
            @Override
            public void onDoubleClick(View v) {
                DoInputMemo();
                Log.d("DEBUG", "memo DOUBLE click");
            }
        });

        dateView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Log.d("DEBUG", "date SINGLE click");
            }
            @Override
            public void onDoubleClick(View v) {
                HideInfoCard();
                Log.d("DEBUG", "date DOUBLE click");
            }
        });

        // 撮影日の表示
        String[] currentJpgData = MyApp.jpgList.imgList.get(dispPosition).toString().split(",");
        String currentJpgDate = currentJpgData[0];
        String currentJpgname = currentJpgData[1];
        String currentJpgPath = currentJpgData[2];
        // display text is yyyy/mm/dd
        String dispDate = currentJpgDate.replaceAll(":", "/").substring(0,10);
        dateView.setText(dispDate);

        // メモ内容の表示
        String DisplayMemo = MainActivity.HelperMemoDB.getMemo(MainActivity.readMemoDB, currentJpgDate, currentJpgname);
        memoView.setText(DisplayMemo);


    }


    // ダブルタップを実装させるためのクラス
    public abstract class DoubleClickListener implements View.OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                onDoubleClick(v);
            } else {
                onSingleClick(v);
            }
            lastClickTime = clickTime;
        }

        public abstract void onSingleClick(View v);
        public abstract void onDoubleClick(View v);
    }

    @Override
    protected void onStop() {
        MainActivity.progressBar.setVisibility(android.widget.ProgressBar.INVISIBLE);
        //Log.d("DEBUG", "Called onStop");
        super.onStop();
    }

    // A custom EditText that draws lines between each line of text that is displayed.
    public static class LinedEditText extends androidx.appcompat.widget.AppCompatEditText {
        private Rect mRect;
        private Paint mPaint;

        // we need this constructor for LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Rect r = mRect;
            Paint paint = mPaint;
            // これが正しい
            int firstBaseline = getLineBounds(0, r);
            int nextBaseline = getLineHeight();
            int count = getHeight() / nextBaseline;

            canvas.drawLine(r.left, firstBaseline + 1, r.right, + firstBaseline+ 1, paint);
            for (int i = 1; i <= count; i++) {
                int baseline = nextBaseline * i + 12*i;
                canvas.drawLine(r.left, baseline + firstBaseline + 1, r.right, baseline + + firstBaseline+ 1, paint);
            }
        }
    }

    // A custom TextView that draws lines between each line of text that is displayed.
    public static class LinedTextView extends androidx.appcompat.widget.AppCompatTextView {
        private Rect mRect;
        private Paint mPaint;

        // we need this constructor for LayoutInflater
        public LinedTextView(Context context, AttributeSet attrs) {
            super(context, attrs);

            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Rect r = mRect;
            Paint paint = mPaint;
            // これが正しい
            int firstBaseline = getLineBounds(0, r);
            int nextBaseline = getLineHeight();
            int count = getHeight() / nextBaseline;

            canvas.drawLine(r.left, firstBaseline + 1, r.right, + firstBaseline+ 1, paint);
            for (int i = 1; i <= count; i++) {
                int baseline = nextBaseline * i + 12*i;
                canvas.drawLine(r.left, baseline + firstBaseline + 1, r.right, baseline + + firstBaseline+ 1, paint);
            }
        }
    }

    private void DoInputMemo() {

        // 2重起動対策
        if (processingMemoDialog == true) return ;

        //int position = viewPager2.getCurrentItem();
        int position = imgViewPager.getCurrentItem();

        String[] jpgdata = MyApp.jpgList.imgList.get(position).toString().split(",");
        String jpgDate = jpgdata[0];
        String jpgName = jpgdata[1];
        String jpgPath = jpgdata[2];

        processingMemoDialog = true;
        String inputValue = MainActivity.HelperMemoDB.getMemo(MainActivity.readMemoDB, jpgDate, jpgName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Dialog用レイアウトの読み込み
        LayoutInflater factory = LayoutInflater.from(this);
        View diagView = factory.inflate(R.layout.dialog_memo, null);
        EditText input = (EditText) diagView.findViewById(R.id.edit_memo);
        input.setText(inputValue);

        builder.setView(diagView);
        builder.setMessage(jpgName + "のメモ");
        builder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().trim();
                        if (value.length() == 0) {
                            MainActivity.HelperMemoDB.Delete(MainActivity.writeMemoDB,jpgDate, jpgName);
                        } else {
                            MainActivity.HelperMemoDB.Save(MainActivity.writeMemoDB,jpgDate, jpgName, value);
                        }
                        String DisplayMemo = MainActivity.HelperMemoDB.getMemo(MainActivity.readMemoDB, jpgDate, jpgName);
                        TextView textMemo = findViewById(R.id.text_memo);
                        textMemo.setText(DisplayMemo);
                        // 配列の入れ替え
                        MyApp.jpgList.imgList.set(position, jpgDate + "," + jpgName + "," + jpgPath + "," + "M");
                        processingMemoDialog = false;
                        dialog.dismiss();

                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        processingMemoDialog = false;
                    }});

        // back buttonによるキャンセル対策
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                processingMemoDialog = false;
                dialog.dismiss();
            }
        });
        builder.show();

    }


    // ViewPagerのページ変更のリスナー
    // ３つのOverrideの関数が必須
    private class PageChangeListener extends ViewPager.SimpleOnPageChangeListener
    {
        // 以下の３つのOverrideの関数は必須
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //Log.d("DEBUG", "onPageScrolled position=" + position);
            View resetView = imgViewPager.findViewWithTag(position);
            // 隠しているviewの中に存在している
            if (resetView != null ) {
                TouchImageView imageView = resetView.findViewById(R.id.image_view_img);
                //Log.d("DEBUG", "onPageScrolled zoom value =" + imageView.getCurrentZoom());
                if (imageView.getCurrentZoom() != 1.0f)  {
                    Log.d("DEBUG", "onPageScrolled reset zoom, position =" + position);
                    imageView.resetZoom();
                }

            }

            super.onPageScrolled( position,  positionOffset,  positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            // Log.d("DEBUG", "onPageSelected position=" + position);

            // 撮影日の表示
            String[] currentJpgData = MyApp.jpgList.imgList.get(position).toString().split(",");
            String currentJpgDate = currentJpgData[0];
            String currentJpgname = currentJpgData[1];
            String currentJpgPath = currentJpgData[2];
            // display text is yyyy/mm/dd
            String dispDate = currentJpgDate.replaceAll(":", "/").substring(0,10);
            dateView.setText(dispDate);

            // メモ内容の表示
            String DisplayMemo = MainActivity.HelperMemoDB.getMemo(MainActivity.readMemoDB, currentJpgDate, currentJpgname);
            memoView.setText(DisplayMemo);

            // 他のViewのリセット
            rotateDegress = 0.0f; // ページ替えしたら回転状態はリセットする

            // 回転操作をさせてそれが表示ページではなくて、隠れている他のViewにある場合、画像を元に戻す
            if (rotatePosition >= 0) {
                if (rotatePosition != position) {
                    View resetView = imgViewPager.findViewWithTag(rotatePosition);
                    // 隠しているviewの中に存在している
                    if (resetView != null ) {
                        String[] jpgdata = MyApp.jpgList.imgList.get(rotatePosition).toString().split(",");
                        String jpgDate = jpgdata[0];
                        String jpgname = jpgdata[1];
                        String jpgPath = jpgdata[2];

                        JpgUtil jpgUtl = new JpgUtil();
                        Bitmap resetBitmap = jpgUtl.decodeSampledBitmapFromFile(jpgPath, displayWidth, displayHeight);
                        TouchImageView resetImageView = resetView.findViewById(R.id.image_view_img);
                        resetImageView.setImageBitmap(resetBitmap);
                        resetImageView.resetZoom();

                        Log.d("DEBUG", "onPageSelected reset image view position=" + rotatePosition);

                    }
                }
                rotatePosition = -1;
            }
            //Log.d("DEBUG", "onPageSelected position=" + position);
            //Log.d("DEBUG", "Pager keeping page num=" + imgViewPager.getChildCount());
            super.onPageSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
    }



}