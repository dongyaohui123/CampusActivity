const { request } = require("../request");
const { baseURL } = require("../../config");
const { getOperatorQuery, hasOperatorContext } = require("../operator-context");
const { buildQueryString } = require("./shared");

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

function getRegistrationTicket(registrationId) {
  return request({
    path: `/api/v1/registrations/${registrationId}/ticket`,
    method: "GET",
    withOperator: true,
  });
}

function getRegistrationTicketQrCodeUrl(registrationId) {
  if (!hasOperatorContext()) {
    return "";
  }
  const query = buildQueryString(getOperatorQuery());
  return `${baseURL}/api/v1/registrations/${registrationId}/ticket/qrcode${query}`;
}

module.exports = {
  registerActivity,
  cancelRegistration,
  getRegistrationTicket,
  getRegistrationTicketQrCodeUrl,
};
