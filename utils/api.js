const { request } = require("./request");
const { getOperatorContext } = require("./operator-context");

/**
 * 认证：登录。
 */
function login(account, password) {
  return request({
    path: "/api/v1/auth/login",
    method: "POST",
    data: {
      username: String(account || "").trim(),
      password: String(password || ""),
    },
    withOperator: false,
  });
}

/**
 * 认证：注册。
 */
function registerUser(username, password, nickname, phone) {
  return request({
    path: "/api/v1/auth/register",
    method: "POST",
    data: {
      username: String(username || "").trim(),
      password: String(password || ""),
      nickname: String(nickname || "").trim(),
      phone: String(phone || "").trim(),
    },
    withOperator: false,
  });
}

/**
 * 公共活动：列表。
 */
function listPublicActivities(params) {
  return request({
    path: "/api/v1/activities",
    method: "GET",
    query: params || {},
    withOperator: false,
  });
}

/**
 * 公共活动：详情。
 */
function getPublicActivityDetail(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}`,
    method: "GET",
    withOperator: false,
  });
}

/**
 * 当前用户报名记录。
 * 若未传 userId，默认取当前操作上下文中的 operatorUserId。
 */
function getMyRegistrations(userId) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "请先登录" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/registrations`,
    method: "GET",
    withOperator: true,
  });
}

/**
 * 学生报名活动。
 */
function registerActivity(activityId, remark) {
  return request({
    path: "/api/v1/registrations",
    method: "POST",
    data: {
      activityId,
      remark: remark || "",
    },
  });
}

/**
 * 学生取消报名。
 */
function cancelRegistration(registrationId, remark) {
  return request({
    path: `/api/v1/registrations/${registrationId}/cancel`,
    method: "PUT",
    data: {
      remark: remark || "",
    },
  });
}

/**
 * 组织者活动列表。
 */
function listOrganizerActivities(params) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "GET",
    query: params || {},
    withOperator: true,
  });
}

/**
 * 组织者创建活动。
 */
function createOrganizerActivity(payload) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "POST",
    data: payload,
  });
}

/**
 * 组织者更新活动。
 */
function updateOrganizerActivity(activityId, payload) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}`,
    method: "PUT",
    data: payload,
  });
}

/**
 * 组织者提审活动。
 */
function submitActivityReview(activityId, comment) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/submit-review`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

/**
 * 管理员：待审核列表。
 */
function listPendingReviews() {
  return request({
    path: "/api/v1/admin/reviews/pending",
    method: "GET",
    withOperator: true,
  });
}

/**
 * 管理员：审核通过。
 */
function approveReview(activityId, comment) {
  return request({
    path: `/api/v1/admin/reviews/${activityId}/approve`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

/**
 * 管理员：审核驳回。
 */
function rejectReview(activityId, comment) {
  return request({
    path: `/api/v1/admin/reviews/${activityId}/reject`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

module.exports = {
  login,
  registerUser,
  listPublicActivities,
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
  listOrganizerActivities,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
  listPendingReviews,
  approveReview,
  rejectReview,
};
