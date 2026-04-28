const { listActivityManagers, addActivityManager, removeActivityManager } = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

function permissionText(permissions) {
  const list = Array.isArray(permissions) ? permissions : [];
  const hasView = list.includes("VIEW_REGISTRATIONS");
  const hasCheckin = list.includes("CHECK_IN");
  if (hasView && hasCheckin) {
    return "报名查看、扫码签到";
  }
  if (hasView) {
    return "报名查看";
  }
  if (hasCheckin) {
    return "扫码签到";
  }
  return "未授权";
}

Page({
  data: {
    i18n: {
      navTitle: "活动管理员",
      roleHint: "仅组织者可管理活动管理员",
      activityTitleLabel: "活动",
      userIdLabel: "管理员用户ID",
      userIdPlaceholder: "请输入用户ID",
      addButton: "添加管理员",
      managerList: "管理员列表",
      remove: "移除",
      empty: "暂无活动管理员",
      loading: "加载中...",
      permissionLabel: "权限",
      statusLabel: "状态",
      createdAtLabel: "添加时间",
    },
    operatorRole: "",
    activityId: 0,
    activityTitle: "",
    loading: false,
    managerUserIdInput: "",
    managers: [],
  },

  onLoad(options) {
    this.setData({
      activityId: Number(options.activityId || 0),
      activityTitle: decodeURIComponent(String(options.title || "")),
    });
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole: operatorRole || "" });
    if (operatorRole === "ORGANIZER" && this.data.activityId) {
      this.loadManagers();
    }
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  onUserIdInput(event) {
    this.setData({ managerUserIdInput: event.detail || "" });
  },

  async loadManagers() {
    this.setData({ loading: true });
    try {
      const list = await listActivityManagers(this.data.activityId);
      this.setData({
        managers: (list || []).map((item) => ({
          ...item,
          permissionText: permissionText(item.permissions),
          createdAtDisplay: String(item.createdAt || "").replace("T", " "),
        })),
      });
    } catch (e) {
      this.setData({ managers: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onAddTap() {
    const userId = Number(this.data.managerUserIdInput);
    if (!Number.isFinite(userId) || userId <= 0) {
      feedback.error("请输入有效用户ID");
      return;
    }
    try {
      await addActivityManager(this.data.activityId, {
        userId,
        permissions: ["VIEW_REGISTRATIONS", "CHECK_IN"],
      });
      feedback.success("管理员已添加");
      this.setData({ managerUserIdInput: "" });
      await this.loadManagers();
    } catch (e) {}
  },

  async onRemoveTap(event) {
    const managerUserId = Number(event.currentTarget.dataset.userId);
    if (!Number.isFinite(managerUserId) || managerUserId <= 0) {
      feedback.error("管理员信息无效");
      return;
    }
    try {
      await removeActivityManager(this.data.activityId, managerUserId);
      feedback.success("管理员已移除");
      await this.loadManagers();
    } catch (e) {}
  },
});
