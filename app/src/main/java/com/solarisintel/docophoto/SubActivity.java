package com.solarisintel.docophoto;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.os.HandlerCompat;

import com.ortiz.touchview.TouchImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;

/*
 イメージ表示は、touchimageview を使用
　https://pisuke-code.com/android-touchimageview-usage/

　settings.gradle に以下を追加
  maven { url 'https://jitpack.io' }

  build.gradleに以下を追加
      implementation 'com.github.MikeOrtiz:TouchImageView:3.2.1'

  layoutのImageViewを com.ortiz.touchview.TouchImageViewに変えるだけで使える

*/

public class SubActivity extends AppCompatActivity {


    private static Bitmap fileBitmap;
    private static Bitmap fileBitmapRotated90;
    private static Bitmap fileBitmapRotated180;
    private static Bitmap fileBitmapRotated270;
    private static TouchImageView imageView;
    private static TouchImageView imageViewRotated90;
    private static TouchImageView imageViewRotated180;
    private static TouchImageView imageViewRotated270;

    private static int prevVisibleImage;
    private static int prevVisibleImage90;
    private static int prevVisibleImage180;
    private static int prevVisibleImage270;

    ProgressBar progressBar;

    float rotateDegress = 0.0f;

    private String latLon;

    private String showPath;
    private String DisplayPhotoDate;
    private String DisplayJpgName;

    private Context context;
    private MemoDbHelper HelperMemoDB;
    private SQLiteDatabase ReadMemoDB;
    private SQLiteDatabase WriteMemoDB;

    private boolean nowRotateProgress = false;

    public static  Menu toolbarMenu;


    private WebView mapWebView;
    private static int mapZoomParam = 15;
    private int   mapUrlHeight;
    private int   mapUrlWidth;
    private String mapUrlLat;
    private String mapUrlLon;

    // mapionでは日本以外の緯度経度を指定すると「住所不明」になる。
    private String mapWhereName;

