const { listPendingReviews, approveReview, rejectReview } = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

const ROLE_TEXT_MAP = {
  STUDENT: "学生",
  ORGANIZER: "组织者",
  ADMIN: "管理员",
};

const REVIEW_STATUS_TEXT_MAP = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已驳回",
};

const REVIEW_STATUS_TONE_MAP = {
  PENDING: "status-warning",
  APPROVED: "status-success",
  REJECTED: "status-danger",
};

function displayTime(value) {
  if (!value) return "时间待定";
  return String(value).replace("T", " ");
}

function normalizeRoleText(role) {
  const normalized = String(role || "").toUpperCase();
  if (!normalized) return "未登录";
  return ROLE_TEXT_MAP[normalized] || "未知角色";
}

function normalizeReviewStatusText(status) {
  const normalized = String(status || "").toUpperCase();
  return REVIEW_STATUS_TEXT_MAP[normalized] || "状态待定";
}

function normalizeReviewStatusTone(status) {
  const normalized = String(status || "").toUpperCase();
  return REVIEW_STATUS_TONE_MAP[normalized] || "status-muted";
}

function mapPendingItem(item) {
  const reviewStatus = String(item.reviewStatus || "PENDING").toUpperCase();
  const activityId = Number(item.activityId || 0);
  const organizerName = String(item.organizerName || "").trim();

  return {
    ...item,
    title: String(item.title || "").trim() || "未命名活动",
    activityId,
    activityIdDisplay: activityId > 0 ? String(activityId) : "-",
    organizerNameDisplay: organizerName || "-",
    startDisplay: displayTime(item.startTime),
    endDisplay: displayTime(item.endTime),
    reviewStatusText: normalizeReviewStatusText(reviewStatus),
    reviewStatusTone: normalizeReviewStatusTone(reviewStatus),
  };
}

Page({
  data: {
    i18n: {
      navTitle: "管理员审核",
      pageTitle: "活动审核管理",
      pageSubtitle: "处理待审核活动并填写审核意见",
      roleBadge: "管理员",
      pendingCountPrefix: "待审核 ",
      noPermissionTitle: "暂无审核权限",
      roleHintPrefix: "当前身份为",
      roleHintSuffix: "，请先切换为管理员后再进行活动审核。",
      reviewCommentTitle: "审核意见",
      reviewCommentTip: "通过时可留空，驳回时必须填写审核意见。",
      reviewCommentPlaceholder: "通过可留空，驳回请填写审核意见",
      featuredLabel: "通过后推荐",
      pendingSectionTitle: "待审核活动",
      loading: "加载中...",
      activityIdLabel: "活动编号",
      organizerLabel: "组织者姓名",
      timeLabel: "活动时间",
      approve: "通过",
      reject: "驳回",
      empty: "暂无待审核活动",
    },
    operatorRole: "",
    operatorRoleText: "未登录",
    loading: false,
    reviewComment: "",
    featuredMap: {},
    pendingList: [],
    pendingCount: 0,
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    const operatorRoleText = normalizeRoleText(operatorRole);

    this.setData({
      operatorRole,
      operatorRoleText,
      pendingList: operatorRole === "ADMIN" ? this.data.pendingList : [],
      pendingCount: operatorRole === "ADMIN" ? this.data.pendingCount : 0,
      featuredMap: operatorRole === "ADMIN" ? this.data.featuredMap : {},
    });

    if (operatorRole === "ADMIN") {
      this.loadPending();
    }
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
      const pendingList = (list || []).map(mapPendingItem);
      const prevFeaturedMap = this.data.featuredMap || {};
      const featuredMap = {};
      pendingList.forEach((item) => {
        const key = String(item.activityId);
        featuredMap[key] = Boolean(prevFeaturedMap[key]);
      });
      this.setData({
        pendingList,
        pendingCount: pendingList.length,
        featuredMap,
      });
    } catch (e) {
      this.setData({
        pendingList: [],
        pendingCount: 0,
        featuredMap: {},
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  onFeaturedChange(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    if (!Number.isFinite(activityId) || activityId <= 0) return;
    this.setData({
      [`featuredMap.${activityId}`]: Boolean(event.detail),
    });
  },

  async onApproveTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    if (!Number.isFinite(activityId) || activityId <= 0) return;
    const featured = Boolean((this.data.featuredMap || {})[String(activityId)]);
    try {
      await approveReview(activityId, {
        comment: this.data.reviewComment,
        featured,
      });
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
