const { listPendingReviews, approveReview, rejectReview } = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "管理员审核（简版）",
      roleHintPrefix: "当前角色为",
      roleHintSuffix: "，请在首页切换为管理员。",
      reviewCommentTitle: "审核意见",
      reviewCommentPlaceholder: "通过可空，驳回必填",
      loading: "加载中...",
      activityIdLabel: "活动编号",
      organizerLabel: "组织者",
      timeLabel: "时间",
      reviewStatusLabel: "审核状态",
      approve: "通过",
      reject: "驳回",
      empty: "暂无待审核活动",
    },
    operatorRole: "STUDENT",
    loading: false,
    reviewComment: "请补充活动说明后再提交",
    pendingList: [],
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ADMIN") this.loadPending();
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  onCommentInput(event) {
    this.setData({ reviewComment: event.detail });
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
      feedback.success("已通过");
      await this.loadPending();
    } catch (e) {}
  },

  /**
   * 驳回必须填写意见，避免服务端校验失败后重复交互。
   */
  async onRejectTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const comment = String(this.data.reviewComment || "").trim();
    if (!comment) return feedback.error("驳回意见不能为空");
    try {
      await rejectReview(activityId, comment);
      feedback.success("已驳回");
      await this.loadPending();
    } catch (e) {}
  },
});
