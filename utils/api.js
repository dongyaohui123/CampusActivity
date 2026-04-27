const { request } = require("./request");
const { baseURL } = require("../config");
const { getOperatorContext } = require("./operator-context");
const feedback = require("./feedback");

const SUCCESS_CODE = 0;

function buildQueryString(query) {
  if (!query || typeof query !== "object") {
    return "";
  }
  const pairs = Object.keys(query)
    .filter((key) => query[key] !== undefined && query[key] !== null && query[key] !== "")
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key])}`);
  return pairs.length > 0 ? `?${pairs.join("&")}` : "";
}

/**
 * 认证：登录。
 */
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

/**
 * 认证：注册。
 */
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

/**
 * 认证：微信小程序登录。
 */
function wechatLogin(payload) {
  return request({
    path: "/api/v1/auth/wechat-login",
    method: "POST",
    data: payload || {},
    withOperator: false,
  });
}

/**
 * 用户资料：更新昵称、头像等。
 */
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

/**
 * 用户：修改密码。
 */
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

/**
 * 用户头像：上传文件，返回 avatarUrl。
 */
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

/**
 * 公共活动：列表。
 */
function listPublicActivities(params) {
  return request({
    path: "/api/v1/activities",
    method: "GET",
    query: params || {},
    withOperator: false,
  });
}

/**
 * 公共活动：详情。
 */
function getPublicActivityDetail(activityId) {
  return request({
    path: `/api/v1/activities/${activityId}`,
    method: "GET",
    withOperator: false,
  });
}

/**
 * 当前用户报名记录。
 * 若未传 userId，默认取当前操作上下文中的 operatorUserId。
 */
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

/**
 * 学生报名活动。
 */
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

/**
 * 学生取消报名。
 */
function cancelRegistration(registrationId, remark) {
  return request({
    path: `/api/v1/registrations/${registrationId}/cancel`,
    method: "PUT",
    data: {
      remark: remark || "",
    },
  });
}

/**
 * 组织者活动列表。
 */
function listOrganizerActivities(params) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "GET",
    query: params || {},
    withOperator: true,
  });
}

/**
 * 组织者创建活动。
 */
function createOrganizerActivity(payload) {
  return request({
    path: "/api/v1/organizer/activities",
    method: "POST",
    data: payload,
  });
}

/**
 * 组织者更新活动。
 */
function updateOrganizerActivity(activityId, payload) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}`,
    method: "PUT",
    data: payload,
  });
}

/**
 * 组织者提审活动。
 */
function submitActivityReview(activityId, comment) {
  return request({
    path: `/api/v1/organizer/activities/${activityId}/submit-review`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

/**
 * 管理员：待审核列表。
 */
function listPendingReviews() {
  return request({
    path: "/api/v1/admin/reviews/pending",
    method: "GET",
    withOperator: true,
  });
}

/**
 * 管理员：审核通过。
 */
function approveReview(activityId, comment) {
  return request({
    path: `/api/v1/admin/reviews/${activityId}/approve`,
    method: "POST",
    data: {
      comment: comment || "",
    },
  });
}

/**
 * 管理员：审核驳回。
 */
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
  login,
  registerUser,
  wechatLogin,
  updateUserProfile,
  uploadUserAvatar,
  changePassword,
  listPublicActivities,
  getPublicActivityDetail,
  getMyRegistrations,
  registerActivity,
  cancelRegistration,
  listOrganizerActivities,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
  listPendingReviews,
  approveReview,
  rejectReview,
};
