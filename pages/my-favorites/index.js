const { getMyFavorites, unfavoriteActivity } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

const STATUS_TEXT_MAP = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  REGISTRATION_OPEN: "报名中",
  REGISTRATION_CLOSED: "报名截止",
  ONGOING: "进行中",
  FINISHED: "已结束",
  CANCELLED: "已取消",
};

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function mapStatusText(status) {
  if (!status) return "状态待定";
  return STATUS_TEXT_MAP[status] || status;
}

function mapStatusTone(status) {
  if (status === "ONGOING") return "success";
  if (status === "FINISHED" || status === "CANCELLED") return "muted";
  if (status === "REGISTRATION_CLOSED") return "danger";
  return "warning";
}

Page({
  data: {
    i18n: {
      navTitle: "我的收藏",
      loginHint: "请先登录后查看已收藏活动。",
      loading: "加载中...",
      empty: "还没有收藏活动，去详情页收藏感兴趣的活动吧",
      summaryEmpty: "暂无活动简介",
      locationLabel: "地点",
      timeLabel: "时间",
      favoriteTimeLabel: "收藏时间",
      viewDetail: "活动详情",
      unfavorite: "取消收藏",
      unfavoriteSuccess: "已取消收藏",
      unknownActivity: "未知活动",
      unknownLocation: "地点待定",
    },
    loading: false,
    loginUser: null,
    favorites: [],
  },

  onShow() {
    const loginUser = getLoginUser();
    this.setData({ loginUser });
    if (!loginUser) {
      this.setData({ favorites: [], loading: false });
      return;
    }
    this.loadFavorites();
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadFavorites() {
    const { i18n } = this.data;
    this.setData({ loading: true });
    try {
      const rows = await getMyFavorites();
      const favorites = (rows || []).map((item) => ({
        ...item,
        titleDisplay: item.title || i18n.unknownActivity,
        summaryDisplay: item.summary || i18n.summaryEmpty,
        locationDisplay: item.location || i18n.unknownLocation,
        startDisplay: displayTime(item.startTime),
        endDisplay: displayTime(item.endTime),
        favoritedAtDisplay: displayTime(item.favoritedAt),
        statusText: mapStatusText(item.status),
        statusTone: mapStatusTone(item.status),
      }));
      this.setData({ favorites });
    } catch (e) {
      this.setData({ favorites: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  onViewDetailTap(event) {
    const activityId = Number(event.currentTarget.dataset.id || 0);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({ url: `/pages/activity-detail/index?activityId=${activityId}` });
  },

  async onUnfavoriteTap(event) {
    const activityId = Number(event.currentTarget.dataset.id || 0);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    try {
      await unfavoriteActivity(activityId);
      feedback.success(this.data.i18n.unfavoriteSuccess);
      await this.loadFavorites();
    } catch (e) {}
  },
});
