package com.frogobox.basegameboard2048.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.frogobox.basegameboard2048.R;
import com.frogobox.basegameboard2048.base.ui.BaseActivity;
import com.frogobox.basegameboard2048.util.helper.FirstLaunchManager;
import com.frogobox.basegameboard2048.view.pager.MainPagerAdapter;

import java.io.File;

import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Const.FILE_STATE;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Ext.TXT;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Extra.EXTRA_FILENAME;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Extra.EXTRA_N;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Extra.EXTRA_NEW;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Extra.EXTRA_POINTS;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Extra.EXTRA_UNDO;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Pref.PREF_CURRENT_PAGE;
import static com.frogobox.basegameboard2048.util.helper.ConstHelper.Pref.PREF_MY;


public class MainActivity extends BaseActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private int currentPage = 0;
    private SharedPreferences.Editor editor;

    private int[] layouts = new int[]{
            R.layout.fragment_games_box,
            R.layout.fragment_games_box,
            R.layout.fragment_games_box,
            R.layout.fragment_games_box
    };
    private boolean[] gameResumeable = new boolean[]{
            false,
            false,
            false,
            false
    };
    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            currentPage = position;
            editor.putInt(PREF_CURRENT_PAGE, currentPage);
            editor.commit();
            updateButtons(position);

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();

        FirstLaunchManager firstLaunchManager = new FirstLaunchManager(this);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);

        //checking resumable
        File directory = getFilesDir();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                Log.i("files", file.getName());
                for (int j = 0; j < gameResumeable.length; j++) {
                    if (file.getName().equals(FILE_STATE + (j + 4) + TXT))
                        gameResumeable[j] = true;
                }
            }
        }


        // adding bottom dots
        addBottomDots(0);

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        MainPagerAdapter mainPagerAdapter = new MainPagerAdapter(layoutInflater, layouts);

        viewPager.setAdapter(mainPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(PREF_MY, Context.MODE_PRIVATE);
        editor = preferences.edit();
        currentPage = preferences.getInt(PREF_CURRENT_PAGE, 0);
        viewPager.setCurrentItem(currentPage);
        updateButtons(currentPage);
    }


    private void addListener(Button b1, Button b2, int n) {
        final int temp = n;
        b1.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_N, temp);
            intent.putExtra(EXTRA_POINTS, 0);
            intent.putExtra(EXTRA_NEW, true);
            intent.putExtra(EXTRA_FILENAME, FILE_STATE + temp + TXT);
            intent.putExtra(EXTRA_UNDO, false);
            createBackStack(intent);
            setupShowAdsInterstitial();
        });
        b2.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(EXTRA_N, temp);
            intent.putExtra(EXTRA_NEW, false);
            intent.putExtra(EXTRA_FILENAME, FILE_STATE + temp + TXT);
            intent.putExtra(EXTRA_UNDO, false);
            createBackStack(intent);
            setupShowAdsInterstitial();
        });
    }

    private void createBackStack(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            TaskStackBuilder builder = TaskStackBuilder.create(this);
            builder.addNextIntentWithParentStack(intent);
            builder.startActivities();
        } else {
            startActivity(intent);
            finish();
        }
    }

    private void addBottomDots(int currentPage) {
        TextView[] dots = new TextView[layouts.length];

        int activeColor = ContextCompat.getColor(this, R.color.dot_light_screen);
        int inactiveColor = ContextCompat.getColor(this, R.color.dot_dark_screen);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(inactiveColor);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(activeColor);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }


    public void updateButtons(int position) {
        Button newGameButton = MainActivity.this.findViewById(R.id.button_newGame);
        Button continueButton = MainActivity.this.findViewById(R.id.button_continueGame);
        try {
            if (gameResumeable[position])
                continueButton.setBackgroundResource(R.drawable.standalone_button);
            else
                continueButton.setBackgroundResource(R.drawable.inactive_button);

            continueButton.setEnabled(gameResumeable[position]);
        } catch (Exception aie) {
            aie.printStackTrace();
        }
        addListener(newGameButton, continueButton, position + 4);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_menu_setting:
                startActivity(new Intent(this, SettingsActivity.class));
                setupShowAdsInterstitial();
                return true;
            case R.id.toolbar_menu_stats:
                startActivity(new Intent(this, StatsActivity.class));
                setupShowAdsInterstitial();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
