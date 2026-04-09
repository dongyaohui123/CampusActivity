package com.campus.activity.common;

import java.util.List;

/**
 * Standard page payload for list APIs.
 */
public class PageResponse<T> {
    private long page;
    private long size;
    private long total;
    private long pages;
    private List<T> records;

    public PageResponse() {
    }

    public PageResponse(long page, long size, long total, long pages, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.pages = pages;
        this.records = records;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
