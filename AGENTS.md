# Clawd Mobile Maintenance

- The canonical mobile source is `android/` in this repository. Do not edit old temporary checkouts for new releases.
- Preserve the AGPL license and upstream attribution. Do not remove old releases or the historical APK.
- Never commit pairing tokens, credentials, local preferences, keystores, or deployment-specific trust fingerprints.
- Before packaging, run `bash scripts/build-mobile.sh`. It embeds the exact source commit and runs lint, focused tests, and the APK build.
- Before publishing, run `node scripts/package-release.mjs`. It rejects dirty sources, wrong versions, wrong embedded commits, and incompatible signing certificates.
- CI artifacts use a CI debug certificate and are not upgrade-compatible release APKs. Official test APKs must use the existing local signing key.
- A build is not a phone acceptance test. Report device/network testing gaps explicitly.
