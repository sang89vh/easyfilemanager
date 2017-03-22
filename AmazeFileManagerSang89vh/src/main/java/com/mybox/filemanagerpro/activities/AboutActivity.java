package com.mybox.filemanagerpro.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mybox.filemanagerpro.R;
import com.mybox.filemanagerpro.utils.PreferenceUtils;
import com.mybox.filemanagerpro.utils.theme.AppTheme;

import java.util.Random;

/**
 * Created by vishal on 27/7/16.
 */
public class AboutActivity extends BasicActivity implements View.OnClickListener {

    private static final int HEADER_HEIGHT = 1024;
    private static final int HEADER_WIDTH = 500;

    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private Toolbar mToolbar;
    private TextView mTitleTextView;
    private RelativeLayout mVersion;
    private RelativeLayout mXda, mRate;
    private ImageView mLicensesIcon;
    private int mCount=0;
    private Toast mToast;
    private SharedPreferences mSharedPref;
    private View mAuthorsDivider;

    private static final String KEY_PREF_STUDIO = "studio";
    private static final String URL_REPO_RATE = "market://details?id=com.mybox.filemanagerpro";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getAppTheme().equals(AppTheme.DARK)) {
            setTheme(R.style.aboutDark);
        } else {
            setTheme(R.style.aboutLight);
        }

        setContentView(R.layout.activity_about);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        mTitleTextView = (TextView) findViewById(R.id.text_view_title);
        mVersion = (RelativeLayout) findViewById(R.id.relative_layout_version);

        mAuthorsDivider = findViewById(R.id.view_divider_authors);
        mRate = (RelativeLayout) findViewById(R.id.relative_layout_rate);
        mLicensesIcon = (ImageView) findViewById(R.id.image_view_license);

        mVersion.setOnClickListener(this);
        mXda.setOnClickListener(this);
        mRate.setOnClickListener(this);

        mAppBarLayout.setLayoutParams(calculateHeaderViewParams());

        mToolbar = (Toolbar)findViewById(R.id.toolBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.md_nav_back));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        switchIcons();

        // license icon easter
        Random random = new Random();
        if (random.nextInt(2) == 0) {
            mLicensesIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_apple_ios_grey600_24dp));
        }

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.about_header);

        // It will generate colors based on the image in an AsyncTask.
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {

                int mutedColor = palette.getMutedColor(getResources().getColor(R.color.primary_blue));
                int darkMutedColor = palette.getDarkMutedColor(getResources().getColor(R.color.primary_blue));
                mCollapsingToolbarLayout.setContentScrimColor(mutedColor);
                mCollapsingToolbarLayout.setStatusBarScrimColor(darkMutedColor);
            }
        });

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                mTitleTextView.setAlpha(Math.abs(verticalOffset / (float)
                        appBarLayout.getTotalScrollRange()));
            }
        });
    }

    /**
     * Calculates aspect ratio for the Easy header
     * @return the layout params with new set of width and height attribute
     */
    private CoordinatorLayout.LayoutParams calculateHeaderViewParams() {

        // calculating cardview height as per the youtube video thumb aspect ratio
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        float vidAspectRatio = (float) HEADER_WIDTH / (float) HEADER_HEIGHT;
        Log.d(getClass().getSimpleName(), Float.valueOf(vidAspectRatio) + "");
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float reqHeightAsPerAspectRatio = Float.valueOf(screenWidth)*vidAspectRatio;
        Log.d(getClass().getSimpleName(), reqHeightAsPerAspectRatio + "");


        Log.d(getClass().getSimpleName(), "new width: " + screenWidth + " and height: " + reqHeightAsPerAspectRatio);

        layoutParams.width = screenWidth;
        layoutParams.height = (int) reqHeightAsPerAspectRatio;
        return layoutParams;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method switches icon resources as per current theme
     */
    private void switchIcons() {
        if (getAppTheme().equals(AppTheme.DARK)) {
            // dark theme
            mAuthorsDivider.setBackgroundColor(getResources().getColor(R.color.divider_dark));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.relative_layout_version:
                mCount++;
                if (mCount >= 5) {
                    if (mToast!=null)
                        mToast.cancel();
                    mToast = Toast.makeText(this, getResources().getString(R.string.easter_egg_title) +
                                    " : " + mCount, Toast.LENGTH_SHORT);
                    mToast.show();

                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, Integer.parseInt(Integer.toString(mCount) + "000")).apply();
                } else {
                    mSharedPref.edit().putInt(KEY_PREF_STUDIO, 0).apply();
                }
                break;



            case R.id.relative_layout_licenses:
                Dialog dialog = new Dialog(this, android.R.style.Theme_Holo_Light);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                final View dialog_view = getLayoutInflater().inflate(R.layout.open_source_licenses, null);
                WebView wv = (WebView) dialog_view.findViewById(R.id.webView1);
                dialog.setContentView(dialog_view);
                wv.loadData(PreferenceUtils.LICENCE_TERMS, "text/html", null);
                dialog.show();
                break;

            case R.id.relative_layout_rate:
                Intent rateIntent = new Intent(Intent.ACTION_VIEW);
                rateIntent.setData(Uri.parse(URL_REPO_RATE));
                startActivity(rateIntent);
                break;
        }
    }
}
