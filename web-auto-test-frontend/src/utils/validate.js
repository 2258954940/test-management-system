// 校验工具

/**
 * 判断 URL 合法性（支持 http/https）
 * @param {string} str
 * @returns {boolean}
 */
export function isUrl(str) {
  if (!str || typeof str !== "string") return false;
  const pattern =
    /^(https?:)\/\/[\w.-]+(?:\.[\w\.-]+)+(?:[\w\-._~:/?#[\]@!$&'()*+,;=.]+)?$/i;
  return pattern.test(str.trim());
}

/**
 * 判断字符串是否为空（含仅空格）
 * @param {string} str
 * @returns {boolean}
 */
export function isEmpty(str) {
  return str === undefined || str === null || String(str).trim() === "";
}

/**
 * 校验密码长度 ≥ 6
 * @param {string} str
 * @returns {boolean}
 */
export function checkPassword(str) {
  if (typeof str !== "string") return false;
  return str.length >= 6;
}

/**
 * 校验必填字段
 * @param {string[]} fields 必填字段名数组
 * @param {Object} data 数据源
 * @returns {{ valid: boolean, missing: string[] }}
 */
export function checkRequired(fields = [], data = {}) {
  const missing = [];
  fields.forEach((key) => {
    const v = data[key];
    if (v === undefined || v === null || String(v).trim() === "") {
      missing.push(key);
    }
  });
  return { valid: missing.length === 0, missing };
}

export default {
  isUrl,
  isEmpty,
  checkPassword,
  checkRequired,
};
