#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export CLAWD_SOURCE_REVISION="$(git -C "$ROOT" rev-parse HEAD)"

if [[ -z "${JAVA_HOME:-}" && -d /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]]; then
  export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

cd "$ROOT/android"
bash ./gradlew :app:lintDebug :app:assembleDebug :app:testDebugUnitTest \
  --tests com.clawd.mobile.ws.ConnectionDiagnosticTest \
  --tests com.clawd.mobile.data.UsageFreshnessTest \
  --tests com.clawd.mobile.ws.MessageParserTest \
  --tests com.clawd.mobile.ws.ConnectionStateTest \
  --tests com.clawd.mobile.ws.WsClientTest \
  --no-daemon
