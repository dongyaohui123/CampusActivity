const { resetPassword } = require("../../utils/api");
const feedback = require("../../utils/feedback");

const PHONE_REGEX = /^1\d{10}$/;

Page({
  data: {
    username: "",
    phone: "",
    newPassword: "",
    confirmPassword: "",
    showNewPassword: false,
    showConfirmPassword: false,
    loading: false,
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

  async onSubmitTap() {
    if (this.data.loading) {
      return;
    }

    const username = String(this.data.username || "").trim();
    const phone = String(this.data.phone || "").trim();
    const newPassword = String(this.data.newPassword || "");
    const confirmPassword = String(this.data.confirmPassword || "");

    if (!username || !phone || !newPassword || !confirmPassword) {
      feedback.error("请完整填写用户名、手机号和新密码");
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
      await resetPassword(username, phone, newPassword);
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
