package com.willchou.dapenti.model;

import android.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DaPenTiCategory {
    static private final String TAG = "DaPenTiCategory";

    private String categoryName;
    private URL categoryUrl;

    public List<DaPenTiPage> pages = new ArrayList<>();

    public interface onCategoryPrepared {
        void onCategoryPrepared(int index);
    }

    public onCategoryPrepared categoryPrepared = null;

    DaPenTiCategory(Pair<String, URL> p) {
        categoryName = p.first;
        categoryUrl = p.second;
    }

    public boolean initiated() { return !pages.isEmpty(); }
    public String getCategoryName() { return categoryName; }

    public void preparePages() {
        //String ss = "div > ul > li > a";
        String ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']";
        List<Pair<String, URL>> subItemPair = DaPenTi.getElementsWithQuery(categoryUrl, ss);

        for (Pair<String, URL> p : subItemPair) {
            pages.add(new DaPenTiPage((p)));
        }

        if (categoryPrepared != null)
            categoryPrepared.onCategoryPrepared(pages.size() - 1);
    }
}
