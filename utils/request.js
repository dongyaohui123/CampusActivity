const { baseURL } = require("../config");
const { getOperatorQuery, hasOperatorContext } = require("./operator-context");
const feedback = require("./feedback");
const { getRuntimeApi } = require("./runtime-api");

const SUCCESS_CODE = 0;
const REQUEST_TIMEOUT = 12000;

function showErrorToast(message) {
  feedback.error(String(message || "请求失败"));
}

/**
 * 组装 query 字符串，自动过滤空值。
 */
function buildQueryString(query) {
  if (!query || typeof query !== "object") {
    return "";
  }
  const pairs = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== "")
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`);
  return pairs.length > 0 ? `?${pairs.join("&")}` : "";
}

function toMethod(method) {
  return String(method || "GET").toUpperCase();
}

/**
 * 写操作默认要求操作人上下文（除非显式 withOperator: false）。
 */
function isWriteMethod(method) {
  return ["POST", "PUT", "PATCH", "DELETE"].includes(method);
}

/**
 * 网络错误到用户文案映射。
 */
function mapNetworkErrorMessage(errMsg) {
  const raw = String(errMsg || "");
  if (/timeout/i.test(raw)) {
    return "请求超时，请检查后端服务";
  }
  if (/refused/i.test(raw)) {
    return "连接被拒绝，请确认后端已启动";
  }
  if (/url not in domain list/i.test(raw)) {
    return "请求域名不在白名单";
  }
  return raw || "网络请求失败";
}

/**
 * 小程序统一请求封装：
 * 1) 自动拼接 operator 参数
 * 2) 统一处理 HTTP 错误和业务码错误
 * 3) 统一吐出用户提示
 */
function request(options) {
  const opts = options || {};
  const runtimeApi = getRuntimeApi();
  const method = toMethod(opts.method);
  const withOperator = opts.withOperator === true || (opts.withOperator !== false && isWriteMethod(method));

  if (!runtimeApi || typeof runtimeApi.request !== "function") {
    const message = "当前小程序环境不支持网络请求";
    if (!opts.silent) {
      showErrorToast(message);
    }
    return Promise.reject({ message });
  }

  if (withOperator && !hasOperatorContext()) {
    const message = "请先登录";
    if (!opts.silent) {
      showErrorToast(message);
    }
    return Promise.reject({ message, code: 40100 });
  }

  const query = Object.assign({}, opts.query || {}, withOperator ? getOperatorQuery() : {});
  const path = opts.path || "";
  const url = `${baseURL}${path}${buildQueryString(query)}`;
  const data = opts.data || {};
  const headers = Object.assign(
    {
      "content-type": "application/json",
    },
    opts.headers || {}
  );
  const silent = Boolean(opts.silent);

  return new Promise((resolve, reject) => {
    runtimeApi.request({
      url,
      method,
      data,
      header: headers,
      timeout: Number(opts.timeout) > 0 ? Number(opts.timeout) : REQUEST_TIMEOUT,
      success(res) {
        const payload = res.data || {};
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const message = payload.message || `HTTP ${res.statusCode}`;
          if (!silent) {
            showErrorToast(message);
          }
          reject({ message, statusCode: res.statusCode, code: payload.code, url });
          return;
        }

        if (payload.code !== SUCCESS_CODE) {
          const message = payload.message || "业务请求失败";
          if (!silent) {
            showErrorToast(message);
          }
          reject({ message, statusCode: res.statusCode, code: payload.code, url });
          return;
        }

        resolve(payload.data);
      },
      fail(err) {
        const rawErrMsg = (err && err.errMsg) || "";
        const message = mapNetworkErrorMessage(rawErrMsg);
        if (!silent) {
          showErrorToast(message);
        }
        reject({ message, errMsg: rawErrMsg, err, url });
      },
    });
  });
}

module.exports = {
  request,
};
