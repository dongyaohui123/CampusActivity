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
  const value = String(time).replace("T", " ");
  return value.slice(5, 16).replace("-", "月").replace(" ", "日 ");
}

function classifyByText(activity) {
  const text = `${activity.title || ""} ${activity.summary || ""}`.toLowerCase();
  if (/运动|篮球|足球|跑步|球赛|健身/.test(text)) {
    return "SPORTS";
  }
  if (/生活|心理|公益|健康|社区/.test(text)) {
    return "LIFE";
  }
  if (/教育|讲座|学习|学术|培训/.test(text)) {
    return "EDU";
  }
  if (/聚会|联谊|沙龙|晚会|音乐|歌手/.test(text)) {
    return "PARTY";
  }
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
      const fullList = (list || []).map(normalizeActivity);
      this.setData({ fullList });
      this.applyFilter();
    } catch (e) {
      this.setData({
        fullList: [],
        displayList: [],
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  applyFilter() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase();
    const selectedCategory = this.data.selectedCategory;
    const displayList = this.data.fullList.filter((item) => {
      const passKeyword =
        !keyword ||
        (item.title || "").toLowerCase().includes(keyword) ||
        (item.summary || "").toLowerCase().includes(keyword) ||
        (item.location || "").toLowerCase().includes(keyword);
      const passCategory = selectedCategory === "ALL" || item.categoryKey === selectedCategory;
      return passKeyword && passCategory;
    });
    this.setData({ displayList });
  },

  goDetail(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    wx.navigateTo({
      url: `/pages/activity-detail/index?activityId=${activityId}`,
    });
  },

  onBottomTabChange(event) {
    const tab = event.detail.tab;
    if (tab === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (tab === "find") {
      return;
    }
    if (tab === "mine") {
      wx.reLaunch({ url: "/pages/mine/index" });
    }
  },
});
