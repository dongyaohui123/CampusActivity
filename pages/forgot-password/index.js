const { sendSmsCode, resetPassword } = require("../../utils/api");
const feedback = require("../../utils/feedback");

const PHONE_REGEX = /^1\d{10}$/;
const COUNTDOWN_SECONDS = 60;

Page({
  data: {
    username: "",
    phone: "",
    code: "",
    newPassword: "",
    confirmPassword: "",
    showNewPassword: false,
    showConfirmPassword: false,
    loading: false,
    sendingCode: false,
    countdown: 0,
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [field]: event.detail.value });
  },

  onTogglePassword(event) {
    const target = event.currentTarget.dataset.target;
    if (!target) {
      return;
    }
    this.setData({ [target]: !this.data[target] });
  },

  async onSendCodeTap() {
    if (this.data.sendingCode || this.data.countdown > 0) {
      return;
    }

    const phone = String(this.data.phone || "").trim();
    if (!phone) {
      feedback.error("请先输入手机号");
      return;
    }
    if (!PHONE_REGEX.test(phone)) {
      feedback.error("请输入正确的11位手机号");
      return;
    }

    this.setData({ sendingCode: true });
    try {
      await sendSmsCode(phone);
      feedback.success("验证码已发送");
      this.startCountdown();
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({ sendingCode: false });
    }
  },

  startCountdown() {
    this.setData({ countdown: COUNTDOWN_SECONDS });
    this._countdownTimer = setInterval(() => {
      const next = this.data.countdown - 1;
      if (next <= 0) {
        clearInterval(this._countdownTimer);
        this._countdownTimer = null;
        this.setData({ countdown: 0 });
      } else {
        this.setData({ countdown: next });
      }
    }, 1000);
  },

  onUnload() {
    if (this._countdownTimer) {
      clearInterval(this._countdownTimer);
      this._countdownTimer = null;
    }
  },

  async onSubmitTap() {
    if (this.data.loading) {
      return;
    }

    const username = String(this.data.username || "").trim();
    const phone = String(this.data.phone || "").trim();
    const code = String(this.data.code || "").trim();
    const newPassword = String(this.data.newPassword || "");
    const confirmPassword = String(this.data.confirmPassword || "");

    if (!username || !phone || !code || !newPassword || !confirmPassword) {
      feedback.error("请完整填写所有信息");
      return;
    }
    if (!PHONE_REGEX.test(phone)) {
      feedback.error("请输入正确的11位手机号");
      return;
    }
    if (newPassword !== confirmPassword) {
      feedback.error("两次输入的新密码不一致");
      return;
    }

    this.setData({ loading: true });
    try {
      await resetPassword(username, phone, newPassword, code);
      feedback.success("密码重置成功，请使用新密码登录");
      setTimeout(() => {
        wx.reLaunch({ url: "/pages/auth/index?mode=login" });
      }, 900);
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({
        loading: false,
        newPassword: "",
        confirmPassword: "",
      });
    }
  },

  onBackTap() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
      return;
    }
    wx.reLaunch({ url: "/pages/auth/index" });
  },
});
