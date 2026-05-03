const { listPublicActivities } = require("../../utils/api");
const { getLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");
const { getActivityFallbackCover, resolveActivityCover } = require("../../utils/image-fallbacks");

function formatDisplayDate(value) {
  if (!value) {
    return "时间待定";
  }
  return String(value).replace("T", " ").slice(5, 16);
}

/**
 * 首页活动卡片展示字段映射。
 */
function mapActivity(item) {
  const fallbackCover = getActivityFallbackCover(item);
  return {
    ...item,
    cover: resolveActivityCover(item),
    fallbackCover,
    startDisplay: formatDisplayDate(item.startTime),
    locationDisplay: item.location || "地点待定",
  };
}

Page({
  data: {
    i18n: {
      city: "合肥",
      navTitle: "首页",
      myActivity: "我的活动",
      myOrg: "我的组织",
      publish: "发布活动",
      recommend: "为你推荐",
      more: "更多",
      signing: "报名中",
      loading: "加载中...",
      emptyRecommend: "暂无推荐活动",
      searchPlaceholder: "请搜索活动名",
      bannerTitle: "校园活动管理平台",
      bannerSub: "一站式发现、报名与管理校园活动",
    },
    banners: [
      "/static/home-banners/banner-01.png",
      "/static/home-banners/banner-02.png",
      "/static/home-banners/banner-03.png",
    ],
    recommendList: [],
    loading: false,
    bottomActive: "home",
    topPanelPaddingTopPx: 20,
    cityRowMinHeightPx: 32,
    searchBoxMarginTopPx: 8,
  },

  onLoad() {
    this.initTopPanelSafePadding();
  },

  onShow() {
    this.loadRecommendList();
  },

  /**
   * 基于状态栏/胶囊信息动态计算顶部安全区，适配不同机型。
   */
  initTopPanelSafePadding() {
    let statusBarHeight = 20;
    let topPanelPaddingTopPx = statusBarHeight + 4;
    let cityRowMinHeightPx = 32;
    let searchBoxMarginTopPx = 8;

    try {
      const systemInfo = wx.getWindowInfo
        ? wx.getWindowInfo()
        : wx.getSystemInfoSync
          ? wx.getSystemInfoSync()
          : null;

      if (systemInfo && typeof systemInfo.statusBarHeight === "number") {
        statusBarHeight = systemInfo.statusBarHeight || statusBarHeight;
      }

      topPanelPaddingTopPx = statusBarHeight + 4;
      cityRowMinHeightPx = 32;
      searchBoxMarginTopPx = 8;

      if (wx.getMenuButtonBoundingClientRect) {
        const menuRect = wx.getMenuButtonBoundingClientRect();
        if (
          menuRect &&
          typeof menuRect.top === "number" &&
          typeof menuRect.height === "number" &&
          typeof menuRect.bottom === "number"
        ) {
          topPanelPaddingTopPx = Math.ceil(menuRect.top);
          cityRowMinHeightPx = Math.ceil(menuRect.height);
          searchBoxMarginTopPx = Math.max(
            0,
            Math.ceil((menuRect.bottom + 8) - (topPanelPaddingTopPx + cityRowMinHeightPx))
          );
        }
      }
    } catch (error) {
      topPanelPaddingTopPx = statusBarHeight + 4;
      cityRowMinHeightPx = 32;
      searchBoxMarginTopPx = 8;
    }

    this.setData({
      topPanelPaddingTopPx,
      cityRowMinHeightPx,
      searchBoxMarginTopPx,
    });
  },

  goFind() {
    wx.reLaunch({ url: "/pages/activity-list/index" });
  },

  /**
   * 拉取推荐活动并裁剪为首页展示数量。
   */
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

  onRecommendImageError(event) {
    const index = Number(event.currentTarget.dataset.index);
    if (!Number.isInteger(index) || index < 0) {
      return;
    }
    const target = this.data.recommendList[index];
    if (target && target.cover !== target.fallbackCover) {
      this.setData({
        [`recommendList[${index}].cover`]: target.fallbackCover,
      });
    }
  },

  /**
   * 快捷入口统一分流：未登录拦截、组织者角色校验、页面跳转。
   */
  onQuickTap(event) {
    const action = event.currentTarget.dataset.action;
    const loginUser = getLoginUser();
    if (!loginUser) {
      feedback.error("请先在“我的”页登录");
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
