const {
  listOrganizerActivities,
  createOrganizerActivity,
  updateOrganizerActivity,
  submitActivityReview,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");

const VISIBILITY_OPTIONS = ["PUBLIC", "PRIVATE"];
const DATE_FIELD_LABEL_KEY = {
  startTime: "startTimeLabel",
  endTime: "endTimeLabel",
  registrationDeadline: "deadlineLabel",
};

function pad2(value) {
  return String(value).padStart(2, "0");
}

function displayTime(value) {
  const normalized = normalizeDateTimeInput(value);
  if (!normalized) return "";
  return normalized.replace("T", " ");
}

function normalizeDateTimeInput(input) {
  const raw = String(input || "").trim();
  if (!raw) return "";

  const withT = raw.replace(" ", "T");
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(withT)) {
    return `${withT}:00`;
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(withT)) {
    return withT;
  }

  const timestamp = Date.parse(withT);
  if (!Number.isFinite(timestamp)) return withT;

  const date = new Date(timestamp);
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function toDateTimeDisplay(input) {
  const normalized = normalizeDateTimeInput(input);
  if (!normalized) return "";
  return normalized.slice(0, 16).replace("T", " ");
}

function parseDateTimeTimestamp(input) {
  const normalized = normalizeDateTimeInput(input);
  if (!normalized) return NaN;
  const timestamp = Date.parse(normalized);
  return Number.isFinite(timestamp) ? timestamp : NaN;
}

function formatTimestampToDisplay(timestamp) {
  const numeric = Number(timestamp);
  if (!Number.isFinite(numeric)) return "";
  const date = new Date(numeric);
  if (!Number.isFinite(date.getTime())) return "";

  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

function extractPickerTimestamp(detail) {
  if (typeof detail === "number") return detail;
  if (detail instanceof Date) return detail.getTime();

  if (detail && typeof detail === "object") {
    if (typeof detail.value === "number") return detail.value;
    if (detail.value instanceof Date) return detail.value.getTime();
  }

  return NaN;
}

function buildFormFromActivity(activity) {
  return {
    title: activity.title || "",
    summary: activity.summary || "",
    location: activity.location || "",
    startTime: toDateTimeDisplay(activity.startTime),
    endTime: toDateTimeDisplay(activity.endTime),
    registrationDeadline: toDateTimeDisplay(activity.registrationDeadline),
    maxParticipants: String(activity.maxParticipants || 0),
    visibilityIndex: Math.max(0, VISIBILITY_OPTIONS.indexOf(activity.visibility || "PUBLIC")),
    featured: Boolean(activity.featured),
  };
}

function emptyForm() {
  return {
    title: "",
    summary: "",
    location: "",
    startTime: "",
    endTime: "",
    registrationDeadline: "",
    maxParticipants: "100",
    visibilityIndex: 0,
    featured: false,
  };
}

Page({
  data: {
    i18n: {
      navTitle: "组织者活动管理",
      roleHintPrefix: "当前角色为",
      roleHintSuffix: "请在首页切换为组织者后再操作。",
      editActivity: "编辑活动",
      createActivity: "创建活动",
      titleLabel: "标题",
      summaryLabel: "摘要",
      locationLabel: "地点",
      startTimeLabel: "开始时间",
      endTimeLabel: "结束时间",
      deadlineLabel: "报名截止时间",
      maxParticipantsLabel: "人数上限",
      visibilityLabel: "可见性",
      featuredLabel: "推荐活动（是否推荐）",
      updateActivity: "更新活动",
      reset: "重置",
      submitNote: "提审备注（可选）",
      submitCommentPlaceholder: "提审备注",
      loading: "加载中...",
      statusLabel: "状态",
      reviewLabel: "审核",
      timeLabel: "时间",
      edit: "编辑",
      submitReview: "提审",
      empty: "暂无活动",
      phTitle: "例如：校园歌手大赛",
      phSummary: "一句话摘要",
      phLocation: "例如：大学生活动中心",
      myActivityList: "我的活动",
      required: "*",
    },
    operatorRole: "STUDENT",
    loading: false,
    activities: [],
    editingActivityId: null,
    reviewComment: "来自小程序的提审备注",
    form: emptyForm(),
    visibilityOptions: VISIBILITY_OPTIONS,
    showVisibilityPicker: false,
    showDateTimePicker: false,
    activeDateField: "",
    dateTimePickerTitle: "",
    dateTimePickerValue: Date.now(),
    dateTimeMin: new Date(2020, 0, 1, 0, 0, 0).getTime(),
    dateTimeMax: new Date(2035, 11, 31, 23, 59, 59).getTime(),
  },

  onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ORGANIZER") this.loadActivities();
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async loadActivities() {
    this.setData({ loading: true });
    try {
      const list = await listOrganizerActivities({});
      this.setData({
        activities: (list || []).map((item) => ({
          ...item,
          startDisplay: displayTime(item.startTime),
          endDisplay: displayTime(item.endTime),
        })),
      });
    } catch (e) {
      this.setData({ activities: [] });
    } finally {
      this.setData({ loading: false });
    }
  },

  onFormInput(event) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail });
  },

  onOpenDateTimePicker(event) {
    const field = event.currentTarget.dataset.field;
    if (!DATE_FIELD_LABEL_KEY[field]) return;

    const valueFromForm = this.data.form[field];
    const parsed = parseDateTimeTimestamp(valueFromForm);

    this.setData({
      activeDateField: field,
      dateTimePickerTitle: this.data.i18n[DATE_FIELD_LABEL_KEY[field]] || "",
      dateTimePickerValue: Number.isFinite(parsed) ? parsed : Date.now(),
      showDateTimePicker: true,
    });
  },

  onDateTimeInput(event) {
    const next = extractPickerTimestamp(event.detail);
    if (Number.isFinite(next)) {
      this.setData({ dateTimePickerValue: next });
    }
  },

  onDateTimeConfirm(event) {
    const field = this.data.activeDateField;
    if (!field) {
      this.setData({ showDateTimePicker: false });
      return;
    }

    const picked = extractPickerTimestamp(event.detail);
    const finalValue = Number.isFinite(picked) ? picked : Number(this.data.dateTimePickerValue);

    if (!Number.isFinite(finalValue)) {
      this.setData({ showDateTimePicker: false, activeDateField: "" });
      return;
    }

    this.setData({
      [`form.${field}`]: formatTimestampToDisplay(finalValue),
      showDateTimePicker: false,
      activeDateField: "",
    });
  },

  onDateTimeCancel() {
    this.setData({ showDateTimePicker: false, activeDateField: "" });
  },

  onOpenVisibilityPicker() {
    this.setData({ showVisibilityPicker: true });
  },

  onCloseVisibilityPicker() {
    this.setData({ showVisibilityPicker: false });
  },

  onVisibilityChange(event) {
    this.setData({ "form.visibilityIndex": Number(event.detail.value) });
  },

  onVisibilityConfirm(event) {
    const rawIndex = Array.isArray(event.detail.index) ? event.detail.index[0] : event.detail.index;
    const index = Number(rawIndex);
    this.setData({
      "form.visibilityIndex": Number.isFinite(index) ? index : 0,
      showVisibilityPicker: false,
    });
  },

  onVisibilityCancel() {
    this.setData({ showVisibilityPicker: false });
  },

  onFeaturedChange(event) {
    this.setData({ "form.featured": Boolean(event.detail) });
  },

  onReviewCommentInput(event) {
    this.setData({ reviewComment: event.detail });
  },

  resetForm() {
    this.setData({
      editingActivityId: null,
      form: emptyForm(),
      showVisibilityPicker: false,
      showDateTimePicker: false,
      activeDateField: "",
    });
  },

  onEditTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const target = this.data.activities.find((item) => Number(item.id) === activityId);
    if (!target) return;

    this.setData({
      editingActivityId: activityId,
      form: buildFormFromActivity(target),
      showDateTimePicker: false,
      activeDateField: "",
    });
  },

  buildPayload() {
    const form = this.data.form;
    const payload = {
      title: String(form.title || "").trim(),
      summary: String(form.summary || "").trim(),
      location: String(form.location || "").trim(),
      startTime: normalizeDateTimeInput(form.startTime),
      endTime: normalizeDateTimeInput(form.endTime),
      maxParticipants: Number(form.maxParticipants || 0),
      visibility: this.data.visibilityOptions[form.visibilityIndex] || "PUBLIC",
      featured: Boolean(form.featured),
    };

    const deadline = normalizeDateTimeInput(form.registrationDeadline);
    if (deadline) payload.registrationDeadline = deadline;

    return payload;
  },

  async onSubmitTap() {
    if (this.data.operatorRole !== "ORGANIZER") return feedback.error("请先切换为组织者");

    const payload = this.buildPayload();
    if (!payload.title || !payload.location || !payload.startTime || !payload.endTime) {
      return feedback.error("标题、地点、开始和结束时间必填");
    }

    const startTimestamp = parseDateTimeTimestamp(payload.startTime);
    const endTimestamp = parseDateTimeTimestamp(payload.endTime);
    if (!Number.isFinite(startTimestamp) || !Number.isFinite(endTimestamp) || startTimestamp >= endTimestamp) {
      return feedback.error("开始时间必须早于结束时间");
    }

    if (!Number.isFinite(payload.maxParticipants) || payload.maxParticipants < 0) {
      return feedback.error("人数必须为非负数");
    }

    try {
      if (this.data.editingActivityId) {
        await updateOrganizerActivity(this.data.editingActivityId, payload);
        feedback.success("活动已更新");
      } else {
        await createOrganizerActivity(payload);
        feedback.success("活动已创建");
      }
      this.resetForm();
      await this.loadActivities();
    } catch (e) {}
  },

  async onSubmitReviewTap(event) {
    try {
      await submitActivityReview(Number(event.currentTarget.dataset.id), this.data.reviewComment);
      feedback.success("提审成功");
      await this.loadActivities();
    } catch (e) {}
  },
});
