package com.willchou.dapenti;

import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;
import com.willchou.dapenti.model.DaPenTiPage;

public class RecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    static private final String TAG = "RecyclerViewAdapter";
    private int daPenTiCategoryIndex;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        holder.update(c.pages.get(position));
        enterAnimation(holder.itemView);
    }

    @Override
    public int getItemCount() {
        DaPenTiCategory c = DaPenTi.daPenTiCategories.get(daPenTiCategoryIndex);
        return c.pages.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.attachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.detachedFromWindow();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.penti_list_item, parent, false);
        return new ViewHolder(v);
    }

    RecyclerViewAdapter(int daPenTiCategoryIndex) {
        this.daPenTiCategoryIndex = daPenTiCategoryIndex;
    }

    private void enterAnimation(View view) {
        // TODO: find Y
        view.setTranslationY(1920);
        view.animate()
                .translationY(0)
                .setInterpolator(new DecelerateInterpolator(3.f))
                .setDuration(400)
                .start();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView titleTextView, descriptionTextView;
        final ImageView imageView;
        final LinearLayout videoLayout;

        static private final String PageProperty_Expanded = "pp_expand";
        static private final String PageProperty_Webview = "pp_webview";

        DaPenTiPage page;
        DWebView dWebView;

        ViewHolder(View v) {
            super(v);
            mView = v;

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

            mView.setOnClickListener((View v) -> {
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
            imageView.setVisibility(View.GONE);
            videoLayout.setVisibility(View.GONE);

            if (dWebView != null)
                dWebView.pauseVideo();
        }

        void showContent(View v, Boolean playVideo) {
            switch (page.getPageType()) {
                case DaPenTiPage.PageTypeVideo:
                    if (dWebView == null) {
                        DaPenTiPage.PageVideo pageVideo = page.pageVideo;
                        dWebView = new DWebView(v.getContext());
                        page.setObjectProperty(PageProperty_Webview, dWebView);

                        dWebView.playOnLoadFinished = playVideo;
                        dWebView.loadDataWithBaseURL("", pageVideo.contentHtml,
                                "text/html", "UTF-8", null);
                    } else if (playVideo)
                        dWebView.startVideo();

                    detachWebView();
                    videoLayout.addView(dWebView);
                    videoLayout.setVisibility(View.VISIBLE);
                    break;

                case DaPenTiPage.PageTypeLongReading:
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
}
