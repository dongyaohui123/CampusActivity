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

const TIME_ITEMS = [
  { key: "ALL", label: "不限" },
  { key: "TODAY", label: "今天" },
  { key: "THIS_WEEK", label: "本周" },
  { key: "WEEKEND", label: "周末" },
];

const LOCATION_ITEMS = [
  { key: "ALL", label: "不限" },
  { key: "东校区", label: "东校区" },
  { key: "西校区", label: "西校区" },
  { key: "图书馆", label: "图书馆" },
  { key: "线上", label: "线上" },
];

const STATUS_ITEMS = [
  { key: "ALL", label: "不限" },
  { key: "OPEN", label: "报名中" },
  { key: "CLOSED", label: "已截止" },
];

function parseTime(value) {
  const time = new Date(value);
  return Number.isNaN(time.getTime()) ? null : time;
}

function padNumber(value) {
  return String(value).padStart(2, "0");
}

function formatTime(timeText) {
  const time = parseTime(timeText);
  if (!time) {
    return "时间待定";
  }
  const year = time.getFullYear();
  const month = padNumber(time.getMonth() + 1);
  const day = padNumber(time.getDate());
  const hour = padNumber(time.getHours());
  const minute = padNumber(time.getMinutes());
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function isSameDay(dateA, dateB) {
  return (
    dateA.getFullYear() === dateB.getFullYear() &&
    dateA.getMonth() === dateB.getMonth() &&
    dateA.getDate() === dateB.getDate()
  );
}

function isWeekend(date) {
  const day = date.getDay();
  return day === 0 || day === 6;
}

function inThisWeek(date, now) {
  const day = now.getDay() || 7;
  const monday = new Date(now);
  monday.setDate(now.getDate() - day + 1);
  monday.setHours(0, 0, 0, 0);

  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  sunday.setHours(23, 59, 59, 999);

  return date >= monday && date <= sunday;
}

function classifyByText(activity) {
  const text = `${activity.title || ""} ${activity.summary || ""}`.toLowerCase();
  if (/sport|basketball|football|run|fitness|运动|篮球|足球|跑步|健身/.test(text)) return "SPORTS";
  if (/life|mental|health|community|charity|生活|心理|健康|社区|公益/.test(text)) return "LIFE";
  if (/lecture|education|study|academic|training|教育|讲座|学习|学术|培训/.test(text)) return "EDU";
  if (/party|music|salon|gathering|聚会|音乐|沙龙|联谊|晚会/.test(text)) return "PARTY";
  return "OTHER";
}

function getCategoryLabel(key) {
  const category = CATEGORY_ITEMS.find((item) => item.key === key);
  return category ? category.label : "其他";
}

function deriveStatusKey(activity, now) {
  if (activity.status === "OPEN" || activity.status === "CLOSED") {
    return activity.status;
  }
  const start = parseTime(activity.startTime);
  if (start && start < now) {
    return "CLOSED";
  }
  return "OPEN";
}

function getStatusDisplay(statusKey) {
  return statusKey === "CLOSED" ? "已截止" : "报名中";
}

function normalizeActivity(item, now) {
  const categoryKey = classifyByText(item);
  const statusKey = deriveStatusKey(item, now);
  return {
    ...item,
    cover: item.coverUrl || item.cover || DEFAULT_COVER,
    timeDisplay: formatTime(item.startTime),
    locationDisplay: item.location || "地点待定",
    categoryKey,
    categoryLabel: getCategoryLabel(categoryKey),
    statusKey,
    statusDisplay: getStatusDisplay(statusKey),
  };
}

Page({
  data: {
    i18n: {
      pageTitle: "校园活动搜索",
      searchPlaceholder: "搜索活动标题或地点",
      filterTitle: "筛选条件",
      resultTitle: "活动列表",
      loading: "加载中...",
      emptyTitle: "暂无匹配活动",
      emptyDesc: "尝试更换关键词或放宽筛选条件",
    },
    loading: false,
    keyword: "",
    selectedCategory: "ALL",
    categories: CATEGORY_ITEMS,
    selectedTime: "ALL",
    timeOptions: TIME_ITEMS,
    selectedLocation: "ALL",
    locationOptions: LOCATION_ITEMS,
    selectedStatus: "ALL",
    statusOptions: STATUS_ITEMS,
    filterExpanded: true,
    filterSummary: "时间不限 · 地点不限 · 状态不限",
    fullList: [],
    displayList: [],
    resultCount: 0,
    bottomActive: "find",
  },

  onShow() {
    this.loadActivities();
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value || "" });
    this.applyFilter();
  },

  onCategoryTap(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ selectedCategory: key });
    this.applyFilter();
  },

  onTimeTap(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ selectedTime: key });
    this.applyFilter();
  },

  onLocationTap(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ selectedLocation: key });
    this.applyFilter();
  },

  onStatusTap(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ selectedStatus: key });
    this.applyFilter();
  },

  toggleFilterPanel() {
    this.setData({ filterExpanded: !this.data.filterExpanded });
  },

  buildFilterSummary() {
    const timeLabel = TIME_ITEMS.find((item) => item.key === this.data.selectedTime)?.label || "不限";
    const locationLabel = LOCATION_ITEMS.find((item) => item.key === this.data.selectedLocation)?.label || "不限";
    const statusLabel = STATUS_ITEMS.find((item) => item.key === this.data.selectedStatus)?.label || "不限";
    return `时间${timeLabel} · 地点${locationLabel} · 状态${statusLabel}`;
  },

  passTimeFilter(itemTime, selectedTime, now) {
    if (selectedTime === "ALL") return true;
    const time = parseTime(itemTime);
    if (!time) return false;
    if (selectedTime === "TODAY") return isSameDay(time, now);
    if (selectedTime === "THIS_WEEK") return inThisWeek(time, now);
    if (selectedTime === "WEEKEND") return isWeekend(time);
    return true;
  },

  passLocationFilter(itemLocation, selectedLocation) {
    if (selectedLocation === "ALL") return true;
    return String(itemLocation || "").includes(selectedLocation);
  },

  applyFilter() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase();
    const selectedCategory = this.data.selectedCategory;
    const selectedTime = this.data.selectedTime;
    const selectedLocation = this.data.selectedLocation;
    const now = new Date();

    const displayList = this.data.fullList.filter((item) => {
      const passKeyword =
        !keyword ||
        String(item.title || "").toLowerCase().includes(keyword) ||
        String(item.summary || "").toLowerCase().includes(keyword) ||
        String(item.location || "").toLowerCase().includes(keyword);

      const passCategory = selectedCategory === "ALL" || item.categoryKey === selectedCategory;
      const passTime = this.passTimeFilter(item.startTime, selectedTime, now);
      const passLocation = this.passLocationFilter(item.location, selectedLocation);
      return passKeyword && passCategory && passTime && passLocation;
    });

    this.setData({
      displayList,
      resultCount: displayList.length,
      filterSummary: this.buildFilterSummary(),
    });
  },

  async loadActivities() {
    this.setData({ loading: true });
    try {
      const now = new Date();
      const list = await listPublicActivities({});
      this.setData({ fullList: (list || []).map((item) => normalizeActivity(item, now)) });
      this.applyFilter();
    } catch (error) {
      this.setData({
        fullList: [],
        displayList: [],
        resultCount: 0,
        filterSummary: this.buildFilterSummary(),
      });
    } finally {
      this.setData({ loading: false });
    }
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
    if (tab === "mine") {
      wx.reLaunch({ url: "/pages/mine/index" });
    }
  },
});
