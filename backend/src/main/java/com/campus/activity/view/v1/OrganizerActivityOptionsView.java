package com.campus.activity.view.v1;

import java.util.Collections;
import java.util.List;

/**
 * 组织者发布活动下拉选项。
 */
public class OrganizerActivityOptionsView {
    private List<String> campusTypes = Collections.emptyList();
    private List<OrganizerActivityTypeOptionView> activityTypes = Collections.emptyList();

    public List<String> getCampusTypes() {
        return campusTypes;
    }

    public void setCampusTypes(List<String> campusTypes) {
        this.campusTypes = campusTypes;
    }

    public List<OrganizerActivityTypeOptionView> getActivityTypes() {
        return activityTypes;
    }

    public void setActivityTypes(List<OrganizerActivityTypeOptionView> activityTypes) {
        this.activityTypes = activityTypes;
    }
}
