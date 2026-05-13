const {
  listOrganizerActivities,
  getOrganizerActivityOptions,
  createOrganizerActivity,
  updateOrganizerActivity,
  uploadOrganizerActivityCover,
  submitActivityReview,
} = require("../../utils/api");
const { getOperatorContext } = require("../../utils/operator-context");
const feedback = require("../../utils/feedback");
const { getActivityFallbackCover, resolveActivityCover } = require("../../utils/image-fallbacks");

const DATE_FIELD_LABEL_KEY = {
  startTime: "startTimeLabel",
  endTime: "endTimeLabel",
  registrationDeadline: "deadlineLabel",
};
const CAMPUS_CODE_LABEL = {
  SOUTH: "南校区",
  NORTH: "北校区",
  ONLINE: "线上",
};

function pad2(value) {
  return String(value).padStart(2, "0");
}

function displayTime(value) {
  const normalized = normalizeDateTimeInput(value);
  if (!normalized) return "";
  return normalized.replace("T", " ");
}

/**
 * 输入时间标准化为后端需要的 ISO 本地格式：YYYY-MM-DDTHH:mm:ss。
 */
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

function toCampusLabel(campusCode) {
  return CAMPUS_CODE_LABEL[String(campusCode || "").toUpperCase()] || "未知校区";
}

function normalizeCampusCode(value) {
  const normalized = String(value || "").trim().toUpperCase();
  if (normalized === "SOUTH" || normalized === "NORTH" || normalized === "ONLINE") {
    return normalized;
  }
  return "";
}

function buildFormFromActivity(activity) {
  const activityTypeId = Number(activity.activityTypeId);
  const campusCode = normalizeCampusCode(activity.locationCampus);
  const fallbackCover = getActivityFallbackCover(activity);
  return {
    title: activity.title || "",
    summary: activity.summary || "",
    coverUrl: resolveActivityCover(activity) || fallbackCover,
    coverTempPath: "",
    location: activity.location || "",
    campusCode,
    activityTypeId: Number.isFinite(activityTypeId) && activityTypeId > 0 ? activityTypeId : null,
    activityTypeName: activity.activityTypeName || "",
    startTime: toDateTimeDisplay(activity.startTime),
    endTime: toDateTimeDisplay(activity.endTime),
    registrationDeadline: toDateTimeDisplay(activity.registrationDeadline),
    maxParticipants: String(activity.maxParticipants || 0),
  };
}

function emptyForm() {
  return {
    title: "",
    summary: "",
    coverUrl: getActivityFallbackCover({}),
    coverTempPath: "",
    location: "",
    campusCode: "",
    activityTypeId: null,
    activityTypeName: "",
    startTime: "",
    endTime: "",
    registrationDeadline: "",
    maxParticipants: "100",
  };
}

