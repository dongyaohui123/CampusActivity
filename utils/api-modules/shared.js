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

module.exports = {
  SUCCESS_CODE,
  buildQueryString,
};
