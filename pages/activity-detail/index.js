const {
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "活动详情",
      loading: "加载中...",
      statusLabel: "状态",
      locationLabel: "地点",
      timeLabel: "活动时间",
      deadlineLabel: "报名截止",
      summaryLabel: "摘要",
      contentLabel: "详情",
      registerOps: "报名操作",
      studentOnly: "仅学生可报名/取消",
      currentStatus: "当前报名状态",
      registerNow: "立即报名",
      cancelRegister: "取消报名",
    },
    activityId: 0,
    loading: false,
    activity: null,
    operatorRole: "STUDENT",
    currentRegistration: null,
    canRegister: false,
    canCancel: false,
  },

  onLoad(options) {
    this.setData({ activityId: Number(options.activityId || 0) });
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  onShow() {
    this.loadAll();
  },

  async loadAll() {
    const { operatorRole } = getOperatorContext();
    this.setData({ loading: true, operatorRole });
    try {
      await this.loadDetail();
      if (operatorRole === "STUDENT") await this.loadRegistrationState();
      else this.setData({ currentRegistration: null, canRegister: false, canCancel: false });
    } catch (e) {
      this.setData({ activity: null, currentRegistration: null, canRegister: false, canCancel: false });
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadDetail() {
    const detail = await getPublicActivityDetail(this.data.activityId);
    this.setData({
      activity: {
        ...detail,
        startDisplay: displayTime(detail.startTime),
        endDisplay: displayTime(detail.endTime),
        deadlineDisplay: displayTime(detail.registrationDeadline),
      },
    });
  },

  async loadRegistrationState() {
    const rows = await getMyRegistrations();
    const currentRegistration = (rows || []).find((item) => Number(item.activityId) === this.data.activityId) || null;
    const canRegister = !currentRegistration || currentRegistration.registrationStatus === "CANCELLED";
    const canCancel = Boolean(currentRegistration && (currentRegistration.registrationStatus === "REGISTERED" || currentRegistration.registrationStatus === "CHECKED_IN"));
    this.setData({ currentRegistration, canRegister, canCancel });
  },

  async onRegisterTap() {
    try {
      await registerActivity(this.data.activityId, "小程序报名");
      feedback.success("报名成功");
      await this.loadRegistrationState();
    } catch (e) {}
  },

  async onCancelTap() {
    const registration = this.data.currentRegistration;
    if (!registration || !registration.registrationId) {
      feedback.error("未找到可取消记录");
      return;
    }
    try {
      await cancelRegistration(registration.registrationId, "miniapp cancel");
      feedback.success("已取消报名");
      await this.loadRegistrationState();
    } catch (e) {}
  },
});
