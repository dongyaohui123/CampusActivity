const { listOrganizerActivityRegistrations, listManageableActivities } = require("../../utils/api");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function mapStatusText(status) {
  if (status === "REGISTERED") return "待核销";
  if (status === "CHECKED_IN") return "已签到";
  if (status === "CANCELLED") return "已取消";
  return status || "状态未知";
}

Page({
  data: {
    i18n: {
      navTitle: "报名名单",
      loading: "加载中...",
      empty: "暂无报名记录",
      activityLabel: "活动",
      timeLabel: "活动时间",
      locationLabel: "活动地点",
    },
    activityId: 0,
    loading: false,
    activity: null,
    registrations: [],
  },

  onLoad(options) {
    this.setData({ activityId: Number(options.activityId || 0) });
  },

  onShow() {
    if (this.data.activityId) {
      this.loadRegistrations();
    }
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadRegistrations() {
    this.setData({ loading: true });
    try {
      const [activities, rows] = await Promise.all([
        listManageableActivities({}),
        listOrganizerActivityRegistrations(this.data.activityId),
      ]);
      const activity =
        (activities || []).find((item) => Number(item.id) === Number(this.data.activityId)) || {
          id: this.data.activityId,
          title: "未知活动",
          location: "-",
          startTime: "",
          endTime: "",
        };
      this.setData({
        activity: {
          ...activity,
          startDisplay: displayTime(activity.startTime),
          endDisplay: displayTime(activity.endTime),
        },
        registrations: (rows || []).map((item) => ({
          ...item,
          statusText: mapStatusText(item.status),
          registeredDisplay: displayTime(item.registeredAt),
          checkinDisplay: displayTime(item.checkinAt),
        })),
      });
    } catch (e) {
      this.setData({ activity: null, registrations: [] });
      feedback.error("报名名单加载失败");
    } finally {
      this.setData({ loading: false });
    }
  },
});
