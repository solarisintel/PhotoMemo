package com.solarisintel.photomemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import java.util.Collections;
import java.util.concurrent.Executors;

public class ListMainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    Context context;
    ListMainAdapter dispAdapter;
    public static ListView listView;
    public static ProgressBar progressBar;

    public static Menu toolbarMenu;

    private int clickedPosition = -1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list_main, menu);
        toolbarMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_list_goto_first) {
            listView.setSelection(0);
            ShowPageNumber(0);
            return true;
        }

        if (itemId == R.id.menu_list_goto_last) {
            listView.setSelection(dispAdapter.getCount() -1);
            ShowPageNumber(dispAdapter.getCount() -1);
            return true;
        }

        if (itemId == R.id.menu_change_grid) {
            Intent intent = new Intent(getApplication(), MainActivity.class);
            startActivity( intent );
            finish();
            return true;
        }

        if (itemId == R.id.menu_list_sort_asc) {
            progressBar.setVisibility(ProgressBar.VISIBLE);

            listView.setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_sort_asc).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_sort_desc).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_goto_last).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_goto_first).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_next_memo).setEnabled(false);

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                Collections.sort(MyApp.jpgList.imgList);
                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    // この順番、enableにしてからsetSelection
                    listView.setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_sort_asc).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_sort_desc).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_goto_last).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_goto_first).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_next_memo).setEnabled(true);
                    listView.setSelection(0);
                    RedrawListView();
                    ShowPageNumber(0);
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                });
            });
            return true;
        }

        if (itemId == R.id.menu_list_sort_desc) {
            progressBar.setVisibility(ProgressBar.VISIBLE);

            listView.setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_sort_asc).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_sort_desc).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_goto_last).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_goto_first).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_list_next_memo).setEnabled(false);

            Executors.newSingleThreadExecutor().execute(() -> {
                // 時間がかかる処理をここに書く
                Collections.sort(MyApp.jpgList.imgList, Collections.reverseOrder());
                // 処理終了後のUIに関係する処理
                HandlerCompat.createAsync(getMainLooper()).post(() -> {
                    // この順番、enableにしてからsetSelection
                    listView.setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_sort_asc).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_sort_desc).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_goto_last).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_goto_first).setEnabled(true);
                    toolbarMenu.findItem(R.id.menu_list_next_memo).setEnabled(true);
                    listView.setSelection(0);
                    RedrawListView();
                    ShowPageNumber(0);
                    progressBar.setVisibility(ProgressBar.INVISIBLE);
                });
            });
            return true;
        }

        if (itemId == R.id.menu_list_next_memo) {
            int start = listView.getFirstVisiblePosition();
            boolean found = false;
            for (int i = start; i < MyApp.jpgList.imgList.size(); i++) {
                if (MyApp.jpgList.imgList.get(i).toString().contains(",M") == true ) {
                    listView.setSelection(i);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_main);

        context = getApplicationContext();
        // ListView(Image+Text)
        listView = findViewById(R.id.listView);
        // Screen Center Progress bar
        progressBar = findViewById(R.id.progressbar);

        // Change color Title bar background
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#808000")));

        dispAdapter = new ListMainAdapter(this.getApplicationContext());

        // Real Photo Adapter set to ListView
        listView.setAdapter(null);
        listView.setAdapter(dispAdapter);
        dispAdapter.notifyDataSetChanged();

        // list view enable click
        listView.setOnItemClickListener(this);

        // スクロールバーの動作検知イベント、デバッグ用
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        ShowPageNumber(listView.getFirstVisiblePosition());
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

    }



    @Override
    protected void onResume() {

        // return to sub activity
        if (clickedPosition > -1) {
            //String dateValue = photoJpgList.dateList.get(clickedPosition);
            //String nameValue = photoJpgList.nameList.get(clickedPosition);
            //String memoData = HelperMemoDB.getMemo(MemoDB, dateValue, nameValue);
            //photoJpgList.memoList.set(clickedPosition, memoData);
            clickedPosition = -1;
        }

        RedrawListView();
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        ShowPageNumber(listView.getFirstVisiblePosition());
        super.onResume();
    }

    // リストをクリックしたら詳細画面を表示
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        Intent intent = new Intent(getApplication(), SubActivity.class);
        intent.putExtra("POSITION", String.valueOf(position));
        clickedPosition = position;
        startActivity( intent );
    }

    public static void StopActionbarProgressAndStartMain() {
        toolbarMenu.findItem(R.id.menu_list_sort_asc).setEnabled(true);
        toolbarMenu.findItem(R.id.menu_list_sort_desc).setEnabled(false);
        toolbarMenu.findItem(R.id.menu_list_goto_first).setEnabled(true);
        toolbarMenu.findItem(R.id.menu_list_goto_last).setEnabled(true);
        toolbarMenu.findItem(R.id.menu_list_next_memo).setEnabled(true);

        listView.setOnScrollListener(null); // remove onScroll event
    }

    private void ShowPageNumber(int i) {
        if (MyApp.jpgList.imgList.size() > 8) {
            int dataDispCount = MyApp.jpgList.imgList.size();

            int first = listView.getFirstVisiblePosition();
            int last = listView.getLastVisiblePosition();
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
}
