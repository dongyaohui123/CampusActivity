const { request } = require("../request");
const { baseURL } = require("../../config");
const { getOperatorContext } = require("../operator-context");
const feedback = require("../feedback");
const { SUCCESS_CODE, buildQueryString } = require("./shared");

function updateUserProfile(userId, payload) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "请先登录" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/profile`,
    method: "PUT",
    data: payload || {},
    withOperator: true,
  });
}

function changePassword(userId, oldPassword, newPassword) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "请先登录" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/password`,
    method: "PUT",
    data: {
      oldPassword: String(oldPassword || ""),
      newPassword: String(newPassword || ""),
    },
    withOperator: true,
  });
}

function uploadUserAvatar(userId, filePath) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    const message = "请先登录";
    feedback.error(message);
    return Promise.reject({ message, code: 40100 });
  }

  const operatorUserId = Number(ctx.operatorUserId);
  const operatorRole = String(ctx.operatorRole || "").toUpperCase();
  if (!Number.isFinite(operatorUserId) || operatorUserId <= 0 || !operatorRole) {
    const message = "请先登录";
    feedback.error(message);
    return Promise.reject({ message, code: 40100 });
  }

  const normalizedFilePath = String(filePath || "").trim();
  if (!normalizedFilePath) {
    const message = "请先选择头像图片";
    feedback.error(message);
    return Promise.reject({ message, code: 40000 });
  }

  const query = buildQueryString({ operatorUserId, operatorRole });
  const uploadPaths = [
    `/api/v1/users/${targetUserId}/avatar`,
    `/api/v1/user/${targetUserId}/avatar`,
    `/api/v1/users/${targetUserId}/avatar/`,
    `/api/v1/user/${targetUserId}/avatar/`,
  ];

  function isStaticResourcePathMiss(message, statusCode) {
    const text = String(message || "").toLowerCase();
    return Number(statusCode) === 404 && /no static resource/.test(text);
  }

  function resolveHttpErrorMessage(payload, statusCode) {
    const raw = String((payload && payload.message) || "").trim();
    if (raw) {
      return raw;
    }
    return `请求失败（${statusCode}）`;
  }

  function doUpload(path, silentError) {
    const url = `${baseURL}${path}${query}`;
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
            const parseFallbackMessage = isStaticResourcePathMiss(rawData, statusCode)
              ? "头像上传接口未命中"
              : "头像上传响应解析失败";
            if (!silentError) {
              feedback.error(parseFallbackMessage);
            }
            reject({
              message: parseFallbackMessage,
              err: e,
              statusCode,
              retryablePathFallback: isStaticResourcePathMiss(rawData, statusCode),
            });
            return;
          }

          if (!res || statusCode < 200 || statusCode >= 300) {
            const rawMessage = String((payload && payload.message) || "").trim();
            const retryablePathFallback =
              isStaticResourcePathMiss(rawMessage, statusCode) || isStaticResourcePathMiss(rawData, statusCode);
            const message = retryablePathFallback ? "头像上传接口未命中" : resolveHttpErrorMessage(payload, statusCode);
            if (!silentError && !retryablePathFallback) {
              feedback.error(message);
            }
            reject({
              message,
              statusCode,
              code: payload && payload.code,
              retryablePathFallback,
            });
            return;
          }

          if (!payload || payload.code !== SUCCESS_CODE) {
            const message = String((payload && payload.message) || "").trim() || "头像上传失败";
            if (!silentError) {
              feedback.error(message);
            }
            reject({
              message,
              statusCode,
              code: payload && payload.code,
              retryablePathFallback: isStaticResourcePathMiss(message, statusCode),
            });
            return;
          }

          resolve(payload.data || {});
        },
        fail(err) {
          const message = "头像上传失败，请稍后重试";
          if (!silentError) {
            feedback.error(message);
          }
          reject({ message, err, retryablePathFallback: false });
        },
      });
    });
  }

  return doUpload(uploadPaths[0], true).catch((firstError) => {
    if (!firstError || !firstError.retryablePathFallback) {
      if (firstError && firstError.message) {
        feedback.error(firstError.message);
      }
      return Promise.reject(firstError);
    }
    return doUpload(uploadPaths[1], true)
      .catch((secondError) => {
        if (!secondError || !secondError.retryablePathFallback) {
          if (secondError && secondError.message) {
            feedback.error(secondError.message);
          }
          return Promise.reject(secondError);
        }
        return doUpload(uploadPaths[2], true)
          .catch((thirdError) => {
            if (!thirdError || !thirdError.retryablePathFallback) {
              if (thirdError && thirdError.message) {
                feedback.error(thirdError.message);
              }
              return Promise.reject(thirdError);
            }
            return doUpload(uploadPaths[3], false);
          });
      });
  });
}

function getMyRegistrations(userId) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "请先登录" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/registrations`,
    method: "GET",
    withOperator: true,
  });
}

function getMyFavorites(userId) {
  const ctx = getOperatorContext();
  const targetUserId = Number(userId || ctx.operatorUserId);
  if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
    return Promise.reject({ message: "请先登录" });
  }
  return request({
    path: `/api/v1/users/${targetUserId}/favorites`,
    method: "GET",
    withOperator: true,
  });
}

module.exports = {
  updateUserProfile,
  changePassword,
  uploadUserAvatar,
  getMyRegistrations,
  getMyFavorites,
};
