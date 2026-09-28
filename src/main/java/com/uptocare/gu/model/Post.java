package com.uptocare.gu.model;

import java.time.LocalDate;

/** 一篇文章：可以是「重要訊息」(NEWS) 或「經驗分享」(SHARE)，內文來自 Google 文件。 */
public class Post {

    public enum Type { NEWS, SHARE }

    private final int id;
    private final Type type;
    private final String title;
    private final String summary;
    private final String docUrl;    // Google 文件連結，可為 null（只顯示摘要）
    private final LocalDate date;
    private final boolean pinned;

    public Post(int id, Type type, String title, String summary, String docUrl,
                LocalDate date, boolean pinned) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.docUrl = docUrl;
        this.date = date;
        this.pinned = pinned;
    }

    public int getId() { return id; }
    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getDocUrl() { return docUrl; }
    public LocalDate getDate() { return date; }
    public String getDateText() { return date.toString(); }
    public boolean isPinned() { return pinned; }
}
