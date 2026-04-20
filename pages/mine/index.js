const { getMyRegistrations, listOrganizerActivities } = require("../../utils/api");
const { getLoginUser, clearLoginUser } = require("../../utils/auth");
const feedback = require("../../utils/feedback");

function formatCount(value) {
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function getAvatarText(loginUser) {
  if (!loginUser) return "访客";
  const base = String(loginUser.nickname || loginUser.username || "用户").trim();
  return base.slice(0, 2) || "用户";
}

function getGuestQuickEntries(i18n) {
  return {
    primary: {
      title: i18n.goLogin,
      desc: i18n.goLoginDesc,
      action: "login",
    },
    secondary: {
      title: i18n.helpCenter,
      desc: i18n.helpDesc,
      action: "help",
    },
  };
}

function getLoginQuickEntries(i18n, loginUser) {
  const secondary =
    loginUser && loginUser.role === "ADMIN"
      ? { title: i18n.reviewManage, desc: i18n.reviewDesc, action: "review" }
      : { title: i18n.myOrg, desc: i18n.myOrgDesc, action: "myOrg" };
  return {
    primary: {
      title: i18n.myActivity,
      desc: i18n.myActivityDesc,
      action: "myActivity",
    },
    secondary,
  };
}

Page({
  data: {
    i18n: {
      navTitle: "我的",
      guestName: "未登录用户",
      guestDesc: "你还未登录，请前往登录页完成认证",
      welcomeBack: "欢迎回来，开始你的校园活动管理",
      setting: "设置",
      goLogin: "去登录",
      goLoginDesc: "登录后查看我的活动记录",
      participatedCount: "参与活动数",
      initiatedCount: "发起活动数",
      managedOrgCount: "管理组织数",
      myActivity: "我的活动",
      myActivityDesc: "查看活动报名与状态",
      myOrg: "我的组织",
      myOrgDesc: "管理组织与发起活动",
      reviewManage: "审核管理",
      reviewDesc: "处理活动审核任务",
      helpCenter: "帮助中心",
      helpDesc: "常见问题与使用指引",
      contact: "联系客服",
      logout: "退出登录",
    },
    loginUser: null,
    avatarText: "访客",
    quickPrimary: {
      title: "去登录",
      desc: "登录后查看我的活动记录",
      action: "login",
    },
    quickSecondary: {
      title: "帮助中心",
      desc: "常见问题与使用指引",
      action: "help",
    },
    bottomActive: "mine",
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
    const { i18n } = this.data;
    const quickEntries = loginUser ? getLoginQuickEntries(i18n, loginUser) : getGuestQuickEntries(i18n);
    this.setData({
      loginUser,
      avatarText: getAvatarText(loginUser),
      quickPrimary: quickEntries.primary,
      quickSecondary: quickEntries.secondary,
    });
    if (loginUser) {
      this.loadStats();
      return;
    }
    this.setData({ stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 } });
  },

  onQuickActionTap(event) {
    const action = event.currentTarget.dataset.action;
    this.handleAction(action);
  },

  onMenuTap(event) {
    const action = event.currentTarget.dataset.action;
    this.handleAction(action);
  },

  handleAction(action) {
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

    if (!loginUser) {
      this.goToAuth();
      return;
    }

    if (action === "myActivity") {
      wx.navigateTo({ url: "/pages/my-registrations/index" });
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
    const quickEntries = getGuestQuickEntries(i18n);
    this.setData({
      loginUser: null,
      avatarText: "访客",
      quickPrimary: quickEntries.primary,
      quickSecondary: quickEntries.secondary,
      stats: { participatedCount: 0, initiatedCount: 0, managedOrgCount: 0 },
    });
    feedback.success("已退出登录");
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
