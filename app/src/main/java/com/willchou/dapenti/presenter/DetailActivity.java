package com.willchou.dapenti.presenter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.hannesdorfmann.swipeback.Position;
import com.hannesdorfmann.swipeback.SwipeBack;
import com.willchou.dapenti.R;
import com.willchou.dapenti.model.Settings;
import com.willchou.dapenti.view.EnhancedWebView;

public class DetailActivity extends AppCompatActivity {
    private final String TAG = "DetailActivity";
    static public final String EXTRA_HTML = "extra_string_html";
    static public final String EXTRA_COVERURL = "extra_string_cover";
    static public final String EXTRA_TITLE = "extra_string_title";

    //private DaPenTi.DaPenTiContent daPenTiContent;
    private EnhancedWebView webView;
    private ImageView coverImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        SwipeBack.attach(this, Position.LEFT)
                .setContentView(R.layout.activity_detail)
                .setSwipeBackView(R.layout.swipeback_default);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener((View v) -> onBackPressed());

        coverImageView = findViewById(R.id.coverImage);
        webView = findViewById(R.id.webview);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        prepareContent();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
        overridePendingTransition(R.anim.swipeback_stack_to_front,
                R.anim.swipeback_stack_right_out);
    }

    private void applyUserSettings() {
        WebSettings webSettings = webView.getSettings();

        Settings settings = Settings.getSettings();
        if (settings == null)
            return;

        switch (settings.getFontSize()) {
            case Settings.FontSizeSmall:
                webSettings.setDefaultFontSize(15);
                break;
            case Settings.FontSizeMedia:
                webSettings.setDefaultFontSize(17);
                break;
            case Settings.FontSizeBig:
                webSettings.setDefaultFontSize(19);
                break;
            case Settings.FontSizeSuperBig:
                webSettings.setDefaultFontSize(21);
                break;
        }
    }

    private void prepareContent() {
        Intent intent = getIntent();
        String htmlString = intent.getStringExtra(EXTRA_HTML);
        String titleString = intent.getStringExtra(EXTRA_TITLE);
        String coverString = intent.getStringExtra(EXTRA_COVERURL);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(titleString);

        applyUserSettings();

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.cat)
                .error(R.drawable.cat)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH);

        Settings settings = Settings.getSettings();
        if (settings != null && settings.isImageEnabled()) {
            Glide.with(this)
                    .load(coverString)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(coverImageView);
        }

        webView.loadDataWithBaseURL(null, htmlString,
                "text/html", "UTF-8", null);
    }
}
