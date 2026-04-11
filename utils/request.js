const { baseURL } = require("../config");
const { getOperatorQuery, hasOperatorContext } = require("./operator-context");

const SUCCESS_CODE = 0;
const REQUEST_TIMEOUT = 12000;

function showErrorToast(message) {
  if (typeof wx !== "undefined" && typeof wx.showToast === "function") {
    wx.showToast({
      title: String(message || "\u8bf7\u6c42\u5931\u8d25"),
      icon: "none",
      duration: 1800,
    });
  }
}

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

function isWriteMethod(method) {
  return ["POST", "PUT", "PATCH", "DELETE"].includes(method);
}

function mapNetworkErrorMessage(errMsg) {
  const raw = String(errMsg || "");
  if (/timeout/i.test(raw)) {
    return "\u8bf7\u6c42\u8d85\u65f6\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u670d\u52a1";
  }
  if (/refused/i.test(raw)) {
    return "\u8fde\u63a5\u88ab\u62d2\u7edd\uff0c\u8bf7\u786e\u8ba4\u540e\u7aef\u5df2\u542f\u52a8";
  }
  if (/url not in domain list/i.test(raw)) {
    return "\u8bf7\u6c42\u57df\u540d\u4e0d\u5728\u767d\u540d\u5355";
  }
  return raw || "\u7f51\u7edc\u8bf7\u6c42\u5931\u8d25";
}

function request(options) {
  const opts = options || {};
  const method = toMethod(opts.method);
  const withOperator = opts.withOperator === true || (opts.withOperator !== false && isWriteMethod(method));

  if (withOperator && !hasOperatorContext()) {
    const message = "\u8bf7\u5148\u767b\u5f55";
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
    wx.request({
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
          const message = payload.message || "\u4e1a\u52a1\u8bf7\u6c42\u5931\u8d25";
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
