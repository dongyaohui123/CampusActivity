const { listManageableActivities, submitActivityReview } = require("../../utils/api");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function mapStatusText(status) {
  if (status === "DRAFT") return "待完善";
  if (status === "PUBLISHED") return "已发布";
  if (status === "REGISTRATION_OPEN") return "报名中";
  if (status === "REGISTRATION_CLOSED") return "报名截止";
  if (status === "ONGOING") return "进行中";
  if (status === "FINISHED") return "已结束";
  if (status === "CANCELLED") return "已取消";
  return status || "状态未知";
}

Page({
  data: {
    i18n: {
      navTitle: "管理活动",
      loading: "加载中...",
      empty: "暂无可管理活动",
      roleOrganizer: "组织者",
      roleManager: "活动管理员",
      timeLabel: "时间",
      locationLabel: "地点",
      checkinAction: "扫码签到",
      registrationAction: "报名名单",
      editAction: "编辑",
      managerAction: "管理员",
      submitReviewAction: "提审",
    },
    loading: false,
    activities: [],
  },

  onShow() {
    this.loadManageableActivities();
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadManageableActivities() {
    this.setData({ loading: true });
    try {
      const rows = await listManageableActivities({});
      const activities = (rows || []).map((item) => ({
        ...item,
        statusText: mapStatusText(item.status),
        startDisplay: displayTime(item.startTime),
        endDisplay: displayTime(item.endTime),
        roleText: item.isOrganizer ? this.data.i18n.roleOrganizer : this.data.i18n.roleManager,
        canCheckIn: Boolean(item.canCheckIn),
        canViewRegistrations: Boolean(item.canViewRegistrations),
        isOrganizer: Boolean(item.isOrganizer),
      }));
      this.setData({ activities });
    } catch (e) {
      this.setData({ activities: [] });
      feedback.error("管理活动加载失败");
    } finally {
      this.setData({ loading: false });
    }
  },

  onCheckinTap(event) {
    const activityId = Number(event.currentTarget.dataset.activityId);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({ url: `/pages/organizer-checkin/index?activityId=${activityId}` });
  },

  onRegistrationTap(event) {
    const activityId = Number(event.currentTarget.dataset.activityId);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({ url: `/pages/managed-registrations/index?activityId=${activityId}` });
  },

  onEditTap(event) {
    const activityId = Number(event.currentTarget.dataset.activityId);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({ url: `/pages/organizer-activities/index?activityId=${activityId}` });
  },

  onManagerTap(event) {
    const activityId = Number(event.currentTarget.dataset.activityId);
    const title = encodeURIComponent(String(event.currentTarget.dataset.title || ""));
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({ url: `/pages/activity-managers/index?activityId=${activityId}&title=${title}` });
  },

  async onSubmitReviewTap(event) {
    const activityId = Number(event.currentTarget.dataset.activityId);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    try {
      await submitActivityReview(activityId, "");
      wx.showToast({
        title: "提审成功",
        icon: "success",
        duration: 2000,
      });
      await this.loadManageableActivities();
    } catch (e) {}
  },
});
