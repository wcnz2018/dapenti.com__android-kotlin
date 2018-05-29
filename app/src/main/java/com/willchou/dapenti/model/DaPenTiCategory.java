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

    public void setPages(List<Pair<String, URL>> pair, boolean fromDatabase) {
        pages.clear();
        Database database = Database.getDatabase();
        for (Pair<String, URL> p : pair) {
            pages.add(new DaPenTiPage((p)));

            if (!fromDatabase && database != null)
                database.addPage(categoryName, p.first, p.second.toString());
        }

        if (categoryPrepared != null)
            categoryPrepared.onCategoryPrepared(pages.size() - 1);
    }

    public void preparePages(boolean fromWeb) {
        List<Pair<String, URL>> subItemPair = new ArrayList<>();

        boolean fromDatabase = true;
        Database database = Database.getDatabase();
        if (!fromWeb && database != null)
            database.getPages(categoryName, subItemPair);

        if (subItemPair.isEmpty()) {
            //String ss = "div > ul > li > a";
            String ss = "li>a[href^='more.asp?name='],span>a[href^='more.asp?name=']";
            subItemPair = DaPenTi.getElementsWithQuery(categoryUrl, ss);

            fromDatabase = false;
        }

        setPages(subItemPair, fromDatabase);
    }
}
