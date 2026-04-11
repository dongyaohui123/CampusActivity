const { listPendingReviews, approveReview, rejectReview } = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "\u7ba1\u7406\u5458\u5ba1\u6838\uff08\u7b80\u9875\uff09",
      roleHintPrefix: "\u5f53\u524d\u89d2\u8272\u4e3a",
      roleHintSuffix: "\u8bf7\u5728\u9996\u9875\u5207\u6362\u4e3a ADMIN\u3002",
      reviewCommentTitle: "\u5ba1\u6838\u610f\u89c1",
      reviewCommentPlaceholder: "\u901a\u8fc7\u53ef\u7a7a\uff0c\u9a73\u56de\u5fc5\u586b",
      loading: "\u52a0\u8f7d\u4e2d...",
      activityIdLabel: "\u6d3b\u52a8ID",
      organizerLabel: "\u7ec4\u7ec7\u8005",
      timeLabel: "\u65f6\u95f4",
      reviewStatusLabel: "\u5ba1\u6838\u72b6\u6001",
      approve: "\u901a\u8fc7",
      reject: "\u9a73\u56de",
      empty: "\u6682\u65e0\u5f85\u5ba1\u6838\u6d3b\u52a8",
    },
    operatorRole: "STUDENT",
    loading: false,
    reviewComment: "\u8bf7\u8865\u5145\u6d3b\u52a8\u8bf4\u660e\u540e\u518d\u63d0\u4ea4",
    pendingList: [],
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ADMIN") this.loadPending();
  },

  onCommentInput(event) {
    this.setData({ reviewComment: event.detail.value });
  },

  async loadPending() {
    this.setData({ loading: true });
    try {
      const list = await listPendingReviews();
      this.setData({ pendingList: (list || []).map((item) => ({ ...item, startDisplay: displayTime(item.startTime), endDisplay: displayTime(item.endTime) })) });
    } catch (e) {
      this.setData({ pendingList: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onApproveTap(event) {
    try {
      await approveReview(Number(event.currentTarget.dataset.id), this.data.reviewComment);
      wx.showToast({ title: "\u5df2\u901a\u8fc7", icon: "success" });
      await this.loadPending();
    } catch (e) {}
  },

  async onRejectTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const comment = String(this.data.reviewComment || "").trim();
    if (!comment) return wx.showToast({ title: "\u9a73\u56de\u610f\u89c1\u4e0d\u80fd\u4e3a\u7a7a", icon: "none" });
    try {
      await rejectReview(activityId, comment);
      wx.showToast({ title: "\u5df2\u9a73\u56de", icon: "success" });
      await this.loadPending();
    } catch (e) {}
  },
});