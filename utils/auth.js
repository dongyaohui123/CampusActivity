const { setOperatorContext, clearOperatorContext } = require("./operator-context");
const { normalizeAvatarUrl } = require("./avatar-url");

const LOGIN_USER_KEY = "login_user";

let memoryLoginUser = null;

function canUseWxStorage() {
  return typeof wx !== "undefined" && typeof wx.getStorageSync === "function" && typeof wx.setStorageSync === "function";
}

/**
 * 统一登录用户结构，保证 id/role 可用。
 */
function normalizeLoginUser(user) {
  if (!user || typeof user !== "object") {
    return null;
  }
  const id = Number(user.id);
  const role = String(user.role || "").toUpperCase();
  if (!Number.isFinite(id) || id <= 0 || !role) {
    return null;
  }
  return {
    id,
    username: user.username || "",
    nickname: user.nickname || "",
    role,
    status: user.status || "",
    avatarUrl: normalizeAvatarUrl(user.avatarUrl),
    phone: user.phone || "",
  };
}

function getLoginUser() {
  if (canUseWxStorage()) {
    const stored = wx.getStorageSync(LOGIN_USER_KEY);
    const normalized = normalizeLoginUser(stored);
    if (normalized) {
      memoryLoginUser = normalized;
      return { ...memoryLoginUser };
    }
  }
  return memoryLoginUser ? { ...memoryLoginUser } : null;
}

/**
 * 登录成功后同步两个上下文：
 * 1) login_user（页面展示）
 * 2) operator_context（接口鉴权 query 参数）
 */
function setLoginUser(user) {
  const normalized = normalizeLoginUser(user);
  if (!normalized) {
    return null;
  }
  memoryLoginUser = normalized;
  if (canUseWxStorage()) {
    wx.setStorageSync(LOGIN_USER_KEY, normalized);
  }
  setOperatorContext({
    operatorUserId: normalized.id,
    operatorRole: normalized.role,
  });
  return { ...normalized };
}

function clearLoginUser() {
  memoryLoginUser = null;
  if (canUseWxStorage()) {
    wx.removeStorageSync(LOGIN_USER_KEY);
  }
  clearOperatorContext();
}

function isLoggedIn() {
  return Boolean(getLoginUser());
}

module.exports = {
  getLoginUser,
  setLoginUser,
  clearLoginUser,
  isLoggedIn,
};
