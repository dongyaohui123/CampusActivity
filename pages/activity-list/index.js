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
  { key: "南校区", label: "南校区" },
  { key: "北校区", label: "北校区" },
  { key: "线上", label: "线上" },
];

const STATUS_ITEMS = [
  { key: "ALL", label: "不限" },
  { key: "OPEN", label: "报名中" },
  { key: "CLOSED", label: "已截止" },
];

const STATUS_DISPLAY_MAP = {
  PUBLISHED: "已发布",
  REGISTRATION_OPEN: "报名中",
  REGISTRATION_CLOSED: "报名已截止",
  ONGOING: "进行中",
  FINISHED: "已结束",
  CANCELLED: "已取消",
};

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

/**
 * 基于标题与摘要关键字进行轻量分类。
 */
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
  return String(activity.status || "");
}

function getStatusDisplay(statusKey) {
  if (!statusKey) return "状态待定";
  return STATUS_DISPLAY_MAP[statusKey] || statusKey;
}

function passStatusFilter(statusKey, selectedStatus) {
  if (selectedStatus === "ALL") return true;
  if (selectedStatus === "OPEN") return statusKey === "REGISTRATION_OPEN";
  if (selectedStatus === "CLOSED") return statusKey && statusKey !== "REGISTRATION_OPEN";
  return true;
}

function normalizeLocationCampus(locationCampus) {
  const normalized = String(locationCampus || "").trim().toUpperCase();
  if (normalized === "SOUTH" || normalized === "NORTH" || normalized === "ONLINE") {
    return normalized;
  }
  return "";
}

/**
 * 列表项视图模型统一：封面/时间/地点/分类/状态。
 */
function normalizeActivity(item, now) {
  const categoryKey = classifyByText(item);
  const statusKey = deriveStatusKey(item, now);
  const locationCampus = normalizeLocationCampus(item.locationCampus);
  return {
    ...item,
    cover: item.coverUrl || item.cover || DEFAULT_COVER,
    timeDisplay: formatTime(item.startTime),
    locationDisplay: item.location || "地点待定",
    categoryKey,
    categoryLabel: getCategoryLabel(categoryKey),
    locationCampus,
    statusKey,
    statusDisplay: getStatusDisplay(statusKey),
    isClosedStatus: statusKey !== "REGISTRATION_OPEN",
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
      emptyDesc: "尝试更换关键字或放宽筛选条件",
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
    topPanelPaddingTopPx: 24,
    titleRowMinHeightPx: 32,
    searchBoxMarginTopPx: 8,
    titleRightSafePx: 12,
    fullList: [],
    displayList: [],
    resultCount: 0,
    bottomActive: "find",
  },

  onLoad() {
    this.initStatusBarHeight();
  },

  onShow() {
    this.loadActivities();
  },

  /**
   * 适配自定义导航栏与胶囊按钮，计算标题与搜索框安全区。
   */
  initStatusBarHeight() {
    let statusBarHeight = 20;
    let topPanelPaddingTop = statusBarHeight + 4;
    let titleRowMinHeight = 32;
    let searchBoxMarginTop = 8;
    let titleRightSafe = 12;
    try {
      let windowWidth = 375;
      if (wx.getWindowInfo) {
        const windowInfo = wx.getWindowInfo();
        if (windowInfo) {
          if (typeof windowInfo.statusBarHeight === "number") {
            statusBarHeight = windowInfo.statusBarHeight || statusBarHeight;
          }
          if (typeof windowInfo.windowWidth === "number") {
            windowWidth = windowInfo.windowWidth;
          }
        }
      } else if (wx.getSystemInfoSync) {
        const systemInfo = wx.getSystemInfoSync();
        if (systemInfo) {
          if (typeof systemInfo.statusBarHeight === "number") {
            statusBarHeight = systemInfo.statusBarHeight || statusBarHeight;
          }
          if (typeof systemInfo.windowWidth === "number") {
            windowWidth = systemInfo.windowWidth;
          }
        }
      }

      topPanelPaddingTop = statusBarHeight + 4;
      titleRowMinHeight = 32;
      searchBoxMarginTop = 8;
      titleRightSafe = 12;

      if (wx.getMenuButtonBoundingClientRect) {
        const menuRect = wx.getMenuButtonBoundingClientRect();
        if (
          menuRect &&
          typeof menuRect.top === "number" &&
          typeof menuRect.height === "number" &&
          typeof menuRect.bottom === "number" &&
          typeof menuRect.left === "number" &&
          typeof windowWidth === "number"
        ) {
          topPanelPaddingTop = Math.ceil(menuRect.top);
          titleRowMinHeight = Math.ceil(menuRect.height);
          searchBoxMarginTop = Math.max(
            0,
            Math.ceil((menuRect.bottom + 8) - (topPanelPaddingTop + titleRowMinHeight))
          );
          titleRightSafe = Math.max(titleRightSafe, Math.ceil(windowWidth - menuRect.left + 12));
        }
      }
    } catch (error) {
      topPanelPaddingTop = statusBarHeight + 4;
      titleRowMinHeight = 32;
      searchBoxMarginTop = 8;
      titleRightSafe = 12;
    }
    this.setData({
      topPanelPaddingTopPx: topPanelPaddingTop,
      titleRowMinHeightPx: titleRowMinHeight,
      searchBoxMarginTopPx: searchBoxMarginTop,
      titleRightSafePx: titleRightSafe,
    });
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
    const codeByFilter = {
      南校区: "SOUTH",
      北校区: "NORTH",
      线上: "ONLINE",
    };
    return String(itemLocation || "").toUpperCase() === codeByFilter[selectedLocation];
  },

  /**
   * 多条件筛选流水线：关键字 + 分类 + 时间 + 地点。
   */
  applyFilter() {
    const keyword = String(this.data.keyword || "").trim().toLowerCase();
    const selectedCategory = this.data.selectedCategory;
    const selectedTime = this.data.selectedTime;
    const selectedLocation = this.data.selectedLocation;
    const selectedStatus = this.data.selectedStatus;
    const now = new Date();

    const displayList = this.data.fullList.filter((item) => {
      const passKeyword =
        !keyword ||
        String(item.title || "").toLowerCase().includes(keyword) ||
        String(item.summary || "").toLowerCase().includes(keyword) ||
        String(item.location || "").toLowerCase().includes(keyword);

      const passCategory = selectedCategory === "ALL" || item.categoryKey === selectedCategory;
      const passTime = this.passTimeFilter(item.startTime, selectedTime, now);
      const passLocation = this.passLocationFilter(item.locationCampus, selectedLocation);
      const passStatus = passStatusFilter(item.statusKey, selectedStatus);
      return passKeyword && passCategory && passTime && passLocation && passStatus;
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
