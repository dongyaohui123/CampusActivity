const {
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "\u6d3b\u52a8\u8be6\u60c5",
      loading: "\u52a0\u8f7d\u4e2d...",
      statusLabel: "\u72b6\u6001",
      locationLabel: "\u5730\u70b9",
      timeLabel: "\u6d3b\u52a8\u65f6\u95f4",
      deadlineLabel: "\u62a5\u540d\u622a\u6b62",
      summaryLabel: "\u6458\u8981",
      contentLabel: "\u8be6\u60c5",
      registerOps: "\u62a5\u540d\u64cd\u4f5c",
      studentOnly: "\u4ec5\u5b66\u751f\u53ef\u62a5\u540d/\u53d6\u6d88",
      currentStatus: "\u5f53\u524d\u62a5\u540d\u72b6\u6001",
      registerNow: "\u7acb\u5373\u62a5\u540d",
      cancelRegister: "\u53d6\u6d88\u62a5\u540d",
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
      await registerActivity(this.data.activityId, "miniapp register");
      wx.showToast({ title: "\u62a5\u540d\u6210\u529f", icon: "success" });
      await this.loadRegistrationState();
    } catch (e) {}
  },

  async onCancelTap() {
    const registration = this.data.currentRegistration;
    if (!registration || !registration.registrationId) {
      wx.showToast({ title: "\u672a\u627e\u5230\u53ef\u53d6\u6d88\u8bb0\u5f55", icon: "none" });
      return;
    }
    try {
      await cancelRegistration(registration.registrationId, "miniapp cancel");
      wx.showToast({ title: "\u5df2\u53d6\u6d88\u62a5\u540d", icon: "success" });
      await this.loadRegistrationState();
    } catch (e) {}
  },
});