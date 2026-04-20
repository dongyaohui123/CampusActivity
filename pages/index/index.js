const { listPublicActivities } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

const DEFAULT_COVER = "https://picsum.photos/640/360?random=9";

function formatDisplayDate(value) {
  if (!value) {
    return "时间待定";
  }
  return String(value).replace("T", " ").slice(5, 16);
}

function mapActivity(item) {
  return {
    ...item,
    cover: item.coverUrl || DEFAULT_COVER,
    startDisplay: formatDisplayDate(item.startTime),
    locationDisplay: item.location || "地点待定",
  };
}

Page({
  data: {
    i18n: {
      navTitle: "首页",
      myActivity: "我的活动",
      myOrg: "我的组织",
      publish: "发布活动",
      recommend: "推荐活动",
      loading: "加载中...",
      emptyRecommend: "暂无推荐活动",
      bannerSub: "校园活动一站式服务",
    },
    banners: [
      "https://picsum.photos/980/420?random=31",
      "https://picsum.photos/980/420?random=32",
      "https://picsum.photos/980/420?random=33",
    ],
    recommendList: [],
    loading: false,
    bottomActive: "home",
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
      feedback.error("请先在我的页登录");
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
      return;
    }

    if (action === "myOrg" || action === "publish") {
      if (loginUser.role !== "ORGANIZER") {
        feedback.error("仅组织者可使用");
        return;
      }
      wx.navigateTo({ url: "/pages/organizer-activities/index" });
    }
  },

  onBottomTabChange(event) {
    const tab = event.detail;
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
