const Toast = require("@vant/weapp/toast/toast");
const Notify = require("@vant/weapp/notify/notify");

const TOAST_SELECTOR = "#van-toast";
const NOTIFY_SELECTOR = "#van-notify";

function normalizeMessage(message, fallback) {
  return String(message || fallback || "");
}

/**
 * Vant 组件不可用时的兜底提示。
 */
function fallbackToast(message, icon = "none", duration = 1800) {
  if (typeof wx !== "undefined" && typeof wx.showToast === "function") {
    wx.showToast({
      title: normalizeMessage(message, "提示"),
      icon,
      duration,
    });
  }
}

function success(message, options = {}) {
  const text = normalizeMessage(message, "操作成功");
  try {
    Toast.success({
      message: text,
      duration: Number(options.duration) > 0 ? Number(options.duration) : 1600,
      selector: TOAST_SELECTOR,
    });
  } catch (e) {
    fallbackToast(text, "success", 1600);
  }
}

function info(message, options = {}) {
  const text = normalizeMessage(message, "提示");
  try {
    Toast({
      message: text,
      icon: "none",
      duration: Number(options.duration) > 0 ? Number(options.duration) : 1800,
      selector: TOAST_SELECTOR,
    });
  } catch (e) {
    fallbackToast(text, "none", 1800);
  }
}

function error(message, options = {}) {
  const text = normalizeMessage(message, "请求失败");
  try {
    Notify({
      message: text,
      type: options.type || "danger",
      duration: Number(options.duration) > 0 ? Number(options.duration) : 2200,
      selector: NOTIFY_SELECTOR,
    });
  } catch (e) {
    fallbackToast(text, "none", 2200);
  }
}

module.exports = {
  success,
  info,
  error,
};
