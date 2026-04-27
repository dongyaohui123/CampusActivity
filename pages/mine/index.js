const { getMyRegistrations, listOrganizerActivities, listPendingReviews } = require("../../utils/api");
const { getLoginUser, clearLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

const ORGANIZER_PUBLISHED_STATUS = [
  "PUBLISHED",
  "REGISTRATION_OPEN",
  "REGISTRATION_CLOSED",
  "ONGOING",
  "FINISHED",
];

function formatCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function getAvatarText(loginUser) {
  if (!loginUser) return "访客";
  const base = String(loginUser.nickname || loginUser.username || "用户").trim();
  return base.slice(0, 2) || "用户";
}

function mapStudentStatusText(status, i18n) {
  if (status === "REGISTERED") return i18n.statusRegistered;
  if (status === "WAITLISTED") return i18n.statusWaitlisted;
  if (status === "CHECKED_IN") return i18n.statusCheckedIn;
  if (status === "CANCELLED") return i18n.statusCancelled;
  return i18n.statusUnknown;
}

function mapActivityStatusText(status, i18n) {
  if (status === "DRAFT") return i18n.activityDraft;
  if (status === "PUBLISHED") return i18n.activityPublished;
  if (status === "REGISTRATION_OPEN") return i18n.activityRegistrationOpen;
  if (status === "REGISTRATION_CLOSED") return i18n.activityRegistrationClosed;
  if (status === "ONGOING") return i18n.activityOngoing;
  if (status === "FINISHED") return i18n.activityFinished;
  if (status === "CANCELLED") return i18n.activityCancelled;
  return i18n.statusUnknown;
}

function mapReviewStatusText(status, i18n) {
  if (status === "PENDING") return i18n.reviewPending;
  if (status === "APPROVED") return i18n.reviewApproved;
  if (status === "REJECTED") return i18n.reviewRejected;
  return "";
}

function resolveStudentTabKey(status) {
  if (status === "CHECKED_IN") return "checkedIn";
  if (status === "CANCELLED") return "cancelled";
  return "upcoming";
}

function resolveOrganizerTabKey(activityStatus, reviewStatus) {
  if (reviewStatus === "REJECTED" || activityStatus === "DRAFT") return "pendingFix";
  if (reviewStatus === "PENDING") return "pendingReview";
  if (reviewStatus === "APPROVED" || ORGANIZER_PUBLISHED_STATUS.includes(activityStatus)) return "published";
  return "pendingFix";
}

function getStatusToneByTab(tabKey) {
  if (tabKey === "published" || tabKey === "checkedIn") return "success";
  if (tabKey === "pendingReview" || tabKey === "upcoming") return "warning";
  if (tabKey === "pendingFix") return "danger";
  return "muted";
}

function getRoleTabDefs(role, i18n) {
  if (role === "STUDENT") {
    return [
      { key: "upcoming", label: i18n.tabUpcoming },
      { key: "checkedIn", label: i18n.tabCheckedIn },
      { key: "cancelled", label: i18n.tabCancelled },
    ];
  }
  if (role === "ORGANIZER") {
    return [
      { key: "pendingFix", label: i18n.tabPendingFix },
      { key: "pendingReview", label: i18n.tabPendingReview },
      { key: "published", label: i18n.tabPublished },
    ];
  }
  if (role === "ADMIN") {
    return [{ key: "pendingReview", label: i18n.tabPendingReview }];
  }
  return [];
}

/**
 * 根据登录态和角色生成快捷入口卡片。
 */
function getQuickActions(i18n, loginUser) {
  if (!loginUser) {
    return [
      { iconName: "contact-o", title: i18n.goLogin, desc: i18n.goLoginDesc, action: "login" },
      { iconName: "friends-o", title: i18n.myActivity, desc: i18n.myActivityDesc, action: "myActivity" },
      { iconName: "question-o", title: i18n.helpCenter, desc: i18n.helpDesc, action: "help" },
      { iconName: "service-o", title: i18n.contact, desc: i18n.contactDesc, action: "contact" },
    ];
  }
  const roleAction =
    loginUser.role === "ADMIN"
      ? { iconName: "search", title: i18n.reviewManage, desc: i18n.reviewDesc, action: "review" }
      : loginUser.role === "ORGANIZER"
        ? { iconName: "cluster-o", title: i18n.myOrg, desc: i18n.myOrgDesc, action: "myOrg" }
        : { iconName: "search", title: i18n.findActivity, desc: i18n.findActivityDesc, action: "findActivity" };
  return [
    { iconName: "friends-o", title: i18n.myActivity, desc: i18n.myActivityDesc, action: "myActivity" },
    roleAction,
    { iconName: "question-o", title: i18n.helpCenter, desc: i18n.helpDesc, action: "help" },
    { iconName: "service-o", title: i18n.contact, desc: i18n.contactDesc, action: "contact" },
  ];
}

function getSectionEntry(i18n, loginUser) {
  if (!loginUser) return { text: i18n.goLogin, action: "login" };
  if (loginUser.role === "ORGANIZER") return { text: i18n.goMyOrg, action: "myOrg" };
  if (loginUser.role === "ADMIN") return { text: i18n.goReviewManage, action: "review" };
  return { text: i18n.goMyRegistrations, action: "myActivity" };
}

function buildTabsWithCount(tabDefs, cards) {
  return tabDefs.map((tab) => ({
    key: tab.key,
    label: tab.label,
    count: cards.filter((card) => card.tabKey === tab.key).length,
  }));
}

function mapStudentCard(item, i18n, index) {
  const status = String(item.registrationStatus || "");
  const tabKey = resolveStudentTabKey(status);
  const activityId = Number(item.activityId || 0);
  return {
    itemKey: `student-${index}-${item.registrationId || activityId}`,
    sourceType: "studentRegistration",
    itemId: activityId,
    title: item.activityTitle || i18n.unknownActivity,
    locationText: item.location || i18n.unknownLocation,
    timeText: `${displayTime(item.activityStartTime)} ~ ${displayTime(item.activityEndTime)}`,
    statusKey: status,
    statusText: mapStudentStatusText(status, i18n),
    statusTone: getStatusToneByTab(tabKey),
    tabKey,
    primaryAction: {
      action: "activityDetail",
      itemId: activityId,
      label: i18n.viewDetail,
    },
    secondaryAction: {
      action: "myActivity",
      itemId: Number(item.registrationId || 0),
      label: i18n.viewRecords,
    },
  };
}

function mapOrganizerCard(item, i18n, index) {
  const activityStatus = String(item.status || "");
  const reviewStatus = String(item.reviewStatus || "");
  const tabKey = resolveOrganizerTabKey(activityStatus, reviewStatus);
  const reviewText = mapReviewStatusText(reviewStatus, i18n);
  const activityText = mapActivityStatusText(activityStatus, i18n);
  const statusText = reviewText ? `${reviewText} / ${activityText}` : activityText;
  const activityId = Number(item.id || 0);
  return {
    itemKey: `org-${index}-${activityId}`,
    sourceType: "organizerActivity",
    itemId: activityId,
    title: item.title || i18n.unknownActivity,
    locationText: item.location || i18n.unknownLocation,
    timeText: `${displayTime(item.startTime)} ~ ${displayTime(item.endTime)}`,
    statusKey: `${reviewStatus || "NA"}_${activityStatus || "NA"}`,
    statusText,
    statusTone: getStatusToneByTab(tabKey),
    tabKey,
    primaryAction: {
      action: "myOrg",
      itemId: activityId,
      label: i18n.goManage,
    },
    secondaryAction: {
      action: "activityDetail",
      itemId: activityId,
      label: i18n.viewDetail,
    },
  };
}

function mapAdminCard(item, i18n, index) {
  const activityId = Number(item.activityId || 0);
  return {
    itemKey: `admin-${index}-${activityId}`,
    sourceType: "adminReview",
    itemId: activityId,
    title: item.title || i18n.unknownActivity,
    locationText: `${i18n.organizerPrefix}${item.organizerId || "-"}`,
    timeText: `${displayTime(item.startTime)} ~ ${displayTime(item.endTime)}`,
    statusKey: String(item.reviewStatus || "PENDING"),
    statusText: mapReviewStatusText(String(item.reviewStatus || "PENDING"), i18n),
    statusTone: getStatusToneByTab("pendingReview"),
    tabKey: "pendingReview",
    primaryAction: {
      action: "review",
      itemId: activityId,
      label: i18n.goReviewManage,
    },
    secondaryAction: {
      action: "activityDetail",
      itemId: activityId,
      label: i18n.viewDetail,
    },
  };
}

Page({
  data: {
    i18n: {
      navTitle: "我的",
      guestName: "未登录用户",
      guestDesc: "登录后可查看你的活动进度和管理入口",
      welcomeBack: "欢迎回来，继续你的校园活动安排",
      userIdPrefix: "用户ID：",
      participatedCount: "参与活动数",
      initiatedCount: "发起活动数",
      managedOrgCount: "管理组织数",
      myActivity: "我的活动",
      myActivityDesc: "查看报名记录和状态",
      myOrg: "我的组织",
      myOrgDesc: "管理活动与发布流程",
      reviewManage: "审核管理",
      reviewDesc: "处理活动审核任务",
      helpCenter: "帮助中心",
      helpDesc: "常见问题与使用说明",
      contact: "联系客服",
      contactDesc: "反馈问题和建议",
      settingsTitle: "设置",
      settingsDesc: "账号与安全",
      changePassword: "修改密码",
      changePasswordDesc: "更新登录密码并重新登录",
      findActivity: "找活动",
      findActivityDesc: "浏览并报名校园活动",
      logout: "退出登录",
      goLogin: "去登录",
      goLoginDesc: "登录后即可同步个人活动数据",
      goMyRegistrations: "查看报名记录",
      goMyOrg: "进入组织管理",
      goReviewManage: "进入审核管理",
      loading: "加载中...",
      unknownActivity: "未知活动",
      unknownLocation: "地点待定",
      locationLabel: "地点",
      timeLabel: "时间",
      guestActivityHint: "登录后查看你的活动安排",
      emptyStudent: "该分栏暂无活动记录",
      emptyOrganizer: "暂无符合条件的组织活动",
      emptyAdmin: "暂无待审核活动",
      statusRegistered: "已报名",
      statusWaitlisted: "候补中",
      statusCheckedIn: "已签到",
      statusCancelled: "已取消",
      statusUnknown: "状态未知",
      activityDraft: "待完善",
      activityPublished: "已发布",
      activityRegistrationOpen: "报名中",
      activityRegistrationClosed: "报名已截止",
      activityOngoing: "进行中",
      activityFinished: "已结束",
      activityCancelled: "活动已取消",
      reviewPending: "待审核",
      reviewApproved: "已通过",
      reviewRejected: "已驳回",
      tabUpcoming: "待参加",
      tabCheckedIn: "已签到",
      tabCancelled: "已取消",
      tabPendingFix: "待完善",
      tabPendingReview: "待审核",
      tabPublished: "已发布",
      viewDetail: "活动详情",
      viewRecords: "报名记录",
      goManage: "组织管理",
      organizerPrefix: "组织者ID：",
    },
    loginUser: null,
    avatarText: "访客",
    quickActions: [],
    sectionEntry: {
      text: "",
      action: "login",
    },
    bottomActive: "mine",
    stats: {
      participatedCount: 0,
      initiatedCount: 0,
      managedOrgCount: 0,
    },
    activityTabs: [],
    activeActivityTab: "",
    activityCards: [],
    filteredActivityCards: [],
    loadingActivities: false,
    activityEmptyText: "",
  },

  onShow() {
    this.restoreLoginState();
  },

  /**
   * 恢复登录态后并行加载统计与卡片数据。
   */
  async restoreLoginState() {
    const loginUser = getLoginUser();
    const { i18n } = this.data;
    const quickActions = getQuickActions(i18n, loginUser);
    const sectionEntry = getSectionEntry(i18n, loginUser);
    this.setData({
      loginUser,
      avatarText: getAvatarText(loginUser),
      quickActions,
      sectionEntry,
    });
    if (!loginUser) {
      this.setData({
        stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 },
        activityTabs: [],
        activeActivityTab: "",
        activityCards: [],
        filteredActivityCards: [],
        activityEmptyText: i18n.guestActivityHint,
        loadingActivities: false,
      });
      return;
    }
    await Promise.all([this.loadStats(loginUser), this.loadMineActivityCards(loginUser)]);
  },

  async loadStats(loginUser) {
    if (!loginUser) return;
    let participatedCount = 0;
    let initiatedCount = 0;
    let managedOrgCount = 0;

    try {
      const myRegistrations = await getMyRegistrations(loginUser.id);
      participatedCount = (myRegistrations || []).length;
    } catch (e) {
      participatedCount = 0;
    }

    if (loginUser.role === "ORGANIZER") {
      try {
        const myActivities = await listOrganizerActivities({});
        initiatedCount = (myActivities || []).length;
      } catch (e) {
        initiatedCount = 0;
      }
      managedOrgCount = 1;
    }

    this.setData({
      stats: {
        participatedCount: formatCount(participatedCount),
        initiatedCount: formatCount(initiatedCount),
        managedOrgCount: formatCount(managedOrgCount),
      },
    });
  },

  /**
   * 按角色加载活动卡片，统一抽象到 tab + card 结构渲染。
   */
  async loadMineActivityCards(loginUser) {
    if (!loginUser) return;
    const { i18n } = this.data;
    const role = loginUser.role || "STUDENT";
    this.setData({ loadingActivities: true });
    let cards = [];

    try {
      if (role === "STUDENT") {
        const rows = await getMyRegistrations(loginUser.id);
        cards = (rows || []).map((item, index) => mapStudentCard(item, i18n, index));
      } else if (role === "ORGANIZER") {
        const rows = await listOrganizerActivities({});
        cards = (rows || []).map((item, index) => mapOrganizerCard(item, i18n, index));
      } else if (role === "ADMIN") {
        const rows = await listPendingReviews();
        cards = (rows || []).map((item, index) => mapAdminCard(item, i18n, index));
      }
    } catch (e) {
      cards = [];
    }

    const tabDefs = getRoleTabDefs(role, i18n);
    const activityTabs = buildTabsWithCount(tabDefs, cards);
    const activeActivityTab =
      activityTabs.find((tab) => tab.key === this.data.activeActivityTab)?.key || (activityTabs[0] && activityTabs[0].key) || "";
    const filteredActivityCards = cards.filter((card) => card.tabKey === activeActivityTab);
    const activityEmptyText =
      role === "ORGANIZER" ? i18n.emptyOrganizer : role === "ADMIN" ? i18n.emptyAdmin : i18n.emptyStudent;

    this.setData({
      activityTabs,
      activeActivityTab,
      activityCards: cards,
      filteredActivityCards,
      activityEmptyText,
      loadingActivities: false,
    });
  },

  onActivityTabTap(event) {
    const key = event.currentTarget.dataset.key;
    const filteredActivityCards = this.data.activityCards.filter((card) => card.tabKey === key);
    this.setData({
      activeActivityTab: key,
      filteredActivityCards,
    });
  },

  onCardActionTap(event) {
    const action = event.currentTarget.dataset.action;
    const itemId = Number(event.currentTarget.dataset.itemId || 0);
    this.handleAction(action, { itemId });
  },

  onQuickActionTap(event) {
    const action = event.currentTarget.dataset.action;
    this.handleAction(action);
  },

  onSectionMoreTap(event) {
    const action = event.currentTarget.dataset.action;
    this.handleAction(action);
  },

  onMenuTap(event) {
    const action = event.currentTarget.dataset.action;
    this.handleAction(action);
  },

  /**
   * 我的页统一动作分发器。
   */
  handleAction(action, payload) {
    const loginUser = this.data.loginUser;
    if (action === "help") {
      feedback.info("帮助中心开发中");
      return;
    }
    if (action === "contact") {
      feedback.info("联系客服：400-000-0000");
      return;
    }
    if (action === "login") {
      this.goToAuth();
      return;
    }
    if (action === "activityDetail") {
      const activityId = Number(payload && payload.itemId);
      if (!activityId) {
        feedback.error("活动信息缺失");
        return;
      }
      wx.navigateTo({ url: `/pages/activity-detail/index?activityId=${activityId}` });
      return;
    }
    if (action === "findActivity") {
      wx.reLaunch({ url: "/pages/activity-list/index" });
      return;
    }

    if (!loginUser) {
      this.goToAuth();
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
      return;
    }
    if (action === "changePassword") {
      wx.navigateTo({ url: "/pages/change-password/index" });
      return;
    }
    if (action === "myOrg") {
      if (loginUser.role !== "ORGANIZER") {
        feedback.error("仅组织者可查看");
        return;
      }
      wx.navigateTo({ url: "/pages/organizer-activities/index" });
      return;
    }
    if (action === "review") {
      if (loginUser.role !== "ADMIN") {
        feedback.error("仅管理员可使用");
        return;
      }
      wx.navigateTo({ url: "/pages/admin-review/index" });
    }
  },

  goToAuth() {
    wx.navigateTo({ url: "/pages/auth/index?mode=login" });
  },

  onLogoutTap() {
    clearLoginUser();
    const { i18n } = this.data;
    this.setData({
      loginUser: null,
      avatarText: "访客",
      quickActions: getQuickActions(i18n, null),
      sectionEntry: getSectionEntry(i18n, null),
      stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 },
      activityTabs: [],
      activeActivityTab: "",
      activityCards: [],
      filteredActivityCards: [],
      activityEmptyText: i18n.guestActivityHint,
      loadingActivities: false,
    });
    feedback.success("已退出登录");
  },

  onBottomTabChange(event) {
    const tab = event.detail;
    if (tab === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (tab === "find") {
      wx.reLaunch({ url: "/pages/activity-list/index" });
    }
  },
});
