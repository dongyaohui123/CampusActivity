const { login, getMyRegistrations, listOrganizerActivities } = require("../../utils/api");
const { getLoginUser, setLoginUser, clearLoginUser } = require("../../utils/auth");

function formatCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

Page({
  data: {
    loginUser: null,
    username: "",
    password: "",
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
    } else {
      this.setData({
        stats: {
          participatedCount: 0,
          initiatedCount: 0,
          managedOrgCount: 0,
        },
      });
    }
  },

  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [field]: event.detail.value,
    });
  },

  async onLoginTap() {
    const username = String(this.data.username || "").trim();
    const password = String(this.data.password || "");
    if (!username || !password) {
      wx.showToast({ title: "请输入用户名和密码", icon: "none" });
      return;
    }

    this.setData({ loading: true });
    try {
      const user = await login(username, password);
      const loginUser = setLoginUser(user);
      this.setData({
        loginUser,
        password: "",
      });
      wx.showToast({ title: "登录成功", icon: "success" });
      await this.loadStats();
    } catch (e) {
      // request.js already shows toast; avoid unhandled promise errors.
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
      stats: {
        participatedCount: 0,
        initiatedCount: 0,
        managedOrgCount: 0,
      },
    });
    wx.showToast({ title: "已退出登录", icon: "success" });
  },

  async loadStats() {
    const loginUser = this.data.loginUser;
    if (!loginUser) {
      return;
    }
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
      wx.showToast({ title: "请先登录", icon: "none" });
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
      return;
    }
    if (action === "myOrg") {
      if (loginUser.role !== "ORGANIZER") {
        wx.showToast({ title: "仅组织者可查看", icon: "none" });
        return;
      }
      wx.navigateTo({ url: "/pages/organizer-activities/index" });
      return;
    }
    if (action === "review") {
      if (loginUser.role !== "ADMIN") {
        wx.showToast({ title: "仅管理员可使用", icon: "none" });
        return;
      }
      wx.navigateTo({ url: "/pages/admin-review/index" });
      return;
    }
    if (action === "help") {
      wx.showToast({ title: "帮助中心开发中", icon: "none" });
      return;
    }
    if (action === "contact") {
      wx.showToast({ title: "联系客服：400-000-0000", icon: "none" });
    }
  },

  onBottomTabChange(event) {
    const tab = event.detail.tab;
    if (tab === "home") {
      wx.reLaunch({ url: "/pages/index/index" });
      return;
    }
    if (tab === "find") {
      wx.reLaunch({ url: "/pages/activity-list/index" });
      return;
    }
    if (tab === "mine") {
      return;
    }
  },
});
