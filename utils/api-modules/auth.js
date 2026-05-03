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

module.exports = {
  login,
  registerUser,
  wechatLogin,
};
