package com.campus.activity.view.v1;

/**
 * 分类亲和度查询结果行。
 */
public class CategoryAffinityRow {

    private Long categoryId;
    private Double score;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
