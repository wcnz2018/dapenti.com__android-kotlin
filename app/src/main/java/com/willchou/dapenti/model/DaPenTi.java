package com.willchou.dapenti.model;

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
    static private final String urlString = "http://www.dapenti.com/blog/index.asp";
    static private final String TAG = "DaPenTi";

    static public String storageDir;
    static public List<DaPenTiCategory> daPenTiCategories;

    public interface onCategoryPrepared {
        void onCategoryPrepared();
    }

    static public onCategoryPrepared categoryPrepared;

    private static DaPenTi daPenTi = null;

    public DaPenTi() {
        daPenTi = this;
    }

    static public DaPenTi getDaPenTi() { return daPenTi; }

    private boolean fetchFromWeb() {
        String ss = "div.center_title > a, div.title > a, div.title > p > a";
        List<Pair<String, URL>> urlPairs = getElementsWithQuery(urlString, ss);
        if (urlPairs.isEmpty())
            return false;

        Database database = Database.getDatabase();
        daPenTiCategories = new ArrayList<>();
        for (Pair<String, URL> p : urlPairs) {
            // "浮世绘"和"本月热读" 中的子页连接是错误的目录内容
            if (p.first.equals("浮世绘") || p.first.equals("本月热读"))
                continue;

            daPenTiCategories.add(new DaPenTiCategory(p));
            if (database != null)
                database.addCategory(p.first, p.second.toString());
        }

        if (categoryPrepared != null)
            categoryPrepared.onCategoryPrepared();

        return true;
    }

    private boolean fetchFromDatabase() {
        Database database = Database.getDatabase();
        if (database == null)
            return false;

        List<Pair<String, URL>> urlPairs = new ArrayList<>();
        database.getCategories(urlPairs);
        if (urlPairs.isEmpty())
            return false;

        daPenTiCategories = new ArrayList<>();
        for (Pair<String, URL> p : urlPairs)
            daPenTiCategories.add(new DaPenTiCategory(p));

        if (categoryPrepared != null)
            categoryPrepared.onCategoryPrepared();
        return true;
    }

    public boolean prepareCategory(boolean fromWeb) {
        Log.d(TAG, "prepareCategory");

        if (!fromWeb && fetchFromDatabase()) {
            Log.d(TAG, "restore data from database");
            return true;
        }

        return fetchFromWeb();
    }

    static private List<Pair<String, URL>> getElementsWithQuery(String url, String query) {
        try {
            return getElementsWithQuery(new URL(url), query);
        } catch (Exception e) { e.printStackTrace(); }

        return new ArrayList<>();
    }

    static private List<Pair<String, URL>> getElementsWithQuery(URL url, String html, String query) {
        List<Pair<String, URL>> lp = new ArrayList<>();

        String us = url.toString();
        String prefix = us.substring(0, us.lastIndexOf("/"));

        try {
            Document doc = Jsoup.parse(html, url.toString());
            Elements titles = doc.select(query);
            Log.d(TAG, "titles: " + titles);
            for (Element e : titles) {
                String t = e.text().replace(" ", "");
                String u = e.attr("href");

                if (t.isEmpty() || u.isEmpty())
                    continue;

                if (!u.contains(url.getProtocol()))
                    u = prefix + "/" + u;

                Log.d(TAG, "title: " + t + ", urlString: " + u);
                lp.add(new Pair<>(t, new URL(u)));
            }
        } catch (Exception e) { e.printStackTrace(); }

        return lp;
    }

    static public List<Pair<String, URL>> getElementsWithQuery(URL url, String query) {
        try {
            Document doc = Jsoup.parse(url, 5000);
            return getElementsWithQuery(url, doc.toString(), query);
        } catch (Exception e) { e.printStackTrace(); }

        return new ArrayList<>();
    }
}
