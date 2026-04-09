const {
  listOrganizerActivities,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

const VISIBILITY_OPTIONS = ["PUBLIC", "PRIVATE"];

function displayTime(value) {
  if (!value) {
    return "";
  }
  return String(value).replace("T", " ");
}

function normalizeDateTimeInput(input) {
  const raw = String(input || "").trim();
  if (!raw) {
    return "";
  }
  const withT = raw.replace(" ", "T");
  return withT.length === 16 ? `${withT}:00` : withT;
}

function buildFormFromActivity(activity) {
  return {
    title: activity.title || "",
    summary: activity.summary || "",
    location: activity.location || "",
    startTime: normalizeDateTimeInput(activity.startTime),
    endTime: normalizeDateTimeInput(activity.endTime),
    registrationDeadline: normalizeDateTimeInput(activity.registrationDeadline),
    maxParticipants: String(activity.maxParticipants || 0),
    visibilityIndex: Math.max(0, VISIBILITY_OPTIONS.indexOf(activity.visibility || "PUBLIC")),
    featured: Boolean(activity.featured),
  };
}

function emptyForm() {
  return {
    title: "",
    summary: "",
    location: "",
    startTime: "",
    endTime: "",
    registrationDeadline: "",
    maxParticipants: "100",
    visibilityIndex: 0,
    featured: false,
  };
}

Page({
  data: {
    operatorRole: "STUDENT",
    loading: false,
    activities: [],
    editingActivityId: null,
    reviewComment: "submit from miniapp",
    form: emptyForm(),
    visibilityOptions: VISIBILITY_OPTIONS,
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ORGANIZER") {
      this.loadActivities();
    }
  },

  async loadActivities() {
    this.setData({ loading: true });
    try {
      const list = await listOrganizerActivities({});
      const activities = (list || []).map((item) => ({
        ...item,
        startDisplay: displayTime(item.startTime),
        endDisplay: displayTime(item.endTime),
      }));
      this.setData({ activities });
    } catch (e) {
      this.setData({ activities: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  onFormInput(event) {
    const field = event.currentTarget.dataset.field;
    const value = event.detail.value;
    this.setData({ [`form.${field}`]: value });
  },

  onVisibilityChange(event) {
    this.setData({ "form.visibilityIndex": Number(event.detail.value) });
  },

  onFeaturedChange(event) {
    this.setData({ "form.featured": Boolean(event.detail.value) });
  },

  onReviewCommentInput(event) {
    this.setData({ reviewComment: event.detail.value });
  },

  resetForm() {
    this.setData({ editingActivityId: null, form: emptyForm() });
  },

  onEditTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const target = this.data.activities.find((item) => Number(item.id) === activityId);
    if (!target) {
      return;
    }
    this.setData({
      editingActivityId: activityId,
      form: buildFormFromActivity(target),
    });
  },

  buildPayload() {
    const form = this.data.form;
    const payload = {
      title: String(form.title || "").trim(),
      summary: String(form.summary || "").trim(),
      location: String(form.location || "").trim(),
      startTime: normalizeDateTimeInput(form.startTime),
      endTime: normalizeDateTimeInput(form.endTime),
      maxParticipants: Number(form.maxParticipants || 0),
      visibility: this.data.visibilityOptions[form.visibilityIndex] || "PUBLIC",
      featured: Boolean(form.featured),
    };
    const deadline = normalizeDateTimeInput(form.registrationDeadline);
    if (deadline) {
      payload.registrationDeadline = deadline;
    }
    return payload;
  },

  async onSubmitTap() {
    if (this.data.operatorRole !== "ORGANIZER") {
      wx.showToast({ title: "请先切换为 ORGANIZER", icon: "none" });
      return;
    }

    const payload = this.buildPayload();
    if (!payload.title || !payload.location || !payload.startTime || !payload.endTime) {
      wx.showToast({ title: "标题、地点、开始和结束时间必填", icon: "none" });
      return;
    }
    if (!Number.isFinite(payload.maxParticipants) || payload.maxParticipants < 0) {
      wx.showToast({ title: "人数必须为非负数", icon: "none" });
      return;
    }

    try {
      if (this.data.editingActivityId) {
        await updateOrganizerActivity(this.data.editingActivityId, payload);
        wx.showToast({ title: "活动已更新", icon: "success" });
      } else {
        await createOrganizerActivity(payload);
        wx.showToast({ title: "活动已创建", icon: "success" });
      }
      this.resetForm();
      await this.loadActivities();
    } catch (e) {
      // request.js 已统一提示
    }
  },

  async onSubmitReviewTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    try {
      await submitActivityReview(activityId, this.data.reviewComment);
      wx.showToast({ title: "提审成功", icon: "success" });
      await this.loadActivities();
    } catch (e) {
      // request.js 已统一提示
    }
  },
});
