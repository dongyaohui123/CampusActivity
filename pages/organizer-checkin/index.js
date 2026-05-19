const {
  listManageableActivities,
  listOrganizerActivityRegistrations,
  organizerCheckIn,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

function displayTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function mapStatusText(status) {
  if (status === "REGISTERED") return "待核销";
  if (status === "CHECKED_IN") return "已签到";
  if (status === "CANCELLED") return "已取消";
  return status || "状态未知";
}

function parseTicketCode(raw, activityId) {
  const text = String(raw || "").trim();
  if (!text) {
    return "";
  }
  if (!text.startsWith("CA-TICKET|")) {
    return text;
  }
  const parts = text.split("|");
  if (parts.length !== 3) {
    throw new Error("二维码格式不正确");
  }
  const qrActivityId = Number(parts[1]);
  if (Number.isFinite(qrActivityId) && qrActivityId > 0 && qrActivityId !== Number(activityId)) {
    throw new Error("该二维码不属于当前活动");
  }
  return String(parts[2] || "").trim();
}

function normalizeRegistrations(list) {
  return (list || []).map((item) => ({
    ...item,
    statusText: mapStatusText(item.status),
    registeredDisplay: displayTime(item.registeredAt),
    checkinDisplay: displayTime(item.checkinAt),
  }));
}

function buildStats(registrations) {
  const activeCount = registrations.filter((item) => item.status === "REGISTERED" || item.status === "CHECKED_IN").length;
  const checkedInCount = registrations.filter((item) => item.status === "CHECKED_IN").length;
  const pendingCount = registrations.filter((item) => item.status === "REGISTERED").length;
  return { activeCount, checkedInCount, pendingCount };
}

Page({
  data: {
    i18n: {
      navTitle: "扫码签到",
      loading: "加载中...",
      roleHint: "组织者或活动管理员可进行扫码签到",
      noCheckinPermission: "当前活动未授权签到",
      scanNow: "立即扫码",
      manualLabel: "手动输入票码",
      manualPlaceholder: "支持直接输入票码或扫描二维码内容",
      manualSubmit: "提交核销",
      activityLabel: "活动信息",
      timeLabel: "活动时间",
      locationLabel: "活动地点",
      activeCount: "有效报名",
      checkedInCount: "已签到",
      pendingCount: "待核销",
      registrationList: "报名名单",
      empty: "暂无报名记录",
      unknownActivity: "未知活动",
    },
    operatorRole: "",
    activityId: 0,
    loading: false,
    activity: null,
    registrations: [],
    stats: {
      activeCount: 0,
      checkedInCount: 0,
      pendingCount: 0,
    },
    manualTicketCode: "",
    canCheckIn: false,
  },

  onLoad(options) {
    this.setData({ activityId: Number(options.activityId || 0) });
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole: operatorRole || "" });
    if (this.data.activityId) {
      this.loadAll();
    }
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadAll() {
    this.setData({ loading: true });
    try {
      const [activities, registrations] = await Promise.all([
        listManageableActivities({}),
        listOrganizerActivityRegistrations(this.data.activityId),
      ]);
      const activity =
        (activities || []).find((item) => Number(item.id) === Number(this.data.activityId)) || {
          id: this.data.activityId,
          title: this.data.i18n.unknownActivity,
          location: "-",
          startTime: "",
          endTime: "",
          canCheckIn: false,
        };
      const normalizedRegistrations = normalizeRegistrations(registrations);
      this.setData({
        activity: {
          ...activity,
          startDisplay: displayTime(activity.startTime),
          endDisplay: displayTime(activity.endTime),
        },
        canCheckIn: Boolean(activity.canCheckIn),
        registrations: normalizedRegistrations,
        stats: buildStats(normalizedRegistrations),
      });
    } catch (e) {
      this.setData({
        activity: null,
        canCheckIn: false,
        registrations: [],
        stats: { activeCount: 0, checkedInCount: 0, pendingCount: 0 },
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  onManualTicketInput(event) {
    this.setData({ manualTicketCode: event.detail || "" });
  },

  onScanTap() {
    if (!this.data.canCheckIn) {
      feedback.error(this.data.i18n.noCheckinPermission);
      return;
    }
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ["qrCode"],
      success: async (res) => {
        await this.submitTicketCode(res.result);
      },
      fail: (err) => {
        const raw = String((err && err.errMsg) || "");
        if (!/cancel/i.test(raw)) {
          feedback.error("扫码失败，请重试");
        }
      },
    });
  },

  async onManualSubmitTap() {
    if (!this.data.canCheckIn) {
      feedback.error(this.data.i18n.noCheckinPermission);
      return;
    }
    await this.submitTicketCode(this.data.manualTicketCode);
  },

  async submitTicketCode(rawTicket) {
    let ticketCode = "";
    try {
      ticketCode = parseTicketCode(rawTicket, this.data.activityId);
    } catch (e) {
      feedback.error(e.message || "二维码格式不正确");
      return;
    }
    if (!ticketCode) {
      feedback.error("请输入有效票码");
      return;
    }

    try {
      const result = await organizerCheckIn(this.data.activityId, ticketCode);
      feedback.success(`${result.nickname || "参与者"} 签到成功`);
      this.setData({ manualTicketCode: "" });
      await this.loadAll();
    } catch (e) {}
  },
});
