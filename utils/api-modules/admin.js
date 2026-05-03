const { request } = require("../request");

function listPendingReviews() {
  return request({
    path: "/api/v1/admin/reviews/pending",
    method: "GET",
    withOperator: true,
  });
}

function approveReview(activityId, payload) {
  const requestPayload = payload && typeof payload === "object" ? payload : {};
  return request({
    path: `/api/v1/admin/reviews/${activityId}/approve`,
    method: "POST",
    data: {
      comment: String(requestPayload.comment || ""),
      featured: Boolean(requestPayload.featured),
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
  listPendingReviews,
  approveReview,
  rejectReview,
};
