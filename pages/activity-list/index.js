const { listPublicActivities } = require("../../utils/api");

const DEFAULT_COVER = "https://picsum.photos/600/360?random=12";

const CATEGORY_ITEMS = [
  { key: "ALL", label: "全部" },
  { key: "SPORTS", label: "运动" },
  { key: "LIFE", label: "生活" },
  { key: "EDU", label: "教育" },
  { key: "PARTY", label: "聚会" },
  { key: "OTHER", label: "其他" },
];

function formatTime(time) {
  if (!time) {
    return "时间待定";
  }
  return String(time).replace("T", " ").slice(5, 16);
}

function classifyByText(activity) {
  const text = `${activity.title || ""} ${activity.summary || ""}`.toLowerCase();
  if (/sport|basketball|football|run|fitness|运动|篮球|足球|跑步|健身/.test(text)) return "SPORTS";
  if (/life|mental|health|community|charity|生活|心理|健康|社区|公益/.test(text)) return "LIFE";
  if (/lecture|education|study|academic|training|教育|讲座|学习|学术|培训/.test(text)) return "EDU";
  if (/party|music|salon|gathering|聚会|音乐|沙龙|联谊|晚会/.test(text)) return "PARTY";
  return "OTHER";
}

function normalizeActivity(item) {
  return {
    ...item,
    cover: item.coverUrl || DEFAULT_COVER,
    timeDisplay: formatTime(item.startTime),
    locationDisplay: item.location || "地点待定",
    categoryKey: classifyByText(item),
  };
}

Page({
  data: {
    i18n: {
      navTitle: "找活动",
      searchPlaceholder: "请输入活动名称或地点",
      loading: "加载中...",
      empty: "暂无更多活动",
    },
    loading: false,
    keyword: "",
    selectedCategory: "ALL",
    categories: CATEGORY_ITEMS,
    fullList: [],
    displayList: [],
    bottomActive: "find",
  },

  onShow() {
    this.loadActivities();
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail });
    this.applyFilter();
  },

  onCategoryTap(event) {
    const category = event.currentTarget.dataset.category;
    this.setData({ selectedCategory: category });
    this.applyFilter();
  },

  onCategoryChange(event) {
    const category = event.detail;
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
    const tab = event.detail;
    if (tab === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (tab === "find") return;
    if (tab === "mine") wx.reLaunch({ url: "/pages/mine/index" });
  },
});
