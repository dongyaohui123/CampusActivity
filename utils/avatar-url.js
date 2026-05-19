const { baseURL } = require("../config");
const { normalizeBackendAssetUrl } = require("./backend-asset-url");

function stripTrailingSlash(value) {
  return String(value || "").replace(/\/+$/, "");
}

function isAbsoluteUrl(value) {
  return /^(https?:)?\/\//i.test(value) || /^wxfile:\/\//i.test(value) || /^data:/i.test(value);
}

function normalizeAvatarUrl(avatarUrl) {
  const value = String(avatarUrl || "").trim();
  if (!value) {
    return "";
  }
  const normalizedBackendUrl = normalizeBackendAssetUrl(value);
  if (normalizedBackendUrl !== value) {
    return normalizedBackendUrl;
  }
  if (isAbsoluteUrl(value)) {
    return value;
  }
  if (value.startsWith("/")) {
    return `${stripTrailingSlash(baseURL)}${value}`;
  }
  return value;
}

module.exports = {
  normalizeAvatarUrl,
};
