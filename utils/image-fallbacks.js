const { normalizeBackendAssetUrl } = require("./backend-asset-url");
const DEFAULT_AVATAR = "/static/placeholders/avatar-default.png";

const COVER_BY_THEME = {
  tech: "/static/activity-themes/data-tech-talk.jpg",
  online: "/static/activity-themes/online-sharing.jpg",
  mental: "/static/activity-themes/mental-health.jpg",
  music: "/static/activity-themes/campus-singer.jpg",
  sports: "/static/activity-themes/basketball.jpg",
  club: "/static/activity-themes/club-activity.jpg",
  generic: "/static/activity-themes/club-activity.jpg",
};

function buildActivityText(activity) {
  if (!activity || typeof activity !== "object") {
    return "";
  }
  return [
    activity.title,
    activity.summary,
    activity.activityTypeName,
    activity.location,
    activity.locationCampus,
    activity.content,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

function detectCoverTheme(activity) {
  const text = buildActivityText(activity);

  if (!text) {
    return "generic";
  }

  if (/(线上|在线|直播|云端|remote|online|经验分享|分享会|保研|求职|实习|成长|交流会)/.test(text)) {
    return "online";
  }

  if (/(运动|篮球|足球|羽毛球|排球|跑步|马拉松|健身|瑜伽|乒乓|赛事|sport|run|fitness|match)/.test(text)) {
    return "sports";
  }

  if (/(歌手|音乐|演出|舞蹈|晚会|乐队|唱歌|合唱|music|show|concert|sing)/.test(text)) {
    return "music";
  }

  if (/(心理|健康周|心灵|咨询|减压|疗愈|倾诉|陪伴|mental|health|counsel)/.test(text)) {
    return "mental";
  }

  if (/(数据|大数据|人工智能|可视化|算法|编程|开发|黑客|技术讲座|科技|创新|云计算|ai|tech|code|data)/.test(text)) {
    return "tech";
  }

  if (/(社团|招新|协会|俱乐部|迎新|读书会|志愿|公益|社区|服务|联谊|沙龙|交流|club|community|society)/.test(text)) {
    return "club";
  }

  return "generic";
}

function getActivityFallbackCover(activity) {
  const theme = detectCoverTheme(activity);
  return COVER_BY_THEME[theme] || COVER_BY_THEME.generic;
}

function resolveActivityCover(activity) {
  const remoteCover = String(
    (activity && (activity.cover || activity.coverUrl || activity.coverDisplay)) || ""
  ).trim();
  const normalizedCover = normalizeBackendAssetUrl(remoteCover);
  return normalizedCover || getActivityFallbackCover(activity);
}

module.exports = {
  DEFAULT_AVATAR,
  COVER_BY_THEME,
  detectCoverTheme,
  getActivityFallbackCover,
  resolveActivityCover,
};
