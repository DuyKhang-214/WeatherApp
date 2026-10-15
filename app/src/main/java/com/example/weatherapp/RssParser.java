package com.example.weatherapp;

import android.util.Xml;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;

/** Minimal RSS 2.0 / Atom parser built on XmlPullParser. */
public class RssParser {
    public static List<RssItem> parse(InputStream in) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        p.setInput(in, null);

        List<RssItem> out = new ArrayList<>();
        RssItem cur = null;
        String tag = null;
        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT && out.size() < 30) {
            if (ev == XmlPullParser.START_TAG) {
                tag = p.getName();
                if ("item".equals(tag) || "entry".equals(tag)) {
                    cur = new RssItem();
                } else if (cur != null && "link".equals(tag)) {
                    String href = p.getAttributeValue(null, "href");
                    if (href != null) cur.link = href;
                }
            } else if (ev == XmlPullParser.TEXT) {
                if (cur != null && tag != null && p.getText() != null) {
                    String s = p.getText().trim();
                    if (!s.isEmpty()) {
                        switch (tag) {
                            case "title": cur.title = cur.title == null ? s : cur.title + s; break;
                            case "link": if (cur.link == null) cur.link = s; break;
                            case "description":
                            case "summary": cur.description = cur.description == null ? s : cur.description + s; break;
                            case "pubDate":
                            case "updated": cur.pubDate = s; break;
                            default: break;
                        }
                    }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                String n = p.getName();
                if (("item".equals(n) || "entry".equals(n)) && cur != null) {
                    out.add(cur);
                    cur = null;
                }
                tag = null;
            }
            ev = p.next();
        }
        return out;
    }
}
