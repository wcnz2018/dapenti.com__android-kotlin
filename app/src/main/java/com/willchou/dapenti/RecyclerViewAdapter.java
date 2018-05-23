package com.willchou.dapenti;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.willchou.dapenti.model.DaPenTi;
import com.willchou.dapenti.model.DaPenTiCategory;
import com.willchou.dapenti.model.DaPenTiPage;

import java.net.URL;

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
        String listTitle;
        URL listUrl;

        final View mView;
        final TextView titleTextView, descriptionTextView;
        final ImageView imageView;
        final DWebView videoWebView;

        DaPenTiPage page;

        ViewHolder(View v) {
            super(v);
            mView = v;

            titleTextView = v.findViewById(R.id.title);
            descriptionTextView = v.findViewById(R.id.description);
            imageView = v.findViewById(R.id.image);
            videoWebView = v.findViewById(R.id.webview);
        }

        void update(DaPenTiPage page) {
            this.page = page;

            descriptionTextView.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            videoWebView.setVisibility(View.GONE);

            titleTextView.setText(page.getPageTitle());

            mView.setOnClickListener((View v) -> {
                if (page.initiated())
                    showContent(v);
                else {
                    page.contentPrepared = () ->
                            new Handler(Looper.getMainLooper()).post(() -> showContent(v));
                    new Thread(page::prepareContent).start();
                }
            });
        }

        void showContent(View v) {
            switch (page.getPageType()) {
                case DaPenTiPage.PageTypeVideo:
                    videoWebView.setVisibility(View.VISIBLE);

                    DaPenTiPage.PageVideo pageVideo = page.pageVideo;
                    videoWebView.loadDataWithBaseURL("", pageVideo.contentHtml,
                            "text/html", "UTF-8", null);
                    break;

                case DaPenTiPage.PageTypeLongReading:
                    DaPenTiPage.PageLongReading pageLongReading = page.pageLongReading;

                    Context context = v.getContext();

                    Intent intent = new Intent(context, DetailActivity.class);
                    intent.putExtra(DetailActivity.EXTRA_HTML, pageLongReading.contentHtml);
                    intent.putExtra(DetailActivity.EXTRA_TITLE, page.getPageTitle());

                    context.startActivity(intent);
                    break;
            }
            Log.d(TAG, "update with pageType: " + page.getPageType());
        }
    }
}
