# Mobile footprints and AGY quota

Approved scope: native session activity and quota footprints, replacing the Devices tab.

## Data boundary

- Forward the local desktop Antigravity quota store, keeping Gemini and third-party five-hour/weekly buckets separate. Preserve observation timestamps and missing values.
- Read only desktop recap daily aggregates and coverage files. Never initialize a second recap writer or send raw journal entries, identities, paths, credentials, or metadata salts.
- Request `footprints_query` over the existing authenticated gateway WebSocket. Return `footprints_snapshot` for today, week (Monday start), month, or year using desktop-local dates.
- Read bounded files, allowlist fields, limit requests and payload size. Missing recording/coverage is not zero activity. Nullable unsupported metrics and partial totals stay explicit.

## UI and lifecycle

- Native Footprints tab with range selector, metric summary, hourly activity for today, daily heatmap for longer ranges, per-agent counts, and current account quota.
- Existing soft cards and light/dark themes; no prominent card borders.
- Connection diagnostics remain available from Footprints. No recording toggle or destructive clear control on mobile.
- Request on tab entry, range change, manual refresh, or reconnect while visible. No background polling. Keep in-memory last snapshot per range, labelled offline/stale. Application restart does not retain footprints.
- Current quota is not historical token spending. Activity signals are not working-duration minutes. Coverage reflects persisted desktop coverage and may lag the live desktop slightly.

## Verification

Gateway fixtures cover quotas, ranges, missing/corrupt files, unsupported metrics, privacy and payload limits. Android parser and state tests cover backwards compatibility, nullable metrics and separate message delivery. Run build/lint/focused tests, then verify authenticated public quota and footprint responses. Physical-phone UI acceptance remains necessary.
