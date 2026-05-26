const { request } = require("../request");

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

function wechatLogin(payload) {
  return request({
    path: "/api/v1/auth/wechat-login",
    method: "POST",
    data: payload || {},
    withOperator: false,
  });
}

function qqLogin(payload) {
  return request({
    path: "/api/v1/auth/qq-login",
    method: "POST",
    data: payload || {},
    withOperator: false,
  });
}

function sendSmsCode(phone) {
  return request({
    path: "/api/v1/auth/send-sms-code",
    method: "POST",
    query: {
      phone: String(phone || "").trim(),
    },
    withOperator: false,
  });
}

function resetPassword(username, phone, newPassword, code) {
  return request({
    path: "/api/v1/auth/reset-password",
    method: "POST",
    data: {
      username: String(username || "").trim(),
      phone: String(phone || "").trim(),
      newPassword: String(newPassword || ""),
      code: String(code || "").trim(),
    },
    withOperator: false,
  });
}

module.exports = {
  login,
  registerUser,
  wechatLogin,
  qqLogin,
  sendSmsCode,
  resetPassword,
};