    // ダブルタップ連続操作でダイアログが２重に出る現象を回避
    private boolean processingMemoDialog = false;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sub, menu);
        toolbarMenu = menu;
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        context = getApplicationContext();
        ReadMemoDB = new MemoDbHelper(context).getReadableDatabase();
        WriteMemoDB = new MemoDbHelper(context).getWritableDatabase();
        HelperMemoDB = new MemoDbHelper(context);

        // show full size image file
        Intent intent = getIntent();
        showPath = intent.getStringExtra("IMAGEPATH");
        imageView = findViewById(R.id.image_view);

        imageViewRotated90 = findViewById(R.id.image_view_sub90);
        imageViewRotated180 = findViewById(R.id.image_view_sub180);
        imageViewRotated270 = findViewById(R.id.image_view_sub270);

        // Back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#808000")));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        latLon = new JpgList().getGpsJpg(showPath);

        // map setting
        mapWebView = findViewById(R.id.map_view);
        mapWebView.setVisibility(View.INVISIBLE);

        // memo setting
        TextView textMemo = findViewById(R.id.text_memo);
        textMemo.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Log.d("DEBUG", "SINGLE click");
            }
            @Override
            public void onDoubleClick(View v) {
                DoInputMemo();
                Log.d("DEBUG", "memo DOUBLE click");
            }
        });

        // address setting
        TextView addressView = findViewById(R.id.text_address);
        addressView.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Log.d("DEBUG", "SINGLE click");
            }
            @Override
            public void onDoubleClick(View v) {
                if (latLon.split(":").length >= 2) {
                    if (mapWebView.getVisibility() == View.INVISIBLE) {
                        ChangeMapFromMenu();
                    } else {
                        ChangePhotoFromMenu();
                    }
                }
                Log.d("DEBUG", "address DOUBLE click");
            }
        });
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
    protected void onResume() {
        Log.d("DEBUG", "Called Resume");

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        fileBitmap = JpgUtil.decodeSampledBitmapFromFile(showPath, displayMetrics.widthPixels, displayMetrics.heightPixels);
        imageView.setImageBitmap(fileBitmap);


        // 回転マトリックス作成（90度回転）
        Matrix mat = new Matrix();

        int bitWidth = fileBitmap.getWidth();
        int bitHeight = fileBitmap.getHeight();

        // 再利用されたときに前の回転状態が残っている
        imageView.setRotation(0);
        imageViewRotated90.setRotation(0);
        imageViewRotated180.setRotation(0);
        imageViewRotated270.setRotation(0);

        // 回転したビットマップを作成
        mat.postRotate(90);
        fileBitmapRotated90 = Bitmap.createBitmap(fileBitmap, 0, 0, bitWidth, bitHeight, mat, true);
        imageViewRotated90.setImageBitmap(fileBitmapRotated90);

        mat.postRotate(90);
        fileBitmapRotated180 = Bitmap.createBitmap(fileBitmap, 0, 0, bitWidth, bitHeight, mat, true);
        imageViewRotated180.setImageBitmap(fileBitmapRotated180);

        mat.postRotate(90);
        fileBitmapRotated270 = Bitmap.createBitmap(fileBitmap, 0, 0, bitWidth, bitHeight, mat, true);
        imageViewRotated270.setImageBitmap(fileBitmapRotated270);

        progressBar = findViewById(R.id.progress_bar_img);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        DisplayJpgName = new File(showPath).getName();

        final String TITILE_DATE = "に撮影";

        String setPhotoDate = "撮影日の記録なし";
        DisplayPhotoDate = new JpgList().getDateJpg(showPath);
        try {
            Date ymd = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss").parse(DisplayPhotoDate);

            String yobi[] = {"（日）","（月）","（火）","（水）","（木）","（金）","（土）"};

            Calendar cl = Calendar.getInstance();
            cl.setTime(ymd);

            String JpnWeekday =  yobi[cl.get(Calendar.DAY_OF_WEEK)-1];

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
            setPhotoDate = dateFormat.format(ymd) + JpnWeekday + TITILE_DATE;

        } catch (ParseException e) {
            setPhotoDate = "date is unknown　(ParseException)";
        }
        TextView textDate = findViewById(R.id.text_date);
        textDate.setVisibility(View.VISIBLE);
        textDate.setText(setPhotoDate);

        latLon = new JpgList().getGpsJpg(showPath);
        String loc[] = latLon.split(":");

        if (loc.length >= 2) {

            // https://nominatim.openstreetmap.org/reverse?lat=35.69977778&lon=139.77170000&format=json
            //String webURL = "https://nominatim.openstreetmap.org/reverse?lat=" + loc[1] + "&lon=" + loc[0] + "&format=json";

            //https://www.mapion.co.jp/m2/33.563222983431736,131.73243059576487,15
            String webURL = "https://www.mapion.co.jp/smp/m2/" + loc[1] + "," + loc[0] + ",15";

            Log.d("DEBUG", "Web URL=" + webURL);
            dowloadWebpage_mapion(webURL);

        } else {
            Log.d("DEBUG", "TAG_GPS_LONGITUDE is nothing ");
            TextView addressView = findViewById(R.id.text_address);
            addressView.setText("撮影場所の記録がありません");
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        }

        String DisplayMemo = HelperMemoDB.getMemo(ReadMemoDB, DisplayPhotoDate, DisplayJpgName);
        TextView textMemo = findViewById(R.id.text_memo);
        textMemo.setText(DisplayMemo);

        MainActivity.progressBar.setVisibility(ProgressBar.INVISIBLE);

        super.onResume();
    }


    private void ChangeMapFromMenu() {

        String loc[] = latLon.split(":");

        if (loc.length < 2) {
            return ;
        }

        if (mapWhereName.contains("住所不明")) {
            return;
        }

        mapUrlLat = loc[0];
        mapUrlLon = loc[1];

        prevVisibleImage = imageView.getVisibility();
        prevVisibleImage90 = imageViewRotated90.getVisibility();
        prevVisibleImage180 = imageViewRotated180.getVisibility();
        prevVisibleImage270 = imageViewRotated270.getVisibility();

        imageView.setVisibility(View.INVISIBLE);
        imageViewRotated90.setVisibility(View.INVISIBLE);
        imageViewRotated180.setVisibility(View.INVISIBLE);
        imageViewRotated270.setVisibility(View.INVISIBLE);

        MenuItem hide_item = toolbarMenu.findItem(R.id.menu_turn);
        hide_item.setVisible(false);

        MenuItem showItemZoomIn = toolbarMenu.findItem(R.id.menu_zoom_in);
        showItemZoomIn.setVisible(true);

        MenuItem showItemZoomOut = toolbarMenu.findItem(R.id.menu_zoom_out);
        showItemZoomOut.setVisible(true);

        MenuItem disableItemPhoto = toolbarMenu.findItem(R.id.menu_photo);
        disableItemPhoto.setEnabled(true);

        MenuItem disableItemMap = toolbarMenu.findItem(R.id.menu_map);
        disableItemMap.setEnabled(false);

        mapWebView.setVisibility(View.VISIBLE);
        ShowMap();

    }

    private void ChangePhotoFromMenu() {
        mapWebView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(prevVisibleImage);
        imageViewRotated90.setVisibility(prevVisibleImage90);
        imageViewRotated180.setVisibility(prevVisibleImage180);
        imageViewRotated270.setVisibility(prevVisibleImage270);

        MenuItem hide_item = toolbarMenu.findItem(R.id.menu_turn);
        hide_item.setVisible(true);

        MenuItem showItemZoomIn = toolbarMenu.findItem(R.id.menu_zoom_in);
        showItemZoomIn.setVisible(false);

        MenuItem showItemZoomOut = toolbarMenu.findItem(R.id.menu_zoom_out);
        showItemZoomOut.setVisible(false);

        MenuItem disableItemPhoto = toolbarMenu.findItem(R.id.menu_photo);
        disableItemPhoto.setEnabled(false);

        MenuItem disableItemMap = toolbarMenu.findItem(R.id.menu_map);
        disableItemMap.setEnabled(true);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_turn) {

            // disable click for animation playing
            if (nowRotateProgress == true) {
                return true;
            }
            nowRotateProgress = true;

            rotateDegress = rotateDegress + 90.0f;
            if (rotateDegress >= 360.0f) {
                rotateDegress = 0.0f;
            }

            // TouchImageViewでsetRotation(90)を使うとピンチイン時のHeightが変更されない不具合が出る
            // 回避するために対応しています。

            RotateAnimation rotate = new RotateAnimation(-90.0f, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(1000);  // animation duration


            if (rotateDegress == 90.0f) {
                Log.d("DEBUG", "Rotate 0->90");
                imageView.setZoom(1.0f);
                imageView.setRotation(90.0f);
                imageView.startAnimation(rotate);
            }

            if (rotateDegress == 180.0f) {
                Log.d("DEBUG", "Rotate 90->180");
                imageViewRotated90.setZoom(1.0f);
                imageViewRotated90.setRotation(90.0f);
                imageViewRotated90.startAnimation(rotate);
            }

            if (rotateDegress == 270.0f) {
                Log.d("DEBUG", "Rotate 180->270");
               imageViewRotated180.setZoom(1.0f);
                imageViewRotated180.setRotation(90.0f);
                imageViewRotated180.startAnimation(rotate);
            }

            if (rotateDegress == 0.0f) {
                Log.d("DEBUG", "Rotate 270->0");
                imageViewRotated270.setZoom(1.0f);
                imageViewRotated270.setRotation(90.0f);
                imageViewRotated270.startAnimation(rotate);
            }

            rotate.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (rotateDegress == 90.0f) {
                        Log.d("DEBUG", "Rotate 0->90 END");
                        imageView.setVisibility(View.INVISIBLE);
                        imageViewRotated90.setVisibility(View.VISIBLE);
                        //前のsetRotationの状態を記憶しているので元に戻す
                        imageViewRotated180.setRotation(0);
                    }
                    if (rotateDegress == 180.0f) {
                        Log.d("DEBUG", "Rotate 90->180 END");
                        imageViewRotated90.setVisibility(View.INVISIBLE);
                        imageViewRotated180.setVisibility(View.VISIBLE);
                        //前のsetRotationの状態を記憶しているので元に戻す
                        imageViewRotated270.setRotation(0);
                    }
                    if (rotateDegress == 270.0f) {
                        Log.d("DEBUG", "Rotate 180->270 END");
                        imageViewRotated180.setVisibility(View.INVISIBLE);
                        imageViewRotated270.setVisibility(View.VISIBLE);
                        //前のsetRotationの状態を記憶しているので元に戻す
                        imageView.setRotation(0);
                    }
                    if (rotateDegress == 0.0f) {
                        imageViewRotated270.setVisibility(View.INVISIBLE);
                        imageView.setVisibility(View.VISIBLE);
                        //前のsetRotationの状態を記憶しているので元に戻す
                        imageViewRotated90.setRotation(0);
                    }
                    nowRotateProgress = false;

                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            return true;
       }
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        if (itemId == R.id.menu_photo) {
            ChangePhotoFromMenu();
            return true;

        }
        if (itemId == R.id.menu_map) {
            ChangeMapFromMenu();
            return true;
        }

        if (itemId == R.id.menu_zoom_in) {
            if (mapZoomParam < 17) {
                mapZoomParam += 1;
                ShowMap();
            }
            return true;
        }
        if (itemId == R.id.menu_zoom_out) {
            if (mapZoomParam > 12) {
                mapZoomParam -= 1;
                ShowMap();
            }
            return true;
        }

        if (itemId == R.id.menu_edit) {

            DoInputMemo();

            return true;
        }
        if (itemId == R.id.menu_info_show) {
            // show jpg info, memo and action bar menu change
            MenuItem hide_item = toolbarMenu.findItem(R.id.menu_info_hide);
            hide_item.setVisible(true);
            item.setVisible(false);

            MenuItem disable_item = toolbarMenu.findItem(R.id.menu_edit);
            disable_item.setEnabled(true);

            CardView cardInfo = findViewById(R.id.carview_info);
            cardInfo.setVisibility(View.VISIBLE);

            LinearLayout layoutImage =findViewById(R.id.linear_image);
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    MATCH_PARENT,
                    1.0f
            );
            layoutImage.setLayoutParams(param);

            LinearLayout screen = findViewById(R.id.layout_screen);
            screen.requestLayout();

            return true;
        }
        if (itemId == R.id.menu_info_hide) {
            // hide jpg info, memo and action bar menu change
            MenuItem show_item = toolbarMenu.findItem(R.id.menu_info_show);
            show_item.setVisible(true);
            item.setVisible(false);

            MenuItem disable_item = toolbarMenu.findItem(R.id.menu_edit);
            disable_item.setEnabled(false);

            CardView cardInfo = findViewById(R.id.carview_info);
            cardInfo.setVisibility(View.INVISIBLE);

            LinearLayout layoutImage =findViewById(R.id.linear_image);
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    MATCH_PARENT,
                    0.0f
            );
            layoutImage.setLayoutParams(param);

            LinearLayout screen = findViewById(R.id.layout_screen);
            screen.requestLayout();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        Log.d("DEBUG", "Called onStop");

        // force free Bitmap resource
        imageView.setImageBitmap(null);
        imageViewRotated90.setImageBitmap(null);
        imageViewRotated180.setImageBitmap(null);
        imageViewRotated270.setImageBitmap(null);
        fileBitmap = null;
        fileBitmapRotated90 = null;
        fileBitmapRotated180 = null;
        fileBitmapRotated270 = null;

        super.onStop();
    }

    private void dowloadWebpage_mapion(String urlSt){

        final String startKeyword  = "<title>";
        final String lastKeyword = "の地図";

        final String DISP_TITLE = "撮影場所\n";

        mapWhereName = "";

        // Singleの別スレッドを立ち上げる
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(urlSt);
                HttpURLConnection urlCon =  (HttpURLConnection) url.openConnection();

                // timeout
                urlCon.setReadTimeout(10000); // msec
                urlCon.setConnectTimeout(20000); // msec

                // http method
                urlCon.setRequestMethod("GET");

                // no redirect
                urlCon.setInstanceFollowRedirects(false);

                InputStream is = urlCon.getInputStream();
                int responseCode = urlCon.getResponseCode();
                Log.d("DEBUG", "Web response code = " + responseCode);

                String responseData = convertToString(urlCon.getInputStream());

                // 別スレッド内での処理を管理し実行する
                HandlerCompat.createAsync(getMainLooper()).post(() ->{
                        // Viewに直接追記する
                        if (responseCode == 200) {
                            int endPos = 0;
                            int firstPos = responseData.indexOf(startKeyword);
                            if (firstPos > 0) {
                                String tmpStr = responseData.substring(firstPos);
                                endPos = tmpStr.indexOf(lastKeyword);
                                if (endPos > 0) {
                                    Log.d("DEBUG", "Web response data = " + tmpStr.substring(50));
                                    mapWhereName = tmpStr.substring(startKeyword.length(), endPos);
                                }

                                Log.d("DEBUG", "Web location =" + mapWhereName);
                                TextView addressView = findViewById(R.id.text_address);
                                addressView.setText(DISP_TITLE + mapWhereName);

                                MenuItem item = toolbarMenu.findItem(R.id.menu_map);
                                if (mapWhereName.contains("住所不明")) {
                                    item.setEnabled(false);
                                } else {
                                    item.setEnabled(true);
                                }
                            }
                        }
                    }
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        });
    }

    public String convertToString(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
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

            Rect r = mRect;
            Paint paint = mPaint;

            int onebaseline = getLineBounds(0, r);
            int count = getHeight() / onebaseline;
            Log.d("DEBUG", "line baseline= " + onebaseline + " count=" + count);

            for (int i = 0; i <= count; i++) {
                int baseline = onebaseline * i + i*4; // 4 is margin

                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            super.onDraw(canvas);
        }
    }

    private String MapCreateURL() {

        //https://mapfan.com/map?c=35.72267961004853,140.33195357343152,12&s=std,pc,ja&p=none
        //https://cm01.mapion.co.jp/m2/map?usr=atlas_org&island=org&lon=131.732430595&lat=33.5632229834&level=15&size=480x640&fxobj=centermark,copyright,scalebar&icon=home|131.73243059576487,33.563222983431736

        String url1 = "https://cm01.mapion.co.jp/m2/map?usr=atlas_org&island=org&";
        String url2 = "lat=" + mapUrlLon; // なぜか元のURLのパラメータが誤っている。
        String url3 = "&lon=" + mapUrlLat;// なぜか元のURLのパラメータが誤っている。
        String url4 = "&level=" + mapZoomParam + "&size=" + mapUrlWidth + "x" + mapUrlHeight;
        String url5 = "&fxobj=centermark,copyright,scalebar&icon=home|";
        String url6 = mapUrlLat + "," + mapUrlLon;
        String retURL = url1 + url2 + url3 + url4 + url5 + url6;

        return retURL;

    }

    private void ShowMap( ) {

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        mapUrlHeight = (int) dpHeight;
        mapUrlWidth = (int) dpWidth;

        String showURL = MapCreateURL();
        Log.d("DEBUG", "SHOW URL=" + showURL);

        // enable JavaScript
        mapWebView.getSettings().setJavaScriptEnabled(true);
        // Pinch In Zoom no display control tool
        mapWebView.getSettings().setSupportZoom(true);
        mapWebView.getSettings().setBuiltInZoomControls(true);
        mapWebView.getSettings().setDisplayZoomControls(false);
        mapWebView.loadUrl(showURL);

    }

    private void DoInputMemo() {

        // 2重起動対策
        if (processingMemoDialog == true) return ;

        processingMemoDialog = true;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText input = new EditText(this);
        int maxLength = 80;
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        input.setGravity(Gravity.TOP);
        input.setMaxLines(5);
        input.setLines(5);
        input.setTextSize(14); // font size
        input.setBackground(context.getDrawable(R.drawable.round_outline));
        layout.setPadding(10, 10, 10, 10);
        input.setHint("5行80文字までメモ入力できます");

        TextView textMemo = findViewById(R.id.text_memo);
        input.setText(textMemo.getText());

        layout.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setMessage(DisplayJpgName + "のメモ");
        builder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().trim();
                        if (value.length() == 0) {
                            HelperMemoDB.Delete(WriteMemoDB,DisplayPhotoDate, DisplayJpgName);
                        } else {
                            HelperMemoDB.Save(WriteMemoDB,DisplayPhotoDate, DisplayJpgName, value);

                        }
                        String DisplayMemo = HelperMemoDB.getMemo(ReadMemoDB, DisplayPhotoDate, DisplayJpgName);
                        TextView textMemo = findViewById(R.id.text_memo);
                        textMemo.setText(DisplayMemo);
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
}