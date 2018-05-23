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

public class DaPenTiPage {
    static private final String TAG = "DaPenTiPage";

    static public final int PageTypeUnknown = 0;
    static public final int PageTypeLongReading = 1;
    static public final int PageTypeNote = 2;
    static public final int PageTypePicture = 3;
    static public final int PageTypeVideo = 4;
    static public final int PageTypeOriginal = 5;

    private String pageTitle;
    URL pageUrl;
    private int pageType = PageTypeUnknown;
    String orignalHtml;

    public interface onContentPrepared {
        void onContentPrepared();
    }

    public onContentPrepared contentPrepared = null;

    public String getPageTitle() { return pageTitle; }
    public int getPageType() { return pageType; }

    static public class PageLongReading {
        public String contentHtml;
        public String coverImageUrl;
    }

    static public class PageNotes {
        public String contentHtml;
    }

    static public class PagePicture {
        public String description;
        public String imageUrl;
    }

    static public class PageVideo {
        public String description;
        public String contentHtml;
    }

    public PageLongReading pageLongReading;
    public PageNotes pageNotes;
    public PagePicture pagePicture;
    public PageVideo pageVideo;

    Element contentElement = null;
    String coverImageUrl = null;

    DaPenTiPage(Pair<String, URL> p) {
        this.pageTitle = p.first;
        this.pageUrl = p.second;
    }

    public boolean initiated() { return pageType != PageTypeUnknown; }

    private Element getFirstElement(Document doc, String s) {
        Elements es = doc.select(s);
        if (es.size() == 0)
            return null;

        Element e = es.first();
        fixContent(e);
        return e;
    }

    private boolean preparePageVideo(Document doc) {
        Element e = getFirstElement(doc, "video");
        if (e == null)
            return false;

        pageVideo = new PageVideo();
        pageVideo.contentHtml = getContent(e);
        // TODO: get description

        pageType = PageTypeVideo;
        return true;
    }

    private boolean preparePagePicture(Document doc) {
        // TODO: finish me
        return false;
    }

    private boolean preparePageNote(Document doc) {
        // TODO: finish me
        return false;
    }

    private boolean prepareLongReading(Document doc) {
        Element e = getFirstElement(doc, "div.oblog_text");
        if (e == null)
            return false;

        pageLongReading = new PageLongReading();
        pageLongReading.contentHtml = getContent(e);
        pageLongReading.coverImageUrl = getCoverImageUrl(e);

        pageType = PageTypeLongReading;
        return true;
    }

    public void prepareContent() {
        if (doPrepareContent() && contentPrepared != null)
            contentPrepared.onContentPrepared();
    }

    private boolean doPrepareContent() {
        Log.d(TAG, "prepareContent with url: " + pageUrl.toString());
        try {
            Document doc = Jsoup.parse(pageUrl, 5000);

            if (prepareLongReading(doc))
                return true;
            if (preparePageVideo(doc))
                return true;
            if (preparePagePicture(doc))
                return true;
            if (preparePageNote(doc))
                return true;

            pageType = PageTypeOriginal;
            orignalHtml = doc.toString();

            return true;

            /*
            Elements es = new Elements();
            String[] sl = {"div.oblog_text", "div[class^=__reader_view_article_wrap]",
                    "video", "table"};

            for (String s : sl) {
                es = doc.select(s);
                if (es.size() == 0) {
                    Log.d(TAG, "selector: " + s + " gets null");
                    continue;
                }

                if (s.equals("video"))
                    es = es.parents();
                break;
            }

            if (es.size() == 0)
                contentElement = doc.select("body").get(0);
            else
                contentElement = es.first();

            if (contentElement != null) {
                es = contentElement.select("img:not(.W_img_face)");
                if (es.size() >= 1)
                    coverImageUrl = es.first().attr("src");

                fixContent();
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getCoverImageUrl(Element contentElement) {
        Elements es = contentElement.select("img:not(.W_img_face)");
        // TODO: check image url valid
        if (es.size() >= 1)
            return es.first().attr("src");

        return "";
    }

    private void fixContent(Element contentElement) {
        contentElement.select("script, ins, hr, iframe").remove();

        List<Element> les = new ArrayList<>();
        for (Element e : contentElement.select("p, div")) {
            String is = e.html().replace("\\s+", "");

            if (is.equals("&nbsp;") || is.contains("友情提示")
                    || is.contains("新浪微博") || is.contains("来源：")
                    || is.contains("喷嚏网：")) {
                les.add(e);
                continue;
            }

            for (Element eae : e.select("a")) {
                String href = eae.attr("href");
                if (href.contains("taobao") || href.contains("5463797858")) {
                    les.add(e);
                    break;
                }
            }
        }
        for (Element e : les)
            e.remove();
    }

    public String getContent(Element contentElement) {
        String html, innerHTML = contentElement.toString();
        innerHTML = innerHTML.replace("广告", "");
        html = "<html><head>" +
                "<meta name=\"content-type\" content=\"text/html; charset=utf-8\">";
        html += "<style>" +
                "img:not(.W_img_face) {display: block; margin: 0 auto;max-width: 100%;}" +
                "video { width:100% !important; height:auto !important; }" +
                "table { width: 100% !important; }" +
                "</style>";
        html += "<script type=\"text/javascript\">\n" +
                "  document.addEventListener(\"DOMContentLoaded\", function(event) {\n" +
                "    document.querySelectorAll('img').forEach(function(img){\n" +
                "      img.onerror = function(){this.style.display='none';};\n" +
                "    })\n" +
                "  });\n" +
                "</script>";
        html += "</head><body>" + innerHTML + "</body></html>";
        Log.d(TAG, "html content: \n" + html);
        return html;
    }
}
