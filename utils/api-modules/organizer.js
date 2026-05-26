const { request } = require("../request");
const { baseURL } = require("../../config");
const { getOperatorContext } = require("../operator-context");
const feedback = require("../feedback");
const { SUCCESS_CODE, buildQueryString } = require("./shared");

function uploadOrganizerActivityCover(filePath) {
  const ctx = getOperatorContext();
  const operatorUserId = Number(ctx.operatorUserId);
  const operatorRole = String(ctx.operatorRole || "").toUpperCase();
  if (!Number.isFinite(operatorUserId) || operatorUserId <= 0 || !operatorRole) {
    const message = "请先登录";
    feedback.error(message);
    return Promise.reject({ message, code: 40100 });
  }

  const normalizedFilePath = String(filePath || "").trim();
  if (!normalizedFilePath) {
    const message = "请先选择活动图片";
    feedback.error(message);
    return Promise.reject({ message, code: 40000 });
  }

  const query = buildQueryString({ operatorUserId, operatorRole });
  const url = `${baseURL}/api/v1/organizer/activities/cover${query}`;

  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url,
      filePath: normalizedFilePath,
      name: "file",
      success(res) {
        const statusCode = res && res.statusCode;
        const rawData = res && res.data ? String(res.data) : "";
        let payload = null;
        try {
          payload = rawData ? JSON.parse(rawData) : null;
        } catch (e) {
          const message = "活动图片上传响应解析失败";
          feedback.error(message);
          reject({ message, err: e, statusCode });
          return;
        }

        if (!res || statusCode < 200 || statusCode >= 300) {
          const message = String((payload && payload.message) || "").trim() || `请求失败（${statusCode}）`;
          feedback.error(message);
          reject({ message, statusCode, code: payload && payload.code });
          return;
        }

        if (!payload || payload.code !== SUCCESS_CODE) {
          const message = String((payload && payload.message) || "").trim() || "活动图片上传失败";
          feedback.error(message);
          reject({ message, statusCode, code: payload && payload.code });
          return;
        }

        resolve(payload.data || {});
      },
      fail(err) {
        const message = "活动图片上传失败，请稍后重试";
        feedback.error(message);
        reject({ message, err });
      },
    });
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

function getOrganizerActivityDetail(activityId) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}`,
    method: "GET",
    withOperator: true,
  });
}

function getOrganizerActivityOptions() {
  return request({
    path: "/api/v1/organizer/activities/options",
    method: "GET",
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

function listOrganizerActivityRegistrations(activityId, status) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/registrations`,
    method: "GET",
    query: status ? { status } : {},
    withOperator: true,
  });
}

function organizerCheckIn(activityId, ticketCode) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/check-in`,
    method: "POST",
    data: {
      ticketCode: String(ticketCode || "").trim(),
    },
  });
}

function listActivityManagers(activityId) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/managers`,
    method: "GET",
    withOperator: true,
  });
}

function addActivityManager(activityId, payload) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/managers`,
    method: "POST",
    data: payload || {},
    withOperator: true,
  });
}

function removeActivityManager(activityId, managerUserId) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/managers/${managerUserId}`,
    method: "DELETE",
    withOperator: true,
  });
}

module.exports = {
  uploadOrganizerActivityCover,
  listOrganizerActivities,
  getOrganizerActivityDetail,
  getOrganizerActivityOptions,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
  listOrganizerActivityRegistrations,
  organizerCheckIn,
  listActivityManagers,
  addActivityManager,
  removeActivityManager,
};
