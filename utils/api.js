const authApi = require("./api-modules/auth");
const activityApi = require("./api-modules/activities");
const registrationApi = require("./api-modules/registrations");
const userApi = require("./api-modules/users");
const organizerApi = require("./api-modules/organizer");
const adminApi = require("./api-modules/admin");

module.exports = Object.assign(
  {},
  authApi,
  activityApi,
  registrationApi,
  userApi,
  organizerApi,
  adminApi
);
