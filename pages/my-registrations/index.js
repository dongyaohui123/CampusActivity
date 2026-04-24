const { getMyRegistrations, cancelRegistration } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

Page({
  data: {
    i18n: {
      navTitle: "我的活动",
      loginHint: "请先在“我的”页面登录后查看活动记录。",
      loading: "加载中...",
      locationLabel: "地点",
      timeLabel: "时间",
      statusLabel: "状态",
      cancelRegister: "取消报名",
      empty: "暂无活动记录",
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

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadRegistrations() {
    this.setData({ loading: true });
    try {
      const list = await getMyRegistrations();
      const registrations = (list || []).map((item) => ({
        ...item,
        activityTitleDisplay: item.activityTitle || "未知活动",
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
      feedback.success("取消成功");
      await this.loadRegistrations();
    } catch (e) {}
  },
});
