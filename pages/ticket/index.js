const { getRegistrationTicket, getRegistrationTicketQrCodeUrl } = require("../../utils/api");
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

Page({
  data: {
    i18n: {
      navTitle: "电子票",
      loading: "加载中...",
      loadFailed: "电子票加载失败，请稍后重试",
      activityLabel: "活动",
      timeLabel: "时间",
      locationLabel: "地点",
      ticketCodeLabel: "票码",
      issuedAtLabel: "签发时间",
      checkinAtLabel: "签到时间",
      copyTicketCode: "复制票码",
      ticketTip: "入场时向组织者出示二维码，由组织者扫码完成签到。",
      checkedInTip: "该电子票已完成签到，无需重复核销。",
      empty: "暂无可用电子票",
    },
    registrationId: 0,
    loading: false,
    ticket: null,
    qrCodeUrl: "",
  },

  onLoad(options) {
    this.setData({ registrationId: Number(options.registrationId || 0) });
  },

  onShow() {
    if (!this.data.registrationId) {
      feedback.error("电子票参数缺失");
      return;
    }
    this.loadTicket();
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadTicket() {
    this.setData({ loading: true });
    try {
      const ticket = await getRegistrationTicket(this.data.registrationId);
      this.setData({
        ticket: {
          ...ticket,
          statusText: mapStatusText(ticket.registrationStatus),
          activityStartDisplay: displayTime(ticket.activityStartTime),
          activityEndDisplay: displayTime(ticket.activityEndTime),
          ticketIssuedDisplay: displayTime(ticket.ticketIssuedAt),
          checkinDisplay: displayTime(ticket.checkinAt),
        },
        qrCodeUrl: getRegistrationTicketQrCodeUrl(this.data.registrationId),
      });
    } catch (e) {
      this.setData({ ticket: null, qrCodeUrl: "" });
    } finally {
      this.setData({ loading: false });
    }
  },

  onCopyTicketCodeTap() {
    const code = this.data.ticket && this.data.ticket.ticketCode;
    if (!code) {
      feedback.error("票码缺失");
      return;
    }
    wx.setClipboardData({
      data: code,
      success: () => feedback.success("票码已复制"),
      fail: () => feedback.error("复制失败"),
    });
  },
});
