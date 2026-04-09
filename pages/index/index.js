const { listPublicActivities } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");

const DEFAULT_COVER = "https://picsum.photos/600/360?random=9";

function mapActivity(item) {
  const start = String(item.startTime || "").replace("T", " ");
  return {
    ...item,
    cover: item.coverUrl || DEFAULT_COVER,
    startDisplay: start
      ? start.slice(5, 16).replace("-", "月").replace(" ", "日 ")
      : "时间待定",
    locationDisplay: item.location || "地点待定",
  };
}

Page({
  data: {
    banners: [
      "https://picsum.photos/900/400?random=31",
      "https://picsum.photos/900/400?random=32",
      "https://picsum.photos/900/400?random=33",
    ],
    recommendList: [],
    loading: false,
  },

  onShow() {
    this.loadRecommendList();
  },

  async loadRecommendList() {
    this.setData({ loading: true });
    try {
      const list = await listPublicActivities({});
      const recommendList = (list || []).slice(0, 6).map(mapActivity);
      this.setData({ recommendList });
    } catch (e) {
      this.setData({ recommendList: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  goDetail(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    wx.navigateTo({
      url: `/pages/activity-detail/index?activityId=${activityId}`,
    });
  },

  onQuickTap(event) {
    const action = event.currentTarget.dataset.action;
    const loginUser = getLoginUser();
    if (!loginUser) {
      wx.showToast({ title: "请先在“我的”登录", icon: "none" });
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
      return;
    }

    if (action === "myOrg" || action === "publish") {
      if (loginUser.role !== "ORGANIZER") {
        wx.showToast({ title: "仅组织者可使用", icon: "none" });
        return;
      }
      wx.navigateTo({ url: "/pages/organizer-activities/index" });
    }
  },

  onBottomTabChange(event) {
    const tab = event.detail.tab;
    if (tab === "home") {
      return;
    }
    if (tab === "find") {
      wx.reLaunch({ url: "/pages/activity-list/index" });
      return;
    }
    if (tab === "mine") {
      wx.reLaunch({ url: "/pages/mine/index" });
    }
  },
});
