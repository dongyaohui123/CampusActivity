package com.campus.activity.view.v1;

/**
 * 组织者亲和度查询结果行。
 */
public class OrganizerAffinityRow {

    private Long organizerId;
    private Double score;

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
