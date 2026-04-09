const { listPendingReviews, approveReview, rejectReview } = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

function displayTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ");
}

Page({
  data: {
    operatorRole: "STUDENT",
    loading: false,
    reviewComment: "请补充活动说明后再提交",
    pendingList: [],
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ADMIN") {
      this.loadPending();
    }
  },

  onCommentInput(event) {
    this.setData({ reviewComment: event.detail.value });
  },

  async loadPending() {
    this.setData({ loading: true });
    try {
      const list = await listPendingReviews();
      const pendingList = (list || []).map((item) => ({
        ...item,
        startDisplay: displayTime(item.startTime),
        endDisplay: displayTime(item.endTime),
      }));
      this.setData({ pendingList });
    } catch (e) {
      this.setData({ pendingList: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onApproveTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    try {
      await approveReview(activityId, this.data.reviewComment);
      wx.showToast({ title: "已通过", icon: "success" });
      await this.loadPending();
    } catch (e) {
      // request.js 已统一提示
    }
  },

  async onRejectTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const comment = String(this.data.reviewComment || "").trim();
    if (!comment) {
      wx.showToast({ title: "驳回意见不能为空", icon: "none" });
      return;
    }
    try {
      await rejectReview(activityId, comment);
      wx.showToast({ title: "已驳回", icon: "success" });
      await this.loadPending();
    } catch (e) {
      // request.js 已统一提示
    }
  },
});
