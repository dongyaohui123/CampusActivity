const {
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

function displayTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ");
}

Page({
  data: {
    activityId: 0,
    loading: false,
    activity: null,
    operatorRole: "STUDENT",
    currentRegistration: null,
    canRegister: false,
    canCancel: false,
  },

  onLoad(options) {
    const activityId = Number(options.activityId || 0);
    this.setData({ activityId });
  },

  onShow() {
    this.loadAll();
  },

  async loadAll() {
    const { operatorRole } = getOperatorContext();
    this.setData({ loading: true, operatorRole });
    try {
      await this.loadDetail();
      if (operatorRole === "STUDENT") {
        await this.loadRegistrationState();
      } else {
        this.setData({
          currentRegistration: null,
          canRegister: false,
          canCancel: false,
        });
      }
    } catch (e) {
      this.setData({
        activity: null,
        currentRegistration: null,
        canRegister: false,
        canCancel: false,
      });
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
    const canCancel = Boolean(
      currentRegistration &&
        (currentRegistration.registrationStatus === "REGISTERED" ||
          currentRegistration.registrationStatus === "CHECKED_IN")
    );
    this.setData({
      currentRegistration,
      canRegister,
      canCancel,
    });
  },

  async onRegisterTap() {
    try {
      await registerActivity(this.data.activityId, "miniapp register");
      wx.showToast({ title: "报名成功", icon: "success" });
      await this.loadRegistrationState();
    } catch (e) {
      // request.js 已统一提示
    }
  },

  async onCancelTap() {
    const registration = this.data.currentRegistration;
    if (!registration || !registration.registrationId) {
      wx.showToast({ title: "未找到可取消记录", icon: "none" });
      return;
    }
    try {
      await cancelRegistration(registration.registrationId, "miniapp cancel");
      wx.showToast({ title: "已取消报名", icon: "success" });
      await this.loadRegistrationState();
    } catch (e) {
      // request.js 已统一提示
    }
  },
});
