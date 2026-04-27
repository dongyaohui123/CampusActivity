const {
  getPublicActivityDetail,
  getMyRegistrations,
  getActivityFavoriteStatus,
  favoriteActivity,
  unfavoriteActivity,
  registerActivity,
  cancelRegistration,
} = require("../../utils/api");
const { getOperatorContext, hasOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

const DEFAULT_COVER = "https://picsum.photos/900/506?random=18";

const ACTIVITY_STATUS_MAP = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  REGISTRATION_OPEN: "报名中",
  REGISTRATION_CLOSED: "报名截止",
  ONGOING: "进行中",
  FINISHED: "已结束",
  CANCELLED: "已取消",
};

const REGISTRATION_STATUS_MAP = {
  REGISTERED: "已报名",
  CHECKED_IN: "已签到",
  CANCELLED: "已取消报名",
  REJECTED: "已拒绝",
};

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function normalizeActivityStatus(status) {
  if (!status) return "状态待定";
  return ACTIVITY_STATUS_MAP[status] || status;
}

function normalizeRegistrationStatus(registration) {
  if (!registration || !registration.registrationStatus) return "未报名";
  const status = registration.registrationStatus;
  return REGISTRATION_STATUS_MAP[status] || status;
}

function isRegistrationOpenActivity(status) {
  return status === "REGISTRATION_OPEN";
}

function isPreStartActivity(status) {
  return status === "REGISTRATION_OPEN" || status === "REGISTRATION_CLOSED";
}

function deriveRegistrationActions(activity, currentRegistration) {
  const activityStatus = String((activity && activity.status) || "");
  const registrationStatus = String((currentRegistration && currentRegistration.registrationStatus) || "");
  return {
    canRegister: isRegistrationOpenActivity(activityStatus) && (!registrationStatus || registrationStatus === "CANCELLED"),
    canCancel: registrationStatus === "REGISTERED" && isPreStartActivity(activityStatus),
    canViewTicket: registrationStatus === "REGISTERED" || registrationStatus === "CHECKED_IN",
  };
}

function buildSharePayload(data) {
  const activityId = Number(data.activityId || 0);
  const title = String((data.activity && data.activity.title) || data.i18n.shareTitleFallback || "校园活动").trim();
  const imageUrl = String(data.coverDisplay || DEFAULT_COVER).trim();
  return {
    title,
    path: `/pages/activity-detail/index?activityId=${activityId}`,
    imageUrl,
  };
}

/**
 * 详情页底部主按钮状态机：根据角色与报名状态决定 CTA。
 */
function resolveCtaState(ctx) {
  const { operatorRole, canRegister, canCancel, registrationStatusText, i18n, activity } = ctx;
  if (!activity) {
    return {
      ctaMode: "disabled",
      ctaText: i18n.ctaUnavailable,
      ctaHint: i18n.loadFailed,
    };
  }
  if (operatorRole !== "STUDENT") {
    return {
      ctaMode: "disabled",
      ctaText: i18n.studentOnlyShort,
      ctaHint: i18n.studentOnly,
    };
  }
  if (canCancel) {
    return {
      ctaMode: "cancel",
      ctaText: i18n.cancelRegister,
      ctaHint: `${i18n.currentStatus}: ${registrationStatusText}`,
    };
  }
  if (canRegister) {
    return {
      ctaMode: "register",
      ctaText: i18n.registerNow,
      ctaHint: `${i18n.deadlineLabel}: ${activity.deadlineDisplay}`,
    };
  }
  return {
    ctaMode: "disabled",
    ctaText: i18n.ctaUnavailable,
    ctaHint: `${i18n.statusLabel}: ${activity.statusText}`,
  };
}

