const { listPublicActivities } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");

const DEFAULT_COVER = "https://picsum.photos/640/360?random=9";

function formatDisplayDate(value) {
  if (!value) {
    return "\u65f6\u95f4\u5f85\u5b9a";
  }
  return String(value).replace("T", " ").slice(5, 16);
}

function mapActivity(item) {
  return {
    ...item,
    cover: item.coverUrl || DEFAULT_COVER,
    startDisplay: formatDisplayDate(item.startTime),
    locationDisplay: item.location || "\u5730\u70b9\u5f85\u5b9a",
  };
}

Page({
  data: {
    i18n: {
      navTitle: "\u9996\u9875",
      myActivity: "\u6211\u7684\u6d3b\u52a8",
      myOrg: "\u6211\u7684\u7ec4\u7ec7",
      publish: "\u53d1\u5e03\u6d3b\u52a8",
      recommend: "\u63a8\u8350\u6d3b\u52a8",
      loading: "\u52a0\u8f7d\u4e2d...",
      emptyRecommend: "\u6682\u65e0\u63a8\u8350\u6d3b\u52a8",
      bannerSub: "\u6821\u56ed\u6d3b\u52a8\u4e00\u7ad9\u5f0f\u670d\u52a1",
    },
    banners: [
      "https://picsum.photos/980/420?random=31",
      "https://picsum.photos/980/420?random=32",
      "https://picsum.photos/980/420?random=33",
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
      wx.showToast({ title: "\u8bf7\u5148\u5728\u6211\u7684\u9875\u767b\u5f55", icon: "none" });
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
      return;
    }

    if (action === "myOrg" || action === "publish") {
      if (loginUser.role !== "ORGANIZER") {
        wx.showToast({ title: "\u4ec5\u7ec4\u7ec7\u8005\u53ef\u4f7f\u7528", icon: "none" });
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
