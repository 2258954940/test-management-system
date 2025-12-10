// 日期工具
// formatDate: 格式化时间戳或 Date 对象
// formatTimeRange: 处理日期范围选择器返回的数组
// getLast7Days: 返回近 7 天日期数组（格式 MM/DD）

function toDate(input) {
  if (input instanceof Date) return input;
  const d = new Date(input);
  return Number.isNaN(d.getTime()) ? null : d;
}

function pad(num) {
  return String(num).padStart(2, "0");
}

/**
 * 按指定格式格式化日期
 * @param {number|string|Date} ts 时间戳或 Date
 * @param {string} format 默认 "YYYY-MM-DD HH:mm:ss"
 * @returns {string}
 */
export function formatDate(ts, format = "YYYY-MM-DD HH:mm:ss") {
  const d = toDate(ts);
  if (!d) return "";
  const map = {
    YYYY: d.getFullYear(),
    MM: pad(d.getMonth() + 1),
    DD: pad(d.getDate()),
    HH: pad(d.getHours()),
    mm: pad(d.getMinutes()),
    ss: pad(d.getSeconds()),
  };
  let res = format;
  Object.keys(map).forEach((k) => {
    res = res.replace(k, map[k]);
  });
  return res;
}

/**
 * 处理日期范围
 * @param {Array} range [start, end]
 * @returns {{ start: string, end: string }}
 */
export function formatTimeRange(range) {
  if (!Array.isArray(range) || range.length < 2) return { start: "", end: "" };
  return {
    start: formatDate(range[0]),
    end: formatDate(range[1]),
  };
}

/**
 * 获取近 7 天日期，格式 MM/DD，从 6 天前到今天
 * @returns {string[]}
 */
export function getLast7Days() {
  const res = [];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 24 * 3600 * 1000);
    res.push(`${pad(d.getMonth() + 1)}/${pad(d.getDate())}`);
  }
  return res;
}

export default {
  formatDate,
  formatTimeRange,
  getLast7Days,
};
