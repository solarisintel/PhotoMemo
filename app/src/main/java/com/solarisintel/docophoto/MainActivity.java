package com.solarisintel.docophoto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

//https://akira-watson.com/android/gridview-icon.html
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_EXTERNAL_STORAGE = 101;
    ArrayList<String> requestPermissions = new ArrayList<>();

    public static  SQLiteDatabase AddrDB;
    AddressDbHelper HelperAddrDB;
    public static  SQLiteDatabase MemoDB;
    MemoDbHelper HelperMemoDB;
    Context context;
    JpgList photoJpgList;
    DispAdapter splashAdapter;
    DispAdapter dispAdapter;

    public static ListView listView;
    public static ProgressBar progressBar;

    public static  Menu toolbarMenu;

    private int clickedPosition = -1;

    public static TextView bottomMsgView;

   private static final String[] SplashData = {
            "asakusa.jpg,2023/01/02,東京都",
            "daibutsu.jpg,2023/01/03,神奈川県",
            "doutonbori.jpg,2023/01/04,大阪府",
            "kinkakuji.jpg,2023/01/05,京都府",
            "miyajima.jpg,2023/01/06,広島県",
            "shibuya.jpg,2023/01/07,東京都",
            "syurijo.jpg,2023/01/08,沖縄県",
            "shika.jpg,023/01/09,奈良県",
            "tokeidai.jpg,023/01/09,北海道",
            "tree.jpg,023/01/09,東京都"

   };

    // drawableに画像を入れる、R.id.xxx はint型
    private static final int[] photoSamples = {
            R.drawable.photo_asakusa_tokyo,
            R.drawable.photo_daibutsu_kanagawa,
            R.drawable.photo_doutonbori_oosaka,
            R.drawable.photo_kinkakuji_kyoto,
            R.drawable.photo_miyajima_hirosima,
            R.drawable.photo_sibuya_tokyo,
            R.drawable.photo_syurijyo_okinawa,
            R.drawable.photo_sika_nara,
            R.drawable.photo_tokeidai_hokkaido,
            R.drawable.photo_tree_tokyo
    };

    private static final String BOTTOM_MESSAGE_START = "おおまかな場所表示です。隣町を出すこともあります";
    private static final String BOTTOM_MESSAGE_SPLASH ="撮影日付のある写真を収集中です";
    private static final String BOTTOM_MESSAGE_COLLECTING ="写真から撮影場所を解析中です";
    private static int outputMessageCount = 0;

    static boolean boolStartMain = false; // 一覧の住所検索が終了すればtrue

    private Timer bottomMsgTimer;					//タイマー用
    private MainTimerTask bottomMsgTimerTask;		//タイマタスククラス
    private Handler bottomMessageHandler = new Handler();   //UI Threadへのpost用ハンドラ

    public static void StopActionbarProgressAndStartMain() {
        toolbarMenu.findItem(R.id.menu_sort_az).setVisible(true);
        toolbarMenu.findItem(R.id.menu_sort_za).setVisible(false);
        toolbarMenu.findItem(R.id.menu_goto_first).setVisible(true);
        toolbarMenu.findItem(R.id.menu_goto_last).setVisible(true);
        toolbarMenu.findItem(R.id.menu_next_memo).setVisible(true);
        toolbarMenu.findItem(R.id.menu_progress).setVisible(false);
        bottomMsgView.setText(BOTTOM_MESSAGE_START);
        boolStartMain = true;
        listView.setOnScrollListener(null); // remove onScroll event
    }


    // permission result check
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean grantedCheck = true;
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        grantedCheck = false;
                    }
                }
            } else {
                grantedCheck = false;
            }
            if (grantedCheck == false) {
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        toolbarMenu = menu;
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_sort_az) {

            // ソート処理、配列のソート
            progressBar.setVisibility(ProgressBar.VISIBLE);
            listView.setEnabled(false);

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                photoJpgList.SortDate(true);

                // waiting code for debug
                //try{
                //    Thread.sleep(2000);
                //}catch(InterruptedException e){}

                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                    toolbarMenu.findItem(R.id.menu_sort_az).setVisible(false);
                    toolbarMenu.findItem(R.id.menu_sort_za).setVisible(true);
                    listView.setEnabled(true);
                    RedrawListView();
                });
            });
            return true;
        }
        if (itemId == R.id.menu_sort_za) {
            // ソート処理、配列のソート
            progressBar.setVisibility(ProgressBar.VISIBLE);
            listView.setEnabled(false);

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                photoJpgList.SortDate(false);
                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                    toolbarMenu.findItem(R.id.menu_sort_az).setVisible(true);
                    toolbarMenu.findItem(R.id.menu_sort_za).setVisible(false);
                    listView.setEnabled(true);
                    RedrawListView();
                });
            });
            return true;
        }
        if (itemId == R.id.menu_goto_first) {
            dispAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            return true;
        }
        if (itemId == R.id.menu_goto_last) {
            dispAdapter.notifyDataSetChanged();
            listView.setSelection(photoJpgList.nameList.size()-1);
            return true;
        }
        if (itemId == R.id.menu_next_memo) {
            ListView listView = findViewById(R.id.listView);
            int first = listView.getFirstVisiblePosition();

            int nextPos = first;
            for (int i = first; i < photoJpgList.nameList.size(); i++) {
                if (photoJpgList.memoList.get(i).length() >= 1) {
                    nextPos = i;
                    break;
                }
            }
            if (nextPos != first) {
                dispAdapter.notifyDataSetChanged();
                listView.setSelection(nextPos);
                Toast.makeText(context, String.valueOf(nextPos +1) + "行目にありました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "メモ記載の写真はありませんでした", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void RedrawListView() {

        ListView listView = findViewById(R.id.listView);
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();

        Log.d("DEBUG", "gird first = " + first + " last ="  +  last);
        View targetView;

        for (int i = first; i <= last; i++) {
            targetView = listView.getChildAt(i-first);
            listView.getAdapter().getView(i, targetView, listView);
        }

    }

    @Override
    protected void onResume() {

        // return to sub activity
        if (clickedPosition > -1) {
            String dateValue = photoJpgList.dateList.get(clickedPosition);
            String nameValue = photoJpgList.nameList.get(clickedPosition);
            String memoData = HelperMemoDB.getMemo(MemoDB, dateValue, nameValue);
            photoJpgList.memoList.set(clickedPosition, memoData);
            clickedPosition = -1;
        }

        RedrawListView();
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // permission request
        // get photo gps location from Android 10
        if (Build.VERSION.SDK_INT > 28) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
            }
        }

        // read stroage/DCIM Androi 13 READ_MEDIA_IMAGES, Android 10 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT > 32) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!requestPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[0]), REQUEST_EXTERNAL_STORAGE);
        }

        photoJpgList = new JpgList();

        context = getApplicationContext();

        // ListView(Image+Text)
        listView = findViewById(R.id.listView);

        // Screen Center Progress bar
        progressBar = findViewById(R.id.progressbar);

        splashAdapter = new DispAdapter(this.getApplicationContext(),
                R.layout.list_item, SplashData, photoSamples);

        // ListViewにadapterをセット
        listView.setAdapter(splashAdapter);

        // Change color Title bar background
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#808000")));

        // スプラッシュ画面のメッセージ
        bottomMsgView = findViewById(R.id.bottom_msg);
        bottomMsgView.setText(BOTTOM_MESSAGE_SPLASH);

        // 簡易住所検索が終了したらtrue
        boolStartMain = false;

        // スクロールバーの動作検知イベント、デバッグ用
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        break;
                    default:
                }
            }
            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
            }
        });

        AddrDB = new AddressDbHelper(context).getReadableDatabase();
        HelperAddrDB = new AddressDbHelper(context);

        MemoDB = new MemoDbHelper(context).getReadableDatabase();
        HelperMemoDB = new MemoDbHelper(context);

        // Singleの別スレッドを立ち上げる
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Collectin jpg files
                String dcimPath;

                DiskInfo deviceVols = new DiskInfo();
                deviceVols.getVolPaths(context);

                // デバイスの分だけ全部jpg fileを取得
                if (deviceVols.volPaths.size() > 1) {
                    for (String pathName : deviceVols.volPaths) {
                        dcimPath = pathName + "/DCIM/";
                        photoJpgList.Collect(dcimPath);
                    }
                } else {
                    dcimPath = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/";
                    photoJpgList.Collect(dcimPath);
                }

                Log.d("DEBUG", "Collecting files =" + photoJpgList.nameList.size());

                //HelperMemoDB.DebugDump(MemoDB);

                // default is date newer sort
                if (photoJpgList.nameList.size() > 1) {
                    photoJpgList.SortDate(false);
                }

                // first address set for first display area
                int firstLoop = 20;
                if (firstLoop > photoJpgList.gpsList.size()) {
                    firstLoop = photoJpgList.gpsList.size();
                }

                // １ページ表示に必要な分だけ最初に住所を取得しておく
                for (int i = 0 ; i < firstLoop ; i++){
                    String dateValue = photoJpgList.dateList.get(i);
                    String nameValue = photoJpgList.nameList.get(i);
                    String value = photoJpgList.gpsList.get(i);
                    if (! value.equals("")) {
                        String[] strDisp = value.split(",");
                        if (strDisp.length >= 2) {
                            String jpgLat = strDisp[0];
                            String jpgLon = strDisp[1];
                            String TownName = HelperAddrDB.GetNearTown(AddrDB, Double.parseDouble(jpgLat), Double.parseDouble(jpgLon));
                            photoJpgList.addrList.set(i, TownName);
                            Log.d("DEBUG", "added loc, addr =" + value + "," + TownName);
                        }
                        String memoData = HelperMemoDB.getMemo(MemoDB, dateValue, nameValue);
                        photoJpgList.memoList.set(i, memoData);
                        Log.d("DEBUG", "added memo =" + memoData + " from date=" + dateValue + " fname=" + nameValue);
                    }
                }

                // waiting code for debug
                //try{
                //    Thread.sleep(30000); // 30sec Sleep
                //}catch(InterruptedException e){}

                // finalize process
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                            // no jpg files check
                            if (photoJpgList.nameList.size() > 0) {
                                // Action bar progress start
                                toolbarMenu.findItem(R.id.menu_progress).setVisible(true);

                                dispAdapter = new DispAdapter(this.getApplicationContext(),
                                        R.layout.list_item, photoJpgList);

                                // Real Photo Adapter set to ListView
                                listView.setAdapter(null);
                                listView.setAdapter(dispAdapter);
                                dispAdapter.notifyDataSetChanged();

                                // 収集中を表示
                                bottomMsgView.setText(BOTTOM_MESSAGE_COLLECTING);

                                // list view enable click
                                listView.setOnItemClickListener(this);

                                //全ファイルの位置情報解析に時間がかかるので、メッセージを定期的に変更する
                                //タイマーインスタンス生成
                                bottomMsgTimer = new Timer();
                                //タスククラスインスタンス生成
                                bottomMsgTimerTask = new MainTimerTask();
                                //タイマースケジュール設定＆開始
                                bottomMsgTimer.schedule(bottomMsgTimerTask, 1000,4000);

                            } else {
                                Toast.makeText(context , "no photo jpg files ", Toast.LENGTH_LONG).show();
                                bottomMsgView.setText("日付付きの写真がありません。");
                            }
                        }

                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    // リストをクリックしたら詳細画面を表示
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        Log.d("DEBUG", "Request Show SubActivity");
        Intent intent = new Intent(getApplication(), SubActivity.class);
        String jpgPath = photoJpgList.pathList.get(position).toString();
        clickedPosition = position;
        intent.putExtra("IMAGEPATH", jpgPath);
        startActivity( intent );

    }


    // タイマータスク: run()に定周期で処理したい内容を記述
    public class MainTimerTask extends TimerTask {

        String ResultMessage;

        @Override
        public void run() {

            //ここに定周期で実行したい処理を記述します
            bottomMessageHandler.post( new Runnable() {
                public void run() {
                    //すべての検索が終わったらタイマークラス終了
                    if (boolStartMain) {
                        bottomMsgView.setText(BOTTOM_MESSAGE_START);
                        bottomMsgTimer.cancel();
                        return;
                    }

                   //実行間隔分を加算処理
                    outputMessageCount += 1;
                    ResultMessage = "";
                    if (outputMessageCount == 1) {
                        ResultMessage = "写真から撮影場所を解析中です";
                    }
                    if (outputMessageCount == 2) {
                        if (photoJpgList.nameList.size() < 200) {
                            ResultMessage = "写真が" + String.valueOf(photoJpgList.nameList.size()) + "枚あります。解析はすぐ終わります";
                        } else {
                            ResultMessage = "写真が" + String.valueOf(photoJpgList.nameList.size()) + "枚あります。時間がかかります";
                        }
                    }
                    if (outputMessageCount == 3) {
                        ResultMessage = "一覧での場所は、おおよその場所です";
                    }
                    if (outputMessageCount == 4) {
                        ResultMessage = "おおよその場所のため、隣町が出ることもあります";
                    }
                    if (outputMessageCount == 5) {
                        ResultMessage = "詳細での場所表示では少し正確です";
                    }
                    if (outputMessageCount == 6) {
                        ResultMessage = "詳細では地図で場所を確認できます";
                    }
                    if (outputMessageCount >= 7) {
                        ResultMessage = "地図にマピオンを利用しています。感謝です";
                        outputMessageCount = 0;
                    }
                    //画面にメッセージを表示
                    bottomMsgView.setText(ResultMessage);
                }
            });
        }
    }
}
