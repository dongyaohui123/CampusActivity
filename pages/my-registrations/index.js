const { getMyRegistrations, cancelRegistration } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");

function displayTime(value) {
  if (!value) {
    return "-";
  }
  return String(value).replace("T", " ");
}

Page({
  data: {
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
      await cancelRegistration(registrationId, "用户主动取消");
      wx.showToast({ title: "取消成功", icon: "success" });
      await this.loadRegistrations();
    } catch (e) {
      // request.js 已统一提示
    }
  },
});
