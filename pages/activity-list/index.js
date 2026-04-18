const { listPublicActivities } = require("../../utils/api");

const DEFAULT_COVER = "https://picsum.photos/600/360?random=12";

const CATEGORY_ITEMS = [
  { key: "ALL", label: "\u5168\u90e8" },
  { key: "SPORTS", label: "\u8fd0\u52a8" },
  { key: "LIFE", label: "\u751f\u6d3b" },
  { key: "EDU", label: "\u6559\u80b2" },
  { key: "PARTY", label: "\u805a\u4f1a" },
  { key: "OTHER", label: "\u5176\u4ed6" },
];

function formatTime(time) {
  if (!time) {
    return "\u65f6\u95f4\u5f85\u5b9a";
  }
  return String(time).replace("T", " ").slice(5, 16);
}

function classifyByText(activity) {
  const text = `${activity.title || ""} ${activity.summary || ""}`.toLowerCase();
  if (/sport|basketball|football|run|fitness|\u8fd0\u52a8|\u7bee\u7403|\u8db3\u7403|\u8dd1\u6b65|\u5065\u8eab/.test(text)) return "SPORTS";
  if (/life|mental|health|community|charity|\u751f\u6d3b|\u5fc3\u7406|\u5065\u5eb7|\u793e\u533a|\u516c\u76ca/.test(text)) return "LIFE";
  if (/lecture|education|study|academic|training|\u6559\u80b2|\u8bb2\u5ea7|\u5b66\u4e60|\u5b66\u672f|\u57f9\u8bad/.test(text)) return "EDU";
  if (/party|music|salon|gathering|\u805a\u4f1a|\u97f3\u4e50|\u6c99\u9f99|\u8054\u8c0a|\u665a\u4f1a/.test(text)) return "PARTY";
  return "OTHER";
}

function normalizeActivity(item) {
  return {
    ...item,
    cover: item.coverUrl || DEFAULT_COVER,
    timeDisplay: formatTime(item.startTime),
    locationDisplay: item.location || "\u5730\u70b9\u5f85\u5b9a",
    categoryKey: classifyByText(item),
  };
}

Page({
  data: {
    i18n: {
      navTitle: "\u627e\u6d3b\u52a8",
      searchPlaceholder: "\u8bf7\u8f93\u5165\u6d3b\u52a8\u540d\u79f0\u6216\u5730\u70b9",
      loading: "\u52a0\u8f7d\u4e2d...",
      empty: "\u6682\u65e0\u66f4\u591a\u6d3b\u52a8",
    },
    loading: false,
    keyword: "",
    selectedCategory: "ALL",
    categories: CATEGORY_ITEMS,
    fullList: [],
    displayList: [],
  },

  onShow() {
    this.loadActivities();
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value });
    this.applyFilter();
  },

  onCategoryTap(event) {
    const category = event.currentTarget.dataset.category;
    this.setData({ selectedCategory: category });
    this.applyFilter();
  },

  async loadActivities() {
    this.setData({ loading: true });
    try {
      const list = await listPublicActivities({});
      this.setData({ fullList: (list || []).map(normalizeActivity) });
      this.applyFilter();
    } catch (e) {
      this.setData({ fullList: [], displayList: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  applyFilter() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase();
    const selectedCategory = this.data.selectedCategory;
    // 从全量名单中进行过滤
    const displayList = this.data.fullList.filter((item) => {
      // 关键词匹配（标题、简介、地点里包含这个词吗？）
      const passKeyword =
        !keyword ||
        (item.title || "").toLowerCase().includes(keyword) ||
        (item.summary || "").toLowerCase().includes(keyword) ||
        (item.location || "").toLowerCase().includes(keyword);
      //分类匹配（是选中的分类吗？）
      const passCategory = selectedCategory === "ALL" || item.categoryKey === selectedCategory;
      //两项都满足才能通过
      return passKeyword && passCategory;
    });
    // 更新到屏幕上
    this.setData({ displayList });
  },

  goDetail(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    wx.navigateTo({ url: `/pages/activity-detail/index?activityId=${activityId}` });
  },

  onBottomTabChange(event) {
    const tab = event.detail.tab;
    if (tab === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (tab === "find") return;
    if (tab === "mine") wx.reLaunch({ url: "/pages/mine/index" });
  },
});