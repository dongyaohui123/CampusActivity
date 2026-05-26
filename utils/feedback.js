const MODAL_SELECTOR = "#custom-modal";

function normalizeMessage(message, fallback) {
  return String(message || fallback || "");
}

function getContext() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}

function showModal(options) {
  const context = getContext();
  if (!context) return;
  const modal = context.selectComponent(MODAL_SELECTOR);
  if (modal) {
    modal.show(options);
  } else {
    console.warn("未找到 custom-modal 节点，请确认页面是否已引入组件");
  }
}

function success(message, options = {}) {
  showModal({
    type: "success",
    message: normalizeMessage(message, "操作成功"),
    duration: Number(options.duration) > 0 ? Number(options.duration) : 1600,
    ...options,
  });
}

function info(message, options = {}) {
  showModal({
    type: "info",
    message: normalizeMessage(message, "提示"),
    duration: Number(options.duration) > 0 ? Number(options.duration) : 1800,
    ...options,
  });
}

function error(message, options = {}) {
  showModal({
    type: "error",
    message: normalizeMessage(message, "请求失败"),
    ...options,
  });
}

function warning(message, options = {}) {
  showModal({
    type: "warning",
    message: normalizeMessage(message, "注意"),
    ...options,
  });
}

function confirm(message, options = {}) {
  return new Promise((resolve, reject) => {
    if (typeof wx === "undefined" || typeof wx.showModal !== "function") {
      reject(new Error("wx.showModal is not available"));
      return;
    }
    wx.showModal({
      title: options.title || "确认",
      content: normalizeMessage(message, "确定执行此操作？"),
      showCancel: options.showCancel !== false,
      cancelText: options.cancelText || "取消",
      confirmText: options.confirmText || "确定",
      confirmColor: "#5062f6",
      success: (res) => {
        resolve(!!res.confirm);
      },
      fail: reject,
    });
  });
}

module.exports = {
  success,
  info,
  error,
  warning,
  confirm,
};
