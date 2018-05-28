package com.willchou.dapenti.model;

import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DaPenTi {
    static private final String url = "http://www.dapenti.com/blog/index.asp";
    static private final String TAG = "DaPenTi";

    static public String storageDir;
    static public List<DaPenTiCategory> daPenTiCategories;

    public interface onCategoryPrepared {
        void onCategoryPrepared();
    }

    static public onCategoryPrepared categoryPrepared;

    static public void prepareCategory() {
        Log.d(TAG, "prepareCategory");

        boolean fromDatabase = true;
        List<Pair<String, URL>> urlPairs = new ArrayList<>();
        Database database = Database.getDatabase();
        if (database != null)
            database.getCategories(urlPairs);

        if (urlPairs.isEmpty()) {
            Log.d(TAG, "Unable to get category from database, try web page");
            String ss = "div.center_title > a, div.title > a, div.title > p > a";
            urlPairs = getElementsWithQuery(url, ss);
            fromDatabase = false;
        }

        if (urlPairs.isEmpty())
            return;

        daPenTiCategories = new ArrayList<>();
        for (Pair<String, URL> p : urlPairs) {
            daPenTiCategories.add(new DaPenTiCategory(p));

            if (!fromDatabase && database != null)
                database.addCategory(p.first, p.second.toString());
        }

        if (categoryPrepared != null)
            categoryPrepared.onCategoryPrepared();
    }

    private static List<Pair<String, URL>> getElementsWithQuery(String url, String query) {
        List<Pair<String, URL>> lp = new ArrayList<>();
        try {
            URL u = new URL(url);
            lp = getElementsWithQuery(u, query);
        } catch (Exception e) { e.printStackTrace(); }

        return lp;
    }

    static public List<Pair<String, URL>> getElementsWithQuery(URL url, String query) {
        List<Pair<String, URL>> lp = new ArrayList<>();

        String us = url.toString();
        String prefix = us.substring(0, us.lastIndexOf("/"));

        try {
            Document doc = Jsoup.parse(url, 5000);
            Elements titles = doc.select(query);
            Log.d(TAG, "titles: " + titles);
            for (Element e : titles) {
                String t = e.text().replace(" ", "");
                String u = e.attr("href");

                if (t.isEmpty() || u.isEmpty())
                    continue;

                if (!u.contains(url.getProtocol()))
                    u = prefix + "/" + u;

                Log.d(TAG, "title: " + t + ", url: " + u);
                lp.add(new Pair<>(t, new URL(u)));
            }
        } catch (Exception e) { e.printStackTrace(); }

        return lp;
    }
}
