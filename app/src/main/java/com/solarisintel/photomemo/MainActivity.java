package com.solarisintel.photomemo;

import androidx.appcompat.app.AppCompatActivity;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.AdapterView;

import android.Manifest;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemClickListener {

    private Context context;
    private static final int REQUEST_EXTERNAL_STORAGE = 101;
    ArrayList<String> requestPermissions = new ArrayList<>();
    private ArrayList<String> prePERMISSIONS = new ArrayList<String>();
    private PermissionUtility permissionUtility;

    public static GridView gridView;
    private static MainGridAdapter gridAdapter;
    private static final String ASSET_JPGFILE = "no_image.jpg";
    public static ProgressBar progressBar;
    private static String noFilesMsgPath;
    private static List<String> deviceVolPaths;

    public static ReScanJpgFiles asyncReScanJpgFiles;

    public static  MemoDbHelper HelperMemoDB;
    public static SQLiteDatabase readMemoDB;
    public static SQLiteDatabase writeMemoDB;

    public static Menu toolbarMenuMain = null;

    public void assetsJpgCopy() {

        Log.d("DEBUG", "assets copy starting");

        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!storageDir.exists()) storageDir.mkdirs();

            Log.d("DEBUG", "local storage path=" + storageDir.toString() +  "/" + ASSET_JPGFILE);

            InputStream inputStream = getAssets().open(ASSET_JPGFILE);

            FileOutputStream fileOutputStream = new FileOutputStream(storageDir.toString() +  "/" + ASSET_JPGFILE, false);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) >= 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.close();
            inputStream.close();
            Log.d("DEBUG", "assets copy compileted");

        } catch (IOException e) {
            Log.d("DEBUG", "assets copy failed");
            e.printStackTrace();
        }
    }

    /*

    // permission result check
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                // exit this application
                finish();
            }
        }
    }
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("DEBUG", "onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        toolbarMenuMain = menu;
        return super.onCreateOptionsMenu(menu);
    }

    public static void RedrawGridView() {

        int first = gridView.getFirstVisiblePosition();
        int last = gridView.getLastVisiblePosition();

        Log.d("DEBUG", "gird first = " + first + " last ="  +  last);
        View targetView;

        for (int i = first; i <= last; i++) {
            targetView = gridView.getChildAt(i-first);
            gridView.getAdapter().getView(i, targetView, gridView);
        }
    }

    private static void DisableMenuAndGridView() {
        gridView.setClickable(false);
        gridView.setEnabled(false);
        gridView.setVerticalScrollBarEnabled(false);

        toolbarMenuMain.findItem(R.id.menu_sort_asc).setEnabled(false);
        toolbarMenuMain.findItem(R.id.menu_sort_desc).setEnabled(false);
        toolbarMenuMain.findItem(R.id.menu_goto_first).setEnabled(false);
        toolbarMenuMain.findItem(R.id.menu_goto_last).setEnabled(false);
        toolbarMenuMain.findItem(R.id.menu_next_memo).setEnabled(false);
    }
    private static void EnableMenuAndGridView() {
        gridView.setClickable(true);
        gridView.setEnabled(true);
        gridView.setVerticalScrollBarEnabled(true);

        toolbarMenuMain.findItem(R.id.menu_sort_asc).setEnabled(true);
        toolbarMenuMain.findItem(R.id.menu_sort_desc).setEnabled(true);
        toolbarMenuMain.findItem(R.id.menu_goto_first).setEnabled(true);
        toolbarMenuMain.findItem(R.id.menu_goto_last).setEnabled(true);
        toolbarMenuMain.findItem(R.id.menu_next_memo).setEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_next_memo) {

            int start = gridView.getFirstVisiblePosition();
            boolean found = false;
            for (int i = start; i < MyApp.jpgList.imgList.size(); i++) {
                if (MyApp.jpgList.imgList.get(i).toString().contains(",M") == true ) {
                    gridView.setSelection(i);
                    ShowPageNumber(i);
                    found = true;
                    break;
                }
            }
            if (found == false) {
                Toast.makeText(context,R.string.not_found_memo, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (itemId == R.id.menu_goto_first) {
            gridView.setSelection(0);
            ShowPageNumber(0);
            return true;
        }
        if (itemId == R.id.menu_goto_last) {
            gridView.setSelection(gridAdapter.getCount() -1);
            ShowPageNumber(gridAdapter.getCount() -1);
            return true;
        }

        if (itemId == R.id.menu_sort_asc) {

            progressBar.setVisibility(android.widget.ProgressBar.VISIBLE);
            DisableMenuAndGridView();

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                Collections.sort(MyApp.jpgList.imgList);
                // test code
                //try{
                //    Thread.sleep(5000); //数秒Sleepする
                //}catch(InterruptedException e){}
                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    // この順番、enableにしてからsetSelection
                    EnableMenuAndGridView();
                    gridView.setSelection(0);
                    RedrawGridView();
                    ShowPageNumber(0);
                    //gridAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(android.widget.ProgressBar.INVISIBLE);
                });
            });

            return true;
        }
        if (itemId == R.id.menu_sort_desc) {
            progressBar.setVisibility(android.widget.ProgressBar.VISIBLE);
            DisableMenuAndGridView();

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                Collections.sort(MyApp.jpgList.imgList, Collections.reverseOrder());
                // test code
                //try{
                //    Thread.sleep(5000); //数秒Sleepする
                //}catch(InterruptedException e){}

                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    // この順番、enableにしてからsetSelection
                    EnableMenuAndGridView();
                    gridView.setSelection(0);
                    RedrawGridView();
                    ShowPageNumber(0);
                    //gridAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(android.widget.ProgressBar.INVISIBLE);
                });
            });
            return true;
        }

        if (itemId == R.id.menu_change_list) {
            Intent intent = new Intent(getApplication(), ListMainActivity.class);
            startActivity( intent );
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void AppPermissionsCheck() {
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


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(permissionUtility.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            Log.d("DEBUG", "onRequestPermissionsResult OK");
            // 権限チェックのダイアログでOKした後
            // Manifestにandroid:launchMode="singleTop"を入れない
            Intent intent = new Intent(getApplication(), MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.d("DEBUG", "onRequestPermissionsResult FALSE");
            // 許可しない場合
            finish();
        }
    }

    private void CreatePermissons () {
        // permission request
        // get photo gps location from Android 10
        if (Build.VERSION.SDK_INT > 28) {
            prePERMISSIONS.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
        }
        // read stroage/DCIM Androi 13 READ_MEDIA_IMAGES, Android 10 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT > 32) {
            prePERMISSIONS.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            prePERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

    }

    private void ShowPageNumber(int i) {
        // 直接最後に移動した場合のPositionの計算
        if (MyApp.jpgList.imgList.size() > 8) {
            int dataDispCount = MyApp.jpgList.imgList.size();
            if (MyApp.jpgList.imgList.size() % 2 == 1) dataDispCount++;

            int first = gridView.getFirstVisiblePosition();
            int last = gridView.getLastVisiblePosition();
            int dispCount = last - first;

            int DispLastPosition = dataDispCount - dispCount - 1;
            if (DispLastPosition < i) {
                i = DispLastPosition;
            }
        } else {
            i = 0;
        }
        getSupportActionBar().setTitle(String.format("%4d/%4d", i + 1, MyApp.jpgList.imgList.size()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // アプリケーション権限チェック
        boolean AppConitune = false;
        CreatePermissons();
        String PERMISSIONS[] = prePERMISSIONS.toArray(new String[prePERMISSIONS.size()]);
        permissionUtility = new PermissionUtility(this, PERMISSIONS);
        Log.d("DEBUG", "Permission count is " + prePERMISSIONS.size());

        if (permissionUtility.arePermissionsEnabled()) {
            Log.d("DEBUG", "Permission granted");
            AppConitune = true;
        } else {
            permissionUtility.requestMultiplePermissions();
            Log.d("DEBUG", "call requestMultiplePermissions");
        }
        // 初回目は権限リクエストを受信しない限り画面処理しない
        if (AppConitune == false) {
            Log.d("DEBUG", "could not receive onRequestPermissionsResult");
            return;
        }

        // ここからがActivityのスタート
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        // GridViewのインスタンスを生成
        gridView = findViewById(R.id.gridview);
        // BaseAdapter を継承したGridAdapterのインスタンスを生成
        // 子要素のレイアウトファイル grid_items.xml を
        // activity_main.xml に inflate するためにGridAdapterに引数として渡す
        gridAdapter = new MainGridAdapter(this.getApplicationContext(),
                R.layout.grid_items);

        // item をクリックしたとき
        gridView.setOnItemClickListener(this);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#808000")));
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setTitle("[タイトル]");

        progressBar = findViewById(R.id.progressbar_main);

        DiskInfo dk = new DiskInfo();
        deviceVolPaths = dk.getVolPaths(context);

        noFilesMsgPath = ASSET_JPGFILE + "," + getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + "/" + ASSET_JPGFILE;

        HelperMemoDB = new MemoDbHelper(context);
        readMemoDB = new MemoDbHelper(context).getReadableDatabase();
        writeMemoDB = new MemoDbHelper(context).getWritableDatabase();

        /// 初回起動
        if (MyApp.firstStart == true) {

            MyApp.firstStart = false;

            progressBar.setVisibility(android.widget.ProgressBar.VISIBLE);

            assetsJpgCopy();

            // 最初は操作できないようにしておく
            gridView.setClickable(false);
            gridView.setEnabled(false);
            gridView.setVerticalScrollBarEnabled(false);

            //gridview.setAdapter(adapter); // ここでは割り当てない
            asyncReScanJpgFiles = new ReScanJpgFiles();
            asyncReScanJpgFiles.execute();
        } else {

            gridView.setAdapter(gridAdapter);

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                do {
                    try {
                        Thread.sleep(100); //　0.3secぐらい待たないとMenuが有効にならない
                    } catch (InterruptedException e) {
                    }
                } while (toolbarMenuMain == null);

                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    // この順番、enableにしてからsetSelection
                    EnableMenuAndGridView();
                    gridView.setSelection(0);
                    RedrawGridView();
                    ShowPageNumber(0);
                    progressBar.setVisibility(android.widget.ProgressBar.INVISIBLE);
                });
            });
        }

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    ShowPageNumber(gridView.getFirstVisiblePosition());
                }

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int currentFirstVisPos = view.getFirstVisiblePosition();
            }
        });

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        progressBar.setVisibility(android.widget.ProgressBar.VISIBLE);
        Log.d("DEBUG", "Showing SubActivity position=" + String.valueOf(position));
        Intent intent = new Intent(getApplication(), SubActivity.class);
        intent.putExtra("POSITION", String.valueOf(position));
        startActivity( intent );
    }


    @Override
    protected void onResume() {

        if ( MyApp.firstStart == false ) {
            RedrawGridView();
        }
        super.onResume();
    }

    private static class ReScanJpgFiles {
        ExecutorService executorService;
        public ReScanJpgFiles() {
            super();
            executorService  = Executors.newSingleThreadExecutor();
        }
        private class TaskRun implements Runnable {

            @Override
            public void run() {

                MyApp.jpgList.imgList.clear();

                String dcimPath;

                if (deviceVolPaths.size() > 1) {
                    //Log.d("DEBUG", "this device has multi storage num=" + deviceVolPaths.size());
                    for (String pathName : deviceVolPaths) {
                        Log.d("DEBUG", "DCIM path = " + pathName);
                        dcimPath = pathName + "/DCIM/";
                        MyApp.jpgList.walk(dcimPath);
                    }
                } else {
                    dcimPath = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM + "/";
                    //Log.d("DEBUG", "single DCIM path =" + dcimPath);
                    MyApp.jpgList.walk(dcimPath);
                }

                // when no files, change to asset folder
                if (MyApp.jpgList.imgList.size() == 0) {
                    MyApp.jpgList.imgList.add(noFilesMsgPath);
                } else {
                    // 日付の降順（最新順）
                    Collections.sort(MyApp.jpgList.imgList, Collections.reverseOrder());
                }

                Log.d("DEBUG", "DCIM jpg Files =" + MyApp.jpgList.imgList.size());

                // test code
                //try{
                //  Thread.sleep(5000); //数秒Sleepする
                //}catch(InterruptedException e){}

                while (toolbarMenuMain == null) {
                    Log.d("DEBUG", "Option Menu is not yet created");
                    try{
                      Thread.sleep(100); //　0.1sec
                    }catch(InterruptedException e){}
                }


                // finalize
                new Handler(Looper.getMainLooper())
                        .post(() -> onPostExecute());
            }
        }

        void execute() {
            executorService.submit(new TaskRun());
        }
        void onPostExecute() {
            progressBar.setVisibility(android.widget.ProgressBar.INVISIBLE);
            gridView.setAdapter(gridAdapter);

            // onCreateOptionsMenu()はonCreate()の後で実行されるので、適当なwaitを入れている
            EnableMenuAndGridView();

        }
    }
}
