const STORAGE_KEY = "operator_context";

const EMPTY_CONTEXT = {
  operatorUserId: null,
  operatorRole: "",
};

const ROLE_SET = new Set(["STUDENT", "ORGANIZER", "ADMIN"]);

let memoryContext = { ...EMPTY_CONTEXT };

function normalizeContext(input) {
  const raw = input || {};
  const operatorUserId = Number(raw.operatorUserId);
  const operatorRole = String(raw.operatorRole || "").toUpperCase();
  const isValid = Number.isFinite(operatorUserId) && operatorUserId > 0 && ROLE_SET.has(operatorRole);

  return isValid
    ? {
        operatorUserId,
        operatorRole,
      }
    : { ...EMPTY_CONTEXT };
}

function canUseWxStorage() {
  return typeof wx !== "undefined" && typeof wx.getStorageSync === "function" && typeof wx.setStorageSync === "function";
}

function getOperatorContext() {
  if (canUseWxStorage()) {
    const stored = wx.getStorageSync(STORAGE_KEY);
    if (stored && typeof stored === "object") {
      memoryContext = normalizeContext(stored);
      return { ...memoryContext };
    }
  }
  return { ...memoryContext };
}

function setOperatorContext(nextContext) {
  memoryContext = normalizeContext(nextContext);
  if (canUseWxStorage()) {
    wx.setStorageSync(STORAGE_KEY, memoryContext);
  }
  return { ...memoryContext };
}

function clearOperatorContext() {
  memoryContext = { ...EMPTY_CONTEXT };
  if (canUseWxStorage()) {
    wx.removeStorageSync(STORAGE_KEY);
  }
}

function hasOperatorContext() {
  const ctx = getOperatorContext();
  return Number.isFinite(Number(ctx.operatorUserId)) && Number(ctx.operatorUserId) > 0 && ROLE_SET.has(ctx.operatorRole);
}

function getOperatorQuery() {
  const ctx = getOperatorContext();
  if (!hasOperatorContext()) {
    return {};
  }
  return {
    operatorUserId: ctx.operatorUserId,
    operatorRole: ctx.operatorRole,
  };
}

module.exports = {
  EMPTY_CONTEXT,
  getOperatorContext,
  setOperatorContext,
  clearOperatorContext,
  hasOperatorContext,
  getOperatorQuery,
};
