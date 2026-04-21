const {
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

const DEFAULT_COVER = "https://picsum.photos/900/506?random=18";

const ACTIVITY_STATUS_MAP = {
  DRAFT: "草稿",
  PUBLISHED: "进行中",
  CLOSED: "已结束",
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
    ctaHint: `${i18n.currentStatus}: ${registrationStatusText}`,
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
      registerNow: "马上预约",
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
      share: "分享",
      service: "客服",
      actionTodo: "功能建设中",
    },
    activityId: 0,
    loading: false,
    activity: null,
    operatorRole: "STUDENT",
    currentRegistration: null,
    canRegister: false,
    canCancel: false,
    coverDisplay: DEFAULT_COVER,
    registrationStatusText: "未报名",
    ctaMode: "disabled",
    ctaText: "暂不可报名",
    ctaHint: "活动信息加载失败，请稍后重试",
  },

  onLoad(options) {
    this.setData({ activityId: Number(options.activityId || 0) });
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  onShow() {
    this.loadAll();
  },

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
    this.setData({ loading: true, operatorRole });
    try {
      await this.loadDetail();
      if (operatorRole === "STUDENT") {
        await this.loadRegistrationState();
      } else {
        this.setData({ currentRegistration: null, canRegister: false, canCancel: false });
        this.refreshPresentationState();
      }
    } catch (e) {
      this.setData({ activity: null, currentRegistration: null, canRegister: false, canCancel: false });
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

  async loadRegistrationState() {
    const rows = await getMyRegistrations();
    const currentRegistration = (rows || []).find((item) => Number(item.activityId) === this.data.activityId) || null;
    const canRegister = !currentRegistration || currentRegistration.registrationStatus === "CANCELLED";
    const canCancel = Boolean(
      currentRegistration &&
        (currentRegistration.registrationStatus === "REGISTERED" ||
          currentRegistration.registrationStatus === "CHECKED_IN")
    );
    this.setData({ currentRegistration, canRegister, canCancel });
    this.refreshPresentationState();
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
    if (action === "share") {
      feedback.info("请使用右上角分享给好友");
      return;
    }
    feedback.info(this.data.i18n.actionTodo);
  },
});
