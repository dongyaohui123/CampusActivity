// Change only this flag:
// "local" -> WeChat DevTools on the same computer
// "lan"   -> real phone in the same LAN
const RUN_ENV = "local";

const BASE_URL_MAP = {
  local: "http://127.0.0.1:8080",
  lan: "http://192.168.1.39:8080",
};

const config = {
  baseURL: BASE_URL_MAP[RUN_ENV] || BASE_URL_MAP.local,
};

module.exports = config;
