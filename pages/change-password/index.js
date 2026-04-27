const { changePassword } = require("../../utils/api");
const { getLoginUser, clearLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

Page({
  data: {
    i18n: {
      navTitle: "修改密码",
      pageTitle: "更新登录密码",
      pageDesc: "修改成功后将退出当前账号，请使用新密码重新登录。",
      oldPasswordPlaceholder: "请输入旧密码",
      newPasswordPlaceholder: "请输入新密码",
      confirmPasswordPlaceholder: "请再次输入新密码",
      submitBtn: "确认修改",
      securityTitle: "安全提示",
      securityTips: "为保证账号安全，请勿将密码告知他人。",
    },
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
    showOldPassword: false,
    showNewPassword: false,
    showConfirmPassword: false,
    loading: false,
  },

  onLoad() {
    const loginUser = getLoginUser();
    if (loginUser) {
      return;
    }
    feedback.info("请先登录");
    setTimeout(() => {
      wx.reLaunch({ url: "/pages/auth/index?mode=login" });
    }, 400);
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

    const loginUser = getLoginUser();
    if (!loginUser) {
      feedback.info("请先登录");
      wx.reLaunch({ url: "/pages/auth/index?mode=login" });
      return;
    }

    const oldPassword = String(this.data.oldPassword || "");
    const newPassword = String(this.data.newPassword || "");
    const confirmPassword = String(this.data.confirmPassword || "");

    if (!oldPassword || !newPassword || !confirmPassword) {
      feedback.error("请完整填写旧密码、新密码和确认密码");
      return;
    }
    if (newPassword !== confirmPassword) {
      feedback.error("两次输入的新密码不一致");
      return;
    }
    if (oldPassword === newPassword) {
      feedback.error("新密码不能与旧密码相同");
      return;
    }

    this.setData({ loading: true });
    try {
      await changePassword(loginUser.id, oldPassword, newPassword);
      clearLoginUser();
      feedback.success("密码修改成功，请重新登录");
      setTimeout(() => {
        wx.reLaunch({ url: "/pages/auth/index?mode=login" });
      }, 900);
    } catch (e) {
      // request 层已统一提示错误
    } finally {
      this.setData({
        loading: false,
        oldPassword: "",
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
    wx.reLaunch({ url: "/pages/mine/index" });
  },
});
