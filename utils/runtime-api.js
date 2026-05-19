function getRuntimeApi(preferred) {
  if (preferred === "qq") {
    return typeof qq !== "undefined" ? qq : null;
  }
  if (preferred === "wx") {
    return typeof wx !== "undefined" ? wx : null;
  }
  if (typeof qq !== "undefined") {
    return qq;
  }
  if (typeof wx !== "undefined") {
    return wx;
  }
  return null;
}

function getStorageApi() {
  const api = getRuntimeApi();
  if (
    api
    && typeof api.getStorageSync === "function"
    && typeof api.setStorageSync === "function"
    && typeof api.removeStorageSync === "function"
  ) {
    return api;
  }
  return null;
}

module.exports = {
  getRuntimeApi,
  getStorageApi,
};
