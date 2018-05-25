package com.willchou.dapenti.view;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.willchou.dapenti.R;
import com.willchou.dapenti.model.DaPenTiPage;
import com.willchou.dapenti.presenter.DetailActivity;

public class RecyclerViewHolder extends RecyclerView.ViewHolder {
    static private final String TAG = "RecyclerViewHolder";

    private final View mView;
    private final TextView titleTextView, descriptionTextView;
    private final ImageView imageView;
    private final LinearLayout videoLayout;

    static private final String PageProperty_Expanded = "pp_expand";
    static private final String PageProperty_Webview = "pp_webview";

    DaPenTiPage page;
    DWebView dWebView;

    private DWebView.onFullScreenTriggered fullScreenTriggered;
    private DWebView.FullScreenViewPair fullScreenViewPair;

    RecyclerViewHolder(View v,
                       DWebView.onFullScreenTriggered fullScreenTriggered,
                       DWebView.FullScreenViewPair fullScreenViewPair) {
        super(v);
        mView = v;

        this.fullScreenTriggered = fullScreenTriggered;
        this.fullScreenViewPair = fullScreenViewPair;

        titleTextView = v.findViewById(R.id.title);
        descriptionTextView = v.findViewById(R.id.description);
        imageView = v.findViewById(R.id.image);

        videoLayout = v.findViewById(R.id.webViewLayout);
    }

    void update(DaPenTiPage page) {
        this.page = page;
        this.dWebView = (DWebView) page.getObjectProperty(PageProperty_Webview);

        titleTextView.setText(page.getPageTitle());
        Log.d(TAG, "update(reuse view): " + page.getPageTitle());

        hideContent(mView);
        if (page.getProperty(PageProperty_Expanded) != null)
            showContent(mView, false);

        titleTextView.setOnClickListener((View v) -> {
            if (page.initiated())
                triggerContent(v);
            else {
                page.contentPrepared = () ->
                        new Handler(Looper.getMainLooper()).post(() -> triggerContent(v));
                new Thread(page::prepareContent).start();
            }
        });
    }

    void detachWebView() {
        DWebView d = (DWebView) page.getObjectProperty(PageProperty_Webview);
        if (d != null) {
            ViewGroup g = (ViewGroup) d.getParent();
            if (g != null)
                g.removeView(d);
        }
    }

    void setupFullScreenWebView() {
        dWebView.fullScreenTriggered = fullScreenTriggered;
        fullScreenViewPair.nonVideoLayout = (ViewGroup) dWebView.getParent();
        dWebView.prepareFullScreen(fullScreenViewPair);
    }

    void attachedToWindow() {
        Log.d(TAG, "attachToWindow: " + page.getPageTitle());
        if (page.getProperty(PageProperty_Expanded) != null)
            showContent(mView, false);
    }

    void detachedFromWindow() {
        Log.d(TAG, "detachedFromWindow: " + page.getPageTitle());
        hideContent(mView);
    }

    void hideContent(View v) {
        descriptionTextView.setVisibility(View.GONE);
        descriptionTextView.setText("");
        imageView.setVisibility(View.GONE);
        imageView.setImageDrawable(null);
        videoLayout.setVisibility(View.GONE);

        detachWebView();
        if (dWebView != null)
            dWebView.pauseVideo();
    }

    void showContent(View v, Boolean playVideo) {
        switch (page.getPageType()) {
            case DaPenTiPage.PageTypeNote:
                DaPenTiPage.PageNotes notes = page.pageNotes;

                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(notes.content);
                break;

            case DaPenTiPage.PageTypePicture:
                DaPenTiPage.PagePicture picture = page.pagePicture;

                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(picture.description);

                imageView.setImageDrawable(null);
                imageView.setVisibility(View.VISIBLE);
                Glide.with(v.getContext())
                        .load(picture.imageUrl)
                        .into(imageView);
                break;

            case DaPenTiPage.PageTypeVideo:
                if (dWebView == null) {
                    dWebView = new DWebView(v.getContext());

                    DaPenTiPage.PageVideo pageVideo = page.pageVideo;
                    page.setObjectProperty(PageProperty_Webview, dWebView);

                    dWebView.playOnLoadFinished = playVideo;
                    dWebView.loadDataWithBaseURL("", pageVideo.contentHtml,
                            "text/html", "UTF-8", null);
                } else if (playVideo)
                    dWebView.startVideo();

                detachWebView();
                videoLayout.addView(dWebView);
                videoLayout.setVisibility(View.VISIBLE);

                setupFullScreenWebView();
                break;

            case DaPenTiPage.PageTypeLongReading:
                // show content with next click
                page.remove(PageProperty_Expanded);
                DaPenTiPage.PageLongReading pageLongReading = page.pageLongReading;

                Context context = v.getContext();

                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_HTML, pageLongReading.contentHtml);
                intent.putExtra(DetailActivity.EXTRA_COVERURL, pageLongReading.coverImageUrl);
                intent.putExtra(DetailActivity.EXTRA_TITLE, page.getPageTitle());

                context.startActivity(intent);
                break;
        }
    }

    void triggerContent(View v) {
        Log.d(TAG, "update with pageType: " + page.getPageType());

        if (page.getProperty(PageProperty_Expanded) != null) {
            page.remove(PageProperty_Expanded);
            hideContent(v);
            return;
        }

        page.setProperty(PageProperty_Expanded, "yes");
        showContent(v, true);
    }
}
