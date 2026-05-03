const { request } = require("../request");

function listPublicActivities(params) {
  return request({
    path: "/api/v1/activities",
    method: "GET",
    query: params || {},
    withOperator: false,
  });
}

function listManageableActivities(params) {
  return request({
    path: "/api/v1/activities/manageable",
    method: "GET",
    query: params || {},
    withOperator: true,
  });
}

function getPublicActivityDetail(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}`,
    method: "GET",
    withOperator: false,
  });
}

function listActivityComments(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}/comments`,
    method: "GET",
    withOperator: false,
  });
}

function createActivityComment(activityId, content) {
  return request({
    path: `/api/v1/activities/${activityId}/comments`,
    method: "POST",
    data: {
      content: String(content || ""),
    },
  });
}

function deleteActivityComment(activityId, commentId) {
  return request({
    path: `/api/v1/activities/${activityId}/comments/${commentId}`,
    method: "DELETE",
  });
}

function getActivityFavoriteStatus(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}/favorite`,
    method: "GET",
    withOperator: true,
  });
}

function favoriteActivity(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}/favorite`,
    method: "POST",
    data: {},
  });
}

function unfavoriteActivity(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}/favorite`,
    method: "DELETE",
  });
}

module.exports = {
  listPublicActivities,
  listManageableActivities,
  getPublicActivityDetail,
  listActivityComments,
  createActivityComment,
  deleteActivityComment,
  getActivityFavoriteStatus,
  favoriteActivity,
  unfavoriteActivity,
};