Page({
  data: {
    i18n: {
      navTitle: "活动详情",
      loading: "加载中...",
      statusLabel: "活动状态",
      locationLabel: "活动地点",
      timeLabel: "活动时间",
      deadlineLabel: "报名截止",
      summaryLabel: "活动简介",
      contentLabel: "活动详情",
      studentOnly: "仅学生可报名或取消报名",
      studentOnlyShort: "学生专属",
      currentStatus: "报名状态",
      registerNow: "马上报名",
      cancelRegister: "取消报名",
      ctaUnavailable: "暂不可报名",
      untitled: "未命名活动",
      emptyText: "暂无信息",
      summaryEmpty: "暂无活动简介，待发布者补充。",
      contentEmpty: "暂无活动详情，待发布者补充。",
      loadFailed: "活动信息加载失败，请稍后重试",
      registerSuccess: "报名成功",
      cancelSuccess: "已取消报名",
      registerRecordMissing: "未找到可取消的报名记录",
      favorite: "收藏",
      favorited: "已收藏",
      favoriteSuccess: "已收藏活动",
      unfavoriteSuccess: "已取消收藏",
      favoriteLoginHint: "请先登录后再收藏活动",
      share: "分享",
      shareTitleFallback: "校园活动",
      viewTicket: "查看电子票",
      ticketHint: "报名成功后可查看二维码凭证",
      service: "客服",
      serviceTip: "客服功能建设中",
      actionTodo: "功能建设中",
    },
    activityId: 0,
    loading: false,
    activity: null,
    operatorRole: "",
    currentRegistration: null,
    canRegister: false,
    canCancel: false,
    canViewTicket: false,
    navSafeHeightPx: 20,
    coverDisplay: DEFAULT_COVER,
    registrationStatusText: "未报名",
    ctaMode: "disabled",
    ctaText: "暂不可报名",
    ctaHint: "活动信息加载失败，请稍后重试",
    favorited: false,
    favoriteLoading: false,
  },

  onLoad(options) {
    this.initNavSafeArea();
    this.initShareMenu();
    this.setData({ activityId: Number(options.activityId || 0) });
  },

  initNavSafeArea() {
    let navSafeHeightPx = 20;
    try {
      const info = typeof wx.getWindowInfo === "function" ? wx.getWindowInfo() : wx.getSystemInfoSync();
      navSafeHeightPx = Number(info.statusBarHeight) > 0 ? Number(info.statusBarHeight) : 20;
    } catch (e) {}
    this.setData({ navSafeHeightPx });
  },

  initShareMenu() {
    try {
      if (typeof wx.showShareMenu === "function") {
        wx.showShareMenu({
          menus: ["shareAppMessage", "shareTimeline"],
        });
      }
    } catch (e) {}
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  onShow() {
    this.loadAll();
  },

  onShareAppMessage() {
    return buildSharePayload(this.data);
  },

  onShareTimeline() {
    const payload = buildSharePayload(this.data);
    return {
      title: payload.title,
      query: `activityId=${this.data.activityId}`,
      imageUrl: payload.imageUrl,
    };
  },

  /**
   * 聚合更新页面展示状态，避免各请求分支重复 setData。
   */
  refreshPresentationState() {
    const { activity, currentRegistration, canRegister, canCancel, operatorRole, i18n } = this.data;
    const registrationStatusText = normalizeRegistrationStatus(currentRegistration);
    const coverDisplay = activity ? activity.coverDisplay || DEFAULT_COVER : DEFAULT_COVER;
    const cta = resolveCtaState({
      operatorRole,
      canRegister,
      canCancel,
      registrationStatusText,
      i18n,
      activity,
    });

    this.setData({
      coverDisplay,
      registrationStatusText,
      ctaMode: cta.ctaMode,
      ctaText: cta.ctaText,
      ctaHint: cta.ctaHint,
    });
  },

  async loadAll() {
    const { operatorRole } = getOperatorContext();
    this.setData({ loading: true, operatorRole: operatorRole || "" });
    try {
      await this.loadDetail();
      if (operatorRole === "STUDENT") {
        await this.loadRegistrationState();
      } else {
        this.setData({ currentRegistration: null, canRegister: false, canCancel: false, canViewTicket: false });
        this.refreshPresentationState();
      }

      if (hasOperatorContext()) {
        await this.loadFavoriteState();
      } else {
        this.setData({ favorited: false });
      }
    } catch (e) {
      this.setData({
        activity: null,
        currentRegistration: null,
        canRegister: false,
        canCancel: false,
        canViewTicket: false,
        favorited: false,
      });
      this.refreshPresentationState();
      feedback.error(this.data.i18n.loadFailed);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadDetail() {
    const detail = await getPublicActivityDetail(this.data.activityId);
    const activity = {
      ...detail,
      startDisplay: displayTime(detail.startTime),
      endDisplay: displayTime(detail.endTime),
      deadlineDisplay: displayTime(detail.registrationDeadline),
      statusText: normalizeActivityStatus(detail.status),
      coverDisplay: detail.coverUrl || detail.cover || DEFAULT_COVER,
    };
    this.setData({ activity });
    this.refreshPresentationState();
  },

  /**
   * 查询当前活动报名态，并推导可报名/可取消状态。
   */
  async loadRegistrationState() {
    const rows = await getMyRegistrations();
    const currentRegistration = (rows || []).find((item) => Number(item.activityId) === this.data.activityId) || null;
    const actions = deriveRegistrationActions(this.data.activity, currentRegistration);
    this.setData({
      currentRegistration,
      canRegister: actions.canRegister,
      canCancel: actions.canCancel,
      canViewTicket: actions.canViewTicket,
    });
    this.refreshPresentationState();
  },

  async loadFavoriteState() {
    const state = await getActivityFavoriteStatus(this.data.activityId);
    this.setData({ favorited: Boolean(state && state.favorited) });
  },

  async onRegisterTap() {
    try {
      await registerActivity(this.data.activityId, "miniapp register");
      feedback.success(this.data.i18n.registerSuccess);
      await this.loadRegistrationState();
    } catch (e) {}
  },

  async onCancelTap() {
    const registration = this.data.currentRegistration;
    if (!registration || !registration.registrationId) {
      feedback.error(this.data.i18n.registerRecordMissing);
      return;
    }
    try {
      await cancelRegistration(registration.registrationId, "miniapp cancel");
      feedback.success(this.data.i18n.cancelSuccess);
      await this.loadRegistrationState();
    } catch (e) {}
  },

  async onFavoriteTap() {
    if (this.data.favoriteLoading) {
      return;
    }
    if (!hasOperatorContext()) {
      feedback.info(this.data.i18n.favoriteLoginHint);
      this.goToAuth();
      return;
    }

    this.setData({ favoriteLoading: true });
    try {
      if (this.data.favorited) {
        await unfavoriteActivity(this.data.activityId);
        this.setData({ favorited: false });
        feedback.success(this.data.i18n.unfavoriteSuccess);
      } else {
        await favoriteActivity(this.data.activityId);
        this.setData({ favorited: true });
        feedback.success(this.data.i18n.favoriteSuccess);
      }
    } catch (e) {
    } finally {
      this.setData({ favoriteLoading: false });
    }
  },

  onCtaTap() {
    if (this.data.ctaMode === "register") {
      this.onRegisterTap();
      return;
    }
    if (this.data.ctaMode === "cancel") {
      this.onCancelTap();
      return;
    }
    feedback.info(this.data.ctaHint || this.data.i18n.actionTodo);
  },

  onQuickActionTap(event) {
    const { action } = event.currentTarget.dataset;
    if (action === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (action === "favorite") {
      this.onFavoriteTap();
      return;
    }
    if (action === "service") {
      feedback.info(this.data.i18n.serviceTip);
      return;
    }
    feedback.info(this.data.i18n.actionTodo);
  },

  onViewTicketTap() {
    const registration = this.data.currentRegistration;
    if (!registration || !registration.registrationId) {
      feedback.error("电子票信息缺失");
      return;
    }
    wx.navigateTo({
      url: `/pages/ticket/index?registrationId=${registration.registrationId}`,
    });
  },

  goToAuth() {
    wx.navigateTo({ url: "/pages/auth/index?mode=login" });
  },
});
