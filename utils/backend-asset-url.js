const { baseURL } = require("../config");

const BACKEND_STATIC_PREFIXES = ["/static/avatars", "/static/activity-covers"];

function stripTrailingSlash(value) {
  return String(value || "").replace(/\/+$/, "");
}

function isAbsoluteUrl(value) {
  return /^(https?:)?\/\//i.test(value);
}

function extractAbsolutePath(value) {
  const match = String(value || "").match(/^https?:\/\/[^/]+(\/.*)?$/i);
  return match && match[1] ? match[1] : "";
}

function isLoopbackUrl(value) {
  return /^https?:\/\/(?:127\.0\.0\.1|localhost)(?::\d+)?(?:\/|$)/i.test(String(value || "").trim());
}

function isBackendStaticPath(value) {
  const path = String(value || "").trim();
  return BACKEND_STATIC_PREFIXES.some((prefix) => path.startsWith(prefix));
}

function toCurrentBackendUrl(path) {
  return `${stripTrailingSlash(baseURL)}${path}`;
}

function normalizeBackendAssetUrl(assetUrl) {
  const value = String(assetUrl || "").trim();
  if (!value) {
    return "";
  }

  if (!isAbsoluteUrl(value)) {
    return isBackendStaticPath(value) ? toCurrentBackendUrl(value) : value;
  }

  if (!isLoopbackUrl(value)) {
    return value;
  }

  const path = extractAbsolutePath(value);
  if (!isBackendStaticPath(path)) {
    return value;
  }

  return toCurrentBackendUrl(path);
}

module.exports = {
  normalizeBackendAssetUrl,
};
