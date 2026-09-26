# Desktop quota and footprints adapter

`desktop-footprints.cjs` is a credential-free, read-only adapter. It reads the local source in `~/.clawd/account-quota.json` and daily aggregates in `~/.clawd/recap-v1`. It does not query provider APIs, instantiate the desktop recap runtime, or modify the signed desktop application.

Deploy beside the existing `mobile-gateway.js`, then apply `mobile-gateway-footprints.patch` to the gateway version following the freshness patch. Back up the existing gateway first and review context on different versions. Restart the gateway LaunchAgent only; the Cloudflare tunnel and paired credentials stay unchanged.

Authenticated clients send `{ "type": "footprints_query", "period": "today" }`. Supported periods: today, week (Monday), month, year. Replies are `footprints_snapshot`, with desktop-local dates, nullable metrics, sanitized agent/scope rows, hourly signal counts, and persisted coverage. Reads are bounded to 8 MiB per file, rows are capped at 96 per day, responses at 2 MiB, and each socket at 8 footprint requests per 10 seconds. No paths or date ranges supplied by clients are used to access files.

Missing/corrupt recording is distinct from zero activity. Coverage may lag the live desktop because it uses persisted aggregates. Current quotas preserve desktop observation time; their outgoing message time is not treated as provider refresh time. Activity counts are not historical billing, token consumption or time spent.

Tests: `node --test gateway/desktop-footprints.test.cjs`.
