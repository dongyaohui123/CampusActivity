const {
  listOrganizerActivities,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

const VISIBILITY_OPTIONS = ["PUBLIC", "PRIVATE"];

function displayTime(value) {
  if (!value) return "";
  return String(value).replace("T", " ");
}

function normalizeDateTimeInput(input) {
  const raw = String(input || "").trim();
  if (!raw) return "";
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
    i18n: {
      navTitle: "\u7ec4\u7ec7\u8005\u6d3b\u52a8\u7ba1\u7406",
      roleHintPrefix: "\u5f53\u524d\u89d2\u8272\u4e3a",
      roleHintSuffix: "\u8bf7\u5728\u9996\u9875\u5207\u6362\u4e3a ORGANIZER \u540e\u518d\u64cd\u4f5c\u3002",
      editActivity: "\u7f16\u8f91\u6d3b\u52a8",
      createActivity: "\u521b\u5efa\u6d3b\u52a8",
      titleLabel: "\u6807\u9898",
      summaryLabel: "\u6458\u8981",
      locationLabel: "\u5730\u70b9",
      startTimeLabel: "\u5f00\u59cb\u65f6\u95f4",
      endTimeLabel: "\u7ed3\u675f\u65f6\u95f4",
      deadlineLabel: "\u62a5\u540d\u622a\u6b62\u65f6\u95f4",
      maxParticipantsLabel: "\u4eba\u6570\u4e0a\u9650",
      visibilityLabel: "\u53ef\u89c1\u6027",
      featuredLabel: "\u63a8\u8350\u6d3b\u52a8\uff08featured\uff09",
      updateActivity: "\u66f4\u65b0\u6d3b\u52a8",
      reset: "\u91cd\u7f6e",
      submitNote: "\u63d0\u5ba1\u5907\u6ce8\uff08\u53ef\u9009\uff09",
      submitCommentPlaceholder: "\u63d0\u5ba1\u5907\u6ce8",
      loading: "\u52a0\u8f7d\u4e2d...",
      statusLabel: "\u72b6\u6001",
      reviewLabel: "\u5ba1\u6838",
      timeLabel: "\u65f6\u95f4",
      edit: "\u7f16\u8f91",
      submitReview: "\u63d0\u5ba1",
      empty: "\u6682\u65e0\u6d3b\u52a8",
      phTitle: "\u4f8b\u5982\uff1a\u6821\u56ed\u6b4c\u624b\u5927\u8d5b",
      phSummary: "\u4e00\u53e5\u8bdd\u6458\u8981",
      phLocation: "\u4f8b\u5982\uff1a\u5927\u5b66\u751f\u6d3b\u52a8\u4e2d\u5fc3",
      required: "*",
    },
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
    if (operatorRole === "ORGANIZER") this.loadActivities();
  },

  async loadActivities() {
    this.setData({ loading: true });
    try {
      const list = await listOrganizerActivities({});
      this.setData({ activities: (list || []).map((item) => ({ ...item, startDisplay: displayTime(item.startTime), endDisplay: displayTime(item.endTime) })) });
    } catch (e) {
      this.setData({ activities: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  onFormInput(event) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value });
  },
  onVisibilityChange(event) { this.setData({ "form.visibilityIndex": Number(event.detail.value) }); },
  onFeaturedChange(event) { this.setData({ "form.featured": Boolean(event.detail.value) }); },
  onReviewCommentInput(event) { this.setData({ reviewComment: event.detail.value }); },
  resetForm() { this.setData({ editingActivityId: null, form: emptyForm() }); },

  onEditTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const target = this.data.activities.find((item) => Number(item.id) === activityId);
    if (!target) return;
    this.setData({ editingActivityId: activityId, form: buildFormFromActivity(target) });
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
    if (deadline) payload.registrationDeadline = deadline;
    return payload;
  },

  async onSubmitTap() {
    if (this.data.operatorRole !== "ORGANIZER") return wx.showToast({ title: "\u8BF7\u5148\u5207\u6362\u4E3A ORGANIZER", icon: "none" });
    const payload = this.buildPayload();
    if (!payload.title || !payload.location || !payload.startTime || !payload.endTime) return wx.showToast({ title: "\u6807\u9898\u3001\u5730\u70B9\u3001\u5F00\u59CB\u548C\u7ED3\u675F\u65F6\u95F4\u5FC5\u586B", icon: "none" });
    if (!Number.isFinite(payload.maxParticipants) || payload.maxParticipants < 0) return wx.showToast({ title: "\u4EBA\u6570\u5FC5\u987B\u4E3A\u975E\u8D1F\u6570", icon: "none" });
    try {
      if (this.data.editingActivityId) {
        await updateOrganizerActivity(this.data.editingActivityId, payload);
        wx.showToast({ title: "\u6D3B\u52A8\u5DF2\u66F4\u65B0", icon: "success" });
      } else {
        await createOrganizerActivity(payload);
        wx.showToast({ title: "\u6D3B\u52A8\u5DF2\u521B\u5EFA", icon: "success" });
      }
      this.resetForm();
      await this.loadActivities();
    } catch (e) {}
  },

  async onSubmitReviewTap(event) {
    try {
      await submitActivityReview(Number(event.currentTarget.dataset.id), this.data.reviewComment);
      wx.showToast({ title: "\u63D0\u5BA1\u6210\u529F", icon: "success" });
      await this.loadActivities();
    } catch (e) {}
  },
});