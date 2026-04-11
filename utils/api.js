const { request } = require("./request");
const { getOperatorContext } = require("./operator-context");

function login(username, password) {
  return request({
    path: "/api/v1/auth/login",
    method: "POST",
    data: {
      username: String(username || "").trim(),
      password: String(password || ""),
    },
    withOperator: false,
  });
}

function registerUser(username, password, nickname) {
  return request({
    path: "/api/v1/auth/register",
    method: "POST",
    data: {
      username: String(username || "").trim(),
      password: String(password || ""),
      nickname: String(nickname || "").trim(),
    },
    withOperator: false,
  });
}

function listPublicActivities(params) {
  return request({
    path: "/api/v1/activities",
    method: "GET",
    query: params || {},
    withOperator: false,
  });
}

function getPublicActivityDetail(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}`,
    method: "GET",
    withOperator: false,
  });
}

function getMyRegistrations(userId) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "\u8bf7\u5148\u767b\u5f55" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/registrations`,
    method: "GET",
    withOperator: true,
  });
}

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

function cancelRegistration(registrationId, remark) {
  return request({
    path: `/api/v1/registrations/${registrationId}/cancel`,
    method: "PUT",
    data: {
      remark: remark || "",
    },
  });
}

function listOrganizerActivities(params) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "GET",
    query: params || {},
    withOperator: true,
  });
}

function createOrganizerActivity(payload) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "POST",
    data: payload,
  });
}

function updateOrganizerActivity(activityId, payload) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}`,
    method: "PUT",
    data: payload,
  });
}

function submitActivityReview(activityId, comment) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/submit-review`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

function listPendingReviews() {
  return request({
    path: "/api/v1/admin/reviews/pending",
    method: "GET",
    withOperator: true,
  });
}

function approveReview(activityId, comment) {
  return request({
    path: `/api/v1/admin/reviews/${activityId}/approve`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

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
