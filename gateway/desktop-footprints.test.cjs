"use strict";
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { readAntigravityQuota, readFootprints, rangeDates } = require("./desktop-footprints.cjs");

function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "clawd-footprints-test-"));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  return { root, write: (name, data) => fs.writeFileSync(path.join(root, name), JSON.stringify(data)) };
}
const now = new Date(2026, 8, 26, 16);
const row = { agentId: "codex", scope: "local", hours: Array(24).fill(3),
  metrics: { sessionsStarted: null, turnsCompleted: 4, toolCalls: 9, activityEvents: 72 },
  support: { sessionsStarted: false, turnsCompleted: true, toolCalls: true },
  sessionsStartedPartial: true, rawPrompt: "PRIVATE", sessionHmac: "PRIVATE" };

test("AGY keeps four independent windows, source observation and zero use", (t) => {
  const { root, write } = fixture(t);
  write("quota.json", { sources: [{ host: null, antigravityQuota: {
    lastSeenAt: 1234, group: {
      geminiFiveHour: { usedPercent: 12, resetAt: 9999 }, geminiWeekly: { usedPercent: 38 },
      thirdPartyFiveHour: { usedPercent: 0 }, thirdPartyWeekly: { usedPercent: 100 },
    },
  } }] });
  const quota = readAntigravityQuota(path.join(root, "quota.json"));
  assert.deepEqual(quota.windows.map((w) => w.remainingPercent), [88, 62, 100, 0]);
  assert.equal(quota.updatedAt, 1234);
  assert.equal(quota.windows[0].resetAt, 9999);
});

test("missing AGY is unavailable, not a full allowance; remote quota is not local", (t) => {
  const { root, write } = fixture(t);
  write("quota.json", { sources: [{ host: "other", antigravityQuota: { group: { geminiWeekly: { usedPercent: 2 } } } }] });
  assert.equal(readAntigravityQuota(path.join(root, "quota.json")).status, "unavailable");
  write("quota.json", { sources: [{ host: null, antigravityQuota: { group: { geminiFiveHour: { usedPercent: null }, geminiWeekly: { usedPercent: "2" } } } }] });
  assert.equal(readAntigravityQuota(path.join(root, "quota.json")).windows.length, 0);
});

test("ranges use local calendar, week starts Monday and year is bounded", () => {
  assert.deepEqual(rangeDates("today", now), ["2026-09-26"]);
  assert.equal(rangeDates("week", now)[0], "2026-09-21");
  assert.equal(rangeDates("month", now).length, 26);
  assert.equal(rangeDates("year", now).length, 269);
  assert.throws(() => rangeDates("../../meta", now));
});

test("footprints preserve nullable counts, coverage and privacy without writes", (t) => {
  const { root, write } = fixture(t);
  const file = path.join(root, "daily-2026-09.json");
  write("daily-2026-09.json", { schemaVersion: 2, month: "2026-09", days: {
    "2026-09-26": { rows: { hidden: row }, hourCapacities: Array(24).fill(60) },
  } });
  write("coverage-2026-09.json", { schemaVersion: 2, month: "2026-09", days: {
    "2026-09-26": { coverageMinutes: Array(24).fill(60), hourCapacities: Array(24).fill(60) },
  } });
  const before = fs.readFileSync(file, "utf8");
  const result = readFootprints("week", root, now);
  assert.equal(result.status, "ready");
  assert.equal(result.days[0].recorded, false);
  assert.equal(result.days.at(-1).rows[0].metrics.sessionsStarted, null);
  assert.equal(result.days.at(-1).coverageMinutes.length, 24);
  assert.equal(JSON.stringify(result).includes("PRIVATE"), false);
  assert.equal(fs.readFileSync(file, "utf8"), before);
});

test("absent and corrupt recording do not produce invented zero metrics", (t) => {
  const { root } = fixture(t);
  const missing = readFootprints("today", root, now);
  assert.equal(missing.status, "unavailable");
  assert.equal(missing.days[0].recorded, false);
  fs.writeFileSync(path.join(root, "daily-2026-09.json"), "{");
  assert.equal(readFootprints("today", root, now).incomplete, true);
});

test("oversized files and invalid rows are rejected", (t) => {
  const { root, write } = fixture(t);
  fs.writeFileSync(path.join(root, "daily-2026-09.json"), " ".repeat(8 * 1024 * 1024 + 1));
  assert.equal(readFootprints("today", root, now).incomplete, true);
  write("daily-2026-09.json", { schemaVersion: 2, month: "2026-09", days: {
    "2026-09-26": { rows: { x: { ...row, agentId: "../private", hours: [] } } },
  } });
  const result = readFootprints("today", root, now);
  assert.equal(result.days[0].rows.length, 0);
  assert.equal(result.incomplete, true);
});
