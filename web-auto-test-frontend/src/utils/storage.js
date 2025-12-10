// 本地存储工具

export function setItem(key, value) {
  try {
    const val = typeof value === "string" ? value : JSON.stringify(value);
    localStorage.setItem(key, val);
  } catch (e) {
    console.error("setItem error", e);
  }
}

export function getItem(key) {
  const val = localStorage.getItem(key);
  if (val === null || val === undefined) return null;
  try {
    return JSON.parse(val);
  } catch (e) {
    return val;
  }
}

export function removeItem(key) {
  localStorage.removeItem(key);
}

export function clearAll() {
  localStorage.clear();
}

export default {
  setItem,
  getItem,
  removeItem,
  clearAll,
};
