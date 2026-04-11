const { getMyRegistrations, cancelRegistration } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "\u6211\u7684\u6d3b\u52a8",
      loginHint: "\u8bf7\u5148\u5728\u201c\u6211\u7684\u201d\u9875\u9762\u767b\u5f55\u540e\u67e5\u770b\u6d3b\u52a8\u8bb0\u5f55\u3002",
      loading: "\u52a0\u8f7d\u4e2d...",
      locationLabel: "\u5730\u70b9",
      timeLabel: "\u65f6\u95f4",
      statusLabel: "\u72b6\u6001",
      cancelRegister: "\u53d6\u6d88\u62a5\u540d",
      empty: "\u6682\u65e0\u6d3b\u52a8\u8bb0\u5f55",
    },
    loading: false,
    registrations: [],
    loginUser: null,
  },

  onShow() {
    const loginUser = getLoginUser();
    this.setData({ loginUser });
    if (!loginUser) {
      this.setData({ registrations: [] });
      return;
    }
    this.loadRegistrations();
  },

  async loadRegistrations() {
    this.setData({ loading: true });
    try {
      const list = await getMyRegistrations();
      const registrations = (list || []).map((item) => ({
        ...item,
        activityTitleDisplay: item.activityTitle || "\u672A\u77E5\u6D3B\u52A8",
        activityStartDisplay: displayTime(item.activityStartTime),
        activityEndDisplay: displayTime(item.activityEndTime),
      }));
      this.setData({ registrations });
    } catch (e) {
      this.setData({ registrations: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onCancelTap(event) {
    const registrationId = Number(event.currentTarget.dataset.id);
    try {
      await cancelRegistration(registrationId, "\u7528\u6237\u4E3B\u52A8\u53D6\u6D88");
      wx.showToast({ title: "\u53D6\u6D88\u6210\u529F", icon: "success" });
      await this.loadRegistrations();
    } catch (e) {}
  },
});