Page({
  data: {
    i18n: {
      navTitle: "组织者活动管理",
      roleHintPrefix: "当前角色为",
      roleHintSuffix: "，请在首页切换为组织者后再操作。",
      editActivity: "编辑活动",
      createActivity: "发布活动",
      formHint: "完善活动信息后即可提交审核",
      tipBadge: "组织者",
      basicInfo: "基础信息",
      publishOptions: "发布设置",
      titleLabel: "标题",
      summaryLabel: "摘要",
      coverLabel: "活动图片",
      coverHint: "支持 jpg/png/webp，可选",
      chooseCover: "选择图片",
      removeCover: "移除图片",
      uploadingCover: "上传中...",
      campusTypeLabel: "校区类型",
      locationLabel: "地点",
      activityTypeLabel: "活动类型",
      startTimeLabel: "开始时间",
      endTimeLabel: "结束时间",
      deadlineLabel: "报名截止时间",
      maxParticipantsLabel: "人数上限",
      updateActivity: "更新活动",
      reset: "重置",
      submitNote: "提审备注（可选）",
      submitCommentPlaceholder: "例如：可填写提审说明（可选）",
      loading: "加载中...",
      statusLabel: "状态",
      reviewLabel: "审核",
      timeLabel: "时间",
      edit: "编辑",
      checkinManage: "签到管理",
      managerManage: "管理员",
      submitReview: "提审",
      empty: "暂无活动",
      phTitle: "例如：校园歌手大赛",
      phSummary: "一句话摘要",
      campusTypePlaceholder: "请选择校区类型",
      phLocation: "请输入具体地点（线上可留空）",
      typePickerPlaceholder: "请选择活动类型",
      myActivityList: "已发布活动",
      timePickerPlaceholder: "请选择日期和时间",
      required: "*",
    },
    operatorRole: "STUDENT",
    loading: false,
    coverUploading: false,
    activities: [],
    editingActivityId: null,
    reviewComment: "",
    form: emptyForm(),
    campusTypeOptions: [],
    campusTypePickerColumns: [],
    activityTypeOptions: [],
    activityTypePickerColumns: [],
    showCampusTypePicker: false,
    showTypePicker: false,
    showDateTimePicker: false,
    activeDateField: "",
    dateTimePickerTitle: "",
    dateTimePickerValue: Date.now(),
    dateTimeMin: new Date(2020, 0, 1, 0, 0, 0).getTime(),
    dateTimeMax: new Date(2035, 11, 31, 23, 59, 59).getTime(),
  },

  async onShow() {
    const { operatorRole } = getOperatorContext();
    this.setData({ operatorRole });
    if (operatorRole === "ORGANIZER") {
      await this.initOrganizerPage();
    }
  },

  onClickNavLeft() {
    wx.navigateBack({ delta: 1 });
  },

  async initOrganizerPage() {
    await this.loadActivityOptions();
    await this.loadActivities();
  },

  async loadActivityOptions() {
    try {
      const options = await getOrganizerActivityOptions();
      const campusTypesRaw = Array.isArray(options && options.campusTypes) ? options.campusTypes : [];
      const campusTypes = campusTypesRaw
        .map((code) => normalizeCampusCode(code))
        .filter((code, index, array) => code && array.indexOf(code) === index);
      const orderedCampusTypes = ["SOUTH", "NORTH", "ONLINE"].filter((code) => campusTypes.includes(code));
      const finalCampusTypes = orderedCampusTypes.length > 0 ? orderedCampusTypes : ["SOUTH", "NORTH", "ONLINE"];
      const activityTypes = Array.isArray(options && options.activityTypes) ? options.activityTypes : [];
      this.setData({
        campusTypeOptions: finalCampusTypes,
        campusTypePickerColumns: finalCampusTypes.map((code) => toCampusLabel(code)),
        activityTypeOptions: activityTypes,
        activityTypePickerColumns: activityTypes.map((item) => item.name || ""),
      });
    } catch (e) {
      this.setData({
        campusTypeOptions: [],
        campusTypePickerColumns: [],
        activityTypeOptions: [],
        activityTypePickerColumns: [],
      });
      feedback.error("活动选项加载失败");
    }
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
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail });
    if (field === "title" || field === "summary" || field === "location") {
      this.refreshDerivedCover();
    }
  },

  onChooseCoverTap() {
    if (this.data.coverUploading) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: async (res) => {
        const file = res && res.tempFiles && res.tempFiles[0];
        const tempFilePath = String((file && file.tempFilePath) || "").trim();
        if (!tempFilePath) return;

        this.setData({
          "form.coverTempPath": tempFilePath,
          coverUploading: true,
        });

        try {
          const uploadResult = await uploadOrganizerActivityCover(tempFilePath);
          const coverUrl = resolveActivityCover({
            coverUrl: uploadResult && uploadResult.coverUrl,
            title: this.data.form.title,
            summary: this.data.form.summary,
            activityTypeName: this.data.form.activityTypeName,
            location: this.data.form.location,
            locationCampus: this.data.form.campusCode,
          });
          if (!coverUrl) {
            feedback.error("活动图片上传失败");
            return;
          }
          this.setData({
            "form.coverUrl": coverUrl,
            "form.coverTempPath": "",
          });
          feedback.success("活动图片上传成功");
        } catch (e) {
        } finally {
          this.setData({ coverUploading: false });
        }
      },
      fail: () => {
        feedback.error("图片选择失败");
      },
    });
  },

  onRemoveCoverTap() {
    if (this.data.coverUploading) return;
    this.setData({
      "form.coverUrl": this.deriveFormFallbackCover(),
      "form.coverTempPath": "",
    });
  },

  onCoverPreviewError() {
    this.setData({
      "form.coverTempPath": "",
      "form.coverUrl": this.deriveFormFallbackCover(),
    });
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

  onOpenCampusTypePicker() {
    if (!Array.isArray(this.data.campusTypeOptions) || this.data.campusTypeOptions.length === 0) {
      feedback.error("暂无可选校区类型");
      return;
    }
    this.setData({ showCampusTypePicker: true });
  },

  onCloseCampusTypePicker() {
    this.setData({ showCampusTypePicker: false });
  },

  onCampusTypeConfirm(event) {
    const rawIndex = Array.isArray(event.detail.index) ? event.detail.index[0] : event.detail.index;
    const index = Number(rawIndex);
    const selected = Number.isFinite(index) ? this.data.campusTypeOptions[index] : null;
    if (!selected) {
      this.setData({ showCampusTypePicker: false });
      return;
    }
    this.setData({
      "form.campusCode": selected,
      showCampusTypePicker: false,
    });
    this.refreshDerivedCover();
  },

  onCampusTypeCancel() {
    this.setData({ showCampusTypePicker: false });
  },

  onOpenTypePicker() {
    if (!Array.isArray(this.data.activityTypeOptions) || this.data.activityTypeOptions.length === 0) {
      feedback.error("暂无可选活动类型，请先维护活动分类");
      return;
    }
    this.setData({ showTypePicker: true });
  },

  onCloseTypePicker() {
    this.setData({ showTypePicker: false });
  },

  onTypeConfirm(event) {
    const rawIndex = Array.isArray(event.detail.index) ? event.detail.index[0] : event.detail.index;
    const index = Number(rawIndex);
    const selected = Number.isFinite(index) ? this.data.activityTypeOptions[index] : null;
    if (!selected) {
      this.setData({ showTypePicker: false });
      return;
    }
    this.setData({
      "form.activityTypeId": Number(selected.id),
      "form.activityTypeName": selected.name || "",
      showTypePicker: false,
    });
    this.refreshDerivedCover();
  },

  onTypeCancel() {
    this.setData({ showTypePicker: false });
  },

  onReviewCommentInput(event) {
    this.setData({ reviewComment: event.detail });
  },

  resetForm() {
    this.setData({
      editingActivityId: null,
      form: emptyForm(),
      reviewComment: "",
      coverUploading: false,
      showCampusTypePicker: false,
      showTypePicker: false,
      showDateTimePicker: false,
      activeDateField: "",
    });
  },

  deriveFormFallbackCover() {
    return getActivityFallbackCover(this.data.form || {});
  },

  refreshDerivedCover() {
    if (this.data.form.coverTempPath) {
      return;
    }
    const currentCoverUrl = String((this.data.form && this.data.form.coverUrl) || "").trim();
    const nextFallback = this.deriveFormFallbackCover();
    if (!currentCoverUrl || currentCoverUrl.startsWith("/static/activity-themes/")) {
      this.setData({
        "form.coverUrl": nextFallback,
      });
    }
  },

  onEditTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const target = this.data.activities.find((item) => Number(item.id) === activityId);
    if (!target) return;

    const nextForm = buildFormFromActivity(target);
    if (!nextForm.campusCode) {
      const locationText = String(nextForm.location || "");
      nextForm.campusCode = locationText.includes("线上") ? "ONLINE" : "NORTH";
    }
    if (!nextForm.activityTypeName && Number.isFinite(Number(nextForm.activityTypeId))) {
      const typeMatch = (this.data.activityTypeOptions || []).find(
        (item) => Number(item.id) === Number(nextForm.activityTypeId)
      );
      if (typeMatch) {
        nextForm.activityTypeName = typeMatch.name || "";
      }
    }

    this.setData({
      editingActivityId: activityId,
      form: nextForm,
      coverUploading: false,
      showCampusTypePicker: false,
      showTypePicker: false,
      showDateTimePicker: false,
      activeDateField: "",
    });
  },

  buildPayload() {
    const form = this.data.form;
    const payload = {
      title: String(form.title || "").trim(),
      summary: String(form.summary || "").trim(),
      coverUrl: String(form.coverUrl || "").trim(),
      campusCode: normalizeCampusCode(form.campusCode),
      location: String(form.location || "").trim(),
      activityTypeId: Number(form.activityTypeId),
      startTime: normalizeDateTimeInput(form.startTime),
      endTime: normalizeDateTimeInput(form.endTime),
      maxParticipants: Number(form.maxParticipants || 0),
    };

    const deadline = normalizeDateTimeInput(form.registrationDeadline);
    if (deadline) payload.registrationDeadline = deadline;
    return payload;
  },

  /**
   * 发布/更新活动：仅组织者可操作，前置校验时间和人数。
   */
  async onSubmitTap() {
    if (this.data.operatorRole !== "ORGANIZER") return feedback.error("请先切换为组织者");
    if (this.data.coverUploading) return feedback.error("活动图片上传中，请稍后再提交");

    const payload = this.buildPayload();
    if (!payload.title || !payload.campusCode || !payload.startTime || !payload.endTime || !payload.activityTypeId) {
      return feedback.error("标题、校区类型、活动类型、开始和结束时间必填");
    }

    const hasValidCampus = (this.data.campusTypeOptions || []).some(
      (item) => item === payload.campusCode
    );
    if (!hasValidCampus) {
      return feedback.error("请选择有效校区类型");
    }

    if (payload.campusCode !== "ONLINE" && !payload.location) {
      return feedback.error("南校区/北校区活动必须填写具体地点");
    }

    const hasValidType = (this.data.activityTypeOptions || []).some(
      (item) => Number(item.id) === Number(payload.activityTypeId)
    );
    if (!hasValidType) {
      return feedback.error("请选择有效活动类型");
    }

    const startTimestamp = parseDateTimeTimestamp(payload.startTime);
    const endTimestamp = parseDateTimeTimestamp(payload.endTime);
    if (!Number.isFinite(startTimestamp) || !Number.isFinite(endTimestamp) || startTimestamp >= endTimestamp) {
      return feedback.error("开始时间必须早于结束时间");
    }

    if (payload.registrationDeadline) {
      const deadlineTimestamp = parseDateTimeTimestamp(payload.registrationDeadline);
      if (!Number.isFinite(deadlineTimestamp)) {
        return feedback.error("报名截止时间格式不正确");
      }
      if (deadlineTimestamp > startTimestamp) {
        return feedback.error("报名截止时间不能晚于开始时间");
      }
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
      this.setData({ reviewComment: "" });
      await this.loadActivities();
    } catch (e) {}
  },

  onCheckinTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({
      url: `/pages/organizer-checkin/index?activityId=${activityId}`,
    });
  },

  onManagerTap(event) {
    const activityId = Number(event.currentTarget.dataset.id);
    const title = encodeURIComponent(String(event.currentTarget.dataset.title || ""));
    if (!activityId) {
      feedback.error("活动信息缺失");
      return;
    }
    wx.navigateTo({
      url: `/pages/activity-managers/index?activityId=${activityId}&title=${title}`,
    });
  },
});
