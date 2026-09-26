"use strict";

const fs = require("node:fs");
const path = require("node:path");
const os = require("node:os");
const MAX_FILE = 8 * 1024 * 1024;
const METRICS = ["sessionsStarted", "turnsCompleted", "toolCalls", "activityEvents"];
const PERIODS = ["today", "week", "month", "year"];

function readJson(file) {
  // A bounded read also protects against a file growing between stat and read.
  const fd = fs.openSync(file, "r");
  try {
    const size = fs.fstatSync(fd).size;
    if (size > MAX_FILE) throw new Error("oversized aggregate");
    const buffer = Buffer.alloc(size + 1);
    const length = fs.readSync(fd, buffer, 0, buffer.length, 0);
    if (length > size) throw new Error("aggregate changed during read");
    return JSON.parse(buffer.toString("utf8", 0, length));
  } finally { fs.closeSync(fd); }
}

function readAntigravityQuota(file = path.join(os.homedir(), ".clawd/account-quota.json")) {
  try {
    const sources = readJson(file).sources;
    const quota = sources?.find((source) => source?.host === null)?.antigravityQuota;
    const labels = {
      geminiFiveHour: "Gemini · 5h", geminiWeekly: "Gemini · 7d",
      thirdPartyFiveHour: "第三方 · 5h", thirdPartyWeekly: "第三方 · 7d",
    };
    const windows = Object.entries(labels).flatMap(([key, label]) => {
      const bucket = quota?.group?.[key];
      const used = bucket?.usedPercent;
      if (typeof used !== "number" || !Number.isFinite(used) || used < 0 || used > 100) return [];
      return [{ key, label, usedPercent: Math.round(used), remainingPercent: Math.round(100 - used),
        resetAt: Number.isFinite(bucket.resetAt) ? bucket.resetAt : null }];
    });
    return { status: windows.length ? "ok" : "unavailable", windows,
      updatedAt: Number(quota?.lastSeenAt) || Number(quota?.updatedAt) || 0 };
  } catch { return { status: "unavailable", windows: [], updatedAt: 0 }; }
}

function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function rangeDates(period, now) {
  if (!PERIODS.includes(period)) throw new Error("unsupported period");
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const start = new Date(end);
  if (period === "week") start.setDate(start.getDate() - (start.getDay() + 6) % 7);
  if (period === "month") start.setDate(1);
  if (period === "year") { start.setDate(1); start.setMonth(0); }
  const dates = [];
  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) dates.push(dateKey(cursor));
  return dates;
}

function count(value) { return Number.isSafeInteger(value) && value >= 0 && value <= 2147483647 ? value : null; }
function hours(value) {
  return Array.isArray(value) && value.length === 24 && value.every((n) => count(n) !== null) ? value.slice() : null;
}

function readFootprints(period = "today", root = path.join(os.homedir(), ".clawd/recap-v1"), now = new Date()) {
  const dates = rangeDates(period, now);
  const months = new Map();
  let incomplete = false;
  const days = dates.map((localDate) => {
    const month = localDate.slice(0, 7);
    if (!months.has(month)) {
      const pair = {};
      for (const kind of ["daily", "coverage"]) {
        try {
          const data = readJson(path.join(root, `${kind}-${month}.json`));
          if (data.schemaVersion !== 2 || data.month !== month || typeof data.days !== "object" || !data.days || Array.isArray(data.days) || Object.keys(data.days).length > 31) throw new Error("invalid aggregate");
          pair[kind] = data.days;
        } catch (error) { if (error.code !== "ENOENT") incomplete = true; }
      }
      months.set(month, pair);
    }
    const pair = months.get(month);
    const day = pair.daily?.[localDate];
    const coverage = pair.coverage?.[localDate];
    const candidates = Object.values(day?.rows || {});
    const rows = candidates.length > 96 ? [] : candidates.flatMap((row) => {
      const hourly = hours(row?.hours);
      if (!hourly || !/^[a-z0-9-]{1,48}$/.test(row.agentId) || !["local", "remote", "wsl"].includes(row.scope)) { incomplete = true; return []; }
      const metrics = Object.fromEntries(METRICS.map((key) => [key, count(row.metrics?.[key])]));
      return [{ agentId: row.agentId, scope: row.scope, metrics, hours: hourly,
        sessionsStartedPartial: row.sessionsStartedPartial === true }];
    });
    if (candidates.length > 96) incomplete = true;
    const coverageMinutes = hours(coverage?.coverageMinutes)?.map((n) => Math.min(n, 1440)) || null;
    const hourCapacities = hours(coverage?.hourCapacities)?.map((n) => Math.min(n, 1440)) || null;
    return { localDate, recorded: !!day || !!coverage, rows,
      coverageMinutes, hourCapacities };
  });
  return { version: "v2", type: "footprints_snapshot", timestamp: now.getTime(),
    status: days.some((day) => day.recorded) ? "ready" : "unavailable",
    period, startDate: dates[0], endDate: dates.at(-1),
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    incomplete, days };
}

module.exports = { readAntigravityQuota, readFootprints, rangeDates };
