package com.willchou.dapenti.model;

import android.util.Log;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DaPenTiPage extends Properties {
    static private final String TAG = "DaPenTiPage";

    // Properties
    private Map<String, Object> mapObject = new HashMap<>();
    public void setObjectProperty(String k, Object o) { mapObject.put(k, o); }
    public Object getObjectProperty(String k) { return mapObject.get(k); }
    public void removeObjectProperty(String k) { mapObject.remove(k); }

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
        public String content;
    }

    static public class PagePicture {
        public String description;
        public String imageUrl;
    }

    static public class PageVideo {
        public String description;
        public String contentHtml;
    }

    public PageLongReading pageLongReading = new PageLongReading();
    public PageNotes pageNotes = new PageNotes();
    public PagePicture pagePicture = new PagePicture();
    public PageVideo pageVideo = new PageVideo();

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

        pageVideo.contentHtml = getContent(e);
        // TODO: get description

        pageType = PageTypeVideo;
        return true;
    }

    private boolean preparePagePicture(Document doc) {
        if (!pageUrl.toString().contains("more.asp?name=tupian"))
            return false;

        String[] ss = {"div>img[src^='http://www.dapenti.com']",
                "div>p>img[src^='http://www.dapenti.com']"};

        for (String s : ss) {
            Element e = getFirstElement(doc, s);
            if (e != null) {
                pagePicture.imageUrl = e.attr("src");

                if (s.contains(">p>"))
                    pagePicture.description = e.parent().parent().text();
                else
                    pagePicture.description = e.parent().text();

                pageType = PageTypePicture;
                return true;
            }
        }

        return false;
    }

    private boolean preparePageNote(Document doc) {
        String ss = "div.WB_info";
        Element e = getFirstElement(doc, ss);
        if (e != null) {
            pageNotes.content = e.text();
            pageType = PageTypeNote;
            return true;
        }

        Elements es = doc.select("div.oblog_text");
        Log.d(TAG, "es.size: " + es.size());
        if (es.size() != 1)
            return false;

        e = es.first();

        String sl[] = {"img", "video", "br"};
        for (String s : sl)
            if (e.select(s).size() > 0)
                return false;

        pageNotes.content = e.text();
        pageType = PageTypeNote;
        return true;
    }

    private boolean prepareLongReading(Document doc) {
        Element e = getFirstElement(doc, "div.oblog_text,span.oblog_text > div");
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

            // Note: order counts
            if (preparePageVideo(doc))
                return true;
            if (preparePageNote(doc))
                return true;
            if (preparePagePicture(doc))
                return true;
            if (prepareLongReading(doc))
                return true;

            pageType = PageTypeOriginal;
            orignalHtml = doc.toString();

            return true;
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
                ".video-rotate {\n" +
                "  position: absolute;\n" +
                "  transform: rotate(90deg);\n" +
                "\n" +
                "  transform-origin: bottom left;\n" +
                "  width: 100vh;\n" +
                "  height: 100vw;\n" +
                "  margin-top: -100vw;\n" +
                "  object-fit: cover;\n" +
                "\n" +
                "  z-index: 4;\n" +
                "  visibility: visible;\n" +
                "}" +
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
