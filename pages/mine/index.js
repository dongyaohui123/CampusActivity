const { login, registerUser, getMyRegistrations, listOrganizerActivities } = require("../../utils/api");
const { getLoginUser, setLoginUser, clearLoginUser } = require("../../utils/auth");

function formatCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

Page({
  data: {
    i18n: {
      navTitle: "\u6211\u7684",
      login: "\u767b\u5f55",
      register: "\u6ce8\u518c",
      username: "\u7528\u6237\u540d",
      password: "\u5bc6\u7801",
      nickname: "\u6635\u79f0",
      registerAndLogin: "\u6ce8\u518c\u5e76\u767b\u5f55",
      wechatLater: "\u540e\u7eed\u53ef\u63a5\u5165\u5fae\u4fe1\u767b\u5f55",
      participatedCount: "\u53c2\u4e0e\u6d3b\u52a8\u6570",
      initiatedCount: "\u53d1\u8d77\u6d3b\u52a8\u6570",
      managedOrgCount: "\u7ba1\u7406\u7ec4\u7ec7\u6570",
      myActivity: "\u6211\u7684\u6d3b\u52a8",
      myOrg: "\u6211\u7684\u7ec4\u7ec7",
      reviewManage: "\u5ba1\u6838\u7ba1\u7406",
      helpCenter: "\u5e2e\u52a9\u4e2d\u5fc3",
      contact: "\u8054\u7cfb\u5ba2\u670d",
      logout: "\u9000\u51fa\u767b\u5f55",
    },
    loginUser: null,
    authMode: "login",
    username: "",
    password: "",
    registerNickname: "",
    loading: false,
    stats: {
      participatedCount: 0,
      initiatedCount: 0,
      managedOrgCount: 0,
    },
  },

  onShow() {
    this.restoreLoginState();
  },

  restoreLoginState() {
    const loginUser = getLoginUser();
    this.setData({ loginUser });
    if (loginUser) {
      this.loadStats();
      return;
    }
    this.setData({ stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 } });
  },

  onAuthModeChange(event) {
    this.setData({ authMode: event.currentTarget.dataset.mode });
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [field]: event.detail.value });
  },

  async onLoginTap() {
    const username = String(this.data.username || "").trim();
    const password = String(this.data.password || "");
    if (!username || !password) {
      wx.showToast({ title: "\u8bf7\u8f93\u5165\u7528\u6237\u540d\u548c\u5bc6\u7801", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      const user = await login(username, password);
      const loginUser = setLoginUser(user);
      this.setData({ loginUser, password: "" });
      wx.showToast({ title: "\u767b\u5f55\u6210\u529f", icon: "success" });
      await this.loadStats();
    } finally {
      this.setData({ loading: false });
    }
  },

  async onRegisterTap() {
    const username = String(this.data.username || "").trim();
    const password = String(this.data.password || "");
    const nickname = String(this.data.registerNickname || "").trim();
    if (!username || !password || !nickname) {
      wx.showToast({ title: "\u8bf7\u5b8c\u6574\u586b\u5199\u7528\u6237\u540d\u3001\u5bc6\u7801\u3001\u6635\u79f0", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    try {
      const user = await registerUser(username, password, nickname);
      const loginUser = setLoginUser(user);
      this.setData({ loginUser, password: "", registerNickname: "", authMode: "login" });
      wx.showToast({ title: "\u6ce8\u518c\u6210\u529f\u5e76\u5df2\u767b\u5f55", icon: "success" });
      await this.loadStats();
    } finally {
      this.setData({ loading: false });
    }
  },

  onLogoutTap() {
    clearLoginUser();
    this.setData({
      loginUser: null,
      username: "",
      password: "",
      registerNickname: "",
      stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 },
    });
    wx.showToast({ title: "\u5df2\u9000\u51fa\u767b\u5f55", icon: "success" });
  },

  async loadStats() {
    const loginUser = this.data.loginUser;
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

  onMenuTap(event) {
    const action = event.currentTarget.dataset.action;
    const loginUser = this.data.loginUser;
    if (!loginUser) {
      wx.showToast({ title: "\u8bf7\u5148\u767b\u5f55", icon: "none" });
      return;
    }
    if (action === "myActivity") return wx.navigateTo({ url: "/pages/my-registrations/index" });
    if (action === "myOrg") {
      if (loginUser.role !== "ORGANIZER") return wx.showToast({ title: "\u4ec5\u7ec4\u7ec7\u8005\u53ef\u67e5\u770b", icon: "none" });
      return wx.navigateTo({ url: "/pages/organizer-activities/index" });
    }
    if (action === "review") {
      if (loginUser.role !== "ADMIN") return wx.showToast({ title: "\u4ec5\u7ba1\u7406\u5458\u53ef\u4f7f\u7528", icon: "none" });
      return wx.navigateTo({ url: "/pages/admin-review/index" });
    }
    if (action === "help") return wx.showToast({ title: "\u5e2e\u52a9\u4e2d\u5fc3\u5f00\u53d1\u4e2d", icon: "none" });
    if (action === "contact") return wx.showToast({ title: "\u8054\u7cfb\u5ba2\u670d\uff1a400-000-0000", icon: "none" });
  },

  onBottomTabChange(event) {
    const tab = event.detail.tab;
    if (tab === "home") return wx.reLaunch({ url: "/pages/index/index" });
    if (tab === "find") return wx.reLaunch({ url: "/pages/activity-list/index" });
  },
});